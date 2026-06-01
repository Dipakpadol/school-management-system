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
import '../../../academic/data/models/academic_models.dart' as academic_models;
import '../../../academic/presentation/controllers/academic_providers.dart'
    as academic;
import '../../data/models/fee_models.dart';
import '../../data/repositories/fees_repository_impl.dart';
import '../controllers/fees_providers.dart';
import '../widgets/fee_widgets.dart';

class FeeStructureFormPage extends ConsumerStatefulWidget {
  const FeeStructureFormPage({this.structureId, super.key});

  final String? structureId;

  @override
  ConsumerState<FeeStructureFormPage> createState() {
    return _FeeStructureFormPageState();
  }
}

class _FeeStructureFormPageState extends ConsumerState<FeeStructureFormPage> {
  final _formKey = GlobalKey<FormState>();
  final _academicYearController = TextEditingController();
  final _classNameController = TextEditingController();
  final _sectionNameController = TextEditingController();
  final _nameController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _items = <_FeeItemInput>[];
  final _installments = <_InstallmentInput>[];

  String? _academicYearId;
  String? _classId;
  bool _activate = true;
  bool _saving = false;
  bool _prefillQueued = false;
  bool _prefilled = false;

  bool get _isEditing => widget.structureId != null;

  @override
  void initState() {
    super.initState();
    _addItem();
    _addInstallment();
  }

