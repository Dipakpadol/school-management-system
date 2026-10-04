import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/download/file_downloader.dart';
import '../../../../core/error/failure.dart';
import '../../../../core/network/page_payload.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/backup_repository.dart';
import '../datasources/backup_remote_data_source.dart';
import '../models/backup_models.dart';

final backupRepositoryProvider = Provider<BackupRepository>((ref) {
  return BackupRepositoryImpl(ref.watch(backupRemoteDataSourceProvider));
});

class BackupRepositoryImpl implements BackupRepository {
  const BackupRepositoryImpl(this._remote);

  final BackupRemoteDataSource _remote;

  @override
  Future<Result<BackupSummaryModel>> summary() => _guard(_remote.summary);

  @override
  Future<Result<PagePayload<BackupRecordModel>>> backups(BackupFilter filter) {
    return _guard(() => _remote.backups(filter));
  }

  @override
  Future<Result<BackupRecordModel>> createBackup({String? notes}) {
    return _guard(() => _remote.createBackup(notes: notes));
  }

  @override
  Future<Result<void>> downloadBackup(BackupRecordModel backup) {
    return _guard(() async {
      final bytes = await _remote.downloadBackup(backup.id);
      await downloadBytes(bytes, backup.fileName, 'application/octet-stream');
    });
  }

  @override
  Future<Result<RestoreHistoryModel>> restoreBackup(
    String backupId, {
    required String confirmationText,
    String? notes,
  }) {
    return _guard(
      () => _remote.restoreBackup(
        backupId,
        confirmationText: confirmationText,
        notes: notes,
      ),
    );
  }

  @override
  Future<Result<BackupRecordModel>> deleteBackup(String backupId) {
    return _guard(() => _remote.deleteBackup(backupId));
  }

  @override
  Future<Result<PagePayload<RestoreHistoryModel>>> restores({
    int page = 0,
    int size = 20,
  }) {
    return _guard(() => _remote.restores(page: page, size: size));
  }

  Future<Result<T>> _guard<T>(Future<T> Function() action) async {
    try {
      return Success(await action());
    } on DioException catch (error) {
      return FailureResult(_failureFromDio(error));
    } on FormatException catch (error) {
      return FailureResult(ValidationFailure(error.message));
    } catch (_) {
      return const FailureResult(UnexpectedFailure());
    }
  }

  Failure _failureFromDio(DioException error) {
    final statusCode = error.response?.statusCode;
    if (statusCode == 401) {
      return const UnauthorizedFailure();
    }
    if (statusCode == 403) {
      return const ValidationFailure('You do not have permission.');
    }
    if (statusCode == 400 ||
        statusCode == 404 ||
        statusCode == 409 ||
        statusCode == 422) {
      return ValidationFailure(_serverMessage(error) ?? 'Request is invalid.');
    }
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.receiveTimeout) {
      return const NetworkFailure('Unable to reach the server.');
    }
    return ValidationFailure(_serverMessage(error) ?? 'Request failed.');
  }

  String? _serverMessage(DioException error) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      return data['message'] as String?;
    }
    return null;
  }
}
