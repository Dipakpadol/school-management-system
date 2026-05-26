import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../../core/result/result.dart';
import '../../../../core/upload/file_picker.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/student_models.dart';
import '../../data/repositories/students_repository_impl.dart';
import '../controllers/students_providers.dart';

class StudentsPage extends ConsumerStatefulWidget {
  const StudentsPage({super.key});

  @override
  ConsumerState<StudentsPage> createState() => _StudentsPageState();
}

class _StudentsPageState extends ConsumerState<StudentsPage> {
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final students = ref.watch(studentsProvider);

    return AdminShell(
      title: 'Student Management',
      activeModuleId: 'students',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: Column(
        children: [
          _StudentHeader(
            searchController: _searchController,
            onAdd: () => _showAdmissionDialog(context, ref),
            onSearch: () {
              ref
                  .read(studentSearchQueryProvider.notifier)
                  .set(_searchController.text.trim());
            },
            onStatusChanged: (value) {
              ref.read(studentStatusFilterProvider.notifier).set(value);
            },
          ),
          const Divider(height: 1),
          Expanded(
            child: students.when(
              data: (items) => _StudentList(students: items),
              error: (error, _) => AppErrorState(
                message: _message(error),
                onRetry: () => ref.invalidate(studentsProvider),
              ),
              loading: () => const AppLoadingState(label: 'Loading students'),
            ),
          ),
        ],
      ),
    );
  }
}

class _StudentHeader extends ConsumerWidget {
  const _StudentHeader({
    required this.searchController,
    required this.onAdd,
    required this.onSearch,
    required this.onStatusChanged,
  });

  final TextEditingController searchController;
  final VoidCallback onAdd;
  final VoidCallback onSearch;
  final ValueChanged<String?> onStatusChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedStatus = ref.watch(studentStatusFilterProvider);

    return Material(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 18, 24, 18),
        child: Wrap(
          spacing: 12,
          runSpacing: 12,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            SizedBox(
              width: 360,
              child: TextField(
                controller: searchController,
                decoration: const InputDecoration(
                  labelText: 'Search students',
                  prefixIcon: Icon(Icons.search),
                ),
                onSubmitted: (_) => onSearch(),
              ),
            ),
            SizedBox(
              width: 210,
              child: DropdownButtonFormField<String?>(
                initialValue: selectedStatus,
                decoration: const InputDecoration(
                  labelText: 'Status',
                  prefixIcon: Icon(Icons.filter_alt_outlined),
                ),
                items: const [
                  DropdownMenuItem(value: null, child: Text('All statuses')),
                  DropdownMenuItem(value: 'ACTIVE', child: Text('Active')),
                  DropdownMenuItem(value: 'INACTIVE', child: Text('Inactive')),
                  DropdownMenuItem(
                    value: 'TRANSFERRED',
                    child: Text('Transferred'),
                  ),
                ],
                onChanged: onStatusChanged,
              ),
            ),
            OutlinedButton.icon(
              onPressed: onSearch,
              icon: const Icon(Icons.tune_outlined),
              label: const Text('Apply'),
            ),
            AppButton(
              label: 'Admit student',
              icon: Icons.person_add_alt_1_outlined,
              onPressed: onAdd,
            ),
            OutlinedButton.icon(
              onPressed: () => _runStudentDownload(
                context,
                ref.read(studentsRepositoryProvider).exportExcel(),
                'Student export downloaded.',
              ),
              icon: const Icon(Icons.download_outlined),
              label: const Text('Export'),
            ),
            OutlinedButton.icon(
              onPressed: () => _runStudentDownload(
                context,
                ref.read(studentsRepositoryProvider).downloadTemplate(),
                'Student template downloaded.',
              ),
              icon: const Icon(Icons.table_view_outlined),
              label: const Text('Template'),
            ),
            PopupMenuButton<String>(
              tooltip: 'Import students',
              icon: const Icon(Icons.upload_file_outlined),
              itemBuilder: (context) => const [
                PopupMenuItem(value: 'excel', child: Text('Import Excel')),
                PopupMenuItem(value: 'csv', child: Text('Import CSV')),
              ],
              onSelected: (format) => _runPickedStudentImport(
                context,
                ref,
                format,
              ),
            ),
            OutlinedButton.icon(
              onPressed: () => ref.invalidate(studentsProvider),
              icon: const Icon(Icons.refresh),
              label: const Text('Refresh'),
            ),
          ],
        ),
      ),
    );
  }
}

