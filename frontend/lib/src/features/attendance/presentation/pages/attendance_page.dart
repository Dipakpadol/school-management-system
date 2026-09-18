import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/download/file_downloader.dart';
import '../../../../core/theme/app_design_system.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../../core/widgets/app_page_layout.dart';
import '../../../academic/data/models/academic_models.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../dashboard/presentation/controllers/dashboard_controller.dart';
import '../../data/models/attendance_models.dart';
import '../../data/repositories/attendance_repository_impl.dart';

class AttendancePage extends ConsumerStatefulWidget {
  const AttendancePage({super.key});

  @override
  ConsumerState<AttendancePage> createState() => _AttendancePageState();
}

class _AttendancePageState extends ConsumerState<AttendancePage> {
  List<AcademicYearModel> _years = const [];
  List<AcademicClassModel> _classes = const [];
  List<AcademicDivisionModel> _sections = const [];
  List<AttendanceStudentModel> _students = const [];
  AttendanceSummaryModel? _dailySummary;
  AttendanceSummaryModel? _monthlySummary;
  final Map<String, String> _statuses = {};
  final Map<String, String> _remarks = {};
  String? _yearId;
  String? _classId;
  String? _sectionId;
  DateTime _date = DateTime.now();
  bool _loading = false;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_loadYears);
  }

  @override
  Widget build(BuildContext context) {
    return AdminShell(
      title: 'Attendance',
      activeModuleId: 'attendance',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(24, 24, 24, 12),
            child: AppPageHeader(
              title: 'Student Attendance',
              subtitle:
                  'Mark daily attendance, review daily and monthly summaries, and export the selected class attendance.',
              icon: Icons.fact_check_outlined,
              actions: [
                OutlinedButton.icon(
                  onPressed: () => context.go(AppRoutes.teacherAttendance),
                  icon: const Icon(Icons.badge_outlined),
                  label: const Text('Teacher attendance'),
                ),
              ],
            ),
          ),
          _filters(context),
          const Divider(height: 1),
          Expanded(child: _body()),
        ],
      ),
    );
  }

  Widget _filters(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 16),
      child: AppSectionCard(
        title: 'Attendance Filters',
        subtitle: 'Select academic year, class, division, and attendance date.',
        child: Wrap(
          spacing: 12,
          runSpacing: 12,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            _dropdown(
              width: 220,
              label: 'Academic year',
              value: _yearId,
              items: _years.map((year) => MapEntry(year.id, year.name)),
              onChanged: (value) {
                setState(() {
                  _yearId = value;
                  _classId = null;
                  _sectionId = null;
                  _classes = const [];
                  _sections = const [];
                  _students = const [];
                  _dailySummary = null;
                  _monthlySummary = null;
                });
                if (value != null) {
                  _loadClasses(value);
                }
              },
            ),
            _dropdown(
              width: 200,
              label: 'Class',
              value: _classId,
              items: _classes.map((item) => MapEntry(item.id, item.name)),
              onChanged: (value) {
                setState(() {
                  _classId = value;
                  _sectionId = null;
                  _sections = const [];
                  _students = const [];
                  _dailySummary = null;
                  _monthlySummary = null;
                });
                if (value != null) {
                  _loadSections(value);
                }
              },
            ),
            _dropdown(
              width: 200,
              label: 'Division',
              value: _sectionId,
              items: _sections.map((item) => MapEntry(item.id, item.name)),
              onChanged: (value) {
                setState(() {
                  _sectionId = value;
                  _students = const [];
                  _dailySummary = null;
                  _monthlySummary = null;
                });
              },
            ),
            OutlinedButton.icon(
              onPressed: _pickDate,
              icon: const Icon(Icons.calendar_today_outlined),
              label: Text(_dateLabel(_date)),
            ),
            AppButton(
              label: 'Load',
              icon: Icons.refresh,
              isLoading: _loading,
              onPressed: _loadAttendance,
            ),
            OutlinedButton.icon(
              onPressed: _students.isEmpty ? null : () => _markAll('PRESENT'),
              icon: const Icon(Icons.done_all_outlined),
              label: const Text('All present'),
            ),
            OutlinedButton.icon(
              onPressed: _students.isEmpty ? null : () => _markAll('ABSENT'),
              icon: const Icon(Icons.remove_done_outlined),
              label: const Text('All absent'),
            ),
            OutlinedButton.icon(
              onPressed: _students.isEmpty ? null : _export,
              icon: const Icon(Icons.download_outlined),
              label: const Text('Export'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _body() {
    if (_loading) {
      return const AppLoadingState(label: 'Loading attendance');
    }
    if (_error != null) {
      return AppErrorState(message: _error!, onRetry: _loadAttendance);
    }
    if (_students.isEmpty) {
      return AppEmptyState(
        message: _yearId == null || _classId == null || _sectionId == null
            ? 'Select academic year, class, division, and date to load attendance.'
            : 'No students found for the selected class and division.',
        icon: Icons.groups_outlined,
      );
    }
    return Column(
      children: [
        if (_dailySummary != null || _monthlySummary != null) _summaryStrip(),
        Expanded(
          child: ListView.separated(
            padding: const EdgeInsets.all(18),
            itemCount: _students.length,
            separatorBuilder: (_, _) => const SizedBox(height: 10),
            itemBuilder: (context, index) {
              final student = _students[index];
              return Card(
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      SizedBox(
                        width: 280,
                        child: ListTile(
                          dense: true,
                          contentPadding: EdgeInsets.zero,
                          title: Text(student.displayName),
                          subtitle: Text(
                            '${student.rollNumber ?? '-'} | ${student.admissionNumber}',
                          ),
                        ),
                      ),
                      SizedBox(
                        width: 180,
                        child: DropdownButtonFormField<String>(
                          initialValue:
                              _statuses[student.studentId] ?? 'PRESENT',
                          decoration: const InputDecoration(
                            labelText: 'Status',
                          ),
                          items: _attendanceStatuses
                              .map(
                                (status) => DropdownMenuItem(
                                  value: status,
                                  child: Text(status.replaceAll('_', ' ')),
                                ),
                              )
                              .toList(),
                          onChanged: (value) {
                            if (value != null) {
                              setState(
                                () => _statuses[student.studentId] = value,
                              );
                            }
                          },
                        ),
                      ),
                      SizedBox(
                        width: 320,
                        child: TextFormField(
                          initialValue: _remarks[student.studentId],
                          decoration: const InputDecoration(
                            labelText: 'Remarks',
                          ),
                          onChanged: (value) =>
                              _remarks[student.studentId] = value,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(18),
          child: Align(
            alignment: Alignment.centerRight,
            child: AppButton(
              label: 'Save attendance',
              icon: Icons.save_outlined,
              isLoading: _saving,
              onPressed: _save,
            ),
          ),
        ),
      ],
    );
  }

  Widget _summaryStrip() {
    final daily = _dailySummary;
    final monthly = _monthlySummary;
    return Padding(
      padding: const EdgeInsets.fromLTRB(18, 14, 18, 4),
      child: AppStatGrid(
        minItemWidth: 160,
        maxColumns: 5,
        children: [
          _summaryTile(
            'Eligible',
            daily?.eligibleStudentCount,
            Icons.groups_outlined,
            AppDesignTokens.primary,
          ),
          _summaryTile(
            'Present',
            daily?.presentCount,
            Icons.check_circle_outline,
            AppDesignTokens.success,
          ),
          _summaryTile(
            'Absent',
            daily?.absentCount,
            Icons.cancel_outlined,
            AppDesignTokens.danger,
          ),
          _summaryTile(
            'Late',
            daily?.lateCount,
            Icons.schedule_outlined,
            AppDesignTokens.warning,
          ),
          _summaryTile(
            'Half day',
            daily?.halfDayCount,
            Icons.timelapse_outlined,
            AppDesignTokens.amber,
          ),
          _summaryTile(
            'Leave',
            daily?.leaveCount,
            Icons.event_available_outlined,
            AppDesignTokens.teal,
          ),
          _summaryTile(
            'Daily %',
            _formatPercent(daily?.attendancePercentage),
            Icons.percent_outlined,
            AppDesignTokens.violet,
          ),
          _summaryTile(
            'Monthly %',
            _formatPercent(monthly?.attendancePercentage),
            Icons.calendar_month_outlined,
            AppDesignTokens.primaryDark,
          ),
          _summaryTile(
            'Month records',
            monthly?.totalRecords,
            Icons.fact_check_outlined,
            AppDesignTokens.muted,
          ),
        ],
      ),
    );
  }

  Widget _summaryTile(String label, Object? value, IconData icon, Color color) {
    return AppStatCard(
      label: label,
      value: value?.toString() ?? '-',
      icon: icon,
      color: color,
    );
  }

  Widget _dropdown({
    required double width,
    required String label,
    required String? value,
    required Iterable<MapEntry<String, String>> items,
    required ValueChanged<String?> onChanged,
  }) {
    return SizedBox(
      width: width,
      child: DropdownButtonFormField<String>(
        initialValue: value,
        isExpanded: true,
        decoration: InputDecoration(labelText: label),
        items: [
          for (final item in items)
            DropdownMenuItem(value: item.key, child: Text(item.value)),
        ],
        onChanged: onChanged,
      ),
    );
  }

  Future<void> _loadYears() async {
    final result = await ref.read(attendanceRepositoryProvider).years();
    result.when(
      success: (years) => setState(() => _years = years),
      failure: (failure) => setState(() => _error = failure.message),
    );
  }

  Future<void> _loadClasses(String yearId) async {
    final result = await ref.read(attendanceRepositoryProvider).classes(yearId);
    result.when(
      success: (classes) => setState(() => _classes = classes),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _loadSections(String classId) async {
    final result = await ref
        .read(attendanceRepositoryProvider)
        .sections(classId);
    result.when(
      success: (sections) => setState(() => _sections = sections),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _loadAttendance() async {
    if (_yearId == null || _classId == null || _sectionId == null) {
      _snack('Select academic year, class, and division.');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
      _dailySummary = null;
      _monthlySummary = null;
    });
    final repository = ref.read(attendanceRepositoryProvider);
    final selectedDate = _dateLabel(_date);
    final studentsResult = await repository.students(
      academicYearId: _yearId!,
      classId: _classId!,
      sectionId: _sectionId!,
      date: selectedDate,
    );
    final dailyResult = await repository.daily(
      academicYearId: _yearId!,
      classId: _classId!,
      sectionId: _sectionId!,
      date: selectedDate,
    );
    final summaryResult = await repository.summary(
      academicYearId: _yearId!,
      classId: _classId!,
      sectionId: _sectionId!,
      fromDate: selectedDate,
      toDate: selectedDate,
    );
    final monthlyResult = await repository.monthly(
      academicYearId: _yearId!,
      classId: _classId!,
      sectionId: _sectionId!,
      year: _date.year,
      month: _date.month,
    );
    if (!mounted) {
      return;
    }
    studentsResult.when(
      success: (students) {
        _students = students;
        _statuses
          ..clear()
          ..addEntries(
            students.map((student) => MapEntry(student.studentId, 'PRESENT')),
          );
        _remarks.clear();
      },
      failure: (failure) => _error = failure.message,
    );
    dailyResult.when(
      success: (daily) {
        for (final record in daily.records) {
          _statuses[record.student.studentId] = record.status;
          if (record.remarks != null) {
            _remarks[record.student.studentId] = record.remarks!;
          }
        }
      },
      failure: (_) {},
    );
    summaryResult.when(
      success: (summary) => _dailySummary = summary,
      failure: (_) {},
    );
    monthlyResult.when(
      success: (summary) => _monthlySummary = summary,
      failure: (_) {},
    );
    setState(() => _loading = false);
  }

  Future<void> _save() async {
    if (_students.isEmpty || _saving) {
      return;
    }
    setState(() => _saving = true);
    final result = await ref.read(attendanceRepositoryProvider).saveDaily({
      'academicYearId': _yearId,
      'classId': _classId,
      'sectionId': _sectionId,
      'attendanceDate': _dateLabel(_date),
      'records': [
        for (final student in _students)
          {
            'studentId': student.studentId,
            'status': _statuses[student.studentId] ?? 'PRESENT',
            'remarks': _blankToNull(_remarks[student.studentId]),
          },
      ],
    });
    if (!mounted) {
      return;
    }
    var saved = false;
    result.when(
      success: (_) {
        ref.invalidate(dashboardOverviewProvider);
        saved = true;
        _snack('Attendance saved successfully.');
      },
      failure: (failure) => _snack(failure.message),
    );
    setState(() => _saving = false);
    if (saved) {
      await _loadAttendance();
    }
  }

  Future<void> _export() async {
    final result = await ref.read(attendanceRepositoryProvider).export({
      'academicYearId': _yearId,
      'classId': _classId,
      'sectionId': _sectionId,
      'fromDate': _dateLabel(_date),
      'toDate': _dateLabel(_date),
    });
    result.when(
      success: (bytes) async {
        await downloadBytes(bytes, 'attendance.csv', 'text/csv');
        _snack('Attendance export downloaded.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (!mounted) {
      return;
    }
    if (picked != null) {
      setState(() => _date = picked);
      if (_yearId != null && _classId != null && _sectionId != null) {
        await _loadAttendance();
      }
    }
  }

  void _markAll(String status) {
    setState(() {
      for (final student in _students) {
        _statuses[student.studentId] = status;
      }
    });
  }

  void _snack(String message) {
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}

const _attendanceStatuses = ['PRESENT', 'ABSENT', 'LATE', 'HALF_DAY', 'LEAVE'];

String _dateLabel(DateTime date) {
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}

String _formatPercent(double? value) {
  if (value == null) {
    return '-';
  }
  if (value == value.roundToDouble()) {
    return '${value.toInt()}%';
  }
  return '${value.toStringAsFixed(1)}%';
}

String? _blankToNull(String? value) {
  if (value == null || value.trim().isEmpty) {
    return null;
  }
  return value.trim();
}
