import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router/app_routes.dart';
import '../../../core/result/result.dart';
import '../../../core/widgets/admin_shell.dart';
import '../../../core/widgets/app_error_state.dart';
import '../../../core/widgets/app_loading_state.dart';
import '../../../core/upload/file_picker.dart';
import '../../auth/presentation/controllers/auth_controller.dart';
import '../../module_records/data/models/module_record_model.dart';
import '../../module_records/data/repositories/module_records_repository_impl.dart';
import '../../module_records/presentation/controllers/module_records_providers.dart';
import '../domain/module_registry.dart';

class ModulePlaceholderPage extends ConsumerStatefulWidget {
  const ModulePlaceholderPage({required this.moduleId, super.key});

  final String moduleId;

  @override
  ConsumerState<ModulePlaceholderPage> createState() =>
      _ModulePlaceholderPageState();
}

class _ModulePlaceholderPageState extends ConsumerState<ModulePlaceholderPage> {
  final _searchController = TextEditingController();
  late String _recordType;
  String _query = '';

  @override
  void initState() {
    super.initState();
    _recordType = _recordTypes(widget.moduleId).first;
  }

  @override
  void didUpdateWidget(covariant ModulePlaceholderPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.moduleId != widget.moduleId) {
      _recordType = _recordTypes(widget.moduleId).first;
      _query = '';
      _searchController.clear();
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final module = findModuleById(widget.moduleId);
    final query = ModuleRecordsQuery(
      moduleId: widget.moduleId,
      recordType: _recordType,
      query: _query,
    );
    final records = ref.watch(moduleRecordsProvider(query));

    return AdminShell(
      title: module?.title ?? _titleCase(widget.moduleId),
      activeModuleId: module?.id,
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: Column(
        children: [
          _ModuleRecordsHeader(
            moduleId: widget.moduleId,
            recordType: _recordType,
            recordTypes: _recordTypes(widget.moduleId),
            searchController: _searchController,
            onRecordTypeChanged: (value) {
              setState(() {
                _recordType = value;
                _query = '';
                _searchController.clear();
              });
            },
            onSearch: () {
              setState(() {
                _query = _searchController.text.trim();
              });
            },
            onAdd: () => _showRecordDialog(
              context,
              moduleId: widget.moduleId,
              recordType: _recordType,
              onChanged: () => ref.invalidate(moduleRecordsProvider(query)),
            ),
            onRefresh: () => ref.invalidate(moduleRecordsProvider(query)),
          ),
          const Divider(height: 1),
          Expanded(
            child: records.when(
              data: (items) => _ModuleRecordList(
                moduleId: widget.moduleId,
                recordType: _recordType,
                records: items,
                onChanged: () => ref.invalidate(moduleRecordsProvider(query)),
              ),
              error: (error, _) => AppErrorState(
                message: error.toString().replaceFirst('Exception: ', ''),
                onRetry: () => ref.invalidate(moduleRecordsProvider(query)),
              ),
              loading: () =>
                  AppLoadingState(label: 'Loading ${_titleCase(_recordType)}'),
            ),
          ),
        ],
      ),
    );
  }
}

class _ModuleRecordsHeader extends ConsumerWidget {
  const _ModuleRecordsHeader({
    required this.moduleId,
    required this.recordType,
    required this.recordTypes,
    required this.searchController,
    required this.onRecordTypeChanged,
    required this.onSearch,
    required this.onAdd,
    required this.onRefresh,
  });

