import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/academic_models.dart';
import '../../data/repositories/academic_repository_impl.dart';
import '../controllers/academic_providers.dart';

class AcademicManagementPage extends ConsumerStatefulWidget {
  const AcademicManagementPage({super.key});

  @override
  ConsumerState<AcademicManagementPage> createState() {
    return _AcademicManagementPageState();
  }
}

class _AcademicManagementPageState
    extends ConsumerState<AcademicManagementPage> {
  String? _selectedYearId;
  String? _selectedClassId;
  String _yearQuery = '';
  String _classQuery = '';
  String _divisionQuery = '';

  @override
  Widget build(BuildContext context) {
    final years = ref.watch(academicYearsProvider);

    return AdminShell(
      title: 'Academic Management',
      activeModuleId: 'academic',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: years.when(
        data: _buildForYears,
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(academicYearsProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading academic years'),
      ),
    );
  }

  Widget _buildForYears(List<AcademicYearModel> years) {
    final effectiveYearId = _validId(
      _selectedYearId,
      years.map((item) => item.id),
    );
    if (_selectedYearId != effectiveYearId) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          setState(() {
            _selectedYearId = effectiveYearId;
            _selectedClassId = null;
          });
        }
      });
    }

    final classes = effectiveYearId == null
        ? null
        : ref.watch(academicClassesProvider(effectiveYearId));

    return Column(
      children: [
        _TopBar(onAddYear: () => _showYearDialog()),
        const Divider(height: 1),
        Expanded(
          child: LayoutBuilder(
            builder: (context, constraints) {
              final compact = constraints.maxWidth < 980;
              final yearPanel = _YearPanel(
                years: _filterYears(years),
                selectedYearId: effectiveYearId,
                onQueryChanged: (value) => setState(() => _yearQuery = value),
                onSelected: (year) {
                  setState(() {
                    _selectedYearId = year.id;
                    _selectedClassId = null;
                  });
                },
                onAdd: () => _showYearDialog(),
                onEdit: _showYearDialog,
                onDelete: _deleteYear,
              );

              final classPanel = classes == null
                  ? const _PlaceholderPanel(
                      icon: Icons.school_outlined,
                      title: 'Classes',
                      message: 'No academic year selected.',
                    )
                  : classes.when(
                      data: (items) {
                        final effectiveClassId = _validId(
                          _selectedClassId,
                          items.map((item) => item.id),
                        );
                        if (_selectedClassId != effectiveClassId) {
                          WidgetsBinding.instance.addPostFrameCallback((_) {
                            if (mounted) {
                              setState(
                                () => _selectedClassId = effectiveClassId,
                              );
                            }
                          });
                        }
                        return _ClassPanel(
                          classes: _filterClasses(items),
                          selectedClassId: effectiveClassId,
                          onQueryChanged: (value) =>
                              setState(() => _classQuery = value),
                          onSelected: (schoolClass) =>
                              setState(() => _selectedClassId = schoolClass.id),
                          onAdd: () => _showClassDialog(effectiveYearId!),
                          onEdit: (schoolClass) =>
                              _showClassDialog(effectiveYearId!, schoolClass),
                          onDelete: _deleteClass,
                        );
                      },
                      error: (error, _) => _RetryPanel(
                        message: _message(error),
                        onRetry: () => ref.invalidate(
                          academicClassesProvider(effectiveYearId!),
                        ),
                      ),
                      loading: () =>
                          const _PanelLoading(label: 'Loading classes'),
                    );

              final divisionPanel = _selectedClassId == null
                  ? const _PlaceholderPanel(
                      icon: Icons.groups_2_outlined,
                      title: 'Divisions',
                      message: 'No class selected.',
                    )
                  : _DivisionPanelHost(
                      classId: _selectedClassId!,
                      query: _divisionQuery,
                      onQueryChanged: (value) =>
                          setState(() => _divisionQuery = value),
                      onAddDivision: () =>
                          _showDivisionDialog(_selectedClassId!),
                      onEditDivision: (division) =>
                          _showDivisionDialog(_selectedClassId!, division),
                      onDeleteDivision: _deleteDivision,
                      onAddSubject: _showSubjectDialog,
                      onEditSubject: _showSubjectDialog,
                      onDeleteSubject: _deleteSubject,
                    );

              if (compact) {
                return ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    SizedBox(height: 430, child: yearPanel),
                    const SizedBox(height: 12),
                    SizedBox(height: 430, child: classPanel),
                    const SizedBox(height: 12),
                    SizedBox(height: 720, child: divisionPanel),
                  ],
                );
              }
              return Row(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  SizedBox(width: 300, child: yearPanel),
                  const VerticalDivider(width: 1),
                  SizedBox(width: 320, child: classPanel),
                  const VerticalDivider(width: 1),
                  Expanded(child: divisionPanel),
                ],
              );
            },
          ),
        ),
      ],
    );
  }

  List<AcademicYearModel> _filterYears(List<AcademicYearModel> years) {
    final query = _yearQuery.trim().toLowerCase();
    if (query.isEmpty) {
      return years;
    }
    return years
        .where(
          (year) =>
              year.name.toLowerCase().contains(query) ||
              year.code.toLowerCase().contains(query),
        )
        .toList(growable: false);
  }

  List<AcademicClassModel> _filterClasses(List<AcademicClassModel> classes) {
    final query = _classQuery.trim().toLowerCase();
    if (query.isEmpty) {
      return classes;
    }
    return classes
        .where(
          (schoolClass) =>
              schoolClass.name.toLowerCase().contains(query) ||
              schoolClass.code.toLowerCase().contains(query),
        )
        .toList(growable: false);
  }

  Future<void> _showYearDialog([AcademicYearModel? year]) async {
    final result = await showDialog<_YearFormValue>(
      context: context,
      builder: (context) => _YearDialog(year: year),
    );
    if (result == null || !mounted) {
      return;
    }
    final repository = ref.read(academicRepositoryProvider);
    final saveResult = year == null
        ? await repository.createYear(result.toPayload())
        : await repository.updateYear(year.id, result.toPayload());
    if (!mounted) {
      return;
    }
    saveResult.when(
      success: (saved) {
        ref.invalidate(academicYearsProvider);
        setState(() => _selectedYearId = saved.id);
        _snack(
          year == null ? 'Academic year created.' : 'Academic year saved.',
        );
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _deleteYear(AcademicYearModel year) async {
    final confirmed = await _confirm('Delete ${year.name}?');
    if (!confirmed || !mounted) {
      return;
    }
    final result = await ref
        .read(academicRepositoryProvider)
        .deleteYear(year.id);
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(academicYearsProvider);
        setState(() {
          if (_selectedYearId == year.id) {
            _selectedYearId = null;
            _selectedClassId = null;
          }
        });
        _snack('Academic year deleted.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _showClassDialog(
    String academicYearId, [
    AcademicClassModel? schoolClass,
  ]) async {
    final result = await showDialog<_ClassFormValue>(
      context: context,
      builder: (context) => _ClassDialog(schoolClass: schoolClass),
    );
    if (result == null || !mounted) {
      return;
    }
    final repository = ref.read(academicRepositoryProvider);
    final saveResult = schoolClass == null
        ? await repository.createClass(academicYearId, result.toPayload())
        : await repository.updateClass(schoolClass.id, result.toPayload());
    if (!mounted) {
      return;
    }
    saveResult.when(
      success: (saved) {
        ref.invalidate(academicClassesProvider(academicYearId));
        setState(() => _selectedClassId = saved.id);
        _snack(schoolClass == null ? 'Class created.' : 'Class saved.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _deleteClass(AcademicClassModel schoolClass) async {
    final confirmed = await _confirm('Delete ${schoolClass.name}?');
    if (!confirmed || !mounted) {
      return;
    }
    final result = await ref
        .read(academicRepositoryProvider)
        .deleteClass(schoolClass.id);
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(academicClassesProvider(schoolClass.academicYearId));
        setState(() {
          if (_selectedClassId == schoolClass.id) {
            _selectedClassId = null;
          }
        });
        _snack('Class deleted.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _showDivisionDialog(
    String classId, [
    AcademicDivisionModel? division,
  ]) async {
    final teachers = await ref.read(academicRepositoryProvider).teachers();
    if (!mounted) {
      return;
    }
    await teachers.when(
      success: (items) async {
        final result = await showDialog<_DivisionFormValue>(
          context: context,
          builder: (context) =>
              _DivisionDialog(division: division, teachers: items),
        );
        if (result == null || !mounted) {
          return;
        }
        final repository = ref.read(academicRepositoryProvider);
        final saveResult = division == null
            ? await repository.createDivision(classId, result.toPayload())
            : await repository.updateDivision(division.id, result.toPayload());
        if (!mounted) {
          return;
        }
        saveResult.when(
          success: (_) {
            ref.invalidate(academicDivisionsProvider(classId));
            _snack(division == null ? 'Division created.' : 'Division saved.');
          },
          failure: (failure) => _snack(failure.message),
        );
      },
      failure: (failure) async => _snack(failure.message),
    );
  }

  Future<void> _deleteDivision(AcademicDivisionModel division) async {
    final confirmed = await _confirm('Delete ${division.name}?');
    if (!confirmed || !mounted) {
      return;
    }
    final result = await ref
        .read(academicRepositoryProvider)
        .deleteDivision(division.id);
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(academicDivisionsProvider(division.classId));
        _snack('Division deleted.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _showSubjectDialog(
    AcademicDivisionModel division, [
    DivisionSubjectModel? subject,
  ]) async {
    final teachers = await ref.read(academicRepositoryProvider).teachers();
    if (!mounted) {
      return;
    }
    await teachers.when(
      success: (items) async {
        final result = await showDialog<_SubjectFormValue>(
          context: context,
          builder: (context) =>
              _SubjectDialog(subject: subject, teachers: items),
        );
        if (result == null || !mounted) {
          return;
        }
        final repository = ref.read(academicRepositoryProvider);
        final saveResult = subject == null
            ? await repository.addDivisionSubject(
                division.id,
                result.toCreatePayload(),
              )
            : await repository.updateDivisionSubject(
                division.id,
                subject.id,
                result.toUpdatePayload(),
              );
        if (!mounted) {
          return;
        }
        saveResult.when(
          success: (_) {
            ref.invalidate(academicDivisionsProvider(division.classId));
            _snack(subject == null ? 'Subject assigned.' : 'Subject saved.');
          },
          failure: (failure) => _snack(failure.message),
        );
      },
      failure: (failure) async => _snack(failure.message),
    );
  }

  Future<void> _deleteSubject(
    AcademicDivisionModel division,
    DivisionSubjectModel subject,
  ) async {
    final confirmed = await _confirm('Remove ${subject.subjectName}?');
    if (!confirmed || !mounted) {
      return;
    }
    final result = await ref
        .read(academicRepositoryProvider)
        .deleteDivisionSubject(division.id, subject.id);
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(academicDivisionsProvider(division.classId));
        _snack('Subject removed.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<bool> _confirm(String message) async {
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

  void _snack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}

class _DivisionPanelHost extends ConsumerWidget {
  const _DivisionPanelHost({
    required this.classId,
    required this.query,
    required this.onQueryChanged,
    required this.onAddDivision,
    required this.onEditDivision,
    required this.onDeleteDivision,
    required this.onAddSubject,
    required this.onEditSubject,
    required this.onDeleteSubject,
  });

  final String classId;
  final String query;
  final ValueChanged<String> onQueryChanged;
  final VoidCallback onAddDivision;
  final ValueChanged<AcademicDivisionModel> onEditDivision;
  final ValueChanged<AcademicDivisionModel> onDeleteDivision;
  final ValueChanged<AcademicDivisionModel> onAddSubject;
  final void Function(
    AcademicDivisionModel division,
    DivisionSubjectModel subject,
  )
  onEditSubject;
  final void Function(
    AcademicDivisionModel division,
    DivisionSubjectModel subject,
  )
  onDeleteSubject;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final divisions = ref.watch(academicDivisionsProvider(classId));
    return divisions.when(
      data: (items) {
        final filtered = _filterDivisions(items, query);
        return _DivisionPanel(
          divisions: filtered,
          onQueryChanged: onQueryChanged,
          onAdd: onAddDivision,
          onEdit: onEditDivision,
          onDelete: onDeleteDivision,
          onAddSubject: onAddSubject,
          onEditSubject: onEditSubject,
          onDeleteSubject: onDeleteSubject,
        );
      },
      error: (error, _) => _RetryPanel(
        message: _message(error),
        onRetry: () => ref.invalidate(academicDivisionsProvider(classId)),
      ),
      loading: () => const _PanelLoading(label: 'Loading divisions'),
    );
  }

  List<AcademicDivisionModel> _filterDivisions(
    List<AcademicDivisionModel> divisions,
    String query,
  ) {
    final text = query.trim().toLowerCase();
    if (text.isEmpty) {
      return divisions;
    }
    return divisions
        .where(
          (division) =>
              division.name.toLowerCase().contains(text) ||
              division.code.toLowerCase().contains(text),
        )
        .toList(growable: false);
  }
}

class _TopBar extends StatelessWidget {
  const _TopBar({required this.onAddYear});

  final VoidCallback onAddYear;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 14, 24, 14),
        child: Row(
          children: [
            Expanded(
              child: Text(
                'Academic Year -> Classes -> Divisions',
                overflow: TextOverflow.ellipsis,
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
              ),
            ),
            AppButton(
              label: 'Add Academic Year',
              icon: Icons.add,
              onPressed: onAddYear,
            ),
          ],
        ),
      ),
    );
  }
}

class _YearPanel extends StatelessWidget {
  const _YearPanel({
    required this.years,
    required this.selectedYearId,
    required this.onQueryChanged,
    required this.onSelected,
    required this.onAdd,
    required this.onEdit,
    required this.onDelete,
  });

  final List<AcademicYearModel> years;
  final String? selectedYearId;
  final ValueChanged<String> onQueryChanged;
  final ValueChanged<AcademicYearModel> onSelected;
  final VoidCallback onAdd;
  final ValueChanged<AcademicYearModel> onEdit;
  final ValueChanged<AcademicYearModel> onDelete;

  @override
  Widget build(BuildContext context) {
    return _HierarchyPanel(
      title: 'Academic Years',
      searchLabel: 'Search years',
      onQueryChanged: onQueryChanged,
      onAdd: onAdd,
      children: years.isEmpty
          ? const [_EmptyTile(message: 'No academic years found.')]
          : [
              for (final year in years)
                _SelectableTile(
                  selected: year.id == selectedYearId,
                  title: year.name,
                  subtitle:
                      '${dateLabel(year.startDate)} to ${dateLabel(year.endDate)}',
                  leading: Icons.calendar_month_outlined,
                  status: year.current
                      ? 'CURRENT'
                      : (year.active ? 'ACTIVE' : 'INACTIVE'),
                  onTap: () => onSelected(year),
                  onEdit: () => onEdit(year),
                  onDelete: () => onDelete(year),
                ),
            ],
    );
  }
}

class _ClassPanel extends StatelessWidget {
  const _ClassPanel({
    required this.classes,
    required this.selectedClassId,
    required this.onQueryChanged,
    required this.onSelected,
    required this.onAdd,
    required this.onEdit,
    required this.onDelete,
  });

  final List<AcademicClassModel> classes;
  final String? selectedClassId;
  final ValueChanged<String> onQueryChanged;
  final ValueChanged<AcademicClassModel> onSelected;
  final VoidCallback onAdd;
  final ValueChanged<AcademicClassModel> onEdit;
  final ValueChanged<AcademicClassModel> onDelete;

  @override
  Widget build(BuildContext context) {
    return _HierarchyPanel(
      title: 'Classes',
      searchLabel: 'Search classes',
      onQueryChanged: onQueryChanged,
      onAdd: onAdd,
      children: classes.isEmpty
          ? const [_EmptyTile(message: 'No classes found.')]
          : [
              for (final schoolClass in classes)
                _SelectableTile(
                  selected: schoolClass.id == selectedClassId,
                  title: schoolClass.name,
                  subtitle: 'Code ${schoolClass.code}',
                  leading: Icons.school_outlined,
                  status: schoolClass.active ? 'ACTIVE' : 'INACTIVE',
                  onTap: () => onSelected(schoolClass),
                  onEdit: () => onEdit(schoolClass),
                  onDelete: () => onDelete(schoolClass),
                ),
            ],
    );
  }
}

class _DivisionPanel extends StatelessWidget {
  const _DivisionPanel({
    required this.divisions,
    required this.onQueryChanged,
    required this.onAdd,
    required this.onEdit,
    required this.onDelete,
    required this.onAddSubject,
    required this.onEditSubject,
    required this.onDeleteSubject,
  });

  final List<AcademicDivisionModel> divisions;
  final ValueChanged<String> onQueryChanged;
  final VoidCallback onAdd;
  final ValueChanged<AcademicDivisionModel> onEdit;
  final ValueChanged<AcademicDivisionModel> onDelete;
  final ValueChanged<AcademicDivisionModel> onAddSubject;
  final void Function(
    AcademicDivisionModel division,
    DivisionSubjectModel subject,
  )
  onEditSubject;
  final void Function(
    AcademicDivisionModel division,
    DivisionSubjectModel subject,
  )
  onDeleteSubject;

  @override
  Widget build(BuildContext context) {
    return _HierarchyPanel(
      title: 'Divisions',
      searchLabel: 'Search divisions',
      onQueryChanged: onQueryChanged,
      onAdd: onAdd,
      children: divisions.isEmpty
          ? const [_EmptyTile(message: 'No divisions found.')]
          : [
              for (final division in divisions)
                _DivisionTile(
                  division: division,
                  onEdit: () => onEdit(division),
                  onDelete: () => onDelete(division),
                  onAddSubject: () => onAddSubject(division),
                  onEditSubject: (subject) => onEditSubject(division, subject),
                  onDeleteSubject: (subject) =>
                      onDeleteSubject(division, subject),
                ),
            ],
    );
  }
}

class _HierarchyPanel extends StatelessWidget {
  const _HierarchyPanel({
    required this.title,
    required this.searchLabel,
    required this.onQueryChanged,
    required this.onAdd,
    required this.children,
  });

  final String title;
  final String searchLabel;
  final ValueChanged<String> onQueryChanged;
  final VoidCallback onAdd;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    title,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                IconButton(
                  tooltip: 'Add',
                  onPressed: onAdd,
                  icon: const Icon(Icons.add_circle_outline),
                ),
              ],
            ),
            const SizedBox(height: 10),
            TextField(
              onChanged: onQueryChanged,
              decoration: InputDecoration(
                labelText: searchLabel,
                prefixIcon: const Icon(Icons.search),
              ),
            ),
            const SizedBox(height: 12),
            Expanded(
              child: ListView.separated(
                itemCount: children.length,
                separatorBuilder: (context, index) => const SizedBox(height: 8),
                itemBuilder: (context, index) => children[index],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SelectableTile extends StatelessWidget {
  const _SelectableTile({
    required this.selected,
    required this.title,
    required this.subtitle,
    required this.leading,
    required this.status,
    required this.onTap,
    required this.onEdit,
    required this.onDelete,
  });

  final bool selected;
  final String title;
  final String subtitle;
  final IconData leading;
  final String status;
  final VoidCallback onTap;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return DecoratedBox(
      decoration: BoxDecoration(
        color: selected ? theme.colorScheme.primaryContainer : Colors.white,
        border: Border.all(
          color: selected ? theme.colorScheme.primary : const Color(0xFFE2E8F0),
        ),
        borderRadius: BorderRadius.circular(8),
      ),
      child: ListTile(
        selected: selected,
        onTap: onTap,
        leading: Icon(leading),
        title: Text(
          title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontWeight: FontWeight.w700),
        ),
        subtitle: Text(
          '$subtitle\n$status',
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: _TileMenu(onEdit: onEdit, onDelete: onDelete),
      ),
    );
  }
}

class _DivisionTile extends StatelessWidget {
  const _DivisionTile({
    required this.division,
    required this.onEdit,
    required this.onDelete,
    required this.onAddSubject,
    required this.onEditSubject,
    required this.onDeleteSubject,
  });

  final AcademicDivisionModel division;
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final VoidCallback onAddSubject;
  final ValueChanged<DivisionSubjectModel> onEditSubject;
  final ValueChanged<DivisionSubjectModel> onDeleteSubject;

  @override
  Widget build(BuildContext context) {
    final teacherName = division.classTeacher?.displayName ?? 'Not assigned';
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: const Color(0xFFE2E8F0)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 10, 8, 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    division.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                _TileMenu(onEdit: onEdit, onDelete: onDelete),
              ],
            ),
            Text(
              'Code ${division.code} - ${division.totalStudents} students',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 4),
            Text(
              'Class teacher: $teacherName',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: Text(
                    'Subjects',
                    style: Theme.of(context).textTheme.labelLarge,
                  ),
                ),
                IconButton(
                  tooltip: 'Add subject',
                  onPressed: onAddSubject,
                  icon: const Icon(Icons.add_circle_outline),
                ),
              ],
            ),
            if (division.subjects.isEmpty)
              const Padding(
                padding: EdgeInsets.only(bottom: 4),
                child: Text('No subjects assigned.'),
              )
            else
              for (final subject in division.subjects)
                ListTile(
                  dense: true,
                  contentPadding: EdgeInsets.zero,
                  title: Text(subject.subjectName),
                  subtitle: Text(
                    subject.teacher?.displayName ?? 'Teacher not assigned',
                  ),
                  trailing: PopupMenuButton<String>(
                    tooltip: 'Subject actions',
                    icon: const Icon(Icons.more_vert),
                    itemBuilder: (context) => const [
                      PopupMenuItem(value: 'edit', child: Text('Edit')),
                      PopupMenuItem(value: 'delete', child: Text('Remove')),
                    ],
                    onSelected: (value) {
                      if (value == 'edit') {
                        onEditSubject(subject);
                      } else {
                        onDeleteSubject(subject);
                      }
                    },
                  ),
                ),
          ],
        ),
      ),
    );
  }
}

