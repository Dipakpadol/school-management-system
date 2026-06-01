import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../../academic/data/models/academic_models.dart';
import '../../domain/repositories/exams_repository.dart';
import '../datasources/exams_remote_data_source.dart';
import '../models/exam_models.dart';

final examsRepositoryProvider = Provider<ExamsRepository>((ref) {
  return ExamsRepositoryImpl(ref.watch(examsRemoteDataSourceProvider));
});

class ExamsRepositoryImpl implements ExamsRepository {
  const ExamsRepositoryImpl(this._remote);

  final ExamsRemoteDataSource _remote;

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
  Future<Result<List<ExamSubjectModel>>> subjects(
    String classId,
    String sectionId,
  ) {
    return _guard(() => _remote.subjects(classId, sectionId));
  }

  @override
  Future<Result<List<ExamStudentModel>>> students({
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
  Future<Result<List<ExamTypeModel>>> types() => _guard(_remote.types);

  @override
  Future<Result<void>> saveType(String? id, Map<String, dynamic> payload) {
    return _guard(() => _remote.saveType(id, payload));
  }

  @override
  Future<Result<void>> deleteType(String id) {
    return _guard(() => _remote.deleteType(id));
  }

  @override
  Future<Result<List<ExamScheduleModel>>> schedules({
    String? academicYearId,
    String? classId,
    String? sectionId,
  }) {
    return _guard(
      () => _remote.schedules(
        academicYearId: academicYearId,
        classId: classId,
        sectionId: sectionId,
      ),
    );
  }

  @override
  Future<Result<void>> saveSchedule(String? id, Map<String, dynamic> payload) {
    return _guard(() => _remote.saveSchedule(id, payload));
  }

  @override
  Future<Result<void>> deleteSchedule(String id) {
    return _guard(() => _remote.deleteSchedule(id));
  }

  @override
  Future<Result<MarksEntryModel>> marks(Map<String, dynamic> query) {
    return _guard(() => _remote.marks(query));
  }

  @override
  Future<Result<void>> saveMarks(Map<String, dynamic> payload) {
    return _guard(() => _remote.saveMarks(payload));
  }

  @override
  Future<Result<List<StudentResultModel>>> generateResults(
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remote.generateResults(payload));
  }

  @override
  Future<Result<List<int>>> reportCard(String studentId, String academicYearId) {
    return _guard(() => _remote.reportCard(studentId, academicYearId));
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
      return ValidationFailure(message ?? 'Exam request is invalid.');
    }
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.receiveTimeout) {
      return const NetworkFailure('Unable to reach the server.');
    }
    return ValidationFailure(message ?? 'Exam request failed.');
  }

  String? _serverMessage(DioException error) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      return data['message'] as String?;
    }
    return null;
  }
}