  final String moduleId;
  final String recordType;
  final List<String> recordTypes;
  final TextEditingController searchController;
  final ValueChanged<String> onRecordTypeChanged;
  final VoidCallback onSearch;
  final VoidCallback onAdd;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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
              width: 230,
              child: DropdownButtonFormField<String>(
                initialValue: recordType,
                decoration: const InputDecoration(
                  labelText: 'Record type',
                  prefixIcon: Icon(Icons.category_outlined),
                ),
                items: [
                  for (final type in recordTypes)
                    DropdownMenuItem(
                      value: type,
                      child: Text(_titleCase(type)),
                    ),
                ],
                onChanged: (value) {
                  if (value != null) {
                    onRecordTypeChanged(value);
                  }
                },
              ),
            ),
            SizedBox(
              width: 300,
              child: TextField(
                controller: searchController,
                decoration: const InputDecoration(
                  labelText: 'Search records',
                  prefixIcon: Icon(Icons.search),
                ),
                onSubmitted: (_) => onSearch(),
              ),
            ),
            OutlinedButton.icon(
              onPressed: onSearch,
              icon: const Icon(Icons.tune_outlined),
              label: const Text('Apply'),
            ),
            FilledButton.icon(
              onPressed: onAdd,
              icon: const Icon(Icons.add),
              label: const Text('Add'),
            ),
            PopupMenuButton<String>(
              tooltip: 'Export',
              icon: const Icon(Icons.download_outlined),
              itemBuilder: (context) => const [
                PopupMenuItem(value: 'excel', child: Text('Export Excel')),
                PopupMenuItem(value: 'csv', child: Text('Export CSV')),
                PopupMenuItem(value: 'pdf', child: Text('Export PDF')),
              ],
              onSelected: (format) => _runAction(
                context,
                ref
                    .read(moduleRecordsRepositoryProvider)
                    .export(
                      moduleId: moduleId,
                      recordType: recordType,
                      format: format,
                      query: searchController.text.trim(),
                    ),
                'Export downloaded.',
              ),
            ),
            OutlinedButton.icon(
              onPressed: () => _runAction(
                context,
                ref
                    .read(moduleRecordsRepositoryProvider)
                    .template(moduleId: moduleId, recordType: recordType),
                'Template downloaded.',
              ),
              icon: const Icon(Icons.table_view_outlined),
              label: const Text('Template'),
            ),
            PopupMenuButton<String>(
              tooltip: 'Import',
              icon: const Icon(Icons.upload_file_outlined),
              itemBuilder: (context) => const [
                PopupMenuItem(value: 'excel', child: Text('Import Excel')),
                PopupMenuItem(value: 'csv', child: Text('Import CSV')),
              ],
              onSelected: (format) => _runPickedModuleImport(
                context,
                ref,
                moduleId,
                recordType,
                format,
              ),
            ),
            OutlinedButton.icon(
              onPressed: onRefresh,
              icon: const Icon(Icons.refresh),
              label: const Text('Refresh'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ModuleRecordList extends ConsumerWidget {
  const _ModuleRecordList({
    required this.moduleId,
    required this.recordType,
    required this.records,
    required this.onChanged,
  });

  final String moduleId;
  final String recordType;
  final List<ModuleRecordModel> records;
  final VoidCallback onChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (records.isEmpty) {
      return Center(child: Text('No ${_titleCase(recordType)} records found.'));
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 1180 ? 2 : 1;
        return GridView.builder(
          padding: const EdgeInsets.all(24),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: columns,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            mainAxisExtent: 156,
          ),
          itemCount: records.length,
          itemBuilder: (context, index) {
            final record = records[index];
            return _ModuleRecordCard(
              record: record,
              onEdit: () => _showRecordDialog(
                context,
                moduleId: moduleId,
                recordType: recordType,
                record: record,
                onChanged: onChanged,
              ),
              onDelete: () async {
                final confirmed = await _confirm(
                  context,
                  'Delete ${record.name}?',
                );
                if (!confirmed || !context.mounted) {
                  return;
                }
                await _runAction(
                  context,
                  ref
                      .read(moduleRecordsRepositoryProvider)
                      .delete(
                        moduleId: moduleId,
                        recordType: recordType,
                        id: record.id,
                      ),
                  'Record deleted.',
                );
                onChanged();
              },
            );
          },
        );
      },
    );
  }
}

class _ModuleRecordCard extends StatelessWidget {
  const _ModuleRecordCard({
    required this.record,
    required this.onEdit,
    required this.onDelete,
  });

