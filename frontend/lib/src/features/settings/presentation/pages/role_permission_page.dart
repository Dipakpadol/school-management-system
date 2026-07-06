import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../dashboard/presentation/controllers/menu_controller.dart';
import '../../../users/data/models/user_models.dart';
import '../../../users/presentation/controllers/users_providers.dart';
import '../../data/models/role_permission_models.dart';
import '../../data/repositories/settings_repository_impl.dart';
import '../controllers/settings_providers.dart';

class RolePermissionPage extends ConsumerStatefulWidget {
  const RolePermissionPage({super.key});

  @override
  ConsumerState<RolePermissionPage> createState() => _RolePermissionPageState();
}

class _RolePermissionPageState extends ConsumerState<RolePermissionPage> {
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final roles = ref.watch(roleManagementRolesProvider);
    final permissions = ref.watch(permissionsProvider);

    return AdminShell(
      title: 'Role & Permission',
      activeModuleId: 'roles',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: Column(
        children: [
          _RoleHeader(
            searchController: _searchController,
            permissions: permissions,
            onAdd: (items) => _showRoleForm(context, ref, permissions: items),
            onSearch: () {
              ref
                  .read(roleManagementSearchQueryProvider.notifier)
                  .set(_searchController.text.trim());
            },
          ),
          const Divider(height: 1),
          Expanded(
            child: roles.when(
              data: (items) =>
                  _RoleList(roles: items, permissions: permissions),
              error: (error, _) => AppErrorState(
                message: _message(error),
                onRetry: () => ref.invalidate(roleManagementRolesProvider),
              ),
              loading: () => const AppLoadingState(label: 'Loading roles'),
            ),
          ),
        ],
      ),
    );
  }
}

class _RoleHeader extends ConsumerWidget {
  const _RoleHeader({
    required this.searchController,
    required this.permissions,
    required this.onAdd,
    required this.onSearch,
  });

  final TextEditingController searchController;
  final AsyncValue<List<PermissionOptionModel>> permissions;
  final ValueChanged<List<PermissionOptionModel>> onAdd;
  final VoidCallback onSearch;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedStatus = ref.watch(roleManagementStatusFilterProvider);

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
              width: 320,
              child: TextField(
                controller: searchController,
                decoration: const InputDecoration(
                  labelText: 'Search roles',
                  prefixIcon: Icon(Icons.search),
                ),
                onSubmitted: (_) => onSearch(),
              ),
            ),
            SizedBox(
              width: 190,
              child: DropdownButtonFormField<String?>(
                initialValue: selectedStatus,
                decoration: const InputDecoration(labelText: 'Status'),
                items: const [
                  DropdownMenuItem(value: null, child: Text('All statuses')),
                  DropdownMenuItem(value: 'ACTIVE', child: Text('Active')),
                  DropdownMenuItem(value: 'INACTIVE', child: Text('Inactive')),
                ],
                onChanged: (value) {
                  ref
                      .read(roleManagementStatusFilterProvider.notifier)
                      .set(value);
                },
              ),
            ),
            OutlinedButton.icon(
              onPressed: onSearch,
              icon: const Icon(Icons.tune_outlined),
              label: const Text('Apply'),
            ),
            AppButton(
              label: 'Add role',
              icon: Icons.add_moderator_outlined,
              onPressed: permissions.maybeWhen(
                data: (items) =>
                    () => onAdd(items),
                orElse: () => null,
              ),
            ),
            OutlinedButton.icon(
              onPressed: () {
                ref.invalidate(roleManagementRolesProvider);
                ref.invalidate(permissionsProvider);
              },
              icon: const Icon(Icons.refresh),
              label: const Text('Refresh'),
            ),
          ],
        ),
      ),
    );
  }
}

class _RoleList extends ConsumerWidget {
  const _RoleList({required this.roles, required this.permissions});

  final List<RoleModel> roles;
  final AsyncValue<List<PermissionOptionModel>> permissions;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (roles.isEmpty) {
      return const Center(child: Text('No roles found.'));
    }

