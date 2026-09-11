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
                  onPressed: () => context.go(AppRoutes.newFeeAssignment),
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

class _StructureList extends ConsumerStatefulWidget {
  const _StructureList();

  @override
  ConsumerState<_StructureList> createState() => _StructureListState();
}

class _StructureListState extends ConsumerState<_StructureList> {
  static const _pageSize = 20;

  FeeStructureFilter _filter = const FeeStructureFilter(size: _pageSize);

  @override
  Widget build(BuildContext context) {
    final structures = ref.watch(feeStructuresPageProvider(_filter));

    return structures.when(
      data: (page) => _ListSurface(
        title: 'Fee structures',
        action: OutlinedButton.icon(
          onPressed: () => context.go(AppRoutes.newFeeStructure),
          icon: const Icon(Icons.add),
          label: const Text('Add fee structure'),
        ),
        child: Column(
          children: [
            _ResponsiveList(
              emptyMessage: 'No fee structures found.',
              itemCount: page.content.length,
              itemBuilder: (context, index) {
                final structure = page.content[index];
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
                              PopupMenuItem(
                                value: 'edit',
                                child: Text('Edit'),
                              ),
                              PopupMenuItem(
                                value: 'delete',
                                child: Text('Delete'),
                              ),
                            ],
                            onSelected: (value) {
                              if (value == 'assign') {
                                context.go(AppRoutes.newFeeAssignment);
                              } else if (value == 'edit') {
                                context.go(
                                  AppRoutes.editFeeStructure(structure.id),
                                );
                              } else {
                                _deleteStructure(context, structure);
                              }
                            },
                          ),
                        ],
                      ),
                      FeeStatusChip(status: structure.status),
                    ],
                  ),
                  onTap: () =>
                      context.go(AppRoutes.editFeeStructure(structure.id)),
                );
              },
            ),
            const SizedBox(height: 8),
            Card(
              child: FeePaginationBar(
                page: page.page,
                size: page.size,
                totalElements: page.totalElements,
                totalPages: page.totalPages,
                onPageChanged: _changePage,
              ),
            ),
          ],
        ),
      ),
      error: (error, _) => _ErrorBody(
        message: _message(error),
        onRetry: () => ref.invalidate(feeStructuresPageProvider(_filter)),
      ),
      loading: () => const _LoadingBody(),
    );
  }

  Future<void> _deleteStructure(
    BuildContext context,
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
        ref.invalidate(feeStructuresPageProvider(_filter));
        _snack(context, 'Fee structure deleted.');
      },
      failure: (failure) => _snack(context, failure.message),
    );
  }

  void _changePage(int page) {
    setState(() => _filter = _filter.copyWith(page: page));
  }
}

class _AssignmentList extends ConsumerStatefulWidget {
  const _AssignmentList();

  @override
  ConsumerState<_AssignmentList> createState() => _AssignmentListState();
}

class _AssignmentListState extends ConsumerState<_AssignmentList> {
  static const _pageSize = 20;

  FeeListFilter _filter = const FeeListFilter(size: _pageSize);
  String? _paymentActionId;
  String? _discountAssignmentId;

