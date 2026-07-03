import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../../students/data/models/student_models.dart';
import '../../domain/repositories/teachers_repository.dart';
import '../datasources/teachers_remote_data_source.dart';
import '../models/teacher_models.dart';

final teachersRepositoryProvider = Provider<TeachersRepository>((ref) {
  return TeachersRepositoryImpl(ref.watch(teachersRemoteDataSourceProvider));
});

class TeachersRepositoryImpl implements TeachersRepository {
  const TeachersRepositoryImpl(this._remote);

  final TeachersRemoteDataSource _remote;

  @override
  Future<Result<List<AcademicYearModel>>> academicYears() =>
      _guard(_remote.academicYears);

  @override
  Future<Result<List<AcademicYearModel>>> attendanceAcademicYears() =>
      _guard(_remote.attendanceAcademicYears);

  @override
  Future<Result<List<TeacherModel>>> teachers({String? academicYearId}) =>
      _guard(() => _remote.teachers(academicYearId: academicYearId));

  @override
  Future<Result<TeacherModel>> createTeacher(Map<String, dynamic> payload) =>
      _guard(() => _remote.createTeacher(payload));

  @override
  Future<Result<TeacherModel>> updateTeacher(
    String id,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateTeacher(id, payload));

  @override
  Future<Result<TeacherModel>> deleteTeacher(String id) =>
      _guard(() => _remote.deleteTeacher(id));

  @override
  Future<Result<TeacherProfileModel>> profile(
    String id, {
    String? academicYearId,
  }) => _guard(() => _remote.profile(id, academicYearId: academicYearId));

  @override
  Future<Result<List<TeacherAttendanceTeacherModel>>> attendanceTeachers(
    String academicYearId,
  ) => _guard(() => _remote.attendanceTeachers(academicYearId));

  @override
  Future<Result<TeacherDailyAttendanceModel>> dailyAttendance({
    required String academicYearId,
    required DateTime date,
  }) => _guard(
    () => _remote.dailyAttendance(academicYearId: academicYearId, date: date),
  );

  @override
  Future<Result<TeacherDailyAttendanceModel>> saveDailyAttendance(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.saveDailyAttendance(payload));

  @override
  Future<Result<TeacherAttendanceHistoryModel>> attendanceHistory({
    required String teacherId,
    String? academicYearId,
    DateTime? fromDate,
    DateTime? toDate,
    String? status,
  }) => _guard(
    () => _remote.attendanceHistory(
      teacherId: teacherId,
      academicYearId: academicYearId,
      fromDate: fromDate,
      toDate: toDate,
      status: status,
    ),
  );

  @override
  Future<Result<TeacherAssignmentModel>> createAssignment(
    String teacherId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createAssignment(teacherId, payload));

  @override
  Future<Result<TeacherAssignmentModel>> updateAssignment(
    String teacherId,
    String assignmentId,
    Map<String, dynamic> payload,
  ) => _guard(
    () => _remote.updateAssignment(teacherId, assignmentId, payload),
  );

  @override
  Future<Result<TeacherAssignmentModel>> deleteAssignment(
    String teacherId,
    String assignmentId,
  ) => _guard(() => _remote.deleteAssignment(teacherId, assignmentId));

  @override
  Future<Result<TeacherDocumentModel>> createDocument(
    String teacherId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createDocument(teacherId, payload));

  @override
  Future<Result<TeacherDocumentModel>> updateDocument(
    String teacherId,
    String documentId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateDocument(teacherId, documentId, payload));

  @override
  Future<Result<TeacherDocumentModel>> deleteDocument(
    String teacherId,
    String documentId,
  ) => _guard(() => _remote.deleteDocument(teacherId, documentId));

  @override
  Future<Result<List<SubjectModel>>> subjects() => _guard(_remote.subjects);

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
