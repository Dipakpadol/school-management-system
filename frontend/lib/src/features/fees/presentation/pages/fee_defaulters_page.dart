import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/result/result.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../students/data/models/student_models.dart';
import '../../../students/presentation/controllers/students_providers.dart';
import '../../data/models/fee_models.dart';
import '../../data/repositories/fees_repository_impl.dart';
import '../controllers/fees_providers.dart';
import '../widgets/fee_widgets.dart';

class FeeDefaultersPage extends ConsumerStatefulWidget {
  const FeeDefaultersPage({super.key});

  @override
  ConsumerState<FeeDefaultersPage> createState() => _FeeDefaultersPageState();
}

class _FeeDefaultersPageState extends ConsumerState<FeeDefaultersPage> {
  final _asOfController = TextEditingController();
  final _searchController = TextEditingController();

  String? _academicYearId;
  String? _classId;
  String? _sectionId;
  late FeeDefaulterFilter _filter;
  bool _exporting = false;

  @override
  void initState() {
    super.initState();
    _asOfController.text = _dateLabel(DateTime.now());
    _filter = FeeDefaulterFilter(asOf: _asOfController.text);
  }

  @override
  void dispose() {
    _asOfController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final years = ref.watch(academicYearsProvider);
    final defaulters = ref.watch(feeDefaultersProvider(_filter));

    return AdminShell(
      title: 'Fee Defaulters',
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
            exporting: _exporting,
            onBack: () => context.go(AppRoutes.fees),
            onExport: _export,
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
          defaulters.when(
            data: _DefaulterResults.new,
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () => ref.invalidate(feeDefaultersProvider(_filter)),
            ),
            loading: () => const AppLoadingState(label: 'Loading defaulters'),
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
                        DropdownMenuItem(value: year.id, child: Text(year.name)),
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
                              child: Text('Select academic year'),
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
                            onChanged: (value) =>
                                setState(() => _sectionId = value),
                          ),
                          error: (error, _) => _InlineError(
                            message: _message(error),
                            onRetry: () =>
                                ref.invalidate(sectionsByClassProvider(_classId!)),
                          ),
                          loading: () => const LinearProgressIndicator(),
                        ),
                ),
                _FieldBox(
                  child: TextField(
                    controller: _asOfController,
                    decoration: const InputDecoration(
                      labelText: 'Due on or before',
                      hintText: 'YYYY-MM-DD',
                      prefixIcon: Icon(Icons.event_outlined),
                    ),
                  ),
                ),
                _FieldBox(
                  child: TextField(
                    controller: _searchController,
                    decoration: const InputDecoration(
                      labelText: 'Search student',
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
      _filter = FeeDefaulterFilter(
        academicYearId: _academicYearId,
        classId: _classId,
        sectionId: _sectionId,
        asOf: _blankToNull(_asOfController.text),
        query: _blankToNull(_searchController.text),
      );
    });
  }

  void _resetFilters() {
    setState(() {
      _academicYearId = null;
      _classId = null;
      _sectionId = null;
      _asOfController.text = _dateLabel(DateTime.now());
      _searchController.clear();
      _filter = FeeDefaulterFilter(asOf: _asOfController.text);
    });
  }

  Future<void> _export(String format) async {
    setState(() => _exporting = true);
    final result = await ref
        .read(feesRepositoryProvider)
        .exportDefaulters(
          format,
          academicYearId: _filter.academicYearId,
          classId: _filter.classId,
          asOf: _filter.asOf,
        );
    if (!mounted) {
      return;
    }
    setState(() => _exporting = false);
    result.when(
      success: (_) => _snack('Defaulters exported.'),
      failure: (failure) => _snack(failure.message),
    );
  }

  void _snack(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }
}

class _Header extends StatelessWidget {
  const _Header({
    required this.exporting,
    required this.onBack,
    required this.onExport,
  });

  final bool exporting;
  final VoidCallback onBack;
  final ValueChanged<String> onExport;

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
                  'Defaulters',
                  style: Theme.of(context)
                      .textTheme
                      .titleLarge
                      ?.copyWith(fontWeight: FontWeight.w800),
                ),
              ],
            ),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                OutlinedButton.icon(
                  onPressed: exporting ? null : () => onExport('csv'),
                  icon: const Icon(Icons.table_view_outlined),
                  label: const Text('CSV'),
                ),
                OutlinedButton.icon(
                  onPressed: exporting ? null : () => onExport('xlsx'),
                  icon: const Icon(Icons.grid_on_outlined),
                  label: const Text('Excel'),
                ),
                FilledButton.icon(
                  onPressed: exporting ? null : () => onExport('pdf'),
                  icon: exporting
                      ? const SizedBox.square(
                          dimension: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.picture_as_pdf_outlined),
                  label: const Text('PDF'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _DefaulterResults extends StatelessWidget {
  const _DefaulterResults(this.items);

  final List<FeeDefaulterModel> items;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Text('No fee defaulters found.'),
        ),
      );
    }
    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 1120 ? 2 : 1;
        return GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: columns,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            mainAxisExtent: 124,
          ),
          itemCount: items.length,
          itemBuilder: (context, index) {
            final item = items[index];
            return Card(
              clipBehavior: Clip.antiAlias,
              child: InkWell(
                onTap: () => context.go(
                  AppRoutes.collectFeePaymentForAssignment(item.assignmentId),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    children: [
                      SizedBox.square(
                        dimension: 44,
                        child: DecoratedBox(
                          decoration: BoxDecoration(
                            color: Theme.of(context).colorScheme.errorContainer,
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Icon(
                            Icons.warning_amber_outlined,
                            color: Theme.of(context).colorScheme.onErrorContainer,
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
                              item.studentName,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: Theme.of(context)
                                  .textTheme
                                  .titleMedium
                                  ?.copyWith(fontWeight: FontWeight.w800),
                            ),
                            const SizedBox(height: 6),
                            Text(
                              [
                                item.admissionNumber,
                                item.academicYear,
                                _classLabel(item),
                                '${item.overdueInstallments} overdue',
                                if (item.oldestDueDate != null)
                                  'Oldest ${_dateLabel(item.oldestDueDate!)}',
                              ].join(' - '),
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                    color: Theme.of(context).colorScheme.outline,
                                  ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 12),
                      MoneyText(item.balanceAmount, emphasized: true),
                    ],
                  ),
                ),
              ),
            );
          },
        );
      },
    );
  }
}

class _FieldBox extends StatelessWidget {
  const _FieldBox({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return SizedBox(width: 260, child: child);
  }
}

class _InlineError extends StatelessWidget {
  const _InlineError({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return InputDecorator(
      decoration: const InputDecoration(labelText: 'Unable to load'),
      child: Row(
        children: [
          Expanded(
            child: Text(
              message,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          IconButton(
            tooltip: 'Retry',
            onPressed: onRetry,
            icon: const Icon(Icons.refresh_outlined),
          ),
        ],
      ),
    );
  }
}

String _classLabel(FeeDefaulterModel item) {
  final section = item.sectionName;
  return section == null || section.isEmpty
      ? item.className
      : '${item.className} $section';
}

String? _blankToNull(String value) {
  final text = value.trim();
  return text.isEmpty ? null : text;
}

String _dateLabel(DateTime date) {
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
