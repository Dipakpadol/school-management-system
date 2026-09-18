import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/theme/app_design_system.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../../core/widgets/app_page_layout.dart';
import '../../../../core/widgets/app_select_field.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../students/data/models/student_models.dart';
import '../../data/models/teacher_models.dart';
import '../../data/repositories/teachers_repository_impl.dart';
import '../controllers/teachers_providers.dart';

class TeacherAttendancePage extends ConsumerStatefulWidget {
  const TeacherAttendancePage({super.key});

  @override
  ConsumerState<TeacherAttendancePage> createState() =>
      _TeacherAttendancePageState();
}

class _TeacherAttendancePageState extends ConsumerState<TeacherAttendancePage> {
  String? _academicYearId;
  DateTime _attendanceDate = _dateOnly(DateTime.now());
  final Map<String, String> _statuses = {};
  final Map<String, String> _remarks = {};
  bool _saving = false;

  @override
  Widget build(BuildContext context) {
    final years = ref.watch(teacherAttendanceAcademicYearsProvider);

    return AdminShell(
      title: 'Teacher Attendance',
      activeModuleId: 'teachers',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: years.when(
        data: (items) {
          final effectiveYearId =
              _academicYearId ?? (items.isEmpty ? null : items.first.id);
          if (_academicYearId == null && effectiveYearId != null) {
            WidgetsBinding.instance.addPostFrameCallback((_) {
              if (mounted) {
                setState(() => _academicYearId = effectiveYearId);
              }
            });
          }
          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(24, 24, 24, 12),
                child: AppPageHeader(
                  title: 'Teacher Attendance',
                  subtitle:
                      'Mark daily attendance for teaching staff and review the selected day summary.',
                  icon: Icons.fact_check_outlined,
                  actions: [
                    OutlinedButton.icon(
                      onPressed: () => context.go(AppRoutes.teachers),
                      icon: const Icon(Icons.arrow_back_outlined),
                      label: const Text('Teachers'),
                    ),
                  ],
                ),
              ),
              _TeacherAttendanceHeader(
                years: items,
                selectedAcademicYearId: effectiveYearId,
                attendanceDate: _attendanceDate,
                saving: _saving,
                onAcademicYearChanged: (value) {
                  setState(() {
                    _academicYearId = value;
                    _statuses.clear();
                    _remarks.clear();
                  });
                },
                onDateChanged: _pickDate,
                onMarkAllPresent: effectiveYearId == null
                    ? null
                    : () => _markAllPresent(effectiveYearId),
                onSave: effectiveYearId == null
                    ? null
                    : () => _save(effectiveYearId),
              ),
              const Divider(height: 1),
              Expanded(
                child: effectiveYearId == null
                    ? const AppEmptyState(
                        message: 'No academic years found for attendance.',
                        icon: Icons.calendar_month_outlined,
                      )
                    : _TeacherAttendanceBody(
                        academicYearId: effectiveYearId,
                        attendanceDate: _attendanceDate,
                        statuses: _statuses,
                        remarks: _remarks,
                        onStatusChanged: (teacherId, status) {
                          setState(() => _statuses[teacherId] = status);
                        },
                        onRemarksChanged: (teacherId, value) {
                          _remarks[teacherId] = value;
                        },
                      ),
              ),
            ],
          );
        },
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(teacherAttendanceAcademicYearsProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading academic years'),
      ),
    );
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _attendanceDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(2035),
    );
    if (picked == null) {
      return;
    }
    setState(() {
      _attendanceDate = _dateOnly(picked);
      _statuses.clear();
      _remarks.clear();
    });
  }

  void _markAllPresent(String academicYearId) {
    final teachers = ref.read(
      teacherAttendanceTeachersProvider(academicYearId),
    );
    teachers.maybeWhen(
      data: (items) {
        setState(() {
          for (final teacher in items) {
            _statuses[teacher.id] = 'PRESENT';
          }
        });
      },
      orElse: () {},
    );
  }

  Future<void> _save(String academicYearId) async {
    final teachers = ref.read(
      teacherAttendanceTeachersProvider(academicYearId),
    );
    final dailyKey = TeacherDailyAttendanceKey(
      academicYearId: academicYearId,
      date: _attendanceDate,
    );
    final daily = ref.read(teacherDailyAttendanceProvider(dailyKey));
    final teacherItems = teachers.maybeWhen(
      data: (items) => items,
      orElse: () => const <TeacherAttendanceTeacherModel>[],
    );
    final dailyRecord = daily.maybeWhen(
      data: (value) => value,
      orElse: () => null,
    );
    if (teacherItems.isEmpty) {
      return;
    }
    setState(() => _saving = true);
    final payload = {
      'academicYearId': academicYearId,
      'attendanceDate': _dateParam(_attendanceDate),
      'records': [
        for (final teacher in teacherItems)
          {
            'teacherId': teacher.id,
            'status':
                _statuses[teacher.id] ??
                _existingRecord(dailyRecord, teacher.id)?.status ??
                'PRESENT',
            'remarks': _remarks.containsKey(teacher.id)
                ? _remarks[teacher.id]
                : _existingRecord(dailyRecord, teacher.id)?.remarks ?? '',
          },
      ],
    };
    final result = await ref
        .read(teachersRepositoryProvider)
        .saveDailyAttendance(payload);
    if (!mounted) {
      return;
    }
    setState(() => _saving = false);
    result.when(
      success: (_) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Teacher attendance saved.')),
        );
        ref.invalidate(teacherDailyAttendanceProvider(dailyKey));
      },
      failure: (failure) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(failure.message)));
      },
    );
  }
}

