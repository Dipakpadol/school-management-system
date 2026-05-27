import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/result/result.dart';
import '../../../../core/upload/file_picker.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_data_table.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/student_models.dart';
import '../../data/repositories/students_repository_impl.dart';
import '../controllers/students_providers.dart';

class StudentSectionDetailPage extends ConsumerWidget {
  const StudentSectionDetailPage({
    required this.classId,
    required this.sectionId,
    this.academicYearId,
    super.key,
  });

  final String classId;
  final String sectionId;
  final String? academicYearId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final filter = StudentSectionFilter(
      academicYearId: academicYearId,
      classId: classId,
      sectionId: sectionId,
    );
    final key = ClassSectionKey(classId: classId, sectionId: sectionId);
    final students = ref.watch(sectionStudentsProvider(filter));
    final teachers = ref.watch(classSectionTeachersProvider(key));
    final years = ref
        .watch(academicYearsProvider)
        .maybeWhen(data: (value) => value, orElse: () => const []);
    final teacherDetails = teachers.maybeWhen(
      data: (value) => value,
      orElse: () => null,
    );
    String? academicYearName;
    for (final year in years) {
      if (year.id == academicYearId) {
        academicYearName = year.name;
        break;
      }
    }

    return AdminShell(
      title: 'Division View',
      activeModuleId: 'students',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: Column(
        children: [
          _DetailHeader(
            onBack: () => context.go(
              AppRoutes.studentClassSections(
                classId,
                academicYearId: academicYearId,
              ),
            ),
            onAddStudent: teacherDetails == null
                ? null
                : () => _showAdmissionDialog(
                    context,
                    ref,
                    filter,
                    teacherDetails,
                    academicYearName,
                  ),
            onImport: () => _runPickedStudentImport(context, ref, filter),
            onExport: () => _runStudentDownload(
              context,
              ref.read(studentsRepositoryProvider).exportExcel(),
              'Student export downloaded.',
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () async {
                ref.invalidate(sectionStudentsProvider(filter));
                ref.invalidate(classSectionTeachersProvider(key));
              },
              child: ListView(
                padding: const EdgeInsets.all(24),
                children: [
                  teachers.when(
                    data: (details) => _TeachersPanel(details: details),
                    error: (error, _) => AppErrorState(
                      message: _message(error),
                      onRetry: () =>
                          ref.invalidate(classSectionTeachersProvider(key)),
                    ),
                    loading: () =>
                        const AppLoadingState(label: 'Loading teachers'),
                  ),
                  const SizedBox(height: 18),
                  students.when(
                    data: (items) => _StudentsTable(students: items),
                    error: (error, _) => AppErrorState(
                      message: _message(error),
                      onRetry: () =>
                          ref.invalidate(sectionStudentsProvider(filter)),
                    ),
                    loading: () =>
                        const AppLoadingState(label: 'Loading students'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _DetailHeader extends StatelessWidget {
  const _DetailHeader({
    required this.onBack,
    required this.onImport,
    required this.onExport,
    this.onAddStudent,
  });

  final VoidCallback onBack;
  final VoidCallback? onAddStudent;
  final VoidCallback onImport;
  final VoidCallback onExport;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 10, 24, 10),
        child: Wrap(
          spacing: 12,
          runSpacing: 10,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            IconButton(
              tooltip: 'Back',
              onPressed: onBack,
              icon: const Icon(Icons.arrow_back),
            ),
            AppButton(
              label: 'Add student',
              icon: Icons.person_add_alt_1_outlined,
              onPressed: onAddStudent,
            ),
            OutlinedButton.icon(
              onPressed: onImport,
              icon: const Icon(Icons.upload_file_outlined),
              label: const Text('Import students'),
            ),
            OutlinedButton.icon(
              onPressed: onExport,
              icon: const Icon(Icons.download_outlined),
              label: const Text('Export students'),
            ),
          ],
        ),
      ),
    );
  }
}

class _TeachersPanel extends StatelessWidget {
  const _TeachersPanel({required this.details});

  final ClassSectionTeachersModel details;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return LayoutBuilder(
      builder: (context, constraints) {
        final twoColumns = constraints.maxWidth >= 860;
        final children = [
          _TeacherBox(
            title: 'Class Teacher',
            child: details.classTeacher == null
                ? Text('Not assigned', style: theme.textTheme.bodyMedium)
                : _TeacherLine(teacher: details.classTeacher!),
          ),
          _TeacherBox(
            title: 'Subject Teachers',
            child: details.subjectTeachers.isEmpty
                ? Text('Not assigned', style: theme.textTheme.bodyMedium)
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      for (final subject in details.subjectTeachers)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 10),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                subject.subjectName,
                                style: theme.textTheme.labelLarge?.copyWith(
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Wrap(
                                spacing: 8,
                                runSpacing: 8,
                                children: [
                                  for (final teacher in subject.teachers)
                                    _TeacherPill(teacher: teacher),
                                ],
                              ),
                            ],
                          ),
                        ),
                    ],
                  ),
          ),
        ];

        if (!twoColumns) {
          return Column(
            children: [
              for (final child in children) ...[
                child,
                const SizedBox(height: 12),
              ],
            ],
          );
        }
        return Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(child: children[0]),
            const SizedBox(width: 12),
            Expanded(child: children[1]),
          ],
        );
      },
    );
  }
}

