import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/audit_logs_repository.dart';
import '../datasources/audit_logs_remote_data_source.dart';
import '../models/audit_log_models.dart';

final auditLogsRepositoryProvider = Provider<AuditLogsRepository>((ref) {
  return AuditLogsRepositoryImpl(ref.watch(auditLogsRemoteDataSourceProvider));
});

class AuditLogsRepositoryImpl implements AuditLogsRepository {
  const AuditLogsRepositoryImpl(this._remoteDataSource);

  final AuditLogsRemoteDataSource _remoteDataSource;

  @override
  Future<Result<List<AuditLogModel>>> auditLogs({
    String? moduleName,
    String? action,
    String? performedBy,
  }) {
    return _guard(
      () async => (await _remoteDataSource.auditLogs(
        moduleName: moduleName,
        action: action,
        performedBy: performedBy,
      ))
          .content,
    );
  }

  @override
  Future<Result<AuditLogModel>> auditLog(String id) {
    return _guard(() => _remoteDataSource.auditLog(id));
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
    if (statusCode == 400 || statusCode == 409 || statusCode == 422) {
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