class _StudentList extends ConsumerWidget {
  const _StudentList({required this.students});

  final List<StudentSummaryModel> students;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (students.isEmpty) {
      return const Center(child: Text('No students found.'));
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 1100 ? 2 : 1;
        return GridView.builder(
          padding: const EdgeInsets.all(24),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: columns,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            mainAxisExtent: 132,
          ),
          itemCount: students.length,
          itemBuilder: (context, index) {
            final student = students[index];
            return _StudentCard(
              student: student,
              onOpen: () => _showStudentProfile(context, ref, student.id),
            );
          },
        );
      },
    );
  }
}

class _StudentCard extends StatelessWidget {
  const _StudentCard({required this.student, required this.onOpen});

  final StudentSummaryModel student;
  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onOpen,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              SizedBox.square(
                dimension: 46,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: theme.colorScheme.primaryContainer,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(
                    Icons.school_outlined,
                    color: theme.colorScheme.onPrimaryContainer,
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
                      student.displayName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      '${student.admissionNumber} - ${_classLabel(student)}',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.outline,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Wrap(
                      spacing: 8,
                      runSpacing: 6,
                      children: [
                        _StatusChip(status: student.status),
                        if (student.rollNumber != null)
                          _SoftChip(label: 'Roll ${student.rollNumber}'),
                      ],
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right),
            ],
          ),
        ),
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

class _SoftChip extends StatelessWidget {
  const _SoftChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 28,
      padding: const EdgeInsets.symmetric(horizontal: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFF1F5F9),
        borderRadius: BorderRadius.circular(14),
      ),
      alignment: Alignment.center,
      child: Text(label, style: Theme.of(context).textTheme.labelSmall),
    );
  }
}

Future<void> _showStudentProfile(
  BuildContext context,
  WidgetRef ref,
  String studentId,
) async {
  await showDialog<void>(
    context: context,
    builder: (context) {
      final profile = ref.watch(studentProfileProvider(studentId));
      return AlertDialog(
        title: const Text('Student profile'),
        content: SizedBox(
          width: 720,
          child: profile.when(
            data: (student) => _StudentProfileDetail(student: student),
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () => ref.invalidate(studentProfileProvider(studentId)),
            ),
            loading: () => const AppLoadingState(label: 'Loading profile'),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Close'),
          ),
          profile.maybeWhen(
            data: (student) {
              return OutlinedButton.icon(
                onPressed: () => _runStudentDownload(
                  context,
                  ref
                      .read(studentsRepositoryProvider)
                      .downloadProfilePdf(student.id),
                  'Student profile PDF downloaded.',
                ),
                icon: const Icon(Icons.picture_as_pdf_outlined),
                label: const Text('PDF'),
              );
            },
            orElse: () => const SizedBox.shrink(),
          ),
          profile.maybeWhen(
            data: (student) {
              return OutlinedButton.icon(
                onPressed: () async {
                  await _showEditStudentDialog(context, ref, student);
                },
                icon: const Icon(Icons.edit_outlined),
                label: const Text('Edit'),
              );
            },
            orElse: () => const SizedBox.shrink(),
          ),
          profile.maybeWhen(
            data: (student) {
              return OutlinedButton.icon(
                onPressed: () async {
                  final confirmed = await _confirm(
                    context,
                    'Delete ${student.displayName}?',
                  );
                  if (!confirmed) {
                    return;
                  }
                  final result = await ref
                      .read(studentsRepositoryProvider)
                      .delete(student.id);
                  if (!context.mounted) {
                    return;
                  }
                  result.when(
                    success: (_) {
                      ref.invalidate(studentsProvider);
                      Navigator.of(context).pop();
                    },
                    failure: (failure) => _snack(context, failure.message),
                  );
                },
                icon: const Icon(Icons.delete_outline),
                label: const Text('Delete'),
              );
            },
            orElse: () => const SizedBox.shrink(),
          ),
          profile.maybeWhen(
            data: (student) {
              final active = student.status == 'ACTIVE';
              return FilledButton.icon(
                onPressed: () async {
                  final repository = ref.read(studentsRepositoryProvider);
                  final result = active
                      ? await repository.deactivate(student.id)
                      : await repository.activate(student.id);
                  if (!context.mounted) {
                    return;
                  }
                  result.when(
                    success: (_) {
                      ref.invalidate(studentsProvider);
                      ref.invalidate(studentProfileProvider(studentId));
                    },
                    failure: (failure) => _snack(context, failure.message),
                  );
                },
                icon: Icon(active ? Icons.block_outlined : Icons.check_circle),
                label: Text(active ? 'Deactivate' : 'Activate'),
              );
            },
            orElse: () => const SizedBox.shrink(),
          ),
        ],
      );
    },
  );
}

