import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/formatters/app_formatters.dart';
import '../../../../core/theme/app_design_system.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../../core/widgets/app_page_layout.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/portal_models.dart';
import '../controllers/portal_providers.dart';

class StudentPortalPage extends ConsumerWidget {
  const StudentPortalPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dashboard = ref.watch(studentPortalProvider);
    return _PortalShell(
      title: 'Student Portal',
      activeModuleId: 'student-portal',
      child: dashboard.when(
        data: (data) => _StudentDashboardView(
          dashboard: data,
          title: data.displayName,
          icon: Icons.school_outlined,
          onRefresh: () => ref.invalidate(studentPortalProvider),
        ),
        error: (error, _) => _PortalError(
          message: error,
          onRetry: () => ref.invalidate(studentPortalProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading portal'),
      ),
    );
  }
}

class ParentPortalPage extends ConsumerStatefulWidget {
  const ParentPortalPage({super.key});

  @override
  ConsumerState<ParentPortalPage> createState() => _ParentPortalPageState();
}

class _ParentPortalPageState extends ConsumerState<ParentPortalPage> {
  String? _selectedChildId;

  @override
  Widget build(BuildContext context) {
    final children = ref.watch(parentChildrenProvider);
    return _PortalShell(
      title: 'Parent Portal',
      activeModuleId: 'parent-portal',
      child: children.when(
        data: _buildChildDashboard,
        error: (error, _) => _PortalError(
          message: error,
          onRetry: () => ref.invalidate(parentChildrenProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading portal'),
      ),
    );
  }

  Widget _buildChildDashboard(List<PortalChildModel> children) {
    if (children.isEmpty) {
      return const AppPageLayout(
        children: [
          AppPageHeader(
            title: 'Parent Portal',
            icon: Icons.family_restroom_outlined,
          ),
          AppEmptyState(message: 'No linked children found'),
        ],
      );
    }
    final selectedId = children.any((child) => child.id == _selectedChildId)
        ? _selectedChildId!
        : children.first.id;
    final dashboard = ref.watch(parentChildPortalProvider(selectedId));
    final selector = ConstrainedBox(
      constraints: const BoxConstraints(minWidth: 220, maxWidth: 320),
      child: DropdownButtonFormField<String>(
        initialValue: selectedId,
        isExpanded: true,
        decoration: const InputDecoration(
          labelText: 'Child',
          prefixIcon: Icon(Icons.family_restroom_outlined),
        ),
        items: [
          for (final child in children)
            DropdownMenuItem(
              value: child.id,
              child: Text(child.displayName, overflow: TextOverflow.ellipsis),
            ),
        ],
        onChanged: (value) => setState(() => _selectedChildId = value),
      ),
    );

    return dashboard.when(
      data: (data) => _StudentDashboardView(
        dashboard: data,
        title: data.displayName,
        icon: Icons.family_restroom_outlined,
        actions: [
          selector,
          IconButton.outlined(
            tooltip: 'Refresh',
            onPressed: () =>
                ref.invalidate(parentChildPortalProvider(selectedId)),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      error: (error, _) => _PortalError(
        message: error,
        onRetry: () => ref.invalidate(parentChildPortalProvider(selectedId)),
      ),
      loading: () => const AppLoadingState(label: 'Loading child portal'),
    );
  }
}

class TeacherPortalPage extends ConsumerWidget {
  const TeacherPortalPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dashboard = ref.watch(teacherPortalProvider);
    return _PortalShell(
      title: 'Teacher Portal',
      activeModuleId: 'teacher-portal',
      child: dashboard.when(
        data: (data) => _TeacherDashboardView(
          dashboard: data,
          onRefresh: () => ref.invalidate(teacherPortalProvider),
        ),
        error: (error, _) => _PortalError(
          message: error,
          onRetry: () => ref.invalidate(teacherPortalProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading portal'),
      ),
    );
  }
}

class _PortalShell extends ConsumerWidget {
  const _PortalShell({
    required this.title,
    required this.activeModuleId,
    required this.child,
  });

  final String title;
  final String activeModuleId;
  final Widget child;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return AdminShell(
      title: title,
      activeModuleId: activeModuleId,
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: child,
    );
  }
}

class _StudentDashboardView extends StatelessWidget {
  const _StudentDashboardView({
    required this.dashboard,
    required this.title,
    required this.icon,
    this.actions = const [],
    this.onRefresh,
  });

  final PortalDashboardModel dashboard;
  final String title;
  final IconData icon;
  final List<Widget> actions;
  final VoidCallback? onRefresh;

  @override
  Widget build(BuildContext context) {
    final headerActions = [
      ...actions,
      if (onRefresh != null)
        IconButton.outlined(
          tooltip: 'Refresh',
          onPressed: onRefresh,
          icon: const Icon(Icons.refresh),
        ),
    ];
    return AppPageLayout(
      children: [
        AppPageHeader(
          title: title,
          subtitle: _studentSubtitle(dashboard),
          icon: icon,
          actions: headerActions,
        ),
        AppStatGrid(
          children: [
            AppStatCard(
              label: 'Attendance',
              value:
                  '${numberText(dashboard.attendance, 'attendancePercentage')}%',
              icon: Icons.fact_check_outlined,
              color: AppDesignTokens.teal,
              subtitle:
                  '${numberText(dashboard.attendance, 'presentDays')} present',
            ),
            AppStatCard(
              label: 'Fee balance',
              value: moneyText(dashboard.fees, 'balanceAmount'),
              icon: Icons.payments_outlined,
              color: AppDesignTokens.warning,
              subtitle: '${moneyText(dashboard.fees, 'paidAmount')} paid',
            ),
            AppStatCard(
              label: 'Exam groups',
              value: '${jsonList(dashboard.exams['results']).length}',
              icon: Icons.assignment_outlined,
              color: AppDesignTokens.primary,
              subtitle: text(dashboard.exams, 'academicYear'),
            ),
            AppStatCard(
              label: 'Library loans',
              value:
                  '${pageContent(jsonMap(dashboard.library['loans'])).length}',
              icon: Icons.local_library_outlined,
              color: AppDesignTokens.violet,
              subtitle: _membershipLabel(dashboard.library),
            ),
          ],
        ),
        _TwoColumn(
          children: [
            _PortalPanel(
              title: 'Communications',
              icon: Icons.campaign_outlined,
              items: [
                for (final item in pageContent(dashboard.communications))
                  _PortalPanelItem(
                    title: text(item, 'title', fallback: 'Communication'),
                    subtitle: text(item, 'message'),
                    trailing: text(item, 'type'),
                  ),
              ],
              emptyMessage: 'No communications',
            ),
            _PortalPanel(
              title: 'Logistics',
              icon: Icons.route_outlined,
              items: [
                _PortalPanelItem(
                  title: 'Hostel',
                  subtitle: _hostelLabel(dashboard.hostel),
                  trailing: text(dashboard.hostel, 'status'),
                ),
                _PortalPanelItem(
                  title: 'Transport',
                  subtitle: _transportLabel(dashboard.transport),
                  trailing: text(dashboard.transport, 'status'),
                ),
              ],
              emptyMessage: 'No logistics records',
            ),
          ],
        ),
        _PortalPanel(
          title: 'Recent Attendance',
          icon: Icons.event_available_outlined,
          items: [
            for (final item in pageContent(
              jsonMap(dashboard.attendance['attendanceRecords']),
            ).take(8))
              _PortalPanelItem(
                title: AppFormatters.backendDate(text(item, 'attendanceDate')),
                subtitle: text(item, 'remarks'),
                trailing: text(item, 'status'),
              ),
          ],
          emptyMessage: 'No attendance records',
        ),
      ],
    );
  }
}

class _TeacherDashboardView extends StatelessWidget {
  const _TeacherDashboardView({
    required this.dashboard,
    required this.onRefresh,
  });

  final PortalTeacherDashboardModel dashboard;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    final subjectScopes = dashboard.scopes
        .where((scope) => text(scope, 'subjectName').isNotEmpty)
        .toList(growable: false);

    return AppPageLayout(
      children: [
        AppPageHeader(
          title: dashboard.displayName,
          subtitle: dashboard.employeeNumber,
          icon: Icons.co_present_outlined,
          actions: [
            IconButton.outlined(
              tooltip: 'Refresh',
              onPressed: onRefresh,
              icon: const Icon(Icons.refresh),
            ),
          ],
        ),
        AppStatGrid(
          children: [
            AppStatCard(
              label: 'Class scopes',
              value: '${dashboard.scopes.length}',
              icon: Icons.groups_outlined,
              color: AppDesignTokens.primary,
            ),
            AppStatCard(
              label: 'Subject scopes',
              value: '${subjectScopes.length}',
              icon: Icons.menu_book_outlined,
              color: AppDesignTokens.teal,
            ),
            AppStatCard(
              label: 'Assignments',
              value: '${dashboard.assignments.length}',
              icon: Icons.assignment_ind_outlined,
              color: AppDesignTokens.warning,
            ),
            AppStatCard(
              label: 'Messages',
              value: '${pageContent(dashboard.communications).length}',
              icon: Icons.campaign_outlined,
              color: AppDesignTokens.violet,
            ),
          ],
        ),
        _TwoColumn(
          children: [
            _PortalPanel(
              title: 'Assigned Scope',
              icon: Icons.account_tree_outlined,
              items: [
                for (final scope in dashboard.scopes)
                  _PortalPanelItem(
                    title: _scopeTitle(scope),
                    subtitle: text(scope, 'academicYear'),
                    trailing: text(scope, 'assignmentType'),
                  ),
              ],
              emptyMessage: 'No assigned classes',
            ),
            _PortalPanel(
              title: 'Communications',
              icon: Icons.campaign_outlined,
              items: [
                for (final item in pageContent(dashboard.communications))
                  _PortalPanelItem(
                    title: text(item, 'title', fallback: 'Communication'),
                    subtitle: text(item, 'message'),
                    trailing: text(item, 'type'),
                  ),
              ],
              emptyMessage: 'No communications',
            ),
          ],
        ),
      ],
    );
  }
}

class _TwoColumn extends StatelessWidget {
  const _TwoColumn({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        if (constraints.maxWidth < 900) {
          return Column(
            children: [
              for (var index = 0; index < children.length; index++) ...[
                children[index],
                if (index != children.length - 1) const SizedBox(height: 12),
              ],
            ],
          );
        }
        return Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (var index = 0; index < children.length; index++) ...[
              Expanded(child: children[index]),
              if (index != children.length - 1) const SizedBox(width: 12),
            ],
          ],
        );
      },
    );
  }
}

class _PortalPanel extends StatelessWidget {
  const _PortalPanel({
    required this.title,
    required this.icon,
    required this.items,
    required this.emptyMessage,
  });

