import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../../core/result/result.dart';
import '../../../../core/upload/file_picker.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/user_models.dart';
import '../../data/repositories/users_repository_impl.dart';
import '../controllers/users_providers.dart';

class UsersPage extends ConsumerStatefulWidget {
  const UsersPage({super.key});

  @override
  ConsumerState<UsersPage> createState() => _UsersPageState();
}

class _UsersPageState extends ConsumerState<UsersPage> {
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final users = ref.watch(usersProvider);

    return AdminShell(
      title: 'User Management',
      activeModuleId: 'users',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: Column(
        children: [
          _UserHeader(
            searchController: _searchController,
            onAdd: () => _showUserDialog(context, ref),
            onSearch: () {
              ref
                  .read(userSearchQueryProvider.notifier)
                  .set(_searchController.text.trim());
            },
          ),
          const Divider(height: 1),
          Expanded(
            child: users.when(
              data: (items) => _UserList(users: items),
              error: (error, _) => AppErrorState(
                message: _message(error),
                onRetry: () => ref.invalidate(usersProvider),
              ),
              loading: () => const AppLoadingState(label: 'Loading users'),
            ),
          ),
        ],
      ),
    );
  }
}

class _UserHeader extends ConsumerWidget {
  const _UserHeader({
    required this.searchController,
    required this.onAdd,
    required this.onSearch,
  });

  final TextEditingController searchController;
  final VoidCallback onAdd;
  final VoidCallback onSearch;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final roles = ref
        .watch(rolesProvider)
        .maybeWhen(data: (roles) => roles, orElse: () => const <RoleModel>[]);
    final selectedRole = ref.watch(userRoleFilterProvider);
    final selectedStatus = ref.watch(userStatusFilterProvider);

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
                  labelText: 'Search users',
                  prefixIcon: Icon(Icons.search),
                ),
                onSubmitted: (_) => onSearch(),
              ),
            ),
            SizedBox(
              width: 220,
              child: DropdownButtonFormField<String?>(
                initialValue: selectedRole,
                decoration: const InputDecoration(
                  labelText: 'Role',
                  prefixIcon: Icon(Icons.admin_panel_settings_outlined),
                ),
                items: [
                  const DropdownMenuItem(value: null, child: Text('All roles')),
                  for (final role in roles)
                    DropdownMenuItem(
                      value: role.name,
                      child: Text(role.displayName),
                    ),
                ],
                onChanged: (value) {
                  ref.read(userRoleFilterProvider.notifier).set(value);
                },
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
                  DropdownMenuItem(value: 'DISABLED', child: Text('Disabled')),
                  DropdownMenuItem(value: 'LOCKED', child: Text('Locked')),
                ],
                onChanged: (value) {
                  ref.read(userStatusFilterProvider.notifier).set(value);
                },
              ),
            ),
            OutlinedButton.icon(
              onPressed: onSearch,
              icon: const Icon(Icons.tune_outlined),
              label: const Text('Apply'),
            ),
            AppButton(
              label: 'Create user',
              icon: Icons.person_add_alt_1_outlined,
              onPressed: onAdd,
            ),
            OutlinedButton.icon(
              onPressed: () => _runUserDownload(
                context,
                ref.read(usersRepositoryProvider).exportExcel(),
                'User export downloaded.',
              ),
              icon: const Icon(Icons.download_outlined),
              label: const Text('Export'),
            ),
            OutlinedButton.icon(
              onPressed: () => _runUserDownload(
                context,
                ref.read(usersRepositoryProvider).downloadTemplate(),
                'User template downloaded.',
              ),
              icon: const Icon(Icons.table_view_outlined),
              label: const Text('Template'),
            ),
            PopupMenuButton<String>(
              tooltip: 'Import users',
              icon: const Icon(Icons.upload_file_outlined),
              itemBuilder: (context) => const [
                PopupMenuItem(value: 'excel', child: Text('Import Excel')),
                PopupMenuItem(value: 'csv', child: Text('Import CSV')),
              ],
              onSelected: (format) => _runPickedUserImport(
                context,
                ref,
                format,
              ),
            ),
            OutlinedButton.icon(
              onPressed: () => ref.invalidate(usersProvider),
              icon: const Icon(Icons.refresh),
              label: const Text('Refresh'),
            ),
          ],
        ),
      ),
    );
  }
}

