import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../dashboard/presentation/controllers/menu_controller.dart';
import '../../../users/data/models/user_models.dart';
import '../../../users/presentation/controllers/users_providers.dart';
import '../../data/models/role_permission_models.dart';
import '../../data/repositories/settings_repository_impl.dart';
import '../controllers/settings_providers.dart';

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final roles = ref.watch(rolesProvider);

    return AdminShell(
      title: 'Settings',
      activeModuleId: 'settings',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: roles.when(
        data: (items) => _SettingsContent(roles: items),
        error: (error, _) => AppErrorState(
          message: error.toString().replaceFirst('Exception: ', ''),
          onRetry: () => ref.invalidate(rolesProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading settings'),
      ),
    );
  }
}

class _SettingsContent extends ConsumerWidget {
  const _SettingsContent({required this.roles});

  final List<RoleModel> roles;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedRoleId = ref.watch(selectedSettingsRoleIdProvider);
    final selectedRoleExists = roles.any((role) => role.id == selectedRoleId);
    final effectiveRoleId = selectedRoleExists
        ? selectedRoleId
        : (roles.isEmpty ? null : roles.first.id);

    if (selectedRoleId != effectiveRoleId && effectiveRoleId != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ref.read(selectedSettingsRoleIdProvider.notifier).set(effectiveRoleId);
      });
    }

    return Column(
      children: [
        _SettingsHeader(
          roles: roles,
          selectedRoleId: effectiveRoleId,
          onRoleChanged: (roleId) {
            ref.read(selectedSettingsRoleIdProvider.notifier).set(roleId);
          },
          onRefresh: () {
            ref.invalidate(rolesProvider);
            if (effectiveRoleId != null) {
              ref.invalidate(rolePermissionMatrixProvider(effectiveRoleId));
            }
          },
        ),
        const Divider(height: 1),
        Expanded(
          child: effectiveRoleId == null
              ? const Center(child: Text('No roles available.'))
              : _RolePermissionPanel(roleId: effectiveRoleId),
        ),
      ],
    );
  }
}

class _SettingsHeader extends StatelessWidget {
  const _SettingsHeader({
    required this.roles,
    required this.selectedRoleId,
    required this.onRoleChanged,
    required this.onRefresh,
  });

  final List<RoleModel> roles;
  final String? selectedRoleId;
  final ValueChanged<String?> onRoleChanged;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 18, 24, 18),
        child: Wrap(
          spacing: 12,
          runSpacing: 12,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            SizedBox(
              width: 360,
              child: DropdownButtonFormField<String>(
                initialValue: selectedRoleId,
                decoration: const InputDecoration(
                  labelText: 'Role',
                  prefixIcon: Icon(Icons.admin_panel_settings_outlined),
                ),
                items: [
                  for (final role in roles)
                    DropdownMenuItem(
                      value: role.id,
                      child: Text(role.displayName),
                    ),
                ],
                onChanged: roles.isEmpty ? null : onRoleChanged,
              ),
            ),
            OutlinedButton.icon(
              onPressed: onRefresh,
              icon: const Icon(Icons.refresh),
              label: const Text('Refresh'),
            ),
          ],
        ),
      ),
    );
  }
}

class _RolePermissionPanel extends ConsumerWidget {
  const _RolePermissionPanel({required this.roleId});

  final String roleId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final matrix = ref.watch(rolePermissionMatrixProvider(roleId));

    return matrix.when(
      data: (data) => _RolePermissionEditor(matrix: data),
      error: (error, _) => AppErrorState(
        message: error.toString().replaceFirst('Exception: ', ''),
        onRetry: () => ref.invalidate(rolePermissionMatrixProvider(roleId)),
      ),
      loading: () => const AppLoadingState(label: 'Loading permissions'),
    );
  }
}

class _RolePermissionEditor extends ConsumerStatefulWidget {
  const _RolePermissionEditor({required this.matrix});

  final RolePermissionMatrixModel matrix;

  @override
  ConsumerState<_RolePermissionEditor> createState() =>
      _RolePermissionEditorState();
}