  final String title;
  final IconData icon;
  final List<_PortalPanelItem> items;
  final String emptyMessage;

  @override
  Widget build(BuildContext context) {
    return AppSectionCard(
      title: title,
      leading: Icon(icon, color: Theme.of(context).colorScheme.primary),
      child: items.isEmpty
          ? AppEmptyState(message: emptyMessage)
          : Column(
              children: [
                for (var index = 0; index < items.length; index++) ...[
                  items[index],
                  if (index != items.length - 1) const Divider(height: 18),
                ],
              ],
            ),
    );
  }
}

class _PortalPanelItem extends StatelessWidget {
  const _PortalPanelItem({
    required this.title,
    required this.subtitle,
    required this.trailing,
  });

  final String title;
  final String subtitle;
  final String trailing;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: AppDesignTokens.ink,
                  fontWeight: FontWeight.w800,
                ),
              ),
              if (subtitle.isNotEmpty) ...[
                const SizedBox(height: 3),
                Text(
                  subtitle,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: AppDesignTokens.muted,
                  ),
                ),
              ],
            ],
          ),
        ),
        if (trailing.isNotEmpty) ...[
          const SizedBox(width: 10),
          AppStatusBadge.status(trailing),
        ],
      ],
    );
  }
}

class _PortalError extends StatelessWidget {
  const _PortalError({required this.message, required this.onRetry});

