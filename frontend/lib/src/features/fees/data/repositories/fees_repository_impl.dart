import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/fees_repository.dart';
import '../datasources/fees_remote_data_source.dart';
import '../models/fee_models.dart';

final feesRepositoryProvider = Provider<FeesRepository>((ref) {
  return FeesRepositoryImpl(ref.watch(feesRemoteDataSourceProvider));
});

class FeesRepositoryImpl implements FeesRepository {
  const FeesRepositoryImpl(this._remoteDataSource);

  final FeesRemoteDataSource _remoteDataSource;

  @override
  Future<Result<List<FeeCategoryModel>>> categories() {
    return _guard(() async => (await _remoteDataSource.categories()).content);
  }

  @override
  Future<Result<FeeCategoryModel>> createCategory(Map<String, dynamic> payload) {
    return _guard(() => _remoteDataSource.createCategory(payload));
  }

  @override
  Future<Result<List<FeeStructureModel>>> structures() {
    return _guard(() async => (await _remoteDataSource.structures()).content);
  }

  @override
  Future<Result<FeeStructureModel>> structure(String id) {
    return _guard(() => _remoteDataSource.structure(id));
  }

  @override
  Future<Result<FeeStructureModel>> createStructure(
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.createStructure(payload));
  }

  @override
  Future<Result<FeeStructureModel>> updateStructure(
    String id,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateStructure(id, payload));
  }

  @override
  Future<Result<List<StudentFeeAssignmentModel>>> assignments() {
    return _guard(() async => (await _remoteDataSource.assignments()).content);
  }

  @override
  Future<Result<StudentFeeAssignmentModel>> createAssignment(
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.createAssignment(payload));
  }

  @override
  Future<Result<FeeReceiptModel>> collectPayment(
    String assignmentId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.collectPayment(assignmentId, payload));
  }

  @override
  Future<Result<FeeReceiptModel>> receipt(String receiptNumber) {
    return _guard(() => _remoteDataSource.receipt(receiptNumber));
  }

  @override
  Future<Result<List<FeeDefaulterModel>>> defaulters() {
    return _guard(() async => (await _remoteDataSource.defaulters()).content);
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