class _StudentProfileDetail extends StatelessWidget {
  const _StudentProfileDetail({required this.student});

  final StudentProfileModel student;

  @override
  Widget build(BuildContext context) {
    final assignment = student.currentAssignment;

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 8,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              Text(
                student.displayName,
                style: Theme.of(
                  context,
                ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
              ),
              _StatusChip(status: student.status),
            ],
          ),
          const SizedBox(height: 8),
          Text('${student.admissionNumber} - ${student.gender}'),
          const SizedBox(height: 18),
          _DetailGrid(
            items: {
              'Date of birth': _dateLabel(student.dateOfBirth),
              'Mobile': student.phoneNumber ?? '-',
              'Email': student.email ?? '-',
              'Class': assignment == null
                  ? '-'
                  : '${assignment.className} ${assignment.sectionName}',
              'Roll': assignment?.rollNumber ?? '-',
              'Academic year': assignment?.academicYear ?? '-',
            },
          ),
          const SizedBox(height: 18),
          _SectionTitle(title: 'Parents'),
          const SizedBox(height: 8),
          for (final parent in student.parents)
            ListTile(
              dense: true,
              leading: const Icon(Icons.family_restroom_outlined),
              subtitle: Text('${parent.phoneNumber}  ${parent.email ?? ''}'),
              title: Text(
                '${parent.displayName} - ${parent.relationType}'
                '${parent.primaryContact ? ' - Primary' : ''}',
              ),
            ),
          const SizedBox(height: 10),
          _SectionTitle(title: 'Documents'),
          const SizedBox(height: 8),
          if (student.documents.isEmpty)
            const Text('No documents uploaded.')
          else
            for (final document in student.documents)
              ListTile(
                dense: true,
                leading: const Icon(Icons.description_outlined),
                subtitle: Text(document.verificationStatus),
                title: Text('${document.documentType} - ${document.fileName}'),
              ),
        ],
      ),
    );
  }
}

class _DetailGrid extends StatelessWidget {
  const _DetailGrid({required this.items});

