import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_info_card.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/student_models.dart';
import '../controllers/students_providers.dart';

class StudentSectionsPage extends ConsumerWidget {
  const StudentSectionsPage({
    required this.classId,
    this.academicYearId,
    super.key,
  });

  final String classId;
  final String? academicYearId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final sections = ref.watch(sectionsByClassProvider(classId));

    return AdminShell(
      title: 'Class Divisions',
      activeModuleId: 'students',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: Column(
        children: [
          _Header(
            title: 'Divisions / Sections',
            onBack: () => context.go(AppRoutes.students),
          ),
          const Divider(height: 1),
          Expanded(
            child: sections.when(
              data: (items) => _SectionGrid(
                classId: classId,
                academicYearId: academicYearId,
                sections: items,
              ),
              error: (error, _) => AppErrorState(
                message: _message(error),
                onRetry: () => ref.invalidate(sectionsByClassProvider(classId)),
              ),
              loading: () => const AppLoadingState(label: 'Loading divisions'),
            ),
          ),
        ],
      ),
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.title, required this.onBack});

  final String title;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: SizedBox(
        height: 64,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            children: [
              IconButton(
                tooltip: 'Back',
                onPressed: onBack,
                icon: const Icon(Icons.arrow_back),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  title,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SectionGrid extends StatelessWidget {
  const _SectionGrid({
    required this.classId,
    required this.sections,
    this.academicYearId,
  });

  final String classId;
  final String? academicYearId;
  final List<SectionModel> sections;

  @override
  Widget build(BuildContext context) {
    if (sections.isEmpty) {
      return const Center(child: Text('No divisions found.'));
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        final columns = width >= 1000
            ? 3
            : width >= 620
            ? 2
            : 1;
        return GridView.builder(
          padding: const EdgeInsets.all(24),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: columns,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            mainAxisExtent: 116,
          ),
          itemCount: sections.length,
          itemBuilder: (context, index) {
            final item = sections[index];
            return AppInfoCard(
              title: item.name,
              subtitle: item.capacity == null
                  ? 'Section ${item.code}'
                  : 'Section ${item.code} - Capacity ${item.capacity}',
              icon: Icons.groups_2_outlined,
              onTap: () => context.go(
                AppRoutes.studentSectionDetail(
                  classId,
                  item.id,
                  academicYearId: academicYearId,
                ),
              ),
            );
          },
        );
      },
    );
  }
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
