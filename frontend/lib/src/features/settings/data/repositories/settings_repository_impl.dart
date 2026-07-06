import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../../users/data/models/user_models.dart';
import '../../domain/repositories/settings_repository.dart';
import '../datasources/settings_remote_data_source.dart';
import '../models/role_permission_models.dart';

final settingsRepositoryProvider = Provider<SettingsRepository>((ref) {
  return SettingsRepositoryImpl(ref.watch(settingsRemoteDataSourceProvider));
});

class SettingsRepositoryImpl implements SettingsRepository {
  const SettingsRepositoryImpl(this._remoteDataSource);

  final SettingsRemoteDataSource _remoteDataSource;

  @override
  Future<Result<List<RoleModel>>> roles({String? query, String? status}) {
    return _guard(() => _remoteDataSource.roles(query: query, status: status));
  }

  @override
  Future<Result<RoleModel>> createRole(Map<String, dynamic> payload) {
    return _guard(() => _remoteDataSource.createRole(payload));
  }

  @override
  Future<Result<RoleModel>> updateRole(
    String roleId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateRole(roleId, payload));
  }

  @override
  Future<Result<void>> deleteRole(String roleId) {
    return _guard(() => _remoteDataSource.deleteRole(roleId));
  }

  @override
  Future<Result<List<PermissionOptionModel>>> permissions() {
    return _guard(_remoteDataSource.permissions);
  }

  @override
  Future<Result<RolePermissionMatrixModel>> rolePermissions(String roleId) {
    return _guard(() => _remoteDataSource.rolePermissions(roleId));
  }

  @override
  Future<Result<RolePermissionMatrixModel>> updateRolePermissions(
    String roleId,
    List<String> permissionIds,
  ) {
    return _guard(
      () => _remoteDataSource.updateRolePermissions(roleId, permissionIds),
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
    if (statusCode == 403) {
      return const ValidationFailure('You do not have permission.');
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
