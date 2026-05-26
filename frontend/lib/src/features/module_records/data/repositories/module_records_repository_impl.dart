import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/download/file_downloader.dart';
import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/module_records_repository.dart';
import '../datasources/module_records_remote_data_source.dart';
import '../models/module_record_model.dart';

final moduleRecordsRepositoryProvider = Provider<ModuleRecordsRepository>((
  ref,
) {
  return ModuleRecordsRepositoryImpl(
    ref.watch(moduleRecordsRemoteDataSourceProvider),
  );
});

class ModuleRecordsRepositoryImpl implements ModuleRecordsRepository {
  const ModuleRecordsRepositoryImpl(this._remoteDataSource);

  final ModuleRecordsRemoteDataSource _remoteDataSource;

  @override
  Future<Result<List<ModuleRecordModel>>> records({
    required String moduleId,
    required String recordType,
    String? query,
  }) {
    return _guard(
      () async => (await _remoteDataSource.records(
        moduleId: moduleId,
        recordType: recordType,
        query: query,
      )).content,
    );
  }

  @override
  Future<Result<ModuleRecordModel>> create({
    required String moduleId,
    required String recordType,
    required Map<String, dynamic> payload,
  }) {
    return _guard(
      () => _remoteDataSource.create(
        moduleId: moduleId,
        recordType: recordType,
        payload: payload,
      ),
    );
  }

  @override
  Future<Result<ModuleRecordModel>> update({
    required String moduleId,
    required String recordType,
    required String id,
    required Map<String, dynamic> payload,
  }) {
    return _guard(
      () => _remoteDataSource.update(
        moduleId: moduleId,
        recordType: recordType,
        id: id,
        payload: payload,
      ),
    );
  }

  @override
  Future<Result<void>> delete({
    required String moduleId,
    required String recordType,
    required String id,
  }) {
    return _guard(
      () => _remoteDataSource.delete(
        moduleId: moduleId,
        recordType: recordType,
        id: id,
      ),
    );
  }

  @override
  Future<Result<void>> export({
    required String moduleId,
    required String recordType,
    required String format,
    String? query,
  }) {
    return _guard(() async {
      final bytes = await _remoteDataSource.export(
        moduleId: moduleId,
        recordType: recordType,
        format: format,
        query: query,
      );
      await downloadBytes(
        bytes,
        '$moduleId-$recordType.$format',
        _contentType(format),
      );
    });
  }

  @override
  Future<Result<void>> template({
    required String moduleId,
    required String recordType,
  }) {
    return _guard(() async {
      final bytes = await _remoteDataSource.template(
        moduleId: moduleId,
        recordType: recordType,
      );
      await downloadBytes(
        bytes,
        '$moduleId-$recordType-template.xlsx',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      );
    });
  }

  @override
  Future<Result<void>> importFile({
    required String moduleId,
    required String recordType,
    required String format,
    required List<int> bytes,
    required String filename,
  }) {
    return _guard(
      () => _remoteDataSource.importFile(
        moduleId: moduleId,
        recordType: recordType,
        format: format,
        bytes: bytes,
        filename: filename,
      ),
    );
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
    if (statusCode == 400 ||
        statusCode == 403 ||
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

  String _contentType(String format) {
    return switch (format) {
      'csv' => 'text/csv',
      'pdf' => 'application/pdf',
      _ => 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    };
  }

  String? _serverMessage(DioException error) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      return data['message'] as String?;
    }
    return null;
  }
}