class _TileMenu extends StatelessWidget {
  const _TileMenu({required this.onEdit, required this.onDelete});

  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<String>(
      tooltip: 'Actions',
      icon: const Icon(Icons.more_vert),
      itemBuilder: (context) => const [
        PopupMenuItem(value: 'edit', child: Text('Edit')),
        PopupMenuItem(value: 'delete', child: Text('Delete')),
      ],
      onSelected: (value) {
        if (value == 'edit') {
          onEdit();
        } else {
          onDelete();
        }
      },
    );
  }
}

class _PlaceholderPanel extends StatelessWidget {
  const _PlaceholderPanel({
    required this.icon,
    required this.title,
    required this.message,
  });

  final IconData icon;
  final String title;
  final String message;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                icon,
                size: 36,
                color: Theme.of(context).colorScheme.outline,
              ),
              const SizedBox(height: 12),
              Text(
                title,
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 6),
              Text(message, textAlign: TextAlign.center),
            ],
          ),
        ),
      ),
    );
  }
}

class _RetryPanel extends StatelessWidget {
  const _RetryPanel({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: AppErrorState(message: message, onRetry: onRetry),
    );
  }
}

class _PanelLoading extends StatelessWidget {
  const _PanelLoading({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Center(child: AppLoadingState(label: label));
  }
}

class _EmptyTile extends StatelessWidget {
  const _EmptyTile({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        border: Border.all(color: const Color(0xFFE2E8F0)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Padding(padding: const EdgeInsets.all(16), child: Text(message)),
    );
  }
}

class _YearDialog extends StatefulWidget {
  const _YearDialog({this.year});

