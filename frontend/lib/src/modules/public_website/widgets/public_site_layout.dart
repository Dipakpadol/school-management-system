import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router/app_routes.dart';
import '../data/public_website_data.dart';
import 'public_site_theme.dart';

class PublicWebsiteLayout extends StatelessWidget {
  const PublicWebsiteLayout({
    required this.currentPath,
    required this.child,
    super.key,
  });

  final String currentPath;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: PublicSiteColors.background,
      endDrawer: _PublicSiteDrawer(currentPath: currentPath),
      body: Builder(
        builder: (context) {
          return Column(
            children: [
              _PublicHeader(
                currentPath: currentPath,
                onMenuPressed: Scaffold.of(context).openEndDrawer,
              ),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      child,
                      const _PublicFooter(),
                    ],
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _PublicHeader extends StatelessWidget {
  const _PublicHeader({
    required this.currentPath,
    required this.onMenuPressed,
  });

  final String currentPath;
  final VoidCallback onMenuPressed;

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.sizeOf(context).width;
    final desktop = width >= 980;

    return Material(
      color: Colors.white,
      elevation: 0,
      child: SafeArea(
        bottom: false,
        child: Container(
          height: 74,
          decoration: const BoxDecoration(
            border: Border(
              bottom: BorderSide(color: PublicSiteColors.border),
            ),
          ),
          padding: EdgeInsets.symmetric(horizontal: desktop ? 32 : 16),
          child: Row(
            children: [
              InkWell(
                borderRadius: BorderRadius.circular(8),
                onTap: () => context.go(AppRoutes.publicHome),
                child: const _PublicBrand(),
              ),
              if (desktop) ...[
                const Spacer(),
                _PublicNavLinks(currentPath: currentPath),
                const SizedBox(width: 16),
                _LoginButton(compact: false),
              ] else ...[
                const Spacer(),
                _LoginButton(compact: true),
                IconButton(
                  tooltip: 'Open navigation menu',
                  onPressed: onMenuPressed,
                  icon: const Icon(Icons.menu),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _PublicBrand extends StatelessWidget {
  const _PublicBrand();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox.square(
          dimension: 44,
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: PublicSiteColors.primary,
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Icon(Icons.school_outlined, color: Colors.white),
          ),
        ),
        const SizedBox(width: 12),
        ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 220),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                schoolProfile.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleMedium?.copyWith(
                  color: PublicSiteColors.ink,
                  fontWeight: FontWeight.w900,
                ),
              ),
              Text(
                'Public School',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: PublicSiteColors.muted,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _PublicNavLinks extends StatelessWidget {
  const _PublicNavLinks({required this.currentPath});

  final String currentPath;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        for (final item in publicNavItems)
          _HeaderNavItem(
            label: item.label,
            path: item.path,
            selected: item.path == currentPath,
          ),
      ],
    );
  }
}

class _HeaderNavItem extends StatelessWidget {
  const _HeaderNavItem({
    required this.label,
    required this.path,
    required this.selected,
  });

  final String label;
  final String path;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    final foreground = selected
        ? PublicSiteColors.primary
        : PublicSiteColors.text;

    return TextButton(
      onPressed: () => context.go(path),
      style: TextButton.styleFrom(
        foregroundColor: foreground,
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 12),
        textStyle: const TextStyle(fontWeight: FontWeight.w800),
      ),
      child: Text(label),
    );
  }
}

class _LoginButton extends StatelessWidget {
  const _LoginButton({required this.compact});

  final bool compact;

  @override
  Widget build(BuildContext context) {
    if (compact) {
      return TextButton.icon(
        onPressed: () => context.go(AppRoutes.login),
        icon: const Icon(Icons.login, size: 19),
        label: const Text('Login'),
      );
    }

    return SizedBox(
      height: 44,
      child: FilledButton.icon(
        onPressed: () => context.go(AppRoutes.login),
        icon: const Icon(Icons.login, size: 19),
        label: const Text('Login'),
        style: FilledButton.styleFrom(
          backgroundColor: PublicSiteColors.primary,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
    );
  }
}

class _PublicSiteDrawer extends StatelessWidget {
  const _PublicSiteDrawer({required this.currentPath});

  final String currentPath;

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Padding(
              padding: EdgeInsets.all(16),
              child: _PublicBrand(),
            ),
            const Divider(height: 1),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  for (final item in publicNavItems)
                    _DrawerNavItem(
                      icon: item.icon,
                      label: item.label,
                      path: item.path,
                      selected: item.path == currentPath,
                    ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: SizedBox(
                width: double.infinity,
                height: 48,
                child: FilledButton.icon(
                  onPressed: () {
                    Navigator.of(context).pop();
                    context.go(AppRoutes.login);
                  },
                  icon: const Icon(Icons.login),
                  label: const Text('Login to ERP'),
                  style: FilledButton.styleFrom(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DrawerNavItem extends StatelessWidget {
  const _DrawerNavItem({
    required this.icon,
    required this.label,
    required this.path,
    required this.selected,
  });

  final IconData icon;
  final String label;
  final String path;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final foreground = selected
        ? PublicSiteColors.primary
        : theme.colorScheme.onSurfaceVariant;

    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: ListTile(
        selected: selected,
        selectedTileColor: PublicSiteColors.primary.withValues(alpha: 0.08),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        leading: Icon(icon, color: foreground),
        title: Text(
          label,
          style: TextStyle(
            color: foreground,
            fontWeight: selected ? FontWeight.w800 : FontWeight.w600,
          ),
        ),
        onTap: () {
          Navigator.of(context).pop();
          context.go(path);
        },
      ),
    );
  }
}

class _PublicFooter extends StatelessWidget {
  const _PublicFooter();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return ColoredBox(
      color: PublicSiteColors.ink,
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 1180),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 42),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LayoutBuilder(
                  builder: (context, constraints) {
                    final stacked = constraints.maxWidth < 760;
                    final columns = [
                      const Expanded(flex: 2, child: _FooterSchoolInfo()),
                      const Expanded(child: _FooterQuickLinks()),
                      const Expanded(child: _FooterContact()),
                    ];

                    if (stacked) {
                      return const Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          _FooterSchoolInfo(),
                          SizedBox(height: 28),
                          _FooterQuickLinks(),
                          SizedBox(height: 28),
                          _FooterContact(),
                        ],
                      );
                    }

                    return Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        for (var index = 0; index < columns.length; index++) ...[
                          columns[index],
                          if (index != columns.length - 1)
                            const SizedBox(width: 40),
                        ],
                      ],
                    );
                  },
                ),
                const SizedBox(height: 32),
                const Divider(color: Color(0xFF334155), height: 1),
                const SizedBox(height: 18),
                Text(
                  'Copyright 2026 ${schoolProfile.name}. Public website content is ready for school-specific updates.',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: const Color(0xFFCBD5E1),
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

class _FooterSchoolInfo extends StatelessWidget {
  const _FooterSchoolInfo();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _PublicBrandOnDark(),
        const SizedBox(height: 16),
        Text(
          schoolProfile.tagline,
          style: theme.textTheme.bodyMedium?.copyWith(
            color: const Color(0xFFCBD5E1),
            height: 1.5,
          ),
        ),
        const SizedBox(height: 16),
        Wrap(
          spacing: 8,
          children: [
            for (final link in schoolProfile.socialLinks)
              IconButton(
                tooltip: link.label,
                onPressed: () {},
                color: Colors.white,
                icon: Icon(link.icon),
              ),
          ],
        ),
      ],
    );
  }
}

class _PublicBrandOnDark extends StatelessWidget {
  const _PublicBrandOnDark();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      children: [
        SizedBox.square(
          dimension: 42,
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: PublicSiteColors.primary,
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Icon(Icons.school_outlined, color: Colors.white),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            schoolProfile.name,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.titleMedium?.copyWith(
              color: Colors.white,
              fontWeight: FontWeight.w900,
            ),
          ),
        ),
      ],
    );
  }
}

class _FooterQuickLinks extends StatelessWidget {
  const _FooterQuickLinks();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Quick Links',
          style: theme.textTheme.titleSmall?.copyWith(
            color: Colors.white,
            fontWeight: FontWeight.w900,
          ),
        ),
        const SizedBox(height: 12),
        for (final item in publicNavItems)
          TextButton(
            onPressed: () => context.go(item.path),
            style: TextButton.styleFrom(
              foregroundColor: const Color(0xFFCBD5E1),
              padding: EdgeInsets.zero,
              alignment: Alignment.centerLeft,
            ),
            child: Text(item.label),
          ),
      ],
    );
  }
}

class _FooterContact extends StatelessWidget {
  const _FooterContact();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Contact',
          style: theme.textTheme.titleSmall?.copyWith(
            color: Colors.white,
            fontWeight: FontWeight.w900,
          ),
        ),
        const SizedBox(height: 14),
        _FooterContactLine(
          icon: Icons.place_outlined,
          text: schoolProfile.address,
        ),
        _FooterContactLine(
          icon: Icons.call_outlined,
          text: schoolProfile.phone,
        ),
        _FooterContactLine(
          icon: Icons.mail_outline,
          text: schoolProfile.email,
        ),
      ],
    );
  }
}

class _FooterContactLine extends StatelessWidget {
  const _FooterContactLine({
    required this.icon,
    required this.text,
  });

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 20, color: const Color(0xFF93C5FD)),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: const Color(0xFFCBD5E1),
                    height: 1.4,
                  ),
            ),
          ),
        ],
      ),
    );
  }
}
