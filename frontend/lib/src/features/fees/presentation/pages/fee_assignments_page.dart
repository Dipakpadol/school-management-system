import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_data_table.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../students/data/models/student_models.dart';
import '../../../students/presentation/controllers/students_providers.dart';
import '../../data/models/fee_models.dart';
import '../controllers/fees_providers.dart';
import '../widgets/fee_widgets.dart';

class FeeAssignmentsPage extends ConsumerStatefulWidget {
  const FeeAssignmentsPage({super.key});

  @override
  ConsumerState<FeeAssignmentsPage> createState() => _FeeAssignmentsPageState();
}

class _FeeAssignmentsPageState extends ConsumerState<FeeAssignmentsPage> {
  final _searchController = TextEditingController();

  String? _academicYearId;
  String? _classId;
  String? _sectionId;
  String? _status;
  FeeListFilter _filter = const FeeListFilter();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final years = ref.watch(academicYearsProvider);
    final assignments = ref.watch(feeAssignmentsProvider(_filter));

    return AdminShell(
      title: 'Fee Assignments',
      activeModuleId: 'fees',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          _Header(
            onBack: () => context.go(AppRoutes.fees),
            onNew: () => context.go(AppRoutes.newFeeAssignment),
            onRefresh: () => ref.invalidate(feeAssignmentsProvider(_filter)),
          ),
          const SizedBox(height: 12),
          years.when(
            data: _filters,
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () => ref.invalidate(academicYearsProvider),
            ),
            loading: () => const AppLoadingState(label: 'Loading filters'),
          ),
          const SizedBox(height: 16),
          assignments.when(
            data: _AssignmentsTable.new,
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () => ref.invalidate(feeAssignmentsProvider(_filter)),
            ),
            loading: () => const AppLoadingState(label: 'Loading assignments'),
          ),
        ],
      ),
    );
  }

  Widget _filters(List<AcademicYearModel> years) {
    final classes = _academicYearId == null
        ? null
        : ref.watch(classesByAcademicYearProvider(_academicYearId!));
    final sections = _classId == null
        ? null
        : ref.watch(sectionsByClassProvider(_classId!));

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                _FieldBox(
                  child: DropdownButtonFormField<String?>(
                    initialValue: _academicYearId,
                    isExpanded: true,
                    decoration: const InputDecoration(
                      labelText: 'Academic year',
                      prefixIcon: Icon(Icons.calendar_month_outlined),
                    ),
                    items: [
                      const DropdownMenuItem(
                        value: null,
                        child: Text('All academic years'),
                      ),
                      for (final year in years)
                        DropdownMenuItem(
                          value: year.id,
                          child: Text(year.name),
                        ),
                    ],
                    onChanged: (value) {
                      setState(() {
                        _academicYearId = value;
                        _classId = null;
                        _sectionId = null;
                      });
                    },
                  ),
                ),
                _FieldBox(
                  child: classes == null
                      ? DropdownButtonFormField<String?>(
                          initialValue: null,
                          decoration: const InputDecoration(
                            labelText: 'Class',
                            prefixIcon: Icon(Icons.school_outlined),
                          ),
                          items: const [
                            DropdownMenuItem(
                              value: null,
                              child: Text('All classes'),
                            ),
                          ],
                          onChanged: null,
                        )
                      : classes.when(
                          data: (items) => DropdownButtonFormField<String?>(
                            initialValue: _classId,
                            isExpanded: true,
                            decoration: const InputDecoration(
                              labelText: 'Class',
                              prefixIcon: Icon(Icons.school_outlined),
                            ),
                            items: [
                              const DropdownMenuItem(
                                value: null,
                                child: Text('All classes'),
                              ),
                              for (final schoolClass in items)
                                DropdownMenuItem(
                                  value: schoolClass.id,
                                  child: Text(schoolClass.name),
                                ),
                            ],
                            onChanged: (value) {
                              setState(() {
                                _classId = value;
                                _sectionId = null;
                              });
                            },
                          ),
                          error: (error, _) => _InlineError(
                            message: _message(error),
                            onRetry: () => ref.invalidate(
                              classesByAcademicYearProvider(_academicYearId!),
                            ),
                          ),
                          loading: () => const LinearProgressIndicator(),
                        ),
                ),
                _FieldBox(
                  child: sections == null
                      ? DropdownButtonFormField<String?>(
                          initialValue: null,
                          decoration: const InputDecoration(
                            labelText: 'Section',
                            prefixIcon: Icon(Icons.view_module_outlined),
                          ),
                          items: const [
                            DropdownMenuItem(
                              value: null,
                              child: Text('All sections'),
                            ),
                          ],
                          onChanged: null,
                        )
                      : sections.when(
                          data: (items) => DropdownButtonFormField<String?>(
                            initialValue: _sectionId,
                            isExpanded: true,
                            decoration: const InputDecoration(
                              labelText: 'Section',
                              prefixIcon: Icon(Icons.view_module_outlined),
                            ),
                            items: [
                              const DropdownMenuItem(
                                value: null,
                                child: Text('All sections'),
                              ),
                              for (final section in items)
                                DropdownMenuItem(
                                  value: section.id,
                                  child: Text(section.name),
                                ),
                            ],
                            onChanged: (value) {
                              setState(() => _sectionId = value);
                            },
                          ),
                          error: (error, _) => _InlineError(
                            message: _message(error),
                            onRetry: () => ref.invalidate(
                              sectionsByClassProvider(_classId!),
                            ),
                          ),
                          loading: () => const LinearProgressIndicator(),
                        ),
                ),
                _FieldBox(
                  child: DropdownButtonFormField<String?>(
                    initialValue: _status,
                    isExpanded: true,
                    decoration: const InputDecoration(
                      labelText: 'Status',
                      prefixIcon: Icon(Icons.flag_outlined),
                    ),
                    items: const [
                      DropdownMenuItem(
                        value: null,
                        child: Text('All statuses'),
                      ),
                      DropdownMenuItem(
                        value: 'PENDING',
                        child: Text('Pending'),
                      ),
                      DropdownMenuItem(
                        value: 'PARTIALLY_PAID',
                        child: Text('Partially paid'),
                      ),
                      DropdownMenuItem(value: 'PAID', child: Text('Paid')),
                      DropdownMenuItem(
                        value: 'OVERDUE',
                        child: Text('Overdue'),
                      ),
                      DropdownMenuItem(
                        value: 'CANCELLED',
                        child: Text('Cancelled'),
                      ),
                    ],
                    onChanged: (value) => setState(() => _status = value),
                  ),
                ),
                _FieldBox(
                  child: TextField(
                    controller: _searchController,
                    decoration: const InputDecoration(
                      labelText: 'Search',
                      prefixIcon: Icon(Icons.search),
                    ),
                    onSubmitted: (_) => _applyFilters(),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              children: [
                FilledButton.icon(
                  onPressed: _applyFilters,
                  icon: const Icon(Icons.tune_outlined),
                  label: const Text('Apply'),
                ),
                OutlinedButton.icon(
                  onPressed: _resetFilters,
                  icon: const Icon(Icons.refresh_outlined),
                  label: const Text('Reset'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _applyFilters() {
    setState(() {
      _filter = FeeListFilter(
        academicYearId: _academicYearId,
        classId: _classId,
        sectionId: _sectionId,
        status: _status,
        query: _blankToNull(_searchController.text),
      );
    });
  }

  void _resetFilters() {
    setState(() {
      _academicYearId = null;
      _classId = null;
      _sectionId = null;
      _status = null;
      _searchController.clear();
      _filter = const FeeListFilter();
    });
  }
}

class _Header extends StatelessWidget {
  const _Header({
    required this.onBack,
    required this.onNew,
    required this.onRefresh,
  });

  final VoidCallback onBack;
  final VoidCallback onNew;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Wrap(
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          runSpacing: 12,
          children: [
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  tooltip: 'Back',
                  onPressed: onBack,
                  icon: const Icon(Icons.arrow_back),
                ),
                const SizedBox(width: 8),
                Text(
                  'Assignments',
                  style: Theme.of(
                    context,
                  ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
                ),
              ],
            ),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                OutlinedButton.icon(
                  onPressed: onRefresh,
                  icon: const Icon(Icons.refresh_outlined),
                  label: const Text('Refresh'),
                ),
                FilledButton.icon(
                  onPressed: onNew,
                  icon: const Icon(Icons.add_outlined),
                  label: const Text('Assign class fee'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _AssignmentsTable extends StatelessWidget {
  const _AssignmentsTable(this.items);

  final List<StudentFeeAssignmentModel> items;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Text('No fee assignments found.'),
        ),
      );
    }
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: AppDataTable<StudentFeeAssignmentModel>(
          items: items,
          onRowTap: (assignment) => context.go(
            AppRoutes.collectFeePaymentForAssignment(assignment.id),
          ),
          columns: [
            AppTableColumn(
              label: 'Student',
              cellBuilder: (_, item) => _TwoLine(
                title: item.studentName,
                subtitle: item.admissionNumber,
              ),
            ),
            AppTableColumn(
              label: 'Academic year',
              cellBuilder: (_, item) => Text(item.academicYear),
            ),
            AppTableColumn(
              label: 'Class',
              cellBuilder: (_, item) => Text(item.className),
            ),
            AppTableColumn(
              label: 'Section',
              cellBuilder: (_, item) => Text(item.sectionName ?? '-'),
            ),
            AppTableColumn(
              label: 'Fee structure',
              cellBuilder: (_, item) => _TwoLine(
                title: item.feeStructureName,
                subtitle: item.feeCategoryName,
              ),
            ),
            AppTableColumn(
              label: 'Source',
              cellBuilder: (_, item) => FeeStatusChip(status: item.sourceType),
            ),
            AppTableColumn(
              label: 'Amount',
              numeric: true,
              cellBuilder: (_, item) => MoneyText(item.grossAmount),
            ),
            AppTableColumn(
              label: 'Paid',
              numeric: true,
              cellBuilder: (_, item) => MoneyText(item.paidAmount),
            ),
            AppTableColumn(
              label: 'Pending',
              numeric: true,
              cellBuilder: (_, item) =>
                  MoneyText(item.balanceAmount, emphasized: true),
            ),
            AppTableColumn(
              label: 'Status',
              cellBuilder: (_, item) => FeeStatusChip(status: item.status),
            ),
          ],
        ),
      ),
    );
  }
}

class _TwoLine extends StatelessWidget {
  const _TwoLine({required this.title, required this.subtitle});

  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 190,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, maxLines: 1, overflow: TextOverflow.ellipsis),
          if (subtitle.trim().isNotEmpty) ...[
            const SizedBox(height: 3),
            Text(
              subtitle,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ],
      ),
    );
  }
}

class _FieldBox extends StatelessWidget {
  const _FieldBox({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return SizedBox(width: 240, child: child);
  }
}

class _InlineError extends StatelessWidget {
  const _InlineError({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return InputDecorator(
      decoration: InputDecoration(
        errorText: message,
        suffixIcon: IconButton(
          tooltip: 'Retry',
          icon: const Icon(Icons.refresh),
          onPressed: onRetry,
        ),
      ),
      child: const SizedBox.shrink(),
    );
  }
}

String? _blankToNull(String value) {
  final trimmed = value.trim();
  return trimmed.isEmpty ? null : trimmed;
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
