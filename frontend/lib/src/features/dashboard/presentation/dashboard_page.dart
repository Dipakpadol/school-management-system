import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router/app_routes.dart';
import '../../../core/theme/app_design_system.dart';
import '../../../core/widgets/admin_shell.dart';
import '../../../core/widgets/app_error_state.dart';
import '../../../core/widgets/app_loading_state.dart';
import '../../../core/widgets/app_page_layout.dart';
import '../../auth/presentation/controllers/auth_controller.dart';
import '../domain/entities/dashboard_metric.dart';
import '../domain/entities/dashboard_overview.dart';
import '../domain/erp_module.dart';
import '../domain/menu_policy.dart';
import 'controllers/dashboard_controller.dart';
import 'controllers/menu_controller.dart';

class DashboardPage extends ConsumerWidget {
  const DashboardPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final overview = ref.watch(dashboardOverviewProvider);
    final user = ref.watch(authControllerProvider).user;
    final fallbackModules = visibleErpModules(user);
    final modules = ref
        .watch(currentMenuProvider)
        .maybeWhen(
          data: (items) => visibleErpModulesFromMenuIds(
            user,
            items.map((item) => item.moduleId),
          ),
          orElse: () => fallbackModules,
        );

    return AdminShell(
      title: 'Dashboard',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: overview.when(
        data: (data) => _DashboardContent(
          overview: data,
          modules: modules,
          onRefresh: () => ref.invalidate(dashboardOverviewProvider),
        ),
        error: (error, _) {
          final message = error.toString().replaceFirst('Exception: ', '');
          if (message == 'Session expired') {
            WidgetsBinding.instance.addPostFrameCallback((_) async {
              await ref.read(authControllerProvider.notifier).logout();
              if (context.mounted) {
                context.go(AppRoutes.login);
              }
            });
            return const AppLoadingState(label: 'Redirecting to login');
          }
          return AppErrorState(
            message: message,
            onRetry: () => ref.invalidate(dashboardOverviewProvider),
          );
        },
        loading: () => const AppLoadingState(label: 'Loading dashboard'),
      ),
    );
  }
}

class _DashboardContent extends StatelessWidget {
  const _DashboardContent({
    required this.overview,
    required this.modules,
    required this.onRefresh,
  });

  final DashboardOverview overview;
  final List<ErpModule> modules;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    return AppPageLayout(
      children: [
        AppPageHeader(
          title: 'School ERP Admin',
          subtitle:
              'Admissions, fees, attendance, hostel, reports, communication, and settings.',
          icon: Icons.dashboard_outlined,
          actions: [
            Tooltip(
              message: 'Refresh dashboard',
              child: IconButton.outlined(
                onPressed: onRefresh,
                icon: const Icon(Icons.refresh),
              ),
            ),
            _StatusPill(status: overview.systemStatus),
          ],
        ),
        _MetricGrid(metrics: overview.metrics),
        const _SectionIntro(
          title: 'Live activity',
          subtitle:
              'Recent actions, birthdays, and notifications from the canonical dashboard APIs.',
        ),
        _DashboardPanels(
          recentActivities: overview.recentActivities,
          notifications: overview.notifications,
          birthdaysToday: overview.birthdaysToday,
        ),
        const _SectionIntro(
          title: 'Module access',
          subtitle: 'Available modules for the signed-in role.',
        ),
        _ModuleGrid(modules: modules),
      ],
    );
  }
}

class _SectionIntro extends StatelessWidget {
  const _SectionIntro({required this.title, this.subtitle});

  final String title;
  final String? subtitle;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: theme.textTheme.titleLarge?.copyWith(
            color: AppDesignTokens.ink,
            fontWeight: FontWeight.w900,
          ),
        ),
        if (subtitle != null) ...[
          const SizedBox(height: 4),
          Text(
            subtitle!,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: AppDesignTokens.muted,
              height: 1.4,
            ),
          ),
        ],
      ],
    );
  }
}

class _DashboardPanels extends StatelessWidget {
  const _DashboardPanels({
    required this.recentActivities,
    required this.notifications,
    required this.birthdaysToday,
  });

  final List<DashboardActivity> recentActivities;
  final List<DashboardNotification> notifications;
  final List<DashboardBirthday> birthdaysToday;

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.sizeOf(context).width;
    final panels = [
      _ActivityPanel(items: recentActivities),
      _BirthdayPanel(items: birthdaysToday),
      _NotificationPanel(items: notifications),
    ];

    if (width >= 1180) {
      return Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (var index = 0; index < panels.length; index++) ...[
            Expanded(child: panels[index]),
            if (index != panels.length - 1) const SizedBox(width: 12),
          ],
        ],
      );
    }

    return Column(
      children: [
        for (var index = 0; index < panels.length; index++) ...[
          panels[index],
          if (index != panels.length - 1) const SizedBox(height: 12),
        ],
      ],
    );
  }
}

class _ActivityPanel extends StatelessWidget {
  const _ActivityPanel({required this.items});

  final List<DashboardActivity> items;

  @override
  Widget build(BuildContext context) {
    return _InfoPanel(
      title: 'Recent actions',
      icon: Icons.history,
      emptyMessage: 'No recent actions',
      children: items
          .map(
            (item) => _PanelTile(
              icon: Icons.bolt_outlined,
              title: '${_titleCase(item.action)} ${item.entityName}',
              subtitle: '${item.moduleName} by ${item.performedBy}',
              trailing: _formatDateTime(item.performedAt),
            ),
          )
          .toList(),
    );
  }
}