class _UserList extends ConsumerWidget {
  const _UserList({required this.users});

  final List<UserModel> users;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (users.isEmpty) {
      return const Center(child: Text('No users found.'));
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 1180 ? 2 : 1;
        return GridView.builder(
          padding: const EdgeInsets.all(24),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: columns,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            mainAxisExtent: 150,
          ),
          itemCount: users.length,
          itemBuilder: (context, index) {
            return _UserCard(user: users[index]);
          },
        );
      },
    );
  }
}

class _UserCard extends ConsumerWidget {
  const _UserCard({required this.user});

  final UserModel user;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final roleText = user.roles.map((role) => role.displayName).join(', ');

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: theme.colorScheme.primaryContainer,
              foregroundColor: theme.colorScheme.onPrimaryContainer,
              child: Text(_initials(user.displayName)),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    user.displayName,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    user.email,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.outline,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 6,
                    children: [
                      _StatusChip(status: user.status),
                      _SoftChip(label: roleText.isEmpty ? 'No role' : roleText),
                    ],
                  ),
                ],
              ),
            ),
            PopupMenuButton<String>(
              tooltip: 'User actions',
              itemBuilder: (context) => [
                const PopupMenuItem(value: 'edit', child: Text('Edit')),
                PopupMenuItem(
                  value: user.status == 'ACTIVE' ? 'deactivate' : 'activate',
                  child: Text(
                    user.status == 'ACTIVE' ? 'Deactivate' : 'Activate',
                  ),
                ),
                const PopupMenuItem(
                  value: 'reset',
                  child: Text('Reset password'),
                ),
                const PopupMenuItem(value: 'delete', child: Text('Delete')),
              ],
              onSelected: (value) => _handleAction(context, ref, user, value),
            ),
          ],
        ),
      ),
    );
  }
}

Future<void> _handleAction(
  BuildContext context,
  WidgetRef ref,
  UserModel user,
  String action,
) async {
  final repository = ref.read(usersRepositoryProvider);
  if (action == 'edit') {
    await _showUserDialog(context, ref, user: user);
    return;
  }
  if (action == 'reset') {
    await _showResetPasswordDialog(context, ref, user);
    return;
  }
  if (action == 'delete') {
    final confirmed = await _confirm(context, 'Delete ${user.displayName}?');
    if (!confirmed) {
      return;
    }
    final result = await repository.delete(user.id);
    if (!context.mounted) {
      return;
    }
    result.when(
      success: (_) => ref.invalidate(usersProvider),
      failure: (failure) => _snack(context, failure.message),
    );
    return;
  }

  final result = action == 'activate'
      ? await repository.activate(user.id)
      : await repository.deactivate(user.id);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) => ref.invalidate(usersProvider),
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _showUserDialog(
  BuildContext context,
  WidgetRef ref, {
  UserModel? user,
}) async {
  final roles = ref
      .read(rolesProvider)
      .maybeWhen(data: (roles) => roles, orElse: () => const <RoleModel>[]);
  final firstName = TextEditingController(text: user?.firstName ?? '');
  final lastName = TextEditingController(text: user?.lastName ?? '');
  final email = TextEditingController(text: user?.email ?? '');
  final username = TextEditingController(text: user?.username ?? '');
  final phone = TextEditingController(text: user?.phoneNumber ?? '');
  final password = TextEditingController(text: 'Demo@12345678');
  final formKey = GlobalKey<FormState>();
  final fallbackRole = roles.isEmpty ? 'ADMIN' : roles.first.name;
  String role = user != null && user.roles.isNotEmpty
      ? user.roles.first.name
      : fallbackRole;

  await showDialog<void>(
    context: context,
    builder: (context) {
      return AlertDialog(
        title: Text(user == null ? 'Create user' : 'Edit user'),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 560,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextFormField(
                    controller: firstName,
                    decoration: const InputDecoration(labelText: 'First name'),
                    validator: _required,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: lastName,
                    decoration: const InputDecoration(labelText: 'Last name'),
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: email,
                    decoration: const InputDecoration(labelText: 'Email'),
                    validator: _required,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: username,
                    decoration: const InputDecoration(labelText: 'Username'),
                    validator: _required,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: phone,
                    decoration: const InputDecoration(labelText: 'Mobile'),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    initialValue: role,
                    decoration: const InputDecoration(labelText: 'Role'),
                    items: [
                      if (roles.isEmpty)
                        const DropdownMenuItem(
                          value: 'ADMIN',
                          child: Text('Admin'),
                        )
                      else
                        for (final item in roles)
                          DropdownMenuItem(
                            value: item.name,
                            child: Text(item.displayName),
                          ),
                    ],
                    onChanged: (value) => role = value ?? role,
                  ),
                  if (user == null) ...[
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: password,
                      decoration: const InputDecoration(labelText: 'Password'),
                      validator: _required,
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: () async {
              if (!(formKey.currentState?.validate() ?? false)) {
                return;
              }
              final payload = {
                'email': email.text.trim(),
                'username': username.text.trim(),
                'firstName': firstName.text.trim(),
                'lastName': _blankToNull(lastName.text),
                'phoneNumber': _blankToNull(phone.text),
                'roles': [role],
                if (user == null) 'password': password.text.trim(),
              };
              final repository = ref.read(usersRepositoryProvider);
              final result = user == null
                  ? await repository.create(payload)
                  : await repository.update(user.id, payload);
              if (!context.mounted) {
                return;
              }
              result.when(
                success: (_) {
                  ref.invalidate(usersProvider);
                  Navigator.of(context).pop();
                },
                failure: (failure) => _snack(context, failure.message),
              );
            },
            icon: const Icon(Icons.save_outlined),
            label: const Text('Save'),
          ),
        ],
      );
    },
  );

  firstName.dispose();
  lastName.dispose();
  email.dispose();
  username.dispose();
  phone.dispose();
  password.dispose();
}