  @override
  Widget build(BuildContext context) {
    final assignments = ref.watch(feeAssignmentsPageProvider(_filter));

    return assignments.when(
      data: (page) => _ListSurface(
        title: 'Student fee assignments',
        action: Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            OutlinedButton.icon(
              onPressed: () => context.go(AppRoutes.newFeeAssignment),
              icon: const Icon(Icons.groups_outlined),
              label: const Text('Assign class fee'),
            ),
            OutlinedButton.icon(
              onPressed: page.content.isEmpty
                  ? null
                  : () => context.go(AppRoutes.feePayments),
              icon: const Icon(Icons.point_of_sale_outlined),
              label: const Text('Collect payment'),
            ),
          ],
        ),
        child: page.content.isEmpty
            ? const _AssignmentsEmptyState()
            : Column(
                children: [
                  _ResponsiveList(
                    emptyMessage: 'No fee assignments found.',
                    itemCount: page.content.length,
                    itemBuilder: (context, index) {
                      final assignment = page.content[index];
                      final completedPayments = assignment.payments
                          .where((payment) => payment.status == 'COMPLETED')
                          .toList(growable: false);
                      final payment = completedPayments.isEmpty
                          ? null
                          : completedPayments.last;
                      final busy =
                          _discountAssignmentId == assignment.id ||
                          _paymentActionId == payment?.id;
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
                                    FeeStatusChip(
                                      status: assignment.sourceType,
                                    ),
                                    FeeStatusChip(status: assignment.status),
                                  ],
                                ),
                              ],
                            ),
                            const SizedBox(width: 6),
                            PopupMenuButton<String>(
                              tooltip: 'Payment actions',
                              icon: busy
                                  ? const SizedBox.square(
                                      dimension: 18,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                      ),
                                    )
                                  : const Icon(Icons.more_vert),
                              itemBuilder: (context) => [
                                const PopupMenuItem(
                                  value: 'collect',
                                  child: Text('Collect payment'),
                                ),
                                PopupMenuItem(
                                  value: 'discount',
                                  enabled:
                                      !busy &&
                                      assignment.balanceAmount > 0 &&
                                      assignment.status != 'CANCELLED',
                                  child: const Text('Apply discount'),
                                ),
                                PopupMenuItem(
                                  value: 'reverse',
                                  enabled: !busy && payment != null,
                                  child: const Text('Reverse payment'),
                                ),
                                PopupMenuItem(
                                  value: 'void',
                                  enabled: !busy && payment != null,
                                  child: const Text('Void payment'),
                                ),
                                PopupMenuItem(
                                  value: 'refund',
                                  enabled: !busy && payment != null,
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
                                if (action == 'discount') {
                                  _applyDiscount(assignment);
                                  return;
                                }
                                if (payment == null) {
                                  return;
                                }
                                _runPaymentAction(payment.id, action);
                              },
                            ),
                          ],
                        ),
                        onTap: () => context.go(
                          AppRoutes.collectFeePaymentForAssignment(
                            assignment.id,
                          ),
                        ),
                      );
                    },
                  ),
                  const SizedBox(height: 8),
                  Card(
                    child: FeePaginationBar(
                      page: page.page,
                      size: page.size,
                      totalElements: page.totalElements,
                      totalPages: page.totalPages,
                      onPageChanged: _changePage,
                    ),
                  ),
                ],
              ),
      ),
      error: (error, _) => _ErrorBody(
        message: _message(error),
        onRetry: () => ref.invalidate(feeAssignmentsPageProvider(_filter)),
      ),
      loading: () => const _LoadingBody(),
    );
  }

  Future<void> _runPaymentAction(String paymentId, String action) async {
    if (_paymentActionId != null) {
      return;
    }
    setState(() => _paymentActionId = paymentId);
    await _handlePaymentAction(
      context,
      ref,
      paymentId,
      action,
      onSuccess: _refreshCurrentLists,
    );
    if (mounted) {
      setState(() => _paymentActionId = null);
    }
  }

  Future<void> _applyDiscount(StudentFeeAssignmentModel assignment) async {
    if (_discountAssignmentId != null) {
      return;
    }
    setState(() => _discountAssignmentId = assignment.id);
    final applied = await _showDiscountDialog(context, ref, assignment);
    if (mounted) {
      setState(() => _discountAssignmentId = null);
    }
    if (applied) {
      _refreshCurrentLists();
    }
  }

  void _refreshCurrentLists() {
    ref.invalidate(feeAssignmentsProvider(const FeeListFilter()));
    ref.invalidate(feeAssignmentsPageProvider(_filter));
    ref.invalidate(feeDefaultersProvider(const FeeDefaulterFilter()));
    ref.invalidate(
      feeDefaultersPageProvider(
        const FeeDefaulterFilter(size: _pageSize),
      ),
    );
  }

  void _changePage(int page) {
    setState(() => _filter = _filter.copyWith(page: page));
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

class _DefaulterList extends ConsumerStatefulWidget {
  const _DefaulterList();

  @override
  ConsumerState<_DefaulterList> createState() => _DefaulterListState();
}

class _DefaulterListState extends ConsumerState<_DefaulterList> {
  static const _pageSize = 20;

  FeeDefaulterFilter _filter = const FeeDefaulterFilter(size: _pageSize);

  @override
  Widget build(BuildContext context) {
    final defaulters = ref.watch(feeDefaultersPageProvider(_filter));

    return defaulters.when(
      data: (page) => _ListSurface(
        title: 'Defaulter list',
        child: Column(
          children: [
            _ResponsiveList(
              emptyMessage: 'No defaulters found.',
              itemCount: page.content.length,
              itemBuilder: (context, index) {
                final defaulter = page.content[index];
                return _InfoCard(
                  icon: Icons.warning_amber_outlined,
                  title: defaulter.studentName,
                  subtitle:
                      '${defaulter.admissionNumber} - ${defaulter.academicYear} - ${defaulter.className}${defaulter.sectionName == null ? '' : ' ${defaulter.sectionName}'} - ${defaulter.sourceType}',
                  trailing: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      MoneyText(defaulter.pendingAmount, emphasized: true),
                      const SizedBox(height: 6),
                      FeeStatusChip(
                        status: defaulter.overdueDays > 0 ? 'OVERDUE' : 'DUE',
                      ),
                    ],
                  ),
                  onTap: () => context.go(
                    AppRoutes.collectFeePaymentForAssignment(
                      defaulter.assignmentId,
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 8),
            Card(
              child: FeePaginationBar(
                page: page.page,
                size: page.size,
                totalElements: page.totalElements,
                totalPages: page.totalPages,
                onPageChanged: _changePage,
              ),
            ),
          ],
        ),
      ),
      error: (error, _) => _ErrorBody(
        message: _message(error),
        onRetry: () => ref.invalidate(feeDefaultersPageProvider(_filter)),
      ),
      loading: () => const _LoadingBody(),
    );
  }

  void _changePage(int page) {
    setState(() => _filter = _filter.copyWith(page: page));
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
            if (action != null) action!,
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

String? _blankToNull(String value) {
  final text = value.trim();
  return text.isEmpty ? null : text;
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
  String action, {
  VoidCallback? onSuccess,
}) async {
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
      onSuccess?.call();
      _snack(context, 'Payment action completed.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<bool> _showDiscountDialog(
  BuildContext context,
  WidgetRef ref,
  StudentFeeAssignmentModel assignment,
) async {
  final formKey = GlobalKey<FormState>();
  final value = TextEditingController();
  final reason = TextEditingController();
  final approvedBy = TextEditingController();
  var discountType = 'WAIVER';
  var calculationType = 'FLAT';
  var saving = false;
  var applied = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          return AlertDialog(
            title: Text('Discount / concession - ${assignment.studentName}'),
            content: Form(
              key: formKey,
              child: SizedBox(
                width: 520,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    DropdownButtonFormField<String>(
                      initialValue: discountType,
                      decoration: const InputDecoration(
                        labelText: 'Discount type',
                      ),
                      items: const [
                        DropdownMenuItem(
                          value: 'SCHOLARSHIP',
                          child: Text('Scholarship'),
                        ),
                        DropdownMenuItem(
                          value: 'SIBLING',
                          child: Text('Sibling'),
                        ),
                        DropdownMenuItem(value: 'STAFF', child: Text('Staff')),
                        DropdownMenuItem(
                          value: 'WAIVER',
                          child: Text('Waiver'),
                        ),
                        DropdownMenuItem(
                          value: 'PROMOTIONAL',
                          child: Text('Promotional'),
                        ),
                        DropdownMenuItem(value: 'OTHER', child: Text('Other')),
                      ],
                      onChanged: saving
                          ? null
                          : (value) {
                              if (value != null) {
                                setDialogState(() => discountType = value);
                              }
                            },
                    ),
                    const SizedBox(height: 12),
                    DropdownButtonFormField<String>(
                      initialValue: calculationType,
                      decoration: const InputDecoration(
                        labelText: 'Calculation',
                      ),
                      items: const [
                        DropdownMenuItem(value: 'FLAT', child: Text('Flat')),
                        DropdownMenuItem(
                          value: 'PERCENTAGE',
                          child: Text('Percentage'),
                        ),
                      ],
                      onChanged: saving
                          ? null
                          : (value) {
                              if (value != null) {
                                setDialogState(() => calculationType = value);
                              }
                            },
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: value,
                      enabled: !saving,
                      keyboardType: TextInputType.number,
                      validator: (raw) {
                        final number = double.tryParse(raw?.trim() ?? '');
                        if (number == null || number <= 0) {
                          return 'Enter a value above 0';
                        }
                        if (calculationType == 'PERCENTAGE' && number > 100) {
                          return 'Enter 100 or less';
                        }
                        if (calculationType == 'FLAT' &&
                            number > assignment.balanceAmount) {
                          return 'Cannot exceed outstanding amount';
                        }
                        return null;
                      },
                      decoration: InputDecoration(
                        labelText: calculationType == 'PERCENTAGE'
                            ? 'Percentage'
                            : 'Amount',
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: reason,
                      enabled: !saving,
                      maxLines: 2,
                      validator: _required,
                      decoration: const InputDecoration(labelText: 'Reason'),
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: approvedBy,
                      enabled: !saving,
                      decoration: const InputDecoration(
                        labelText: 'Approved by',
                      ),
                    ),
                  ],
                ),
              ),
            ),
            actions: [
              TextButton(
                onPressed: saving
                    ? null
                    : () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton.icon(
                onPressed: saving
                    ? null
                    : () async {
                        if (!(formKey.currentState?.validate() ?? false)) {
                          return;
                        }
                        setDialogState(() => saving = true);
                        final result = await ref
                            .read(feesRepositoryProvider)
                            .applyDiscount(assignment.id, {
                              'discountType': discountType,
                              'calculationType': calculationType,
                              'value':
                                  double.tryParse(value.text.trim()) ?? 0,
                              'reason': reason.text.trim(),
                              'approvedBy': _blankToNull(approvedBy.text),
                            });
                        if (!dialogContext.mounted) {
                          return;
                        }
                        result.when(
                          success: (_) {
                            applied = true;
                            Navigator.of(dialogContext).pop();
                          },
                          failure: (failure) {
                            setDialogState(() => saving = false);
                            _snack(dialogContext, failure.message);
                          },
                        );
                      },
                icon: saving
                    ? const SizedBox.square(
                        dimension: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.percent_outlined),
                label: const Text('Apply'),
              ),
            ],
          );
        },
      );
    },
  );

  value.dispose();
  reason.dispose();
  approvedBy.dispose();
  if (applied && context.mounted) {
    _snack(context, 'Discount applied.');
  }
  return applied;
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
      ref.invalidate(
        feeStructuresPageProvider(const FeeStructureFilter(size: 20)),
      );
      ref.invalidate(feeAssignmentsProvider(const FeeListFilter()));
      ref.invalidate(feeAssignmentsPageProvider(const FeeListFilter(size: 20)));
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
