import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/student_models.dart';
import '../controllers/students_providers.dart';

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
      length: 8,
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
                        Expanded(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                student.displayName,
                                overflow: TextOverflow.ellipsis,
                                style: Theme.of(context).textTheme.titleMedium
                                    ?.copyWith(fontWeight: FontWeight.w800),
                              ),
                              Text(
                                student.admissionNumber,
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
                _ParentsTab(parents: student.parents),
                _AcademicTab(assignments: student.classAssignments),
                const _EmptyTab(label: 'No attendance records.'),
                const _EmptyTab(label: 'No fee records.'),
                _DocumentsTab(documents: student.documents),
                const _EmptyTab(label: 'No hostel allocation.'),
                const _EmptyTab(label: 'No transport allocation.'),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PersonalTab extends StatelessWidget {
  const _PersonalTab({required this.student});

  final StudentProfileModel student;

  @override
  Widget build(BuildContext context) {
    return _TabSurface(
      child: _DetailGrid(
        items: {
          'First name': student.firstName,
          'Last name': student.lastName ?? '-',
          'Date of birth': _dateLabel(student.dateOfBirth),
          'Gender': student.gender,
          'Blood group': student.bloodGroup ?? '-',
          'Mobile': student.phoneNumber ?? '-',
          'Email': student.email ?? '-',
          'City': student.city ?? '-',
          'State': student.state ?? '-',
          'Country': student.country ?? '-',
        },
      ),
    );
  }
}

class _ParentsTab extends StatelessWidget {
  const _ParentsTab({required this.parents});

  final List<StudentParentModel> parents;

  @override
  Widget build(BuildContext context) {
    if (parents.isEmpty) {
      return const _EmptyTab(label: 'No parents linked.');
    }
    return _TabSurface(
      child: Column(
        children: [
          for (final parent in parents)
            ListTile(
              leading: const Icon(Icons.family_restroom_outlined),
              title: Text(parent.displayName),
              subtitle: Text(
                '${parent.relationType} - ${parent.phoneNumber}'
                '${parent.email == null ? '' : ' - ${parent.email}'}',
              ),
              trailing: parent.primaryContact
                  ? const _SmallBadge(label: 'Primary')
                  : null,
            ),
        ],
      ),
    );
  }
}

class _AcademicTab extends StatelessWidget {
  const _AcademicTab({required this.assignments});

  final List<ClassAssignmentModel> assignments;

  @override
  Widget build(BuildContext context) {
    if (assignments.isEmpty) {
      return const _EmptyTab(label: 'No academic assignments.');
    }
    return _TabSurface(
      child: Column(
        children: [
          for (final assignment in assignments)
            ListTile(
              leading: Icon(
                assignment.active
                    ? Icons.check_circle_outline
                    : Icons.history_outlined,
              ),
              title: Text('${assignment.className} ${assignment.sectionName}'),
              subtitle: Text(
                '${assignment.academicYear} - Roll ${assignment.rollNumber ?? '-'}',
              ),
            ),
        ],
      ),
    );
  }
}

class _DocumentsTab extends StatelessWidget {
  const _DocumentsTab({required this.documents});

  final List<StudentDocumentModel> documents;

  @override
  Widget build(BuildContext context) {
    if (documents.isEmpty) {
      return const _EmptyTab(label: 'No documents uploaded.');
    }
    return _TabSurface(
      child: Column(
        children: [
          for (final document in documents)
            ListTile(
              leading: const Icon(Icons.description_outlined),
              title: Text(document.fileName),
              subtitle: Text(
                '${document.documentType} - ${document.verificationStatus}',
              ),
            ),
        ],
      ),
    );
  }
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
                  entry.value,
                  maxLines: 1,
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

class _EmptyTab extends StatelessWidget {
  const _EmptyTab({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Center(child: Text(label));
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

String _dateLabel(DateTime value) {
  return value.toIso8601String().split('T').first;
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
