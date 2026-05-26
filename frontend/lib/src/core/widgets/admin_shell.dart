import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/router/app_routes.dart';
import '../../features/auth/presentation/controllers/auth_controller.dart';
import '../../features/dashboard/domain/erp_module.dart';
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
  final VoidCallback onLogout;
  final String? activeModuleId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final compact = ResponsiveBreakpoints.isCompact(context);
    final user = ref.watch(authControllerProvider).user;
    final fallbackModules = visibleErpModules(user);
    final modules = ref.watch(currentMenuProvider).maybeWhen(
          data: (items) => visibleErpModulesFromMenuIds(
            user,
            items.map((item) => item.moduleId),
          ),
          orElse: () => fallbackModules,
        );

    if (compact) {
      return Scaffold(
        appBar: AppBar(
          title: Text(title),
          actions: [
            IconButton(
              tooltip: 'Sign out',
              onPressed: onLogout,
              icon: const Icon(Icons.logout_outlined),
            ),
          ],
        ),
        drawer: _ModuleDrawer(
          activeModuleId: activeModuleId,
          modules: modules,
          onLogout: onLogout,
        ),
        body: child,
      );
    }

    return Scaffold(
      body: Row(
        children: [
          _DesktopNavigation(
            activeModuleId: activeModuleId,
            modules: modules,
            onLogout: onLogout,
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

class _TopBar extends StatelessWidget {
  const _TopBar({
    required this.title,
    required this.onLogout,
  });

  final String title;
  final VoidCallback onLogout;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 64,
      color: Colors.white,
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Row(
        children: [
          Text(
            title,
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
          const Spacer(),
          IconButton(
            tooltip: 'Sign out',
            onPressed: onLogout,
            icon: const Icon(Icons.logout_outlined),
          ),
        ],
      ),
    );
  }
}

class _DesktopNavigation extends StatelessWidget {
  const _DesktopNavigation({
    required this.onLogout,
    required this.modules,
    this.activeModuleId,
  });

  final VoidCallback onLogout;
  final List<ErpModule> modules;
  final String? activeModuleId;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 280,
      child: ColoredBox(
        color: Colors.white,
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _BrandHeader(),
                const SizedBox(height: 24),
                Expanded(
                  child: ListView(
                    children: [
                      _NavItem(
                        icon: Icons.dashboard_outlined,
                        label: 'Dashboard',
                        selected: activeModuleId == null,
                        onTap: () => context.go(AppRoutes.dashboard),
                      ),
                      const SizedBox(height: 8),
                      for (final module in modules)
                        _NavItem(
                          icon: module.icon,
                          label: module.title,
                          selected: activeModuleId == module.id,
                          onTap: () => context.go(AppRoutes.module(module.id)),
                        ),
                    ],
                  ),
                ),
                const Divider(),
                _NavItem(
                  icon: Icons.logout_outlined,
                  label: 'Sign out',
                  selected: false,
                  onTap: onLogout,
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
    required this.onLogout,
    required this.modules,
    this.activeModuleId,
  });

  final VoidCallback onLogout;
  final List<ErpModule> modules;
  final String? activeModuleId;

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: SafeArea(
        child: Column(
          children: [
            const Padding(
              padding: EdgeInsets.all(16),
              child: _BrandHeader(),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.symmetric(horizontal: 12),
                children: [
                  _NavItem(
                    icon: Icons.dashboard_outlined,
                    label: 'Dashboard',
                    selected: activeModuleId == null,
                    onTap: () => context.go(AppRoutes.dashboard),
                  ),
                  const SizedBox(height: 8),
                  for (final module in modules)
                    _NavItem(
                      icon: module.icon,
                      label: module.title,
                      selected: activeModuleId == module.id,
                      onTap: () => context.go(AppRoutes.module(module.id)),
                    ),
                ],
              ),
            ),
            const Divider(),
            Padding(
              padding: const EdgeInsets.all(12),
              child: _NavItem(
                icon: Icons.logout_outlined,
                label: 'Sign out',
                selected: false,
                onTap: onLogout,
              ),
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
            child: const Icon(
              Icons.school_outlined,
              color: Colors.white,
            ),
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
  });

  final IconData icon;
  final String label;
  final bool selected;
  final VoidCallback onTap;

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
          height: 44,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children: [
                Icon(icon, color: foreground, size: 21),
                const SizedBox(width: 12),
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
