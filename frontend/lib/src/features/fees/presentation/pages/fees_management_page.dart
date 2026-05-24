import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/fee_models.dart';
import '../../data/repositories/fees_repository_impl.dart';
import '../controllers/fees_providers.dart';
import '../widgets/fee_widgets.dart';

class FeesManagementPage extends ConsumerWidget {
  const FeesManagementPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return AdminShell(
      title: 'Fees Management',
      activeModuleId: 'fees',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: const DefaultTabController(
        length: 5,
        child: Column(
          children: [
            _FeesHeader(),
            Divider(height: 1),
            Expanded(
              child: TabBarView(
                children: [
                  _CategoryList(),
                  _StructureList(),
                  _AssignmentList(),
                  _ReceiptHistory(),
                  _DefaulterList(),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FeesHeader extends StatelessWidget {
  const _FeesHeader();

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 18, 24, 0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Wrap(
              spacing: 10,
              runSpacing: 10,
              children: [
                AppButton(
                  label: 'Fee structure',
                  icon: Icons.add,
                  onPressed: () => context.go(AppRoutes.newFeeStructure),
                ),
                OutlinedButton.icon(
                  onPressed: () => context.go(AppRoutes.newFeeAssignment),
                  icon: const Icon(Icons.person_add_alt_1_outlined),
                  label: const Text('Assign student'),
                ),
                OutlinedButton.icon(
                  onPressed: () => context.go(AppRoutes.collectFeePayment),
                  icon: const Icon(Icons.point_of_sale_outlined),
                  label: const Text('Collect payment'),
                ),
              ],
            ),
            const SizedBox(height: 14),
            const TabBar(
              isScrollable: true,
              tabs: [
                Tab(icon: Icon(Icons.category_outlined), text: 'Categories'),
                Tab(icon: Icon(Icons.account_tree_outlined), text: 'Structures'),
                Tab(icon: Icon(Icons.assignment_ind_outlined), text: 'Assignments'),
                Tab(icon: Icon(Icons.receipt_long_outlined), text: 'Receipts'),
                Tab(icon: Icon(Icons.warning_amber_outlined), text: 'Defaulters'),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _CategoryList extends ConsumerWidget {
  const _CategoryList();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final categories = ref.watch(feeCategoriesProvider);

    return categories.when(
      data: (items) => _ListSurface(
        title: 'Fee categories',
        action: OutlinedButton.icon(
          onPressed: () => _showCategoryDialog(context, ref),
          icon: const Icon(Icons.add),
          label: const Text('Category'),
        ),
        child: _ResponsiveList(
          emptyMessage: 'No fee categories found.',
          itemCount: items.length,
          itemBuilder: (context, index) {
            final category = items[index];
            return _InfoCard(
              icon: Icons.category_outlined,
              title: category.name,
              subtitle: '${category.code} - sort ${category.sortOrder}',
              trailing: FeeStatusChip(status: category.active ? 'ACTIVE' : 'INACTIVE'),
            );
          },
        ),
      ),
      error: (error, _) => _ErrorBody(
        message: _message(error),
        onRetry: () => ref.invalidate(feeCategoriesProvider),
      ),
      loading: () => const _LoadingBody(),
    );
  }

  Future<void> _showCategoryDialog(BuildContext context, WidgetRef ref) async {
    final code = TextEditingController();
    final name = TextEditingController();
    final description = TextEditingController();
    final formKey = GlobalKey<FormState>();

    await showDialog<void>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Fee category'),
          content: Form(
            key: formKey,
            child: SizedBox(
              width: 420,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextFormField(
                    controller: code,
                    decoration: const InputDecoration(labelText: 'Code'),
                    textCapitalization: TextCapitalization.characters,
                    validator: _required,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: name,
                    decoration: const InputDecoration(labelText: 'Name'),
                    validator: _required,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: description,
                    decoration: const InputDecoration(labelText: 'Description'),
                    maxLines: 2,
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () async {
                if (!(formKey.currentState?.validate() ?? false)) {
                  return;
                }
                final result = await ref.read(feesRepositoryProvider).createCategory({
                  'code': code.text.trim(),
                  'name': name.text.trim(),
                  'description': description.text.trim(),
                  'active': true,
                  'sortOrder': 0,
                });
                if (!context.mounted) {
                  return;
                }
                result.when(
                  success: (_) {
                    ref.invalidate(feeCategoriesProvider);
                    Navigator.of(context).pop();
                  },
                  failure: (failure) => ScaffoldMessenger.of(context)
                      .showSnackBar(SnackBar(content: Text(failure.message))),
                );
              },
              child: const Text('Save'),
            ),
          ],
        );
      },
    );

    code.dispose();
    name.dispose();
    description.dispose();
  }
}

class _StructureList extends ConsumerWidget {
  const _StructureList();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final structures = ref.watch(feeStructuresProvider);

    return structures.when(
      data: (items) => _ListSurface(
        title: 'Fee structures',
        child: _ResponsiveList(
          emptyMessage: 'No fee structures found.',
          itemCount: items.length,
          itemBuilder: (context, index) {
            final structure = items[index];
            return _InfoCard(
              icon: Icons.account_tree_outlined,
              title: structure.name,
              subtitle:
                  '${structure.academicYear} - ${structure.className}${structure.sectionName == null ? '' : ' ${structure.sectionName}'}',
              trailing: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  MoneyText(structure.totalAmount, emphasized: true),
                  const SizedBox(height: 6),
                  FeeStatusChip(status: structure.status),
                ],
              ),
              onTap: () => context.go(AppRoutes.editFeeStructure(structure.id)),
            );
          },
        ),
      ),
      error: (error, _) => _ErrorBody(
        message: _message(error),
        onRetry: () => ref.invalidate(feeStructuresProvider),
      ),
      loading: () => const _LoadingBody(),
    );
  }
}

class _AssignmentList extends ConsumerWidget {
  const _AssignmentList();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final assignments = ref.watch(feeAssignmentsProvider);

    return assignments.when(
      data: (items) => _ListSurface(
        title: 'Student fee assignments',
        child: _ResponsiveList(
          emptyMessage: 'No fee assignments found.',
          itemCount: items.length,
          itemBuilder: (context, index) {
            final assignment = items[index];
            return _InfoCard(
              icon: Icons.assignment_ind_outlined,
              title: assignment.studentName,
              subtitle:
                  '${assignment.admissionNumber} - ${assignment.feeStructureName}',
              trailing: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  MoneyText(assignment.balanceAmount, emphasized: true),
                  const SizedBox(height: 6),
                  FeeStatusChip(status: assignment.status),
                ],
              ),
              onTap: () => context.go(
                '${AppRoutes.collectFeePayment}?assignmentId=${assignment.id}',
              ),
            );
          },
        ),
      ),
      error: (error, _) => _ErrorBody(
        message: _message(error),
        onRetry: () => ref.invalidate(feeAssignmentsProvider),
      ),
      loading: () => const _LoadingBody(),
    );
  }
}

class _ReceiptHistory extends ConsumerStatefulWidget {
  const _ReceiptHistory();

  @override
  ConsumerState<_ReceiptHistory> createState() => _ReceiptHistoryState();
}

class _ReceiptHistoryState extends ConsumerState<_ReceiptHistory> {
  final _receiptController = TextEditingController();
  FeeReceiptModel? _receipt;
  String? _error;
  bool _loading = false;

  @override
  void dispose() {
    _receiptController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _ListSurface(
      title: 'Receipt history',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 520),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _receiptController,
                    decoration: const InputDecoration(
                      labelText: 'Receipt number',
                      prefixIcon: Icon(Icons.receipt_long_outlined),
                    ),
                    onSubmitted: (_) => _lookup(),
                  ),
                ),
                const SizedBox(width: 10),
                SizedBox(
                  height: 48,
                  child: FilledButton.icon(
                    onPressed: _loading ? null : _lookup,
                    icon: _loading
                        ? const SizedBox.square(
                            dimension: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.search),
                    label: const Text('Find'),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          if (_error != null)
            Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          if (_receipt != null)
            _InfoCard(
              icon: Icons.receipt_long_outlined,
              title: _receipt!.receiptNumber,
              subtitle:
                  '${_receipt!.studentName} - ${_receipt!.admissionNumber} - ${dateLabel(_receipt!.receiptDate)}',
              trailing: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  MoneyText(_receipt!.totalAmount, emphasized: true),
                  const SizedBox(height: 6),
                  FeeStatusChip(status: _receipt!.status),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Future<void> _lookup() async {
    final receiptNumber = _receiptController.text.trim();
    if (receiptNumber.isEmpty) {
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
      _receipt = null;
    });
    final result = await ref.read(feesRepositoryProvider).receipt(receiptNumber);
    if (!mounted) {
      return;
    }
    result.when(
      success: (receipt) => setState(() => _receipt = receipt),
      failure: (failure) => setState(() => _error = failure.message),
    );
    setState(() => _loading = false);
  }
}

class _DefaulterList extends ConsumerWidget {
  const _DefaulterList();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final defaulters = ref.watch(feeDefaultersProvider);

    return defaulters.when(
      data: (items) => _ListSurface(
        title: 'Defaulter list',
        child: _ResponsiveList(
          emptyMessage: 'No defaulters found.',
          itemCount: items.length,
          itemBuilder: (context, index) {
            final defaulter = items[index];
            return _InfoCard(
              icon: Icons.warning_amber_outlined,
              title: defaulter.studentName,
              subtitle:
                  '${defaulter.admissionNumber} - ${defaulter.className}${defaulter.sectionName == null ? '' : ' ${defaulter.sectionName}'} - ${defaulter.overdueInstallments} overdue',
              trailing: MoneyText(defaulter.balanceAmount, emphasized: true),
              onTap: () => context.go(
                '${AppRoutes.collectFeePayment}?assignmentId=${defaulter.assignmentId}',
              ),
            );
          },
        ),
      ),
      error: (error, _) => _ErrorBody(
        message: _message(error),
        onRetry: () => ref.invalidate(feeDefaultersProvider),
      ),
      loading: () => const _LoadingBody(),
    );
  }
}

class _ListSurface extends StatelessWidget {
  const _ListSurface({
    required this.title,
    required this.child,
    this.action,
  });

  final String title;
  final Widget child;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                title,
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
            ?action,
          ],
        ),
        const SizedBox(height: 16),
        child,
      ],
    );
  }
}