Future<void> _showResetPasswordDialog(
  BuildContext context,
  WidgetRef ref,
  UserModel user,
) async {
  final password = TextEditingController(text: 'Demo@12345678');
  final formKey = GlobalKey<FormState>();

  await showDialog<void>(
    context: context,
    builder: (context) {
      return AlertDialog(
        title: Text('Reset ${user.displayName} password'),
        content: Form(
          key: formKey,
          child: TextFormField(
            controller: password,
            decoration: const InputDecoration(labelText: 'New password'),
            validator: _required,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: () async {
              if (!(formKey.currentState?.validate() ?? false)) {
                return;
              }
              final result = await ref
                  .read(usersRepositoryProvider)
                  .resetPassword(user.id, password.text.trim());
              if (!context.mounted) {
                return;
              }
              result.when(
                success: (_) {
                  Navigator.of(context).pop();
                  _snack(context, 'Password reset successfully.');
                },
                failure: (failure) => _snack(context, failure.message),
              );
            },
            icon: const Icon(Icons.lock_reset_outlined),
            label: const Text('Reset'),
          ),
        ],
      );
    },
  );

  password.dispose();
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
        status,
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

String _initials(String value) {
  final words = value.trim().split(RegExp(r'\s+'));
  if (words.isEmpty || words.first.isEmpty) {
    return 'U';
  }
  if (words.length == 1) {
    return words.first.substring(0, 1).toUpperCase();
  }
  return '${words.first.substring(0, 1)}${words.last.substring(0, 1)}'
      .toUpperCase();
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
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

Future<void> _runUserDownload(
  BuildContext context,
  Future<Result<void>> action,
  String successMessage,
) async {
  final result = await action;
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) => _snack(context, successMessage),
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _runPickedUserImport(
  BuildContext context,
  WidgetRef ref,
  String format,
) async {
  final file = await pickUploadFile(
    accept: format == 'csv' ? '.csv,text/csv' : '.xlsx,.xls',
  );
  if (file == null || !context.mounted) {
    return;
  }
  final repository = ref.read(usersRepositoryProvider);
  final result = format == 'csv'
      ? await repository.importCsv(file.bytes, file.name)
      : await repository.importExcel(file.bytes, file.name);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      _snack(context, 'User import completed.');
      ref.invalidate(usersProvider);
    },
    failure: (failure) => _snack(context, failure.message),
  );
}
