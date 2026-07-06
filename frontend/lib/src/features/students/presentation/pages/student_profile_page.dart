import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../fees/data/models/fee_models.dart';
import '../../../fees/presentation/controllers/fees_providers.dart';
import '../../../hostel/data/models/hostel_models.dart';
import '../../../hostel/data/repositories/hostel_repository_impl.dart';
import '../../../hostel/presentation/controllers/hostel_providers.dart';
import '../../../transport/data/models/transport_models.dart';
import '../../../transport/data/repositories/transport_repository_impl.dart';
import '../../../transport/presentation/controllers/transport_providers.dart';
import '../../data/models/student_models.dart';
import '../../data/repositories/students_repository_impl.dart';
import '../controllers/students_providers.dart';
import '../widgets/student_attendance_tab.dart';
import '../widgets/student_exam_results_tab.dart';

class StudentProfilePage extends ConsumerWidget {
  const StudentProfilePage({required this.studentId, super.key});

  final String studentId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profile = ref.watch(studentProfileProvider(studentId));

    return AdminShell(
      title: 'Student Profile',
      activeModuleId: 'students',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: profile.when(
        data: (student) => _ProfileScaffold(student: student),
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(studentProfileProvider(studentId)),
        ),
        loading: () => const AppLoadingState(label: 'Loading profile'),
      ),
    );
  }
}

class _ProfileScaffold extends StatelessWidget {
  const _ProfileScaffold({required this.student});

