import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/network/page_payload.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_confirm_dialog.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../../core/widgets/app_page_layout.dart';
import '../../../../core/widgets/app_select_field.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../../auth/domain/entities/auth_user.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/communication_models.dart';
import '../../data/repositories/communications_repository_impl.dart';
import '../controllers/communications_providers.dart';

class CommunicationManagementPage extends ConsumerStatefulWidget {
  const CommunicationManagementPage({super.key});

  @override
  ConsumerState<CommunicationManagementPage> createState() =>
      _CommunicationManagementPageState();
}

class _CommunicationManagementPageState
    extends ConsumerState<CommunicationManagementPage> {
  CommunicationFilter _filter = const CommunicationFilter(size: 20);

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authControllerProvider).user;
    final canCreate = _hasAny(user, const ['COMMUNICATION_CREATE']);
    final canUpdate = _hasAny(user, const ['COMMUNICATION_UPDATE']);
    final canPublish = _hasAny(user, const ['COMMUNICATION_PUBLISH']);
    final records = ref.watch(communicationsPageProvider(_filter));

    return AdminShell(
      title: 'Communications',
      activeModuleId: 'communications',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          AppPageHeader(
            title: 'Communications',
            subtitle:
                'Create, publish, and review announcements, circulars, notice-board items, and events.',
            icon: Icons.campaign_outlined,
            actions: [
              if (canCreate)
                FilledButton.icon(
                  onPressed: () => _showCommunicationDialog(context, ref),
                  icon: const Icon(Icons.add),
                  label: const Text('Create'),
                ),
            ],
          ),
          const SizedBox(height: 18),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          'Communication history',
                          style: Theme.of(context).textTheme.titleLarge
                              ?.copyWith(fontWeight: FontWeight.w800),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    children: [
                      _select(
                        width: 220,
                        label: 'Type',
                        value: _filter.type,
                        items: const {
                          'ANNOUNCEMENT': 'Announcement',
                          'CIRCULAR': 'Circular',
                          'NOTICE': 'Notice board',
                          'EVENT': 'Event',
                        },
                        onChanged: (value) => setState(() {
                          _filter = _filter.copyWith(
                            type: value,
                            page: 0,
                            clearType: value == null,
                          );
                        }),
                      ),
                      _select(
                        width: 180,
                        label: 'Status',
                        value: _filter.status,
                        items: const {
                          'DRAFT': 'Draft',
                          'PUBLISHED': 'Published',
                          'ARCHIVED': 'Archived',
                        },
                        onChanged: (value) => setState(() {
                          _filter = _filter.copyWith(
                            status: value,
                            page: 0,
                            clearStatus: value == null,
                          );
                        }),
                      ),
                      IconButton.outlined(
                        tooltip: 'Refresh',
                        onPressed: () =>
                            ref.invalidate(communicationsPageProvider(_filter)),
                        icon: const Icon(Icons.refresh),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  records.when(
                    data: (page) => _CommunicationPageView(
                      page: page,
                      canUpdate: canUpdate,
                      canPublish: canPublish,
                      onPageChanged: (page) => setState(() {
                        _filter = _filter.copyWith(page: page);
                      }),
                    ),
                    error: (error, _) => AppErrorState(
                      message: _message(error),
                      onRetry: () =>
                          ref.invalidate(communicationsPageProvider(_filter)),
                    ),
                    loading: () =>
                        const AppLoadingState(label: 'Loading communications'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _CommunicationPageView extends ConsumerWidget {
  const _CommunicationPageView({
    required this.page,
    required this.canUpdate,
    required this.canPublish,
    required this.onPageChanged,
  });

  final PagePayload<CommunicationModel> page;
  final bool canUpdate;
  final bool canPublish;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (page.content.isEmpty) {
      return const _EmptyBox(message: 'No communications found.');
    }
    return Column(
      children: [
        LayoutBuilder(
          builder: (context, constraints) {
            final columns = constraints.maxWidth >= 1120 ? 2 : 1;
            return GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: columns,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                mainAxisExtent: 146,
              ),
              itemCount: page.content.length,
              itemBuilder: (context, index) {
                final communication = page.content[index];
                return _CommunicationCard(
                  communication: communication,
                  canUpdate: canUpdate,
                  canPublish: canPublish,
                );
              },
            );
          },
        ),
        const SizedBox(height: 12),
        _PaginationBar(
          page: page.page,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          onPageChanged: onPageChanged,
        ),
      ],
    );
  }
}

class _CommunicationCard extends ConsumerWidget {
  const _CommunicationCard({
    required this.communication,
    required this.canUpdate,
    required this.canPublish,
  });

  final CommunicationModel communication;
  final bool canUpdate;
  final bool canPublish;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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
                  _typeIcon(communication.type),
                  color: theme.colorScheme.onPrimaryContainer,
                ),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    communication.title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    communication.message,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.outline,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 6,
                    children: [
                      _SmallChip(label: _display(communication.type)),
                      _SmallChip(label: _display(communication.audienceType)),
                      _SmallChip(
                        label: '${communication.recipientCount} recipient(s)',
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                _StatusChip(status: communication.status),
                PopupMenuButton<String>(
                  tooltip: 'Communication actions',
                  icon: const Icon(Icons.more_vert),
                  onSelected: (value) {
                    if (value == 'edit') {
                      _showCommunicationDialog(
                        context,
                        ref,
                        communication: communication,
                      );
                    } else {
                      _runAction(context, ref, communication, value);
                    }
                  },
                  itemBuilder: (context) => [
                    if (canUpdate && communication.status != 'ARCHIVED')
                      const PopupMenuItem(value: 'edit', child: Text('Edit')),
                    if (canPublish && communication.status != 'PUBLISHED')
                      const PopupMenuItem(
                        value: 'publish',
                        child: Text('Publish'),
                      ),
                    if (canPublish && communication.status == 'PUBLISHED')
                      const PopupMenuItem(
                        value: 'unpublish',
                        child: Text('Unpublish'),
                      ),
                    if (canUpdate && communication.status != 'ARCHIVED')
                      const PopupMenuItem(
                        value: 'archive',
                        child: Text('Archive'),
                      ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

Future<void> _showCommunicationDialog(
  BuildContext context,
  WidgetRef ref, {
  CommunicationModel? communication,
}) async {
  final title = TextEditingController(text: communication?.title ?? '');
  final message = TextEditingController(text: communication?.message ?? '');
  final academicYearId = TextEditingController(
    text: communication?.academicYearId ?? '',
  );
  final classId = TextEditingController(text: communication?.classId ?? '');
  final sectionId = TextEditingController(text: communication?.sectionId ?? '');
  final publishAt = TextEditingController(
    text: _instantLabel(communication?.publishAt),
  );
  final expiryAt = TextEditingController(
    text: _instantLabel(communication?.expiryAt),
  );
  final eventStartAt = TextEditingController(
    text: _instantLabel(communication?.eventStartAt),
  );
  final eventEndAt = TextEditingController(
    text: _instantLabel(communication?.eventEndAt),
  );
  final location = TextEditingController(text: communication?.location ?? '');
  final formKey = GlobalKey<FormState>();
  var type = communication?.type ?? 'ANNOUNCEMENT';
  var audience = communication?.audienceType ?? 'ALL';
  var status = communication?.status == 'ARCHIVED'
      ? 'DRAFT'
      : communication?.status ?? 'DRAFT';
  var saving = false;

  await showDialog<void>(
    context: context,
    builder: (dialogContext) => StatefulBuilder(
      builder: (dialogContext, setDialogState) => AlertDialog(
        title: Text(
          communication == null ? 'Create communication' : 'Edit communication',
        ),
        content: Form(
          key: formKey,
          child: SizedBox(
            width: 740,
            child: SingleChildScrollView(
              child: Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _DialogSelect(
                    label: 'Type',
                    value: type,
                    items: const {
                      'ANNOUNCEMENT': 'Announcement',
                      'CIRCULAR': 'Circular',
                      'NOTICE': 'Notice board',
                      'EVENT': 'Event',
                    },
                    onChanged: saving
                        ? null
                        : (value) => setDialogState(() {
                            type = value ?? type;
                          }),
                  ),
                  _DialogSelect(
                    label: 'Audience',
                    value: audience,
                    items: const {
                      'ALL': 'All',
                      'STAFF': 'Staff',
                      'TEACHERS': 'Teachers',
                      'PARENTS': 'Parents',
                      'STUDENTS': 'Students',
                      'CLASS': 'Class',
                      'DIVISION': 'Division',
                    },
                    onChanged: saving
                        ? null
                        : (value) => setDialogState(() {
                            audience = value ?? audience;
                          }),
                  ),
                  _DialogSelect(
                    label: 'Status',
                    value: status,
                    items: const {'DRAFT': 'Draft', 'PUBLISHED': 'Published'},
                    onChanged: saving
                        ? null
                        : (value) => setDialogState(() {
                            status = value ?? status;
                          }),
                  ),
                  _DialogField(controller: title, label: 'Title'),
                  SizedBox(
                    width: 612,
                    child: TextFormField(
                      controller: message,
                      decoration: const InputDecoration(labelText: 'Message'),
                      minLines: 3,
                      maxLines: 5,
                      validator: _required,
                    ),
                  ),
                  _DialogField(
                    controller: academicYearId,
                    label: 'Academic year ID',
                    required: false,
                  ),
                  if (audience == 'CLASS' || audience == 'DIVISION')
                    _DialogField(
                      controller: classId,
                      label: 'Class ID',
                      validator: audience == 'CLASS' || audience == 'DIVISION'
                          ? _required
                          : null,
                    ),
                  if (audience == 'DIVISION')
                    _DialogField(
                      controller: sectionId,
                      label: 'Division ID',
                      validator: _required,
                    ),
                  _DialogField(
                    controller: publishAt,
                    label: 'Publish at',
                    required: false,
                    validator: _optionalInstant,
                  ),
                  _DialogField(
                    controller: expiryAt,
                    label: 'Expiry at',
                    required: false,
                    validator: _optionalInstant,
                  ),
                  if (type == 'EVENT') ...[
                    _DialogField(
                      controller: eventStartAt,
                      label: 'Event start',
                      validator: _requiredInstant,
                    ),
                    _DialogField(
                      controller: eventEndAt,
                      label: 'Event end',
                      validator: _requiredInstant,
                    ),
                    _DialogField(
                      controller: location,
                      label: 'Location',
                      required: false,
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: saving ? null : () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: saving
                ? null
                : () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    final start = _instantOrNull(eventStartAt.text);
                    final end = _instantOrNull(eventEndAt.text);
                    if (type == 'EVENT' &&
                        start != null &&
                        end != null &&
                        end.isBefore(start)) {
                      _snack(
                        dialogContext,
                        'Event end cannot be before event start.',
                      );
                      return;
                    }
                    setDialogState(() => saving = true);
                    final payload = {
                      'type': type,
                      'title': title.text.trim(),
                      'message': message.text.trim(),
                      'audienceType': audience,
                      'academicYearId': _blankToNull(academicYearId.text),
                      'classId': audience == 'CLASS' || audience == 'DIVISION'
                          ? _blankToNull(classId.text)
                          : null,
                      'sectionId': audience == 'DIVISION'
                          ? _blankToNull(sectionId.text)
                          : null,
                      'publishAt': _instantPayload(publishAt.text),
                      'expiryAt': _instantPayload(expiryAt.text),
                      'eventStartAt': type == 'EVENT'
                          ? _instantPayload(eventStartAt.text)
                          : null,
                      'eventEndAt': type == 'EVENT'
                          ? _instantPayload(eventEndAt.text)
                          : null,
                      'location': type == 'EVENT'
                          ? _blankToNull(location.text)
                          : null,
                      'status': status,
                    };
                    final repository = ref.read(
                      communicationsRepositoryProvider,
                    );
                    final result = communication == null
                        ? await repository.create(payload)
                        : await repository.update(communication.id, payload);
                    if (!dialogContext.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(communicationsPageProvider);
                        _snack(context, 'Communication saved.');
                        Navigator.of(dialogContext).pop();
                      },
                      failure: (failure) {
                        setDialogState(() => saving = false);
                        _snack(dialogContext, failure.message);
                      },
                    );
                  },
            icon: saving
                ? const SizedBox.square(
                    dimension: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.save_outlined),
            label: const Text('Save'),
          ),
        ],
      ),
    ),
  );

  title.dispose();
  message.dispose();
  academicYearId.dispose();
  classId.dispose();
  sectionId.dispose();
  publishAt.dispose();
  expiryAt.dispose();
  eventStartAt.dispose();
  eventEndAt.dispose();
  location.dispose();
}

Future<void> _runAction(
  BuildContext context,
  WidgetRef ref,
  CommunicationModel communication,
  String action,
) async {
  final confirmed = await _confirm(
    context,
    '${_display(action)} ${communication.title}?',
  );
  if (!confirmed || !context.mounted) {
    return;
  }
  final repository = ref.read(communicationsRepositoryProvider);
  final result = switch (action) {
    'publish' => await repository.publish(communication.id),
    'unpublish' => await repository.unpublish(communication.id),
    _ => await repository.archive(communication.id),
  };
  if (!context.mounted) {
    return;
  }
  result.when(
    success: (_) {
      ref.invalidate(communicationsPageProvider);
      _snack(context, 'Communication ${action}ed.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

class _DialogField extends StatelessWidget {
  const _DialogField({
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
      width: 300,
      child: AppTextField(
        controller: controller,
        label: label,
        required: required,
        validator: validator ?? (required ? _required : null),
      ),
    );
  }
}

class _DialogSelect extends StatelessWidget {
  const _DialogSelect({
    required this.label,
    required this.value,
    required this.items,
    required this.onChanged,
  });

  final String label;
  final String? value;
  final Map<String, String> items;
  final ValueChanged<String?>? onChanged;

  @override
  Widget build(BuildContext context) {
    final validValue = items.containsKey(value) ? value : null;
    return SizedBox(
      width: 300,
      child: AppSelectField<String>(
        label: label,
        value: validValue,
        items: [
          for (final entry in items.entries)
            DropdownMenuItem(value: entry.key, child: Text(entry.value)),
        ],
        enabled: onChanged != null && items.isNotEmpty,
        onChanged: (value) => onChanged?.call(value),
      ),
    );
  }
}

class _SmallChip extends StatelessWidget {
  const _SmallChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 26,
      padding: const EdgeInsets.symmetric(horizontal: 9),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: const Color(0xFFF1F5F9),
        borderRadius: BorderRadius.circular(13),
      ),
      child: Text(
        label,
        overflow: TextOverflow.ellipsis,
        style: Theme.of(context).textTheme.labelSmall,
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final normalized = status.toUpperCase();
    final color = switch (normalized) {
      'PUBLISHED' => const Color(0xFF16A34A),
      'DRAFT' => const Color(0xFFF59E0B),
      'ARCHIVED' => const Color(0xFF64748B),
      _ => const Color(0xFF0891B2),
    };
    return AppStatusBadge(label: _display(status), color: color);
  }
}

class _EmptyBox extends StatelessWidget {
  const _EmptyBox({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return AppEmptyState(message: message, icon: Icons.campaign_outlined);
  }
}

class _PaginationBar extends StatelessWidget {
  const _PaginationBar({
    required this.page,
    required this.totalPages,
    required this.totalElements,
    required this.onPageChanged,
  });

  final int page;
  final int totalPages;
  final int totalElements;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Text('$totalElements records'),
        const Spacer(),
        IconButton(
          tooltip: 'Previous page',
          onPressed: page == 0 ? null : () => onPageChanged(page - 1),
          icon: const Icon(Icons.chevron_left),
        ),
        Text('Page ${totalPages == 0 ? 0 : page + 1} of $totalPages'),
        IconButton(
          tooltip: 'Next page',
          onPressed: totalPages == 0 || page >= totalPages - 1
              ? null
              : () => onPageChanged(page + 1),
          icon: const Icon(Icons.chevron_right),
        ),
      ],
    );
  }
}

Widget _select({
  required double width,
  required String label,
  required String? value,
  required Map<String, String> items,
  required ValueChanged<String?> onChanged,
}) {
  final validValue = items.containsKey(value) ? value : null;
  return SizedBox(
    width: width,
    child: DropdownButtonFormField<String>(
      initialValue: validValue,
      isExpanded: true,
      decoration: InputDecoration(labelText: label),
      items: [
        const DropdownMenuItem(value: '', child: Text('All')),
        for (final entry in items.entries)
          DropdownMenuItem(value: entry.key, child: Text(entry.value)),
      ],
      onChanged: (value) =>
          onChanged(value == null || value.isEmpty ? null : value),
    ),
  );
}

bool _hasAny(AuthUser? user, Iterable<String> permissions) {
  if (user == null) {
    return false;
  }
  if (user.hasRole('SUPER_ADMIN') || user.hasRole('ADMIN')) {
    return true;
  }
  if (user.permissions.isNotEmpty) {
    return user.hasAnyPermission(permissions);
  }
  if (permissions.contains('COMMUNICATION_READ')) {
    return user.hasRole('PRINCIPAL') ||
        user.hasRole('TEACHER') ||
        user.hasRole('PARENT') ||
        user.hasRole('STUDENT') ||
        user.hasRole('ACCOUNTANT');
  }
  return user.hasRole('PRINCIPAL');
}

Future<bool> _confirm(BuildContext context, String message) async {
  return showAppConfirmDialog(
    context: context,
    title: 'Delete communication?',
    message:
        '$message This removes the communication record from active management. Delivery history already created remains governed by backend rules.',
    confirmLabel: 'Delete',
    confirmIcon: Icons.delete_outline,
    destructive: true,
  );
}

IconData _typeIcon(String type) {
  return switch (type) {
    'CIRCULAR' => Icons.article_outlined,
    'NOTICE' => Icons.push_pin_outlined,
    'EVENT' => Icons.event_outlined,
    _ => Icons.campaign_outlined,
  };
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _requiredInstant(String? value) {
  return _required(value) ?? _optionalInstant(value);
}

String? _optionalInstant(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  return _instantOrNull(text) == null
      ? 'Use ISO time, for example 2026-09-01T09:00:00Z'
      : null;
}

String? _blankToNull(String? value) {
  if (value == null || value.trim().isEmpty) {
    return null;
  }
  return value.trim();
}

String? _instantPayload(String value) {
  final parsed = _instantOrNull(value);
  return parsed?.toUtc().toIso8601String();
}

DateTime? _instantOrNull(String value) {
  if (value.trim().isEmpty) {
    return null;
  }
  return DateTime.tryParse(value.trim());
}

String _instantLabel(DateTime? value) {
  return value == null ? '' : value.toUtc().toIso8601String();
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

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}