  final Map<String, String> items;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      children: [
        for (final entry in items.entries)
          SizedBox(
            width: 210,
            child: DecoratedBox(
              decoration: BoxDecoration(
                border: Border.all(color: const Color(0xFFE2E8F0)),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      entry.key,
                      style: Theme.of(context).textTheme.labelMedium?.copyWith(
                        color: Theme.of(context).colorScheme.outline,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      entry.value,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Text(
      title,
      style: Theme.of(
        context,
      ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
    );
  }
}

Future<void> _showAdmissionDialog(BuildContext context, WidgetRef ref) async {
  final admissionNo = TextEditingController(
    text: 'ADM-2026-${DateTime.now().millisecondsSinceEpoch % 100000}',
  );
  final firstName = TextEditingController();
  final lastName = TextEditingController();
  final phone = TextEditingController(text: '+9198');
  final className = TextEditingController(text: 'Class 6');
  final section = TextEditingController(text: 'A');
  final roll = TextEditingController();
  final parentName = TextEditingController();
  final parentPhone = TextEditingController(text: '+9198');
  final formKey = GlobalKey<FormState>();
  String gender = 'MALE';

  await showDialog<void>(
    context: context,
    builder: (context) {
      return AlertDialog(
        title: const Text('Admit student'),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 620,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _TwoColumnFields(
                    children: [
                      TextFormField(
                        controller: admissionNo,
                        decoration: const InputDecoration(
                          labelText: 'Admission number',
                        ),
                        validator: _required,
                      ),
                      TextFormField(
                        controller: firstName,
                        decoration: const InputDecoration(
                          labelText: 'First name',
                        ),
                        validator: _required,
                      ),
                      TextFormField(
                        controller: lastName,
                        decoration: const InputDecoration(
                          labelText: 'Last name',
                        ),
                      ),
                      DropdownButtonFormField<String>(
                        initialValue: gender,
                        decoration: const InputDecoration(labelText: 'Gender'),
                        items: const [
                          DropdownMenuItem(value: 'MALE', child: Text('Male')),
                          DropdownMenuItem(
                            value: 'FEMALE',
                            child: Text('Female'),
                          ),
                          DropdownMenuItem(
                            value: 'OTHER',
                            child: Text('Other'),
                          ),
                        ],
                        onChanged: (value) => gender = value ?? gender,
                      ),
                      TextFormField(
                        controller: phone,
                        decoration: const InputDecoration(labelText: 'Mobile'),
                      ),
                      TextFormField(
                        controller: className,
                        decoration: const InputDecoration(labelText: 'Class'),
                        validator: _required,
                      ),
                      TextFormField(
                        controller: section,
                        decoration: const InputDecoration(labelText: 'Section'),
                        validator: _required,
                      ),
                      TextFormField(
                        controller: roll,
                        decoration: const InputDecoration(
                          labelText: 'Roll number',
                        ),
                      ),
                      TextFormField(
                        controller: parentName,
                        decoration: const InputDecoration(
                          labelText: 'Primary parent',
                        ),
                        validator: _required,
                      ),
                      TextFormField(
                        controller: parentPhone,
                        decoration: const InputDecoration(
                          labelText: 'Parent mobile',
                        ),
                        validator: _required,
                      ),
                    ],
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
                  'academicYear': '2026-2027',
                  'className': className.text.trim(),
                  'sectionName': section.text.trim(),
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
                  ref.invalidate(studentsProvider);
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
  className.dispose();
  section.dispose();
  roll.dispose();
  parentName.dispose();
  parentPhone.dispose();
}

Future<void> _showEditStudentDialog(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student,
) async {
  final firstName = TextEditingController(text: student.firstName);
  final lastName = TextEditingController(text: student.lastName ?? '');
  final phone = TextEditingController(text: student.phoneNumber ?? '');
  final email = TextEditingController(text: student.email ?? '');
  final formKey = GlobalKey<FormState>();

  await showDialog<void>(
    context: context,
    builder: (context) {
      return AlertDialog(
        title: const Text('Edit student'),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 480,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: firstName,
                  decoration: const InputDecoration(labelText: 'First name'),
                  validator: _required,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: lastName,
                  decoration: const InputDecoration(labelText: 'Last name'),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: phone,
                  decoration: const InputDecoration(labelText: 'Mobile'),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: email,
                  decoration: const InputDecoration(labelText: 'Email'),
                ),
              ],
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
              final result = await ref
                  .read(studentsRepositoryProvider)
                  .updateProfile(
                    student.id,
                    student.toProfilePayload(
                      firstName: firstName.text.trim(),
                      lastName: _blankToNull(lastName.text),
                      phoneNumber: _blankToNull(phone.text),
                      email: _blankToNull(email.text),
                    ),
                  );
              if (!context.mounted) {
                return;
              }
              result.when(
                success: (_) {
                  ref.invalidate(studentsProvider);
                  ref.invalidate(studentProfileProvider(student.id));
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

  firstName.dispose();
  lastName.dispose();
  phone.dispose();
  email.dispose();
}

class _TwoColumnFields extends StatelessWidget {
  const _TwoColumnFields({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final twoColumns = constraints.maxWidth >= 560;
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
        return Wrap(
          spacing: 12,
          runSpacing: 12,
          children: [
            for (final child in children)
              SizedBox(width: (constraints.maxWidth - 12) / 2, child: child),
          ],
        );
      },
    );
  }
}

String _classLabel(StudentSummaryModel student) {
  final section = student.sectionName == null ? '' : ' ${student.sectionName}';
  if (student.className == null) {
    return 'No class assigned';
  }
  return '${student.className}$section';
}

String _dateLabel(DateTime value) {
  return value.toIso8601String().split('T').first;
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
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

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
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

Future<void> _runPickedStudentImport(
  BuildContext context,
  WidgetRef ref,
  String format,
) async {
  final file = await pickUploadFile(
    accept: format == 'csv' ? '.csv,text/csv' : '.xlsx,.xls',
  );
  if (file == null || !context.mounted) {
    return;
  }
  final repository = ref.read(studentsRepositoryProvider);
  final result = format == 'csv'
      ? await repository.importCsv(file.bytes, file.name)
      : await repository.importExcel(file.bytes, file.name);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      _snack(context, 'Student import completed.');
      ref.invalidate(studentsProvider);
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<bool> _confirm(BuildContext context, String message) async {
  final result = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('Confirm action'),
      content: Text(message),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () => Navigator.of(context).pop(true),
          child: const Text('Confirm'),
        ),
      ],
    ),
  );
  return result ?? false;
}