  final AcademicYearModel? year;

  @override
  State<_YearDialog> createState() => _YearDialogState();
}

class _YearDialogState extends State<_YearDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _name = TextEditingController(text: widget.year?.name ?? '');
  late final _code = TextEditingController(text: widget.year?.code ?? '');
  late final _startDate = TextEditingController(
    text: widget.year == null ? '' : dateLabel(widget.year!.startDate),
  );
  late final _endDate = TextEditingController(
    text: widget.year == null ? '' : dateLabel(widget.year!.endDate),
  );
  late final _description = TextEditingController(
    text: widget.year?.description ?? '',
  );
  late var _active = widget.year?.active ?? true;
  late var _current = widget.year?.current ?? false;

  @override
  void dispose() {
    _name.dispose();
    _code.dispose();
    _startDate.dispose();
    _endDate.dispose();
    _description.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(
        widget.year == null ? 'Add academic year' : 'Edit academic year',
      ),
      content: Form(
        key: _formKey,
        child: SizedBox(
          width: 520,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: _name,
                  validator: _required,
                  decoration: const InputDecoration(
                    labelText: 'Academic year name',
                  ),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _code,
                  decoration: const InputDecoration(
                    labelText: 'Academic year code',
                  ),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _startDate,
                  validator: _date,
                  decoration: const InputDecoration(
                    labelText: 'Start date',
                    hintText: 'YYYY-MM-DD',
                  ),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _endDate,
                  validator: _date,
                  decoration: const InputDecoration(
                    labelText: 'End date',
                    hintText: 'YYYY-MM-DD',
                  ),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _description,
                  maxLines: 2,
                  decoration: const InputDecoration(labelText: 'Description'),
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Active'),
                  value: _active,
                  onChanged: (value) => setState(() {
                    _active = value;
                    if (!value) {
                      _current = false;
                    }
                  }),
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Current academic year'),
                  value: _active && _current,
                  onChanged: _active
                      ? (value) => setState(() => _current = value)
                      : null,
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
        FilledButton(
          onPressed: () {
            if (!(_formKey.currentState?.validate() ?? false)) {
              return;
            }
            final startDate = DateTime.parse(_startDate.text.trim());
            final endDate = DateTime.parse(_endDate.text.trim());
            if (!startDate.isBefore(endDate)) {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Start date must be before end date.'),
                ),
              );
              return;
            }
            Navigator.of(context).pop(
              _YearFormValue(
                name: _name.text.trim(),
                code: _blankToNull(_code.text),
                startDate: _startDate.text.trim(),
                endDate: _endDate.text.trim(),
                description: _blankToNull(_description.text),
                active: _active,
                current: _current,
              ),
            );
          },
          child: const Text('Save'),
        ),
      ],
    );
  }
}

