import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../../core/result/result.dart';
import '../../../../core/upload/file_picker.dart';
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

class _FeesHeader extends ConsumerWidget {
  const _FeesHeader();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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
                OutlinedButton.icon(
                  onPressed: () => context.go(AppRoutes.feeAssignments),
                  icon: const Icon(Icons.groups_outlined),
                  label: const Text('Assign class fee'),
                ),
                OutlinedButton.icon(
                  onPressed: () => context.go(AppRoutes.hostel),
                  icon: const Icon(Icons.apartment_outlined),
                  label: const Text('Hostel fees'),
                ),
                OutlinedButton.icon(
                  onPressed: () => context.go(AppRoutes.transport),
                  icon: const Icon(Icons.directions_bus_outlined),
                  label: const Text('Transport fees'),
                ),
                OutlinedButton.icon(
                  onPressed: () => context.go(AppRoutes.feePayments),
                  icon: const Icon(Icons.point_of_sale_outlined),
                  label: const Text('Collect payment'),
                ),
                PopupMenuButton<String>(
                  tooltip: 'Export fees',
                  icon: const Icon(Icons.download_outlined),
                  itemBuilder: (context) => const [
                    PopupMenuItem(
                      value: 'structures',
                      child: Text('Export structures'),
                    ),
                    PopupMenuItem(
                      value: 'assignments',
                      child: Text('Export assignments'),
                    ),
                    PopupMenuItem(
                      value: 'defaulters-pdf',
                      child: Text('Defaulters PDF'),
                    ),
                    PopupMenuItem(
                      value: 'collection-pdf',
                      child: Text('Collection PDF'),
                    ),
                  ],
                  onSelected: (value) =>
                      _runFeeDownload(context, switch (value) {
                        'structures' =>
                          ref
                              .read(feesRepositoryProvider)
                              .exportStructuresExcel(),
                        'assignments' =>
                          ref
                              .read(feesRepositoryProvider)
                              .exportAssignmentsExcel(),
                        'defaulters-pdf' =>
                          ref
                              .read(feesRepositoryProvider)
                              .exportDefaulters('pdf'),
                        _ =>
                          ref
                              .read(feesRepositoryProvider)
                              .exportCollection('pdf'),
                      }, 'Fee export downloaded.'),
                ),
                PopupMenuButton<String>(
                  tooltip: 'Download templates',
                  icon: const Icon(Icons.table_view_outlined),
                  itemBuilder: (context) => const [
                    PopupMenuItem(
                      value: 'structure',
                      child: Text('Structure template'),
                    ),
                    PopupMenuItem(
                      value: 'assignment',
                      child: Text('Assignment template'),
                    ),
                  ],
                  onSelected: (value) => _runFeeDownload(
                    context,
                    value == 'structure'
                        ? ref
                              .read(feesRepositoryProvider)
                              .downloadStructureTemplate()
                        : ref
                              .read(feesRepositoryProvider)
                              .downloadAssignmentTemplate(),
                    'Template downloaded.',
                  ),
                ),
                PopupMenuButton<String>(
                  tooltip: 'Import fees',
                  icon: const Icon(Icons.upload_file_outlined),
                  itemBuilder: (context) => const [
                    PopupMenuItem(
                      value: 'structures-excel',
                      child: Text('Import structures Excel'),
                    ),
                    PopupMenuItem(
                      value: 'structures-csv',
                      child: Text('Import structures CSV'),
                    ),
                    PopupMenuItem(
                      value: 'assignments-excel',
                      child: Text('Import assignments Excel'),
                    ),
                    PopupMenuItem(
                      value: 'assignments-csv',
                      child: Text('Import assignments CSV'),
                    ),
                  ],
                  onSelected: (value) =>
                      _runPickedFeeImport(context, ref, value),
                ),
              ],
            ),
            const SizedBox(height: 14),
            const TabBar(
              isScrollable: true,
              tabs: [
                Tab(icon: Icon(Icons.category_outlined), text: 'Categories'),
                Tab(
                  icon: Icon(Icons.account_tree_outlined),
                  text: 'Structures',
                ),
                Tab(
                  icon: Icon(Icons.assignment_ind_outlined),
                  text: 'Assignments',
                ),
                Tab(icon: Icon(Icons.receipt_long_outlined), text: 'Receipts'),
                Tab(
                  icon: Icon(Icons.warning_amber_outlined),
                  text: 'Defaulters',
                ),
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
          label: const Text('Add category'),
        ),
        child: _ResponsiveList(
          emptyMessage: 'No fee categories found.',
          itemCount: items.length,
          itemBuilder: (context, index) {
            final category = items[index];
            return _InfoCard(
              icon: Icons.category_outlined,
              title: category.name,
              subtitle:
                  '${category.code} - sort ${category.sortOrder}${category.isMandatory ? ' - mandatory' : ''}',
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  FeeStatusChip(
                    status: category.active ? 'ACTIVE' : 'INACTIVE',
                  ),
                  PopupMenuButton<String>(
                    tooltip: 'Category actions',
                    icon: const Icon(Icons.more_vert),
                    itemBuilder: (context) => const [
                      PopupMenuItem(value: 'edit', child: Text('Edit')),
                      PopupMenuItem(value: 'delete', child: Text('Delete')),
                    ],
                    onSelected: (value) {
                      if (value == 'edit') {
                        _showCategoryDialog(context, ref, category);
                      } else {
                        _deleteCategory(context, ref, category);
                      }
                    },
                  ),
                ],
              ),
              onTap: () => _showCategoryDialog(context, ref, category),
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

  Future<void> _showCategoryDialog(
    BuildContext context,
    WidgetRef ref, [
    FeeCategoryModel? category,
  ]) async {
    final code = TextEditingController(text: category?.code ?? '');
    final name = TextEditingController(text: category?.name ?? '');
    final description = TextEditingController(
      text: category?.description ?? '',
    );
    final sortOrder = TextEditingController(
      text: (category?.sortOrder ?? 0).toString(),
    );
    final formKey = GlobalKey<FormState>();
    var active = category?.active ?? true;
    var mandatory = category?.isMandatory ?? true;

    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setState) => AlertDialog(
            title: Text(
              category == null ? 'Add fee category' : 'Edit fee category',
            ),
            content: Form(
              key: formKey,
              child: SizedBox(
                width: 460,
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
                      controller: sortOrder,
                      decoration: const InputDecoration(
                        labelText: 'Sort order',
                      ),
                      keyboardType: TextInputType.number,
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: description,
                      decoration: const InputDecoration(
                        labelText: 'Description',
                      ),
                      maxLines: 2,
                    ),
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text('Active'),
                      value: active,
                      onChanged: (value) => setState(() => active = value),
                    ),
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text('Mandatory by default'),
                      value: mandatory,
                      onChanged: (value) => setState(() => mandatory = value),
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
                  final payload = {
                    'code': code.text.trim(),
                    'name': name.text.trim(),
                    'description': description.text.trim(),
                    'active': active,
                    'sortOrder': int.tryParse(sortOrder.text.trim()) ?? 0,
                    'isMandatory': mandatory,
                  };
                  final repository = ref.read(feesRepositoryProvider);
                  final result = category == null
                      ? await repository.createCategory(payload)
                      : await repository.updateCategory(category.id, payload);
                  if (!context.mounted) {
                    return;
                  }
                  result.when(
                    success: (_) {
                      ref.invalidate(feeCategoriesProvider);
                      Navigator.of(context).pop();
                      _snack(context, 'Fee category saved.');
                    },
                    failure: (failure) => _snack(context, failure.message),
                  );
                },
                child: const Text('Save'),
              ),
            ],
          ),
        );
      },
    );

    code.dispose();
    name.dispose();
    description.dispose();
    sortOrder.dispose();
  }

  Future<void> _deleteCategory(
    BuildContext context,
    WidgetRef ref,
    FeeCategoryModel category,
  ) async {
    final confirmed = await _confirm(context, 'Delete ${category.name}?');
    if (!confirmed || !context.mounted) {
      return;
    }
    final result = await ref
        .read(feesRepositoryProvider)
        .deleteCategory(category.id);
    if (!context.mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(feeCategoriesProvider);
        _snack(context, 'Fee category deleted.');
      },
      failure: (failure) => _snack(context, failure.message),
    );
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
        action: OutlinedButton.icon(
          onPressed: () => context.go(AppRoutes.newFeeStructure),
          icon: const Icon(Icons.add),
          label: const Text('Add fee structure'),
        ),
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
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      MoneyText(structure.totalAmount, emphasized: true),
                      PopupMenuButton<String>(
                        tooltip: 'Structure actions',
                        icon: const Icon(Icons.more_vert),
                        itemBuilder: (context) => const [
                          PopupMenuItem(
                            value: 'assign',
                            child: Text('Assign to class'),
                          ),
                          PopupMenuItem(value: 'edit', child: Text('Edit')),
                          PopupMenuItem(value: 'delete', child: Text('Delete')),
                        ],
                        onSelected: (value) {
                          if (value == 'assign') {
                            context.go(AppRoutes.feeAssignments);
                          } else if (value == 'edit') {
                            context.go(
                              AppRoutes.editFeeStructure(structure.id),
                            );
                          } else {
                            _deleteStructure(context, ref, structure);
                          }
                        },
                      ),
                    ],
                  ),
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

  Future<void> _deleteStructure(
    BuildContext context,
    WidgetRef ref,
    FeeStructureModel structure,
  ) async {
    final confirmed = await _confirm(context, 'Delete ${structure.name}?');
    if (!confirmed || !context.mounted) {
      return;
    }
    final result = await ref
        .read(feesRepositoryProvider)
        .deleteStructure(structure.id);
    if (!context.mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(feeStructuresProvider);
        _snack(context, 'Fee structure deleted.');
      },
      failure: (failure) => _snack(context, failure.message),
    );
  }
}

