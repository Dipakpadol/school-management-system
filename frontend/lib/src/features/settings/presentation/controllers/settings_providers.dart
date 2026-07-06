import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../../users/data/models/user_models.dart';
import '../../data/models/role_permission_models.dart';
import '../../data/repositories/settings_repository_impl.dart';

final roleManagementSearchQueryProvider =
    NotifierProvider<RoleManagementSearchQueryController, String>(
      RoleManagementSearchQueryController.new,
    );

final roleManagementStatusFilterProvider =
    NotifierProvider<RoleManagementStatusFilterController, String?>(
      RoleManagementStatusFilterController.new,
    );

final roleManagementRolesProvider = FutureProvider<List<RoleModel>>((ref) {
  return _resolve(
    ref
        .watch(settingsRepositoryProvider)
        .roles(
          query: ref.watch(roleManagementSearchQueryProvider),
          status: ref.watch(roleManagementStatusFilterProvider),
        ),
  );
});

final permissionsProvider = FutureProvider<List<PermissionOptionModel>>((ref) {
  return _resolve(ref.watch(settingsRepositoryProvider).permissions());
});

final selectedSettingsRoleIdProvider =
    NotifierProvider<SelectedSettingsRoleController, String?>(
      SelectedSettingsRoleController.new,
    );

final rolePermissionMatrixProvider =
    FutureProvider.family<RolePermissionMatrixModel, String>((ref, roleId) {
      return _resolve(
        ref.watch(settingsRepositoryProvider).rolePermissions(roleId),
      );
    });

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}

class SelectedSettingsRoleController extends Notifier<String?> {
  @override
  String? build() => null;

  void set(String? value) {
    state = value;
  }
}

class RoleManagementSearchQueryController extends Notifier<String> {
  @override
  String build() => '';

  void set(String value) {
    state = value;
  }
}

class RoleManagementStatusFilterController extends Notifier<String?> {
  @override
  String? build() => null;

  void set(String? value) {
    state = value;
  }
}
