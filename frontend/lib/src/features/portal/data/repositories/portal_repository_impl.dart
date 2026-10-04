import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/portal_repository.dart';
import '../datasources/portal_remote_data_source.dart';
import '../models/portal_models.dart';

final portalRepositoryProvider = Provider<PortalRepository>((ref) {
  return PortalRepositoryImpl(ref.watch(portalRemoteDataSourceProvider));
});

class PortalRepositoryImpl implements PortalRepository {
  const PortalRepositoryImpl(this._remote);

  final PortalRemoteDataSource _remote;

  @override
  Future<Result<PortalDashboardModel>> studentDashboard() {
    return _guard(_remote.studentDashboard);
  }

  @override
  Future<Result<List<PortalChildModel>>> parentChildren() {
    return _guard(_remote.parentChildren);
  }

  @override
  Future<Result<PortalDashboardModel>> parentChildDashboard(String childId) {
    return _guard(() => _remote.parentChildDashboard(childId));
  }

  @override
  Future<Result<PortalTeacherDashboardModel>> teacherDashboard() {
    return _guard(_remote.teacherDashboard);
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
