import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/notifications_repository.dart';
import '../datasources/notifications_remote_data_source.dart';
import '../models/notification_models.dart';

final notificationsRepositoryProvider = Provider<NotificationsRepository>((
  ref,
) {
  return NotificationsRepositoryImpl(
    ref.watch(notificationsRemoteDataSourceProvider),
  );
});

class NotificationsRepositoryImpl implements NotificationsRepository {
  const NotificationsRepositoryImpl(this._remoteDataSource);

  final NotificationsRemoteDataSource _remoteDataSource;

  @override
  Future<Result<NotificationLogModel>> sendTest({
    required String channel,
    required String recipient,
    required String subject,
    required String message,
  }) {
    return _guard(
      () => _remoteDataSource.sendTest(
        channel: channel,
        recipient: recipient,
        subject: subject,
        message: message,
      ),
    );
  }

  @override
  Future<Result<List<NotificationLogModel>>> logs() {
    return _guard(_remoteDataSource.logs);
  }

  @override
  Future<Result<List<NotificationTemplateModel>>> templates() {
    return _guard(_remoteDataSource.templates);
  }

  @override
  Future<Result<NotificationTemplateModel>> createTemplate(
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.createTemplate(payload));
  }

  @override
  Future<Result<NotificationTemplateModel>> updateTemplate(
    String id,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateTemplate(id, payload));
  }

  @override
  Future<Result<void>> deleteTemplate(String id) {
    return _guard(() => _remoteDataSource.deleteTemplate(id));
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