class _ClassDialog extends StatefulWidget {
  const _ClassDialog({this.schoolClass});

  final AcademicClassModel? schoolClass;

  @override
  State<_ClassDialog> createState() => _ClassDialogState();
}

class _ClassDialogState extends State<_ClassDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _name = TextEditingController(
    text: widget.schoolClass?.name ?? '',
  );
  late final _code = TextEditingController(
    text: widget.schoolClass?.code ?? '',
  );
  late final _displayOrder = TextEditingController(
    text: (widget.schoolClass?.displayOrder ?? 0).toString(),
  );
  late var _active = widget.schoolClass?.active ?? true;

  @override
  void dispose() {
    _name.dispose();
    _code.dispose();
    _displayOrder.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.schoolClass == null ? 'Add class' : 'Edit class'),
      content: Form(
        key: _formKey,
        child: SizedBox(
          width: 460,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _name,
                validator: _required,
                decoration: const InputDecoration(labelText: 'Class name'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _code,
                decoration: const InputDecoration(labelText: 'Class code'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _displayOrder,
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                decoration: const InputDecoration(labelText: 'Display order'),
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Active'),
                value: _active,
                onChanged: (value) => setState(() => _active = value),
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
        FilledButton(
          onPressed: () {
            if (!(_formKey.currentState?.validate() ?? false)) {
              return;
            }
            Navigator.of(context).pop(
              _ClassFormValue(
                name: _name.text.trim(),
                code: _blankToNull(_code.text),
                displayOrder: int.tryParse(_displayOrder.text.trim()) ?? 0,
                active: _active,
              ),
            );
          },
          child: const Text('Save'),
        ),
      ],
    );
  }
}

