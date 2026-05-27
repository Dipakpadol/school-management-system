import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_info_card.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../../core/widgets/app_select_field.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/student_models.dart';
import '../controllers/students_providers.dart';

class StudentsPage extends ConsumerWidget {
  const StudentsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final academicYears = ref.watch(academicYearsProvider);

    return AdminShell(
      title: 'Student Management',
      activeModuleId: 'students',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: academicYears.when(
        data: (years) => _StudentManagementHome(years: years),
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(academicYearsProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading academic years'),
      ),
    );
  }
}

class _StudentManagementHome extends ConsumerWidget {
  const _StudentManagementHome({required this.years});

  final List<AcademicYearModel> years;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedId = ref.watch(selectedAcademicYearIdProvider);
    final effectiveYearId =
        selectedId ?? (years.isEmpty ? null : years.first.id);

    if (selectedId == null && effectiveYearId != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ref.read(selectedAcademicYearIdProvider.notifier).set(effectiveYearId);
      });
    }

    final classes = effectiveYearId == null
        ? null
        : ref.watch(classesByAcademicYearProvider(effectiveYearId));

    return Column(
      children: [
        Material(
          color: Colors.white,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 18, 24, 18),
            child: Align(
              alignment: Alignment.centerLeft,
              child: SizedBox(
                width: 320,
                child: AppSelectField<String>(
                  label: 'Academic Year',
                  icon: Icons.calendar_month_outlined,
                  value: effectiveYearId,
                  items: [
                    for (final year in years)
                      DropdownMenuItem(value: year.id, child: Text(year.name)),
                  ],
                  onChanged: (value) {
                    ref
                        .read(selectedAcademicYearIdProvider.notifier)
                        .set(value);
                    if (value != null) {
                      context.go(AppRoutes.studentClasses);
                    }
                  },
                ),
              ),
            ),
          ),
        ),
        const Divider(height: 1),
        Expanded(
          child: classes == null
              ? const Center(child: Text('No academic years found.'))
              : classes.when(
                  data: (items) => _ClassGrid(
                    academicYearId: effectiveYearId!,
                    classes: items,
                  ),
                  error: (error, _) => AppErrorState(
                    message: _message(error),
                    onRetry: () => ref.invalidate(
                      classesByAcademicYearProvider(effectiveYearId!),
                    ),
                  ),
                  loading: () =>
                      const AppLoadingState(label: 'Loading classes'),
                ),
        ),
      ],
    );
  }
}

class _ClassGrid extends StatelessWidget {
  const _ClassGrid({required this.academicYearId, required this.classes});

  final String academicYearId;
  final List<SchoolClassModel> classes;

  @override
  Widget build(BuildContext context) {
    if (classes.isEmpty) {
      return const Center(child: Text('No classes found.'));
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        final columns = width >= 1180
            ? 4
            : width >= 860
            ? 3
            : width >= 560
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
          itemCount: classes.length,
          itemBuilder: (context, index) {
            final item = classes[index];
            return AppInfoCard(
              title: item.name,
              subtitle: item.active ? 'Active class' : 'Inactive class',
              icon: Icons.school_outlined,
              onTap: () => context.go(
                AppRoutes.studentClassSections(
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