class _ResponsiveList extends StatelessWidget {
  const _ResponsiveList({
    required this.itemCount,
    required this.itemBuilder,
    required this.emptyMessage,
  });

  final int itemCount;
  final IndexedWidgetBuilder itemBuilder;
  final String emptyMessage;

  @override
  Widget build(BuildContext context) {
    if (itemCount == 0) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(emptyMessage),
        ),
      );
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 1100
            ? 2
            : 1;
        return GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: columns,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            mainAxisExtent: 116,
          ),
          itemCount: itemCount,
          itemBuilder: itemBuilder,
        );
      },
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    this.trailing,
    this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final Widget? trailing;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              SizedBox.square(
                dimension: 44,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: theme.colorScheme.primaryContainer,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(icon, color: theme.colorScheme.onPrimaryContainer),
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      subtitle,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.outline,
                      ),
                    ),
                  ],
                ),
              ),
              if (trailing != null) ...[
                const SizedBox(width: 12),
                trailing!,
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _LoadingBody extends StatelessWidget {
  const _LoadingBody();

  @override
  Widget build(BuildContext context) {
    return const AppLoadingState(label: 'Loading fees');
  }
}

class _ErrorBody extends StatelessWidget {
  const _ErrorBody({
    required this.message,
    required this.onRetry,
  });

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return AppErrorState(message: message, onRetry: onRetry);
  }
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
