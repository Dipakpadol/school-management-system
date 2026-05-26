import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/fee_models.dart';
import '../../data/repositories/fees_repository_impl.dart';
import '../controllers/fees_providers.dart';

class StudentFeeAssignmentPage extends ConsumerStatefulWidget {
  const StudentFeeAssignmentPage({super.key});

  @override
  ConsumerState<StudentFeeAssignmentPage> createState() {
    return _StudentFeeAssignmentPageState();
  }
}

class _StudentFeeAssignmentPageState
    extends ConsumerState<StudentFeeAssignmentPage> {
  final _formKey = GlobalKey<FormState>();
  final _studentIdController = TextEditingController();
  final _assignedDateController = TextEditingController();
  final _notesController = TextEditingController();

  String? _feeStructureId;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _assignedDateController.text = _dateLabel(DateTime.now());
  }

  @override
  void dispose() {
    _studentIdController.dispose();
    _assignedDateController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final structures = ref.watch(feeStructuresProvider);

    return AdminShell(
      title: 'Student fee assignment',
      activeModuleId: 'fees',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: structures.when(
        data: _buildForm,
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(feeStructuresProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading structures'),
      ),
    );
  }

  Widget _buildForm(List<FeeStructureModel> structures) {
    final structureIds = structures.map((structure) => structure.id).toSet();
    final selectedStructure = structureIds.contains(_feeStructureId)
        ? _feeStructureId
        : null;

    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 860),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _Header(
                  onBack: () => context.go(AppRoutes.fees),
                  onSave: _saving ? null : _submit,
                  saving: _saving,
                ),
                const SizedBox(height: 12),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: Column(
                      children: [
                        DropdownButtonFormField<String>(
                          initialValue: selectedStructure,
                          isExpanded: true,
                          items: [
                            for (final structure in structures)
                              DropdownMenuItem(
                                value: structure.id,
                                child: Text(
                                  '${structure.name} - ${structure.academicYear}',
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                          ],
                          onChanged: (value) {
                            setState(() => _feeStructureId = value);
                          },
                          validator: _required,
                          decoration: const InputDecoration(
                            labelText: 'Fee structure',
                            prefixIcon: Icon(Icons.account_tree_outlined),
                          ),
                        ),
                        const SizedBox(height: 12),
                        _ResponsiveFields(
                          children: [
                            TextFormField(
                              controller: _studentIdController,
                              validator: _uuid,
                              decoration: const InputDecoration(
                                labelText: 'Student ID',
                                prefixIcon: Icon(Icons.badge_outlined),
                              ),
                            ),
                            TextFormField(
                              controller: _assignedDateController,
                              validator: _date,
                              decoration: const InputDecoration(
                                labelText: 'Assigned date',
                                hintText: 'YYYY-MM-DD',
                                prefixIcon: Icon(Icons.event_outlined),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: _notesController,
                          maxLines: 3,
                          decoration: const InputDecoration(
                            labelText: 'Notes',
                            alignLabelWithHint: true,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }
    setState(() => _saving = true);
    final result = await ref.read(feesRepositoryProvider).createAssignment({
      'studentId': _studentIdController.text.trim(),
      'feeStructureId': _feeStructureId,
      'assignedDate': _assignedDateController.text.trim(),
      'notes': _blankToNull(_notesController.text),
    });
    if (!mounted) {
      return;
    }
    setState(() => _saving = false);
    result.when<void>(
      success: (_) {
        ref.invalidate(feeAssignmentsProvider);
        ref.invalidate(feeDefaultersProvider);
        context.go(AppRoutes.fees);
      },
      failure: (failure) => _showSnack(failure.message),
    );
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}

class _Header extends StatelessWidget {
  const _Header({
    required this.onBack,
    required this.onSave,
    required this.saving,
  });

  final VoidCallback onBack;
  final VoidCallback? onSave;
  final bool saving;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Wrap(
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          runSpacing: 12,
          children: [
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  tooltip: 'Back',
                  onPressed: onBack,
                  icon: const Icon(Icons.arrow_back),
                ),
                const SizedBox(width: 8),
                Text(
                  'Assign fee structure',
                  style: Theme.of(
                    context,
                  ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
                ),
              ],
            ),
            AppButton(
              label: 'Assign',
              icon: Icons.assignment_ind_outlined,
              isLoading: saving,
              onPressed: onSave,
            ),
          ],
        ),
      ),
    );
  }
}

class _ResponsiveFields extends StatelessWidget {
  const _ResponsiveFields({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 720 ? 2 : 1;
        return GridView.count(
          crossAxisCount: columns,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          childAspectRatio: columns == 1 ? 5.2 : 4.8,
          physics: const NeverScrollableScrollPhysics(),
          shrinkWrap: true,
          children: children,
        );
      },
    );
  }
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _uuid(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return 'Required';
  }
  final uuidPattern = RegExp(
    r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
  );
  return uuidPattern.hasMatch(text) ? null : 'Enter a valid UUID';
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

String _dateLabel(DateTime date) {
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
