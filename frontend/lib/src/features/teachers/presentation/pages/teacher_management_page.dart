import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_data_table.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../../core/widgets/app_select_field.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../students/data/models/student_models.dart';
import '../../../students/presentation/controllers/students_providers.dart';
import '../../data/models/teacher_models.dart';
import '../../data/repositories/teachers_repository_impl.dart';
import '../controllers/teachers_providers.dart';

class TeacherManagementPage extends ConsumerStatefulWidget {
  const TeacherManagementPage({super.key});

  @override
  ConsumerState<TeacherManagementPage> createState() =>
      _TeacherManagementPageState();
}

class _TeacherManagementPageState extends ConsumerState<TeacherManagementPage> {
  String? _academicYearId;

  @override
  Widget build(BuildContext context) {
    final years = ref.watch(teacherAcademicYearsProvider);

    return AdminShell(
      title: 'Teacher Management',
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
              _Header(
                years: items,
                selectedAcademicYearId: effectiveYearId,
                onAcademicYearChanged: (value) {
                  setState(() => _academicYearId = value);
                },
                onAddTeacher: () => _showTeacherDialog(
                  context,
                  ref,
                  academicYearId: effectiveYearId,
                ),
              ),
              const Divider(height: 1),
              Expanded(
                child: effectiveYearId == null
                    ? const Center(child: Text('No academic years found.'))
                    : _TeacherList(academicYearId: effectiveYearId),
              ),
            ],
          );
        },
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(teacherAcademicYearsProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading academic years'),
      ),
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({
    required this.years,
    required this.selectedAcademicYearId,
    required this.onAcademicYearChanged,
    required this.onAddTeacher,
  });

  final List<AcademicYearModel> years;
  final String? selectedAcademicYearId;
  final ValueChanged<String?> onAcademicYearChanged;
  final VoidCallback onAddTeacher;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 16),
        child: Wrap(
          spacing: 12,
          runSpacing: 10,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            SizedBox(
              width: 320,
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
            FilledButton.icon(
              onPressed: onAddTeacher,
              icon: const Icon(Icons.person_add_alt_1_outlined),
              label: const Text('Add teacher'),
            ),
          ],
        ),
      ),
    );
  }
}

class _TeacherList extends ConsumerWidget {
  const _TeacherList({required this.academicYearId});

  final String academicYearId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final teachers = ref.watch(teachersProvider(academicYearId));

