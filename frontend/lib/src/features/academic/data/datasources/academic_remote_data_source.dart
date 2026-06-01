import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../models/academic_models.dart';

final academicRemoteDataSourceProvider = Provider<AcademicRemoteDataSource>((
  ref,
) {
  return AcademicRemoteDataSource(ref.watch(apiClientProvider));
});

class AcademicRemoteDataSource {
  const AcademicRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<List<AcademicYearModel>> years() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.academicYears,
    );
    return _unwrapList(response.data, AcademicYearModel.fromJson);
  }

  Future<AcademicYearModel> createYear(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.academicYears,
      data: payload,
    );
    return AcademicYearModel.fromJson(_unwrapData(response.data));
  }

  Future<AcademicYearModel> updateYear(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.academicYear(id),
      data: payload,
    );
    return AcademicYearModel.fromJson(_unwrapData(response.data));
  }

  Future<void> deleteYear(String id) async {
    await _apiClient.delete<Map<String, dynamic>>(ApiPaths.academicYear(id));
  }

  Future<List<AcademicClassModel>> classes(String academicYearId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.academicYearClasses(academicYearId),
    );
    return _unwrapList(response.data, AcademicClassModel.fromJson);
  }

  Future<AcademicClassModel> createClass(
    String academicYearId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.academicYearClasses(academicYearId),
      data: payload,
    );
    return AcademicClassModel.fromJson(_unwrapData(response.data));
  }

  Future<AcademicClassModel> updateClass(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.schoolClass(id),
      data: payload,
    );
    return AcademicClassModel.fromJson(_unwrapData(response.data));
  }

  Future<void> deleteClass(String id) async {
    await _apiClient.delete<Map<String, dynamic>>(ApiPaths.schoolClass(id));
  }

  Future<List<AcademicDivisionModel>> divisions(String classId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.classDivisions(classId),
    );
    return _unwrapList(response.data, AcademicDivisionModel.fromJson);
  }

  Future<AcademicDivisionModel> createDivision(
    String classId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.classDivisions(classId),
      data: payload,
    );
    return AcademicDivisionModel.fromJson(_unwrapData(response.data));
  }

  Future<AcademicDivisionModel> updateDivision(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.division(id),
      data: payload,
    );
    return AcademicDivisionModel.fromJson(_unwrapData(response.data));
  }

  Future<void> deleteDivision(String id) async {
    await _apiClient.delete<Map<String, dynamic>>(ApiPaths.division(id));
  }

  Future<List<AcademicTeacherModel>> teachers() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.teachers,
    );
    return _unwrapList(response.data, AcademicTeacherModel.fromJson);
  }

  Future<DivisionSubjectModel> addDivisionSubject(
    String divisionId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.divisionSubjects(divisionId),
      data: payload,
    );
    return DivisionSubjectModel.fromJson(_unwrapData(response.data));
  }

  Future<DivisionSubjectModel> updateDivisionSubject(
    String divisionId,
    String divisionSubjectId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.divisionSubject(divisionId, divisionSubjectId),
      data: payload,
    );
    return DivisionSubjectModel.fromJson(_unwrapData(response.data));
  }

  Future<void> deleteDivisionSubject(
    String divisionId,
    String divisionSubjectId,
  ) async {
    await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.divisionSubject(divisionId, divisionSubjectId),
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