class _BirthdayPanel extends StatelessWidget {
  const _BirthdayPanel({required this.items});

  final List<DashboardBirthday> items;

  @override
  Widget build(BuildContext context) {
    return _InfoPanel(
      title: 'Birthdays today',
      icon: Icons.cake_outlined,
      emptyMessage: 'No birthdays today',
      children: items
          .map(
            (item) => _PanelTile(
              icon: Icons.person_outline,
              title: item.studentName,
              subtitle: _classSection(item),
              trailing: _formatDate(item.dateOfBirth),
            ),
          )
          .toList(),
    );
  }
}

class _NotificationPanel extends StatelessWidget {
  const _NotificationPanel({required this.items});

  final List<DashboardNotification> items;

  @override
  Widget build(BuildContext context) {
    return _InfoPanel(
      title: 'Notifications',
      icon: Icons.notifications_none,
      emptyMessage: 'No notifications',
      children: items
          .map(
            (item) => _PanelTile(
              icon: Icons.campaign_outlined,
              title: item.title,
              subtitle: item.message,
              trailing: _formatDateTime(item.createdAt),
            ),
          )
          .toList(),
    );
  }
}

class _InfoPanel extends StatelessWidget {
  const _InfoPanel({
    required this.title,
    required this.icon,
    required this.emptyMessage,
    required this.children,
  });

  final String title;
  final IconData icon;
  final String emptyMessage;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 20, color: theme.colorScheme.primary),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (children.isEmpty)
              SizedBox(
                height: 88,
                child: Center(
                  child: Text(
                    emptyMessage,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.outline,
                    ),
                  ),
                ),
              )
            else
              ...children.take(5),
          ],
        ),
      ),
    );
  }
}

class _PanelTile extends StatelessWidget {
  const _PanelTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.trailing,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final String trailing;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox.square(
            dimension: 34,
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: theme.colorScheme.primaryContainer,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(
                icon,
                size: 18,
                color: theme.colorScheme.onPrimaryContainer,
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  subtitle,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.outline,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Text(
            trailing,
            style: theme.textTheme.labelSmall?.copyWith(
              color: theme.colorScheme.outline,
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusPill extends StatelessWidget {
  const _StatusPill({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final healthy = status.toUpperCase() == 'UP';
    return AppStatusBadge(
      label: 'API $status',
      color: healthy ? AppDesignTokens.success : AppDesignTokens.danger,
      icon: healthy ? Icons.check_circle_outline : Icons.warning_amber_outlined,
    );
  }
}

class _MetricGrid extends StatelessWidget {
  const _MetricGrid({required this.metrics});

  final List<DashboardMetric> metrics;

  @override
  Widget build(BuildContext context) {
    return AppStatGrid(
      children: [
        for (final metric in metrics)
          AppStatCard(
            label: metric.label,
            value: metric.value,
            icon: metric.icon,
            color: metric.color,
          ),
      ],
    );
  }
}

class _ModuleGrid extends StatelessWidget {
  const _ModuleGrid({required this.modules});

  final List<ErpModule> modules;

  @override
  Widget build(BuildContext context) {
    return AppStatGrid(
      minItemWidth: 320,
      maxColumns: 3,
      children: [
        for (final module in modules)
          _ModuleCard(
            module: module,
            onTap: () => context.go(AppRoutes.module(module.id)),
          ),
      ],
    );
  }
}

class _ModuleCard extends StatelessWidget {
  const _ModuleCard({required this.module, required this.onTap});

  final ErpModule module;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              SizedBox.square(
                dimension: 46,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: theme.colorScheme.primaryContainer,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(
                    module.icon,
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
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            module.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ),
                        _ModuleStatus(status: module.status),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      module.description,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.outline,
                        height: 1.35,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              const Icon(Icons.chevron_right),
            ],
          ),
        ),
      ),
    );
  }
}

class _ModuleStatus extends StatelessWidget {
  const _ModuleStatus({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final ready = status == 'API ready';
    final color = ready ? const Color(0xFF16A34A) : const Color(0xFF64748B);

    return Container(
      height: 26,
      padding: const EdgeInsets.symmetric(horizontal: 9),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(13),
      ),
      alignment: Alignment.center,
      child: Text(
        status,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: color,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

String _titleCase(String value) {
  return value
      .replaceAll('_', ' ')
      .toLowerCase()
      .split(' ')
      .where((word) => word.isNotEmpty)
      .map((word) => '${word[0].toUpperCase()}${word.substring(1)}')
      .join(' ');
}

String _classSection(DashboardBirthday item) {
  final className = item.className;
  final sectionName = item.sectionName;
  if (className == null || className.isEmpty) {
    return 'Student';
  }
  if (sectionName == null || sectionName.isEmpty) {
    return className;
  }
  return '$className - $sectionName';
}

String _formatDateTime(DateTime? value) {
  if (value == null) {
    return '';
  }
  final local = value.toLocal();
  return '${_two(local.day)}/${_two(local.month)} ${_two(local.hour)}:${_two(local.minute)}';
}

String _formatDate(DateTime? value) {
  if (value == null) {
    return '';
  }
  final local = value.toLocal();
  return '${_two(local.day)}/${_two(local.month)}';
}

String _two(int value) => value.toString().padLeft(2, '0');
