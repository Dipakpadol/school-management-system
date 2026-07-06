import '../../../../core/result/result.dart';
import '../../../users/data/models/user_models.dart';
import '../../data/models/role_permission_models.dart';

abstract interface class SettingsRepository {
  Future<Result<List<RoleModel>>> roles({String? query, String? status});

  Future<Result<RoleModel>> createRole(Map<String, dynamic> payload);

  Future<Result<RoleModel>> updateRole(
    String roleId,
    Map<String, dynamic> payload,
  );

  Future<Result<void>> deleteRole(String roleId);

  Future<Result<List<PermissionOptionModel>>> permissions();

  Future<Result<RolePermissionMatrixModel>> rolePermissions(String roleId);

  Future<Result<RolePermissionMatrixModel>> updateRolePermissions(
    String roleId,
    List<String> permissionIds,
  );
}
