import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/notification_models.dart';
import '../../data/repositories/notifications_repository_impl.dart';
import '../controllers/notifications_providers.dart';

class NotificationManagementPage extends ConsumerStatefulWidget {
  const NotificationManagementPage({super.key});

  @override
  ConsumerState<NotificationManagementPage> createState() {
    return _NotificationManagementPageState();
  }
}

class _NotificationManagementPageState
    extends ConsumerState<NotificationManagementPage> {
  final _formKey = GlobalKey<FormState>();
  final _recipient = TextEditingController();
  final _subject = TextEditingController(text: 'Test Email');
  final _messageController = TextEditingController(
    text: 'This is a test notification from School ERP.',
  );
  String _channel = 'EMAIL';

  @override
  Widget build(BuildContext context) {
    return AdminShell(
      title: 'Notification Management',
      activeModuleId: 'notifications',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: DefaultTabController(
        length: 3,
        child: Column(
          children: [
            const Material(
              color: Colors.white,
              child: TabBar(
                isScrollable: true,
                tabs: [
                  Tab(icon: Icon(Icons.send_outlined), text: 'Send Test'),
                  Tab(icon: Icon(Icons.history_outlined), text: 'Logs'),
                  Tab(
                    icon: Icon(Icons.description_outlined),
                    text: 'Templates',
                  ),
                ],
              ),
            ),
            const Divider(height: 1),
            Expanded(
              child: TabBarView(
                children: [_sendTestTab(), _logsTab(), _templatesTab()],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _sendTestTab() {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Form(
          key: _formKey,
          child: Wrap(
            spacing: 12,
            runSpacing: 12,
            children: [
              SizedBox(
                width: 220,
                child: DropdownButtonFormField<String>(
                  initialValue: _channel,
                  decoration: const InputDecoration(labelText: 'Channel'),
                  items: const [
                    DropdownMenuItem(value: 'EMAIL', child: Text('Email')),
                    DropdownMenuItem(value: 'SMS', child: Text('SMS')),
                    DropdownMenuItem(
                      value: 'WHATSAPP',
                      child: Text('WhatsApp'),
                    ),
                  ],
                  onChanged: (value) => setState(() {
                    _channel = value ?? _channel;
                    if (_channel != 'EMAIL') {
                      _subject.text = '';
                    } else if (_subject.text.trim().isEmpty) {
                      _subject.text = 'Test Email';
                    }
                  }),
                ),
              ),
              SizedBox(
                width: 320,
                child: TextFormField(
                  controller: _recipient,
                  decoration: InputDecoration(
                    labelText: _channel == 'EMAIL' ? 'Email' : 'Mobile number',
                  ),
                  validator: _channel == 'EMAIL' ? _email : _mobile,
                ),
              ),
              if (_channel == 'EMAIL')
                SizedBox(
                  width: 320,
                  child: TextFormField(
                    controller: _subject,
                    decoration: const InputDecoration(labelText: 'Subject'),
                    validator: _required,
                  ),
                ),
              SizedBox(
                width: 652,
                child: TextFormField(
                  controller: _messageController,
                  decoration: const InputDecoration(labelText: 'Message'),
                  minLines: 4,
                  maxLines: 6,
                  validator: _required,
                ),
              ),
              SizedBox(
                width: 180,
                height: 56,
                child: FilledButton.icon(
                  onPressed: _sendTest,
                  icon: const Icon(Icons.send_outlined),
                  label: const Text('Send'),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _logsTab() {
    final logs = ref.watch(notificationLogsProvider);
    return Padding(
      padding: const EdgeInsets.all(16),
      child: logs.when(
        data: (items) {
          if (items.isEmpty) {
            return const _EmptyState(message: 'No notification logs found.');
          }
          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(notificationLogsProvider),
            child: ListView.separated(
              itemCount: items.length,
              separatorBuilder: (_, _) => const Divider(height: 1),
              itemBuilder: (context, index) {
                final item = items[index];
                return ListTile(
                  leading: Icon(_channelIcon(item.channel)),
                  title: Text('${item.channel} to ${item.recipient}'),
                  subtitle: Text(
                    [
                      item.message,
                      if (item.errorMessage != null) item.errorMessage!,
                      _dateLabel(item.sentAt ?? item.createdAt),
                    ].join(' - '),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  trailing: _StatusBadge(label: item.status),
                );
              },
            ),
          );
        },
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(notificationLogsProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading logs'),
      ),
    );
  }

  Widget _templatesTab() {
    final templates = ref.watch(notificationTemplatesProvider);
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          FilledButton.icon(
            onPressed: () => _showTemplateDialog(),
            icon: const Icon(Icons.add_outlined),
            label: const Text('Add template'),
          ),
          const SizedBox(height: 12),
          Expanded(
            child: templates.when(
              data: (items) {
                if (items.isEmpty) {
                  return const _EmptyState(
                    message: 'No notification templates found.',
                  );
                }
                return ListView.separated(
                  itemCount: items.length,
                  separatorBuilder: (_, _) => const Divider(height: 1),
                  itemBuilder: (context, index) {
                    final item = items[index];
                    return ListTile(
                      leading: Icon(_channelIcon(item.channel)),
                      title: Text(item.templateName),
                      subtitle: Text(
                        '${item.templateCode} - ${item.channel} - ${item.body}',
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                      trailing: Wrap(
                        spacing: 4,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        children: [
                          _StatusBadge(label: item.status),
                          IconButton(
                            tooltip: 'Edit',
                            onPressed: () => _showTemplateDialog(item),
                            icon: const Icon(Icons.edit_outlined),
                          ),
                          IconButton(
                            tooltip: 'Delete',
                            onPressed: () => _deleteTemplate(item),
                            icon: const Icon(Icons.delete_outline),
                          ),
                        ],
                      ),
                    );
                  },
                );
              },
              error: (error, _) => AppErrorState(
                message: _message(error),
                onRetry: () => ref.invalidate(notificationTemplatesProvider),
              ),
              loading: () => const AppLoadingState(label: 'Loading templates'),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _sendTest() async {
    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }
    final result = await ref
        .read(notificationsRepositoryProvider)
        .sendTest(
          channel: _channel,
          recipient: _recipient.text.trim(),
          subject: _subject.text.trim(),
          message: _messageController.text.trim(),
        );
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(notificationLogsProvider);
        _snack('Notification processed.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  Future<void> _showTemplateDialog([
    NotificationTemplateModel? template,
  ]) async {
    final formKey = GlobalKey<FormState>();
    final code = TextEditingController(text: template?.templateCode ?? '');
    final name = TextEditingController(text: template?.templateName ?? '');
    final subject = TextEditingController(text: template?.subject ?? '');
    final body = TextEditingController(text: template?.body ?? '');
    final variables = TextEditingController(text: template?.variables ?? '');
    var channel = template?.channel ?? 'EMAIL';
    var status = template?.status ?? 'ACTIVE';

    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: Text(template == null ? 'Add template' : 'Edit template'),
              content: Form(
                key: formKey,
                child: SizedBox(
                  width: 680,
                  child: SingleChildScrollView(
                    child: Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: [
                        _DialogField(
                          child: TextFormField(
                            controller: code,
                            decoration: const InputDecoration(
                              labelText: 'Template code',
                            ),
                            validator: _required,
                          ),
                        ),
                        _DialogField(
                          child: TextFormField(
                            controller: name,
                            decoration: const InputDecoration(
                              labelText: 'Template name',
                            ),
                            validator: _required,
                          ),
                        ),
                        _DialogField(
                          child: DropdownButtonFormField<String>(
                            initialValue: channel,
                            decoration: const InputDecoration(
                              labelText: 'Channel',
                            ),
                            items: const [
                              DropdownMenuItem(
                                value: 'EMAIL',
                                child: Text('Email'),
                              ),
                              DropdownMenuItem(
                                value: 'SMS',
                                child: Text('SMS'),
                              ),
                              DropdownMenuItem(
                                value: 'WHATSAPP',
                                child: Text('WhatsApp'),
                              ),
                            ],
                            onChanged: (value) => setDialogState(
                              () => channel = value ?? channel,
                            ),
                          ),
                        ),
                        _DialogField(
                          child: DropdownButtonFormField<String>(
                            initialValue: status,
                            decoration: const InputDecoration(
                              labelText: 'Status',
                            ),
                            items: const [
                              DropdownMenuItem(
                                value: 'ACTIVE',
                                child: Text('Active'),
                              ),
                              DropdownMenuItem(
                                value: 'INACTIVE',
                                child: Text('Inactive'),
                              ),
                            ],
                            onChanged: (value) =>
                                setDialogState(() => status = value ?? status),
                          ),
                        ),
                        SizedBox(
                          width: 652,
                          child: TextFormField(
                            controller: subject,
                            decoration: const InputDecoration(
                              labelText: 'Subject',
                            ),
                          ),
                        ),
                        SizedBox(
                          width: 652,
                          child: TextFormField(
                            controller: body,
                            decoration: const InputDecoration(
                              labelText: 'Body',
                            ),
                            minLines: 3,
                            maxLines: 5,
                            validator: _required,
                          ),
                        ),
                        SizedBox(
                          width: 652,
                          child: TextFormField(
                            controller: variables,
                            decoration: const InputDecoration(
                              labelText: 'Variables JSON',
                            ),
                            maxLines: 2,
                          ),
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
                FilledButton.icon(
                  onPressed: () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }
                    final payload = {
                      'templateCode': code.text.trim(),
                      'templateName': name.text.trim(),
                      'channel': channel,
                      'subject': _blankToNull(subject.text),
                      'body': body.text.trim(),
                      'variables': _blankToNull(variables.text),
                      'status': status,
                    };
                    final repository = ref.read(
                      notificationsRepositoryProvider,
                    );
                    final result = template == null
                        ? await repository.createTemplate(payload)
                        : await repository.updateTemplate(template.id, payload);
                    if (!context.mounted) {
                      return;
                    }
                    result.when(
                      success: (_) {
                        ref.invalidate(notificationTemplatesProvider);
                        _snack('Template saved.');
                        Navigator.of(context).pop();
                      },
                      failure: (failure) => _snack(failure.message),
                    );
                  },
                  icon: const Icon(Icons.save_outlined),
                  label: const Text('Save'),
                ),
              ],
            );
          },
        );
      },
    );

    code.dispose();
    name.dispose();
    subject.dispose();
    body.dispose();
    variables.dispose();
  }

  Future<void> _deleteTemplate(NotificationTemplateModel template) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete template'),
        content: Text(template.templateName),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton.icon(
            onPressed: () => Navigator.of(context).pop(true),
            icon: const Icon(Icons.delete_outline),
            label: const Text('Delete'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) {
      return;
    }
    final result = await ref
        .read(notificationsRepositoryProvider)
        .deleteTemplate(template.id);
    if (!mounted) {
      return;
    }
    result.when(
      success: (_) {
        ref.invalidate(notificationTemplatesProvider);
        _snack('Template deleted.');
      },
      failure: (failure) => _snack(failure.message),
    );
  }

  @override
  void dispose() {
    _recipient.dispose();
    _subject.dispose();
    _messageController.dispose();
    super.dispose();
  }

  void _snack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}

class _DialogField extends StatelessWidget {
  const _DialogField({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return SizedBox(width: 320, child: child);
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Chip(visualDensity: VisualDensity.compact, label: Text(label));
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Text(message, style: Theme.of(context).textTheme.bodyMedium),
    );
  }
}

IconData _channelIcon(String channel) {
  return switch (channel) {
    'SMS' => Icons.sms_outlined,
    'WHATSAPP' => Icons.chat_outlined,
    _ => Icons.email_outlined,
  };
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _email(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return 'Required';
  }
  final pattern = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
  return pattern.hasMatch(text) ? null : 'Enter a valid email';
}

String? _mobile(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return 'Required';
  }
  return RegExp(r'^\+?[0-9]{10,15}$').hasMatch(text)
      ? null
      : 'Enter 10 to 15 digits';
}

String? _blankToNull(String value) {
  return value.trim().isEmpty ? null : value.trim();
}

String _dateLabel(DateTime? value) {
  if (value == null) {
    return '-';
  }
  return value.toLocal().toIso8601String().split('T').first;
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
