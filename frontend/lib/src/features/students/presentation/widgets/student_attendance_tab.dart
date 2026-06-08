import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/widgets/app_data_table.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../data/models/student_models.dart';
import '../../data/models/student_profile_history_models.dart';
import '../../data/repositories/student_profile_repository_impl.dart';
import '../controllers/students_providers.dart';

class StudentAttendanceTab extends ConsumerStatefulWidget {
  const StudentAttendanceTab({required this.student, super.key});

  final StudentProfileModel student;

  @override
  ConsumerState<StudentAttendanceTab> createState() =>
      _StudentAttendanceTabState();
}

class _StudentAttendanceTabState extends ConsumerState<StudentAttendanceTab> {
  String? _academicYearId;
  DateTimeRange? _dateRange;
  String? _status;
  int _page = 0;
  static const _pageSize = 20;

  @override
  void initState() {
    super.initState();
    _academicYearId = widget.student.currentAssignment?.academicYearId;
  }

  @override
  Widget build(BuildContext context) {
    final query = _query();
    final history = ref.watch(studentAttendanceHistoryProvider(query));

    return _ProfileTabSurface(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _filters(context, query),
          const SizedBox(height: 16),
          history.when(
            data: (value) => _historyBody(value, query),
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () =>
                  ref.invalidate(studentAttendanceHistoryProvider(query)),
            ),
            loading: () =>
                const AppLoadingState(label: 'Loading attendance history'),
          ),
        ],
      ),
    );
  }

  StudentAttendanceHistoryQuery _query() {
    return StudentAttendanceHistoryQuery(
      studentId: widget.student.id,
      academicYearId: _academicYearId,
      fromDate: _dateRange?.start,
      toDate: _dateRange?.end,
      status: _status,
      page: _page,
      size: _pageSize,
    );
  }

  Widget _filters(BuildContext context, StudentAttendanceHistoryQuery query) {
    final years = ref.watch(academicYearsProvider);
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        SizedBox(
          width: 220,
          child: years.when(
            data: (items) => DropdownButtonFormField<String>(
              key: ValueKey('attendance-year-${_academicYearId ?? ''}'),
              initialValue: _academicYearId ?? '',
              isExpanded: true,
              decoration: const InputDecoration(labelText: 'Academic Year'),
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
                _page = 0;
              }),
            ),
            error: (_, _) => const Text('Academic years unavailable'),
            loading: () => const LinearProgressIndicator(),
          ),
        ),
        OutlinedButton.icon(
          onPressed: _pickDateRange,
          icon: const Icon(Icons.date_range_outlined),
          label: Text(_dateRangeLabel),
        ),
        SizedBox(
          width: 180,
          child: DropdownButtonFormField<String>(
            key: ValueKey('attendance-status-${_status ?? ''}'),
            initialValue: _status ?? '',
            isExpanded: true,
            decoration: const InputDecoration(labelText: 'Status'),
            items: const [
              DropdownMenuItem(value: '', child: Text('All statuses')),
              DropdownMenuItem(value: 'PRESENT', child: Text('Present')),
              DropdownMenuItem(value: 'ABSENT', child: Text('Absent')),
              DropdownMenuItem(value: 'LATE', child: Text('Late')),
              DropdownMenuItem(value: 'HALF_DAY', child: Text('Half day')),
              DropdownMenuItem(value: 'LEAVE', child: Text('Leave')),
            ],
            onChanged: (value) => setState(() {
              _status = _blankToNull(value);
              _page = 0;
            }),
          ),
        ),
        OutlinedButton.icon(
          onPressed: () =>
              ref.invalidate(studentAttendanceHistoryProvider(query)),
          icon: const Icon(Icons.refresh),
          label: const Text('Refresh'),
        ),
        OutlinedButton.icon(
          onPressed: _export,
          icon: const Icon(Icons.download_outlined),
          label: const Text('Export'),
        ),
      ],
    );
  }

  Widget _historyBody(
    StudentAttendanceHistoryModel history,
    StudentAttendanceHistoryQuery query,
  ) {
    final page = history.attendanceRecords;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _summary(history),
        const SizedBox(height: 18),
        if (page.content.isEmpty)
          const _InlineMessage(message: 'No attendance records found.')
        else ...[
          AppDataTable<StudentAttendanceHistoryRecordModel>(
            items: page.content,
            columns: [
              AppTableColumn(
                label: 'Date',
                cellBuilder: (_, item) => Text(_dateLabel(item.attendanceDate)),
              ),
              AppTableColumn(
                label: 'Status',
                cellBuilder: (_, item) => _StatusBadge(status: item.status),
              ),
              AppTableColumn(
                label: 'Remarks',
                cellBuilder: (_, item) => Text(_dash(item.remarks)),
              ),
              AppTableColumn(
                label: 'Marked by',
                cellBuilder: (_, item) => Text(_dash(item.markedBy)),
              ),
              AppTableColumn(
                label: 'Updated date',
                cellBuilder: (_, item) => Text(_dateTimeLabel(item.updatedAt)),
              ),
            ],
          ),
          const SizedBox(height: 12),
          _pagination(page.page, page.totalPages),
        ],
      ],
    );
  }

  Widget _summary(StudentAttendanceHistoryModel history) {
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      children: [
        _MetricTile(
          label: 'Total working days',
          value: history.totalWorkingDays.toString(),
        ),
        _MetricTile(label: 'Present', value: history.presentDays.toString()),
        _MetricTile(label: 'Absent', value: history.absentDays.toString()),
        _MetricTile(label: 'Late', value: history.lateDays.toString()),
        _MetricTile(label: 'Half day', value: history.halfDays.toString()),
        _MetricTile(label: 'Leave', value: history.leaveDays.toString()),
        _MetricTile(
          label: 'Attendance percentage',
          value: '${history.attendancePercentage.toStringAsFixed(2)}%',
        ),
      ],
    );
  }

  Widget _pagination(int page, int totalPages) {
    final pageCount = totalPages == 0 ? 1 : totalPages;
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        OutlinedButton.icon(
          onPressed: page == 0 ? null : () => setState(() => _page--),
          icon: const Icon(Icons.chevron_left),
          label: const Text('Previous'),
        ),
        const SizedBox(width: 12),
        Text('Page ${page + 1} of $pageCount'),
        const SizedBox(width: 12),
        OutlinedButton.icon(
          onPressed: page + 1 >= totalPages
              ? null
              : () => setState(() => _page++),
          icon: const Icon(Icons.chevron_right),
          label: const Text('Next'),
        ),
      ],
    );
  }

  Future<void> _pickDateRange() async {
    final picked = await showDateRangePicker(
      context: context,
      initialDateRange: _dateRange,
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (picked != null) {
      setState(() {
        _dateRange = picked;
        _page = 0;
      });
    }
  }

  Future<void> _export() async {
    final result = await ref
        .read(studentProfileRepositoryProvider)
        .exportAttendanceHistory(
          studentId: widget.student.id,
          academicYearId: _academicYearId,
          fromDate: _dateRange?.start,
          toDate: _dateRange?.end,
          status: _status,
        );
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) => _snack('Attendance history downloaded.'),
      failure: (failure) => _snack(failure.message),
    );
  }

  String get _dateRangeLabel {
    if (_dateRange == null) {
      return 'Date range';
    }
    return '${_dateLabel(_dateRange!.start)} - ${_dateLabel(_dateRange!.end)}';
  }

  void _snack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
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
      width: 170,
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

String _dateTimeLabel(DateTime? value) {
  if (value == null) {
    return '-';
  }
  return _dateLabel(value);
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
