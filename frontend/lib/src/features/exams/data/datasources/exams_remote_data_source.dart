import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../academic/data/models/academic_models.dart';
import '../models/exam_models.dart';

final examsRemoteDataSourceProvider = Provider<ExamsRemoteDataSource>((ref) {
  return ExamsRemoteDataSource(ref.watch(apiClientProvider));
});

class ExamsRemoteDataSource {
  const ExamsRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<List<AcademicYearModel>> years() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.examAcademicYears,
    );
    return _unwrapList(response.data, AcademicYearModel.fromJson);
  }

  Future<List<AcademicClassModel>> classes(String academicYearId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.examClasses(academicYearId),
    );
    return _unwrapList(response.data, AcademicClassModel.fromJson);
  }

  Future<List<AcademicDivisionModel>> sections(String classId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.examSections(classId),
    );
    return _unwrapList(response.data, AcademicDivisionModel.fromJson);
  }

  Future<List<ExamSubjectModel>> subjects(
    String classId,
    String sectionId,
  ) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.examSubjects(classId, sectionId),
    );
    return _unwrapList(response.data, DivisionSubjectModel.fromJson);
  }

  Future<List<ExamStudentModel>> students({
    required String academicYearId,
    required String classId,
    required String sectionId,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.examStudents,
      queryParameters: {
        'academicYearId': academicYearId,
        'classId': classId,
        'sectionId': sectionId,
      },
    );
    return _unwrapList(response.data, ExamStudentModel.fromJson);
  }

  Future<List<ExamTypeModel>> types() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.examTypes,
    );
    return _unwrapList(response.data, ExamTypeModel.fromJson);
  }

  Future<void> saveType(String? id, Map<String, dynamic> payload) async {
    if (id == null) {
      await _apiClient.post<Map<String, dynamic>>(
        ApiPaths.examTypes,
        data: payload,
      );
      return;
    }
    await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.examType(id),
      data: payload,
    );
  }

  Future<void> deleteType(String id) async {
    await _apiClient.delete<Map<String, dynamic>>(ApiPaths.examType(id));
  }

  Future<List<ExamScheduleModel>> schedules({
    String? academicYearId,
    String? classId,
    String? sectionId,
  }) async {
    final query = <String, dynamic>{};
    if (academicYearId != null) {
      query['academicYearId'] = academicYearId;
    }
    if (classId != null) {
      query['classId'] = classId;
    }
    if (sectionId != null) {
      query['sectionId'] = sectionId;
    }
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.examSchedules,
      queryParameters: query,
    );
    return _unwrapList(response.data, ExamScheduleModel.fromJson);
  }

  Future<void> saveSchedule(String? id, Map<String, dynamic> payload) async {
    if (id == null) {
      await _apiClient.post<Map<String, dynamic>>(
        ApiPaths.examSchedules,
        data: payload,
      );
      return;
    }
    await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.examSchedule(id),
      data: payload,
    );
  }

  Future<void> deleteSchedule(String id) async {
    await _apiClient.delete<Map<String, dynamic>>(ApiPaths.examSchedule(id));
  }

  Future<MarksEntryModel> marks(Map<String, dynamic> query) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.examMarks,
      queryParameters: query,
    );
    return MarksEntryModel.fromJson(_unwrapData(response.data));
  }

  Future<void> saveMarks(Map<String, dynamic> payload) async {
    await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.examMarks,
      data: payload,
    );
  }

  Future<List<StudentResultModel>> generateResults(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.examResultsGenerate,
      data: payload,
    );
    return _unwrapList(response.data, StudentResultModel.fromJson);
  }

  Future<List<int>> reportCard(String studentId, String academicYearId) {
    return _apiClient.download(
      ApiPaths.examReportCard(studentId),
      queryParameters: {'academicYearId': academicYearId},
    );
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Exam response is invalid.');
  }

  List<T> _unwrapList<T>(
    Map<String, dynamic>? body,
    T Function(Map<String, dynamic>) mapper,
  ) {
    final data = body?['data'];
    if (data is List) {
      return data.whereType<Map<String, dynamic>>().map(mapper).toList();
    }
    throw const FormatException('Exam response is invalid.');
  }
}