class _DivisionDialog extends StatefulWidget {
  const _DivisionDialog({required this.teachers, this.division});

  final List<AcademicTeacherModel> teachers;
  final AcademicDivisionModel? division;

  @override
  State<_DivisionDialog> createState() => _DivisionDialogState();
}

class _DivisionDialogState extends State<_DivisionDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _name = TextEditingController(text: widget.division?.name ?? '');
  late final _code = TextEditingController(text: widget.division?.code ?? '');
  late final _capacity = TextEditingController(
    text: widget.division?.capacity?.toString() ?? '',
  );
  late final _displayOrder = TextEditingController(
    text: (widget.division?.displayOrder ?? 0).toString(),
  );
  late var _teacherId = widget.division?.classTeacher?.id;
  late var _active = widget.division?.active ?? true;

  @override
  void dispose() {
    _name.dispose();
    _code.dispose();
    _capacity.dispose();
    _displayOrder.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.division == null ? 'Add division' : 'Edit division'),
      content: Form(
        key: _formKey,
        child: SizedBox(
          width: 520,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: _name,
                  validator: _required,
                  decoration: const InputDecoration(labelText: 'Division name'),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _code,
                  decoration: const InputDecoration(labelText: 'Division code'),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _capacity,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  decoration: const InputDecoration(labelText: 'Capacity'),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _displayOrder,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  decoration: const InputDecoration(labelText: 'Display order'),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _teacherId,
                  isExpanded: true,
                  decoration: const InputDecoration(labelText: 'Class teacher'),
                  items: [
                    const DropdownMenuItem(
                      value: null,
                      child: Text('Not assigned'),
                    ),
                    for (final teacher in widget.teachers)
                      DropdownMenuItem(
                        value: teacher.id,
                        child: Text(teacher.displayName),
                      ),
                  ],
                  onChanged: (value) => setState(() => _teacherId = value),
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Active'),
                  value: _active,
                  onChanged: (value) => setState(() => _active = value),
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
        FilledButton(
          onPressed: () {
            if (!(_formKey.currentState?.validate() ?? false)) {
              return;
            }
            Navigator.of(context).pop(
              _DivisionFormValue(
                name: _name.text.trim(),
                code: _blankToNull(_code.text),
                capacity: int.tryParse(_capacity.text.trim()),
                displayOrder: int.tryParse(_displayOrder.text.trim()) ?? 0,
                classTeacherId: _teacherId,
                active: _active,
              ),
            );
          },
          child: const Text('Save'),
        ),
      ],
    );
  }
}

