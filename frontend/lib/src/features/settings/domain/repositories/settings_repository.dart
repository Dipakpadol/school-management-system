import '../../../../core/result/result.dart';
import '../../../users/data/models/user_models.dart';
import '../../data/models/role_permission_models.dart';
import '../../data/models/settings_models.dart';

abstract interface class SettingsRepository {
  Future<Result<ApplicationSettingsModel>> appSettings();

  Future<Result<ApplicationSettingsModel>> updateAppSettings(
    ApplicationSettingsModel settings,
  );

  Future<Result<List<RoleModel>>> roles({String? query, String? status});

  Future<Result<RoleModel>> createRole(Map<String, dynamic> payload);

  Future<Result<RoleModel>> updateRole(
    String roleId,
    Map<String, dynamic> payload,
  );

  Future<Result<void>> deleteRole(String roleId);

  Future<Result<List<PermissionOptionModel>>> permissions({
    String? query,
    String? moduleName,
    String? status,
  });

  Future<Result<PermissionOptionModel>> createPermission(
    Map<String, dynamic> payload,
  );

  Future<Result<PermissionOptionModel>> updatePermission(
    String permissionId,
    Map<String, dynamic> payload,
  );

  Future<Result<void>> deletePermission(String permissionId);

  Future<Result<RolePermissionMatrixModel>> rolePermissions(String roleId);

  Future<Result<RolePermissionMatrixModel>> updateRolePermissions(
    String roleId,
    List<String> permissionIds,
  );
}
