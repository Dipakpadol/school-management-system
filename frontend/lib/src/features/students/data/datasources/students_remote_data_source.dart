import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/network/page_payload.dart';
import '../models/student_models.dart';

final studentsRemoteDataSourceProvider = Provider<StudentsRemoteDataSource>((
  ref,
) {
  return StudentsRemoteDataSource(ref.watch(apiClientProvider));
});

class StudentsRemoteDataSource {
  const StudentsRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<PagePayload<StudentSummaryModel>> students({
    String? query,
    String? status,
    String? academicYearId,
    String? classId,
    String? sectionId,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.students,
      queryParameters: {
        if (query != null && query.trim().isNotEmpty) 'query': query.trim(),
        if (status != null && status.isNotEmpty) 'status': status,
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
        if (classId != null && classId.isNotEmpty) 'classId': classId,
        if (sectionId != null && sectionId.isNotEmpty) 'sectionId': sectionId,
        'size': 50,
      },
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      StudentSummaryModel.fromJson,
    );
  }

  Future<StudentProfileModel> profile(String studentId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.studentProfile(studentId),
    );
    return StudentProfileModel.fromJson(_unwrapData(response.data));
  }

  Future<List<AcademicYearModel>> academicYears() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.academicYears,
    );
    return _unwrapList(response.data, AcademicYearModel.fromJson);
  }

  Future<List<SchoolClassModel>> classes(String academicYearId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.academicYearClasses(academicYearId),
    );
    return _unwrapList(response.data, SchoolClassModel.fromJson);
  }

  Future<List<SectionModel>> sections(String classId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.classSections(classId),
    );
    return _unwrapList(response.data, SectionModel.fromJson);
  }

  Future<ClassSectionTeachersModel> sectionTeachers(
    String classId,
    String sectionId,
  ) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.classSectionTeachers(classId, sectionId),
    );
    return ClassSectionTeachersModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentProfileModel> admit(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.studentAdmissions,
      data: payload,
    );
    return StudentProfileModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentProfileModel> updateProfile(
    String studentId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.studentProfile(studentId),
      data: payload,
    );
    return StudentProfileModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentProfileModel> activate(String studentId) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.studentActivate(studentId),
    );
    return StudentProfileModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentProfileModel> deactivate(String studentId) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.studentDeactivate(studentId),
    );
    return StudentProfileModel.fromJson(_unwrapData(response.data));
  }

  Future<void> delete(String studentId) async {
    await _apiClient.delete<Map<String, dynamic>>(ApiPaths.student(studentId));
  }

  Future<List<int>> exportExcel() {
    return _apiClient.download(ApiPaths.studentsExportExcel);
  }

  Future<List<int>> exportCsv() {
    return _apiClient.download(ApiPaths.studentsExportCsv);
  }

  Future<List<int>> template() {
    return _apiClient.download(ApiPaths.studentsTemplate);
  }

  Future<List<int>> profilePdf(String studentId) {
    return _apiClient.download(ApiPaths.studentPdf(studentId));
  }

  Future<void> importExcel(List<int> bytes, String filename) async {
    await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.studentsImportExcel,
      data: FormData.fromMap({
        'file': MultipartFile.fromBytes(bytes, filename: filename),
      }),
    );
  }

  Future<void> importCsv(List<int> bytes, String filename) async {
    await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.studentsImportCsv,
      data: FormData.fromMap({
        'file': MultipartFile.fromBytes(bytes, filename: filename),
      }),
    );
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }

  List<T> _unwrapList<T>(
    Map<String, dynamic>? body,
    T Function(Map<String, dynamic>) mapper,
  ) {
    final data = body?['data'];
    if (data is List) {
      return data.whereType<Map<String, dynamic>>().map(mapper).toList();
    }
    throw const FormatException('Response payload is invalid.');
  }
}
