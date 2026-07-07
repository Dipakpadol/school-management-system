import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/settings_models.dart';
import '../../data/repositories/settings_repository_impl.dart';
import '../controllers/settings_providers.dart';

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(appSettingsProvider);

    return AdminShell(
      title: 'Settings',
      activeModuleId: 'settings',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: settings.when(
        data: (value) => _SettingsContent(settings: value),
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(appSettingsProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading settings'),
      ),
    );
  }
}

class _SettingsContent extends StatelessWidget {
  const _SettingsContent({required this.settings});

  final ApplicationSettingsModel settings;

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: _groups.length,
      child: Column(
        children: [
          const Material(
            color: Colors.white,
            child: TabBar(
              isScrollable: true,
              tabs: [
                Tab(icon: Icon(Icons.school_outlined), text: 'School Profile'),
                Tab(icon: Icon(Icons.calendar_month_outlined), text: 'Academic'),
                Tab(icon: Icon(Icons.payments_outlined), text: 'Fees'),
                Tab(
                  icon: Icon(Icons.notifications_active_outlined),
                  text: 'Notifications',
                ),
                Tab(icon: Icon(Icons.settings_outlined), text: 'System'),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: TabBarView(
              children: [
                for (final group in _groups)
                  _SettingsGroupEditor(
                    group: group,
                    initialSettings: settings,
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SettingsGroupEditor extends ConsumerStatefulWidget {
  const _SettingsGroupEditor({
    required this.group,
    required this.initialSettings,
  });

  final _SettingsGroup group;
  final ApplicationSettingsModel initialSettings;

  @override
  ConsumerState<_SettingsGroupEditor> createState() =>
      _SettingsGroupEditorState();
}

class _SettingsGroupEditorState extends ConsumerState<_SettingsGroupEditor> {
  late ApplicationSettingsModel _draft;
  bool _editing = false;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _draft = widget.initialSettings;
  }

  @override
  void didUpdateWidget(covariant _SettingsGroupEditor oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!_editing && oldWidget.initialSettings != widget.initialSettings) {
      _draft = widget.initialSettings;
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(widget.group.icon),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        widget.group.title,
                        style: Theme.of(context)
                            .textTheme
                            .titleLarge
                            ?.copyWith(fontWeight: FontWeight.w800),
                      ),
                    ),
                    if (_editing) ...[
                      TextButton(
                        onPressed: _saving ? null : _cancel,
                        child: const Text('Cancel'),
                      ),
                      const SizedBox(width: 8),
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
                    ] else
                      OutlinedButton.icon(
                        onPressed: () => setState(() => _editing = true),
                        icon: const Icon(Icons.edit_outlined),
                        label: const Text('Edit'),
                      ),
                  ],
                ),
                const SizedBox(height: 16),
                LayoutBuilder(
                  builder: (context, constraints) {
                    final columns = constraints.maxWidth >= 820 ? 2 : 1;
                    return GridView.count(
                      crossAxisCount: columns,
                      crossAxisSpacing: 12,
                      mainAxisSpacing: 12,
                      childAspectRatio: columns == 1 ? 5 : 4.4,
                      physics: const NeverScrollableScrollPhysics(),
                      shrinkWrap: true,
                      children: [
                        for (final field in widget.group.fields)
                          _SettingField(
                            field: field,
                            enabled: _editing && !_saving,
                            value: _draft.value(widget.group.id, field.key),
                            onChanged: (value) {
                              setState(() {
                                _draft = _draft.copyWithValue(
                                  widget.group.id,
                                  field.key,
                                  value,
                                );
                              });
                            },
                          ),
                      ],
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  void _cancel() {
    setState(() {
      _draft = widget.initialSettings;
      _editing = false;
    });
  }

  Future<void> _save() async {
    setState(() => _saving = true);
    final result = await ref
        .read(settingsRepositoryProvider)
        .updateAppSettings(_draft);
    if (!mounted) {
      return;
    }
    setState(() => _saving = false);
    result.when(
      success: (_) {
        ref.invalidate(appSettingsProvider);
        setState(() => _editing = false);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Settings saved.')),
        );
      },
      failure: (failure) => ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(failure.message)),
      ),
    );
  }
}

class _SettingField extends StatelessWidget {
  const _SettingField({
    required this.field,
    required this.value,
    required this.enabled,
    required this.onChanged,
  });

  final _SettingsField field;
  final String value;
  final bool enabled;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    if (field.kind == _FieldKind.boolean) {
      return SwitchListTile(
        value: value.toLowerCase() == 'true',
        onChanged: enabled ? (next) => onChanged(next.toString()) : null,
        title: Text(field.label),
        contentPadding: const EdgeInsets.symmetric(horizontal: 8),
      );
    }
    if (field.options.isNotEmpty) {
      final current = field.options.contains(value) ? value : field.options.first;
      return DropdownButtonFormField<String>(
        initialValue: current,
        decoration: InputDecoration(
          labelText: field.label,
          prefixIcon: Icon(field.icon),
        ),
        items: [
          for (final option in field.options)
            DropdownMenuItem(value: option, child: Text(option)),
        ],
        onChanged: enabled ? (next) => onChanged(next ?? current) : null,
      );
    }
    return TextFormField(
      initialValue: value,
      enabled: enabled,
      decoration: InputDecoration(
        labelText: field.label,
        prefixIcon: Icon(field.icon),
      ),
      keyboardType: field.kind == _FieldKind.number
          ? const TextInputType.numberWithOptions(decimal: true)
          : TextInputType.text,
      onChanged: onChanged,
    );
  }
}

enum _FieldKind { text, number, boolean }

class _SettingsGroup {
  const _SettingsGroup({
    required this.id,
    required this.title,
    required this.icon,
    required this.fields,
  });

  final String id;
  final String title;
  final IconData icon;
  final List<_SettingsField> fields;
}

class _SettingsField {
  const _SettingsField({
    required this.key,
    required this.label,
    required this.icon,
    this.kind = _FieldKind.text,
    this.options = const [],
  });

  final String key;
  final String label;
  final IconData icon;
  final _FieldKind kind;
  final List<String> options;
}

const _groups = [
  _SettingsGroup(
    id: 'schoolProfile',
    title: 'School Profile Settings',
    icon: Icons.school_outlined,
    fields: [
      _SettingsField(key: 'schoolName', label: 'School name', icon: Icons.school_outlined),
      _SettingsField(key: 'schoolCode', label: 'School code', icon: Icons.tag_outlined),
      _SettingsField(key: 'address', label: 'Address', icon: Icons.location_on_outlined),
      _SettingsField(key: 'contactNumber', label: 'Contact number', icon: Icons.call_outlined),
      _SettingsField(key: 'email', label: 'Email', icon: Icons.email_outlined),
      _SettingsField(key: 'logoUrl', label: 'Logo URL', icon: Icons.image_outlined),
      _SettingsField(key: 'principalName', label: 'Principal name', icon: Icons.person_outline),
      _SettingsField(key: 'affiliationBoard', label: 'Affiliation / board', icon: Icons.verified_outlined),
    ],
  ),
  _SettingsGroup(
    id: 'academic',
    title: 'Academic Settings',
    icon: Icons.calendar_month_outlined,
    fields: [
      _SettingsField(key: 'currentAcademicYearId', label: 'Current academic year', icon: Icons.calendar_today_outlined),
      _SettingsField(key: 'defaultAttendanceTime', label: 'Default attendance time', icon: Icons.schedule_outlined),
      _SettingsField(key: 'workingDays', label: 'Working days', icon: Icons.date_range_outlined),
      _SettingsField(key: 'holidayConfiguration', label: 'Holiday configuration', icon: Icons.event_busy_outlined),
    ],
  ),
  _SettingsGroup(
    id: 'fees',
    title: 'Fee Settings',
    icon: Icons.payments_outlined,
    fields: [
      _SettingsField(key: 'receiptPrefix', label: 'Receipt prefix', icon: Icons.receipt_outlined),
      _SettingsField(key: 'receiptNumberFormat', label: 'Receipt number format', icon: Icons.format_list_numbered_outlined),
      _SettingsField(key: 'lateFeeDefaultAmount', label: 'Late fee default', icon: Icons.warning_amber_outlined, kind: _FieldKind.number),
      _SettingsField(key: 'paymentModesEnabled', label: 'Payment modes enabled', icon: Icons.account_balance_wallet_outlined),
      _SettingsField(key: 'onlinePaymentEnabled', label: 'Online payment enabled', icon: Icons.public_outlined, kind: _FieldKind.boolean),
    ],
  ),
  _SettingsGroup(
    id: 'notifications',
    title: 'Notification Settings',
    icon: Icons.notifications_active_outlined,
    fields: [
      _SettingsField(key: 'emailEnabled', label: 'Email enabled', icon: Icons.email_outlined, kind: _FieldKind.boolean),
      _SettingsField(key: 'smsEnabled', label: 'SMS enabled', icon: Icons.sms_outlined, kind: _FieldKind.boolean),
      _SettingsField(key: 'whatsAppEnabled', label: 'WhatsApp enabled', icon: Icons.chat_outlined, kind: _FieldKind.boolean),
      _SettingsField(key: 'reminderDaysBeforeDueDate', label: 'Reminder days before due date', icon: Icons.alarm_outlined, kind: _FieldKind.number),
      _SettingsField(key: 'schedulerEnabled', label: 'Scheduler enabled', icon: Icons.update_outlined, kind: _FieldKind.boolean),
    ],
  ),
  _SettingsGroup(
    id: 'system',
    title: 'System Settings',
    icon: Icons.settings_outlined,
    fields: [
      _SettingsField(key: 'timezone', label: 'Timezone', icon: Icons.public_outlined),
      _SettingsField(key: 'dateFormat', label: 'Date format', icon: Icons.date_range_outlined, options: ['yyyy-MM-dd', 'dd-MM-yyyy', 'MM/dd/yyyy']),
      _SettingsField(key: 'language', label: 'Language', icon: Icons.language_outlined, options: ['en', 'hi']),
      _SettingsField(key: 'themePreference', label: 'Theme preference', icon: Icons.contrast_outlined, options: ['system', 'light', 'dark']),
      _SettingsField(key: 'maintenanceMode', label: 'Maintenance mode', icon: Icons.construction_outlined, kind: _FieldKind.boolean),
    ],
  ),
];

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}