class _TeacherAttendanceHeader extends StatelessWidget {
  const _TeacherAttendanceHeader({
    required this.years,
    required this.selectedAcademicYearId,
    required this.attendanceDate,
    required this.saving,
    required this.onAcademicYearChanged,
    required this.onDateChanged,
    required this.onMarkAllPresent,
    required this.onSave,
  });

  final List<AcademicYearModel> years;
  final String? selectedAcademicYearId;
  final DateTime attendanceDate;
  final bool saving;
  final ValueChanged<String?> onAcademicYearChanged;
  final VoidCallback onDateChanged;
  final VoidCallback? onMarkAllPresent;
  final VoidCallback? onSave;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 16),
      child: AppSectionCard(
        title: 'Attendance Controls',
        subtitle:
            'Select academic year and date before marking teacher attendance.',
        child: Wrap(
          spacing: 12,
          runSpacing: 10,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            SizedBox(
              width: 300,
              child: AppSelectField<String>(
                label: 'Academic Year',
                icon: Icons.calendar_month_outlined,
                value: selectedAcademicYearId,
                items: [
                  for (final year in years)
                    DropdownMenuItem(value: year.id, child: Text(year.name)),
                ],
                onChanged: onAcademicYearChanged,
              ),
            ),
            OutlinedButton.icon(
              onPressed: onDateChanged,
              icon: const Icon(Icons.event_outlined),
              label: Text(_dateLabel(attendanceDate)),
            ),
            OutlinedButton.icon(
              onPressed: onMarkAllPresent,
              icon: const Icon(Icons.done_all_outlined),
              label: const Text('Mark all present'),
            ),
            FilledButton.icon(
              onPressed: saving ? null : onSave,
              icon: saving
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.save_outlined),
              label: const Text('Save'),
            ),
          ],
        ),
      ),
    );
  }
}

class _TeacherAttendanceBody extends ConsumerWidget {
  const _TeacherAttendanceBody({
    required this.academicYearId,
    required this.attendanceDate,
    required this.statuses,
    required this.remarks,
    required this.onStatusChanged,
    required this.onRemarksChanged,
  });

  final String academicYearId;
  final DateTime attendanceDate;
  final Map<String, String> statuses;
  final Map<String, String> remarks;
  final void Function(String teacherId, String status) onStatusChanged;
  final void Function(String teacherId, String remarks) onRemarksChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final teachers = ref.watch(
      teacherAttendanceTeachersProvider(academicYearId),
    );
    final dailyKey = TeacherDailyAttendanceKey(
      academicYearId: academicYearId,
      date: attendanceDate,
    );
    final daily = ref.watch(teacherDailyAttendanceProvider(dailyKey));