  final StudentProfileModel student;

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 9,
      child: Column(
        children: [
          Material(
            color: Colors.white,
            child: Column(
              children: [
                SizedBox(
                  height: 72,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: Row(
                      children: [
                        IconButton(
                          tooltip: 'Back',
                          onPressed: () => context.go(AppRoutes.students),
                          icon: const Icon(Icons.arrow_back),
                        ),
                        const SizedBox(width: 8),
                        _PhotoAvatar(student: student, radius: 22),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                student.fullName,
                                overflow: TextOverflow.ellipsis,
                                style: Theme.of(context).textTheme.titleMedium
                                    ?.copyWith(fontWeight: FontWeight.w800),
                              ),
                              Text(
                                '${student.admissionNumber} - ${student.currentAssignment?.className ?? 'No class'}',
                                overflow: TextOverflow.ellipsis,
                                style: Theme.of(context).textTheme.bodySmall,
                              ),
                            ],
                          ),
                        ),
                        _StatusChip(status: student.status),
                      ],
                    ),
                  ),
                ),
                const TabBar(
                  isScrollable: true,
                  tabs: [
                    Tab(text: 'Personal Details'),
                    Tab(text: 'Parent / Guardian Details'),
                    Tab(text: 'Academic Details'),
                    Tab(text: 'Attendance'),
                    Tab(text: 'Fees'),
                    Tab(text: 'Documents'),
                    Tab(text: 'Hostel'),
                    Tab(text: 'Transport'),
                    Tab(text: 'Exams & Results'),
                  ],
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: TabBarView(
              children: [
                _PersonalTab(student: student),
                _ParentsTab(student: student),
                _AcademicTab(student: student),
                StudentAttendanceTab(student: student),
                _FeesTab(student: student),
                _DocumentsTab(student: student),
                _HostelTab(student: student),
                _TransportTab(student: student),
                StudentExamResultsTab(student: student),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PersonalTab extends ConsumerWidget {
  const _PersonalTab({required this.student});

  final StudentProfileModel student;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final active = student.status == 'ACTIVE';

    return _TabSurface(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SectionHeader(
            title: 'Personal Details',
            actions: [
              OutlinedButton.icon(
                onPressed: () => _showPhotoDialog(context, ref, student),
                icon: const Icon(Icons.add_a_photo_outlined),
                label: const Text('Photo'),
              ),
              OutlinedButton.icon(
                onPressed: () => _toggleStatus(context, ref, student),
                icon: Icon(
                  active
                      ? Icons.pause_circle_outline
                      : Icons.play_circle_outline,
                ),
                label: Text(active ? 'Deactivate' : 'Activate'),
              ),
              FilledButton.icon(
                onPressed: () => _showPersonalDialog(context, ref, student),
                icon: const Icon(Icons.edit_outlined),
                label: const Text('Edit'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 18,
            runSpacing: 18,
            crossAxisAlignment: WrapCrossAlignment.start,
            children: [
              _PhotoAvatar(student: student, radius: 52),
              ExpandedGrid(
                items: {
                  'Full name': student.fullName,
                  'First name': student.firstName,
                  'Middle name': student.middleName,
                  'Last name': student.lastName,
                  'Admission no.': student.admissionNumber,
                  'Admission date': _dateLabel(student.admissionDate),
                  'Date of birth': _dateLabel(student.dateOfBirth),
                  'Gender': student.gender,
                  'Blood group': student.bloodGroup,
                  'Mobile': student.phoneNumber,
                  'Email': student.email,
                  'Address': _address(student),
                  'City': student.city,
                  'State': student.state,
                  'Pin code': student.postalCode,
                  'Status': student.status,
                },
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ParentsTab extends ConsumerWidget {
  const _ParentsTab({required this.student});

  final StudentProfileModel student;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final parents = ref.watch(studentParentsProvider(student.id));

    return _TabSurface(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SectionHeader(
            title: 'Parent / Guardian Details',
            actions: [
              FilledButton.icon(
                onPressed: () => _showParentDialog(context, ref, student),
                icon: const Icon(Icons.person_add_alt_1_outlined),
                label: const Text('Add'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          parents.when(
            data: (items) {
              if (items.isEmpty) {
                return const _InlineEmpty(
                  message: 'No parent/guardian details available.',
                );
              }
              return Column(
                children: [
                  for (final parent in items)
                    _ParentTile(student: student, parent: parent),
                ],
              );
            },
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () => ref.invalidate(studentParentsProvider(student.id)),
            ),
            loading: () => const AppLoadingState(label: 'Loading parents'),
          ),
        ],
      ),
    );
  }
}

class _ParentTile extends ConsumerWidget {
  const _ParentTile({required this.student, required this.parent});

  final StudentProfileModel student;
  final StudentParentModel parent;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFFE2E8F0)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: ListTile(
        contentPadding: EdgeInsets.zero,
        leading: const Icon(Icons.family_restroom_outlined),
        title: Wrap(
          spacing: 8,
          runSpacing: 6,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            Text(
              parent.displayName,
              style: Theme.of(
                context,
              ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
            ),
            _SmallBadge(label: parent.relationType),
            if (parent.primaryContact) const _SmallBadge(label: 'Primary'),
          ],
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 8),
          child: ExpandedGrid(
            items: {
              'Mobile': parent.phoneNumber,
              'Alternate mobile': parent.alternatePhoneNumber,
              'Email': parent.email,
              'Occupation': parent.occupation,
              'Address': _parentAddress(parent),
            },
          ),
        ),
        trailing: Wrap(
          spacing: 4,
          children: [
            if (!parent.primaryContact)
              IconButton(
                tooltip: 'Mark primary',
                onPressed: () =>
                    _markParentPrimary(context, ref, student, parent),
                icon: const Icon(Icons.star_outline),
              ),
            IconButton(
              tooltip: 'Edit',
              onPressed: () => _showParentDialog(context, ref, student, parent),
              icon: const Icon(Icons.edit_outlined),
            ),
            IconButton(
              tooltip: 'Remove',
              onPressed: () => _deleteParent(context, ref, student, parent),
              icon: const Icon(Icons.delete_outline),
            ),
          ],
        ),
      ),
    );
  }
}

class _AcademicTab extends ConsumerWidget {
  const _AcademicTab({required this.student});

  final StudentProfileModel student;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return _TabSurface(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SectionHeader(
            title: 'Academic Details',
            actions: [
              FilledButton.icon(
                onPressed: () => _showClassDialog(context, ref, student),
                icon: const Icon(Icons.swap_horiz_outlined),
                label: const Text('Assign class'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (student.classAssignments.isEmpty)
            const _InlineEmpty(message: 'No academic assignments.')
          else
            for (final assignment in student.classAssignments)
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: Icon(
                  assignment.active
                      ? Icons.check_circle_outline
                      : Icons.history_outlined,
                ),
                title: Text(
                  '${assignment.className} ${assignment.sectionName}',
                ),
                subtitle: Text(
                  [
                    assignment.academicYear,
                    'Roll ${assignment.rollNumber ?? '-'}',
                    'From ${_dateLabel(assignment.effectiveFrom)}',
                  ].join(' - '),
                ),
              ),
        ],
      ),
    );
  }
}

class _FeesTab extends ConsumerWidget {
  const _FeesTab({required this.student});

  final StudentProfileModel student;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final summary = ref.watch(studentFeeSummaryProvider(student.id));
    final history = ref.watch(studentPaymentHistoryProvider(student.id));

    return _TabSurface(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SectionHeader(
            title: 'Fees',
            actions: [
              FilledButton.icon(
                onPressed: () => context.go(
                  AppRoutes.studentFeePaymentCollection(student.id),
                ),
                icon: const Icon(Icons.point_of_sale_outlined),
                label: const Text('Collect payment'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          summary.when(
            data: (item) => Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                ExpandedGrid(
                  items: {
                    'Student': item.studentName,
                    'Admission no.': item.admissionNumber,
                    'Total assigned': _money(item.grossAmount),
                    'Total paid': _money(item.paidAmount),
                    'Total pending': _money(item.balanceAmount),
                  },
                ),
                const SizedBox(height: 18),
                _FeeGroupSection(
                  title: 'Class Fees',
                  fees: item.classFees,
                  emptyMessage: 'No class fees assigned.',
                ),
                const SizedBox(height: 16),
                _FeeGroupSection(
                  title: 'Hostel Fees',
                  fees: item.hostelFees,
                  emptyMessage: 'No hostel fees assigned.',
                ),
                const SizedBox(height: 16),
                _FeeGroupSection(
                  title: 'Transport Fees',
                  fees: item.transportFees,
                  emptyMessage: 'No transport fees assigned.',
                ),
              ],
            ),
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () =>
                  ref.invalidate(studentFeeSummaryProvider(student.id)),
            ),
            loading: () => const AppLoadingState(label: 'Loading fees'),
          ),
          const SizedBox(height: 18),
          Text(
            'Payment History',
            style: Theme.of(
              context,
            ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          history.when(
            data: (items) {
              if (items.isEmpty) {
                return const _InlineEmpty(message: 'No payments collected.');
              }
              return Column(
                children: [
                  for (final payment in items)
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.receipt_long_outlined),
                      title: Text(
                        '${_money(payment.amount)} - ${payment.paymentMode}',
                      ),
                      subtitle: Text(
                        [
                          _dateLabel(payment.paymentDate),
                          if ((payment.referenceNumber ?? '').isNotEmpty)
                            payment.referenceNumber!,
                          payment.receiptNumber,
                        ].join(' - '),
                      ),
                      trailing: _SmallBadge(label: payment.status),
                    ),
                ],
              );
            },
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () =>
                  ref.invalidate(studentPaymentHistoryProvider(student.id)),
            ),
            loading: () => const AppLoadingState(label: 'Loading payments'),
          ),
        ],
      ),
    );
  }
}

class _FeeGroupSection extends StatelessWidget {
  const _FeeGroupSection({
    required this.title,
    required this.fees,
    required this.emptyMessage,
  });

  final String title;
  final List<StudentFeeAssignmentModel> fees;
  final String emptyMessage;

  @override
  Widget build(BuildContext context) {
    final paid = fees.fold(0.0, (sum, fee) => sum + fee.paidAmount);
    final pending = fees.fold(0.0, (sum, fee) => sum + fee.balanceAmount);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: Theme.of(
            context,
          ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        if (fees.isEmpty)
          _InlineEmpty(message: emptyMessage)
        else ...[
          for (final fee in fees)
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.receipt_long_outlined),
              title: Text(fee.feeStructureName),
              subtitle: Text(
                [
                  fee.academicYear,
                  _feeScopeLabel(fee),
                  'Paid ${_money(fee.paidAmount)}',
                  'Pending ${_money(fee.balanceAmount)}',
                ].where((value) => value.trim().isNotEmpty).join(' - '),
              ),
              trailing: _SmallBadge(label: fee.status),
            ),
          ExpandedGrid(
            items: {'Paid': _money(paid), 'Pending': _money(pending)},
          ),
        ],
      ],
    );
  }
}

class _DocumentsTab extends ConsumerWidget {
  const _DocumentsTab({required this.student});

  final StudentProfileModel student;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return _TabSurface(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SectionHeader(
            title: 'Documents',
            actions: [
              FilledButton.icon(
                onPressed: () => _showDocumentDialog(context, ref, student),
                icon: const Icon(Icons.upload_file_outlined),
                label: const Text('Upload'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (student.documents.isEmpty)
            const _InlineEmpty(message: 'No documents uploaded.')
          else
            for (final document in student.documents)
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.description_outlined),
                title: Text(document.fileName),
                subtitle: Text(
                  [
                    document.documentType,
                    document.verificationStatus,
                    if (document.createdAt != null)
                      'Uploaded ${_dateLabel(document.createdAt!)}',
                  ].join(' - '),
                ),
                trailing: Wrap(
                  spacing: 4,
                  children: [
                    if (document.fileUrl != null)
                      IconButton(
                        tooltip: 'Download',
                        onPressed: () => _snack(
                          context,
                          'Open this document URL from the stored file link.',
                        ),
                        icon: const Icon(Icons.download_outlined),
                      ),
                    IconButton(
                      tooltip: 'Edit',
                      onPressed: () =>
                          _showDocumentDialog(context, ref, student, document),
                      icon: const Icon(Icons.edit_outlined),
                    ),
                    IconButton(
                      tooltip: 'Delete',
                      onPressed: () =>
                          _deleteDocument(context, ref, student, document),
                      icon: const Icon(Icons.delete_outline),
                    ),
                  ],
                ),
              ),
        ],
      ),
    );
  }
}

class _HostelTab extends ConsumerWidget {
  const _HostelTab({required this.student});

  final StudentProfileModel student;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final academicYearId = student.currentAssignment?.academicYearId;
    final allocationKey = academicYearId == null || academicYearId.isEmpty
        ? null
        : StudentHostelAllocationKey(
            studentId: student.id,
            academicYearId: academicYearId,
          );
    final allocation = allocationKey == null
        ? null
        : ref.watch(studentCurrentHostelAllocationProvider(allocationKey));
    final fees = ref.watch(studentHostelFeesProvider(student.id));

