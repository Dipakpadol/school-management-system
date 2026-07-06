import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../users/data/models/user_models.dart';
import '../models/role_permission_models.dart';

final settingsRemoteDataSourceProvider = Provider<SettingsRemoteDataSource>((
  ref,
) {
  return SettingsRemoteDataSource(ref.watch(apiClientProvider));
});

class SettingsRemoteDataSource {
  const SettingsRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<List<RoleModel>> roles({String? query, String? status}) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.roleManagement,
      queryParameters: {
        if (query != null && query.trim().isNotEmpty) 'query': query.trim(),
        if (status != null && status.isNotEmpty) 'status': status,
      },
    );
    final data = response.data?['data'];
    if (data is List) {
      return data
          .whereType<Map<String, dynamic>>()
          .map(RoleModel.fromJson)
          .toList(growable: false);
    }
    throw const FormatException('Response payload is invalid.');
  }

  Future<RoleModel> createRole(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.roleManagement,
      data: payload,
    );
    return RoleModel.fromJson(_unwrapData(response.data));
  }

  Future<RoleModel> updateRole(
    String roleId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.role(roleId),
      data: payload,
    );
    return RoleModel.fromJson(_unwrapData(response.data));
  }

  Future<void> deleteRole(String roleId) async {
    await _apiClient.delete<Map<String, dynamic>>(ApiPaths.role(roleId));
  }

  Future<List<PermissionOptionModel>> permissions() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.permissions,
    );
    final data = response.data?['data'];
    if (data is List) {
      return data
          .whereType<Map<String, dynamic>>()
          .map(PermissionOptionModel.fromJson)
          .toList(growable: false);
    }
    throw const FormatException('Response payload is invalid.');
  }

  Future<RolePermissionMatrixModel> rolePermissions(String roleId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.rolePermissions(roleId),
    );
    return RolePermissionMatrixModel.fromJson(_unwrapData(response.data));
  }

  Future<RolePermissionMatrixModel> updateRolePermissions(
    String roleId,
    List<String> permissionIds,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.rolePermissions(roleId),
      data: {'permissionIds': permissionIds},
    );
    return RolePermissionMatrixModel.fromJson(_unwrapData(response.data));
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }
}