  @override
  void dispose() {
    _academicYearController.dispose();
    _classNameController.dispose();
    _sectionNameController.dispose();
    _nameController.dispose();
    _descriptionController.dispose();
    for (final item in _items) {
      item.dispose();
    }
    for (final installment in _installments) {
      installment.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final categories = ref.watch(feeCategoriesProvider);
    final structure = _isEditing
        ? ref.watch(feeStructureProvider(widget.structureId!))
        : null;

    return AdminShell(
      title: _isEditing ? 'Edit fee structure' : 'Add fee structure',
      activeModuleId: 'fees',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: categories.when(
        data: (categoryItems) {
          final academicYears = ref.watch(academic.academicYearsProvider);
          return academicYears.when(
            data: (yearItems) {
              if (structure == null) {
                return _buildForm(categoryItems, academicYears: yearItems);
              }
              return structure.when(
                data: (feeStructure) {
                  _queuePrefill(feeStructure);
                  return _buildForm(categoryItems, academicYears: yearItems);
                },
                error: (error, _) => AppErrorState(
                  message: _message(error),
                  onRetry: () {
                    ref.invalidate(feeStructureProvider(widget.structureId!));
                  },
                ),
                loading: () =>
                    const AppLoadingState(label: 'Loading structure'),
              );
            },
            error: (error, _) => AppErrorState(
              message: _message(error),
              onRetry: () => ref.invalidate(academic.academicYearsProvider),
            ),
            loading: () =>
                const AppLoadingState(label: 'Loading academic years'),
          );
        },
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(feeCategoriesProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading categories'),
      ),
    );
  }

  Widget _buildForm(
    List<FeeCategoryModel> categories, {
    List<academic_models.AcademicYearModel> academicYears = const [],
    bool readOnly = false,
  }) {
    return Form(
      key: _formKey,
      child: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(24, 24, 24, 12),
            sliver: SliverToBoxAdapter(
              child: _FormHeader(
                title: _isEditing ? 'Fee structure details' : 'New structure',
                readOnly: readOnly,
                onBack: () => context.go(AppRoutes.fees),
                onSave: readOnly || _saving ? null : _submit,
                saving: _saving,
              ),
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
            sliver: SliverToBoxAdapter(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 1180),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _SectionCard(
                      title: 'Structure',
                      child: _ResponsiveFields(
                        children: [
                          ..._academicFields(academicYears, readOnly),
                          _TextInput(
                            controller: _sectionNameController,
                            label: 'Section',
                            enabled: !readOnly,
                          ),
                          _TextInput(
                            controller: _nameController,
                            label: 'Structure name',
                            enabled: !readOnly,
                            validator: _required,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                    _SectionCard(
                      title: 'Description',
                      child: Column(
                        children: [
                          TextFormField(
                            controller: _descriptionController,
                            enabled: !readOnly,
                            maxLines: 3,
                            decoration: const InputDecoration(
                              labelText: 'Description',
                              alignLabelWithHint: true,
                            ),
                          ),
                          const SizedBox(height: 12),
                          SwitchListTile(
                            contentPadding: EdgeInsets.zero,
                            title: const Text('Activate structure'),
                            value: _activate,
                            onChanged: readOnly
                                ? null
                                : (value) => setState(() => _activate = value),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                    _SectionCard(
                      title: 'Fee items',
                      trailing: IconButton(
                        tooltip: 'Add item',
                        onPressed: readOnly
                            ? null
                            : () => setState(() => _addItem()),
                        icon: const Icon(Icons.add_circle_outline),
                      ),
                      child: Column(
                        children: [
                          for (var index = 0; index < _items.length; index++)
                            _FeeItemRow(
                              input: _items[index],
                              categories: categories,
                              readOnly: readOnly,
                              onRemove: _items.length == 1 || readOnly
                                  ? null
                                  : () => setState(() {
                                      _items.removeAt(index).dispose();
                                    }),
                            ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                    _SectionCard(
                      title: 'Installments',
                      trailing: IconButton(
                        tooltip: 'Add installment',
                        onPressed: readOnly
                            ? null
                            : () => setState(() => _addInstallment()),
                        icon: const Icon(Icons.add_circle_outline),
                      ),
                      child: Column(
                        children: [
                          for (
                            var index = 0;
                            index < _installments.length;
                            index++
                          )
                            _InstallmentRow(
                              input: _installments[index],
                              sequenceNo: index + 1,
                              readOnly: readOnly,
                              onRemove: _installments.length == 1 || readOnly
                                  ? null
                                  : () => setState(() {
                                      _installments.removeAt(index).dispose();
                                    }),
                            ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 18),
                    Align(
                      alignment: Alignment.centerRight,
                      child: Wrap(
                        spacing: 10,
                        runSpacing: 10,
                        children: [
                          OutlinedButton.icon(
                            onPressed: () => context.go(AppRoutes.fees),
                            icon: const Icon(Icons.arrow_back),
                            label: const Text('Back'),
                          ),
                          AppButton(
                            label: _isEditing ? 'Save changes' : 'Save',
                            icon: Icons.save_outlined,
                            isLoading: _saving,
                            onPressed: readOnly || _saving ? null : _submit,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  List<Widget> _academicFields(
    List<academic_models.AcademicYearModel> academicYears,
    bool readOnly,
  ) {
    if (academicYears.isEmpty) {
      return [
        _TextInput(
          controller: _academicYearController,
          label: 'Academic year',
          enabled: !readOnly,
          validator: _required,
        ),
        _TextInput(
          controller: _classNameController,
          label: 'Class',
          enabled: !readOnly,
          validator: _required,
        ),
      ];
    }

    final selectedYearId = _effectiveAcademicYearId(academicYears);
    final selectedYear = _findYear(academicYears, selectedYearId);
    if (selectedYear != null &&
        (_academicYearId != selectedYearId ||
            _academicYearController.text != selectedYear.name)) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) {
          return;
        }
        setState(() {
          _academicYearId = selectedYear.id;
          _academicYearController.text = selectedYear.name;
        });
      });
    }

    return [
      DropdownButtonFormField<String>(
        key: ValueKey('fee-year-$selectedYearId'),
        initialValue: selectedYearId,
        isExpanded: true,
        items: [
          for (final year in academicYears)
            DropdownMenuItem(value: year.id, child: Text(year.name)),
        ],
        onChanged: readOnly
            ? null
            : (value) {
                final selectedYear = _findYear(academicYears, value);
                setState(() {
                  _academicYearId = value;
                  _classId = null;
                  _classNameController.clear();
                  _academicYearController.text = selectedYear?.name ?? '';
                });
              },
        validator: _required,
        decoration: const InputDecoration(
          labelText: 'Academic year',
          prefixIcon: Icon(Icons.calendar_month_outlined),
        ),
      ),
      _ClassDropdownField(
        academicYearId: selectedYearId,
        selectedClassId: _classId,
        className: _classNameController.text,
        readOnly: readOnly,
        onSelected: (schoolClass) {
          setState(() {
            _classId = schoolClass.id;
            _classNameController.text = schoolClass.name;
          });
        },
        onResolved: (schoolClass) {
          if (!mounted ||
              (_classId == schoolClass.id &&
                  _classNameController.text == schoolClass.name)) {
            return;
          }
          setState(() {
            _classId = schoolClass.id;
            _classNameController.text = schoolClass.name;
          });
        },
      ),
    ];
  }

  String _effectiveAcademicYearId(
    List<academic_models.AcademicYearModel> academicYears,
  ) {
    final selectedId = _academicYearId;
    if (selectedId != null && _findYear(academicYears, selectedId) != null) {
      return selectedId;
    }
    final text = _academicYearController.text.trim().toLowerCase();
    if (text.isNotEmpty) {
      for (final year in academicYears) {
        if (year.name.toLowerCase() == text ||
            year.code.toLowerCase() == text) {
          return year.id;
        }
      }
    }
    return academicYears.first.id;
  }

  academic_models.AcademicYearModel? _findYear(
    List<academic_models.AcademicYearModel> academicYears,
    String? yearId,
  ) {
    if (yearId == null) {
      return null;
    }
    for (final year in academicYears) {
      if (year.id == yearId) {
        return year;
      }
    }
    return null;
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }
    if (_academicYearController.text.trim().isEmpty ||
        _classNameController.text.trim().isEmpty) {
      _showSnack('Academic year and class are required.');
      return;
    }

    final itemTotal = _items.fold<double>(
      0,
      (total, item) => total + (_amount(item.amountController.text) ?? 0),
    );
    final installmentTotal = _installments.fold<double>(0, (
      total,
      installment,
    ) {
      return total + (_amount(installment.amountController.text) ?? 0);
    });
    if ((itemTotal - installmentTotal).abs() > 0.009) {
      _showSnack('Fee item total must equal installment total.');
      return;
    }

    setState(() => _saving = true);
    final structureId = widget.structureId;
    final repository = ref.read(feesRepositoryProvider);
    final result = structureId == null
        ? await repository.createStructure(_payload())
        : await repository.updateStructure(structureId, _payload());
    if (!mounted) {
      return;
    }
    setState(() => _saving = false);
    result.when<void>(
      success: (_) {
        ref.invalidate(feeStructuresProvider);
        if (structureId != null) {
          ref.invalidate(feeStructureProvider(structureId));
        }
        context.go(AppRoutes.fees);
      },
      failure: (failure) => _showSnack(failure.message),
    );
  }

  Map<String, dynamic> _payload() {
    return {
      'academicYear': _academicYearController.text.trim(),
      'className': _classNameController.text.trim(),
      'sectionName': _blankToNull(_sectionNameController.text),
      'name': _nameController.text.trim(),
      'description': _blankToNull(_descriptionController.text),
      'activate': _activate,
      'items': [
        for (var index = 0; index < _items.length; index++)
          {
            'categoryId': _items[index].categoryId,
            'amount': _amount(_items[index].amountController.text),
            'mandatory': _items[index].mandatory,
            'sortOrder': index + 1,
          },
      ],
      'installments': [
        for (var index = 0; index < _installments.length; index++)
          {
            'sequenceNo': index + 1,
            'title': _installments[index].titleController.text.trim(),
            'dueDate': _installments[index].dueDateController.text.trim(),
            'amount': _amount(_installments[index].amountController.text),
          },
      ],
    };
  }

  void _queuePrefill(FeeStructureModel structure) {
    if (_prefillQueued || _prefilled) {
      return;
    }
    _prefillQueued = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || _prefilled) {
        return;
      }
      setState(() {
        _academicYearController.text = structure.academicYear;
        _classNameController.text = structure.className;
        _academicYearId = structure.academicYearId;
        _classId = structure.classId;
        _sectionNameController.text = structure.sectionName ?? '';
        _nameController.text = structure.name;
        _descriptionController.text = structure.description ?? '';
        _activate = structure.status == 'ACTIVE';

        for (final item in _items) {
          item.dispose();
        }
        _items
          ..clear()
          ..addAll(
            structure.items.map(
              (item) => _FeeItemInput(
                categoryId: item.categoryId,
                amount: item.amount,
                mandatory: item.mandatory,
              ),
            ),
          );
        if (_items.isEmpty) {
          _addItem();
        }

        for (final installment in _installments) {
          installment.dispose();
        }
        _installments
          ..clear()
          ..addAll(
            structure.installments.map(
              (installment) => _InstallmentInput(
                title: installment.title,
                dueDate: dateLabel(installment.dueDate),
                amount: installment.amount,
              ),
            ),
          );
        if (_installments.isEmpty) {
          _addInstallment();
        }
        _prefilled = true;
      });
    });
  }

  void _addItem() {
    _items.add(_FeeItemInput());
  }

  void _addInstallment() {
    _installments.add(_InstallmentInput());
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}

class _ClassDropdownField extends ConsumerWidget {
  const _ClassDropdownField({
    required this.academicYearId,
    required this.selectedClassId,
    required this.className,
    required this.readOnly,
    required this.onSelected,
    required this.onResolved,
  });

  final String academicYearId;
  final String? selectedClassId;
  final String className;
  final bool readOnly;
  final ValueChanged<academic_models.AcademicClassModel> onSelected;
  final ValueChanged<academic_models.AcademicClassModel> onResolved;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final classes = ref.watch(academic.academicClassesProvider(academicYearId));
    return classes.when(
      data: (items) {
        if (items.isEmpty) {
          return const InputDecorator(
            decoration: InputDecoration(
              labelText: 'Class',
              prefixIcon: Icon(Icons.school_outlined),
            ),
            child: Text('No classes available'),
          );
        }
        final selectedClass = _effectiveClass(items);
        WidgetsBinding.instance.addPostFrameCallback((_) {
          onResolved(selectedClass);
        });
        return DropdownButtonFormField<String>(
          key: ValueKey('fee-class-${selectedClass.id}'),
          initialValue: selectedClass.id,
          isExpanded: true,
          items: [
            for (final schoolClass in items)
              DropdownMenuItem(
                value: schoolClass.id,
                child: Text(schoolClass.name),
              ),
          ],
          onChanged: readOnly
              ? null
              : (value) {
                  final selected = _findClass(items, value);
                  if (selected != null) {
                    onSelected(selected);
                  }
                },
          validator: _required,
          decoration: const InputDecoration(
            labelText: 'Class',
            prefixIcon: Icon(Icons.school_outlined),
          ),
        );
      },
      error: (error, _) => InputDecorator(
        decoration: const InputDecoration(
          labelText: 'Class',
          prefixIcon: Icon(Icons.school_outlined),
        ),
        child: Text(_message(error)),
      ),
      loading: () => const InputDecorator(
        decoration: InputDecoration(
          labelText: 'Class',
          prefixIcon: Icon(Icons.school_outlined),
        ),
        child: LinearProgressIndicator(),
      ),
    );
  }

  academic_models.AcademicClassModel _effectiveClass(
    List<academic_models.AcademicClassModel> classes,
  ) {
    final selected = _findClass(classes, selectedClassId);
    if (selected != null) {
      return selected;
    }
    final text = className.trim().toLowerCase();
    if (text.isNotEmpty) {
      for (final schoolClass in classes) {
        if (schoolClass.name.toLowerCase() == text ||
            schoolClass.code.toLowerCase() == text) {
          return schoolClass;
        }
      }
    }
    return classes.first;
  }

  academic_models.AcademicClassModel? _findClass(
    List<academic_models.AcademicClassModel> classes,
    String? classId,
  ) {
    if (classId == null) {
      return null;
    }
    for (final schoolClass in classes) {
      if (schoolClass.id == classId) {
        return schoolClass;
      }
    }
    return null;
  }
}

class _FormHeader extends StatelessWidget {
  const _FormHeader({
    required this.title,
    required this.readOnly,
    required this.onBack,
    required this.onSave,
    required this.saving,
  });

  final String title;
  final bool readOnly;
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
                  title,
                  style: Theme.of(
                    context,
                  ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
                ),
              ],
            ),
            AppButton(
              label: readOnly ? 'Save changes' : 'Save',
              icon: Icons.save_outlined,
              isLoading: saving,
              onPressed: onSave,
            ),
          ],
        ),
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({required this.title, required this.child, this.trailing});

  final String title;
  final Widget child;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                ?trailing,
              ],
            ),
            const SizedBox(height: 14),
            child,
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
        final columns = constraints.maxWidth >= 820 ? 2 : 1;
        return GridView.count(
          crossAxisCount: columns,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          childAspectRatio: columns == 1 ? 5.4 : 5.8,
          physics: const NeverScrollableScrollPhysics(),
          shrinkWrap: true,
          children: children,
        );
      },
    );
  }
}

class _TextInput extends StatelessWidget {
  const _TextInput({
    required this.controller,
    required this.label,
    this.enabled = true,
    this.validator,
  });

  final TextEditingController controller;
  final String label;
  final bool enabled;
  final FormFieldValidator<String>? validator;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      enabled: enabled,
      validator: validator,
      decoration: InputDecoration(labelText: label),
    );
  }
}

class _FeeItemRow extends StatefulWidget {
  const _FeeItemRow({
    required this.input,
    required this.categories,
    required this.readOnly,
    this.onRemove,
  });

  final _FeeItemInput input;
  final List<FeeCategoryModel> categories;
  final bool readOnly;
  final VoidCallback? onRemove;

  @override
  State<_FeeItemRow> createState() => _FeeItemRowState();
}

class _FeeItemRowState extends State<_FeeItemRow> {
  @override
  Widget build(BuildContext context) {
    final categoryIds = widget.categories
        .map((category) => category.id)
        .toSet();
    final missingSelectedCategory =
        widget.input.categoryId != null &&
        !categoryIds.contains(widget.input.categoryId);
    final selectedCategory = widget.input.categoryId;

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final compact = constraints.maxWidth < 680;
          final categoryField = DropdownButtonFormField<String>(
            initialValue: selectedCategory,
            isExpanded: true,
            items: [
              if (missingSelectedCategory)
                DropdownMenuItem(
                  value: widget.input.categoryId!,
                  child: const Text('Current category'),
                ),
              for (final category in widget.categories)
                DropdownMenuItem(
                  value: category.id,
                  child: Text('${category.code} - ${category.name}'),
                ),
            ],
            onChanged: widget.readOnly
                ? null
                : (value) => setState(() => widget.input.categoryId = value),
            validator: widget.readOnly ? null : _required,
            decoration: const InputDecoration(labelText: 'Category'),
          );
          final amountField = TextFormField(
            controller: widget.input.amountController,
            enabled: !widget.readOnly,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            inputFormatters: [_MoneyFormatter()],
            validator: _positiveAmount,
            decoration: const InputDecoration(labelText: 'Amount'),
          );
          final mandatoryField = CheckboxListTile(
            contentPadding: EdgeInsets.zero,
            controlAffinity: ListTileControlAffinity.leading,
            title: const Text('Mandatory'),
            value: widget.input.mandatory,
            onChanged: widget.readOnly
                ? null
                : (value) {
                    setState(() {
                      widget.input.mandatory = value ?? true;
                    });
                  },
          );
          final removeButton = IconButton(
            tooltip: 'Remove item',
            onPressed: widget.onRemove,
            icon: const Icon(Icons.remove_circle_outline),
          );

          if (compact) {
            return Column(
              children: [
                categoryField,
                const SizedBox(height: 10),
                amountField,
                Row(
                  children: [
                    Expanded(child: mandatoryField),
                    removeButton,
                  ],
                ),
              ],
            );
          }
          return Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(flex: 3, child: categoryField),
              const SizedBox(width: 12),
              Expanded(flex: 2, child: amountField),
              const SizedBox(width: 12),
              SizedBox(width: 170, child: mandatoryField),
              removeButton,
            ],
          );
        },
      ),
    );
  }
}