class _RolePermissionEditorState extends ConsumerState<_RolePermissionEditor> {
  final _searchController = TextEditingController();
  late Set<String> _selectedIds;
  String _query = '';
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _selectedIds = _assignedIds(widget.matrix);
  }

  @override
  void didUpdateWidget(covariant _RolePermissionEditor oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.matrix.roleId != widget.matrix.roleId ||
        oldWidget.matrix.permissions != widget.matrix.permissions) {
      _selectedIds = _assignedIds(widget.matrix);
      _query = '';
      _searchController.clear();
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final permissions = _filteredPermissions(widget.matrix.permissions, _query);
    final assignedCount = _selectedIds.length;

    return Column(
      children: [
        Material(
          color: const Color(0xFFF8FAFC),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 18, 24, 18),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.matrix.displayName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '$assignedCount of ${widget.matrix.permissions.length} permissions selected',
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.outline,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                FilledButton.icon(
                  onPressed: _saving ? null : _save,
                  icon: _saving
                      ? const SizedBox.square(
                          dimension: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.save_outlined),
                  label: const Text('Save'),
                ),
              ],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(24, 18, 24, 0),
          child: TextField(
            controller: _searchController,
            decoration: const InputDecoration(
              labelText: 'Search permissions',
              prefixIcon: Icon(Icons.search),
            ),
            onChanged: (value) {
              setState(() {
                _query = value.trim();
              });
            },
          ),
        ),
        Expanded(
          child: permissions.isEmpty
              ? const Center(child: Text('No permissions found.'))
              : LayoutBuilder(
                  builder: (context, constraints) {
                    if (constraints.maxWidth >= 880) {
                      return _PermissionTable(
                        permissions: permissions,
                        selectedIds: _selectedIds,
                        onChanged: _toggle,
                      );
                    }
                    return _PermissionList(
                      permissions: permissions,
                      selectedIds: _selectedIds,
                      onChanged: _toggle,
                    );
                  },
                ),
        ),
      ],
    );
  }

  Future<void> _save() async {
    setState(() => _saving = true);
    final roleId = widget.matrix.roleId;
    final result = await ref
        .read(settingsRepositoryProvider)
        .updateRolePermissions(roleId, _selectedIds.toList(growable: false));

    if (!mounted) {
      return;
    }

    setState(() => _saving = false);
    result.when(
      success: (matrix) {
        setState(() {
          _selectedIds = _assignedIds(matrix);
        });
        ref.invalidate(rolePermissionMatrixProvider(roleId));
        ref.invalidate(currentMenuProvider);
        _snack(context, 'Role permissions updated.');
      },
      failure: (failure) => _snack(context, failure.message),
    );
  }

  void _toggle(String permissionId, bool value) {
    setState(() {
      if (value) {
        _selectedIds.add(permissionId);
      } else {
        _selectedIds.remove(permissionId);
      }
    });
  }
}

class _PermissionTable extends StatelessWidget {
  const _PermissionTable({
    required this.permissions,
    required this.selectedIds,
    required this.onChanged,
  });

  final List<PermissionOptionModel> permissions;
  final Set<String> selectedIds;
  final void Function(String permissionId, bool value) onChanged;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: SizedBox(
        width: double.infinity,
        child: DataTable(
          headingRowHeight: 44,
          dataRowMinHeight: 58,
          dataRowMaxHeight: 72,
          columns: const [
            DataColumn(label: Text('Allow')),
            DataColumn(label: Text('Permission')),
            DataColumn(label: Text('Module')),
            DataColumn(label: Text('Description')),
          ],
          rows: [
            for (final permission in permissions)
              DataRow(
                cells: [
                  DataCell(
                    Checkbox(
                      value: selectedIds.contains(permission.id),
                      onChanged: (value) {
                        onChanged(permission.id, value ?? false);
                      },
                    ),
                  ),
                  DataCell(_PermissionName(permission: permission)),
                  DataCell(Text(_moduleName(permission.code))),
                  DataCell(
                    SizedBox(
                      width: 360,
                      child: Text(
                        permission.description?.trim().isNotEmpty ?? false
                            ? permission.description!
                            : 'No description',
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}

class _PermissionList extends StatelessWidget {
  const _PermissionList({
    required this.permissions,
    required this.selectedIds,
    required this.onChanged,
  });

  final List<PermissionOptionModel> permissions;
  final Set<String> selectedIds;
  final void Function(String permissionId, bool value) onChanged;

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      padding: const EdgeInsets.all(24),
      itemCount: permissions.length,
      separatorBuilder: (_, _) => const SizedBox(height: 10),
      itemBuilder: (context, index) {
        final permission = permissions[index];
        return Card(
          child: CheckboxListTile(
            value: selectedIds.contains(permission.id),
            onChanged: (value) => onChanged(permission.id, value ?? false),
            title: _PermissionName(permission: permission),
            subtitle: Text(
              permission.description?.trim().isNotEmpty ?? false
                  ? permission.description!
                  : _moduleName(permission.code),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            controlAffinity: ListTileControlAffinity.leading,
          ),
        );
      },
    );
  }
}

class _PermissionName extends StatelessWidget {
  const _PermissionName({required this.permission});

  final PermissionOptionModel permission;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          permission.name,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.bodyMedium?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          permission.code,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.primary,
          ),
        ),
      ],
    );
  }
}

Set<String> _assignedIds(RolePermissionMatrixModel matrix) {
  return matrix.permissions
      .where((permission) => permission.assigned)
      .map((permission) => permission.id)
      .toSet();
}

List<PermissionOptionModel> _filteredPermissions(
  List<PermissionOptionModel> permissions,
  String query,
) {
  if (query.isEmpty) {
    return permissions;
  }
  final normalized = query.toLowerCase();
  return permissions.where((permission) {
    return permission.code.toLowerCase().contains(normalized) ||
        permission.name.toLowerCase().contains(normalized) ||
        (permission.description ?? '').toLowerCase().contains(normalized);
  }).toList(growable: false);
}

String _moduleName(String permissionCode) {
  final prefix = permissionCode.split('_').first;
  return prefix
      .toLowerCase()
      .split('-')
      .where((word) => word.isNotEmpty)
      .map((word) => '${word[0].toUpperCase()}${word.substring(1)}')
      .join(' ');
}

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}