    final canEditSystemRoles =
        ref.watch(authControllerProvider).user?.hasRole('SUPER_ADMIN') ?? false;

    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 1180 ? 2 : 1;
        return GridView.builder(
          padding: const EdgeInsets.all(24),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: columns,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            mainAxisExtent: 174,
          ),
          itemCount: roles.length,
          itemBuilder: (context, index) {
            return _RoleCard(
              role: roles[index],
              canEditSystemRoles: canEditSystemRoles,
              permissions: permissions,
            );
          },
        );
      },
    );
  }
}

class _RoleCard extends ConsumerWidget {
  const _RoleCard({
    required this.role,
    required this.canEditSystemRoles,
    required this.permissions,
  });

  final RoleModel role;
  final bool canEditSystemRoles;
  final AsyncValue<List<PermissionOptionModel>> permissions;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final canEdit = !role.systemRole || canEditSystemRoles;
    final canDelete = !role.systemRole;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: theme.colorScheme.primaryContainer,
              foregroundColor: theme.colorScheme.onPrimaryContainer,
              child: const Icon(Icons.admin_panel_settings_outlined),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          role.displayName,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      _StatusChip(status: role.status),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    role.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.primary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    role.description?.trim().isNotEmpty ?? false
                        ? role.description!
                        : 'No description',
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.outline,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 8,
                    runSpacing: 6,
                    children: [
                      _SoftChip(
                        label:
                            '${role.permissions.length} permission${role.permissions.length == 1 ? '' : 's'}',
                      ),
                      if (role.systemRole)
                        const _SoftChip(label: 'System protected'),
                    ],
                  ),
                ],
              ),
            ),
            PopupMenuButton<String>(
              tooltip: 'Role actions',
              itemBuilder: (context) => [
                const PopupMenuItem(value: 'view', child: Text('View')),
                PopupMenuItem(
                  value: 'edit',
                  enabled: canEdit,
                  child: const Text('Edit'),
                ),
                PopupMenuItem(
                  value: 'toggle',
                  enabled: canEdit,
                  child: Text(
                    role.status == 'ACTIVE' ? 'Deactivate' : 'Activate',
                  ),
                ),
                PopupMenuItem(
                  value: 'delete',
                  enabled: canDelete,
                  child: const Text('Delete'),
                ),
              ],
              onSelected: (value) =>
                  _handleRoleAction(context, ref, role, value, permissions),
            ),
          ],
        ),
      ),
    );
  }
}

