import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../students/data/models/student_models.dart';
import '../../../students/presentation/controllers/students_providers.dart';
import '../../data/models/fee_models.dart';
import '../../data/repositories/fees_repository_impl.dart';
import '../controllers/fees_providers.dart';
import '../widgets/fee_widgets.dart';

class StudentFeeAssignmentPage extends ConsumerStatefulWidget {
  const StudentFeeAssignmentPage({super.key});

  @override
  ConsumerState<StudentFeeAssignmentPage> createState() {
    return _StudentFeeAssignmentPageState();
  }
}

class _StudentFeeAssignmentPageState
    extends ConsumerState<StudentFeeAssignmentPage> {
  final _formKey = GlobalKey<FormState>();
  final _assignedDateController = TextEditingController();
  final _notesController = TextEditingController();

  String? _academicYearId;
  String? _classId;
  String? _feeStructureId;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _assignedDateController.text = _dateLabel(DateTime.now());
  }

  @override
  void dispose() {
    _assignedDateController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final years = ref.watch(academicYearsProvider);

    return AdminShell(
      title: 'Class fee assignment',
      activeModuleId: 'fees',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: years.when(
        data: _buildForYears,
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(academicYearsProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading academic years'),
      ),
    );
  }

  Widget _buildForYears(List<AcademicYearModel> years) {
    if (years.isEmpty) {
      return const _EmptyPanel(
        icon: Icons.calendar_month_outlined,
        title: 'No academic years available',
        message: 'Create an academic year before assigning class fees.',
      );
    }
    final selectedYear = years.any((year) => year.id == _academicYearId)
        ? _academicYearId!
        : years.first.id;
    if (_academicYearId != selectedYear) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          setState(() {
            _academicYearId = selectedYear;
            _classId = null;
            _feeStructureId = null;
          });
        }
      });
    }

    final classes = ref.watch(classesByAcademicYearProvider(selectedYear));

    return classes.when(
      data: (items) => _buildForm(years, items, selectedYear),
      error: (error, _) => AppErrorState(
        message: _message(error),
        onRetry: () => ref.invalidate(
          classesByAcademicYearProvider(selectedYear),
        ),
      ),
      loading: () => const AppLoadingState(label: 'Loading classes'),
    );
  }

  Widget _buildForm(
    List<AcademicYearModel> years,
    List<SchoolClassModel> classes,
    String selectedYear,
  ) {
    final selectedClass = classes.isEmpty
        ? null
        : classes.any((item) => item.id == _classId)
            ? _classId
            : classes.first.id;
    if (selectedClass != null && _classId != selectedClass) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted && _classId != selectedClass) {
          setState(() {
            _classId = selectedClass;
            _feeStructureId = null;
          });
        }
      });
    }
    final key = selectedClass != null
        ? FeeClassKey(academicYearId: selectedYear, classId: selectedClass)
        : null;
    final structures = key == null ? null : ref.watch(feeStructuresByClassProvider(key));
    final students = selectedClass == null
        ? null
        : ref.watch(classFeeStudentsProvider(selectedClass));

    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 980),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _Header(
                  onBack: () => context.go(AppRoutes.fees),
                  onSave: _saving ||
                          selectedClass == null ||
                          _classId != selectedClass ||
                          _feeStructureId == null
                      ? null
                      : _submit,
                  saving: _saving,
                ),
                const SizedBox(height: 12),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: Column(
                      children: [
                        _ResponsiveFields(
                          children: [
                            DropdownButtonFormField<String>(
                              initialValue: selectedYear,
                              isExpanded: true,
                              items: [
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
                                  _feeStructureId = null;
                                });
                              },
                              validator: _required,
                              decoration: const InputDecoration(
                                labelText: 'Academic year',
                                prefixIcon: Icon(
                                  Icons.calendar_month_outlined,
                                ),
                              ),
                            ),
                            if (classes.isNotEmpty)
                              DropdownButtonFormField<String>(
                                initialValue: selectedClass,
                                isExpanded: true,
                                items: [
                                  for (final schoolClass in classes)
                                    DropdownMenuItem(
                                      value: schoolClass.id,
                                      child: Text(schoolClass.name),
                                    ),
                                ],
                                onChanged: (value) {
                                  setState(() {
                                    _classId = value;
                                    _feeStructureId = null;
                                  });
                                },
                                validator: _required,
                                decoration: const InputDecoration(
                                  labelText: 'Class',
                                  prefixIcon: Icon(Icons.school_outlined),
                                ),
                              ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        if (classes.isEmpty)
                          const _InlineEmptyMessage(
                            icon: Icons.school_outlined,
                            title: 'No classes available',
                            message:
                                'Create classes for the selected academic year first.',
                          )
                        else ...[
                          structures == null
                              ? const SizedBox.shrink()
                              : structures.when(
                                  data: _structureDropdown,
                                  error: (error, _) => Text(_message(error)),
                                  loading: () =>
                                      const LinearProgressIndicator(),
                                ),
                          const SizedBox(height: 12),
                          _ResponsiveFields(
                            children: [
                              TextFormField(
                                controller: _assignedDateController,
                                validator: _date,
                                decoration: const InputDecoration(
                                  labelText: 'Assigned date',
                                  hintText: 'YYYY-MM-DD',
                                  prefixIcon: Icon(Icons.event_outlined),
                                ),
                              ),
                              TextFormField(
                                controller: _notesController,
                                decoration: const InputDecoration(
                                  labelText: 'Notes',
                                  prefixIcon: Icon(Icons.notes_outlined),
                                ),
                              ),
                            ],
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 14),
                if (students != null)
                  students.when(
                    data: (items) => _ClassStudentsPanel(students: items),
                    error: (error, _) => AppErrorState(message: _message(error)),
                    loading: () =>
                        const AppLoadingState(label: 'Loading students'),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _structureDropdown(List<FeeStructureModel> structures) {
    final selected = structures.any((item) => item.id == _feeStructureId)
        ? _feeStructureId
        : null;
    if (structures.isEmpty) {
      return const Align(
        alignment: Alignment.centerLeft,
        child: Text(
          'No fee structure available. Please create a fee structure first.',
        ),
      );
    }
    if (selected == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted &&
            !structures.any((structure) => structure.id == _feeStructureId)) {
          setState(() => _feeStructureId = structures.first.id);
        }
      });
    }
    return DropdownButtonFormField<String>(
      initialValue: selected ?? structures.first.id,
      isExpanded: true,
      items: [
        for (final structure in structures)
          DropdownMenuItem(
            value: structure.id,
            child: Text(
              '${structure.name} - INR ${structure.totalAmount.toStringAsFixed(2)}',
              overflow: TextOverflow.ellipsis,
            ),
          ),
      ],
      onChanged: (value) => setState(() => _feeStructureId = value),
      validator: _required,
      decoration: const InputDecoration(
        labelText: 'Class fee structure',
        prefixIcon: Icon(Icons.account_tree_outlined),
      ),
    );
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }
    final classId = _classId;
    final feeStructureId = _feeStructureId;
    if (classId == null || feeStructureId == null) {
      return;
    }
    setState(() => _saving = true);
    final result = await ref.read(feesRepositoryProvider).assignClassFee(
      classId,
      {
        'feeStructureId': feeStructureId,
        'assignedDate': _assignedDateController.text.trim(),
        'notes': _blankToNull(_notesController.text),
        'skipExisting': true,
      },
    );
    if (!mounted) {
      return;
    }
    setState(() => _saving = false);
    result.when<void>(
      success: (summary) {
        ref.invalidate(feeAssignmentsProvider);
        ref.invalidate(classFeeStudentsProvider(classId));
        _showSnack(
          'Created ${summary.createdAssignments}, skipped ${summary.skippedAssignments}.',
        );
      },
      failure: (failure) => _showSnack(failure.message),
    );
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}

class _Header extends StatelessWidget {
  const _Header({
    required this.onBack,
    required this.onSave,
    required this.saving,
  });

  final VoidCallback onBack;
  final VoidCallback? onSave;
  final bool saving;

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
                  'Assign fee to class',
                  style: Theme.of(
                    context,
                  ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
                ),
              ],
            ),
            AppButton(
              label: 'Assign',
              icon: Icons.groups_outlined,
              isLoading: saving,
              onPressed: onSave,
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyPanel extends StatelessWidget {
  const _EmptyPanel({
    required this.icon,
    required this.title,
    required this.message,
  });

  final IconData icon;
  final String title;
  final String message;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(22),
        child: Row(
          children: [
            Icon(icon, size: 34, color: Theme.of(context).colorScheme.outline),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    title,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w800,
                        ),
                  ),
                  const SizedBox(height: 4),
                  Text(message),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InlineEmptyMessage extends StatelessWidget {
  const _InlineEmptyMessage({
    required this.icon,
    required this.title,
    required this.message,
  });

  final IconData icon;
  final String title;
  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Row(
        children: [
          Icon(icon, size: 30, color: Theme.of(context).colorScheme.outline),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  title,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(message),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ClassStudentsPanel extends StatelessWidget {
  const _ClassStudentsPanel({required this.students});

  final List<ClassStudentFeeModel> students;

  @override
  Widget build(BuildContext context) {
    if (students.isEmpty) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(18),
          child: Text('No students found in this class.'),
        ),
      );
    }
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Students in class',
              style: Theme.of(
                context,
              ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 10),
            for (final student in students)
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(student.studentName),
                subtitle: Text(
                  '${student.admissionNumber} - Roll ${student.rollNumber ?? '-'}',
                ),
                trailing: student.assignmentId == null
                    ? const FeeStatusChip(status: 'UNASSIGNED')
                    : Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          MoneyText(student.balanceAmount, emphasized: true),
                          const SizedBox(height: 4),
                          FeeStatusChip(status: student.status ?? 'PENDING'),
                        ],
                      ),
              ),
          ],
        ),
      ),
    );
  }
}

class _ResponsiveFields extends StatelessWidget {
  const _ResponsiveFields({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 720 ? 2 : 1;
        return GridView.count(
          crossAxisCount: columns,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          childAspectRatio: columns == 1 ? 5.2 : 4.8,
          physics: const NeverScrollableScrollPhysics(),
          shrinkWrap: true,
          children: children,
        );
      },
    );
  }
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _date(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return 'Required';
  }
  final parsed = DateTime.tryParse(text);
  if (parsed == null || text.length != 10) {
    return 'Use YYYY-MM-DD';
  }
  return null;
}

String? _blankToNull(String value) {
  final trimmed = value.trim();
  return trimmed.isEmpty ? null : trimmed;
}

String _dateLabel(DateTime date) {
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
