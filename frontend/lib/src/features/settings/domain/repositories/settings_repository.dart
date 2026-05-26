import '../../../../core/result/result.dart';
import '../../data/models/role_permission_models.dart';

abstract interface class SettingsRepository {
  Future<Result<RolePermissionMatrixModel>> rolePermissions(String roleId);

  Future<Result<RolePermissionMatrixModel>> updateRolePermissions(
    String roleId,
    List<String> permissionIds,
  );
}
