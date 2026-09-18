import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/network/page_payload.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/communications_repository.dart';
import '../datasources/communications_remote_data_source.dart';
import '../models/communication_models.dart';

final communicationsRepositoryProvider = Provider<CommunicationsRepository>((
  ref,
) {
  return CommunicationsRepositoryImpl(
    ref.watch(communicationsRemoteDataSourceProvider),
  );
});

class CommunicationsRepositoryImpl implements CommunicationsRepository {
  const CommunicationsRepositoryImpl(this._remote);

  final CommunicationsRemoteDataSource _remote;

  @override
  Future<Result<PagePayload<CommunicationModel>>> communications(
    CommunicationFilter filter,
  ) => _guard(() => _remote.communications(filter));

  @override
  Future<Result<CommunicationModel>> create(Map<String, dynamic> payload) =>
      _guard(() => _remote.create(payload));

  @override
  Future<Result<CommunicationModel>> update(
    String communicationId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.update(communicationId, payload));

  @override
  Future<Result<CommunicationModel>> publish(String communicationId) =>
      _guard(() => _remote.publish(communicationId));

  @override
  Future<Result<CommunicationModel>> unpublish(String communicationId) =>
      _guard(() => _remote.unpublish(communicationId));

  @override
  Future<Result<CommunicationModel>> archive(String communicationId) =>
      _guard(() => _remote.archive(communicationId));

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
