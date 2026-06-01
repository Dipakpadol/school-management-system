import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../academic/data/models/academic_models.dart';
import '../models/attendance_models.dart';

final attendanceRemoteDataSourceProvider =
    Provider<AttendanceRemoteDataSource>((ref) {
  return AttendanceRemoteDataSource(ref.watch(apiClientProvider));
});

class AttendanceRemoteDataSource {
  const AttendanceRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<List<AcademicYearModel>> years() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.attendanceAcademicYears,
    );
    return _unwrapList(response.data, AcademicYearModel.fromJson);
  }

  Future<List<AcademicClassModel>> classes(String academicYearId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.attendanceClasses(academicYearId),
    );
    return _unwrapList(response.data, AcademicClassModel.fromJson);
  }

  Future<List<AcademicDivisionModel>> sections(String classId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.attendanceSections(classId),
    );
    return _unwrapList(response.data, AcademicDivisionModel.fromJson);
  }

  Future<List<AttendanceStudentModel>> students({
    required String academicYearId,
    required String classId,
    required String sectionId,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.attendanceStudents,
      queryParameters: {
        'academicYearId': academicYearId,
        'classId': classId,
        'sectionId': sectionId,
      },
    );
    return _unwrapList(response.data, AttendanceStudentModel.fromJson);
  }

  Future<DailyAttendanceModel> daily({
    required String academicYearId,
    required String classId,
    required String sectionId,
    required String date,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.attendanceDaily,
      queryParameters: {
        'academicYearId': academicYearId,
        'classId': classId,
        'sectionId': sectionId,
        'date': date,
      },
    );
    return DailyAttendanceModel.fromJson(_unwrapData(response.data));
  }

  Future<void> saveDaily(Map<String, dynamic> payload) async {
    await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.attendanceDaily,
      data: payload,
    );
  }

  Future<List<int>> export(Map<String, dynamic> query) {
    return _apiClient.download(
      ApiPaths.attendanceExport,
      queryParameters: query,
    );
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Attendance response is invalid.');
  }

  List<T> _unwrapList<T>(
    Map<String, dynamic>? body,
    T Function(Map<String, dynamic>) mapper,
  ) {
    final data = body?['data'];
    if (data is List) {
      return data.whereType<Map<String, dynamic>>().map(mapper).toList();
    }
    throw const FormatException('Attendance response is invalid.');
  }
}
