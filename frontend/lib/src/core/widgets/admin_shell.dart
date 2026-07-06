import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/router/app_routes.dart';
import '../../features/auth/domain/entities/auth_user.dart';
import '../../features/auth/presentation/controllers/auth_controller.dart';
import '../../features/dashboard/domain/menu_policy.dart';
import '../../features/dashboard/presentation/controllers/menu_controller.dart';
import '../layout/responsive_breakpoints.dart';

class AdminShell extends ConsumerWidget {
  const AdminShell({
    required this.title,
    required this.child,
    required this.onLogout,
    this.activeModuleId,
    super.key,
  });

  final String title;
  final Widget child;
  final FutureOr<void> Function() onLogout;
  final String? activeModuleId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final compact = ResponsiveBreakpoints.isCompact(context);
    final user = ref.watch(authControllerProvider).user;
    final menuIds = ref
        .watch(currentMenuProvider)
        .maybeWhen(
          data: (items) =>
              items.map((item) => item.moduleId.toLowerCase()).toSet(),
          orElse: () => null,
        );
    final sections = _visibleSections(user, menuIds);
    final activeUri = GoRouterState.of(context).uri;

    if (compact) {
      return Scaffold(
        appBar: AppBar(
          title: Text(title),
          actions: [_ProfileMenu(onLogout: onLogout)],
        ),
        drawer: _ModuleDrawer(
          activeModuleId: activeModuleId,
          activeUri: activeUri,
          sections: sections,
        ),
        body: child,
      );
    }

    return Scaffold(
      body: Row(
        children: [
          _DesktopNavigation(
            activeModuleId: activeModuleId,
            activeUri: activeUri,
            sections: sections,
          ),
          const VerticalDivider(width: 1),
          Expanded(
            child: Column(
              children: [
                _TopBar(title: title, onLogout: onLogout),
                const Divider(height: 1),
                Expanded(child: child),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _TopBar extends ConsumerWidget {
  const _TopBar({required this.title, required this.onLogout});

  final String title;
  final FutureOr<void> Function() onLogout;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Container(
      height: 64,
      color: Colors.white,
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Row(
        children: [
          Text(
            title,
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
          ),
          const Spacer(),
          _ProfileMenu(onLogout: onLogout),
        ],
      ),
    );
  }
}

class _ProfileMenu extends ConsumerWidget {
  const _ProfileMenu({required this.onLogout});

  final FutureOr<void> Function() onLogout;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authControllerProvider);
    final user = authState.user;

    if (authState.isSubmitting) {
      return const Padding(
        padding: EdgeInsets.symmetric(horizontal: 12),
        child: SizedBox.square(
          dimension: 20,
          child: CircularProgressIndicator(strokeWidth: 2),
        ),
      );
    }

    return PopupMenuButton<String>(
      tooltip: 'User profile',
      onSelected: (value) async {
        if (value == 'logout') {
          await onLogout();
        }
      },
      itemBuilder: (context) => [
        PopupMenuItem(
          enabled: false,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                user?.displayName ?? 'User',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(
                  context,
                ).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w800),
              ),
              Text(
                user?.email ?? '',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Theme.of(context).colorScheme.outline,
                ),
              ),
            ],
          ),
        ),
        const PopupMenuDivider(),
        const PopupMenuItem(
          value: 'logout',
          child: Row(
            children: [
              Icon(Icons.logout_outlined),
              SizedBox(width: 12),
              Text('Sign out'),
            ],
          ),
        ),
      ],
      child: CircleAvatar(
        radius: 18,
        backgroundColor: Theme.of(context).colorScheme.primaryContainer,
        foregroundColor: Theme.of(context).colorScheme.onPrimaryContainer,
        child: Text(_initials(user?.displayName ?? user?.email ?? 'U')),
      ),
    );
  }
}

class _DesktopNavigation extends StatelessWidget {
  const _DesktopNavigation({
    required this.activeUri,
    required this.sections,
    this.activeModuleId,
  });