class _InstallmentRow extends StatelessWidget {
  const _InstallmentRow({
    required this.input,
    required this.sequenceNo,
    required this.readOnly,
    this.onRemove,
  });

  final _InstallmentInput input;
  final int sequenceNo;
  final bool readOnly;
  final VoidCallback? onRemove;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final compact = constraints.maxWidth < 760;
          final sequence = SizedBox(
            width: 48,
            child: Center(
              child: Text(
                sequenceNo.toString(),
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
              ),
            ),
          );
          final title = TextFormField(
            controller: input.titleController,
            enabled: !readOnly,
            validator: _required,
            decoration: const InputDecoration(labelText: 'Title'),
          );
          final dueDate = TextFormField(
            controller: input.dueDateController,
            enabled: !readOnly,
            validator: _date,
            decoration: const InputDecoration(
              labelText: 'Due date',
              hintText: 'YYYY-MM-DD',
            ),
          );
          final amount = TextFormField(
            controller: input.amountController,
            enabled: !readOnly,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            inputFormatters: [_MoneyFormatter()],
            validator: _positiveAmount,
            decoration: const InputDecoration(labelText: 'Amount'),
          );
          final remove = IconButton(
            tooltip: 'Remove installment',
            onPressed: onRemove,
            icon: const Icon(Icons.remove_circle_outline),
          );

