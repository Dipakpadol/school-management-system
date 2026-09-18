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
import '../theme/app_design_system.dart';
import 'app_page_layout.dart';

class AdminShell extends ConsumerStatefulWidget {
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
  ConsumerState<AdminShell> createState() => _AdminShellState();
}

class _AdminShellState extends ConsumerState<AdminShell> {
  bool _navigationCollapsed = false;

  @override
  Widget build(BuildContext context) {
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
    final autoCollapsed =
        MediaQuery.sizeOf(context).width < ResponsiveBreakpoints.medium;
    final effectiveCollapsed = autoCollapsed || _navigationCollapsed;

    if (compact) {
      return Scaffold(
        appBar: AppBar(
          title: Text(widget.title),
          actions: [_ProfileMenu(onLogout: widget.onLogout)],
        ),
        drawer: _ModuleDrawer(
          activeModuleId: widget.activeModuleId,
          activeUri: activeUri,
          sections: sections,
        ),
        body: widget.child,
      );
    }

    return Scaffold(
      body: Row(
        children: [
          _DesktopNavigation(
            activeModuleId: widget.activeModuleId,
            activeUri: activeUri,
            sections: sections,
            collapsed: effectiveCollapsed,
            toggleEnabled: !autoCollapsed,
            onToggleCollapsed: () {
              setState(() => _navigationCollapsed = !_navigationCollapsed);
            },
          ),
          const VerticalDivider(width: 1),
          Expanded(
            child: Column(
              children: [
                _TopBar(title: widget.title, onLogout: widget.onLogout),
                const Divider(height: 1),
                Expanded(child: widget.child),
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
    final user = ref.watch(authControllerProvider).user;
    final roleLabel = _roleLabel(user);

    return Container(
      height: 64,
      color: Colors.white,
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final tight = constraints.maxWidth < 620;
          final veryTight = constraints.maxWidth < 480;

          return Row(
            children: [
              Expanded(
                child: Text(
                  title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              if (!veryTight) ...[
                const _AcademicYearChip(),
                const SizedBox(width: 10),
              ],
              if (!veryTight) ...[
                Tooltip(
                  message: 'Notifications',
                  child: IconButton.outlined(
                    onPressed: () => context.go(AppRoutes.notifications),
                    icon: const Icon(Icons.notifications_none_outlined),
                  ),
                ),
                const SizedBox(width: 10),
              ],
              if (!tight && roleLabel.isNotEmpty) ...[
                AppStatusBadge(
                  label: roleLabel,
                  color: AppDesignTokens.teal,
                  icon: Icons.verified_user_outlined,
                ),
                const SizedBox(width: 10),
              ],
              _ProfileMenu(onLogout: onLogout),
            ],
          );
        },
      ),
    );
  }
}

class _AcademicYearChip extends StatelessWidget {
  const _AcademicYearChip();

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      message: 'Academic year context',
      child: Container(
        height: 36,
        padding: const EdgeInsets.symmetric(horizontal: 12),
        decoration: BoxDecoration(
          color: AppDesignTokens.tint(AppDesignTokens.primary),
          borderRadius: BorderRadius.circular(18),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.calendar_month_outlined,
              size: 18,
              color: AppDesignTokens.primary,
            ),
            const SizedBox(width: 8),
            Text(
              '2026-27',
              style: Theme.of(context).textTheme.labelLarge?.copyWith(
                color: AppDesignTokens.primary,
                fontWeight: FontWeight.w900,
              ),
            ),
          ],
        ),
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
              if ((user?.roles ?? const []).isNotEmpty) ...[
                const SizedBox(height: 4),
                Text(
                  (user?.roles ?? const []).join(', '),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: AppDesignTokens.teal,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ],
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
    required this.collapsed,
    required this.toggleEnabled,
    required this.onToggleCollapsed,
    this.activeModuleId,
  });

  final Uri activeUri;
  final List<_SidebarSection> sections;
  final bool collapsed;
  final bool toggleEnabled;
  final VoidCallback onToggleCollapsed;
  final String? activeModuleId;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: collapsed ? 84 : 292,
      child: ColoredBox(
        color: Colors.white,
        child: SafeArea(
          child: Padding(
            padding: EdgeInsets.fromLTRB(
              collapsed ? 12 : 16,
              16,
              collapsed ? 12 : 16,
              12,
            ),
            child: Column(
              crossAxisAlignment:
                  collapsed ? CrossAxisAlignment.center : CrossAxisAlignment.start,
              children: [
                _BrandHeader(collapsed: collapsed),
                const SizedBox(height: 12),
                IconButton.outlined(
                  tooltip: toggleEnabled
                      ? collapsed
                            ? 'Expand navigation'
                            : 'Collapse navigation'
                      : 'Navigation is compact on this screen',
                  onPressed: toggleEnabled ? onToggleCollapsed : null,
                  icon: Icon(
                    collapsed
                        ? Icons.keyboard_double_arrow_right
                        : Icons.keyboard_double_arrow_left,
                  ),
                ),
                const SizedBox(height: 14),
                Expanded(
                  child: ListView(
                    children: [
                      _NavItem(
                        icon: Icons.dashboard_outlined,
                        label: 'Dashboard',
                        selected: _isDashboardActive(activeUri, activeModuleId),
                        collapsed: collapsed,
                        onTap: () => context.go(AppRoutes.dashboard),
                      ),
                      const SizedBox(height: 8),
                      for (final section in sections)
                        collapsed
                            ? _CollapsedNavSection(
                                section: section,
                                activeUri: activeUri,
                                activeModuleId: activeModuleId,
                              )
                            : _NavSection(
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
            const Padding(
              padding: EdgeInsets.all(16),
              child: _BrandHeader(collapsed: false),
            ),
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

class _CollapsedNavSection extends StatelessWidget {
  const _CollapsedNavSection({
    required this.section,
    required this.activeUri,
    this.activeModuleId,
  });

  final _SidebarSection section;
  final Uri activeUri;
  final String? activeModuleId;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final active = section.isActive(activeUri, activeModuleId);
    final foreground = active
        ? AppDesignTokens.primary
        : theme.colorScheme.onSurfaceVariant;

    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Tooltip(
        message: section.label,
        child: PopupMenuButton<_SidebarItem>(
          tooltip: section.label,
          onSelected: (item) => context.go(item.route),
          itemBuilder: (context) => [
            for (final item in section.items)
              PopupMenuItem(
                value: item,
                child: Row(
                  children: [
                    Icon(item.icon, size: 19, color: foreground),
                    const SizedBox(width: 10),
                    Flexible(child: Text(item.label)),
                  ],
                ),
              ),
          ],
          child: Material(
            color: active
                ? AppDesignTokens.tint(AppDesignTokens.primary)
                : Colors.transparent,
            borderRadius: AppDesignTokens.borderRadius,
            child: SizedBox.square(
              dimension: 48,
              child: Icon(section.icon, color: foreground, size: 22),
            ),
          ),
        ),
      ),
    );
  }
}

class _BrandHeader extends StatelessWidget {
  const _BrandHeader({required this.collapsed});

  final bool collapsed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    if (collapsed) {
      return Tooltip(
        message: 'School ERP Admin Panel',
        child: SizedBox.square(
          dimension: 48,
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: theme.colorScheme.primary,
              borderRadius: AppDesignTokens.borderRadius,
            ),
            child: const Icon(Icons.school_outlined, color: Colors.white),
          ),
        ),
      );
    }

    return Row(
      children: [
        SizedBox.square(
          dimension: 44,
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: theme.colorScheme.primary,
              borderRadius: AppDesignTokens.borderRadius,
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
    this.collapsed = false,
  });

  final IconData icon;
  final String label;
  final bool selected;
  final VoidCallback onTap;
  final bool compact;
  final bool collapsed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final background = selected
        ? AppDesignTokens.tint(AppDesignTokens.primary, 0.14)
        : Colors.transparent;
    final foreground = selected
        ? theme.colorScheme.primary
        : theme.colorScheme.onSurfaceVariant;

    if (collapsed) {
      return Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: Tooltip(
          message: label,
          child: Material(
            color: background,
            borderRadius: AppDesignTokens.borderRadius,
            child: InkWell(
              borderRadius: AppDesignTokens.borderRadius,
              onTap: onTap,
              child: SizedBox.square(
                dimension: 48,
                child: Icon(icon, color: foreground, size: 22),
              ),
            ),
          ),
        ),
      );
    }

    return Material(
      color: background,
      borderRadius: AppDesignTokens.borderRadius,
      child: InkWell(
        borderRadius: AppDesignTokens.borderRadius,
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

String _roleLabel(AuthUser? user) {
  final roles = user?.roles ?? const [];
  if (roles.isEmpty) {
    return '';
  }
  return roles
      .take(2)
      .map((role) => _titleCase(role.replaceAll('_', ' ')))
      .join(', ');
}

String _titleCase(String value) {
  return value
      .trim()
      .split(RegExp(r'\s+'))
      .where((word) => word.isNotEmpty)
      .map((word) {
        final lower = word.toLowerCase();
        return '${lower[0].toUpperCase()}${lower.substring(1)}';
      })
      .join(' ');
}

const _sidebarSections = <_SidebarSection>[
  _SidebarSection(
    id: 'academics',
    label: 'ACADEMICS',
    icon: Icons.account_tree_outlined,
    items: [
      _SidebarItem(
        id: 'academic-years',
        label: 'Academic Years',
        icon: Icons.calendar_month_outlined,
        route: '/academic?section=academic-years',
        moduleId: 'academic',
        defaultForPath: AppRoutes.academic,
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
    ],
  ),
  _SidebarSection(
    id: 'people',
    label: 'PEOPLE',
    icon: Icons.groups_outlined,
    items: [
      _SidebarItem(
        id: 'students',
        label: 'Students',
        icon: Icons.school_outlined,
        route: AppRoutes.students,
        moduleId: 'students',
        activePrefixes: ['/students'],
      ),
      _SidebarItem(
        id: 'teachers',
        label: 'Teachers',
        icon: Icons.badge_outlined,
        route: AppRoutes.teachers,
        moduleId: 'teachers',
      ),
      _SidebarItem(
        id: 'staff',
        label: 'Staff',
        icon: Icons.badge_outlined,
        route: AppRoutes.staff,
        moduleId: 'staff',
        activePrefixes: ['/modules/staff'],
      ),
      _SidebarItem(
        id: 'staff-attendance',
        label: 'Staff Attendance',
        icon: Icons.fact_check_outlined,
        route: '${AppRoutes.staff}?section=attendance',
        moduleId: 'staff',
      ),
      _SidebarItem(
        id: 'leave',
        label: 'Leave Management',
        icon: Icons.event_available_outlined,
        route: '${AppRoutes.staff}?section=leave',
        moduleId: 'staff',
      ),
      _SidebarItem(
        id: 'payroll',
        label: 'Payroll',
        icon: Icons.request_quote_outlined,
        route: '${AppRoutes.staff}?section=payroll',
        moduleId: 'staff',
      ),
    ],
  ),
  _SidebarSection(
    id: 'finance',
    label: 'FINANCE',
    icon: Icons.payments_outlined,
    items: [
      _SidebarItem(
        id: 'academic-fees',
        label: 'Fees',
        icon: Icons.category_outlined,
        route: '/fees?section=categories',
        moduleId: 'fees',
        defaultForPath: AppRoutes.fees,
      ),
    ],
  ),
  _SidebarSection(
    id: 'operations',
    label: 'OPERATIONS',
    icon: Icons.business_center_outlined,
    items: [
      _SidebarItem(
        id: 'hostel',
        label: 'Hostel',
        icon: Icons.meeting_room_outlined,
        route: '/hostels?section=rooms',
        moduleId: 'hostel',
        defaultForPath: AppRoutes.hostel,
      ),
      _SidebarItem(
        id: 'transport',
        label: 'Transport',
        icon: Icons.directions_bus_filled_outlined,
        route: '/transport?section=vehicles',
        moduleId: 'transport',
        defaultForPath: AppRoutes.transport,
      ),
      _SidebarItem(
        id: 'library',
        label: 'Library',
        icon: Icons.local_library_outlined,
        route: AppRoutes.library,
        moduleId: 'library',
        activePrefixes: ['/modules/library'],
      ),
    ],
  ),
  _SidebarSection(
    id: 'communication',
    label: 'COMMUNICATION',
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
        id: 'communications',
        label: 'Communications',
        icon: Icons.campaign_outlined,
        route: AppRoutes.communications,
        moduleId: 'communications',
        activePrefixes: ['/modules/communications'],
      ),
    ],
  ),
  _SidebarSection(
    id: 'documents',
    label: 'DOCUMENTS',
    icon: Icons.description_outlined,
    items: [
      _SidebarItem(
        id: 'documents',
        label: 'Student Documents',
        icon: Icons.description_outlined,
        route: '/students?section=documents',
        moduleId: 'students',
      ),
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
      _SidebarItem(
        id: 'library-reports',
        label: 'Library Reports',
        icon: Icons.local_library_outlined,
        route: '/reports?section=library',
        moduleId: 'reports',
      ),
    ],
  ),
  _SidebarSection(
    id: 'administration',
    label: 'ADMINISTRATION',
    icon: Icons.admin_panel_settings_outlined,
    items: [
      _SidebarItem(
        id: 'users',
        label: 'Users',
        icon: Icons.manage_accounts_outlined,
        route: AppRoutes.users,
        moduleId: 'users',
      ),
      _SidebarItem(
        id: 'roles',
        label: 'Roles & Permissions',
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
];