class _SubjectDialog extends StatefulWidget {
  const _SubjectDialog({required this.teachers, this.subject});

  final List<AcademicTeacherModel> teachers;
  final DivisionSubjectModel? subject;

  @override
  State<_SubjectDialog> createState() => _SubjectDialogState();
}

class _SubjectDialogState extends State<_SubjectDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _name = TextEditingController(
    text: widget.subject?.subjectName ?? '',
  );
  late final _code = TextEditingController(
    text: widget.subject?.subjectCode ?? '',
  );
  late var _teacherId = widget.subject?.teacher?.id;
  late var _active = widget.subject?.active ?? true;

  bool get _editing => widget.subject != null;

  @override
  void dispose() {
    _name.dispose();
    _code.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(_editing ? 'Edit subject' : 'Add subject'),
      content: Form(
        key: _formKey,
        child: SizedBox(
          width: 480,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _name,
                enabled: !_editing,
                validator: _editing ? null : _required,
                decoration: const InputDecoration(labelText: 'Subject name'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _code,
                enabled: !_editing,
                validator: _editing ? null : _required,
                textCapitalization: TextCapitalization.characters,
                decoration: const InputDecoration(labelText: 'Subject code'),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: _teacherId,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Subject teacher'),
                items: [
                  const DropdownMenuItem(
                    value: null,
                    child: Text('Not assigned'),
                  ),
                  for (final teacher in widget.teachers)
                    DropdownMenuItem(
                      value: teacher.id,
                      child: Text(teacher.displayName),
                    ),
                ],
                onChanged: (value) => setState(() => _teacherId = value),
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Active'),
                value: _active,
                onChanged: (value) => setState(() => _active = value),
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
        FilledButton(
          onPressed: () {
            if (!(_formKey.currentState?.validate() ?? false)) {
              return;
            }
            Navigator.of(context).pop(
              _SubjectFormValue(
                subjectId: widget.subject?.subjectId,
                subjectName: _name.text.trim(),
                subjectCode: _code.text.trim(),
                teacherId: _teacherId,
                active: _active,
              ),
            );
          },
          child: const Text('Save'),
        ),
      ],
    );
  }
}

