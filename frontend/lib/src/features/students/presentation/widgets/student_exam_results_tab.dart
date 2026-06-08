import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/widgets/app_data_table.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../exams/data/models/exam_models.dart';
import '../../data/models/student_models.dart';
import '../../data/models/student_profile_history_models.dart';
import '../../data/repositories/student_profile_repository_impl.dart';
import '../controllers/students_providers.dart';

class StudentExamResultsTab extends ConsumerStatefulWidget {
  const StudentExamResultsTab({required this.student, super.key});

  final StudentProfileModel student;

  @override
  ConsumerState<StudentExamResultsTab> createState() =>
      _StudentExamResultsTabState();
}

class _StudentExamResultsTabState extends ConsumerState<StudentExamResultsTab> {
  String? _academicYearId;
  String? _examTypeId;
  String? _examScheduleId;

  @override
  void initState() {
    super.initState();
    _academicYearId = widget.student.currentAssignment?.academicYearId;
  }

  @override
  Widget build(BuildContext context) {
    final assignment = _assignmentForFilter();
    final schedulesQuery = StudentProfileExamSchedulesQuery(
      academicYearId: _academicYearId,
      classId: assignment?.classId,
      sectionId: assignment?.sectionId,
    );
    final query = StudentExamResultsQuery(
      studentId: widget.student.id,
      academicYearId: _academicYearId,
      examTypeId: _examTypeId,
      examScheduleId: _examScheduleId,
    );
    final results = ref.watch(studentExamResultsProvider(query));

    return _ProfileTabSurface(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _filters(query, schedulesQuery),
          const SizedBox(height: 16),
          results.when(
            data: (value) => _resultsBody(value),
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () => ref.invalidate(studentExamResultsProvider(query)),
            ),
            loading: () => const AppLoadingState(label: 'Loading exam results'),
          ),
        ],
      ),
    );
  }

  Widget _filters(
    StudentExamResultsQuery query,
    StudentProfileExamSchedulesQuery schedulesQuery,
  ) {
    final years = ref.watch(academicYearsProvider);
    final examTypes = ref.watch(studentProfileExamTypesProvider);
    final schedules = ref.watch(
      studentProfileExamSchedulesProvider(schedulesQuery),
    );
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        SizedBox(
          width: 220,
          child: years.when(
            data: (items) => DropdownButtonFormField<String>(
              key: ValueKey('exam-year-${_academicYearId ?? ''}'),
              initialValue: _academicYearId ?? '',
              isExpanded: true,
              decoration: const InputDecoration(labelText: 'Academic year'),
              items: [
                const DropdownMenuItem(
                  value: '',
                  child: Text('All academic years'),
                ),
                for (final year in items)
                  DropdownMenuItem(value: year.id, child: Text(year.name)),
              ],
              onChanged: (value) => setState(() {
                _academicYearId = _blankToNull(value);
                _examScheduleId = null;
              }),
            ),
            error: (_, _) => const Text('Academic years unavailable'),
            loading: () => const LinearProgressIndicator(),
          ),
        ),
        SizedBox(
          width: 220,
          child: examTypes.when(
            data: (items) => DropdownButtonFormField<String>(
              key: ValueKey('exam-type-${_examTypeId ?? ''}'),
              initialValue: _examTypeId ?? '',
              isExpanded: true,
              decoration: const InputDecoration(labelText: 'Exam type'),
              items: [
                const DropdownMenuItem(value: '', child: Text('All types')),
                for (final item in items)
                  DropdownMenuItem(value: item.id, child: Text(item.name)),
              ],
              onChanged: (value) => setState(() {
                _examTypeId = _blankToNull(value);
                _examScheduleId = null;
              }),
            ),
            error: (_, _) => const Text('Exam types unavailable'),
            loading: () => const LinearProgressIndicator(),
          ),
        ),
        SizedBox(
          width: 280,
          child: schedules.when(
            data: (items) {
              final filtered = _examTypeId == null
                  ? items
                  : items
                        .where((item) => item.examTypeId == _examTypeId)
                        .toList();
              final selected =
                  filtered.any((item) => item.id == _examScheduleId)
                  ? _examScheduleId
                  : '';
              return DropdownButtonFormField<String>(
                key: ValueKey('exam-schedule-${selected ?? ''}'),
                initialValue: selected ?? '',
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Exam'),
                items: [
                  const DropdownMenuItem(value: '', child: Text('All exams')),
                  for (final item in filtered)
                    DropdownMenuItem(
                      value: item.id,
                      child: Text('${item.examTypeName} - ${item.subjectName}'),
                    ),
                ],
                onChanged: (value) {
                  final schedule = _scheduleById(filtered, value);
                  setState(() {
                    _examScheduleId = _blankToNull(value);
                    _examTypeId = schedule?.examTypeId ?? _examTypeId;
                  });
                },
              );
            },
            error: (_, _) => const Text('Exams unavailable'),
            loading: () => const LinearProgressIndicator(),
          ),
        ),
        OutlinedButton.icon(
          onPressed: () => ref.invalidate(studentExamResultsProvider(query)),
          icon: const Icon(Icons.refresh),
          label: const Text('Refresh'),
        ),
      ],
    );
  }

  Widget _resultsBody(StudentExamResultsModel model) {
    if (model.results.isEmpty) {
      return const _InlineMessage(message: 'No exam or result records found.');
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (final result in model.results) ...[
          _ResultPanel(
            result: result,
            onDownload: result.resultId.isEmpty
                ? null
                : () => _downloadReportCard(result),
          ),
          const SizedBox(height: 16),
        ],
      ],
    );
  }

  ClassAssignmentModel? _assignmentForFilter() {
    if (_academicYearId == null) {
      return widget.student.currentAssignment;
    }
    for (final assignment in widget.student.classAssignments) {
      if (assignment.academicYearId == _academicYearId) {
        return assignment;
      }
    }
    return widget.student.currentAssignment;
  }

  ExamScheduleModel? _scheduleById(
    List<ExamScheduleModel> schedules,
    String? id,
  ) {
    if (id == null || id.isEmpty) {
      return null;
    }
    for (final schedule in schedules) {
      if (schedule.id == id) {
        return schedule;
      }
    }
    return null;
  }

  Future<void> _downloadReportCard(StudentExamResultModel result) async {
    final response = await ref
        .read(studentProfileRepositoryProvider)
        .downloadReportCard(
          studentId: widget.student.id,
          resultId: result.resultId,
          academicYearId: result.academicYearId ?? _academicYearId,
        );
    if (!mounted) {
      return;
    }
    response.when(
      success: (_) => _snack('Report card downloaded.'),
      failure: (failure) => _snack(failure.message),
    );
  }

  void _snack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}