Future<void> _handleRoleAction(
  BuildContext context,
  WidgetRef ref,
  RoleModel role,
  String action,
  AsyncValue<List<PermissionOptionModel>> permissions,
) async {
  if (action == 'view') {
    await _showRoleDetails(context, role);
    return;
  }

  if (action == 'edit') {
    final items = permissions.maybeWhen(
      data: (items) => items,
      orElse: () => const <PermissionOptionModel>[],
    );
    if (items.isEmpty) {
      _snack(context, 'Permissions are still loading.');
      return;
    }
    await _showRoleForm(context, ref, role: role, permissions: items);
    return;
  }

  if (action == 'delete') {
    final confirmed = await _confirm(context, 'Delete ${role.displayName}?');
    if (!confirmed) {
      return;
    }
    final result = await ref
        .read(settingsRepositoryProvider)
        .deleteRole(role.id);
    if (!context.mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(roleManagementRolesProvider);
        ref.invalidate(rolesProvider);
        ref.invalidate(currentMenuProvider);
        _snack(context, 'Role deleted.');
      },
      failure: (failure) => _snack(context, failure.message),
    );
    return;
  }

  final result = await ref
      .read(settingsRepositoryProvider)
      .updateRole(
        role.id,
        _rolePayload(
          roleName: role.name,
          displayName: role.displayName,
          description: role.description,
          status: role.status == 'ACTIVE' ? 'INACTIVE' : 'ACTIVE',
          permissionIds: role.permissions.map((item) => item.id).toList(),
        ),
      );
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      ref.invalidate(roleManagementRolesProvider);
      ref.invalidate(rolesProvider);
      ref.invalidate(currentMenuProvider);
      _snack(
        context,
        role.status == 'ACTIVE' ? 'Role deactivated.' : 'Role activated.',
      );
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _showRoleForm(
  BuildContext context,
  WidgetRef ref, {
  RoleModel? role,
  required List<PermissionOptionModel> permissions,
}) async {
  final roleName = TextEditingController(text: role?.name ?? '');
  final displayName = TextEditingController(text: role?.displayName ?? '');
  final description = TextEditingController(text: role?.description ?? '');
  final formKey = GlobalKey<FormState>();
  String status = role?.status ?? 'ACTIVE';
  Set<String> selectedPermissionIds =
      role?.permissions.map((item) => item.id).toSet() ?? <String>{};
  bool saving = false;
  final pageContext = context;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return StatefulBuilder(
        builder: (context, setState) {
          return AlertDialog(
            title: Text(role == null ? 'Add role' : 'Edit role'),
            content: Form(
              key: formKey,
              child: SizedBox(
                width: 640,
                child: SingleChildScrollView(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      TextFormField(
                        controller: roleName,
                        enabled: !(role?.systemRole ?? false),
                        decoration: const InputDecoration(
                          labelText: 'Role name',
                          prefixIcon: Icon(Icons.badge_outlined),
                        ),
                        validator: _required,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: displayName,
                        decoration: const InputDecoration(
                          labelText: 'Display name',
                        ),
                        validator: _required,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: description,
                        decoration: const InputDecoration(
                          labelText: 'Description',
                        ),
                        maxLines: 3,
                      ),
                      const SizedBox(height: 12),
                      DropdownButtonFormField<String>(
                        initialValue: status,
                        decoration: const InputDecoration(labelText: 'Status'),
                        items: const [
                          DropdownMenuItem(
                            value: 'ACTIVE',
                            child: Text('Active'),
                          ),
                          DropdownMenuItem(
                            value: 'INACTIVE',
                            child: Text('Inactive'),
                          ),
                        ],
                        onChanged: (value) {
                          status = value ?? status;
                        },
                      ),
                      const SizedBox(height: 16),
                      Align(
                        alignment: Alignment.centerLeft,
                        child: Text(
                          'Permissions',
                          style: Theme.of(context).textTheme.titleSmall
                              ?.copyWith(fontWeight: FontWeight.w800),
                        ),
                      ),
                      const SizedBox(height: 8),
                      if (selectedPermissionIds.isEmpty)
                        Align(
                          alignment: Alignment.centerLeft,
                          child: Text(
                            'Select at least one permission.',
                            style: Theme.of(context).textTheme.bodySmall
                                ?.copyWith(color: themeError(context)),
                          ),
                        ),
                      SizedBox(
                        height: 300,
                        child: permissions.isEmpty
                            ? const Center(child: Text('No permissions found.'))
                            : ListView.builder(
                                itemCount: permissions.length,
                                itemBuilder: (context, index) {
                                  final permission = permissions[index];
                                  return CheckboxListTile(
                                    value: selectedPermissionIds.contains(
                                      permission.id,
                                    ),
                                    onChanged: (value) {
                                      setState(() {
                                        if (value ?? false) {
                                          selectedPermissionIds.add(
                                            permission.id,
                                          );
                                        } else {
                                          selectedPermissionIds.remove(
                                            permission.id,
                                          );
                                        }
                                      });
                                    },
                                    title: Text(permission.name),
                                    subtitle: Text(permission.code),
                                    controlAffinity:
                                        ListTileControlAffinity.leading,
                                  );
                                },
                              ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            actions: [
              TextButton(
                onPressed: saving ? null : () => Navigator.of(context).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton.icon(
                onPressed: saving
                    ? null
                    : () async {
                        if (!(formKey.currentState?.validate() ?? false)) {
                          return;
                        }
                        if (selectedPermissionIds.isEmpty) {
                          setState(() {});
                          return;
                        }
                        setState(() => saving = true);
                        final payload = _rolePayload(
                          roleName: roleName.text.trim(),
                          displayName: displayName.text.trim(),
                          description: description.text,
                          status: status,
                          permissionIds: selectedPermissionIds.toList(
                            growable: false,
                          ),
                        );
                        final repository = ref.read(settingsRepositoryProvider);
                        final result = role == null
                            ? await repository.createRole(payload)
                            : await repository.updateRole(role.id, payload);
                        if (!dialogContext.mounted) {
                          return;
                        }
                        setState(() => saving = false);
                        result.when(
                          success: (_) {
                            ref.invalidate(roleManagementRolesProvider);
                            ref.invalidate(rolesProvider);
                            ref.invalidate(currentMenuProvider);
                            Navigator.of(dialogContext).pop();
                            _snack(
                              pageContext,
                              role == null ? 'Role created.' : 'Role updated.',
                            );
                          },
                          failure: (failure) =>
                              _snack(pageContext, failure.message),
                        );
                      },
                icon: saving
                    ? const SizedBox.square(
                        dimension: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.save_outlined),
                label: const Text('Save'),
              ),
            ],
          );
        },
      );
    },
  );

  roleName.dispose();
  displayName.dispose();
  description.dispose();
}

Future<void> _showRoleDetails(BuildContext context, RoleModel role) {
  return showDialog<void>(
    context: context,
    builder: (context) {
      return AlertDialog(
        title: Text(role.displayName),
        content: SizedBox(
          width: 560,
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                _DetailRow(label: 'Role name', value: role.name),
                _DetailRow(label: 'Status', value: _statusLabel(role.status)),
                _DetailRow(
                  label: 'Protected',
                  value: role.systemRole ? 'Yes' : 'No',
                ),
                _DetailRow(
                  label: 'Description',
                  value: role.description?.trim().isNotEmpty ?? false
                      ? role.description!
                      : 'No description',
                ),
                const SizedBox(height: 12),
                Text(
                  'Permissions',
                  style: Theme.of(
                    context,
                  ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    for (final permission in role.permissions)
                      Chip(label: Text(permission.code)),
                    if (role.permissions.isEmpty)
                      const Text('No permissions assigned.'),
                  ],
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Close'),
          ),
        ],
      );
    },
  );
}

Map<String, dynamic> _rolePayload({
  required String roleName,
  required String displayName,
  required String? description,
  required String status,
  required List<String> permissionIds,
}) {
  return {
    'roleName': roleName,
    'displayName': displayName,
    'description': _blankToNull(description ?? ''),
    'status': status,
    'permissionIds': permissionIds,
  };
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(
              label,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.outline,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final active = status == 'ACTIVE';
    final color = active ? const Color(0xFF16A34A) : const Color(0xFF64748B);

    return Container(
      height: 28,
      padding: const EdgeInsets.symmetric(horizontal: 10),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(14),
      ),
      alignment: Alignment.center,
      child: Text(
        _statusLabel(status),
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: color,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _SoftChip extends StatelessWidget {
  const _SoftChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 28,
      padding: const EdgeInsets.symmetric(horizontal: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFF1F5F9),
        borderRadius: BorderRadius.circular(14),
      ),
      alignment: Alignment.center,
      child: Text(
        label,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: Theme.of(context).textTheme.labelSmall,
      ),
    );
  }
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}

String _statusLabel(String status) {
  return status == 'ACTIVE' ? 'Active' : 'Inactive';
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _blankToNull(String value) {
  return value.trim().isEmpty ? null : value.trim();
}

Color themeError(BuildContext context) {
  return Theme.of(context).colorScheme.error;
}

Future<bool> _confirm(BuildContext context, String message) async {
  final result = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('Confirm action'),
      content: Text(message),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () => Navigator.of(context).pop(true),
          child: const Text('Confirm'),
        ),
      ],
    ),
  );
  return result ?? false;
}

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}