  final Uri activeUri;
  final List<_SidebarSection> sections;
  final String? activeModuleId;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 292,
      child: ColoredBox(
        color: Colors.white,
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _BrandHeader(),
                const SizedBox(height: 22),
                Expanded(
                  child: ListView(
                    children: [
                      _NavItem(
                        icon: Icons.dashboard_outlined,
                        label: 'Dashboard',
                        selected: _isDashboardActive(activeUri, activeModuleId),
                        onTap: () => context.go(AppRoutes.dashboard),
                      ),
                      const SizedBox(height: 8),
                      for (final section in sections)
                        _NavSection(
                          section: section,
                          activeUri: activeUri,
                          activeModuleId: activeModuleId,
                        ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ModuleDrawer extends StatelessWidget {
  const _ModuleDrawer({
    required this.activeUri,
    required this.sections,
    this.activeModuleId,
  });

  final Uri activeUri;
  final List<_SidebarSection> sections;
  final String? activeModuleId;

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: SafeArea(
        child: Column(
          children: [
            const Padding(padding: EdgeInsets.all(16), child: _BrandHeader()),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.symmetric(horizontal: 12),
                children: [
                  _NavItem(
                    icon: Icons.dashboard_outlined,
                    label: 'Dashboard',
                    selected: _isDashboardActive(activeUri, activeModuleId),
                    onTap: () {
                      Navigator.of(context).pop();
                      context.go(AppRoutes.dashboard);
                    },
                  ),
                  const SizedBox(height: 8),
                  for (final section in sections)
                    _NavSection(
                      section: section,
                      activeUri: activeUri,
                      activeModuleId: activeModuleId,
                      closeDrawerOnTap: true,
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _NavSection extends StatelessWidget {
  const _NavSection({
    required this.section,
    required this.activeUri,
    this.activeModuleId,
    this.closeDrawerOnTap = false,
  });

  final _SidebarSection section;
  final Uri activeUri;
  final String? activeModuleId;
  final bool closeDrawerOnTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final active = section.isActive(activeUri, activeModuleId);
    final foreground = active
        ? theme.colorScheme.primary
        : theme.colorScheme.onSurfaceVariant;

    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: ExpansionTile(
          key: PageStorageKey(
            'nav-${section.id}-${active ? 'open' : 'closed'}',
          ),
          initiallyExpanded: active,
          maintainState: true,
          tilePadding: const EdgeInsets.symmetric(horizontal: 12),
          childrenPadding: const EdgeInsets.fromLTRB(8, 0, 0, 8),
          backgroundColor: active
              ? const Color(0xFFEEF6FF)
              : Colors.transparent,
          collapsedBackgroundColor: active
              ? const Color(0xFFEEF6FF)
              : Colors.transparent,
          iconColor: foreground,
          collapsedIconColor: foreground,
          textColor: foreground,
          collapsedTextColor: foreground,
          leading: Icon(section.icon, size: 21),
          title: Text(
            section.label,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.bodyMedium?.copyWith(
              fontWeight: active ? FontWeight.w800 : FontWeight.w600,
            ),
          ),
          children: [
            for (final item in section.items)
              _NavItem(
                icon: item.icon,
                label: item.label,
                selected: item.isActive(activeUri, activeModuleId),
                compact: true,
                onTap: () {
                  if (closeDrawerOnTap) {
                    Navigator.of(context).pop();
                  }
                  context.go(item.route);
                },
              ),
          ],
        ),
      ),
    );
  }
}

class _BrandHeader extends StatelessWidget {
  const _BrandHeader();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      children: [
        SizedBox.square(
          dimension: 44,
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: theme.colorScheme.primary,
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Icon(Icons.school_outlined, color: Colors.white),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'School ERP',
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
              Text(
                'Admin Panel',
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.outline,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _NavItem extends StatelessWidget {
  const _NavItem({
    required this.icon,
    required this.label,
    required this.selected,
    required this.onTap,
    this.compact = false,
  });

  final IconData icon;
  final String label;
  final bool selected;
  final VoidCallback onTap;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final background = selected ? const Color(0xFFDBEAFE) : Colors.transparent;
    final foreground = selected
        ? theme.colorScheme.primary
        : theme.colorScheme.onSurfaceVariant;

    return Material(
      color: background,
      borderRadius: BorderRadius.circular(8),
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: onTap,
        child: SizedBox(
          height: compact ? 40 : 44,
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: compact ? 10 : 12),
            child: Row(
              children: [
                Icon(icon, color: foreground, size: compact ? 19 : 21),
                SizedBox(width: compact ? 10 : 12),
                Expanded(
                  child: Text(
                    label,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: foreground,
                      fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SidebarSection {
  const _SidebarSection({
    required this.id,
    required this.label,
    required this.icon,
    required this.items,
  });

  final String id;
  final String label;
  final IconData icon;
  final List<_SidebarItem> items;

  bool isActive(Uri uri, String? activeModuleId) {
    return items.any((item) => item.isActive(uri, activeModuleId));
  }
}

class _SidebarItem {
  const _SidebarItem({
    required this.id,
    required this.label,
    required this.icon,
    required this.route,
    required this.moduleId,
    this.activePrefixes = const [],
    this.defaultForPath,
  });

  final String id;
  final String label;
  final IconData icon;
  final String route;
  final String moduleId;
  final List<String> activePrefixes;
  final String? defaultForPath;

  bool isActive(Uri uri, String? activeModuleId) {
    final routeUri = Uri.parse(route);
    final routeSection = routeUri.queryParameters['section'];
    final currentSection = uri.queryParameters['section'];

    if (routeSection != null) {
      return uri.path == routeUri.path && currentSection == routeSection;
    }
    if (defaultForPath != null &&
        uri.path == defaultForPath &&
        currentSection == null) {
      return true;
    }
    if (currentSection == null &&
        activePrefixes.any(
          (prefix) => uri.path == prefix || uri.path.startsWith('$prefix/'),
        )) {
      return true;
    }
    if (uri.path == routeUri.path && currentSection == null) {
      return true;
    }
    return false;
  }
}

List<_SidebarSection> _visibleSections(AuthUser? user, Set<String>? menuIds) {
  return _sidebarSections
      .map((section) {
        final items = section.items
            .where((item) => _canShowItem(user, menuIds, item))
            .toList(growable: false);
        return _SidebarSection(
          id: section.id,
          label: section.label,
          icon: section.icon,
          items: items,
        );
      })
      .where((section) => section.items.isNotEmpty)
      .toList(growable: false);
}

bool _canShowItem(AuthUser? user, Set<String>? menuIds, _SidebarItem item) {
  if (!canAccessModule(user, item.moduleId)) {
    return false;
  }
  if (menuIds == null || menuIds.isEmpty) {
    return true;
  }
  return menuIds.contains(item.moduleId);
}

bool _isDashboardActive(Uri uri, String? activeModuleId) {
  return uri.path == AppRoutes.dashboard && activeModuleId == null;
}

String _initials(String value) {
  final words = value.trim().split(RegExp(r'\s+'));
  if (words.isEmpty || words.first.isEmpty) {
    return 'U';
  }
  if (words.length == 1) {
    return words.first.substring(0, 1).toUpperCase();
  }
  return '${words.first.substring(0, 1)}${words.last.substring(0, 1)}'
      .toUpperCase();
}

const _sidebarSections = <_SidebarSection>[
  _SidebarSection(
    id: 'administration',
    label: 'Administration',
    icon: Icons.admin_panel_settings_outlined,
    items: [
      _SidebarItem(
        id: 'users',
        label: 'User Management',
        icon: Icons.manage_accounts_outlined,
        route: AppRoutes.users,
        moduleId: 'users',
      ),
      _SidebarItem(
        id: 'roles',
        label: 'Role & Permission',
        icon: Icons.verified_user_outlined,
        route: AppRoutes.roles,
        moduleId: 'roles',
      ),
      _SidebarItem(
        id: 'settings',
        label: 'Settings',
        icon: Icons.tune_outlined,
        route: AppRoutes.settings,
        moduleId: 'settings',
      ),
      _SidebarItem(
        id: 'audit-logs',
        label: 'Audit Logs',
        icon: Icons.manage_search_outlined,
        route: AppRoutes.auditLogs,
        moduleId: 'audit-logs',
      ),
    ],
  ),
  _SidebarSection(
    id: 'academic',
    label: 'Academic',
    icon: Icons.account_tree_outlined,
    items: [
      _SidebarItem(
        id: 'academic-years',
        label: 'Academic Year',
        icon: Icons.calendar_month_outlined,
        route: '/academic?section=academic-years',
        moduleId: 'academic',
        defaultForPath: AppRoutes.academic,
      ),
      _SidebarItem(
        id: 'classes',
        label: 'Classes & Divisions',
        icon: Icons.class_outlined,
        route: '/academic?section=classes',
        moduleId: 'academic',
      ),
      _SidebarItem(
        id: 'subjects',
        label: 'Subjects',
        icon: Icons.menu_book_outlined,
        route: '/academic?section=subjects',
        moduleId: 'academic',
      ),
      _SidebarItem(
        id: 'teachers',
        label: 'Teacher Management',
        icon: Icons.badge_outlined,
        route: AppRoutes.teachers,
        moduleId: 'teachers',
      ),
      _SidebarItem(
        id: 'teacher-attendance',
        label: 'Teacher Attendance',
        icon: Icons.fact_check_outlined,
        route: AppRoutes.teacherAttendance,
        moduleId: 'teachers',
      ),
    ],
  ),
  _SidebarSection(
    id: 'student',
    label: 'Student',
    icon: Icons.school_outlined,
    items: [
      _SidebarItem(
        id: 'students',
        label: 'Student Management',
        icon: Icons.groups_outlined,
        route: AppRoutes.students,
        moduleId: 'students',
        activePrefixes: ['/students'],
      ),
      _SidebarItem(
        id: 'student-attendance',
        label: 'Student Attendance',
        icon: Icons.fact_check_outlined,
        route: AppRoutes.attendance,
        moduleId: 'attendance',
      ),
      _SidebarItem(
        id: 'exams',
        label: 'Exams & Results',
        icon: Icons.assignment_outlined,
        route: AppRoutes.exams,
        moduleId: 'exams',
        activePrefixes: ['/exams'],
      ),
      _SidebarItem(
        id: 'documents',
        label: 'Documents',
        icon: Icons.description_outlined,
        route: '/students?section=documents',
        moduleId: 'students',
      ),
    ],
  ),
  _SidebarSection(
    id: 'fees',
    label: 'Fees',
    icon: Icons.payments_outlined,
    items: [
      _SidebarItem(
        id: 'fee-categories',
        label: 'Fee Categories',
        icon: Icons.category_outlined,
        route: '/fees?section=categories',
        moduleId: 'fees',
      ),
      _SidebarItem(
        id: 'fee-structures',
        label: 'Fee Structures',
        icon: Icons.account_balance_wallet_outlined,
        route: '/fees?section=structures',
        moduleId: 'fees',
        activePrefixes: ['/fees/structures'],
        defaultForPath: AppRoutes.fees,
      ),
      _SidebarItem(
        id: 'fee-assignments',
        label: 'Fee Assignments',
        icon: Icons.assignment_ind_outlined,
        route: AppRoutes.feeAssignments,
        moduleId: 'fees',
        activePrefixes: ['/fees/assignments'],
      ),
      _SidebarItem(
        id: 'payment-collection',
        label: 'Payment Collection',
        icon: Icons.receipt_long_outlined,
        route: AppRoutes.feePayments,
        moduleId: 'fees',
        activePrefixes: ['/fees/payments', '/fees/students'],
      ),
      _SidebarItem(
        id: 'defaulters',
        label: 'Defaulters',
        icon: Icons.warning_amber_outlined,
        route: '/fees?section=defaulters',
        moduleId: 'fees',
      ),
      _SidebarItem(
        id: 'fee-reports',
        label: 'Fee Reports',
        icon: Icons.analytics_outlined,
        route: '/reports?section=fees',
        moduleId: 'reports',
      ),
    ],
  ),
  _SidebarSection(
    id: 'hostel',
    label: 'Hostel',
    icon: Icons.apartment_outlined,
    items: [
      _SidebarItem(
        id: 'hostel-rooms',
        label: 'Hostel Rooms',
        icon: Icons.meeting_room_outlined,
        route: '/hostels?section=rooms',
        moduleId: 'hostel',
        defaultForPath: AppRoutes.hostel,
      ),
      _SidebarItem(
        id: 'hostel-allocation',
        label: 'Hostel Allocation',
        icon: Icons.bed_outlined,
        route: '/hostels?section=allocation',
        moduleId: 'hostel',
      ),
      _SidebarItem(
        id: 'hostel-fees',
        label: 'Hostel Fees',
        icon: Icons.payments_outlined,
        route: '/hostels?section=fees',
        moduleId: 'hostel',
      ),
    ],
  ),
  _SidebarSection(
    id: 'transport',
    label: 'Transport',
    icon: Icons.directions_bus_outlined,
    items: [
      _SidebarItem(
        id: 'vehicles',
        label: 'Vehicles / Buses',
        icon: Icons.directions_bus_filled_outlined,
        route: '/transport?section=vehicles',
        moduleId: 'transport',
        defaultForPath: AppRoutes.transport,
      ),
      _SidebarItem(
        id: 'routes',
        label: 'Routes',
        icon: Icons.route_outlined,
        route: '/transport?section=routes',
        moduleId: 'transport',
      ),
      _SidebarItem(
        id: 'pickup-points',
        label: 'Pickup Points',
        icon: Icons.pin_drop_outlined,
        route: '/transport?section=pickup-points',
        moduleId: 'transport',
      ),
      _SidebarItem(
        id: 'transport-assignment',
        label: 'Transport Assignment',
        icon: Icons.assignment_ind_outlined,
        route: '/transport?section=assignments',
        moduleId: 'transport',
      ),
      _SidebarItem(
        id: 'transport-fees',
        label: 'Transport Fees',
        icon: Icons.payments_outlined,
        route: '/transport?section=fees',
        moduleId: 'transport',
      ),
    ],
  ),
  _SidebarSection(
    id: 'communication',
    label: 'Communication',
    icon: Icons.campaign_outlined,
    items: [
      _SidebarItem(
        id: 'notifications',
        label: 'Notifications',
        icon: Icons.notifications_active_outlined,
        route: AppRoutes.notifications,
        moduleId: 'notifications',
        defaultForPath: AppRoutes.notifications,
      ),
      _SidebarItem(
        id: 'notification-templates',
        label: 'Notification Templates',
        icon: Icons.article_outlined,
        route: '/notifications?section=templates',
        moduleId: 'notifications',
      ),
      _SidebarItem(
        id: 'notification-logs',
        label: 'Notification Logs',
        icon: Icons.history_outlined,
        route: '/notifications?section=logs',
        moduleId: 'notifications',
      ),
    ],
  ),
  _SidebarSection(
    id: 'reports',
    label: 'Reports',
    icon: Icons.analytics_outlined,
    items: [
      _SidebarItem(
        id: 'student-reports',
        label: 'Student Reports',
        icon: Icons.school_outlined,
        route: '/reports?section=students',
        moduleId: 'reports',
        defaultForPath: AppRoutes.reports,
      ),
      _SidebarItem(
        id: 'attendance-reports',
        label: 'Attendance Reports',
        icon: Icons.fact_check_outlined,
        route: '/reports?section=attendance',
        moduleId: 'reports',
      ),
      _SidebarItem(
        id: 'fee-report',
        label: 'Fee Reports',
        icon: Icons.payments_outlined,
        route: '/reports?section=fees',
        moduleId: 'reports',
      ),
      _SidebarItem(
        id: 'exam-reports',
        label: 'Exam Reports',
        icon: Icons.assignment_outlined,
        route: '/reports?section=exams',
        moduleId: 'reports',
      ),
      _SidebarItem(
        id: 'audit-reports',
        label: 'Audit Reports',
        icon: Icons.manage_search_outlined,
        route: '/reports?section=audit',
        moduleId: 'reports',
      ),
    ],
  ),
];