class _AssignmentList extends ConsumerWidget {
  const _AssignmentList();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    const filter = FeeListFilter();
    final assignments = ref.watch(feeAssignmentsProvider(filter));

    return assignments.when(
      data: (items) => _ListSurface(
        title: 'Student fee assignments',
        action: Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            OutlinedButton.icon(
              onPressed: () => context.go(AppRoutes.feeAssignments),
              icon: const Icon(Icons.groups_outlined),
              label: const Text('Assign class fee'),
            ),
            OutlinedButton.icon(
              onPressed: items.isEmpty
                  ? null
                  : () => context.go(AppRoutes.feePayments),
              icon: const Icon(Icons.point_of_sale_outlined),
              label: const Text('Collect payment'),
            ),
          ],
        ),
        child: items.isEmpty
            ? const _AssignmentsEmptyState()
            : _ResponsiveList(
                emptyMessage: 'No fee assignments found.',
                itemCount: items.length,
                itemBuilder: (context, index) {
                  final assignment = items[index];
                  final completedPayments = assignment.payments
                      .where((payment) => payment.status == 'COMPLETED')
                      .toList(growable: false);
                  final payment = completedPayments.isEmpty
                      ? null
                      : completedPayments.last;
                  return _InfoCard(
                    icon: Icons.assignment_ind_outlined,
                    title: assignment.studentName,
                    subtitle:
                        '${assignment.admissionNumber} - '
                        '${_sourceLabel(assignment.sourceType)} - '
                        '${_assignmentContext(assignment)} - '
                        '${assignment.feeStructureName}',
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Column(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            MoneyText(
                              assignment.balanceAmount,
                              emphasized: true,
                            ),
                            const SizedBox(height: 6),
                            Wrap(
                              spacing: 6,
                              runSpacing: 6,
                              alignment: WrapAlignment.end,
                              children: [
                                FeeStatusChip(status: assignment.sourceType),
                                FeeStatusChip(status: assignment.status),
                              ],
                            ),
                          ],
                        ),
                        const SizedBox(width: 6),
                        PopupMenuButton<String>(
                          tooltip: 'Payment actions',
                          icon: const Icon(Icons.more_vert),
                          itemBuilder: (context) => [
                            const PopupMenuItem(
                              value: 'collect',
                              child: Text('Collect payment'),
                            ),
                            PopupMenuItem(
                              value: 'reverse',
                              enabled: payment != null,
                              child: const Text('Reverse payment'),
                            ),
                            PopupMenuItem(
                              value: 'void',
                              enabled: payment != null,
                              child: const Text('Void payment'),
                            ),
                            PopupMenuItem(
                              value: 'refund',
                              enabled: payment != null,
                              child: const Text('Refund payment'),
                            ),
                          ],
                          onSelected: (action) {
                            if (action == 'collect') {
                              context.go(
                                AppRoutes.collectFeePaymentForAssignment(
                                  assignment.id,
                                ),
                              );
                              return;
                            }
                            if (payment == null) {
                              return;
                            }
                            _handlePaymentAction(
                              context,
                              ref,
                              payment.id,
                              action,
                            );
                          },
                        ),
                      ],
                    ),
                    onTap: () => context.go(
                      AppRoutes.collectFeePaymentForAssignment(assignment.id),
                    ),
                  );
                },
              ),
      ),
      error: (error, _) => _ErrorBody(
        message: _message(error),
        onRetry: () => ref.invalidate(feeAssignmentsProvider(filter)),
      ),
      loading: () => const _LoadingBody(),
    );
  }
}

