import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/result/result.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/audit_log_models.dart';
import '../../data/repositories/audit_logs_repository_impl.dart';
import '../controllers/audit_logs_providers.dart';

class AuditLogsPage extends ConsumerStatefulWidget {
  const AuditLogsPage({super.key});

  @override
  ConsumerState<AuditLogsPage> createState() => _AuditLogsPageState();
}

class _AuditLogsPageState extends ConsumerState<AuditLogsPage> {
  @override
  Widget build(BuildContext context) {
    final logs = ref.watch(auditLogsProvider);

    return AdminShell(
      title: 'Audit Logs',
      activeModuleId: 'audit-logs',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: Column(
        children: [
          const _AuditHeader(),
          const Divider(height: 1),
          Expanded(
            child: logs.when(
              data: (items) => _AuditLogList(logs: items),
              error: (error, _) => AppErrorState(
                message: _message(error),
                onRetry: () => ref.invalidate(auditLogsProvider),
              ),
              loading: () => const AppLoadingState(label: 'Loading audit logs'),
            ),
          ),
        ],
      ),
    );
  }
}

class _AuditHeader extends ConsumerWidget {
  const _AuditHeader();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final moduleName = ref.watch(auditModuleFilterProvider);
    final action = ref.watch(auditActionFilterProvider);
    final performedBy = ref.watch(auditUserFilterProvider);
    final fromDate = ref.watch(auditFromDateFilterProvider);
    final toDate = ref.watch(auditToDateFilterProvider);
    final moduleOptions = ref.watch(auditModuleOptionsProvider);
    final actionOptions = ref.watch(auditActionOptionsProvider);
    final userOptions = ref.watch(auditUserOptionsProvider);

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
              width: 210,
              child: _FilterDropdown(
                label: 'Module',
                icon: Icons.apps_outlined,
                value: moduleName,
                options: moduleOptions,
                onChanged: (value) => ref
                    .read(auditModuleFilterProvider.notifier)
                    .set(value ?? ''),
              ),
            ),
            SizedBox(
              width: 210,
              child: _FilterDropdown(
                label: 'Action',
                icon: Icons.bolt_outlined,
                value: action,
                options: actionOptions,
                onChanged: (value) => ref
                    .read(auditActionFilterProvider.notifier)
                    .set(value ?? ''),
              ),
            ),
            SizedBox(
              width: 240,
              child: _FilterDropdown(
                label: 'Performed by',
                icon: Icons.person_search_outlined,
                value: performedBy,
                options: userOptions,
                onChanged: (value) =>
                    ref.read(auditUserFilterProvider.notifier).set(value ?? ''),
              ),
            ),
            OutlinedButton.icon(
              onPressed: () => _pickDateRange(context, ref),
              icon: const Icon(Icons.date_range_outlined),
              label: Text(
                fromDate.isEmpty || toDate.isEmpty
                    ? 'Date range'
                    : '$fromDate to $toDate',
              ),
            ),
            OutlinedButton.icon(
              onPressed: () {
                ref.read(auditModuleFilterProvider.notifier).set('');
                ref.read(auditActionFilterProvider.notifier).set('');
                ref.read(auditUserFilterProvider.notifier).set('');
                ref.read(auditFromDateFilterProvider.notifier).set('');
                ref.read(auditToDateFilterProvider.notifier).set('');
              },
              icon: const Icon(Icons.clear),
              label: const Text('Reset filters'),
            ),
            OutlinedButton.icon(
              onPressed: () => ref.invalidate(auditLogsProvider),
              icon: const Icon(Icons.refresh),
              label: const Text('Refresh'),
            ),
            PopupMenuButton<String>(
              tooltip: 'Export audit logs',
              icon: const Icon(Icons.download_outlined),
              onSelected: (format) {
                final repository = ref.read(auditLogsRepositoryProvider);
                final download = switch (format) {
                  'excel' => repository.exportExcel(
                    moduleName: moduleName,
                    action: action,
                    performedBy: performedBy,
                    fromDate: fromDate,
                    toDate: toDate,
                  ),
                  'csv' => repository.exportCsv(
                    moduleName: moduleName,
                    action: action,
                    performedBy: performedBy,
                    fromDate: fromDate,
                    toDate: toDate,
                  ),
                  _ => repository.exportPdf(
                    moduleName: moduleName,
                    action: action,
                    performedBy: performedBy,
                    fromDate: fromDate,
                    toDate: toDate,
                  ),
                };
                _runAuditDownload(context, download);
              },
              itemBuilder: (context) => const [
                PopupMenuItem(value: 'excel', child: Text('Export Excel')),
                PopupMenuItem(value: 'csv', child: Text('Export CSV')),
                PopupMenuItem(value: 'pdf', child: Text('Export PDF')),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _FilterDropdown extends StatelessWidget {
  const _FilterDropdown({
    required this.label,
    required this.icon,
    required this.value,
    required this.options,
    required this.onChanged,
  });

  final String label;
  final IconData icon;
  final String value;
  final AsyncValue<List<String>> options;
  final ValueChanged<String?> onChanged;

  @override
  Widget build(BuildContext context) {
    final items = options.maybeWhen(data: (data) => data, orElse: () => const []);
    final selected = value.isEmpty || !items.contains(value) ? null : value;
    return DropdownButtonFormField<String>(
      initialValue: selected,
      isExpanded: true,
      decoration: InputDecoration(labelText: label, prefixIcon: Icon(icon)),
      items: [
        const DropdownMenuItem(value: '', child: Text('All')),
        for (final item in items)
          DropdownMenuItem(value: item, child: Text(item)),
      ],
      onChanged: onChanged,
    );
  }
}

Future<void> _pickDateRange(BuildContext context, WidgetRef ref) async {
  final now = DateTime.now();
  final range = await showDateRangePicker(
    context: context,
    firstDate: DateTime(now.year - 5),
    lastDate: DateTime(now.year + 1, 12, 31),
  );
  if (range == null) {
    return;
  }
  ref
      .read(auditFromDateFilterProvider.notifier)
      .set(_dateOnlyLabel(range.start));
  ref.read(auditToDateFilterProvider.notifier).set(_dateOnlyLabel(range.end));
}

class _AuditLogList extends StatelessWidget {
  const _AuditLogList({required this.logs});

  final List<AuditLogModel> logs;

  @override
  Widget build(BuildContext context) {
    if (logs.isEmpty) {
      return const Center(child: Text('No audit logs found.'));
    }

    return ListView.separated(
      padding: const EdgeInsets.all(24),
      itemBuilder: (context, index) {
        final log = logs[index];
        return _AuditLogTile(log: log);
      },
      itemCount: logs.length,
      separatorBuilder: (context, index) => const SizedBox(height: 10),
    );
  }
}

class _AuditLogTile extends StatelessWidget {
  const _AuditLogTile({required this.log});

  final AuditLogModel log;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => _showAuditDetail(context, log),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              SizedBox.square(
                dimension: 44,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: theme.colorScheme.secondaryContainer,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(
                    Icons.manage_search_outlined,
                    color: theme.colorScheme.onSecondaryContainer,
                  ),
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      spacing: 8,
                      runSpacing: 6,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        Text(
                          '${log.moduleName} / ${log.action}',
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        _AuditChip(label: log.entityName),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      '${log.performedBy} - ${_dateTimeLabel(log.performedAt)}',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.outline,
                      ),
                    ),
                    if (log.entityId != null) ...[
                      const SizedBox(height: 4),
                      Text(
                        log.entityId!,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.labelSmall,
                      ),
                    ],
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

class _AuditChip extends StatelessWidget {
  const _AuditChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 26,
      padding: const EdgeInsets.symmetric(horizontal: 9),
      decoration: BoxDecoration(
        color: const Color(0xFFF1F5F9),
        borderRadius: BorderRadius.circular(13),
      ),
      alignment: Alignment.center,
      child: Text(label, style: Theme.of(context).textTheme.labelSmall),
    );
  }
}

void _showAuditDetail(BuildContext context, AuditLogModel log) {
  showDialog<void>(
    context: context,
    builder: (context) {
      return AlertDialog(
        title: Text('${log.moduleName} / ${log.action}'),
        content: SizedBox(
          width: 720,
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                _DetailRow(label: 'Entity', value: log.entityName),
                _DetailRow(label: 'Entity ID', value: log.entityId ?? '-'),
                _DetailRow(label: 'Performed by', value: log.performedBy),
                _DetailRow(
                  label: 'Performed at',
                  value: _dateTimeLabel(log.performedAt),
                ),
                _DetailRow(label: 'IP address', value: log.ipAddress ?? '-'),
                const SizedBox(height: 12),
                _JsonBlock(title: 'Old value', value: log.oldValue),
                const SizedBox(height: 12),
                _JsonBlock(title: 'New value', value: log.newValue),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Close'),
          ),
        ],
      );
    },
  );
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 120,
            child: Text(
              label,
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                color: Theme.of(context).colorScheme.outline,
              ),
            ),
          ),
          Expanded(child: SelectableText(value)),
        ],
      ),
    );
  }
}

class _JsonBlock extends StatelessWidget {
  const _JsonBlock({required this.title, required this.value});

  final String title;
  final String? value;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: Theme.of(
            context,
          ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 6),
        DecoratedBox(
          decoration: BoxDecoration(
            color: const Color(0xFFF8FAFC),
            border: Border.all(color: const Color(0xFFE2E8F0)),
            borderRadius: BorderRadius.circular(8),
          ),
          child: SizedBox(
            width: double.infinity,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: SelectableText(
                value?.trim().isNotEmpty ?? false
                    ? value!
                    : 'No value recorded.',
              ),
            ),
          ),
        ),
      ],
    );
  }
}

String _dateTimeLabel(DateTime value) {
  final local = value.toLocal();
  final date = local.toIso8601String().split('T').first;
  final time = local.toIso8601String().split('T').last.substring(0, 8);
  return '$date $time';
}

String _dateOnlyLabel(DateTime value) {
  return value.toIso8601String().split('T').first;
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}

Future<void> _runAuditDownload(
  BuildContext context,
  Future<Result<void>> action,
) async {
  final result = await action;
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) => _snack(context, 'Audit log export downloaded.'),
    failure: (failure) => _snack(context, failure.message),
  );
}
