import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/download/file_downloader.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../academic/data/models/academic_models.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/exam_models.dart';
import '../../data/repositories/exams_repository_impl.dart';

class ExamsPage extends ConsumerStatefulWidget {
  const ExamsPage({super.key});

  @override
  ConsumerState<ExamsPage> createState() => _ExamsPageState();
}

class _ExamsPageState extends ConsumerState<ExamsPage> {
  List<AcademicYearModel> _years = const [];
  List<AcademicClassModel> _classes = const [];
  List<AcademicDivisionModel> _sections = const [];
  List<ExamSubjectModel> _subjects = const [];
  List<ExamTypeModel> _types = const [];
  List<ExamScheduleModel> _schedules = const [];
  List<ExamStudentModel> _students = const [];
  List<StudentResultModel> _results = const [];
  final Map<String, TextEditingController> _markControllers = {};
  String? _yearId;
  String? _classId;
  String? _sectionId;
  String? _examTypeId;
  String? _subjectId;
  String? _scheduleId;
  bool _loading = false;
  bool _savingMarks = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(_initialLoad);
  }

  @override
  void dispose() {
    for (final controller in _markControllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AdminShell(
      title: 'Exams & Results',
      activeModuleId: 'exams',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: DefaultTabController(
        length: 4,
        child: Column(
          children: [
            const Material(
              color: Colors.white,
              child: TabBar(
                isScrollable: true,
                tabs: [
                  Tab(text: 'Exam Types'),
                  Tab(text: 'Schedules'),
                  Tab(text: 'Marks Entry'),
                  Tab(text: 'Results'),
                ],
              ),
            ),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator())
                  : TabBarView(
                      children: [
                        _typesTab(),
                        _schedulesTab(),
                        _marksTab(),
                        _resultsTab(),
                      ],
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _typesTab() {
    return Column(
      children: [
        _toolbar([
          AppButton(
            label: 'Add exam type',
            icon: Icons.add,
            onPressed: () => _showTypeDialog(),
          ),
          OutlinedButton.icon(
            onPressed: _loadTypes,
            icon: const Icon(Icons.refresh),
            label: const Text('Refresh'),
          ),
        ]),
        Expanded(
          child: _types.isEmpty
              ? const Center(child: Text('No exam types found.'))
              : ListView.separated(
                  padding: const EdgeInsets.all(18),
                  itemCount: _types.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 10),
                  itemBuilder: (context, index) {
                    final type = _types[index];
                    return Card(
                      child: ListTile(
                        leading: const Icon(Icons.assignment_outlined),
                        title: Text(type.name),
                        subtitle: Text(
                          '${type.code} | ${type.active ? 'Active' : 'Inactive'}',
                        ),
                        trailing: PopupMenuButton<String>(
                          onSelected: (action) {
                            if (action == 'edit') {
                              _showTypeDialog(type: type);
                            } else {
                              _deleteType(type.id);
                            }
                          },
                          itemBuilder: (context) => const [
                            PopupMenuItem(value: 'edit', child: Text('Edit')),
                            PopupMenuItem(
                              value: 'delete',
                              child: Text('Delete'),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _schedulesTab() {
    return Column(
      children: [
        _hierarchyFilters(showExamType: true, showSubject: false),
        _toolbar([
          AppButton(
            label: 'Add schedule',
            icon: Icons.add,
            onPressed: () => _showScheduleDialog(),
          ),
          OutlinedButton.icon(
            onPressed: _loadSchedules,
            icon: const Icon(Icons.refresh),
            label: const Text('Refresh'),
          ),
        ]),
        Expanded(
          child: _schedules.isEmpty
              ? const Center(child: Text('No exam schedules found.'))
              : ListView.separated(
                  padding: const EdgeInsets.all(18),
                  itemCount: _schedules.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 10),
                  itemBuilder: (context, index) {
                    final schedule = _schedules[index];
                    return Card(
                      child: ListTile(
                        leading: const Icon(Icons.event_note_outlined),
                        title: Text(
                          schedule.examName.isEmpty
                              ? schedule.examTypeName
                              : schedule.examName,
                        ),
                        subtitle: Text(
                          '${schedule.examTypeName} | ${schedule.subjects.length} subject${schedule.subjects.length == 1 ? '' : 's'} | ${schedule.status}\n'
                          '${schedule.subjects.map(_scheduleSubjectSummary).join(' | ')}',
                        ),
                        trailing: PopupMenuButton<String>(
                          onSelected: (action) {
                            if (action == 'edit') {
                              _showScheduleDialog(schedule: schedule);
                            } else {
                              _deleteSchedule(schedule.id);
                            }
                          },
                          itemBuilder: (context) => const [
                            PopupMenuItem(value: 'edit', child: Text('Edit')),
                            PopupMenuItem(
                              value: 'delete',
                              child: Text('Delete'),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _marksTab() {
    final selectedSubject = _selectedScheduleSubject;
    final passingMarks = selectedSubject?.passingMarks;
    return Column(
      children: [
        _hierarchyFilters(showExamType: false, showSubject: false),
        _toolbar([
          _dropdown(
            width: 300,
            label: 'Schedule',
            value: _scheduleId,
            items: _schedules.map(
              (item) => MapEntry(
                item.id,
                item.examName.isEmpty
                    ? item.examTypeName
                    : '${item.examName} - ${item.examTypeName}',
              ),
            ),
            onChanged: (value) => setState(() {
              _scheduleId = value;
              _subjectId = _selectedSchedule?.subjects.isEmpty ?? true
                  ? null
                  : _selectedSchedule!.subjects.first.subjectId;
            }),
          ),
          _dropdown(
            width: 240,
            label: 'Subject',
            value: _subjectId,
            items: (_selectedSchedule?.subjects ?? const []).map(
              (item) => MapEntry(item.subjectId, item.subjectName),
            ),
            onChanged: (value) => setState(() => _subjectId = value),
          ),
          _readonlyMetric(
            'Max',
            selectedSubject == null
                ? '-'
                : selectedSubject.maxMarks.toStringAsFixed(0),
          ),
          _readonlyMetric(
            'Passing',
            passingMarks == null ? '-' : passingMarks.toStringAsFixed(0),
          ),
          AppButton(
            label: 'Load students',
            icon: Icons.group_outlined,
            onPressed: _loadMarksStudents,
          ),
          AppButton(
            label: 'Save marks',
            icon: Icons.save_outlined,
            onPressed: _savingMarks ? null : _saveMarks,
            isLoading: _savingMarks,
          ),
        ]),
        Expanded(
          child: _students.isEmpty
              ? const Center(child: Text('No students loaded.'))
              : ListView.separated(
                  padding: const EdgeInsets.all(18),
                  itemCount: _students.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 10),
                  itemBuilder: (context, index) {
                    final student = _students[index];
                    final controller = _markControllers.putIfAbsent(
                      student.studentId,
                      () => TextEditingController(),
                    );
                    return Card(
                      child: ListTile(
                        title: Text(student.displayName),
                        subtitle: Text(
                          '${student.rollNumber ?? '-'} | ${student.admissionNumber}',
                        ),
                        trailing: SizedBox(
                          width: 120,
                          child: TextField(
                            controller: controller,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: 'Marks',
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _resultsTab() {
    return Column(
      children: [
        _hierarchyFilters(showExamType: true, showSubject: false),
        _toolbar([
          AppButton(
            label: 'Generate result',
            icon: Icons.calculate_outlined,
            onPressed: _generateResults,
          ),
        ]),
        Expanded(
          child: _results.isEmpty
              ? const Center(child: Text('No results generated.'))
              : ListView.separated(
                  padding: const EdgeInsets.all(18),
                  itemCount: _results.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 10),
                  itemBuilder: (context, index) {
                    final result = _results[index];
                    return Card(
                      child: ListTile(
                        leading: CircleAvatar(
                          child: Text(result.rank?.toString() ?? '-'),
                        ),
                        title: Text(result.studentName),
                        subtitle: Text(
                          '${result.percentage.toStringAsFixed(2)}% | Grade ${result.grade} | ${result.passed ? 'Pass' : 'Fail'}',
                        ),
                        trailing: IconButton(
                          tooltip: 'Download report card',
                          icon: const Icon(Icons.download_outlined),
                          onPressed: () => _downloadReport(result.studentId),
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _hierarchyFilters({
    required bool showExamType,
    required bool showSubject,
  }) {
    return _toolbar([
      _dropdown(
        width: 220,
        label: 'Academic year',
        value: _yearId,
        items: _years.map((item) => MapEntry(item.id, item.name)),
        onChanged: (value) {
          setState(() {
            _yearId = value;
            _classId = null;
            _sectionId = null;
            _classes = const [];
            _sections = const [];
            _subjects = const [];
            _schedules = const [];
            _scheduleId = null;
            _subjectId = null;
          });
          if (value != null) {
            _loadClasses(value);
          }
        },
      ),
      _dropdown(
        width: 190,
        label: 'Class',
        value: _classId,
        items: _classes.map((item) => MapEntry(item.id, item.name)),
        onChanged: (value) {
          setState(() {
            _classId = value;
            _sectionId = null;
            _sections = const [];
            _subjects = const [];
            _schedules = const [];
            _scheduleId = null;
            _subjectId = null;
          });
          if (value != null) {
            _loadSections(value);
          }
        },
      ),
      _dropdown(
        width: 190,
        label: 'Division',
        value: _sectionId,
        items: _sections.map((item) => MapEntry(item.id, item.name)),
        onChanged: (value) {
          setState(() {
            _sectionId = value;
            _scheduleId = null;
            _subjectId = null;
            _schedules = const [];
          });
          if (_classId != null && value != null) {
            _loadSubjects(_classId!, value);
            _loadSchedules();
          }
        },
      ),
      if (showExamType)
        _dropdown(
          width: 220,
          label: 'Exam type',
          value: _examTypeId,
          items: _types.map((item) => MapEntry(item.id, item.name)),
          onChanged: (value) => setState(() => _examTypeId = value),
        ),
      if (showSubject)
        _dropdown(
          width: 220,
          label: 'Subject',
          value: _subjectId,
          items: _subjects.map(
            (item) => MapEntry(item.subjectId, item.subjectName),
          ),
          onChanged: (value) => setState(() => _subjectId = value),
        ),
    ]);
  }

  Widget _toolbar(List<Widget> children) {
    return Material(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Wrap(
          spacing: 12,
          runSpacing: 12,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: children,
        ),
      ),
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

  Widget _readonlyMetric(String label, String value) {
    return SizedBox(
      width: 110,
      child: InputDecorator(
        decoration: InputDecoration(labelText: label),
        child: Text(value),
      ),
    );
  }

  Future<void> _initialLoad() async {
    setState(() => _loading = true);
    await Future.wait([_loadYears(), _loadTypes()]);
    if (mounted) {
      setState(() => _loading = false);
    }
  }

  Future<void> _loadYears() async {
    final result = await ref.read(examsRepositoryProvider).years();
    result.when(
      success: (years) => setState(() => _years = years),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _loadClasses(String yearId) async {
    final result = await ref.read(examsRepositoryProvider).classes(yearId);
    result.when(
      success: (classes) => setState(() => _classes = classes),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _loadSections(String classId) async {
    final result = await ref.read(examsRepositoryProvider).sections(classId);
    result.when(
      success: (sections) => setState(() => _sections = sections),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _loadSubjects(String classId, String sectionId) async {
    final result = await ref
        .read(examsRepositoryProvider)
        .subjects(classId, sectionId);
    result.when(
      success: (subjects) => setState(() => _subjects = subjects),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _loadTypes() async {
    final result = await ref.read(examsRepositoryProvider).types();
    result.when(
      success: (types) => setState(() => _types = types),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _loadSchedules() async {
    final result = await ref
        .read(examsRepositoryProvider)
        .schedules(
          academicYearId: _yearId,
          classId: _classId,
          sectionId: _sectionId,
        );
    result.when(
      success: (schedules) => setState(() {
        _schedules = schedules;
        if (_scheduleId != null &&
            !schedules.any((schedule) => schedule.id == _scheduleId)) {
          _scheduleId = null;
          _subjectId = null;
        }
      }),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _showTypeDialog({ExamTypeModel? type}) async {
    final code = TextEditingController(text: type?.code ?? '');
    final name = TextEditingController(text: type?.name ?? '');
    final description = TextEditingController(text: type?.description ?? '');
    bool active = type?.active ?? true;
    await showDialog<void>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(type == null ? 'Add exam type' : 'Edit exam type'),
          content: StatefulBuilder(
            builder: (context, setDialogState) {
              return SizedBox(
                width: 420,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    TextField(
                      controller: code,
                      decoration: const InputDecoration(labelText: 'Code'),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: name,
                      decoration: const InputDecoration(labelText: 'Name'),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: description,
                      decoration: const InputDecoration(
                        labelText: 'Description',
                      ),
                    ),
                    const SizedBox(height: 12),
                    SwitchListTile(
                      value: active,
                      onChanged: (value) =>
                          setDialogState(() => active = value),
                      title: const Text('Active'),
                    ),
                  ],
                ),
              );
            },
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            FilledButton.icon(
              onPressed: () async {
                final result = await ref
                    .read(examsRepositoryProvider)
                    .saveType(type?.id, {
                      'code': code.text.trim(),
                      'name': name.text.trim(),
                      'description': _blankToNull(description.text),
                      'displayOrder': 0,
                      'active': active,
                    });
                if (!context.mounted) {
                  return;
                }
                result.when(
                  success: (_) {
                    Navigator.of(context).pop();
                    _loadTypes();
                  },
                  failure: (failure) => _snack(failure.message),
                );
              },
              icon: const Icon(Icons.save_outlined),
              label: const Text('Save'),
            ),
          ],
        );
      },
    );
    code.dispose();
    name.dispose();
    description.dispose();
  }

  Future<void> _deleteType(String id) async {
    final result = await ref.read(examsRepositoryProvider).deleteType(id);
    result.when(
      success: (_) => _loadTypes(),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _deleteSchedule(String id) async {
    final result = await ref.read(examsRepositoryProvider).deleteSchedule(id);
    result.when(
      success: (_) => _loadSchedules(),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _showScheduleDialog({ExamScheduleModel? schedule}) async {
    if (schedule == null &&
        (_yearId == null ||
            _classId == null ||
            _sectionId == null ||
            _examTypeId == null)) {
      _snack('Select academic year, class, division, and exam type.');
      return;
    }
    if (_subjects.isEmpty && _classId != null && _sectionId != null) {
      await _loadSubjects(_classId!, _sectionId!);
      if (!mounted) {
        return;
      }
    }
    final examName = TextEditingController(text: schedule?.examName ?? '');
    final rows = schedule == null
        ? <_ScheduleSubjectDraft>[]
        : schedule.subjects.map(_ScheduleSubjectDraft.fromModel).toList();
    String status = schedule?.status ?? 'SCHEDULED';
    String? subjectToAdd;
    bool saving = false;
    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: Text(schedule == null ? 'Add schedule' : 'Edit schedule'),
              content: SizedBox(
                width: 760,
                child: SingleChildScrollView(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      TextField(
                        controller: examName,
                        decoration: const InputDecoration(
                          labelText: 'Exam name',
                        ),
                      ),
                      const SizedBox(height: 12),
                      DropdownButtonFormField<String>(
                        initialValue: status,
                        decoration: const InputDecoration(labelText: 'Status'),
                        items: const [
                          DropdownMenuItem(
                            value: 'SCHEDULED',
                            child: Text('Scheduled'),
                          ),
                          DropdownMenuItem(
                            value: 'COMPLETED',
                            child: Text('Completed'),
                          ),
                          DropdownMenuItem(
                            value: 'CANCELLED',
                            child: Text('Cancelled'),
                          ),
                        ],
                        onChanged: (value) => status = value ?? status,
                      ),
                      const SizedBox(height: 16),
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: DropdownButtonFormField<String>(
                              key: ValueKey(
                                'schedule-subject-${rows.length}-$subjectToAdd',
                              ),
                              initialValue: subjectToAdd,
                              isExpanded: true,
                              decoration: const InputDecoration(
                                labelText: 'Subject',
                              ),
                              items: [
                                for (final subject in _subjects.where(
                                  (subject) => !rows.any(
                                    (row) => row.subjectId == subject.subjectId,
                                  ),
                                ))
                                  DropdownMenuItem(
                                    value: subject.subjectId,
                                    child: Text(subject.subjectName),
                                  ),
                              ],
                              onChanged: (value) {
                                setDialogState(() => subjectToAdd = value);
                              },
                            ),
                          ),
                          const SizedBox(width: 12),
                          FilledButton.icon(
                            onPressed: subjectToAdd == null
                                ? null
                                : () {
                                    final subject = _subjects.firstWhere(
                                      (item) => item.subjectId == subjectToAdd,
                                    );
                                    setDialogState(() {
                                      rows.add(
                                        _ScheduleSubjectDraft.fromSubject(
                                          subject,
                                        ),
                                      );
                                      subjectToAdd = null;
                                    });
                                  },
                            icon: const Icon(Icons.add),
                            label: const Text('Add subject'),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      if (rows.isEmpty)
                        const Padding(
                          padding: EdgeInsets.symmetric(vertical: 20),
                          child: Center(child: Text('No subjects selected.')),
                        )
                      else
                        Column(
                          children: [
                            for (final row in rows)
                              _scheduleSubjectRow(
                                context,
                                row,
                                onDateChanged: (date) {
                                  setDialogState(() => row.examDate = date);
                                },
                                onRemove: () {
                                  setDialogState(() {
                                    rows.remove(row);
                                    row.dispose();
                                  });
                                },
                              ),
                          ],
                        ),
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: saving ? null : () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
                FilledButton.icon(
                  onPressed: saving
                      ? null
                      : () async {
                          final payload = _schedulePayload(
                            schedule: schedule,
                            examName: examName.text,
                            status: status,
                            rows: rows,
                          );
                          if (payload == null) {
                            return;
                          }
                          setDialogState(() => saving = true);
                          final result = await ref
                              .read(examsRepositoryProvider)
                              .saveSchedule(schedule?.id, payload);
                          setDialogState(() => saving = false);
                          if (!context.mounted) {
                            return;
                          }
                          result.when(
                            success: (_) {
                              Navigator.of(context).pop();
                              _snack('Exam schedule saved.');
                              _loadSchedules();
                            },
                            failure: (failure) => _snack(failure.message),
                          );
                        },
                  icon: saving
                      ? const SizedBox.square(
                          dimension: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.save_outlined),
                  label: const Text('Save'),
                ),
              ],
            );
          },
        );
      },
    );
    examName.dispose();
    for (final row in rows) {
      row.dispose();
    }
  }

  Widget _scheduleSubjectRow(
    BuildContext context,
    _ScheduleSubjectDraft row, {
    required ValueChanged<DateTime> onDateChanged,
    required VoidCallback onRemove,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  row.subjectName,
                  style: Theme.of(context).textTheme.titleSmall,
                ),
              ),
              IconButton(
                tooltip: 'Remove subject',
                onPressed: onRemove,
                icon: const Icon(Icons.delete_outline),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              SizedBox(
                width: 150,
                child: OutlinedButton.icon(
                  onPressed: () async {
                    final picked = await showDatePicker(
                      context: context,
                      initialDate: row.examDate,
                      firstDate: DateTime(2020),
                      lastDate: DateTime(2100),
                    );
                    if (picked != null) {
                      onDateChanged(picked);
                    }
                  },
                  icon: const Icon(Icons.calendar_today_outlined),
                  label: Text(_dateLabel(row.examDate)),
                ),
              ),
              SizedBox(
                width: 90,
                child: TextField(
                  controller: row.startTime,
                  keyboardType: TextInputType.datetime,
                  decoration: const InputDecoration(
                    labelText: 'Start',
                    hintText: 'HH:mm',
                  ),
                ),
              ),
              SizedBox(
                width: 90,
                child: TextField(
                  controller: row.endTime,
                  keyboardType: TextInputType.datetime,
                  decoration: const InputDecoration(
                    labelText: 'End',
                    hintText: 'HH:mm',
                  ),
                ),
              ),
              SizedBox(
                width: 130,
                child: TextField(
                  controller: row.room,
                  decoration: const InputDecoration(labelText: 'Room'),
                ),
              ),
              SizedBox(
                width: 110,
                child: TextField(
                  controller: row.maxMarks,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: 'Max marks'),
                ),
              ),
              SizedBox(
                width: 110,
                child: TextField(
                  controller: row.passingMarks,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: 'Passing'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Map<String, dynamic>? _schedulePayload({
    required ExamScheduleModel? schedule,
    required String examName,
    required String status,
    required List<_ScheduleSubjectDraft> rows,
  }) {
    final academicYearId = schedule?.academicYearId ?? _yearId;
    final classId = schedule?.classId ?? _classId;
    final sectionId = schedule?.sectionId ?? _sectionId;
    final examTypeId = schedule?.examTypeId ?? _examTypeId;
    if (academicYearId == null ||
        classId == null ||
        sectionId == null ||
        examTypeId == null) {
      _snack('Select academic year, class, division, and exam type.');
      return null;
    }
    if (_blankToNull(examName) == null) {
      _snack('Enter exam name.');
      return null;
    }
    if (rows.isEmpty) {
      _snack('Select at least one subject.');
      return null;
    }
    final subjects = <Map<String, dynamic>>[];
    for (final row in rows) {
      final maxMarks = double.tryParse(row.maxMarks.text.trim());
      final passingMarks = double.tryParse(row.passingMarks.text.trim());
      final startTime = _blankToNull(row.startTime.text);
      final endTime = _blankToNull(row.endTime.text);
      final room = _blankToNull(row.room.text);
      if (maxMarks == null || maxMarks <= 0) {
        _snack('Enter valid max marks for ${row.subjectName}.');
        return null;
      }
      if (passingMarks != null && passingMarks > maxMarks) {
        _snack('Passing marks cannot exceed max marks for ${row.subjectName}.');
        return null;
      }
      if (!_validTime(startTime)) {
        _snack('Enter start time as HH:mm for ${row.subjectName}.');
        return null;
      }
      if (!_validTime(endTime)) {
        _snack('Enter end time as HH:mm for ${row.subjectName}.');
        return null;
      }
      final startMinutes = _timeMinutes(startTime);
      final endMinutes = _timeMinutes(endTime);
      if (startMinutes != null &&
          endMinutes != null &&
          endMinutes <= startMinutes) {
        _snack('End time must be after start time for ${row.subjectName}.');
        return null;
      }
      subjects.add({
        'subjectId': row.subjectId,
        'examDate': _dateLabel(row.examDate),
        if (startTime != null) 'startTime': startTime,
        if (endTime != null) 'endTime': endTime,
        if (room != null) 'room': room,
        'maxMarks': maxMarks,
        if (passingMarks != null) 'passingMarks': passingMarks,
      });
    }
    return {
      'academicYearId': academicYearId,
      'classId': classId,
      'sectionId': sectionId,
      'examTypeId': examTypeId,
      'examName': examName.trim(),
      'status': status,
      'subjects': subjects,
    };
  }

  Future<void> _loadMarksStudents() async {
    if (_yearId == null ||
        _classId == null ||
        _sectionId == null ||
        _scheduleId == null ||
        _subjectId == null) {
      _snack('Select academic year, class, division, schedule, and subject.');
      return;
    }
    final repository = ref.read(examsRepositoryProvider);
    final studentsResult = await repository.students(
      academicYearId: _yearId!,
      classId: _classId!,
      sectionId: _sectionId!,
    );
    final marksResult = await repository.marks({
      'academicYearId': _yearId,
      'classId': _classId,
      'sectionId': _sectionId,
      'examScheduleId': _scheduleId,
      'subjectId': _subjectId,
    });
    studentsResult.when(
      success: (students) {
        setState(() => _students = students);
        for (final controller in _markControllers.values) {
          controller.dispose();
        }
        _markControllers.clear();
        for (final student in students) {
          _markControllers[student.studentId] = TextEditingController();
        }
      },
      failure: (failure) => _snack(failure.message),
    );
    marksResult.when(
      success: (marks) {
        for (final mark in marks.records) {
          _markControllers[mark.student.studentId]?.text = mark.marksObtained
              .toStringAsFixed(0);
        }
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _saveMarks() async {
    if (_yearId == null ||
        _classId == null ||
        _sectionId == null ||
        _scheduleId == null ||
        _subjectId == null) {
      _snack('Select academic year, class, division, schedule, and subject.');
      return;
    }
    final scheduleSubject = _selectedScheduleSubject;
    if (scheduleSubject == null) {
      _snack('Selected subject is not part of the selected schedule.');
      return;
    }
    if (_students.isEmpty) {
      _snack('Load students before saving marks.');
      return;
    }
    final records = <Map<String, dynamic>>[];
    for (final student in _students) {
      final marks = double.tryParse(
        _markControllers[student.studentId]?.text.trim() ?? '',
      );
      if (marks == null || marks < 0) {
        _snack('Enter valid marks for ${student.displayName}.');
        return;
      }
      if (marks > scheduleSubject.maxMarks) {
        _snack(
          'Marks for ${student.displayName} cannot exceed ${scheduleSubject.maxMarks.toStringAsFixed(0)}.',
        );
        return;
      }
      records.add({'studentId': student.studentId, 'marksObtained': marks});
    }
    setState(() => _savingMarks = true);
    final result = await ref.read(examsRepositoryProvider).saveMarks({
      'academicYearId': _yearId,
      'classId': _classId,
      'sectionId': _sectionId,
      'examScheduleId': _scheduleId,
      'subjectId': _subjectId,
      'records': records,
    });
    if (mounted) {
      setState(() => _savingMarks = false);
    }
    result.when(
      success: (_) => _snack('Marks saved successfully.'),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _generateResults() async {
    if (_yearId == null || _classId == null || _sectionId == null) {
      _snack('Select academic year, class, and division.');
      return;
    }
    final result = await ref.read(examsRepositoryProvider).generateResults({
      'academicYearId': _yearId,
      'classId': _classId,
      'sectionId': _sectionId,
      if (_examTypeId != null) 'examTypeId': _examTypeId,
    });
    result.when(
      success: (results) => setState(() => _results = results),
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _downloadReport(String studentId) async {
    if (_yearId == null) {
      return;
    }
    final result = await ref
        .read(examsRepositoryProvider)
        .reportCard(studentId, _yearId!);
    result.when(
      success: (bytes) async {
        await downloadBytes(bytes, 'report-card.csv', 'text/csv');
        _snack('Report card downloaded.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  void _snack(String message) {
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  ExamScheduleModel? get _selectedSchedule {
    if (_scheduleId == null) {
      return null;
    }
    for (final schedule in _schedules) {
      if (schedule.id == _scheduleId) {
        return schedule;
      }
    }
    return null;
  }

  ExamScheduleSubjectModel? get _selectedScheduleSubject {
    return _selectedSchedule?.subjectById(_subjectId);
  }
}

class _ScheduleSubjectDraft {
  _ScheduleSubjectDraft({
    required this.subjectId,
    required this.subjectName,
    required this.examDate,
    required String startTime,
    required String endTime,
    required String room,
    required String maxMarks,
    required String passingMarks,
  }) : startTime = TextEditingController(text: startTime),
       endTime = TextEditingController(text: endTime),
       room = TextEditingController(text: room),
       maxMarks = TextEditingController(text: maxMarks),
       passingMarks = TextEditingController(text: passingMarks);

  factory _ScheduleSubjectDraft.fromSubject(ExamSubjectModel subject) {
    return _ScheduleSubjectDraft(
      subjectId: subject.subjectId,
      subjectName: subject.subjectName,
      examDate: DateTime.now(),
      startTime: '',
      endTime: '',
      room: '',
      maxMarks: '',
      passingMarks: '',
    );
  }

  factory _ScheduleSubjectDraft.fromModel(ExamScheduleSubjectModel subject) {
    return _ScheduleSubjectDraft(
      subjectId: subject.subjectId,
      subjectName: subject.subjectName,
      examDate: subject.examDate,
      startTime: _shortTime(subject.startTime),
      endTime: _shortTime(subject.endTime),
      room: subject.room ?? '',
      maxMarks: subject.maxMarks == 0
          ? ''
          : subject.maxMarks.toStringAsFixed(0),
      passingMarks: subject.passingMarks == null
          ? ''
          : subject.passingMarks!.toStringAsFixed(0),
    );
  }

  final String subjectId;
  final String subjectName;
  DateTime examDate;
  final TextEditingController startTime;
  final TextEditingController endTime;
  final TextEditingController room;
  final TextEditingController maxMarks;
  final TextEditingController passingMarks;

  void dispose() {
    startTime.dispose();
    endTime.dispose();
    room.dispose();
    maxMarks.dispose();
    passingMarks.dispose();
  }
}

String _dateLabel(DateTime date) {
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}

String? _blankToNull(String value) {
  return value.trim().isEmpty ? null : value.trim();
}

String _shortTime(String? value) {
  if (value == null || value.isEmpty) {
    return '';
  }
  return value.length >= 5 ? value.substring(0, 5) : value;
}

bool _validTime(String? value) {
  return value == null || _timeMinutes(value) != null;
}

int? _timeMinutes(String? value) {
  if (value == null) {
    return null;
  }
  final parts = value.split(':');
  if (parts.length < 2) {
    return null;
  }
  final hour = int.tryParse(parts[0]);
  final minute = int.tryParse(parts[1]);
  if (hour == null || minute == null) {
    return null;
  }
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
    return null;
  }
  return hour * 60 + minute;
}

String _scheduleSubjectSummary(ExamScheduleSubjectModel subject) {
  final passing = subject.passingMarks == null
      ? ''
      : ', Pass ${subject.passingMarks!.toStringAsFixed(0)}';
  final start = _shortTime(subject.startTime);
  final end = _shortTime(subject.endTime);
  final time = start.isEmpty && end.isEmpty
      ? ''
      : ', ${start.isEmpty ? '-' : start}-${end.isEmpty ? '-' : end}';
  final room = _blankToNull(subject.room ?? '') == null
      ? ''
      : ', ${subject.room!.trim()}';
  return '${subject.subjectName} ${_dateLabel(subject.examDate)}$time$room Max ${subject.maxMarks.toStringAsFixed(0)}$passing';
}