          if (compact) {
            return Column(
              children: [
                Row(
                  children: [
                    sequence,
                    Expanded(child: title),
                    remove,
                  ],
                ),
                const SizedBox(height: 10),
                dueDate,
                const SizedBox(height: 10),
                amount,
              ],
            );
          }
          return Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              sequence,
              Expanded(flex: 3, child: title),
              const SizedBox(width: 12),
              Expanded(flex: 2, child: dueDate),
              const SizedBox(width: 12),
              Expanded(flex: 2, child: amount),
              remove,
            ],
          );
        },
      ),
    );
  }
}

class _FeeItemInput {
  _FeeItemInput({this.categoryId, double? amount, this.mandatory = true})
    : amountController = TextEditingController(
        text: amount == null ? '' : amount.toStringAsFixed(2),
      );

  String? categoryId;
  bool mandatory;
  final TextEditingController amountController;

  void dispose() {
    amountController.dispose();
  }
}

class _InstallmentInput {
  _InstallmentInput({String? title, String? dueDate, double? amount})
    : titleController = TextEditingController(text: title ?? ''),
      dueDateController = TextEditingController(text: dueDate ?? ''),
      amountController = TextEditingController(
        text: amount == null ? '' : amount.toStringAsFixed(2),
      );

  final TextEditingController titleController;
  final TextEditingController dueDateController;
  final TextEditingController amountController;

  void dispose() {
    titleController.dispose();
    dueDateController.dispose();
    amountController.dispose();
  }
}

class _MoneyFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final text = newValue.text;
    if (text.isEmpty || RegExp(r'^\d{0,10}(\.\d{0,2})?$').hasMatch(text)) {
      return newValue;
    }
    return oldValue;
  }
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _positiveAmount(String? value) {
  final amount = _amount(value);
  if (amount == null || amount <= 0) {
    return 'Enter a valid amount';
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

double? _amount(String? value) {
  return double.tryParse(value?.trim() ?? '');
}

String? _blankToNull(String value) {
  final trimmed = value.trim();
  return trimmed.isEmpty ? null : trimmed;
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