    return _TabSurface(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SectionHeader(
            title: 'Hostel',
            actions: [
              OutlinedButton.icon(
                onPressed: () =>
                    _showStudentHostelAssignmentDialog(context, ref, student),
                icon: const Icon(Icons.hotel_outlined),
                label: const Text('Assign hostel'),
              ),
              FilledButton.icon(
                onPressed: () => context.go(
                  AppRoutes.studentFeePaymentCollection(student.id),
                ),
                icon: const Icon(Icons.point_of_sale_outlined),
                label: const Text('Collect payment'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (allocationKey == null)
            const _InlineEmpty(
              message: 'Assign class and academic year before hostel.',
            )
          else
            allocation!.when(
              data: (item) {
                if (item == null) {
                  return const _InlineEmpty(
                    message: 'No hostel allocation found.',
                  );
                }
                return _HostelAllocationTile(
                  allocation: item,
                  onChange: () => _showStudentHostelAssignmentDialog(
                    context,
                    ref,
                    student,
                    existingAllocation: item,
                  ),
                  onVacate: () => _vacateStudentHostelAllocation(
                    context,
                    ref,
                    student,
                    item,
                  ),
                );
              },
              error: (error, _) => AppErrorState(
                message: _message(error),
                onRetry: () => ref.invalidate(
                  studentCurrentHostelAllocationProvider(allocationKey),
                ),
              ),
              loading: () => const AppLoadingState(label: 'Loading hostel'),
            ),
          const SizedBox(height: 20),
          Text(
            'Hostel Allocation History',
            style: Theme.of(
              context,
            ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          ref
              .watch(studentHostelAllocationsProvider(student.id))
              .when(
                data: (items) {
                  final history = items.where(
                    (item) => item.status != 'ACTIVE',
                  );
                  if (history.isEmpty) {
                    return const _InlineEmpty(
                      message: 'No previous hostel allocation is recorded.',
                    );
                  }
                  return Column(
                    children: [
                      for (final allocation in history)
                        _HostelAllocationTile(allocation: allocation),
                    ],
                  );
                },
                error: (error, _) => AppErrorState(
                  message: _message(error),
                  onRetry: () => ref.invalidate(
                    studentHostelAllocationsProvider(student.id),
                  ),
                ),
                loading: () => const AppLoadingState(label: 'Loading hostel'),
              ),
          const SizedBox(height: 20),
          Text(
            'Hostel Fees',
            style: Theme.of(
              context,
            ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          fees.when(
            data: (items) {
              if (items.isEmpty) {
                return const _InlineEmpty(message: 'No hostel fees assigned.');
              }
              return Column(
                children: [
                  for (final fee in items)
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.receipt_long_outlined),
                      title: Text(fee.feeStructureName),
                      subtitle: Text(
                        [
                          fee.academicYear,
                          'Paid ${fee.paidAmount.toStringAsFixed(2)}',
                          'Pending ${fee.balanceAmount.toStringAsFixed(2)}',
                        ].join(' - '),
                      ),
                      trailing: _SmallBadge(label: fee.status),
                    ),
                  const SizedBox(height: 8),
                  ExpandedGrid(
                    items: {
                      'Total assigned': items
                          .fold<double>(0, (sum, fee) => sum + fee.grossAmount)
                          .toStringAsFixed(2),
                      'Total paid': items
                          .fold<double>(0, (sum, fee) => sum + fee.paidAmount)
                          .toStringAsFixed(2),
                      'Total pending': items
                          .fold<double>(
                            0,
                            (sum, fee) => sum + fee.balanceAmount,
                          )
                          .toStringAsFixed(2),
                    },
                  ),
                ],
              );
            },
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () =>
                  ref.invalidate(studentHostelFeesProvider(student.id)),
            ),
            loading: () => const AppLoadingState(label: 'Loading hostel fees'),
          ),
        ],
      ),
    );
  }
}

class _HostelAllocationTile extends StatelessWidget {
  const _HostelAllocationTile({
    required this.allocation,
    this.onChange,
    this.onVacate,
  });

  final HostelAllocationModel allocation;
  final VoidCallback? onChange;
  final VoidCallback? onVacate;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: const Icon(Icons.hotel_outlined),
      title: Text(
        '${allocation.hostelName} - Room ${allocation.roomNumber}',
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Text(
        [
          allocation.academicYear,
          allocation.roomType,
          if (allocation.bedNumber != null) 'Bed ${allocation.bedNumber}',
          'From ${_nullableDateLabel(allocation.allocationDate)}',
          'Fee ${allocation.feeAssignedStatus}',
          if (allocation.vacateDate != null)
            'Vacated ${_nullableDateLabel(allocation.vacateDate)}',
        ].join(' - '),
      ),
      trailing: Wrap(
        spacing: 4,
        crossAxisAlignment: WrapCrossAlignment.center,
        children: [
          _SmallBadge(label: allocation.status),
          if (onChange != null)
            IconButton(
              tooltip: 'Change room',
              onPressed: onChange,
              icon: const Icon(Icons.swap_horiz_outlined),
            ),
          if (onVacate != null)
            IconButton(
              tooltip: 'Vacate',
              onPressed: onVacate,
              icon: const Icon(Icons.meeting_room_outlined),
            ),
        ],
      ),
    );
  }
}

class _TransportTab extends ConsumerWidget {
  const _TransportTab({required this.student});

  final StudentProfileModel student;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final academicYearId = student.currentAssignment?.academicYearId;
    final assignmentKey = academicYearId == null || academicYearId.isEmpty
        ? null
        : StudentTransportAssignmentKey(
            studentId: student.id,
            academicYearId: academicYearId,
          );
    final assignment = assignmentKey == null
        ? null
        : ref.watch(studentCurrentTransportAssignmentProvider(assignmentKey));

    return _TabSurface(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SectionHeader(
            title: 'Transport',
            actions: [
              OutlinedButton.icon(
                onPressed: () =>
                    _showStudentTransportDialog(context, ref, student),
                icon: const Icon(Icons.directions_bus_outlined),
                label: const Text('Assign transport'),
              ),
              FilledButton.icon(
                onPressed: () => context.go(
                  AppRoutes.studentFeePaymentCollection(student.id),
                ),
                icon: const Icon(Icons.point_of_sale_outlined),
                label: const Text('Collect payment'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (assignmentKey == null)
            const _InlineEmpty(
              message: 'Assign class and academic year before transport.',
            )
          else
            assignment!.when(
              data: (item) {
                if (item == null) {
                  return const _InlineEmpty(
                    message: 'No transport assignment found.',
                  );
                }
                return _TransportAssignmentTile(
                  assignment: item,
                  onChange: () => _showStudentTransportDialog(
                    context,
                    ref,
                    student,
                    existingAssignment: item,
                  ),
                  onRemove: () => _removeStudentTransportAssignment(
                    context,
                    ref,
                    student,
                    item,
                  ),
                );
              },
              error: (error, _) => AppErrorState(
                message: _message(error),
                onRetry: () => ref.invalidate(
                  studentCurrentTransportAssignmentProvider(assignmentKey),
                ),
              ),
              loading: () => const AppLoadingState(label: 'Loading transport'),
            ),
        ],
      ),
    );
  }
}

class _TransportAssignmentTile extends StatelessWidget {
  const _TransportAssignmentTile({
    required this.assignment,
    this.onChange,
    this.onRemove,
  });

