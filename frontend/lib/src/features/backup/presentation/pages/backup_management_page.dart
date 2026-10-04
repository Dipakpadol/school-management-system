import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_confirm_dialog.dart';
import '../../../../core/widgets/app_page_layout.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../../auth/domain/entities/auth_user.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/backup_models.dart';
import '../../data/repositories/backup_repository_impl.dart';
import '../controllers/backup_providers.dart';

class BackupManagementPage extends ConsumerStatefulWidget {
  const BackupManagementPage({super.key});

  @override
  ConsumerState<BackupManagementPage> createState() =>
      _BackupManagementPageState();
}

class _BackupManagementPageState extends ConsumerState<BackupManagementPage> {
  final _keywordController = TextEditingController();
  BackupFilter _filter = const BackupFilter();
  int _restorePage = 0;
  bool _busy = false;

  @override
  void dispose() {
    _keywordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authControllerProvider).user;
    final canRead = _hasAny(user, const ['BACKUP_READ']);
    final canCreate = _hasAny(user, const ['BACKUP_CREATE']);
    final canDownload = _hasAny(user, const ['BACKUP_DOWNLOAD']);
    final canDelete = _hasAny(user, const ['BACKUP_DELETE']);
    final canRestore = _hasAny(user, const ['BACKUP_RESTORE']);
    final summary = ref.watch(backupSummaryProvider);
    final backups = ref.watch(backupRecordsProvider(_filter));
    final restores = ref.watch(restoreHistoryProvider(_restorePage));