class _AssignmentsEmptyState extends StatelessWidget {
  const _AssignmentsEmptyState();

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Row(
          children: [
            Icon(
              Icons.assignment_late_outlined,
              size: 34,
              color: Theme.of(context).colorScheme.outline,
            ),
            const SizedBox(width: 14),
            const Expanded(
              child: Text(
                'No fee assignments found. Assign a fee structure to a class first, then collect payment from this tab.',
              ),
            ),
          ],
        ),
      ),
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
            Text(
              _error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
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
          if (_receipt != null) ...[
            const SizedBox(height: 10),
            OutlinedButton.icon(
              onPressed: () => _runFeeDownload(
                context,
                ref
                    .read(feesRepositoryProvider)
                    .downloadReceiptPdf(_receipt!.receiptNumber),
                'Receipt PDF downloaded.',
              ),
              icon: const Icon(Icons.picture_as_pdf_outlined),
              label: const Text('Download receipt PDF'),
            ),
          ],
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
    final result = await ref
        .read(feesRepositoryProvider)
        .receipt(receiptNumber);
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
    const filter = FeeDefaulterFilter();
    final defaulters = ref.watch(feeDefaultersProvider(filter));

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
                AppRoutes.collectFeePaymentForAssignment(
                  defaulter.assignmentId,
                ),
              ),
            );
          },
        ),
      ),
      error: (error, _) => _ErrorBody(
        message: _message(error),
        onRetry: () => ref.invalidate(feeDefaultersProvider(filter)),
      ),
      loading: () => const _LoadingBody(),
    );
  }
}

