import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/network/page_payload.dart';
import '../models/user_models.dart';

final usersRemoteDataSourceProvider = Provider<UsersRemoteDataSource>((ref) {
  return UsersRemoteDataSource(ref.watch(apiClientProvider));
});

class UsersRemoteDataSource {
  const UsersRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<PagePayload<UserModel>> users({
    String? query,
    String? role,
    String? status,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.users,
      queryParameters: {
        if (query != null && query.trim().isNotEmpty) 'query': query.trim(),
        if (role != null && role.isNotEmpty) 'role': role,
        if (status != null && status.isNotEmpty) 'status': status,
        'size': 50,
      },
    );
    return PagePayload.fromJson(_unwrapData(response.data), UserModel.fromJson);
  }

  Future<List<RoleModel>> roles() async {
    final response = await _apiClient.get<Map<String, dynamic>>(ApiPaths.roles);
    final data = response.data?['data'];
    if (data is List) {
      return data
          .whereType<Map<String, dynamic>>()
          .map(RoleModel.fromJson)
          .toList(growable: false);
    }
    throw const FormatException('Response payload is invalid.');
  }

  Future<UserModel> create(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.users,
      data: payload,
    );
    return UserModel.fromJson(_unwrapData(response.data));
  }

  Future<UserModel> update(String userId, Map<String, dynamic> payload) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.user(userId),
      data: payload,
    );
    return UserModel.fromJson(_unwrapData(response.data));
  }

  Future<UserModel> activate(String userId) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.userActivate(userId),
    );
    return UserModel.fromJson(_unwrapData(response.data));
  }

  Future<UserModel> deactivate(String userId) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.userDeactivate(userId),
    );
    return UserModel.fromJson(_unwrapData(response.data));
  }

  Future<void> resetPassword(String userId, String password) async {
    await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.userResetPassword(userId),
      data: {'newPassword': password, 'confirmPassword': password},
    );
  }

  Future<void> delete(String userId) async {
    await _apiClient.delete<Map<String, dynamic>>(ApiPaths.user(userId));
  }

  Future<List<int>> exportExcel() {
    return _apiClient.download(ApiPaths.usersExportExcel);
  }

  Future<List<int>> exportCsv() {
    return _apiClient.download(ApiPaths.usersExportCsv);
  }

  Future<List<int>> template() {
    return _apiClient.download(ApiPaths.usersTemplate);
  }

  Future<void> importExcel(List<int> bytes, String filename) async {
    await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.usersImportExcel,
      data: FormData.fromMap({
        'file': MultipartFile.fromBytes(bytes, filename: filename),
      }),
    );
  }

  Future<void> importCsv(List<int> bytes, String filename) async {
    await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.usersImportCsv,
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
}
