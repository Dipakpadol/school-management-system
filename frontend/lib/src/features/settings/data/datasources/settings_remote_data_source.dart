import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../models/role_permission_models.dart';

final settingsRemoteDataSourceProvider = Provider<SettingsRemoteDataSource>((
  ref,
) {
  return SettingsRemoteDataSource(ref.watch(apiClientProvider));
});

class SettingsRemoteDataSource {
  const SettingsRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

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