    return AdminShell(
      title: 'Backup & Restore',
      activeModuleId: 'backup',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: canRead
          ? RefreshIndicator(
              onRefresh: _refresh,
              child: AppPageLayout(
                children: [
                  AppPageHeader(
                    title: 'Backup & Restore',
                    subtitle:
                        'Database backups, secure downloads, restore validation, and retention status.',
                    icon: Icons.backup_outlined,
                    actions: [
                      IconButton.outlined(
                        tooltip: 'Refresh',
                        onPressed: _busy ? null : _refresh,
                        icon: const Icon(Icons.refresh),
                      ),
                      if (canCreate)
                        FilledButton.icon(
                          onPressed: _busy ? null : _createBackup,
                          icon: _busy
                              ? const SizedBox.square(
                                  dimension: 16,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                  ),
                                )
                              : const Icon(Icons.add_circle_outline),
                          label: const Text('Create backup'),
                        ),
                    ],
                  ),
                  summary.when(
                    data: (item) => _SummaryPanel(summary: item),
                    error: (error, _) => _ErrorPanel(
                      message: _message(error),
                      onRetry: () => ref.invalidate(backupSummaryProvider),
                    ),
                    loading: () =>
                        const _LoadingPanel(label: 'Loading backup status'),
                  ),
                  _BackupFilterBar(
                    filter: _filter,
                    keywordController: _keywordController,
                    onChanged: (filter) => setState(() => _filter = filter),
                  ),
                  backups.when(
                    data: (page) => _BackupHistoryPanel(
                      page: page,
                      canDownload: canDownload,
                      canDelete: canDelete,
                      canRestore: canRestore,
                      busy: _busy,
                      onDownload: _downloadBackup,
                      onRestore: _restoreBackup,
                      onDelete: _deleteBackup,
                      onPageChanged: (page) => setState(
                        () => _filter = _filter.copyWith(page: page),
                      ),
                    ),
                    error: (error, _) => _ErrorPanel(
                      message: _message(error),
                      onRetry: () =>
                          ref.invalidate(backupRecordsProvider(_filter)),
                    ),
                    loading: () =>
                        const _LoadingPanel(label: 'Loading backups'),
                  ),
                  restores.when(
                    data: (page) => _RestoreHistoryPanel(
                      page: page,
                      onPageChanged: (page) =>
                          setState(() => _restorePage = page),
                    ),
                    error: (error, _) => _ErrorPanel(
                      message: _message(error),
                      onRetry: () =>
                          ref.invalidate(restoreHistoryProvider(_restorePage)),
                    ),
                    loading: () =>
                        const _LoadingPanel(label: 'Loading restore history'),
                  ),
                ],
              ),
            )
          : const Center(child: Text('You do not have permission.')),
    );
  }

  Future<void> _refresh() async {
    ref.invalidate(backupSummaryProvider);
    ref.invalidate(backupRecordsProvider);
    ref.invalidate(restoreHistoryProvider);
  }

  Future<void> _createBackup() async {
    final notes = await _showNotesDialog(
      context,
      title: 'Create database backup',
      label: 'Notes',
      confirmLabel: 'Create',
      confirmIcon: Icons.backup_outlined,
    );
    if (notes == null) {
      return;
    }
    await _run(
      () => ref.read(backupRepositoryProvider).createBackup(notes: notes),
      successMessage: 'Backup created.',
    );
  }

  Future<void> _downloadBackup(BackupRecordModel backup) async {
    await _run(
      () => ref.read(backupRepositoryProvider).downloadBackup(backup),
      successMessage: 'Backup downloaded.',
      refresh: false,
    );
  }

  Future<void> _restoreBackup(BackupRecordModel backup) async {
    final request = await _showRestoreDialog(context, backup);
    if (request == null) {
      return;
    }
    await _run(
      () => ref
          .read(backupRepositoryProvider)
          .restoreBackup(
            backup.id,
            confirmationText: request.confirmationText,
            notes: request.notes,
          ),
      successMessage: 'Restore completed.',
    );
  }

  Future<void> _deleteBackup(BackupRecordModel backup) async {
    final confirmed = await showAppConfirmDialog(
      context: context,
      title: 'Delete backup?',
      message:
          'This removes the backup archive and keeps the history row marked as deleted.',
      confirmLabel: 'Delete',
      confirmIcon: Icons.delete_outline,
      destructive: true,
    );
    if (!confirmed) {
      return;
    }
    await _run(
      () => ref.read(backupRepositoryProvider).deleteBackup(backup.id),
      successMessage: 'Backup deleted.',
    );
  }

  Future<void> _run<T>(
    Future<dynamic> Function() action, {
    required String successMessage,
    bool refresh = true,
  }) async {
    setState(() => _busy = true);
    final messenger = ScaffoldMessenger.of(context);
    final result = await action();
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        messenger.showSnackBar(SnackBar(content: Text(successMessage)));
        if (refresh) {
          _refresh();
        }
      },
      failure: (failure) {
        messenger.showSnackBar(SnackBar(content: Text(failure.message)));
      },
    );
    if (mounted) {
      setState(() => _busy = false);
    }
  }
}

class _SummaryPanel extends StatelessWidget {
  const _SummaryPanel({required this.summary});

  final BackupSummaryModel summary;

  @override
  Widget build(BuildContext context) {
    return AppStatGrid(
      minItemWidth: 210,
      children: [
        AppStatCard(
          label: 'Completed backups',
          value: _intLabel(summary.completedBackupCount),
          icon: Icons.task_alt_outlined,
          color: const Color(0xFF0F766E),
          subtitle: summary.backupAgeHours == null
              ? 'No completed backup'
              : '${summary.backupAgeHours}h since latest',
        ),
        AppStatCard(
          label: 'Stored size',
          value: _bytes(summary.totalStorageBytes),
          icon: Icons.storage_outlined,
          color: const Color(0xFF2563EB),
          subtitle:
              'Retain ${summary.retentionCount} / ${summary.retentionDays}d',
        ),
        AppStatCard(
          label: 'PostgreSQL tools',
          value: summary.pgDumpAvailable && summary.pgRestoreAvailable
              ? 'Ready'
              : 'Check',
          icon: Icons.terminal_outlined,
          color: summary.pgDumpAvailable && summary.pgRestoreAvailable
              ? const Color(0xFF16A34A)
              : const Color(0xFFB45309),
          subtitle: 'Flyway V${summary.latestDatabaseVersion}',
        ),
        AppStatCard(
          label: 'Scheduled backups',
          value: summary.scheduledBackupsEnabled ? 'Enabled' : 'Off',
          icon: Icons.schedule_outlined,
          color: summary.scheduledBackupsEnabled
              ? const Color(0xFF7C3AED)
              : const Color(0xFF64748B),
          subtitle:
              '${_display(summary.scheduleFrequency)} ${summary.scheduleTime}',
        ),
      ],
    );
  }
}

