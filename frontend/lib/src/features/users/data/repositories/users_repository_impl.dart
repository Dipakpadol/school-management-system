import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/users_repository.dart';
import '../datasources/users_remote_data_source.dart';
import '../models/user_models.dart';

final usersRepositoryProvider = Provider<UsersRepository>((ref) {
  return UsersRepositoryImpl(ref.watch(usersRemoteDataSourceProvider));
});

class UsersRepositoryImpl implements UsersRepository {
  const UsersRepositoryImpl(this._remoteDataSource);

  final UsersRemoteDataSource _remoteDataSource;

  @override
  Future<Result<List<UserModel>>> users({
    String? query,
    String? role,
    String? status,
  }) {
    return _guard(
      () async => (await _remoteDataSource.users(
        query: query,
        role: role,
        status: status,
      ))
          .content,
    );
  }

  @override
  Future<Result<List<RoleModel>>> roles() {
    return _guard(_remoteDataSource.roles);
  }

  @override
  Future<Result<UserModel>> create(Map<String, dynamic> payload) {
    return _guard(() => _remoteDataSource.create(payload));
  }

  @override
  Future<Result<UserModel>> update(String userId, Map<String, dynamic> payload) {
    return _guard(() => _remoteDataSource.update(userId, payload));
  }

  @override
  Future<Result<UserModel>> activate(String userId) {
    return _guard(() => _remoteDataSource.activate(userId));
  }

  @override
  Future<Result<UserModel>> deactivate(String userId) {
    return _guard(() => _remoteDataSource.deactivate(userId));
  }

  @override
  Future<Result<void>> resetPassword(String userId, String password) {
    return _guard(() => _remoteDataSource.resetPassword(userId, password));
  }

  @override
  Future<Result<void>> delete(String userId) {
    return _guard(() => _remoteDataSource.delete(userId));
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
