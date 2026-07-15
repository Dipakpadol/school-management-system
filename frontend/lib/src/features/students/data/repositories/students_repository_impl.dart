import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/download/file_downloader.dart';
import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/students_repository.dart';
import '../datasources/students_remote_data_source.dart';
import '../models/student_models.dart';

final studentsRepositoryProvider = Provider<StudentsRepository>((ref) {
  return StudentsRepositoryImpl(ref.watch(studentsRemoteDataSourceProvider));
});

class StudentsRepositoryImpl implements StudentsRepository {
  const StudentsRepositoryImpl(this._remoteDataSource);

  final StudentsRemoteDataSource _remoteDataSource;

  @override
  Future<Result<List<StudentSummaryModel>>> students({
    String? query,
    String? status,
    String? academicYearId,
    String? classId,
    String? sectionId,
  }) {
    return _guard(
      () async => (await _remoteDataSource.students(
        query: query,
        status: status,
        academicYearId: academicYearId,
        classId: classId,
        sectionId: sectionId,
      )).content,
    );
  }

  @override
  Future<Result<List<AcademicYearModel>>> academicYears() {
    return _guard(_remoteDataSource.academicYears);
  }

  @override
  Future<Result<List<SchoolClassModel>>> classes(String academicYearId) {
    return _guard(() => _remoteDataSource.classes(academicYearId));
  }

  @override
  Future<Result<List<SectionModel>>> sections(String classId) {
    return _guard(() => _remoteDataSource.sections(classId));
  }

  @override
  Future<Result<ClassSectionTeachersModel>> sectionTeachers(
    String classId,
    String sectionId,
  ) {
    return _guard(() => _remoteDataSource.sectionTeachers(classId, sectionId));
  }

  @override
  Future<Result<StudentProfileModel>> profile(String studentId) {
    return _guard(() => _remoteDataSource.profile(studentId));
  }

  @override
  Future<Result<List<StudentParentModel>>> parents(String studentId) {
    return _guard(() => _remoteDataSource.parents(studentId));
  }

  @override
  Future<Result<StudentProfileModel>> admit(Map<String, dynamic> payload) {
    return _guard(() => _remoteDataSource.admit(payload));
  }

  @override
  Future<Result<StudentProfileModel>> updateProfile(
    String studentId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateProfile(studentId, payload));
  }

  @override
  Future<Result<StudentProfileModel>> updatePhoto(
    String studentId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updatePhoto(studentId, payload));
  }

  @override
  Future<Result<StudentProfileModel>> addParent(
    String studentId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.addParent(studentId, payload));
  }

  @override
  Future<Result<StudentProfileModel>> updateParent(
    String studentId,
    String mappingId,
    Map<String, dynamic> payload,
  ) {
    return _guard(
      () => _remoteDataSource.updateParent(studentId, mappingId, payload),
    );
  }

  @override
  Future<Result<StudentProfileModel>> deleteParent(
    String studentId,
    String mappingId,
  ) {
    return _guard(() => _remoteDataSource.deleteParent(studentId, mappingId));
  }

  @override
  Future<Result<StudentProfileModel>> addDocument(
    String studentId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.addDocument(studentId, payload));
  }

  @override
  Future<Result<StudentProfileModel>> updateDocument(
    String studentId,
    String documentId,
    Map<String, dynamic> payload,
  ) {
    return _guard(
      () => _remoteDataSource.updateDocument(studentId, documentId, payload),
    );
  }

  @override
  Future<Result<StudentProfileModel>> deleteDocument(
    String studentId,
    String documentId,
  ) {
    return _guard(
      () => _remoteDataSource.deleteDocument(studentId, documentId),
    );
  }

  @override
  Future<Result<StudentProfileModel>> assignClass(
    String studentId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.assignClass(studentId, payload));
  }

  @override
  Future<Result<StudentProfileModel>> activate(String studentId) {
    return _guard(() => _remoteDataSource.activate(studentId));
  }

  @override
  Future<Result<StudentProfileModel>> deactivate(String studentId) {
    return _guard(() => _remoteDataSource.deactivate(studentId));
  }

  @override
  Future<Result<void>> delete(String studentId) {
    return _guard(() => _remoteDataSource.delete(studentId));
  }

  @override
  Future<Result<void>> exportExcel() {
    return _guard(() async {
      final bytes = await _remoteDataSource.exportExcel();
      await downloadBytes(
        bytes,
        'students.xlsx',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      );
    });
  }

  @override
  Future<Result<void>> exportCsv() {
    return _guard(() async {
      final bytes = await _remoteDataSource.exportCsv();
      await downloadBytes(bytes, 'students.csv', 'text/csv');
    });
  }

  @override
  Future<Result<void>> downloadTemplate() {
    return _guard(() async {
      final bytes = await _remoteDataSource.template();
      await downloadBytes(
        bytes,
        'student-import-template.xlsx',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      );
    });
  }

  @override
  Future<Result<void>> downloadProfilePdf(String studentId) {
    return _guard(() async {
      final bytes = await _remoteDataSource.profilePdf(studentId);
      await downloadBytes(
        bytes,
        'student-profile-$studentId.pdf',
        'application/pdf',
      );
    });
  }

  @override
  Future<Result<void>> downloadGeneratedDocument(
    String studentId,
    String type,
  ) {
    return _guard(() async {
      final bytes = await _remoteDataSource.generatedDocument(studentId, type);
      await downloadBytes(
        bytes,
        "student-$studentId-${type.toLowerCase().replaceAll('_', '-')}.pdf",
        'application/pdf',
      );
    });
  }

  @override
  Future<Result<StudentImportResultModel>> importExcel(
    List<int> bytes,
    String filename,
  ) {
    return _guard(() => _remoteDataSource.importExcel(bytes, filename));
  }

  @override
  Future<Result<StudentImportResultModel>> importCsv(
    List<int> bytes,
    String filename,
  ) {
    return _guard(() => _remoteDataSource.importCsv(bytes, filename));
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
