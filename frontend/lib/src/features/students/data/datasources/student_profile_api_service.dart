import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../models/student_profile_history_models.dart';

final studentProfileApiServiceProvider = Provider<StudentProfileApiService>((
  ref,
) {
  return StudentProfileApiService(ref.watch(apiClientProvider));
});

class StudentProfileApiService {
  const StudentProfileApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<StudentAttendanceHistoryModel> getStudentAttendanceHistory({
    required String studentId,
    String? academicYearId,
    DateTime? fromDate,
    DateTime? toDate,
    String? status,
    int page = 0,
    int size = 20,
    String? sort,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.studentAttendanceHistory(studentId),
      queryParameters: {
        if (_hasText(academicYearId)) 'academicYearId': academicYearId,
        if (fromDate != null) 'fromDate': _date(fromDate),
        if (toDate != null) 'toDate': _date(toDate),
        if (_hasText(status)) 'status': status,
        'page': page,
        'size': size,
        if (_hasText(sort)) 'sort': sort,
      },
    );
    return StudentAttendanceHistoryModel.fromJson(_unwrapData(response.data));
  }

  Future<List<int>> exportStudentAttendanceHistory({
    required String studentId,
    String? academicYearId,
    DateTime? fromDate,
    DateTime? toDate,
    String? status,
  }) {
    return _apiClient.download(
      ApiPaths.studentAttendanceHistoryExport(studentId),
      queryParameters: {
        if (_hasText(academicYearId)) 'academicYearId': academicYearId,
        if (fromDate != null) 'fromDate': _date(fromDate),
        if (toDate != null) 'toDate': _date(toDate),
        if (_hasText(status)) 'status': status,
      },
    );
  }

  Future<StudentExamResultsModel> getStudentExamResults({
    required String studentId,
    String? academicYearId,
    String? examTypeId,
    String? examScheduleId,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.studentExamResults(studentId),
      queryParameters: {
        if (_hasText(academicYearId)) 'academicYearId': academicYearId,
        if (_hasText(examTypeId)) 'examTypeId': examTypeId,
        if (_hasText(examScheduleId)) 'examScheduleId': examScheduleId,
      },
    );
    return StudentExamResultsModel.fromJson(_unwrapData(response.data));
  }

  Future<List<int>> downloadStudentReportCard({
    required String studentId,
    required String resultId,
    String? academicYearId,
  }) {
    return _apiClient.download(
      ApiPaths.studentExamReportCard(studentId, resultId),
      queryParameters: {
        if (_hasText(academicYearId)) 'academicYearId': academicYearId,
      },
    );
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Student profile response is invalid.');
  }
}

bool _hasText(String? value) => value != null && value.trim().isNotEmpty;

String _date(DateTime date) {
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}
