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
import '../../../hostel/data/models/hostel_models.dart';
import '../../../hostel/presentation/controllers/hostel_providers.dart';
import '../../../transport/data/models/transport_models.dart';
import '../../../transport/presentation/controllers/transport_providers.dart';
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
            onTemplate: () => _runStudentDownload(
              context,
              ref.read(studentsRepositoryProvider).downloadTemplate(),
              'Student template downloaded.',
            ),
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
    required this.onTemplate,
    required this.onExport,
    this.onAddStudent,
  });

  final VoidCallback onBack;
  final VoidCallback? onAddStudent;
  final VoidCallback onImport;
  final VoidCallback onTemplate;
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
              onPressed: onTemplate,
              icon: const Icon(Icons.table_view_outlined),
              label: const Text('Download template'),
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
  final dateOfBirth = TextEditingController(text: '2015-06-01');
  final admissionDate = TextEditingController(text: '2026-04-01');
  final bloodGroup = TextEditingController();
  final email = TextEditingController();
  final phone = TextEditingController(text: '+9198');
  final roll = TextEditingController();
  final previousSchool = TextEditingController();
  final addressLine1 = TextEditingController();
  final addressLine2 = TextEditingController();
  final city = TextEditingController(text: 'Bengaluru');
  final state = TextEditingController(text: 'Karnataka');
  final postalCode = TextEditingController(text: '560001');
  final country = TextEditingController(text: 'India');
  final parentName = TextEditingController();
  final parentPhone = TextEditingController(text: '+9198');
  final parentEmail = TextEditingController();
  final parentOccupation = TextEditingController();
  final hostelAllocationDate = TextEditingController(text: admissionDate.text);
  final transportAssignmentDate = TextEditingController(
    text: admissionDate.text,
  );
  final formKey = GlobalKey<FormState>();
  var gender = 'MALE';
  var status = 'ACTIVE';
  var parentRelation = 'GUARDIAN';
  var hostelRequired = false;
  var hostelFeeApplicable = true;
  var transportRequired = false;
  var transportFeeApplicable = true;
  String? selectedHostelId;
  String? selectedRoomId;
  String? selectedBedId;
  String? selectedTransportRouteId;
  String? selectedTransportPickupPointId;

  await showDialog<void>(
    context: context,
    builder: (context) {
      return StatefulBuilder(
        builder: (context, setDialogState) {
          return Consumer(
            builder: (context, ref, _) {
              final hostels = ref.watch(hostelsProvider);
              final rooms = filter.academicYearId == null
                  ? const AsyncValue.data(<HostelRoomSummaryModel>[])
                  : ref.watch(hostelRoomsProvider(filter.academicYearId!));
              final roomItems = rooms.maybeWhen(
                data: (items) => selectedHostelId == null
                    ? items
                    : items
                          .where((room) => room.hostelId == selectedHostelId)
                          .toList(),
                orElse: () => const <HostelRoomSummaryModel>[],
              );
              final selectedRoom = _hostelRoomById(roomItems, selectedRoomId);
              final availableBeds = selectedRoom == null
                  ? const <HostelBedModel>[]
                  : selectedRoom.beds
                        .where((bed) => bed.active && !bed.occupied)
                        .toList();
              final transportRoutes = filter.academicYearId == null
                  ? const AsyncValue.data(<TransportRouteModel>[])
                  : ref.watch(transportRoutesProvider(filter.academicYearId!));
              final transportRouteItems = transportRoutes.maybeWhen(
                data: (items) => items,
                orElse: () => const <TransportRouteModel>[],
              );
              final selectedTransportRoute = _transportRouteById(
                transportRouteItems,
                selectedTransportRouteId,
              );
              final pickupPoints = selectedTransportRouteId == null
                  ? const AsyncValue.data(<TransportPickupPointModel>[])
                  : ref.watch(
                      transportPickupPointsProvider(selectedTransportRouteId!),
                    );
              final pickupPointItems = pickupPoints.maybeWhen(
                data: (items) => items,
                orElse: () => const <TransportPickupPointModel>[],
              );

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
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: lastName,
                              decoration: const InputDecoration(
                                labelText: 'Last name',
                              ),
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: dateOfBirth,
                              decoration: const InputDecoration(
                                labelText: 'Date of birth',
                                hintText: 'YYYY-MM-DD',
                              ),
                              validator: _date,
                            ),
                          ),
                          _Field(
                            width: 290,
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
                              onChanged: (value) => gender = value ?? gender,
                            ),
                          ),
                          _Field(
                            width: 290,
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
                                DropdownMenuItem(
                                  value: 'TRANSFERRED',
                                  child: Text('Transferred'),
                                ),
                              ],
                              onChanged: (value) => status = value ?? status,
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: admissionDate,
                              decoration: const InputDecoration(
                                labelText: 'Admission date',
                                hintText: 'YYYY-MM-DD',
                              ),
                              validator: _date,
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
                              controller: email,
                              decoration: const InputDecoration(
                                labelText: 'E-mail',
                              ),
                              validator: _email,
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: phone,
                              decoration: const InputDecoration(
                                labelText: 'Mobile',
                              ),
                              validator: _optionalMobile,
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
                            child: DropdownButtonFormField<String>(
                              initialValue: parentRelation,
                              decoration: const InputDecoration(
                                labelText: 'Relation',
                              ),
                              items: const [
                                DropdownMenuItem(
                                  value: 'FATHER',
                                  child: Text('Father'),
                                ),
                                DropdownMenuItem(
                                  value: 'MOTHER',
                                  child: Text('Mother'),
                                ),
                                DropdownMenuItem(
                                  value: 'GUARDIAN',
                                  child: Text('Guardian'),
                                ),
                                DropdownMenuItem(
                                  value: 'OTHER',
                                  child: Text('Other'),
                                ),
                              ],
                              onChanged: (value) =>
                                  parentRelation = value ?? parentRelation,
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: parentName,
                              decoration: const InputDecoration(
                                labelText: 'Guardian',
                              ),
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
                              validator: _mobile,
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: parentEmail,
                              decoration: const InputDecoration(
                                labelText: 'Guardian email',
                              ),
                              validator: _email,
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: parentOccupation,
                              decoration: const InputDecoration(
                                labelText: 'Guardian occupation',
                              ),
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: previousSchool,
                              decoration: const InputDecoration(
                                labelText: 'Previous school',
                              ),
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: addressLine1,
                              decoration: const InputDecoration(
                                labelText: 'Address line 1',
                              ),
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: addressLine2,
                              decoration: const InputDecoration(
                                labelText: 'Address line 2',
                              ),
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: city,
                              decoration: const InputDecoration(
                                labelText: 'City',
                              ),
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: state,
                              decoration: const InputDecoration(
                                labelText: 'State',
                              ),
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: postalCode,
                              decoration: const InputDecoration(
                                labelText: 'Postal code',
                              ),
                              validator: _optionalPinCode,
                            ),
                          ),
                          _Field(
                            width: 290,
                            child: TextFormField(
                              controller: country,
                              decoration: const InputDecoration(
                                labelText: 'Country',
                              ),
                            ),
                          ),
                          _Field(
                            width: 592,
                            child: SwitchListTile(
                              contentPadding: EdgeInsets.zero,
                              title: const Text('Hostel required'),
                              value: hostelRequired,
                              onChanged: (value) => setDialogState(() {
                                hostelRequired = value;
                                selectedHostelId = null;
                                selectedRoomId = null;
                                selectedBedId = null;
                                hostelAllocationDate.text = admissionDate.text
                                    .trim();
                              }),
                            ),
                          ),
                          if (hostelRequired) ...[
                            _Field(
                              width: 290,
                              child: hostels.when(
                                data: (items) =>
                                    DropdownButtonFormField<String>(
                                      initialValue: selectedHostelId,
                                      decoration: const InputDecoration(
                                        labelText: 'Hostel',
                                      ),
                                      items: [
                                        for (final hostel in items)
                                          DropdownMenuItem(
                                            value: hostel.id,
                                            child: Text(hostel.name),
                                          ),
                                      ],
                                      validator: _required,
                                      onChanged: (value) => setDialogState(() {
                                        selectedHostelId = value;
                                        selectedRoomId = null;
                                        selectedBedId = null;
                                      }),
                                    ),
                                error: (error, _) => Text(_message(error)),
                                loading: () => const LinearProgressIndicator(),
                              ),
                            ),
                            _Field(
                              width: 290,
                              child: rooms.when(
                                data: (_) => DropdownButtonFormField<String>(
                                  initialValue: selectedRoomId,
                                  decoration: const InputDecoration(
                                    labelText: 'Room',
                                  ),
                                  items: [
                                    for (final room in roomItems)
                                      DropdownMenuItem(
                                        value: room.id,
                                        child: Text(
                                          '${room.roomNumber} (${room.availableBeds} free)',
                                        ),
                                      ),
                                  ],
                                  validator: (value) {
                                    final required = _required(value);
                                    if (required != null) {
                                      return required;
                                    }
                                    final room = _hostelRoomById(
                                      roomItems,
                                      value,
                                    );
                                    if (room != null &&
                                        room.availableBeds <= 0) {
                                      return 'Room is full';
                                    }
                                    return null;
                                  },
                                  onChanged: (value) => setDialogState(() {
                                    selectedRoomId = value;
                                    selectedBedId = null;
                                  }),
                                ),
                                error: (error, _) => Text(_message(error)),
                                loading: () => const LinearProgressIndicator(),
                              ),
                            ),
                            if (selectedRoom != null)
                              _Field(
                                width: 592,
                                child: _HostelAvailabilityLine(
                                  room: selectedRoom,
                                ),
                              ),
                            if (selectedRoom?.bedConceptEnabled ?? false)
                              _Field(
                                width: 290,
                                child: DropdownButtonFormField<String>(
                                  initialValue: selectedBedId,
                                  decoration: const InputDecoration(
                                    labelText: 'Bed',
                                  ),
                                  items: [
                                    for (final bed in availableBeds)
                                      DropdownMenuItem(
                                        value: bed.id,
                                        child: Text(bed.bedNumber),
                                      ),
                                  ],
                                  validator: _required,
                                  onChanged: (value) => setDialogState(
                                    () => selectedBedId = value,
                                  ),
                                ),
                              ),
                            _Field(
                              width: 290,
                              child: TextFormField(
                                controller: hostelAllocationDate,
                                decoration: const InputDecoration(
                                  labelText: 'Allocation date',
                                  hintText: 'YYYY-MM-DD',
                                ),
                                validator: _date,
                              ),
                            ),
                            _Field(
                              width: 290,
                              child: SwitchListTile(
                                contentPadding: EdgeInsets.zero,
                                title: const Text('Hostel fee applicable'),
                                value: hostelFeeApplicable,
                                onChanged: (value) => setDialogState(
                                  () => hostelFeeApplicable = value,
                                ),
                              ),
                            ),
                          ],
                          _Field(
                            width: 592,
                            child: SwitchListTile(
                              contentPadding: EdgeInsets.zero,
                              title: const Text('Transport required'),
                              value: transportRequired,
                              onChanged: (value) => setDialogState(() {
                                transportRequired = value;
                                selectedTransportRouteId = null;
                                selectedTransportPickupPointId = null;
                                transportAssignmentDate.text = admissionDate
                                    .text
                                    .trim();
                              }),
                            ),
                          ),
                          if (transportRequired) ...[
                            _Field(
                              width: 290,
                              child: TextFormField(
                                controller: transportAssignmentDate,
                                decoration: const InputDecoration(
                                  labelText: 'Transport assignment date',
                                  hintText: 'YYYY-MM-DD',
                                ),
                                validator: _date,
                              ),
                            ),
                            _Field(
                              width: 290,
                              child: transportRoutes.when(
                                data: (_) => DropdownButtonFormField<String>(
                                  initialValue: selectedTransportRouteId,
                                  decoration: const InputDecoration(
                                    labelText: 'Route',
                                  ),
                                  isExpanded: true,
                                  items: [
                                    for (final route in transportRouteItems)
                                      DropdownMenuItem(
                                        value: route.id,
                                        child: Text(
                                          '${route.routeName} (${_dash(route.vehicleNumber)})',
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ),
                                  ],
                                  validator: _required,
                                  onChanged: (value) => setDialogState(() {
                                    selectedTransportRouteId = value;
                                    selectedTransportPickupPointId = null;
                                  }),
                                ),
                                error: (error, _) => Text(_message(error)),
                                loading: () => const LinearProgressIndicator(),
                              ),
                            ),
                            _Field(
                              width: 290,
                              child: pickupPoints.when(
                                data: (_) => DropdownButtonFormField<String>(
                                  initialValue: selectedTransportPickupPointId,
                                  decoration: const InputDecoration(
                                    labelText: 'Pickup point',
                                  ),
                                  isExpanded: true,
                                  items: [
                                    for (final point in pickupPointItems)
                                      DropdownMenuItem(
                                        value: point.id,
                                        child: Text(
                                          point.pointName,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ),
                                  ],
                                  validator: _required,
                                  onChanged: (value) => setDialogState(
                                    () =>
                                        selectedTransportPickupPointId = value,
                                  ),
                                ),
                                error: (error, _) => Text(_message(error)),
                                loading: () => const LinearProgressIndicator(),
                              ),
                            ),
                            _Field(
                              width: 290,
                              child: InputDecorator(
                                decoration: const InputDecoration(
                                  labelText: 'Vehicle',
                                ),
                                child: Text(
                                  selectedTransportRoute?.vehicleNumber ?? '-',
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ),
                            _Field(
                              width: 290,
                              child: SwitchListTile(
                                contentPadding: EdgeInsets.zero,
                                title: const Text('Transport fee applicable'),
                                value: transportFeeApplicable,
                                onChanged: (value) => setDialogState(
                                  () => transportFeeApplicable = value,
                                ),
                              ),
                            ),
                          ],
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
                      final result = await ref
                          .read(studentsRepositoryProvider)
                          .admit({
                            'admissionNumber': admissionNo.text.trim(),
                            'profile': {
                              'firstName': firstName.text.trim(),
                              'middleName': _blankToNull(middleName.text),
                              'lastName': _blankToNull(lastName.text),
                              'dateOfBirth': dateOfBirth.text.trim(),
                              'gender': gender,
                              'bloodGroup': _blankToNull(bloodGroup.text),
                              'email': _blankToNull(email.text),
                              'phoneNumber': _blankToNull(phone.text),
                              'admissionDate': admissionDate.text.trim(),
                              'previousSchool': _blankToNull(
                                previousSchool.text,
                              ),
                              'addressLine1': _blankToNull(addressLine1.text),
                              'addressLine2': _blankToNull(addressLine2.text),
                              'city': _blankToNull(city.text),
                              'state': _blankToNull(state.text),
                              'postalCode': _blankToNull(postalCode.text),
                              'country': _blankToNull(country.text),
                            },
                            'status': status,
                            'parents': [
                              {
                                'relationType': parentRelation,
                                'primaryContact': true,
                                'emergencyContact': true,
                                'pickupAllowed': true,
                                'parent': {
                                  'firstName': parentName.text.trim(),
                                  'lastName': null,
                                  'email': _blankToNull(parentEmail.text),
                                  'phoneNumber': parentPhone.text.trim(),
                                  'alternatePhoneNumber': null,
                                  'occupation': _blankToNull(
                                    parentOccupation.text,
                                  ),
                                  'addressLine1': _blankToNull(
                                    addressLine1.text,
                                  ),
                                  'addressLine2': _blankToNull(
                                    addressLine2.text,
                                  ),
                                  'city': _blankToNull(city.text),
                                  'state': _blankToNull(state.text),
                                  'postalCode': _blankToNull(postalCode.text),
                                  'country': _blankToNull(country.text),
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
                              'effectiveFrom': admissionDate.text.trim(),
                            },
                            'documents': [],
                            'hostelAssignment': hostelRequired
                                ? {
                                    'hostelRequired': true,
                                    'academicYearId': filter.academicYearId,
                                    'hostelId': selectedHostelId,
                                    'roomId': selectedRoomId,
                                    'bedId': selectedBedId,
                                    'allocationDate': hostelAllocationDate.text
                                        .trim(),
                                    'hostelFeeApplicable': hostelFeeApplicable,
                                  }
                                : null,
                            'transportAssignment': transportRequired
                                ? {
                                    'transportRequired': true,
                                    'academicYearId': filter.academicYearId,
                                    'vehicleId':
                                        selectedTransportRoute?.vehicleId,
                                    'routeId': selectedTransportRouteId,
                                    'pickupPointId':
                                        selectedTransportPickupPointId,
                                    'assignmentDate': transportAssignmentDate
                                        .text
                                        .trim(),
                                    'transportFeeApplicable':
                                        transportFeeApplicable,
                                  }
                                : null,
                          });
                      if (!context.mounted) {
                        return;
                      }
                      result.when(
                        success: (student) {
                          ref.invalidate(sectionStudentsProvider(filter));
                          _snack(context, _studentSavedMessage(student));
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
        },
      );
    },
  );

  admissionNo.dispose();
  firstName.dispose();
  middleName.dispose();
  lastName.dispose();
  dateOfBirth.dispose();
  admissionDate.dispose();
  bloodGroup.dispose();
  email.dispose();
  phone.dispose();
  roll.dispose();
  previousSchool.dispose();
  addressLine1.dispose();
  addressLine2.dispose();
  city.dispose();
  state.dispose();
  postalCode.dispose();
  country.dispose();
  parentName.dispose();
  parentPhone.dispose();
  parentEmail.dispose();
  parentOccupation.dispose();
  hostelAllocationDate.dispose();
  transportAssignmentDate.dispose();
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

class _HostelAvailabilityLine extends StatelessWidget {
  const _HostelAvailabilityLine({required this.room});

  final HostelRoomSummaryModel room;

  @override
  Widget build(BuildContext context) {
    final color = room.availableBeds > 0 ? Colors.green : Colors.red;
    return Row(
      children: [
        Icon(Icons.bed_outlined, color: color, size: 20),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            'Room ${room.roomNumber}: ${room.occupiedCount}/${room.capacity} occupied, ${room.availableBeds} available',
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }
}

Future<void> _runPickedStudentImport(
  BuildContext context,
  WidgetRef ref,
  StudentSectionFilter filter,
) async {
  final proceed = await _showStudentImportGuide(context);
  if (proceed != true || !context.mounted) {
    return;
  }
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
    success: (summary) {
      final warningText = summary.warningRows > 0
          ? ' ${summary.warningRows} row(s) have hostel warnings.'
          : '';
      final failedText = summary.failedRows > 0
          ? ' ${summary.failedRows} row(s) failed.'
          : '';
      _snack(
        context,
        'Imported ${summary.successRows}/${summary.totalRows} student row(s).$warningText$failedText',
      );
      ref.invalidate(sectionStudentsProvider(filter));
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<bool?> _showStudentImportGuide(BuildContext context) {
  return showDialog<bool>(
    context: context,
    builder: (context) {
      return AlertDialog(
        title: const Text('Import students'),
        content: const SizedBox(
          width: 520,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Hostel columns supported in the template:'),
              SizedBox(height: 8),
              Text('hostelRequired, hostelName, roomNumber, bedNumber'),
              Text('hostelAllocationDate, hostelFeeApplicable'),
              SizedBox(height: 12),
              Text(
                'If student data is valid but hostel assignment fails, the student row is imported with a warning.',
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: () => Navigator.of(context).pop(true),
            icon: const Icon(Icons.upload_file_outlined),
            label: const Text('Choose file'),
          ),
        ],
      );
    },
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

String? _email(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  final pattern = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
  return pattern.hasMatch(text) ? null : 'Enter a valid email';
}

String? _mobile(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return 'Required';
  }
  return RegExp(r'^\+?[0-9]{10,15}$').hasMatch(text)
      ? null
      : 'Enter 10 to 15 digits';
}

String? _optionalMobile(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  return _mobile(text);
}

String? _optionalPinCode(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  return RegExp(r'^[0-9A-Za-z -]{4,12}$').hasMatch(text)
      ? null
      : 'Enter a valid pin code';
}

HostelRoomSummaryModel? _hostelRoomById(
  List<HostelRoomSummaryModel> rooms,
  String? roomId,
) {
  for (final room in rooms) {
    if (room.id == roomId) {
      return room;
    }
  }
  return null;
}

TransportRouteModel? _transportRouteById(
  List<TransportRouteModel> routes,
  String? routeId,
) {
  for (final route in routes) {
    if (route.id == routeId) {
      return route;
    }
  }
  return null;
}

String? _blankToNull(String value) {
  return value.trim().isEmpty ? null : value.trim();
}

String _dash(String? value) {
  return value == null || value.trim().isEmpty ? '-' : value.trim();
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}

String _studentSavedMessage(StudentProfileModel student) {
  final summary = student.feeAssignmentSummary;
  if (summary == null) {
    return 'Student saved successfully.';
  }
  final base =
      'Student added successfully. Class fees assigned: ${summary.classFeesAssignedCount}, '
      'Hostel fees assigned: ${summary.hostelFeesAssignedCount}, '
      'Transport fees assigned: ${summary.transportFeesAssignedCount}.';
  if (summary.warnings.isEmpty) {
    return base;
  }
  return '$base ${summary.warnings.join(' ')}';
}

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}