class _ListSurface extends StatelessWidget {
  const _ListSurface({required this.title, required this.child, this.action});

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
                style: Theme.of(
                  context,
                ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
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
        final columns = constraints.maxWidth >= 1100 ? 2 : 1;
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
                  child: Icon(
                    icon,
                    color: theme.colorScheme.onPrimaryContainer,
                  ),
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
              if (trailing != null) ...[const SizedBox(width: 12), trailing!],
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
  const _ErrorBody({required this.message, required this.onRetry});

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

String _sourceLabel(String sourceType) {
  return switch (sourceType) {
    'HOSTEL' => 'Hostel Fees',
    'TRANSPORT' => 'Transport Fees',
    'MANUAL' => 'Manual Fees',
    _ => 'Class Fees',
  };
}

String _assignmentContext(StudentFeeAssignmentModel assignment) {
  final text = switch (assignment.sourceType) {
    'HOSTEL' => [
      if ((assignment.hostelName ?? '').isNotEmpty) assignment.hostelName,
      if ((assignment.hostelRoomNumber ?? '').isNotEmpty)
        'Room ${assignment.hostelRoomNumber}',
      if ((assignment.roomType ?? '').isNotEmpty) assignment.roomType,
    ].whereType<String>().join(' - '),
    'TRANSPORT' => [
      if ((assignment.transportRouteName ?? '').isNotEmpty)
        assignment.transportRouteName,
      if ((assignment.transportPickupPointName ?? '').isNotEmpty)
        assignment.transportPickupPointName,
    ].whereType<String>().join(' - '),
    _ => [
      if (assignment.className.isNotEmpty) assignment.className,
      if ((assignment.sectionName ?? '').isNotEmpty) assignment.sectionName,
    ].whereType<String>().join(' - '),
  };
  return text.isEmpty ? '-' : text;
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}

Future<void> _handlePaymentAction(
  BuildContext context,
  WidgetRef ref,
  String paymentId,
  String action,
) async {
  final confirmed = await _confirm(
    context,
    '${action[0].toUpperCase()}${action.substring(1)} this payment?',
  );
  if (!confirmed) {
    return;
  }
  final repository = ref.read(feesRepositoryProvider);
  final result = switch (action) {
    'reverse' => await repository.reversePayment(paymentId),
    'void' => await repository.voidPayment(paymentId),
    _ => await repository.refundPayment(paymentId),
  };
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      ref.invalidate(feeAssignmentsProvider(const FeeListFilter()));
      ref.invalidate(feeDefaultersProvider(const FeeDefaulterFilter()));
      _snack(context, 'Payment action completed.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _runFeeDownload(
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

Future<void> _runPickedFeeImport(
  BuildContext context,
  WidgetRef ref,
  String value,
) async {
  final csv = value.endsWith('csv');
  final file = await pickUploadFile(
    accept: csv ? '.csv,text/csv' : '.xlsx,.xls',
  );
  if (file == null || !context.mounted) {
    return;
  }
  final repository = ref.read(feesRepositoryProvider);
  final result = switch (value) {
    'structures-csv' => await repository.importStructuresCsv(
      file.bytes,
      file.name,
    ),
    'assignments-excel' => await repository.importAssignmentsExcel(
      file.bytes,
      file.name,
    ),
    'assignments-csv' => await repository.importAssignmentsCsv(
      file.bytes,
      file.name,
    ),
    _ => await repository.importStructuresExcel(file.bytes, file.name),
  };
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      ref.invalidate(feeStructuresProvider);
      ref.invalidate(feeAssignmentsProvider(const FeeListFilter()));
      _snack(context, 'Fee import completed.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
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
