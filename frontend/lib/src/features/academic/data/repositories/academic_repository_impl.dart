import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/academic_repository.dart';
import '../datasources/academic_remote_data_source.dart';
import '../models/academic_models.dart';

final academicRepositoryProvider = Provider<AcademicRepository>((ref) {
  return AcademicRepositoryImpl(ref.watch(academicRemoteDataSourceProvider));
});

class AcademicRepositoryImpl implements AcademicRepository {
  const AcademicRepositoryImpl(this._remoteDataSource);

  final AcademicRemoteDataSource _remoteDataSource;

  @override
  Future<Result<List<AcademicYearModel>>> years() {
    return _guard(_remoteDataSource.years);
  }

  @override
  Future<Result<AcademicYearModel>> createYear(Map<String, dynamic> payload) {
    return _guard(() => _remoteDataSource.createYear(payload));
  }

  @override
  Future<Result<AcademicYearModel>> updateYear(
    String id,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateYear(id, payload));
  }

  @override
  Future<Result<void>> deleteYear(String id) {
    return _guard(() => _remoteDataSource.deleteYear(id));
  }

  @override
  Future<Result<List<AcademicClassModel>>> classes(String academicYearId) {
    return _guard(() => _remoteDataSource.classes(academicYearId));
  }

  @override
  Future<Result<AcademicClassModel>> createClass(
    String academicYearId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.createClass(academicYearId, payload));
  }

  @override
  Future<Result<AcademicClassModel>> updateClass(
    String id,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateClass(id, payload));
  }

  @override
  Future<Result<void>> deleteClass(String id) {
    return _guard(() => _remoteDataSource.deleteClass(id));
  }

  @override
  Future<Result<List<AcademicDivisionModel>>> divisions(String classId) {
    return _guard(() => _remoteDataSource.divisions(classId));
  }

  @override
  Future<Result<AcademicDivisionModel>> createDivision(
    String classId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.createDivision(classId, payload));
  }

  @override
  Future<Result<AcademicDivisionModel>> updateDivision(
    String id,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateDivision(id, payload));
  }

  @override
  Future<Result<void>> deleteDivision(String id) {
    return _guard(() => _remoteDataSource.deleteDivision(id));
  }

  @override
  Future<Result<List<AcademicTeacherModel>>> teachers() {
    return _guard(_remoteDataSource.teachers);
  }

  @override
  Future<Result<DivisionSubjectModel>> addDivisionSubject(
    String divisionId,
    Map<String, dynamic> payload,
  ) {
    return _guard(
      () => _remoteDataSource.addDivisionSubject(divisionId, payload),
    );
  }

  @override
  Future<Result<DivisionSubjectModel>> updateDivisionSubject(
    String divisionId,
    String divisionSubjectId,
    Map<String, dynamic> payload,
  ) {
    return _guard(
      () => _remoteDataSource.updateDivisionSubject(
        divisionId,
        divisionSubjectId,
        payload,
      ),
    );
  }

  @override
  Future<Result<void>> deleteDivisionSubject(
    String divisionId,
    String divisionSubjectId,
  ) {
    return _guard(
      () => _remoteDataSource.deleteDivisionSubject(
        divisionId,
        divisionSubjectId,
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
