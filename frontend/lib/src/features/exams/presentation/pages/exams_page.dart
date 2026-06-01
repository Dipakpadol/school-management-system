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
  DateTime _examDate = DateTime.now();
  bool _loading = false;

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
                        subtitle: Text('${type.code} | ${type.active ? 'Active' : 'Inactive'}'),
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
                            PopupMenuItem(value: 'delete', child: Text('Delete')),
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
        _hierarchyFilters(showExamType: true, showSubject: true),
        _toolbar([
          OutlinedButton.icon(
            onPressed: _pickExamDate,
            icon: const Icon(Icons.calendar_today_outlined),
            label: Text(_dateLabel(_examDate)),
          ),
          AppButton(
            label: 'Add schedule',
            icon: Icons.add,
            onPressed: _saveSchedule,
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
                        title: Text('${schedule.examTypeName} - ${schedule.subjectName}'),
                        subtitle: Text('${_dateLabel(schedule.examDate)} | Max ${schedule.maxMarks.toStringAsFixed(0)} | ${schedule.status}'),
                        trailing: PopupMenuButton<String>(
                          onSelected: (action) {
                            if (action == 'edit') {
                              _showScheduleDialog(schedule);
                            } else {
                              _deleteSchedule(schedule.id);
                            }
                          },
                          itemBuilder: (context) => const [
                            PopupMenuItem(value: 'edit', child: Text('Edit')),
                            PopupMenuItem(value: 'delete', child: Text('Delete')),
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
    return Column(
      children: [
        _hierarchyFilters(showExamType: false, showSubject: true),
        _toolbar([
          _dropdown(
            width: 300,
            label: 'Schedule',
            value: _scheduleId,
            items: _schedules.map(
              (item) => MapEntry(
                item.id,
                '${item.examTypeName} - ${item.subjectName}',
              ),
            ),
            onChanged: (value) => setState(() {
              _scheduleId = value;
              _subjectId = _subjectForSchedule(value);
            }),
          ),
          AppButton(
            label: 'Load students',
            icon: Icons.group_outlined,
            onPressed: _loadMarksStudents,
          ),
          AppButton(
            label: 'Save marks',
            icon: Icons.save_outlined,
            onPressed: _saveMarks,
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
                        subtitle: Text('${student.rollNumber ?? '-'} | ${student.admissionNumber}'),
                        trailing: SizedBox(
                          width: 120,
                          child: TextField(
                            controller: controller,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(labelText: 'Marks'),
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
          setState(() => _sectionId = value);
          if (_classId != null && value != null) {
            _loadSubjects(_classId!, value);
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
          items: _subjects.map((item) => MapEntry(item.subjectId, item.subjectName)),
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
    final result =
        await ref.read(examsRepositoryProvider).subjects(classId, sectionId);
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
    final result = await ref.read(examsRepositoryProvider).schedules(
          academicYearId: _yearId,
          classId: _classId,
          sectionId: _sectionId,
        );
    result.when(
      success: (schedules) => setState(() => _schedules = schedules),
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
                    TextField(controller: code, decoration: const InputDecoration(labelText: 'Code')),
                    const SizedBox(height: 12),
                    TextField(controller: name, decoration: const InputDecoration(labelText: 'Name')),
                    const SizedBox(height: 12),
                    TextField(controller: description, decoration: const InputDecoration(labelText: 'Description')),
                    const SizedBox(height: 12),
                    SwitchListTile(
                      value: active,
                      onChanged: (value) => setDialogState(() => active = value),
                      title: const Text('Active'),
                    ),
                  ],
                ),
              );
            },
          ),
          actions: [
            TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Cancel')),
            FilledButton.icon(
              onPressed: () async {
                final result = await ref.read(examsRepositoryProvider).saveType(type?.id, {
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

  Future<void> _saveSchedule() async {
    if (_yearId == null || _classId == null || _sectionId == null || _examTypeId == null || _subjectId == null) {
      _snack('Select academic year, class, division, exam type, and subject.');
      return;
    }
    final result = await ref.read(examsRepositoryProvider).saveSchedule(null, {
      'academicYearId': _yearId,
      'classId': _classId,
      'sectionId': _sectionId,
      'examTypeId': _examTypeId,
      'subjectId': _subjectId,
      'examDate': _dateLabel(_examDate),
      'maxMarks': 100,
      'status': 'SCHEDULED',
    });
    result.when(
      success: (_) {
        _snack('Exam schedule saved.');
        _loadSchedules();
      },
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

  Future<void> _showScheduleDialog(ExamScheduleModel schedule) async {
    DateTime examDate = schedule.examDate;
    String status = schedule.status;
    final maxMarks = TextEditingController(
      text: schedule.maxMarks.toStringAsFixed(0),
    );
    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Text('Edit schedule'),
              content: SizedBox(
                width: 420,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text('${schedule.examTypeName} - ${schedule.subjectName}'),
                      subtitle: Text(_dateLabel(examDate)),
                      trailing: IconButton(
                        tooltip: 'Pick date',
                        icon: const Icon(Icons.calendar_today_outlined),
                        onPressed: () async {
                          final picked = await showDatePicker(
                            context: context,
                            initialDate: examDate,
                            firstDate: DateTime(2020),
                            lastDate: DateTime(2100),
                          );
                          if (picked != null) {
                            setDialogState(() => examDate = picked);
                          }
                        },
                      ),
                    ),
                    TextField(
                      controller: maxMarks,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(labelText: 'Max marks'),
                    ),
                    const SizedBox(height: 12),
                    DropdownButtonFormField<String>(
                      initialValue: status,
                      decoration: const InputDecoration(labelText: 'Status'),
                      items: const [
                        DropdownMenuItem(value: 'SCHEDULED', child: Text('Scheduled')),
                        DropdownMenuItem(value: 'COMPLETED', child: Text('Completed')),
                        DropdownMenuItem(value: 'CANCELLED', child: Text('Cancelled')),
                      ],
                      onChanged: (value) => status = value ?? status,
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
                FilledButton.icon(
                  onPressed: () async {
                    final result = await ref.read(examsRepositoryProvider).saveSchedule(schedule.id, {
                      'academicYearId': schedule.academicYearId,
                      'classId': schedule.classId,
                      'sectionId': schedule.sectionId,
                      'examTypeId': schedule.examTypeId,
                      'subjectId': schedule.subjectId,
                      'examDate': _dateLabel(examDate),
                      'maxMarks': double.tryParse(maxMarks.text) ?? schedule.maxMarks,
                      'status': status,
                    });
                    if (!context.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        Navigator.of(context).pop();
                        _loadSchedules();
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
      },
    );
    maxMarks.dispose();
  }

  Future<void> _loadMarksStudents() async {
    if (_yearId == null || _classId == null || _sectionId == null || _scheduleId == null) {
      _snack('Select academic year, class, division, and schedule.');
      return;
    }
    final repository = ref.read(examsRepositoryProvider);
    final studentsResult = await repository.students(
      academicYearId: _yearId!,
      classId: _classId!,
      sectionId: _sectionId!,
    );
    final marksResult = _subjectId == null
        ? null
        : await repository.marks({
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
    marksResult?.when(
      success: (marks) {
        for (final mark in marks.records) {
          _markControllers[mark.student.studentId]?.text =
              mark.marksObtained.toStringAsFixed(0);
        }
      },
      failure: (_) {},
    );
  }

  Future<void> _saveMarks() async {
    if (_yearId == null || _classId == null || _sectionId == null || _scheduleId == null || _subjectId == null) {
      _snack('Select academic year, class, division, schedule, and subject.');
      return;
    }
    final result = await ref.read(examsRepositoryProvider).saveMarks({
      'academicYearId': _yearId,
      'classId': _classId,
      'sectionId': _sectionId,
      'examScheduleId': _scheduleId,
      'subjectId': _subjectId,
      'records': [
        for (final student in _students)
          {
            'studentId': student.studentId,
            'marksObtained': double.tryParse(_markControllers[student.studentId]?.text ?? '') ?? 0,
            'maxMarks': 100,
          },
      ],
    });
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
    final result =
        await ref.read(examsRepositoryProvider).reportCard(studentId, _yearId!);
    result.when(
      success: (bytes) async {
        await downloadBytes(bytes, 'report-card.csv', 'text/csv');
        _snack('Report card downloaded.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _pickExamDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _examDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (picked != null) {
      setState(() => _examDate = picked);
    }
  }

  void _snack(String message) {
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  String? _subjectForSchedule(String? scheduleId) {
    if (scheduleId == null) {
      return null;
    }
    for (final schedule in _schedules) {
      if (schedule.id == scheduleId) {
        return schedule.subjectId;
      }
    }
    return null;
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