  final StudentTransportAssignmentModel assignment;
  final VoidCallback? onChange;
  final VoidCallback? onRemove;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: const Icon(Icons.directions_bus_outlined),
      title: Text(
        '${assignment.vehicleNumber} - ${assignment.routeName}',
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Padding(
        padding: const EdgeInsets.only(top: 8),
        child: ExpandedGrid(
          items: {
            'Academic year': assignment.academicYear,
            'Vehicle': '${assignment.vehicleNumber} ${assignment.vehicleName}',
            'Route': assignment.routeName,
            'Pickup point': assignment.pickupPointName,
            'Pickup time': assignment.pickupTime,
            'Drop time': assignment.dropTime,
            'Driver': assignment.driverName,
            'Driver mobile': assignment.driverMobile,
            'Assignment date': _nullableDateLabel(assignment.assignmentDate),
            'Fee status': assignment.feeAssignedStatus,
          },
        ),
      ),
      trailing: Wrap(
        spacing: 4,
        crossAxisAlignment: WrapCrossAlignment.center,
        children: [
          _SmallBadge(label: assignment.status),
          if (onChange != null)
            IconButton(
              tooltip: 'Change transport',
              onPressed: onChange,
              icon: const Icon(Icons.swap_horiz_outlined),
            ),
          if (onRemove != null)
            IconButton(
              tooltip: 'Remove transport',
              onPressed: onRemove,
              icon: const Icon(Icons.link_off_outlined),
            ),
        ],
      ),
    );
  }
}

Future<void> _showStudentTransportDialog(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student, {
  StudentTransportAssignmentModel? existingAssignment,
}) async {
  final academicYearId = student.currentAssignment?.academicYearId;
  if (academicYearId == null || academicYearId.isEmpty) {
    _snack(context, 'Assign class and academic year before transport.');
    return;
  }

  final formKey = GlobalKey<FormState>();
  final assignmentDate = TextEditingController(
    text: existingAssignment?.assignmentDate == null
        ? _dateLabel(DateTime.now())
        : _dateLabel(existingAssignment!.assignmentDate!),
  );
  var selectedRouteId = existingAssignment?.routeId;
  var selectedPickupPointId = existingAssignment?.pickupPointId;
  var transportFeeApplicable = true;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          return Consumer(
            builder: (dialogContext, ref, _) {
              final routes = ref.watch(transportRoutesProvider(academicYearId));
              final pickupPoints = selectedRouteId == null
                  ? const AsyncValue.data(<TransportPickupPointModel>[])
                  : ref.watch(transportPickupPointsProvider(selectedRouteId!));
              final routeItems = routes.maybeWhen(
                data: (items) => items,
                orElse: () => const <TransportRouteModel>[],
              );
              final pickupItems = pickupPoints.maybeWhen(
                data: (items) => items,
                orElse: () => const <TransportPickupPointModel>[],
              );
              final selectedRoute = _transportRouteById(
                routeItems,
                selectedRouteId,
              );

              return AlertDialog(
                title: Text(
                  existingAssignment == null
                      ? 'Assign transport'
                      : 'Change transport',
                ),
                content: Form(
                  key: formKey,
                  child: SizedBox(
                    width: 620,
                    child: Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: [
                        _Field(
                          controller: assignmentDate,
                          label: 'Assignment date',
                          validator: _date,
                        ),
                        SizedBox(
                          width: 290,
                          child: routes.when(
                            data: (_) => DropdownButtonFormField<String>(
                              initialValue: selectedRouteId,
                              decoration: const InputDecoration(
                                labelText: 'Route',
                              ),
                              isExpanded: true,
                              items: [
                                for (final route in routeItems)
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
                                selectedRouteId = value;
                                selectedPickupPointId = null;
                              }),
                            ),
                            error: (error, _) => Text(_message(error)),
                            loading: () => const LinearProgressIndicator(),
                          ),
                        ),
                        SizedBox(
                          width: 290,
                          child: pickupPoints.when(
                            data: (_) => DropdownButtonFormField<String>(
                              initialValue: selectedPickupPointId,
                              decoration: const InputDecoration(
                                labelText: 'Pickup point',
                              ),
                              isExpanded: true,
                              items: [
                                for (final point in pickupItems)
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
                                () => selectedPickupPointId = value,
                              ),
                            ),
                            error: (error, _) => Text(_message(error)),
                            loading: () => const LinearProgressIndicator(),
                          ),
                        ),
                        SizedBox(
                          width: 290,
                          child: InputDecorator(
                            decoration: const InputDecoration(
                              labelText: 'Vehicle',
                            ),
                            child: Text(
                              selectedRoute?.vehicleNumber ?? '-',
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ),
                        SizedBox(
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
                        'transportRequired': true,
                        'academicYearId': academicYearId,
                        'vehicleId': selectedRoute?.vehicleId,
                        'routeId': selectedRouteId,
                        'pickupPointId': selectedPickupPointId,
                        'assignmentDate': assignmentDate.text.trim(),
                        'transportFeeApplicable': transportFeeApplicable,
                      };
                      final repository = ref.read(transportRepositoryProvider);
                      final result = existingAssignment == null
                          ? await repository.assignStudentTransport(
                              student.id,
                              payload,
                            )
                          : await repository.changeStudentTransport(
                              student.id,
                              existingAssignment.id,
                              payload,
                            );
                      if (!dialogContext.mounted) {
                        return;
                      }
                      result.when(
                        success: (_) {
                          _refreshStudentTransportState(
                            ref,
                            student.id,
                            academicYearId,
                          );
                          _snack(context, 'Transport assignment saved.');
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

  assignmentDate.dispose();
}

Future<void> _removeStudentTransportAssignment(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student,
  StudentTransportAssignmentModel assignment,
) async {
  final endDate = TextEditingController(text: _dateLabel(DateTime.now()));
  final formKey = GlobalKey<FormState>();

  await showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return AlertDialog(
        title: const Text('Remove transport'),
        content: Form(
          key: formKey,
          child: _Field(
            controller: endDate,
            label: 'End date',
            validator: _date,
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
              final result = await ref
                  .read(transportRepositoryProvider)
                  .removeStudentTransport(student.id, assignment.id, {
                    'endDate': endDate.text.trim(),
                  });
              if (!dialogContext.mounted) {
                return;
              }
              result.when(
                success: (_) {
                  _refreshStudentTransportState(
                    ref,
                    student.id,
                    assignment.academicYearId,
                  );
                  _snack(context, 'Transport assignment removed.');
                  Navigator.of(dialogContext).pop();
                },
                failure: (failure) => _snack(dialogContext, failure.message),
              );
            },
            icon: const Icon(Icons.link_off_outlined),
            label: const Text('Remove'),
          ),
        ],
      );
    },
  );

  endDate.dispose();
}

Future<void> _showStudentHostelAssignmentDialog(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student, {
  HostelAllocationModel? existingAllocation,
}) async {
  final academicYearId = student.currentAssignment?.academicYearId;
  if (academicYearId == null || academicYearId.isEmpty) {
    _snack(context, 'Assign class and academic year before hostel.');
    return;
  }

  final isChange = existingAllocation != null;
  final formKey = GlobalKey<FormState>();
  final allocationDate = TextEditingController(
    text: _dateLabel(DateTime.now()),
  );
  String? selectedHostelId = existingAllocation?.hostelId;
  String? selectedRoomId;
  String? selectedBedId;
  var hostelFeeApplicable = true;

  await showDialog<void>(
    context: context,
    builder: (context) {
      return StatefulBuilder(
        builder: (context, setDialogState) {
          return Consumer(
            builder: (context, ref, _) {
              final hostels = ref.watch(hostelsProvider);
              final rooms = ref.watch(hostelRoomsProvider(academicYearId));
              final roomItems = rooms.maybeWhen(
                data: (items) => selectedHostelId == null
                    ? items
                    : items
                          .where((room) => room.hostelId == selectedHostelId)
                          .where(
                            (room) => room.id != existingAllocation?.roomId,
                          )
                          .toList(),
                orElse: () => const <HostelRoomSummaryModel>[],
              );
              final selectedRoom = _profileHostelRoomById(
                roomItems,
                selectedRoomId,
              );
              final freeBeds = selectedRoom == null
                  ? const <HostelBedModel>[]
                  : selectedRoom.beds
                        .where((bed) => bed.active && !bed.occupied)
                        .toList();

              return AlertDialog(
                title: Text(
                  '${isChange ? 'Change hostel' : 'Assign hostel'} - ${student.fullName}',
                ),
                content: Form(
                  key: formKey,
                  child: SizedBox(
                    width: 560,
                    child: SingleChildScrollView(
                      child: Wrap(
                        spacing: 12,
                        runSpacing: 12,
                        children: [
                          SizedBox(
                            width: 260,
                            child: hostels.when(
                              data: (items) => DropdownButtonFormField<String>(
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
                          SizedBox(
                            width: 260,
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
                                  final room = _profileHostelRoomById(
                                    roomItems,
                                    value,
                                  );
                                  if (room != null && room.availableBeds <= 0) {
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
                          if (selectedRoom?.bedConceptEnabled ?? false)
                            SizedBox(
                              width: 260,
                              child: DropdownButtonFormField<String>(
                                initialValue: selectedBedId,
                                decoration: const InputDecoration(
                                  labelText: 'Bed',
                                ),
                                items: [
                                  for (final bed in freeBeds)
                                    DropdownMenuItem(
                                      value: bed.id,
                                      child: Text(bed.bedNumber),
                                    ),
                                ],
                                validator: _required,
                                onChanged: (value) =>
                                    setDialogState(() => selectedBedId = value),
                              ),
                            ),
                          SizedBox(
                            width: 260,
                            child: TextFormField(
                              controller: allocationDate,
                              decoration: const InputDecoration(
                                labelText: 'Allocation date',
                              ),
                              validator: _date,
                            ),
                          ),
                          SizedBox(
                            width: 260,
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
                      final repository = ref.read(hostelRepositoryProvider);
                      final result = isChange
                          ? await repository.changeStudentHostelRoom(
                              student.id,
                              existingAllocation.id,
                              {
                                'roomId': selectedRoomId,
                                'bedId': selectedBedId,
                                'allocationDate': allocationDate.text.trim(),
                                'hostelFeeApplicable': hostelFeeApplicable,
                              },
                            )
                          : await repository
                                .assignStudentHostelAllocation(student.id, {
                                  'hostelRequired': true,
                                  'academicYearId': academicYearId,
                                  'hostelId': selectedHostelId,
                                  'roomId': selectedRoomId,
                                  'bedId': selectedBedId,
                                  'allocationDate': allocationDate.text.trim(),
                                  'hostelFeeApplicable': hostelFeeApplicable,
                                });
                      if (!context.mounted) {
                        return;
                      }
                      result.when(
                        success: (_) {
                          ref.invalidate(
                            studentCurrentHostelAllocationProvider(
                              StudentHostelAllocationKey(
                                studentId: student.id,
                                academicYearId: academicYearId,
                              ),
                            ),
                          );
                          ref.invalidate(
                            studentHostelAllocationsProvider(student.id),
                          );
                          ref.invalidate(studentHostelFeesProvider(student.id));
                          ref.invalidate(hostelRoomsProvider(academicYearId));
                          _snack(
                            context,
                            isChange
                                ? 'Hostel room changed.'
                                : 'Hostel assigned.',
                          );
                          Navigator.of(context).pop();
                        },
                        failure: (failure) => _snack(context, failure.message),
                      );
                    },
                    icon: Icon(
                      isChange
                          ? Icons.swap_horiz_outlined
                          : Icons.check_outlined,
                    ),
                    label: Text(isChange ? 'Change' : 'Assign'),
                  ),
                ],
              );
            },
          );
        },
      );
    },
  );
  allocationDate.dispose();
}

Future<void> _vacateStudentHostelAllocation(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student,
  HostelAllocationModel allocation,
) async {
  final academicYearId =
      allocation.academicYearId ?? student.currentAssignment?.academicYearId;
  final formKey = GlobalKey<FormState>();
  final vacateDate = TextEditingController(text: _dateLabel(DateTime.now()));
  await showDialog<void>(
    context: context,
    builder: (context) {
      return AlertDialog(
        title: Text('Vacate hostel - ${student.fullName}'),
        content: Form(
          key: formKey,
          child: TextFormField(
            controller: vacateDate,
            decoration: const InputDecoration(labelText: 'Vacate date'),
            validator: _date,
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
                  .read(hostelRepositoryProvider)
                  .vacateStudentHostelAllocation(student.id, allocation.id, {
                    'vacateDate': vacateDate.text.trim(),
                  });
              if (!context.mounted) {
                return;
              }
              result.when(
                success: (_) {
                  if (academicYearId != null && academicYearId.isNotEmpty) {
                    ref.invalidate(
                      studentCurrentHostelAllocationProvider(
                        StudentHostelAllocationKey(
                          studentId: student.id,
                          academicYearId: academicYearId,
                        ),
                      ),
                    );
                    ref.invalidate(hostelRoomsProvider(academicYearId));
                  }
                  ref.invalidate(studentHostelAllocationsProvider(student.id));
                  ref.invalidate(studentHostelFeesProvider(student.id));
                  _snack(context, 'Hostel allocation vacated.');
                  Navigator.of(context).pop();
                },
                failure: (failure) => _snack(context, failure.message),
              );
            },
            icon: const Icon(Icons.meeting_room_outlined),
            label: const Text('Vacate'),
          ),
        ],
      );
    },
  );
  vacateDate.dispose();
}

class _TabSurface extends StatelessWidget {
  const _TabSurface({required this.child});

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

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.title, this.actions = const []});

  final String title;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        SizedBox(
          width: 260,
          child: Text(
            title,
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
          ),
        ),
        ...actions,
      ],
    );
  }
}

class ExpandedGrid extends StatelessWidget {
  const ExpandedGrid({required this.items, super.key});

  final Map<String, String?> items;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      children: [
        for (final entry in items.entries)
          SizedBox(
            width: 220,
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
                  _dash(entry.value),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(
                    context,
                  ).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w700),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

class _PhotoAvatar extends StatelessWidget {
  const _PhotoAvatar({required this.student, required this.radius});

  final StudentProfileModel student;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final photoUrl = student.photoUrl;
    return CircleAvatar(
      radius: radius,
      backgroundColor: const Color(0xFFE0F2FE),
      foregroundImage: photoUrl == null || photoUrl.isEmpty
          ? null
          : NetworkImage(photoUrl),
      child: photoUrl == null || photoUrl.isEmpty
          ? Text(
              student.firstName.isEmpty ? '?' : student.firstName[0],
              style: TextStyle(
                fontSize: radius * 0.7,
                fontWeight: FontWeight.w800,
                color: const Color(0xFF0369A1),
              ),
            )
          : null,
    );
  }
}

class _InlineEmpty extends StatelessWidget {
  const _InlineEmpty({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 18),
      child: Text(message),
    );
  }
}

class _SmallBadge extends StatelessWidget {
  const _SmallBadge({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 28,
      padding: const EdgeInsets.symmetric(horizontal: 10),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: const Color(0xFFFEF3C7),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: const Color(0xFF92400E),
          fontWeight: FontWeight.w800,
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

Future<void> _showPersonalDialog(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student,
) async {
  final firstName = TextEditingController(text: student.firstName);
  final middleName = TextEditingController(text: student.middleName ?? '');
  final lastName = TextEditingController(text: student.lastName ?? '');
  final dateOfBirth = TextEditingController(
    text: _dateLabel(student.dateOfBirth),
  );
  final admissionDate = TextEditingController(
    text: _dateLabel(student.admissionDate),
  );
  final bloodGroup = TextEditingController(text: student.bloodGroup ?? '');
  final email = TextEditingController(text: student.email ?? '');
  final phone = TextEditingController(text: student.phoneNumber ?? '');
  final previousSchool = TextEditingController(
    text: student.previousSchool ?? '',
  );
  final addressLine1 = TextEditingController(text: student.addressLine1 ?? '');
  final addressLine2 = TextEditingController(text: student.addressLine2 ?? '');
  final city = TextEditingController(text: student.city ?? '');
  final state = TextEditingController(text: student.state ?? '');
  final postalCode = TextEditingController(text: student.postalCode ?? '');
  final country = TextEditingController(text: student.country ?? '');
  final formKey = GlobalKey<FormState>();
  var gender = student.gender;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return AlertDialog(
        title: const Text('Edit personal details'),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 720,
            child: SingleChildScrollView(
              child: Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _Field(controller: firstName, label: 'First name'),
                  _Field(
                    controller: middleName,
                    label: 'Middle name',
                    required: false,
                  ),
                  _Field(
                    controller: lastName,
                    label: 'Last name',
                    required: false,
                  ),
                  _Field(
                    controller: dateOfBirth,
                    label: 'Date of birth',
                    validator: _date,
                  ),
                  _Field(
                    controller: admissionDate,
                    label: 'Admission date',
                    validator: _date,
                  ),
                  SizedBox(
                    width: 220,
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
                        DropdownMenuItem(
                          value: 'UNSPECIFIED',
                          child: Text('Unspecified'),
                        ),
                      ],
                      onChanged: (value) => gender = value ?? gender,
                    ),
                  ),
                  _Field(
                    controller: bloodGroup,
                    label: 'Blood group',
                    required: false,
                  ),
                  _Field(
                    controller: phone,
                    label: 'Mobile',
                    validator: _optionalMobile,
                  ),
                  _Field(controller: email, label: 'Email', validator: _email),
                  _Field(
                    controller: previousSchool,
                    label: 'Previous school',
                    required: false,
                  ),
                  _Field(
                    controller: addressLine1,
                    label: 'Address line 1',
                    required: false,
                  ),
                  _Field(
                    controller: addressLine2,
                    label: 'Address line 2',
                    required: false,
                  ),
                  _Field(controller: city, label: 'City', required: false),
                  _Field(controller: state, label: 'State', required: false),
                  _Field(
                    controller: postalCode,
                    label: 'Pin code',
                    validator: _optionalPinCode,
                  ),
                  _Field(
                    controller: country,
                    label: 'Country',
                    required: false,
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
              final result = await ref
                  .read(studentsRepositoryProvider)
                  .updateProfile(student.id, {
                    'firstName': firstName.text.trim(),
                    'middleName': _blankToNull(middleName.text),
                    'lastName': _blankToNull(lastName.text),
                    'dateOfBirth': dateOfBirth.text.trim(),
                    'gender': gender,
                    'bloodGroup': _blankToNull(bloodGroup.text),
                    'email': _blankToNull(email.text),
                    'phoneNumber': _blankToNull(phone.text),
                    'admissionDate': admissionDate.text.trim(),
                    'previousSchool': _blankToNull(previousSchool.text),
                    'addressLine1': _blankToNull(addressLine1.text),
                    'addressLine2': _blankToNull(addressLine2.text),
                    'city': _blankToNull(city.text),
                    'state': _blankToNull(state.text),
                    'postalCode': _blankToNull(postalCode.text),
                    'country': _blankToNull(country.text),
                  });
              if (!dialogContext.mounted) {
                return;
              }
              result.when(
                success: (_) {
                  ref.invalidate(studentProfileProvider(student.id));
                  _snack(context, 'Student details updated.');
                  Navigator.of(dialogContext).pop();
                },
                failure: (failure) => _snack(dialogContext, failure.message),
              );
            },
            icon: const Icon(Icons.save_outlined),
            label: const Text('Save'),
          ),
        ],
      );
    },
  );

  for (final controller in [
    firstName,
    middleName,
    lastName,
    dateOfBirth,
    admissionDate,
    bloodGroup,
    email,
    phone,
    previousSchool,
    addressLine1,
    addressLine2,
    city,
    state,
    postalCode,
    country,
  ]) {
    controller.dispose();
  }
}

Future<void> _showPhotoDialog(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student,
) async {
  final url = TextEditingController(text: student.photoUrl ?? '');
  final storageKey = TextEditingController(text: student.photoStorageKey ?? '');
  final contentType = TextEditingController(
    text: student.photoContentType ?? 'image/jpeg',
  );
  final fileName = TextEditingController(text: student.photoFileName ?? '');
  final formKey = GlobalKey<FormState>();

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: const Text('Student photo'),
      content: Form(
        key: formKey,
        child: SizedBox(
          width: 520,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: url,
                decoration: const InputDecoration(labelText: 'Photo URL'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: storageKey,
                decoration: const InputDecoration(labelText: 'Storage key'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: fileName,
                decoration: const InputDecoration(labelText: 'File name'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: contentType,
                decoration: const InputDecoration(labelText: 'Content type'),
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
            if (_blankToNull(url.text) == null &&
                _blankToNull(storageKey.text) == null) {
              _snack(dialogContext, 'Photo URL or storage key is required.');
              return;
            }
            final result = await ref
                .read(studentsRepositoryProvider)
                .updatePhoto(student.id, {
                  'photoUrl': _blankToNull(url.text),
                  'photoStorageKey': _blankToNull(storageKey.text),
                  'photoFileName': _blankToNull(fileName.text),
                  'photoContentType': _blankToNull(contentType.text),
                });
            if (!dialogContext.mounted) {
              return;
            }
            result.when(
              success: (_) {
                ref.invalidate(studentProfileProvider(student.id));
                _snack(context, 'Photo updated.');
                Navigator.of(dialogContext).pop();
              },
              failure: (failure) => _snack(dialogContext, failure.message),
            );
          },
          icon: const Icon(Icons.save_outlined),
          label: const Text('Save'),
        ),
      ],
    ),
  );

  url.dispose();
  storageKey.dispose();
  contentType.dispose();
  fileName.dispose();
}

Future<void> _showParentDialog(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student, [
  StudentParentModel? parent,
]) async {
  final firstName = TextEditingController(text: parent?.firstName ?? '');
  final lastName = TextEditingController(text: parent?.lastName ?? '');
  final email = TextEditingController(text: parent?.email ?? '');
  final phone = TextEditingController(text: parent?.phoneNumber ?? '');
  final alternatePhone = TextEditingController(
    text: parent?.alternatePhoneNumber ?? '',
  );
  final occupation = TextEditingController(text: parent?.occupation ?? '');
  final addressLine1 = TextEditingController(text: parent?.addressLine1 ?? '');
  final addressLine2 = TextEditingController(text: parent?.addressLine2 ?? '');
  final city = TextEditingController(text: parent?.city ?? '');
  final state = TextEditingController(text: parent?.state ?? '');
  final postalCode = TextEditingController(text: parent?.postalCode ?? '');
  final country = TextEditingController(text: parent?.country ?? 'India');
  final formKey = GlobalKey<FormState>();
  var relation = parent?.relationType ?? 'GUARDIAN';
  var primary = parent?.primaryContact ?? student.parents.isEmpty;
  var emergency = parent?.emergencyContact ?? true;
  var pickup = parent?.pickupAllowed ?? true;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) {
      return StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          title: Text(
            parent == null ? 'Add parent / guardian' : 'Edit parent / guardian',
          ),
          content: Form(
            key: formKey,
            child: SizedBox(
              width: 720,
              child: SingleChildScrollView(
                child: Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    _Field(controller: firstName, label: 'First name'),
                    _Field(
                      controller: lastName,
                      label: 'Last name',
                      required: false,
                    ),
                    SizedBox(
                      width: 220,
                      child: DropdownButtonFormField<String>(
                        initialValue: relation,
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
                        onChanged: (value) => relation = value ?? relation,
                      ),
                    ),
                    _Field(
                      controller: phone,
                      label: 'Mobile',
                      validator: _mobile,
                    ),
                    _Field(
                      controller: alternatePhone,
                      label: 'Alternate mobile',
                      validator: _optionalMobile,
                    ),
                    _Field(
                      controller: email,
                      label: 'Email',
                      validator: _email,
                    ),
                    _Field(
                      controller: occupation,
                      label: 'Occupation',
                      required: false,
                    ),
                    _Field(
                      controller: addressLine1,
                      label: 'Address line 1',
                      required: false,
                    ),
                    _Field(
                      controller: addressLine2,
                      label: 'Address line 2',
                      required: false,
                    ),
                    _Field(controller: city, label: 'City', required: false),
                    _Field(controller: state, label: 'State', required: false),
                    _Field(
                      controller: postalCode,
                      label: 'Pin code',
                      validator: _optionalPinCode,
                    ),
                    _Field(
                      controller: country,
                      label: 'Country',
                      required: false,
                    ),
                    SizedBox(
                      width: 220,
                      child: CheckboxListTile(
                        value: primary,
                        onChanged: (value) =>
                            setState(() => primary = value ?? primary),
                        title: const Text('Primary contact'),
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                    SizedBox(
                      width: 220,
                      child: CheckboxListTile(
                        value: emergency,
                        onChanged: (value) =>
                            setState(() => emergency = value ?? emergency),
                        title: const Text('Emergency'),
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                    SizedBox(
                      width: 220,
                      child: CheckboxListTile(
                        value: pickup,
                        onChanged: (value) =>
                            setState(() => pickup = value ?? pickup),
                        title: const Text('Pickup allowed'),
                        contentPadding: EdgeInsets.zero,
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
                  'relationType': relation,
                  'primaryContact': primary,
                  'emergencyContact': emergency,
                  'pickupAllowed': pickup,
                  'parent': {
                    'firstName': firstName.text.trim(),
                    'lastName': _blankToNull(lastName.text),
                    'email': _blankToNull(email.text),
                    'phoneNumber': phone.text.trim(),
                    'alternatePhoneNumber': _blankToNull(alternatePhone.text),
                    'occupation': _blankToNull(occupation.text),
                    'addressLine1': _blankToNull(addressLine1.text),
                    'addressLine2': _blankToNull(addressLine2.text),
                    'city': _blankToNull(city.text),
                    'state': _blankToNull(state.text),
                    'postalCode': _blankToNull(postalCode.text),
                    'country': _blankToNull(country.text),
                    'userAccountId': null,
                  },
                };
                final repository = ref.read(studentsRepositoryProvider);
                final result = parent == null
                    ? await repository.addParent(student.id, payload)
                    : await repository.updateParent(
                        student.id,
                        parent.mappingId,
                        payload,
                      );
                if (!dialogContext.mounted) {
                  return;
                }
                result.when(
                  success: (_) {
                    _refreshParentState(ref, student.id);
                    _snack(context, 'Parent details saved.');
                    Navigator.of(dialogContext).pop();
                  },
                  failure: (failure) => _snack(dialogContext, failure.message),
                );
              },
              icon: const Icon(Icons.save_outlined),
              label: const Text('Save'),
            ),
          ],
        ),
      );
    },
  );

  for (final controller in [
    firstName,
    lastName,
    email,
    phone,
    alternatePhone,
    occupation,
    addressLine1,
    addressLine2,
    city,
    state,
    postalCode,
    country,
  ]) {
    controller.dispose();
  }
}

Future<void> _markParentPrimary(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student,
  StudentParentModel parent,
) async {
  final confirmed = await _confirm(
    context,
    'Mark ${parent.displayName} as the primary contact?',
  );
  if (!confirmed || !context.mounted) {
    return;
  }
  final result = await ref
      .read(studentsRepositoryProvider)
      .updateParent(
        student.id,
        parent.mappingId,
        _parentPayload(parent, primaryContact: true),
      );
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      _refreshParentState(ref, student.id);
      _snack(context, 'Primary contact updated.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _showClassDialog(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student,
) async {
  final assignment = student.currentAssignment;
  final academicYear = TextEditingController(
    text: assignment?.academicYear ?? '',
  );
  final className = TextEditingController(text: assignment?.className ?? '');
  final sectionName = TextEditingController(
    text: assignment?.sectionName ?? '',
  );
  final roll = TextEditingController(text: assignment?.rollNumber ?? '');
  final effectiveFrom = TextEditingController(text: _dateLabel(DateTime.now()));
  final formKey = GlobalKey<FormState>();

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: const Text('Assign class / section'),
      content: Form(
        key: formKey,
        child: SizedBox(
          width: 520,
          child: Wrap(
            spacing: 12,
            runSpacing: 12,
            children: [
              _Field(controller: academicYear, label: 'Academic year'),
              _Field(controller: className, label: 'Class'),
              _Field(controller: sectionName, label: 'Section / division'),
              _Field(controller: roll, label: 'Roll number', required: false),
              _Field(
                controller: effectiveFrom,
                label: 'Effective from',
                validator: _date,
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
            final result = await ref
                .read(studentsRepositoryProvider)
                .assignClass(student.id, {
                  'academicYearId': null,
                  'classId': null,
                  'sectionId': null,
                  'academicYear': academicYear.text.trim(),
                  'className': className.text.trim(),
                  'sectionName': sectionName.text.trim(),
                  'rollNumber': _blankToNull(roll.text),
                  'effectiveFrom': effectiveFrom.text.trim(),
                });
            if (!dialogContext.mounted) {
              return;
            }
            result.when(
              success: (_) {
                ref.invalidate(studentProfileProvider(student.id));
                _snack(context, 'Class assignment updated.');
                Navigator.of(dialogContext).pop();
              },
              failure: (failure) => _snack(dialogContext, failure.message),
            );
          },
          icon: const Icon(Icons.save_outlined),
          label: const Text('Save'),
        ),
      ],
    ),
  );

  academicYear.dispose();
  className.dispose();
  sectionName.dispose();
  roll.dispose();
  effectiveFrom.dispose();
}

Future<void> _showDocumentDialog(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student, [
  StudentDocumentModel? document,
]) async {
  const documentTypes = [
    'BIRTH_CERTIFICATE',
    'TRANSFER_CERTIFICATE',
    'MARKSHEET',
    'ADDRESS_PROOF',
    'PHOTO',
    'ID_PROOF',
    'MEDICAL_RECORD',
    'OTHER',
  ];
  final number = TextEditingController(text: document?.documentNumber ?? '');
  final fileName = TextEditingController(text: document?.fileName ?? '');
  final fileUrl = TextEditingController(text: document?.fileUrl ?? '');
  final storageKey = TextEditingController();
  final remarks = TextEditingController();
  final formKey = GlobalKey<FormState>();
  var type = document?.documentType ?? 'BIRTH_CERTIFICATE';

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: Text(document == null ? 'Upload document' : 'Edit document'),
      content: Form(
        key: formKey,
        child: SizedBox(
          width: 560,
          child: Wrap(
            spacing: 12,
            runSpacing: 12,
            children: [
              SizedBox(
                width: 260,
                child: DropdownButtonFormField<String>(
                  initialValue: type,
                  isExpanded: true,
                  decoration: const InputDecoration(labelText: 'Type'),
                  items: [
                    for (final item in documentTypes)
                      DropdownMenuItem(
                        value: item,
                        child: Text(item.replaceAll('_', ' ')),
                      ),
                  ],
                  onChanged: (value) => type = value ?? type,
                ),
              ),
              _Field(
                controller: number,
                label: 'Document number',
                required: false,
              ),
              _Field(controller: fileName, label: 'File name'),
              _Field(controller: fileUrl, label: 'File URL', required: false),
              _Field(
                controller: storageKey,
                label: 'Storage key',
                required: false,
              ),
              _Field(controller: remarks, label: 'Remarks', required: false),
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
            if (_blankToNull(fileUrl.text) == null &&
                _blankToNull(storageKey.text) == null) {
              _snack(dialogContext, 'File URL or storage key is required.');
              return;
            }
            final payload = {
              'documentType': type,
              'documentNumber': _blankToNull(number.text),
              'fileName': fileName.text.trim(),
              'contentType': null,
              'fileSize': null,
              'storageKey': _blankToNull(storageKey.text),
              'fileUrl': _blankToNull(fileUrl.text),
              'remarks': _blankToNull(remarks.text),
            };
            final repository = ref.read(studentsRepositoryProvider);
            final result = document == null
                ? await repository.addDocument(student.id, payload)
                : await repository.updateDocument(
                    student.id,
                    document.id,
                    payload,
                  );
            if (!dialogContext.mounted) {
              return;
            }
            result.when(
              success: (_) {
                ref.invalidate(studentProfileProvider(student.id));
                _snack(context, 'Document saved.');
                Navigator.of(dialogContext).pop();
              },
              failure: (failure) => _snack(dialogContext, failure.message),
            );
          },
          icon: const Icon(Icons.save_outlined),
          label: const Text('Save'),
        ),
      ],
    ),
  );

  number.dispose();
  fileName.dispose();
  fileUrl.dispose();
  storageKey.dispose();
  remarks.dispose();
}

Future<void> _toggleStatus(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student,
) async {
  final active = student.status == 'ACTIVE';
  final confirmed = await _confirm(
    context,
    active ? 'Deactivate this student?' : 'Activate this student?',
  );
  if (!confirmed || !context.mounted) {
    return;
  }
  final repository = ref.read(studentsRepositoryProvider);
  final result = active
      ? await repository.deactivate(student.id)
      : await repository.activate(student.id);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      ref.invalidate(studentProfileProvider(student.id));
      _snack(context, active ? 'Student deactivated.' : 'Student activated.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _deleteParent(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student,
  StudentParentModel parent,
) async {
  final confirmed = await _confirm(context, 'Remove ${parent.displayName}?');
  if (!confirmed || !context.mounted) {
    return;
  }
  final result = await ref
      .read(studentsRepositoryProvider)
      .deleteParent(student.id, parent.mappingId);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      _refreshParentState(ref, student.id);
      _snack(context, 'Parent removed.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _deleteDocument(
  BuildContext context,
  WidgetRef ref,
  StudentProfileModel student,
  StudentDocumentModel document,
) async {
  final confirmed = await _confirm(context, 'Delete ${document.fileName}?');
  if (!confirmed || !context.mounted) {
    return;
  }
  final result = await ref
      .read(studentsRepositoryProvider)
      .deleteDocument(student.id, document.id);
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      ref.invalidate(studentProfileProvider(student.id));
      _snack(context, 'Document deleted.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

class _Field extends StatelessWidget {
  const _Field({
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
    return SizedBox(
      width: 220,
      child: TextFormField(
        controller: controller,
        decoration: InputDecoration(labelText: label),
        validator: validator ?? (required ? _required : null),
      ),
    );
  }
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

String _address(StudentProfileModel student) {
  return [
    student.addressLine1,
    student.addressLine2,
    student.city,
    student.state,
    student.postalCode,
    student.country,
  ].whereType<String>().where((value) => value.trim().isNotEmpty).join(', ');
}

String _parentAddress(StudentParentModel parent) {
  return [
    parent.addressLine1,
    parent.addressLine2,
    parent.city,
    parent.state,
    parent.postalCode,
    parent.country,
  ].whereType<String>().where((value) => value.trim().isNotEmpty).join(', ');
}

Map<String, dynamic> _parentPayload(
  StudentParentModel parent, {
  bool? primaryContact,
}) {
  return {
    'relationType': parent.relationType,
    'primaryContact': primaryContact ?? parent.primaryContact,
    'emergencyContact': parent.emergencyContact,
    'pickupAllowed': parent.pickupAllowed,
    'parent': {
      'firstName': parent.firstName,
      'lastName': parent.lastName,
      'email': parent.email,
      'phoneNumber': parent.phoneNumber,
      'alternatePhoneNumber': parent.alternatePhoneNumber,
      'occupation': parent.occupation,
      'addressLine1': parent.addressLine1,
      'addressLine2': parent.addressLine2,
      'city': parent.city,
      'state': parent.state,
      'postalCode': parent.postalCode,
      'country': parent.country,
      'userAccountId': null,
    },
  };
}

void _refreshParentState(WidgetRef ref, String studentId) {
  ref.invalidate(studentProfileProvider(studentId));
  ref.invalidate(studentParentsProvider(studentId));
}

String _dateLabel(DateTime value) {
  return value.toIso8601String().split('T').first;
}

String _nullableDateLabel(DateTime? value) {
  return value == null ? '-' : _dateLabel(value);
}

String _feeScopeLabel(StudentFeeAssignmentModel fee) {
  if (fee.sourceType == 'HOSTEL') {
    return [
      fee.hostelName,
      if ((fee.hostelRoomNumber ?? '').isNotEmpty)
        'Room ${fee.hostelRoomNumber}'
      else
        fee.roomType,
    ].whereType<String>().where((value) => value.trim().isNotEmpty).join(' - ');
  }
  if (fee.sourceType == 'TRANSPORT') {
    return [
      fee.transportRouteName,
      fee.transportPickupPointName,
    ].whereType<String>().where((value) => value.trim().isNotEmpty).join(' - ');
  }
  final section = fee.sectionName;
  return section == null || section.isEmpty
      ? fee.className
      : '${fee.className} $section';
}

String _money(double value) {
  return 'INR ${value.toStringAsFixed(2)}';
}

HostelRoomSummaryModel? _profileHostelRoomById(
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

void _refreshStudentTransportState(
  WidgetRef ref,
  String studentId,
  String academicYearId,
) {
  ref.invalidate(studentProfileProvider(studentId));
  ref.invalidate(
    studentCurrentTransportAssignmentProvider(
      StudentTransportAssignmentKey(
        studentId: studentId,
        academicYearId: academicYearId,
      ),
    ),
  );
  ref.invalidate(transportVehiclesProvider(academicYearId));
  ref.invalidate(transportRoutesProvider(academicYearId));
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

String? _blankToNull(String value) {
  return value.trim().isEmpty ? null : value.trim();
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