    return teachers.when(
      data: (teacherItems) {
        return daily.when(
          data: (dailyRecord) {
            if (teacherItems.isEmpty) {
              return const AppEmptyState(
                message: 'No teachers found for the selected academic year.',
                icon: Icons.badge_outlined,
              );
            }
            return ListView(
              padding: const EdgeInsets.all(24),
              children: [
                AppStatGrid(
                  minItemWidth: 160,
                  maxColumns: 5,
                  children: [
                    _SummaryChip(
                      label: 'Present',
                      value: dailyRecord.present,
                      icon: Icons.check_circle_outline,
                      color: AppDesignTokens.success,
                    ),
                    _SummaryChip(
                      label: 'Absent',
                      value: dailyRecord.absent,
                      icon: Icons.cancel_outlined,
                      color: AppDesignTokens.danger,
                    ),
                    _SummaryChip(
                      label: 'Late',
                      value: dailyRecord.late,
                      icon: Icons.schedule_outlined,
                      color: AppDesignTokens.warning,
                    ),
                    _SummaryChip(
                      label: 'Half day',
                      value: dailyRecord.halfDay,
                      icon: Icons.timelapse_outlined,
                      color: AppDesignTokens.amber,
                    ),
                    _SummaryChip(
                      label: 'Leave',
                      value: dailyRecord.leave,
                      icon: Icons.event_available_outlined,
                      color: AppDesignTokens.teal,
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                DecoratedBox(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    border: Border.all(color: const Color(0xFFE2E8F0)),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Column(
                    children: [
                      for (final teacher in teacherItems)
                        _TeacherAttendanceRow(
                          key: ValueKey(
                            '${teacher.id}-${_dateParam(attendanceDate)}',
                          ),
                          teacher: teacher,
                          existing: _existingRecord(dailyRecord, teacher.id),
                          selectedStatus:
                              statuses[teacher.id] ??
                              _existingRecord(
                                dailyRecord,
                                teacher.id,
                              )?.status ??
                              'PRESENT',
                          remarks: remarks.containsKey(teacher.id)
                              ? remarks[teacher.id] ?? ''
                              : _existingRecord(
                                      dailyRecord,
                                      teacher.id,
                                    )?.remarks ??
                                    '',
                          onStatusChanged: (status) =>
                              onStatusChanged(teacher.id, status),
                          onRemarksChanged: (value) =>
                              onRemarksChanged(teacher.id, value),
                        ),
                    ],
                  ),
                ),
              ],
            );
          },
          error: (error, _) => AppErrorState(
            message: _message(error),
            onRetry: () =>
                ref.invalidate(teacherDailyAttendanceProvider(dailyKey)),
          ),
          loading: () => const AppLoadingState(label: 'Loading attendance'),
        );
      },
      error: (error, _) => AppErrorState(
        message: _message(error),
        onRetry: () =>
            ref.invalidate(teacherAttendanceTeachersProvider(academicYearId)),
      ),
      loading: () => const AppLoadingState(label: 'Loading teachers'),
    );
  }
}

class _TeacherAttendanceRow extends StatelessWidget {
  const _TeacherAttendanceRow({
    super.key,
    required this.teacher,
    required this.existing,
    required this.selectedStatus,
    required this.remarks,
    required this.onStatusChanged,
    required this.onRemarksChanged,
  });

  final TeacherAttendanceTeacherModel teacher;
  final TeacherAttendanceRecordModel? existing;
  final String selectedStatus;
  final String remarks;
  final ValueChanged<String> onStatusChanged;
  final ValueChanged<String> onRemarksChanged;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final narrow = constraints.maxWidth < 760;
          final name = Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                teacher.displayName,
                style: Theme.of(
                  context,
                ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 2),
              Text(
                teacher.employeeNumber,
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          );
          final status = SizedBox(
            width: narrow ? double.infinity : 180,
            child: DropdownButtonFormField<String>(
              initialValue: selectedStatus,
              decoration: const InputDecoration(labelText: 'Status'),
              items: const [
                DropdownMenuItem(value: 'PRESENT', child: Text('Present')),
                DropdownMenuItem(value: 'ABSENT', child: Text('Absent')),
                DropdownMenuItem(value: 'LATE', child: Text('Late')),
                DropdownMenuItem(value: 'HALF_DAY', child: Text('Half day')),
                DropdownMenuItem(value: 'LEAVE', child: Text('Leave')),
              ],
              onChanged: (value) => onStatusChanged(value ?? 'PRESENT'),
            ),
          );
          final remarkField = SizedBox(
            width: narrow ? double.infinity : 320,
            child: TextFormField(
              initialValue: remarks,
              decoration: const InputDecoration(labelText: 'Remarks'),
              maxLength: 500,
              onChanged: onRemarksChanged,
            ),
          );
          if (narrow) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                name,
                const SizedBox(height: 10),
                status,
                const SizedBox(height: 10),
                remarkField,
              ],
            );
          }
          return Row(
            children: [
              Expanded(child: name),
              const SizedBox(width: 16),
              status,
              const SizedBox(width: 16),
              remarkField,
            ],
          );
        },
      ),
    );
  }
}

class _SummaryChip extends StatelessWidget {
  const _SummaryChip({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
  });

  final String label;
  final int value;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return AppStatCard(
      label: label,
      value: value.toString(),
      icon: icon,
      color: color,
    );
  }
}

TeacherAttendanceRecordModel? _existingRecord(
  TeacherDailyAttendanceModel? daily,
  String teacherId,
) {
  if (daily == null) {
    return null;
  }
  for (final record in daily.records) {
    if (record.teacher.id == teacherId) {
      return record;
    }
  }
  return null;
}

DateTime _dateOnly(DateTime value) =>
    DateTime(value.year, value.month, value.day);

String _dateParam(DateTime value) {
  return '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';
}

String _dateLabel(DateTime value) {
  return '${value.day.toString().padLeft(2, '0')}/'
      '${value.month.toString().padLeft(2, '0')}/'
      '${value.year}';
}

String _message(Object error) {
  final text = error.toString();
  return text.startsWith('Exception: ') ? text.substring(11) : text;
}
