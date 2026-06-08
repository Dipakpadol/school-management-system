import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/download/file_downloader.dart';
import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/student_profile_repository.dart';
import '../datasources/student_profile_api_service.dart';
import '../models/student_profile_history_models.dart';

final studentProfileRepositoryProvider = Provider<StudentProfileRepository>((
  ref,
) {
  return StudentProfileRepositoryImpl(
    ref.watch(studentProfileApiServiceProvider),
  );
});

class StudentProfileRepositoryImpl implements StudentProfileRepository {
  const StudentProfileRepositoryImpl(this._apiService);

  final StudentProfileApiService _apiService;

  @override
  Future<Result<StudentAttendanceHistoryModel>> fetchAttendanceHistory({
    required String studentId,
    String? academicYearId,
    DateTime? fromDate,
    DateTime? toDate,
    String? status,
    int page = 0,
    int size = 20,
    String? sort,
  }) {
    return _guard(
      () => _apiService.getStudentAttendanceHistory(
        studentId: studentId,
        academicYearId: academicYearId,
        fromDate: fromDate,
        toDate: toDate,
        status: status,
        page: page,
        size: size,
        sort: sort,
      ),
    );
  }

  @override
  Future<Result<void>> exportAttendanceHistory({
    required String studentId,
    String? academicYearId,
    DateTime? fromDate,
    DateTime? toDate,
    String? status,
  }) {
    return _guard(() async {
      final bytes = await _apiService.exportStudentAttendanceHistory(
        studentId: studentId,
        academicYearId: academicYearId,
        fromDate: fromDate,
        toDate: toDate,
        status: status,
      );
      await downloadBytes(
        bytes,
        'student-attendance-$studentId.csv',
        'text/csv',
      );
    });
  }

  @override
  Future<Result<StudentExamResultsModel>> fetchExamResults({
    required String studentId,
    String? academicYearId,
    String? examTypeId,
    String? examScheduleId,
  }) {
    return _guard(
      () => _apiService.getStudentExamResults(
        studentId: studentId,
        academicYearId: academicYearId,
        examTypeId: examTypeId,
        examScheduleId: examScheduleId,
      ),
    );
  }

  @override
  Future<Result<void>> downloadReportCard({
    required String studentId,
    required String resultId,
    String? academicYearId,
  }) {
    return _guard(() async {
      final bytes = await _apiService.downloadStudentReportCard(
        studentId: studentId,
        resultId: resultId,
        academicYearId: academicYearId,
      );
      await downloadBytes(
        bytes,
        'student-report-card-$studentId.csv',
        'text/csv',
      );
    });
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
    if (statusCode == 400 || statusCode == 409 || statusCode == 422) {
      return ValidationFailure(
        message ?? 'Student profile request is invalid.',
      );
    }
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.receiveTimeout) {
      return const NetworkFailure('Unable to reach the server.');
    }
    return ValidationFailure(message ?? 'Student profile request failed.');
  }

  String? _serverMessage(DioException error) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      return data['message'] as String?;
    }
    return null;
  }
}