class _TeacherBox extends StatelessWidget {
  const _TeacherBox({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: const Color(0xFFE2E8F0)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: Theme.of(
                context,
              ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 12),
            child,
          ],
        ),
      ),
    );
  }
}

class _TeacherLine extends StatelessWidget {
  const _TeacherLine({required this.teacher});

  final TeacherSummaryModel teacher;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Icon(Icons.badge_outlined, size: 20),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            '${teacher.displayName} (${teacher.employeeNumber})',
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }
}

class _TeacherPill extends StatelessWidget {
  const _TeacherPill({required this.teacher});

  final TeacherSummaryModel teacher;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 30,
      padding: const EdgeInsets.symmetric(horizontal: 10),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: const Color(0xFFDCFCE7),
        borderRadius: BorderRadius.circular(15),
      ),
      child: Text(
        teacher.displayName,
        overflow: TextOverflow.ellipsis,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: const Color(0xFF166534),
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _StudentsTable extends StatelessWidget {
  const _StudentsTable({required this.students});

  final List<StudentSummaryModel> students;

  @override
  Widget build(BuildContext context) {
    if (students.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(32),
          child: Text('No students found.'),
        ),
      );
    }

    return DecoratedBox(
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: const Color(0xFFE2E8F0)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: AppDataTable<StudentSummaryModel>(
        items: students,
        onRowTap: (student) => context.go(AppRoutes.studentProfile(student.id)),
        columns: [
          AppTableColumn(
            label: 'Admission No.',
            cellBuilder: (_, student) => Text(student.admissionNumber),
          ),
          AppTableColumn(
            label: 'Student',
            cellBuilder: (_, student) =>
                Text(student.displayName, overflow: TextOverflow.ellipsis),
          ),
          AppTableColumn(
            label: 'Roll',
            cellBuilder: (_, student) => Text(student.rollNumber ?? '-'),
          ),
          AppTableColumn(
            label: 'Status',
            cellBuilder: (_, student) => _StatusChip(status: student.status),
          ),
        ],
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final active = status == 'ACTIVE';
    final color = active ? const Color(0xFF16A34A) : const Color(0xFF64748B);

    return Container(
      height: 28,
      padding: const EdgeInsets.symmetric(horizontal: 10),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(14),
      ),
      alignment: Alignment.center,
      child: Text(
        status,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: color,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

Future<void> _showAdmissionDialog(
  BuildContext context,
  WidgetRef ref,
  StudentSectionFilter filter,
  ClassSectionTeachersModel details,
  String? academicYearName,
) async {
  final admissionNo = TextEditingController(
    text: 'ADM-2026-${DateTime.now().millisecondsSinceEpoch % 100000}',
  );
  final firstName = TextEditingController();
  final middleName = TextEditingController();
  final lastName = TextEditingController();
  final phone = TextEditingController(text: '+9198');
  final roll = TextEditingController();
  final parentName = TextEditingController();
  final parentPhone = TextEditingController(text: '+9198');
  final formKey = GlobalKey<FormState>();
  var gender = 'MALE';
  final dateOfBirth = DateTime(2015, 6, 1);
  final bloodGroup = TextEditingController(text: 'A+');
  final email = null;
  final previousSchool = null;
  final addressLine1 = 'Demo campus address';
  final addressLine2 = null;
  final city = '';
  final state = '';
  final postalCode = '';
  final country = 'India';

  await showDialog<void>(
    context: context,
    builder: (context) {
      return AlertDialog(
        title: const Text('Add student'),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 620,
            child: SingleChildScrollView(
              child: Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: admissionNo,
                      decoration: const InputDecoration(
                        labelText: 'Admission number',
                      ),
                      validator: _required,
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: firstName,
                      decoration: const InputDecoration(
                        labelText: 'First name',
                      ),
                      validator: _required,
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: middleName,
                      decoration: const InputDecoration(
                        labelText: 'Middle name',
                      ),
                      validator: _required,
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: lastName,
                      decoration: const InputDecoration(labelText: 'Last name'),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: dateOfBirth == null
                          ? null
                          : TextEditingController(
                              text:
                                  '${dateOfBirth.year}-${dateOfBirth.month.toString().padLeft(2, '0')}-${dateOfBirth.day.toString().padLeft(2, '0')}',
                            ),
                      decoration: const InputDecoration(
                        labelText: 'Date of birth',
                      ),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: DropdownButtonFormField<String>(
                      initialValue: gender,
                      decoration: const InputDecoration(labelText: 'Gender'),
                      items: const [
                        DropdownMenuItem(value: 'MALE', child: Text('Male')),
                        DropdownMenuItem(
                          value: 'FEMALE',
                          child: Text('Female'),
                        ),
                        DropdownMenuItem(value: 'OTHER', child: Text('Other')),
                      ],
                      onChanged: (value) => gender = value ?? gender,
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: bloodGroup,
                      decoration: const InputDecoration(
                        labelText: 'Blood group',
                      ),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: email != null
                          ? TextEditingController(text: email)
                          : null,
                      decoration: const InputDecoration(labelText: 'E-mail'),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: phone,
                      decoration: const InputDecoration(labelText: 'Mobile'),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: roll,
                      decoration: const InputDecoration(
                        labelText: 'Roll number',
                      ),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: parentName,
                      decoration: const InputDecoration(labelText: 'Guardian'),
                      validator: _required,
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: parentPhone,
                      decoration: const InputDecoration(
                        labelText: 'Guardian mobile',
                      ),
                      validator: _required,
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: previousSchool != null
                          ? TextEditingController(text: previousSchool)
                          : null,
                      decoration: const InputDecoration(
                        labelText: 'Previous school',
                      ),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: addressLine1 != null
                          ? TextEditingController(text: addressLine1)
                          : null,
                      decoration: const InputDecoration(
                        labelText: 'Address line 1',
                      ),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: addressLine2 != null
                          ? TextEditingController(text: addressLine2)
                          : null,
                      decoration: const InputDecoration(
                        labelText: 'Address line 2',
                      ),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: city != null
                          ? TextEditingController(text: city)
                          : null,
                      decoration: const InputDecoration(labelText: 'City'),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: state != null
                          ? TextEditingController(text: state)
                          : null,
                      decoration: const InputDecoration(labelText: 'State'),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: postalCode != null
                          ? TextEditingController(text: postalCode)
                          : null,
                      decoration: const InputDecoration(
                        labelText: 'Postal code',
                      ),
                    ),
                  ),
                  _Field(
                    width: 290,
                    child: TextFormField(
                      controller: country != null
                          ? TextEditingController(text: country)
                          : null,
                      decoration: const InputDecoration(labelText: 'Country'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: () async {
              if (!(formKey.currentState?.validate() ?? false)) {
                return;
              }
              final result = await ref.read(studentsRepositoryProvider).admit({
                'admissionNumber': admissionNo.text.trim(),
                'profile': {
                  'firstName': firstName.text.trim(),
                  'middleName': null,
                  'lastName': _blankToNull(lastName.text),
                  'dateOfBirth': '2015-06-01',
                  'gender': gender,
                  'bloodGroup': null,
                  'email': null,
                  'phoneNumber': _blankToNull(phone.text),
                  'admissionDate': '2026-04-01',
                  'previousSchool': null,
                  'addressLine1': 'Demo campus address',
                  'addressLine2': null,
                  'city': 'Bengaluru',
                  'state': 'Karnataka',
                  'postalCode': '560001',
                  'country': 'India',
                },
                'parents': [
                  {
                    'relationType': 'GUARDIAN',
                    'primaryContact': true,
                    'emergencyContact': true,
                    'pickupAllowed': true,
                    'parent': {
                      'firstName': parentName.text.trim(),
                      'lastName': null,
                      'email': null,
                      'phoneNumber': parentPhone.text.trim(),
                      'alternatePhoneNumber': null,
                      'occupation': null,
                      'addressLine1': 'Demo campus address',
                      'addressLine2': null,
                      'city': 'Bengaluru',
                      'state': 'Karnataka',
                      'postalCode': '560001',
                      'country': 'India',
                      'userAccountId': null,
                    },
                  },
                ],
                'classAssignment': {
                  'academicYearId': filter.academicYearId,
                  'classId': filter.classId,
                  'sectionId': filter.sectionId,
                  'academicYear': academicYearName ?? '2026-2027',
                  'className': details.className,
                  'sectionName': details.sectionName,
                  'rollNumber': _blankToNull(roll.text),
                  'effectiveFrom': '2026-04-01',
                },
                'documents': [],
              });
              if (!context.mounted) {
                return;
              }
              result.when(
                success: (_) {
                  ref.invalidate(sectionStudentsProvider(filter));
                  Navigator.of(context).pop();
                },
                failure: (failure) => _snack(context, failure.message),
              );
            },
            icon: const Icon(Icons.save_outlined),
            label: const Text('Save'),
          ),
        ],
      );
    },
  );

  admissionNo.dispose();
  firstName.dispose();
  lastName.dispose();
  phone.dispose();
  roll.dispose();
  parentName.dispose();
  parentPhone.dispose();
}

class _Field extends StatelessWidget {
  const _Field({required this.width, required this.child});

  final double width;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return SizedBox(width: width, child: child);
  }
}

Future<void> _runPickedStudentImport(
  BuildContext context,
  WidgetRef ref,
  StudentSectionFilter filter,
) async {
  final file = await pickUploadFile(accept: '.xlsx,.xls,.csv,text/csv');
  if (file == null || !context.mounted) {
    return;
  }
  final repository = ref.read(studentsRepositoryProvider);
  final result = file.name.toLowerCase().endsWith('.csv')
      ? await repository.importCsv(file.bytes, file.name)
      : await repository.importExcel(file.bytes, file.name);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      _snack(context, 'Student import completed.');
      ref.invalidate(sectionStudentsProvider(filter));
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _runStudentDownload(
  BuildContext context,
  Future<Result<void>> action,
  String successMessage,
) async {
  final result = await action;
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) => _snack(context, successMessage),
    failure: (failure) => _snack(context, failure.message),
  );
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _blankToNull(String value) {
  return value.trim().isEmpty ? null : value.trim();
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}