    return teachers.when(
      data: (items) {
        if (items.isEmpty) {
          return const Center(child: Text('No teachers found.'));
        }
        return ListView(
          padding: const EdgeInsets.all(24),
          children: [
            DecoratedBox(
              decoration: BoxDecoration(
                color: Colors.white,
                border: Border.all(color: const Color(0xFFE2E8F0)),
                borderRadius: BorderRadius.circular(8),
              ),
              child: AppDataTable<TeacherModel>(
                items: items,
                onRowTap: (teacher) => _showTeacherProfileDialog(
                  context,
                  ref,
                  teacher,
                  academicYearId,
                ),
                columns: [
                  AppTableColumn(
                    label: 'Employee Code',
                    cellBuilder: (_, teacher) => Text(teacher.employeeCode),
                  ),
                  AppTableColumn(
                    label: 'Name',
                    cellBuilder: (_, teacher) => Text(
                      teacher.displayName,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  AppTableColumn(
                    label: 'Mobile',
                    cellBuilder: (_, teacher) =>
                        Text(_dash(teacher.mobileNumber)),
                  ),
                  AppTableColumn(
                    label: 'Email',
                    cellBuilder: (_, teacher) => Text(_dash(teacher.email)),
                  ),
                  AppTableColumn(
                    label: 'Qualification',
                    cellBuilder: (_, teacher) =>
                        Text(_dash(teacher.qualification)),
                  ),
                  AppTableColumn(
                    label: 'Mappings',
                    cellBuilder: (_, teacher) => Text(
                      '${teacher.assignedClassesCount} class / ${teacher.assignedSubjectsCount} subject',
                    ),
                  ),
                  AppTableColumn(
                    label: 'Status',
                    cellBuilder: (_, teacher) =>
                        _StatusBadge(label: teacher.status),
                  ),
                  AppTableColumn(
                    label: 'Actions',
                    cellBuilder: (context, teacher) => Wrap(
                      spacing: 2,
                      children: [
                        IconButton(
                          tooltip: 'View profile',
                          onPressed: () => _showTeacherProfileDialog(
                            context,
                            ref,
                            teacher,
                            academicYearId,
                          ),
                          icon: const Icon(Icons.visibility_outlined),
                        ),
                        IconButton(
                          tooltip: 'Edit',
                          onPressed: () => _showTeacherDialog(
                            context,
                            ref,
                            teacher: teacher,
                            academicYearId: academicYearId,
                          ),
                          icon: const Icon(Icons.edit_outlined),
                        ),
                        IconButton(
                          tooltip: 'Delete',
                          onPressed: () => _deleteTeacher(
                            context,
                            ref,
                            teacher,
                            academicYearId,
                          ),
                          icon: const Icon(Icons.delete_outline),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        );
      },
      error: (error, _) => AppErrorState(
        message: _message(error),
        onRetry: () => ref.invalidate(teachersProvider(academicYearId)),
      ),
      loading: () => const AppLoadingState(label: 'Loading teachers'),
    );
  }
}

Future<void> _showTeacherDialog(
  BuildContext context,
  WidgetRef ref, {
  TeacherModel? teacher,
  String? academicYearId,
}) async {
  final employeeCode = TextEditingController(text: teacher?.employeeCode ?? '');
  final firstName = TextEditingController(text: teacher?.firstName ?? '');
  final middleName = TextEditingController(text: teacher?.middleName ?? '');
  final lastName = TextEditingController(text: teacher?.lastName ?? '');
  final dateOfBirth = TextEditingController(
    text: _nullableDateLabel(teacher?.dateOfBirth),
  );
  final mobile = TextEditingController(text: teacher?.mobileNumber ?? '');
  final email = TextEditingController(text: teacher?.email ?? '');
  final qualification = TextEditingController(
    text: teacher?.qualification ?? '',
  );
  final experience = TextEditingController(
    text: teacher?.experienceYears?.toString() ?? '',
  );
  final joiningDate = TextEditingController(
    text: _nullableDateLabel(teacher?.joiningDate),
  );
  final formKey = GlobalKey<FormState>();
  var gender = teacher?.gender ?? 'MALE';
  var status = teacher?.status ?? 'ACTIVE';

  await showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          return AlertDialog(
            title: Text(teacher == null ? 'Add teacher' : 'Edit teacher'),
            content: Form(
              key: formKey,
              child: SizedBox(
                width: 640,
                child: SingleChildScrollView(
                  child: Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    children: [
                      _DialogField(
                        controller: employeeCode,
                        label: 'Employee code',
                      ),
                      _DialogField(controller: firstName, label: 'First name'),
                      _DialogField(
                        controller: middleName,
                        label: 'Middle name',
                        required: false,
                      ),
                      _DialogField(
                        controller: lastName,
                        label: 'Last name',
                        required: false,
                      ),
                      _DialogField(
                        controller: dateOfBirth,
                        label: 'Date of birth',
                        required: false,
                        validator: _optionalDate,
                      ),
                      _SizedField(
                        child: DropdownButtonFormField<String>(
                          initialValue: gender,
                          decoration: const InputDecoration(
                            labelText: 'Gender',
                          ),
                          items: const [
                            DropdownMenuItem(
                              value: 'MALE',
                              child: Text('Male'),
                            ),
                            DropdownMenuItem(
                              value: 'FEMALE',
                              child: Text('Female'),
                            ),
                            DropdownMenuItem(
                              value: 'OTHER',
                              child: Text('Other'),
                            ),
                          ],
                          onChanged: (value) =>
                              setDialogState(() => gender = value ?? gender),
                        ),
                      ),
                      _DialogField(
                        controller: mobile,
                        label: 'Mobile',
                        required: false,
                        validator: _optionalMobile,
                      ),
                      _DialogField(
                        controller: email,
                        label: 'Email',
                        required: false,
                        validator: _email,
                      ),
                      _DialogField(
                        controller: qualification,
                        label: 'Qualification',
                        required: false,
                      ),
                      _DialogField(
                        controller: experience,
                        label: 'Experience years',
                        required: false,
                        validator: _optionalInt,
                      ),
                      _DialogField(
                        controller: joiningDate,
                        label: 'Joining date',
                        required: false,
                        validator: _optionalDate,
                      ),
                      _SizedField(
                        child: DropdownButtonFormField<String>(
                          initialValue: status,
                          decoration: const InputDecoration(
                            labelText: 'Status',
                          ),
                          items: const [
                            DropdownMenuItem(
                              value: 'ACTIVE',
                              child: Text('Active'),
                            ),
                            DropdownMenuItem(
                              value: 'INACTIVE',
                              child: Text('Inactive'),
                            ),
                          ],
                          onChanged: (value) =>
                              setDialogState(() => status = value ?? status),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton.icon(
                onPressed: () async {
                  if (!(formKey.currentState?.validate() ?? false)) {
                    return;
                  }
                  final payload = {
                    'employeeCode': employeeCode.text.trim(),
                    'firstName': firstName.text.trim(),
                    'middleName': _blankToNull(middleName.text),
                    'lastName': _blankToNull(lastName.text),
                    'gender': gender,
                    'dateOfBirth': _blankToNull(dateOfBirth.text),
                    'mobileNumber': _blankToNull(mobile.text),
                    'email': _blankToNull(email.text),
                    'qualification': _blankToNull(qualification.text),
                    'experienceYears': _intOrNull(experience.text),
                    'joiningDate': _blankToNull(joiningDate.text),
                    'status': status,
                  };
                  final repository = ref.read(teachersRepositoryProvider);
                  final result = teacher == null
                      ? await repository.createTeacher(payload)
                      : await repository.updateTeacher(teacher.id, payload);
                  if (!dialogContext.mounted) {
                    return;
                  }
                  result.when(
                    success: (_) {
                      _refreshTeacherState(ref, academicYearId, teacher?.id);
                      _snack(
                        context,
                        teacher == null ? 'Teacher created.' : 'Teacher saved.',
                      );
                      Navigator.of(dialogContext).pop();
                    },
                    failure: (failure) => _snack(
                      dialogContext,
                      failure.message,
                    ),
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

  employeeCode.dispose();
  firstName.dispose();
  middleName.dispose();
  lastName.dispose();
  dateOfBirth.dispose();
  mobile.dispose();
  email.dispose();
  qualification.dispose();
  experience.dispose();
  joiningDate.dispose();
}

Future<void> _showTeacherProfileDialog(
  BuildContext context,
  WidgetRef ref,
  TeacherModel teacher,
  String academicYearId,
) async {
  final key = TeacherProfileKey(
    teacherId: teacher.id,
    academicYearId: academicYearId,
  );

  await showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return Consumer(
        builder: (dialogContext, ref, _) {
          final profile = ref.watch(teacherProfileProvider(key));
          return AlertDialog(
            title: Text(teacher.displayName),
            content: SizedBox(
              width: 920,
              height: 600,
              child: profile.when(
                data: (item) => _TeacherProfileContent(
                  profile: item,
                  academicYearId: academicYearId,
                  onEdit: () => _showTeacherDialog(
                    context,
                    ref,
                    teacher: item.personalDetails,
                    academicYearId: academicYearId,
                  ),
                  onAddAssignment: () => _showAssignmentDialog(
                    context,
                    ref,
                    teacher: item.personalDetails,
                    academicYearId: academicYearId,
                  ),
                  onAddDocument: () => _showTeacherDocumentDialog(
                    context,
                    ref,
                    item.personalDetails,
                    academicYearId,
                  ),
                ),
                error: (error, _) => AppErrorState(
                  message: _message(error),
                  onRetry: () => ref.invalidate(teacherProfileProvider(key)),
                ),
                loading: () => const AppLoadingState(label: 'Loading profile'),
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Close'),
              ),
            ],
          );
        },
      );
    },
  );
}

class _TeacherProfileContent extends StatelessWidget {
  const _TeacherProfileContent({
    required this.profile,
    required this.academicYearId,
    required this.onEdit,
    required this.onAddAssignment,
    required this.onAddDocument,
  });

  final TeacherProfileModel profile;
  final String academicYearId;
  final VoidCallback onEdit;
  final VoidCallback onAddAssignment;
  final VoidCallback onAddDocument;

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 7,
      child: Column(
        children: [
          const TabBar(
            isScrollable: true,
            tabs: [
              Tab(text: 'Personal Details'),
              Tab(text: 'Academic Assignments'),
              Tab(text: 'Classes & Subjects'),
              Tab(text: 'Attendance'),
              Tab(text: 'Documents'),
              Tab(text: 'Payroll'),
              Tab(text: 'Notifications'),
            ],
          ),
          const Divider(height: 1),
          Expanded(
            child: TabBarView(
              children: [
                _PersonalTab(teacher: profile.personalDetails, onEdit: onEdit),
                _AssignmentsTab(
                  teacher: profile.personalDetails,
                  academicYearId: academicYearId,
                  assignments: profile.academicAssignments,
                  onAdd: onAddAssignment,
                ),
                _ClassesSubjectsTab(profile: profile),
                _MessageTab(message: profile.attendanceSummary),
                _DocumentsTab(
                  teacher: profile.personalDetails,
                  academicYearId: academicYearId,
                  documents: profile.documents,
                  onAdd: onAddDocument,
                ),
                _MessageTab(message: profile.payrollSummary),
                _MessageTab(message: profile.notificationSummary),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PersonalTab extends StatelessWidget {
  const _PersonalTab({required this.teacher, required this.onEdit});

  final TeacherModel teacher;
  final VoidCallback onEdit;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Align(
          alignment: Alignment.centerRight,
          child: FilledButton.icon(
            onPressed: onEdit,
            icon: const Icon(Icons.edit_outlined),
            label: const Text('Edit'),
          ),
        ),
        const SizedBox(height: 12),
        _DetailGrid(
          items: {
            'Name': teacher.displayName,
            'Employee code': teacher.employeeCode,
            'Gender': teacher.gender,
            'DOB': _nullableDateLabel(teacher.dateOfBirth),
            'Mobile': teacher.mobileNumber,
            'Email': teacher.email,
            'Qualification': teacher.qualification,
            'Experience': teacher.experienceYears == null
                ? null
                : '${teacher.experienceYears} year(s)',
            'Joining date': _nullableDateLabel(teacher.joiningDate),
            'Status': teacher.status,
          },
        ),
      ],
    );
  }
}

class _AssignmentsTab extends ConsumerWidget {
  const _AssignmentsTab({
    required this.teacher,
    required this.academicYearId,
    required this.assignments,
    required this.onAdd,
  });

  final TeacherModel teacher;
  final String academicYearId;
  final List<TeacherAssignmentModel> assignments;
  final VoidCallback onAdd;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Align(
          alignment: Alignment.centerRight,
          child: FilledButton.icon(
            onPressed: onAdd,
            icon: const Icon(Icons.add_outlined),
            label: const Text('Add assignment'),
          ),
        ),
        const SizedBox(height: 12),
        if (assignments.isEmpty)
          const _InlineEmpty(message: 'No academic assignments.')
        else
          for (final assignment in assignments)
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.assignment_ind_outlined),
              title: Text(_assignmentTitle(assignment)),
              subtitle: Text(
                [
                  assignment.academicYear,
                  assignment.assignmentType.replaceAll('_', ' '),
                  assignment.status,
                ].join(' - '),
              ),
              trailing: Wrap(
                spacing: 2,
                children: [
                  IconButton(
                    tooltip: 'Edit assignment',
                    onPressed: () => _showAssignmentDialog(
                      context,
                      ref,
                      teacher: teacher,
                      academicYearId: academicYearId,
                      assignment: assignment,
                    ),
                    icon: const Icon(Icons.edit_outlined),
                  ),
                  IconButton(
                    tooltip: 'Delete assignment',
                    onPressed: () => _deleteAssignment(
                      context,
                      ref,
                      teacher,
                      academicYearId,
                      assignment,
                    ),
                    icon: const Icon(Icons.delete_outline),
                  ),
                ],
              ),
            ),
      ],
    );
  }
}

class _ClassesSubjectsTab extends StatelessWidget {
  const _ClassesSubjectsTab({required this.profile});

  final TeacherProfileModel profile;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          'Class Teacher',
          style: Theme.of(
            context,
          ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        if (profile.classTeacherMappings.isEmpty)
          const _InlineEmpty(message: 'No class teacher mapping.')
        else
          for (final item in profile.classTeacherMappings)
            _MappingTile(mapping: item),
        const SizedBox(height: 20),
        Text(
          'Subject Teacher',
          style: Theme.of(
            context,
          ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        if (profile.subjectTeacherMappings.isEmpty)
          const _InlineEmpty(message: 'No subject teacher mapping.')
        else
          for (final item in profile.subjectTeacherMappings)
            _MappingTile(mapping: item),
      ],
    );
  }
}

class _DocumentsTab extends ConsumerWidget {
  const _DocumentsTab({
    required this.teacher,
    required this.academicYearId,
    required this.documents,
    required this.onAdd,
  });

  final TeacherModel teacher;
  final String academicYearId;
  final List<TeacherDocumentModel> documents;
  final VoidCallback onAdd;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Align(
          alignment: Alignment.centerRight,
          child: FilledButton.icon(
            onPressed: onAdd,
            icon: const Icon(Icons.upload_file_outlined),
            label: const Text('Upload'),
          ),
        ),
        const SizedBox(height: 12),
        if (documents.isEmpty)
          const _InlineEmpty(message: 'No documents uploaded.')
        else
          for (final document in documents)
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.description_outlined),
              title: Text(document.fileName),
              subtitle: Text(
                [
                  document.documentType,
                  document.status,
                  if (document.uploadedAt != null)
                    'Uploaded ${_nullableDateLabel(document.uploadedAt)}',
                ].join(' - '),
              ),
              trailing: Wrap(
                spacing: 2,
                children: [
                  IconButton(
                    tooltip: 'Edit document',
                    onPressed: () => _showTeacherDocumentDialog(
                      context,
                      ref,
                      teacher,
                      academicYearId,
                      document: document,
                    ),
                    icon: const Icon(Icons.edit_outlined),
                  ),
                  IconButton(
                    tooltip: 'Delete document',
                    onPressed: () => _deleteDocument(
                      context,
                      ref,
                      teacher,
                      academicYearId,
                      document,
                    ),
                    icon: const Icon(Icons.delete_outline),
                  ),
                ],
              ),
            ),
      ],
    );
  }
}

class _MessageTab extends StatelessWidget {
  const _MessageTab({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Text(message, textAlign: TextAlign.center),
    );
  }
}

class _MappingTile extends StatelessWidget {
  const _MappingTile({required this.mapping});

  final TeacherAcademicMappingModel mapping;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: const Icon(Icons.school_outlined),
      title: Text(
        [
          mapping.className,
          mapping.sectionName,
          if (mapping.subjectName != null) mapping.subjectName!,
        ].join(' - '),
      ),
      subtitle: Text(mapping.academicYear),
      trailing: _StatusBadge(label: mapping.active ? 'ACTIVE' : 'INACTIVE'),
    );
  }
}

Future<void> _showAssignmentDialog(
  BuildContext context,
  WidgetRef ref, {
  required TeacherModel teacher,
  required String academicYearId,
  TeacherAssignmentModel? assignment,
}) async {
  final formKey = GlobalKey<FormState>();
  var assignmentType = assignment?.assignmentType ?? 'CLASS_TEACHER';
  var selectedClassId = assignment?.classId;
  var selectedSectionId = assignment?.sectionId;
  var selectedSubjectId = assignment?.subjectId;
  var status = assignment?.status ?? 'ACTIVE';

  await showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          return Consumer(
            builder: (dialogContext, ref, _) {
              final classes = ref.watch(
                classesByAcademicYearProvider(academicYearId),
              );
              final sections = selectedClassId == null
                  ? const AsyncValue.data(<SectionModel>[])
                  : ref.watch(sectionsByClassProvider(selectedClassId!));
              final subjects = ref.watch(teacherSubjectsProvider);
              final classItems = classes.maybeWhen(
                data: (items) => items,
                orElse: () => const <SchoolClassModel>[],
              );
              final sectionItems = sections.maybeWhen(
                data: (items) => items,
                orElse: () => const <SectionModel>[],
              );
              final subjectItems = subjects.maybeWhen(
                data: (items) => items,
                orElse: () => const <SubjectModel>[],
              );
              return AlertDialog(
                title: Text(
                  assignment == null ? 'Add assignment' : 'Edit assignment',
                ),
                content: Form(
                  key: formKey,
                  child: SizedBox(
                    width: 640,
                    child: Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: [
                        _SizedField(
                          child: DropdownButtonFormField<String>(
                            initialValue: assignmentType,
                            decoration: const InputDecoration(
                              labelText: 'Assignment type',
                            ),
                            items: const [
                              DropdownMenuItem(
                                value: 'CLASS_TEACHER',
                                child: Text('Class teacher'),
                              ),
                              DropdownMenuItem(
                                value: 'SUBJECT_TEACHER',
                                child: Text('Subject teacher'),
                              ),
                              DropdownMenuItem(
                                value: 'COORDINATOR',
                                child: Text('Coordinator'),
                              ),
                            ],
                            onChanged: (value) => setDialogState(() {
                              assignmentType = value ?? assignmentType;
                              if (assignmentType != 'SUBJECT_TEACHER') {
                                selectedSubjectId = null;
                              }
                            }),
                          ),
                        ),
                        _SizedField(
                          child: classes.when(
                            data: (_) => DropdownButtonFormField<String>(
                              initialValue: selectedClassId,
                              decoration: const InputDecoration(
                                labelText: 'Class',
                              ),
                              items: [
                                for (final item in classItems)
                                  DropdownMenuItem(
                                    value: item.id,
                                    child: Text(item.name),
                                  ),
                              ],
                              validator: assignmentType == 'COORDINATOR'
                                  ? null
                                  : _required,
                              onChanged: (value) => setDialogState(() {
                                selectedClassId = value;
                                selectedSectionId = null;
                              }),
                            ),
                            error: (error, _) => Text(_message(error)),
                            loading: () => const LinearProgressIndicator(),
                          ),
                        ),
                        _SizedField(
                          child: sections.when(
                            data: (_) => DropdownButtonFormField<String>(
                              initialValue: selectedSectionId,
                              decoration: const InputDecoration(
                                labelText: 'Section',
                              ),
                              items: [
                                for (final item in sectionItems)
                                  DropdownMenuItem(
                                    value: item.id,
                                    child: Text(item.name),
                                  ),
                              ],
                              validator: assignmentType == 'COORDINATOR'
                                  ? null
                                  : _required,
                              onChanged: (value) => setDialogState(
                                () => selectedSectionId = value,
                              ),
                            ),
                            error: (error, _) => Text(_message(error)),
                            loading: () => const LinearProgressIndicator(),
                          ),
                        ),
                        if (assignmentType == 'SUBJECT_TEACHER')
                          _SizedField(
                            child: subjects.when(
                              data: (_) => DropdownButtonFormField<String>(
                                initialValue: selectedSubjectId,
                                decoration: const InputDecoration(
                                  labelText: 'Subject',
                                ),
                                items: [
                                  for (final item in subjectItems)
                                    DropdownMenuItem(
                                      value: item.id,
                                      child: Text(item.name),
                                    ),
                                ],
                                validator: _required,
                                onChanged: (value) => setDialogState(
                                  () => selectedSubjectId = value,
                                ),
                              ),
                              error: (error, _) => Text(_message(error)),
                              loading: () => const LinearProgressIndicator(),
                            ),
                          ),
                        _SizedField(
                          child: DropdownButtonFormField<String>(
                            initialValue: status,
                            decoration: const InputDecoration(
                              labelText: 'Status',
                            ),
                            items: const [
                              DropdownMenuItem(
                                value: 'ACTIVE',
                                child: Text('Active'),
                              ),
                              DropdownMenuItem(
                                value: 'INACTIVE',
                                child: Text('Inactive'),
                              ),
                            ],
                            onChanged: (value) =>
                                setDialogState(() => status = value ?? status),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.of(dialogContext).pop(),
                    child: const Text('Cancel'),
                  ),
                  FilledButton.icon(
                    onPressed: () async {
                      if (!(formKey.currentState?.validate() ?? false)) {
                        return;
                      }
                      final payload = {
                        'academicYearId': academicYearId,
                        'assignmentType': assignmentType,
                        'classId': selectedClassId,
                        'sectionId': selectedSectionId,
                        'subjectId': selectedSubjectId,
                        'status': status,
                      };
                      final repository = ref.read(teachersRepositoryProvider);
                      final result = assignment == null
                          ? await repository.createAssignment(
                              teacher.id,
                              payload,
                            )
                          : await repository.updateAssignment(
                              teacher.id,
                              assignment.id,
                              payload,
                            );
                      if (!dialogContext.mounted) {
                        return;
                      }
                      result.when(
                        success: (_) {
                          _refreshTeacherState(ref, academicYearId, teacher.id);
                          _snack(context, 'Assignment saved.');
                          Navigator.of(dialogContext).pop();
                        },
                        failure: (failure) =>
                            _snack(dialogContext, failure.message),
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
    },
  );
}

Future<void> _showTeacherDocumentDialog(
  BuildContext context,
  WidgetRef ref,
  TeacherModel teacher,
  String academicYearId, {
  TeacherDocumentModel? document,
}) async {
  final type = TextEditingController(text: document?.documentType ?? '');
  final fileName = TextEditingController(text: document?.fileName ?? '');
  final fileUrl = TextEditingController(text: document?.fileUrl ?? '');
  final filePath = TextEditingController(text: document?.filePath ?? '');
  final formKey = GlobalKey<FormState>();
  var status = document?.status ?? 'ACTIVE';

  await showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          return AlertDialog(
            title: Text(document == null ? 'Upload document' : 'Edit document'),
            content: Form(
              key: formKey,
              child: SizedBox(
                width: 620,
                child: Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    _DialogField(controller: type, label: 'Document type'),
                    _DialogField(controller: fileName, label: 'File name'),
                    _DialogField(
                      controller: fileUrl,
                      label: 'File URL',
                      required: false,
                    ),
                    _DialogField(
                      controller: filePath,
                      label: 'File path',
                      required: false,
                    ),
                    _SizedField(
                      child: DropdownButtonFormField<String>(
                        initialValue: status,
                        decoration: const InputDecoration(labelText: 'Status'),
                        items: const [
                          DropdownMenuItem(
                            value: 'ACTIVE',
                            child: Text('Active'),
                          ),
                          DropdownMenuItem(
                            value: 'INACTIVE',
                            child: Text('Inactive'),
                          ),
                        ],
                        onChanged: (value) =>
                            setDialogState(() => status = value ?? status),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton.icon(
                onPressed: () async {
                  if (!(formKey.currentState?.validate() ?? false)) {
                    return;
                  }
                  final payload = {
                    'documentType': type.text.trim(),
                    'fileName': fileName.text.trim(),
                    'fileUrl': _blankToNull(fileUrl.text),
                    'filePath': _blankToNull(filePath.text),
                    'status': status,
                  };
                  final repository = ref.read(teachersRepositoryProvider);
                  final result = document == null
                      ? await repository.createDocument(teacher.id, payload)
                      : await repository.updateDocument(
                          teacher.id,
                          document.id,
                          payload,
                        );
                  if (!dialogContext.mounted) {
                    return;
                  }
                  result.when(
                    success: (_) {
                      _refreshTeacherState(ref, academicYearId, teacher.id);
                      _snack(context, 'Document saved.');
                      Navigator.of(dialogContext).pop();
                    },
                    failure: (failure) =>
                        _snack(dialogContext, failure.message),
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

  type.dispose();
  fileName.dispose();
  fileUrl.dispose();
  filePath.dispose();
}

Future<void> _deleteTeacher(
  BuildContext context,
  WidgetRef ref,
  TeacherModel teacher,
  String academicYearId,
) async {
  final confirmed = await _confirm(context, 'Delete ${teacher.displayName}?');
  if (!confirmed || !context.mounted) {
    return;
  }
  final result = await ref.read(teachersRepositoryProvider).deleteTeacher(
        teacher.id,
      );
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      _refreshTeacherState(ref, academicYearId, teacher.id);
      _snack(context, 'Teacher deleted.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _deleteAssignment(
  BuildContext context,
  WidgetRef ref,
  TeacherModel teacher,
  String academicYearId,
  TeacherAssignmentModel assignment,
) async {
  final confirmed = await _confirm(context, 'Delete this assignment?');
  if (!confirmed || !context.mounted) {
    return;
  }
  final result = await ref
      .read(teachersRepositoryProvider)
      .deleteAssignment(teacher.id, assignment.id);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      _refreshTeacherState(ref, academicYearId, teacher.id);
      _snack(context, 'Assignment deleted.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _deleteDocument(
  BuildContext context,
  WidgetRef ref,
  TeacherModel teacher,
  String academicYearId,
  TeacherDocumentModel document,
) async {
  final confirmed = await _confirm(context, 'Delete ${document.fileName}?');
  if (!confirmed || !context.mounted) {
    return;
  }
  final result = await ref
      .read(teachersRepositoryProvider)
      .deleteDocument(teacher.id, document.id);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      _refreshTeacherState(ref, academicYearId, teacher.id);
      _snack(context, 'Document deleted.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

class _DialogField extends StatelessWidget {
  const _DialogField({
    required this.controller,
    required this.label,
    this.required = true,
    this.validator,
  });

  final TextEditingController controller;
  final String label;
  final bool required;
  final String? Function(String?)? validator;

  @override
  Widget build(BuildContext context) {
    return _SizedField(
      child: TextFormField(
        controller: controller,
        decoration: InputDecoration(labelText: label),
        validator: validator ?? (required ? _required : null),
      ),
    );
  }
}

class _SizedField extends StatelessWidget {
  const _SizedField({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return SizedBox(width: 300, child: child);
  }
}

class _DetailGrid extends StatelessWidget {
  const _DetailGrid({required this.items});

  final Map<String, String?> items;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 700 ? 2 : 1;
        return GridView.count(
          crossAxisCount: columns,
          childAspectRatio: columns == 1 ? 7 : 4,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          children: [
            for (final entry in items.entries)
              DecoratedBox(
                decoration: BoxDecoration(
                  border: Border.all(color: const Color(0xFFE2E8F0)),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        entry.key,
                        style: Theme.of(context).textTheme.labelMedium
                            ?.copyWith(color: const Color(0xFF64748B)),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _dash(entry.value),
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.bodyMedium
                            ?.copyWith(fontWeight: FontWeight.w700),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        );
      },
    );
  }
}

class _InlineEmpty extends StatelessWidget {
  const _InlineEmpty({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFFE2E8F0)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(message),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    final active = label == 'ACTIVE';
    final color = active ? const Color(0xFF16A34A) : const Color(0xFF64748B);
    return Container(
      height: 28,
      padding: const EdgeInsets.symmetric(horizontal: 10),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: color,
              fontWeight: FontWeight.w800,
            ),
      ),
    );
  }
}

void _refreshTeacherState(
  WidgetRef ref,
  String? academicYearId,
  String? teacherId,
) {
  ref.invalidate(teachersProvider(academicYearId));
  if (teacherId != null && academicYearId != null) {
    ref.invalidate(
      teacherProfileProvider(
        TeacherProfileKey(
          teacherId: teacherId,
          academicYearId: academicYearId,
        ),
      ),
    );
  }
}

Future<bool> _confirm(BuildContext context, String message) async {
  final result = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: const Text('Confirm action'),
      content: Text(message),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () => Navigator.of(dialogContext).pop(true),
          child: const Text('Confirm'),
        ),
      ],
    ),
  );
  return result ?? false;
}

String _assignmentTitle(TeacherAssignmentModel assignment) {
  final parts = [
    assignment.className,
    assignment.sectionName,
    assignment.subjectName,
  ].whereType<String>().where((value) => value.trim().isNotEmpty).toList();
  return parts.isEmpty ? assignment.assignmentType : parts.join(' - ');
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _email(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  return RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(text)
      ? null
      : 'Enter a valid email';
}

String? _optionalMobile(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  return RegExp(r'^\+?[0-9]{10,15}$').hasMatch(text)
      ? null
      : 'Enter 10 to 15 digits';
}

String? _optionalDate(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  final parsed = DateTime.tryParse(text);
  return parsed == null || text.length != 10 ? 'Use YYYY-MM-DD' : null;
}

String? _optionalInt(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  return int.tryParse(text) == null ? 'Enter a whole number' : null;
}

String? _blankToNull(String value) {
  return value.trim().isEmpty ? null : value.trim();
}

int? _intOrNull(String value) {
  return int.tryParse(value.trim());
}

String _nullableDateLabel(DateTime? value) {
  return value == null ? '' : value.toIso8601String().split('T').first;
}

String _dash(String? value) {
  return value == null || value.trim().isEmpty ? '-' : value.trim();
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}