  final ModuleRecordModel record;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            SizedBox.square(
              dimension: 44,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: theme.colorScheme.primaryContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(
                  Icons.inventory_2_outlined,
                  color: theme.colorScheme.onPrimaryContainer,
                ),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          record.name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ),
                      _StatusChip(status: record.status),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    record.code,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: theme.colorScheme.primary,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    record.description?.trim().isNotEmpty ?? false
                        ? record.description!
                        : 'No description',
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.outline,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    [
                      if (record.recordDate != null)
                        _dateLabel(record.recordDate!),
                      if (record.amount != null) 'Amount ${record.amount}',
                      record.active ? 'Active' : 'Inactive',
                    ].join('  |  '),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.labelSmall,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 10),
            Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconButton.outlined(
                  tooltip: 'Edit',
                  onPressed: onEdit,
                  icon: const Icon(Icons.edit_outlined),
                ),
                const SizedBox(height: 8),
                IconButton.outlined(
                  tooltip: 'Delete',
                  onPressed: onDelete,
                  icon: const Icon(Icons.delete_outline),
                ),
              ],
            ),
          ],
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
    final isActive = status.toUpperCase() == 'ACTIVE';
    final color = isActive ? const Color(0xFF16A34A) : const Color(0xFF64748B);
    return Container(
      height: 26,
      padding: const EdgeInsets.symmetric(horizontal: 9),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(13),
      ),
      alignment: Alignment.center,
      child: Text(
        _titleCase(status),
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: color,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

Future<void> _showRecordDialog(
  BuildContext context, {
  required String moduleId,
  required String recordType,
  ModuleRecordModel? record,
  VoidCallback? onChanged,
}) {
  return showDialog<void>(
    context: context,
    builder: (context) {
      return _RecordFormDialog(
        moduleId: moduleId,
        recordType: recordType,
        record: record,
        onChanged: onChanged,
      );
    },
  );
}

class _RecordFormDialog extends ConsumerStatefulWidget {
  const _RecordFormDialog({
    required this.moduleId,
    required this.recordType,
    this.record,
    this.onChanged,
  });

  final String moduleId;
  final String recordType;
  final ModuleRecordModel? record;
  final VoidCallback? onChanged;

  @override
  ConsumerState<_RecordFormDialog> createState() => _RecordFormDialogState();
}

class _RecordFormDialogState extends ConsumerState<_RecordFormDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _code;
  late final TextEditingController _name;
  late final TextEditingController _description;
  late final TextEditingController _status;
  late final TextEditingController _recordDate;
  late final TextEditingController _amount;
  late final TextEditingController _metadata;
  late bool _active;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    final record = widget.record;
    _code = TextEditingController(text: record?.code ?? '');
    _name = TextEditingController(text: record?.name ?? '');
    _description = TextEditingController(text: record?.description ?? '');
    _status = TextEditingController(text: record?.status ?? 'ACTIVE');
    _recordDate = TextEditingController(
      text: record?.recordDate == null ? '' : _dateLabel(record!.recordDate!),
    );
    _amount = TextEditingController(text: record?.amount?.toString() ?? '');
    _metadata = TextEditingController(text: record?.metadataJson ?? '');
    _active = record?.active ?? true;
  }

  @override
  void dispose() {
    _code.dispose();
    _name.dispose();
    _description.dispose();
    _status.dispose();
    _recordDate.dispose();
    _amount.dispose();
    _metadata.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.record == null ? 'Add record' : 'Edit record'),
      content: SizedBox(
        width: 720,
        child: SingleChildScrollView(
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _TwoColumnFields(
                  children: [
                    TextFormField(
                      controller: _code,
                      decoration: const InputDecoration(labelText: 'Code'),
                      validator: _required,
                    ),
                    TextFormField(
                      controller: _name,
                      decoration: const InputDecoration(labelText: 'Name'),
                      validator: _required,
                    ),
                    TextFormField(
                      controller: _status,
                      decoration: const InputDecoration(labelText: 'Status'),
                      validator: _required,
                    ),
                    TextFormField(
                      controller: _recordDate,
                      decoration: const InputDecoration(
                        labelText: 'Record date',
                        hintText: 'yyyy-MM-dd',
                      ),
                    ),
                    TextFormField(
                      controller: _amount,
                      decoration: const InputDecoration(labelText: 'Amount'),
                      keyboardType: TextInputType.number,
                    ),
                    SwitchListTile(
                      value: _active,
                      onChanged: (value) => setState(() => _active = value),
                      title: const Text('Active'),
                      contentPadding: EdgeInsets.zero,
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _description,
                  minLines: 2,
                  maxLines: 3,
                  decoration: const InputDecoration(labelText: 'Description'),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _metadata,
                  minLines: 3,
                  maxLines: 5,
                  decoration: const InputDecoration(
                    labelText: 'Metadata JSON',
                    hintText: '{"key":"value"}',
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _saving ? null : () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton.icon(
          onPressed: _saving ? null : _save,
          icon: _saving
              ? const SizedBox.square(
                  dimension: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.save_outlined),
          label: const Text('Save'),
        ),
      ],
    );
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    setState(() => _saving = true);
    final payload = moduleRecordPayload(
      code: _code.text,
      name: _name.text,
      description: _description.text,
      status: _status.text,
      recordDate: _recordDate.text,
      amount: _amount.text,
      metadataJson: _metadata.text,
      active: _active,
    );
    final repository = ref.read(moduleRecordsRepositoryProvider);
    final result = widget.record == null
        ? await repository.create(
            moduleId: widget.moduleId,
            recordType: widget.recordType,
            payload: payload,
          )
        : await repository.update(
            moduleId: widget.moduleId,
            recordType: widget.recordType,
            id: widget.record!.id,
            payload: payload,
          );

    if (!mounted) {
      return;
    }
    setState(() => _saving = false);
    result.when(
      success: (_) {
        _snack(context, 'Record saved.');
        widget.onChanged?.call();
        Navigator.of(context).pop();
      },
      failure: (failure) => _snack(context, failure.message),
    );
  }
}

class _TwoColumnFields extends StatelessWidget {
  const _TwoColumnFields({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        if (constraints.maxWidth < 620) {
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

List<String> _recordTypes(String moduleId) {
  return switch (moduleId) {
    'academic' => const [
      'years',
      'classes',
      'sections',
      'subjects',
      'class-subjects',
      'timetable-slots',
    ],
    'hostel' => const [
      'hostels',
      'blocks',
      'floors',
      'rooms',
      'beds',
      'allocations',
    ],
    'attendance' => const ['records', 'monthly-reports'],
    'exams' => const ['types', 'schedules', 'marks', 'results', 'grades'],
    'reports' => const ['definitions'],
    'notifications' => const ['templates', 'history'],
    'settings' => const ['school-profile', 'lookups', 'app-settings'],
    _ => const ['records'],
  };
}

String _titleCase(String value) {
  return value
      .replaceAll('_', ' ')
      .replaceAll('-', ' ')
      .toLowerCase()
      .split(' ')
      .where((word) => word.isNotEmpty)
      .map((word) => '${word[0].toUpperCase()}${word.substring(1)}')
      .join(' ');
}

String _dateLabel(DateTime value) {
  return value.toIso8601String().split('T').first;
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}

Future<void> _runAction(
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

Future<void> _runPickedModuleImport(
  BuildContext context,
  WidgetRef ref,
  String moduleId,
  String recordType,
  String format,
) async {
  final file = await pickUploadFile(
    accept: format == 'csv' ? '.csv,text/csv' : '.xlsx,.xls',
  );
  if (file == null || !context.mounted) {
    return;
  }
  final result = await ref
      .read(moduleRecordsRepositoryProvider)
      .importFile(
        moduleId: moduleId,
        recordType: recordType,
        format: format,
        bytes: file.bytes,
        filename: file.name,
      );
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      _snack(context, 'Import completed.');
      ref.invalidate(
        moduleRecordsProvider(
          ModuleRecordsQuery(
            moduleId: moduleId,
            recordType: recordType,
            query: '',
          ),
        ),
      );
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