class _ResultPanel extends StatelessWidget {
  const _ResultPanel({required this.result, required this.onDownload});

  final StudentExamResultModel result;
  final VoidCallback? onDownload;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFFE2E8F0)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Wrap(
              spacing: 12,
              runSpacing: 8,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                SizedBox(
                  width: 280,
                  child: Text(
                    result.examName,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                _StatusBadge(status: result.passFailStatus),
                OutlinedButton.icon(
                  onPressed: onDownload,
                  icon: const Icon(Icons.download_outlined),
                  label: const Text('Report card'),
                ),
              ],
            ),
            const SizedBox(height: 14),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                _MetricTile(
                  label: 'Total marks',
                  value: result.totalMarks.toStringAsFixed(0),
                ),
                _MetricTile(
                  label: 'Obtained',
                  value: result.obtainedMarks.toStringAsFixed(0),
                ),
                _MetricTile(
                  label: 'Percentage',
                  value: '${result.percentage.toStringAsFixed(2)}%',
                ),
                _MetricTile(label: 'Grade', value: result.grade ?? '-'),
                _MetricTile(label: 'Status', value: result.passFailStatus),
                _MetricTile(
                  label: 'Rank',
                  value: result.rank?.toString() ?? '-',
                ),
              ],
            ),
            const SizedBox(height: 16),
            AppDataTable<StudentSubjectExamResultModel>(
              items: result.subjectResults,
              columns: [
                AppTableColumn(
                  label: 'Subject',
                  cellBuilder: (_, item) => Text(item.subjectName),
                ),
                AppTableColumn(
                  label: 'Max marks',
                  numeric: true,
                  cellBuilder: (_, item) =>
                      Text(item.maxMarks.toStringAsFixed(0)),
                ),
                AppTableColumn(
                  label: 'Marks',
                  numeric: true,
                  cellBuilder: (_, item) =>
                      Text(item.marksObtained?.toStringAsFixed(0) ?? '-'),
                ),
                AppTableColumn(
                  label: 'Grade',
                  cellBuilder: (_, item) => Text(item.grade ?? '-'),
                ),
                AppTableColumn(
                  label: 'Remarks',
                  cellBuilder: (_, item) => Text(_dash(item.remarks)),
                ),
                AppTableColumn(
                  label: 'Exam date',
                  cellBuilder: (_, item) => Text(_dateLabel(item.examDate)),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _ProfileTabSurface extends StatelessWidget {
  const _ProfileTabSurface({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: Colors.white,
          border: Border.all(color: const Color(0xFFE2E8F0)),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Padding(padding: const EdgeInsets.all(16), child: child),
      ),
    );
  }
}

class _MetricTile extends StatelessWidget {
  const _MetricTile({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 150,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: const Color(0xFFF8FAFC),
          border: Border.all(color: const Color(0xFFE2E8F0)),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                value,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 4),
              Text(label, overflow: TextOverflow.ellipsis),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(0xFFE0F2FE),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(status.replaceAll('_', ' ')),
    );
  }
}

class _InlineMessage extends StatelessWidget {
  const _InlineMessage({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 28),
      child: Row(
        children: [
          Icon(
            Icons.info_outline,
            color: Theme.of(context).colorScheme.outline,
          ),
          const SizedBox(width: 10),
          Expanded(child: Text(message)),
        ],
      ),
    );
  }
}

String _dateLabel(DateTime value) {
  final month = value.month.toString().padLeft(2, '0');
  final day = value.day.toString().padLeft(2, '0');
  return '${value.year}-$month-$day';
}

String? _blankToNull(String? value) {
  if (value == null || value.trim().isEmpty) {
    return null;
  }
  return value.trim();
}

String _dash(String? value) {
  return value == null || value.trim().isEmpty ? '-' : value.trim();
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
