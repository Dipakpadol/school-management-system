import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/network/page_payload.dart';
import '../models/student_models.dart';

final studentsRemoteDataSourceProvider =
    Provider<StudentsRemoteDataSource>((ref) {
  return StudentsRemoteDataSource(ref.watch(apiClientProvider));
});

class StudentsRemoteDataSource {
  const StudentsRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<PagePayload<StudentSummaryModel>> students({
    String? query,
    String? status,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.students,
      queryParameters: {
        if (query != null && query.trim().isNotEmpty) 'query': query.trim(),
        if (status != null && status.isNotEmpty) 'status': status,
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
      ApiPaths.student(studentId),
    );
    return StudentProfileModel.fromJson(_unwrapData(response.data));
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

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }
}