class _BackupFilterBar extends StatelessWidget {
  const _BackupFilterBar({
    required this.filter,
    required this.keywordController,
    required this.onChanged,
  });

  final BackupFilter filter;
  final TextEditingController keywordController;
  final ValueChanged<BackupFilter> onChanged;

  @override
  Widget build(BuildContext context) {
    return AppSectionCard(
      title: 'Backup history',
      trailing: IconButton.outlined(
        tooltip: 'Clear filters',
        onPressed: () {
          keywordController.clear();
          onChanged(const BackupFilter());
        },
        icon: const Icon(Icons.filter_alt_off_outlined),
      ),
      child: Wrap(
        spacing: 12,
        runSpacing: 12,
        children: [
          SizedBox(
            width: 220,
            child: DropdownButtonFormField<String>(
              initialValue: filter.status,
              isExpanded: true,
              decoration: const InputDecoration(labelText: 'Status'),
              items: const [
                DropdownMenuItem(value: '', child: Text('All')),
                DropdownMenuItem(value: 'COMPLETED', child: Text('Completed')),
                DropdownMenuItem(value: 'RESTORED', child: Text('Restored')),
                DropdownMenuItem(value: 'FAILED', child: Text('Failed')),
                DropdownMenuItem(value: 'DELETED', child: Text('Deleted')),
              ],
              onChanged: (value) => onChanged(
                filter.copyWith(
                  status: value == null || value.isEmpty ? null : value,
                  page: 0,
                  clearStatus: value == null || value.isEmpty,
                ),
              ),
            ),
          ),
          SizedBox(
            width: 320,
            child: AppTextField(
              controller: keywordController,
              label: 'Search',
              prefixIcon: Icons.search_outlined,
              suffixIcon: IconButton(
                tooltip: 'Apply search',
                onPressed: () => onChanged(
                  filter.copyWith(
                    keyword: keywordController.text,
                    page: 0,
                    clearKeyword: keywordController.text.trim().isEmpty,
                  ),
                ),
                icon: const Icon(Icons.arrow_forward),
              ),
              onSubmitted: (value) => onChanged(
                filter.copyWith(
                  keyword: value,
                  page: 0,
                  clearKeyword: value.trim().isEmpty,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _BackupHistoryPanel extends StatelessWidget {
  const _BackupHistoryPanel({
    required this.page,
    required this.canDownload,
    required this.canDelete,
    required this.canRestore,
    required this.busy,
    required this.onDownload,
    required this.onRestore,
    required this.onDelete,
    required this.onPageChanged,
  });

  final BackupRecordPage page;
  final bool canDownload;
  final bool canDelete;
  final bool canRestore;
  final bool busy;
  final ValueChanged<BackupRecordModel> onDownload;
  final ValueChanged<BackupRecordModel> onRestore;
  final ValueChanged<BackupRecordModel> onDelete;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context) {
    if (page.content.isEmpty) {
      return const AppEmptyState(
        message: 'No backups found.',
        icon: Icons.backup_outlined,
      );
    }
    return AppSectionCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: DataTable(
              columns: const [
                DataColumn(label: Text('File')),
                DataColumn(label: Text('Status')),
                DataColumn(label: Text('Started')),
                DataColumn(label: Text('Size')),
                DataColumn(label: Text('Version')),
                DataColumn(label: Text('Actions')),
              ],
              rows: [
                for (final backup in page.content)
                  DataRow(
                    cells: [
                      DataCell(
                        SizedBox(
                          width: 260,
                          child: Text(
                            backup.fileName,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ),
                      DataCell(_BackupStatusBadge(status: backup.status)),
                      DataCell(Text(_dateTime(backup.startedAt))),
                      DataCell(Text(_bytes(backup.sizeBytes ?? 0))),
                      DataCell(Text('V${backup.databaseVersion ?? '-'}')),
                      DataCell(
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            if (canDownload && _isDownloadable(backup))
                              IconButton(
                                tooltip: 'Download',
                                onPressed: busy
                                    ? null
                                    : () => onDownload(backup),
                                icon: const Icon(Icons.download_outlined),
                              ),
                            if (canRestore && _isDownloadable(backup))
                              IconButton(
                                tooltip: 'Restore',
                                onPressed: busy
                                    ? null
                                    : () => onRestore(backup),
                                icon: const Icon(Icons.restore_outlined),
                              ),
                            if (canDelete && backup.status != 'RUNNING')
                              IconButton(
                                tooltip: 'Delete',
                                onPressed: busy ? null : () => onDelete(backup),
                                icon: const Icon(Icons.delete_outline),
                              ),
                          ],
                        ),
                      ),
                    ],
                  ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          AppPaginationBar(
            page: page.page,
            totalPages: page.totalPages,
            onPrevious: page.page == 0
                ? null
                : () => onPageChanged(page.page - 1),
            onNext: page.totalPages == 0 || page.page >= page.totalPages - 1
                ? null
                : () => onPageChanged(page.page + 1),
          ),
        ],
      ),
    );
  }
}

class _RestoreHistoryPanel extends StatelessWidget {
  const _RestoreHistoryPanel({required this.page, required this.onPageChanged});

  final RestoreHistoryPage page;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context) {
    return AppSectionCard(
      title: 'Restore history',
      child: page.content.isEmpty
          ? const AppEmptyState(
              message: 'No restore attempts recorded.',
              icon: Icons.history_outlined,
            )
          : Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: DataTable(
                    columns: const [
                      DataColumn(label: Text('Backup')),
                      DataColumn(label: Text('Status')),
                      DataColumn(label: Text('Started')),
                      DataColumn(label: Text('Safety backup')),
                      DataColumn(label: Text('User')),
                    ],
                    rows: [
                      for (final restore in page.content)
                        DataRow(
                          cells: [
                            DataCell(
                              SizedBox(
                                width: 260,
                                child: Text(
                                  restore.backupFileName,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ),
                            DataCell(
                              _BackupStatusBadge(status: restore.status),
                            ),
                            DataCell(Text(_dateTime(restore.startedAt))),
                            DataCell(
                              SizedBox(
                                width: 240,
                                child: Text(
                                  restore.safetyBackupFileName ?? '-',
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ),
                            DataCell(Text(restore.initiatedBy ?? '-')),
                          ],
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                AppPaginationBar(
                  page: page.page,
                  totalPages: page.totalPages,
                  onPrevious: page.page == 0
                      ? null
                      : () => onPageChanged(page.page - 1),
                  onNext:
                      page.totalPages == 0 || page.page >= page.totalPages - 1
                      ? null
                      : () => onPageChanged(page.page + 1),
                ),
              ],
            ),
    );
  }
}

class _BackupStatusBadge extends StatelessWidget {
  const _BackupStatusBadge({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    return AppStatusBadge.status(status);
  }
}

class _LoadingPanel extends StatelessWidget {
  const _LoadingPanel({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 120,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const CircularProgressIndicator(),
            const SizedBox(height: 12),
            Text(label),
          ],
        ),
      ),
    );
  }
}

class _ErrorPanel extends StatelessWidget {
  const _ErrorPanel({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return AppSectionCard(
      child: Row(
        children: [
          Expanded(child: Text(message)),
          OutlinedButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh),
            label: const Text('Retry'),
          ),
        ],
      ),
    );
  }
}

class _RestoreDialogResult {
  const _RestoreDialogResult({required this.confirmationText, this.notes});

  final String confirmationText;
  final String? notes;
}

Future<String?> _showNotesDialog(
  BuildContext context, {
  required String title,
  required String label,
  required String confirmLabel,
  required IconData confirmIcon,
}) async {
  final controller = TextEditingController();
  final result = await showDialog<String>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: Text(title),
      content: SizedBox(
        width: 420,
        child: AppTextField(controller: controller, label: label, maxLines: 3),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton.icon(
          onPressed: () => Navigator.of(dialogContext).pop(controller.text),
          icon: Icon(confirmIcon),
          label: Text(confirmLabel),
        ),
      ],
    ),
  );
  controller.dispose();
  return result;
}

Future<_RestoreDialogResult?> _showRestoreDialog(
  BuildContext context,
  BackupRecordModel backup,
) async {
  final confirmation = TextEditingController();
  final notes = TextEditingController();
  final formKey = GlobalKey<FormState>();
  final result = await showDialog<_RestoreDialogResult>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: const Text('Restore database backup?'),
      content: SizedBox(
        width: 440,
        child: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                backup.fileName,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 16),
              AppTextField(
                controller: confirmation,
                label: 'Confirmation',
                required: true,
                validator: (value) =>
                    value == 'RESTORE' ? null : 'Type RESTORE',
              ),
              const SizedBox(height: 12),
              AppTextField(controller: notes, label: 'Notes', maxLines: 3),
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
          style: FilledButton.styleFrom(
            backgroundColor: Theme.of(context).colorScheme.error,
            foregroundColor: Theme.of(context).colorScheme.onError,
          ),
          onPressed: () {
            if (formKey.currentState?.validate() ?? false) {
              Navigator.of(dialogContext).pop(
                _RestoreDialogResult(
                  confirmationText: confirmation.text,
                  notes: notes.text,
                ),
              );
            }
          },
          icon: const Icon(Icons.restore_outlined),
          label: const Text('Restore'),
        ),
      ],
    ),
  );
  confirmation.dispose();
  notes.dispose();
  return result;
}

bool _hasAny(AuthUser? user, Iterable<String> permissions) {
  if (user == null) {
    return false;
  }
  if (user.hasRole('SUPER_ADMIN')) {
    return true;
  }
  if (user.permissions.isNotEmpty) {
    return user.hasAnyPermission(permissions);
  }
  if (permissions.contains('BACKUP_RESTORE')) {
    return false;
  }
  return user.hasRole('ADMIN') &&
      permissions.any(
        const {
          'BACKUP_READ',
          'BACKUP_CREATE',
          'BACKUP_DOWNLOAD',
          'BACKUP_DELETE',
        }.contains,
      );
}

bool _isDownloadable(BackupRecordModel backup) {
  return backup.status == 'COMPLETED' || backup.status == 'RESTORED';
}

String _bytes(int bytes) {
  if (bytes < 1024) {
    return '$bytes B';
  }
  final kb = bytes / 1024;
  if (kb < 1024) {
    return '${kb.toStringAsFixed(1)} KB';
  }
  final mb = kb / 1024;
  if (mb < 1024) {
    return '${mb.toStringAsFixed(1)} MB';
  }
  return '${(mb / 1024).toStringAsFixed(1)} GB';
}

String _dateTime(DateTime value) {
  final local = value.toLocal();
  final month = local.month.toString().padLeft(2, '0');
  final day = local.day.toString().padLeft(2, '0');
  final hour = local.hour.toString().padLeft(2, '0');
  final minute = local.minute.toString().padLeft(2, '0');
  return '${local.year}-$month-$day $hour:$minute';
}

String _display(String value) {
  return value
      .replaceAll('_', ' ')
      .toLowerCase()
      .split(' ')
      .where((part) => part.isNotEmpty)
      .map((part) => '${part[0].toUpperCase()}${part.substring(1)}')
      .join(' ');
}

String _intLabel(int value) {
  final raw = value.toString();
  final buffer = StringBuffer();
  for (var index = 0; index < raw.length; index++) {
    final remaining = raw.length - index;
    buffer.write(raw[index]);
    if (remaining > 1 && remaining % 3 == 1) {
      buffer.write(',');
    }
  }
  return buffer.toString();
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