class _YearFormValue {
  const _YearFormValue({
    required this.name,
    required this.startDate,
    required this.endDate,
    required this.active,
    required this.current,
    this.code,
    this.description,
  });

  final String name;
  final String? code;
  final String startDate;
  final String endDate;
  final bool active;
  final bool current;
  final String? description;

  Map<String, dynamic> toPayload() {
    return {
      'name': name,
      'code': code,
      'startDate': startDate,
      'endDate': endDate,
      'active': active,
      'current': current,
      'description': description,
    };
  }
}

class _ClassFormValue {
  const _ClassFormValue({
    required this.name,
    required this.displayOrder,
    required this.active,
    this.code,
  });

  final String name;
  final String? code;
  final int displayOrder;
  final bool active;

  Map<String, dynamic> toPayload() {
    return {
      'name': name,
      'code': code,
      'displayOrder': displayOrder,
      'active': active,
    };
  }
}

class _DivisionFormValue {
  const _DivisionFormValue({
    required this.name,
    required this.displayOrder,
    required this.active,
    this.code,
    this.capacity,
    this.classTeacherId,
  });

  final String name;
  final String? code;
  final int? capacity;
  final int displayOrder;
  final bool active;
  final String? classTeacherId;

  Map<String, dynamic> toPayload() {
    return {
      'name': name,
      'code': code,
      'capacity': capacity,
      'displayOrder': displayOrder,
      'active': active,
      'classTeacherId': classTeacherId,
    };
  }
}

class _SubjectFormValue {
  const _SubjectFormValue({
    required this.subjectName,
    required this.subjectCode,
    required this.active,
    this.subjectId,
    this.teacherId,
  });

  final String? subjectId;
  final String subjectName;
  final String subjectCode;
  final String? teacherId;
  final bool active;

  Map<String, dynamic> toCreatePayload() {
    return {
      'subjectId': subjectId,
      'subjectName': subjectName,
      'subjectCode': subjectCode,
      'teacherId': teacherId,
      'active': active,
    };
  }

  Map<String, dynamic> toUpdatePayload() {
    return {'subjectId': subjectId, 'teacherId': teacherId, 'active': active};
  }
}

String? _validId(String? current, Iterable<String> ids) {
  final values = ids.toList(growable: false);
  if (current != null && values.contains(current)) {
    return current;
  }
  return values.isEmpty ? null : values.first;
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

String? _blankToNull(String value) {
  final trimmed = value.trim();
  return trimmed.isEmpty ? null : trimmed;
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