  final Object message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return AppErrorState(
      message: message.toString().replaceFirst('Exception: ', ''),
      onRetry: onRetry,
    );
  }
}

String _studentSubtitle(PortalDashboardModel dashboard) {
  final parts = [
    dashboard.admissionNumber,
    dashboard.className,
    dashboard.sectionName,
    if (dashboard.rollNumber.isNotEmpty) 'Roll ${dashboard.rollNumber}',
  ].where((part) => part.isNotEmpty).toList();
  return parts.join(' | ');
}

String _membershipLabel(JsonMap library) {
  final membership = jsonMap(library['membership']);
  final number = text(membership, 'membershipNumber');
  return number.isEmpty ? 'No membership' : number;
}

String _hostelLabel(JsonMap hostel) {
  final hostelName = text(hostel, 'hostelName');
  final room = text(hostel, 'roomNumber');
  final bed = text(hostel, 'bedNumber');
  final parts = [
    hostelName,
    if (room.isNotEmpty) 'Room $room',
    if (bed.isNotEmpty) 'Bed $bed',
  ].where((part) => part.isNotEmpty).toList();
  return parts.isEmpty ? 'Not allocated' : parts.join(' | ');
}

String _transportLabel(JsonMap transport) {
  final route = text(transport, 'routeName');
  final pickup = text(transport, 'pickupPointName');
  final vehicle = text(transport, 'vehicleNumber');
  final parts = [
    route,
    pickup,
    vehicle,
  ].where((part) => part.isNotEmpty).toList();
  return parts.isEmpty ? 'Not assigned' : parts.join(' | ');
}

String _scopeTitle(JsonMap scope) {
  final className = text(scope, 'className');
  final sectionName = text(scope, 'sectionName');
  final subjectName = text(scope, 'subjectName');
  final classSection = [
    className,
    if (sectionName.isNotEmpty) sectionName,
  ].where((part) => part.isNotEmpty).join(' ');
  if (subjectName.isEmpty) {
    return classSection.isEmpty ? 'Class scope' : classSection;
  }
  return '$classSection | $subjectName';
}

String moneyText(JsonMap json, String key) {
  final value = json[key];
  if (value is num) {
    return AppFormatters.money(value);
  }
  return AppFormatters.money(num.tryParse('$value'));
}
