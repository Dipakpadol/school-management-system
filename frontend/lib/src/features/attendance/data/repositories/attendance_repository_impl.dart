import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../../academic/data/models/academic_models.dart';
import '../../domain/repositories/attendance_repository.dart';
import '../datasources/attendance_remote_data_source.dart';
import '../models/attendance_models.dart';

final attendanceRepositoryProvider = Provider<AttendanceRepository>((ref) {
  return AttendanceRepositoryImpl(
    ref.watch(attendanceRemoteDataSourceProvider),
  );
});

class AttendanceRepositoryImpl implements AttendanceRepository {
  const AttendanceRepositoryImpl(this._remote);

  final AttendanceRemoteDataSource _remote;

  @override
  Future<Result<List<AcademicYearModel>>> years() => _guard(_remote.years);

  @override
  Future<Result<List<AcademicClassModel>>> classes(String academicYearId) {
    return _guard(() => _remote.classes(academicYearId));
  }

  @override
  Future<Result<List<AcademicDivisionModel>>> sections(String classId) {
    return _guard(() => _remote.sections(classId));
  }

  @override
  Future<Result<List<AttendanceStudentModel>>> students({
    required String academicYearId,
    required String classId,
    required String sectionId,
  }) {
    return _guard(
      () => _remote.students(
        academicYearId: academicYearId,
        classId: classId,
        sectionId: sectionId,
      ),
    );
  }

  @override
  Future<Result<DailyAttendanceModel>> daily({
    required String academicYearId,
    required String classId,
    required String sectionId,
    required String date,
  }) {
    return _guard(
      () => _remote.daily(
        academicYearId: academicYearId,
        classId: classId,
        sectionId: sectionId,
        date: date,
      ),
    );
  }

  @override
  Future<Result<void>> saveDaily(Map<String, dynamic> payload) {
    return _guard(() => _remote.saveDaily(payload));
  }

  @override
  Future<Result<List<int>>> export(Map<String, dynamic> query) {
    return _guard(() => _remote.export(query));
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
    final message = _serverMessage(error);
    if (statusCode == 400 || statusCode == 422 || statusCode == 409) {
      return ValidationFailure(message ?? 'Attendance request is invalid.');
    }
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.receiveTimeout) {
      return const NetworkFailure('Unable to reach the server.');
    }
    return ValidationFailure(message ?? 'Attendance request failed.');
  }

  String? _serverMessage(DioException error) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      return data['message'] as String?;
    }
    return null;
  }
}
