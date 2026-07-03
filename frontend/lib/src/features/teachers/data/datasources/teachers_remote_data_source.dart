import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../students/data/models/student_models.dart';
import '../models/teacher_models.dart';

final teachersRemoteDataSourceProvider = Provider<TeachersRemoteDataSource>((
  ref,
) {
  return TeachersRemoteDataSource(ref.watch(apiClientProvider));
});

class TeachersRemoteDataSource {
  const TeachersRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<List<AcademicYearModel>> academicYears() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.teacherAcademicYears,
    );
    return _unwrapList(response.data, AcademicYearModel.fromJson);
  }

  Future<List<AcademicYearModel>> attendanceAcademicYears() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.teacherAttendanceAcademicYears,
    );
    return _unwrapList(response.data, AcademicYearModel.fromJson);
  }

  Future<List<TeacherModel>> teachers({String? academicYearId}) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.teacherManagement,
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
      },
    );
    return _unwrapList(response.data, TeacherModel.fromJson);
  }

  Future<TeacherModel> createTeacher(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.teacherManagement,
      data: payload,
    );
    return TeacherModel.fromJson(_unwrapData(response.data));
  }

  Future<TeacherModel> updateTeacher(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.teacher(id),
      data: payload,
    );
    return TeacherModel.fromJson(_unwrapData(response.data));
  }

  Future<TeacherModel> deleteTeacher(String id) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.teacher(id),
    );
    return TeacherModel.fromJson(_unwrapData(response.data));
  }

  Future<TeacherProfileModel> profile(
    String id, {
    String? academicYearId,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.teacherProfile(id),
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
      },
    );
    return TeacherProfileModel.fromJson(_unwrapData(response.data));
  }

  Future<List<TeacherAttendanceTeacherModel>> attendanceTeachers(
    String academicYearId,
  ) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.teacherAttendanceTeachers,
      queryParameters: {'academicYearId': academicYearId},
    );
    return _unwrapList(response.data, TeacherAttendanceTeacherModel.fromJson);
  }

  Future<TeacherDailyAttendanceModel> dailyAttendance({
    required String academicYearId,
    required DateTime date,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.teacherAttendanceDaily,
      queryParameters: {
        'academicYearId': academicYearId,
        'date': _dateParam(date),
      },
    );
    return TeacherDailyAttendanceModel.fromJson(_unwrapData(response.data));
  }

  Future<TeacherDailyAttendanceModel> saveDailyAttendance(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.teacherAttendanceDaily,
      data: payload,
    );
    return TeacherDailyAttendanceModel.fromJson(_unwrapData(response.data));
  }

  Future<TeacherAttendanceHistoryModel> attendanceHistory({
    required String teacherId,
    String? academicYearId,
    DateTime? fromDate,
    DateTime? toDate,
    String? status,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.teacherAttendanceHistory(teacherId),
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
        if (fromDate != null) 'fromDate': _dateParam(fromDate),
        if (toDate != null) 'toDate': _dateParam(toDate),
        if (status != null && status.isNotEmpty) 'status': status,
        'size': 50,
        'sort': 'attendanceDate,desc',
      },
    );
    return TeacherAttendanceHistoryModel.fromJson(_unwrapData(response.data));
  }

  Future<TeacherAssignmentModel> createAssignment(
    String teacherId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.teacherAssignments(teacherId),
      data: payload,
    );
    return TeacherAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<TeacherAssignmentModel> updateAssignment(
    String teacherId,
    String assignmentId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.teacherAssignment(teacherId, assignmentId),
      data: payload,
    );
    return TeacherAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<TeacherAssignmentModel> deleteAssignment(
    String teacherId,
    String assignmentId,
  ) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.teacherAssignment(teacherId, assignmentId),
    );
    return TeacherAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<TeacherDocumentModel> createDocument(
    String teacherId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.teacherDocuments(teacherId),
      data: payload,
    );
    return TeacherDocumentModel.fromJson(_unwrapData(response.data));
  }

  Future<TeacherDocumentModel> updateDocument(
    String teacherId,
    String documentId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.teacherDocument(teacherId, documentId),
      data: payload,
    );
    return TeacherDocumentModel.fromJson(_unwrapData(response.data));
  }

  Future<TeacherDocumentModel> deleteDocument(
    String teacherId,
    String documentId,
  ) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.teacherDocument(teacherId, documentId),
    );
    return TeacherDocumentModel.fromJson(_unwrapData(response.data));
  }

  Future<List<SubjectModel>> subjects() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.subjects,
    );
    return _unwrapList(response.data, SubjectModel.fromJson);
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
    if (data is Map<String, dynamic> && data['content'] is List) {
      return (data['content'] as List)
          .whereType<Map<String, dynamic>>()
          .map(mapper)
          .toList();
    }
    throw const FormatException('Response payload is invalid.');
  }

  String _dateParam(DateTime value) {
    return '${value.year.toString().padLeft(4, '0')}-'
        '${value.month.toString().padLeft(2, '0')}-'
        '${value.day.toString().padLeft(2, '0')}';
  }
}
