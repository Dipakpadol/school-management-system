import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../theme/app_design_system.dart';

class AppPageLayout extends StatelessWidget {
  const AppPageLayout({
    required this.children,
    this.padding = const EdgeInsets.all(AppDesignTokens.pagePadding),
    this.spacing = 18,
    this.maxWidth,
    super.key,
  });

  final List<Widget> children;
  final EdgeInsetsGeometry padding;
  final double spacing;
  final double? maxWidth;

  @override
  Widget build(BuildContext context) {
    final content = ListView.separated(
      padding: padding,
      itemCount: children.length,
      separatorBuilder: (context, index) => SizedBox(height: spacing),
      itemBuilder: (context, index) => children[index],
    );

    if (maxWidth == null) {
      return content;
    }

    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth!),
        child: content,
      ),
    );
  }
}

class AppPageHeader extends StatelessWidget {
  const AppPageHeader({
    required this.title,
    this.subtitle,
    this.icon,
    this.actions = const [],
    super.key,
  });

  final String title;
  final String? subtitle;
  final IconData? icon;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppDesignTokens.surface,
        border: Border.all(color: AppDesignTokens.border),
        borderRadius: AppDesignTokens.borderRadius,
      ),
      child: Wrap(
        alignment: WrapAlignment.spaceBetween,
        crossAxisAlignment: WrapCrossAlignment.center,
        runSpacing: 14,
        children: [
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 680),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (icon != null) ...[
                  SizedBox.square(
                    dimension: 48,
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        color: AppDesignTokens.tint(AppDesignTokens.primary),
                        borderRadius: AppDesignTokens.borderRadius,
                      ),
                      child: Icon(icon, color: AppDesignTokens.primary),
                    ),
                  ),
                  const SizedBox(width: 14),
                ],
                Flexible(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        title,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.headlineSmall?.copyWith(
                          color: AppDesignTokens.ink,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      if (subtitle != null) ...[
                        const SizedBox(height: 6),
                        Text(
                          subtitle!,
                          maxLines: 3,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: AppDesignTokens.muted,
                            height: 1.45,
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
          ),
          if (actions.isNotEmpty)
            Wrap(spacing: 10, runSpacing: 10, children: actions),
        ],
      ),
    );
  }
}

class AppSectionCard extends StatelessWidget {
  const AppSectionCard({
    required this.child,
    this.title,
    this.subtitle,
    this.leading,
    this.trailing,
    this.padding = const EdgeInsets.all(16),
    super.key,
  });

  final String? title;
  final String? subtitle;
  final Widget? leading;
  final Widget? trailing;
  final Widget child;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final hasHeader = title != null || subtitle != null || trailing != null;

    return Card(
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: padding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (hasHeader) ...[
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (leading != null) ...[leading!, const SizedBox(width: 10)],
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (title != null)
                          Text(
                            title!,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w900,
                              color: AppDesignTokens.ink,
                            ),
                          ),
                        if (subtitle != null) ...[
                          const SizedBox(height: 4),
                          Text(
                            subtitle!,
                            maxLines: 3,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.bodyMedium?.copyWith(
                              color: AppDesignTokens.muted,
                              height: 1.4,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  if (trailing != null) ...[
                    const SizedBox(width: 12),
                    trailing!,
                  ],
                ],
              ),
              const SizedBox(height: 16),
            ],
            child,
          ],
        ),
      ),
    );
  }
}

class AppFilterBar extends StatelessWidget {
  const AppFilterBar({required this.children, super.key});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        border: Border.all(color: AppDesignTokens.border),
        borderRadius: AppDesignTokens.borderRadius,
      ),
      child: Wrap(spacing: 12, runSpacing: 12, children: children),
    );
  }
}

class AppStatGrid extends StatelessWidget {
  const AppStatGrid({
    required this.children,
    this.minItemWidth = 240,
    this.maxColumns = 4,
    this.spacing = 12,
    super.key,
  });

  final List<Widget> children;
  final double minItemWidth;
  final int maxColumns;
  final double spacing;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = math.max(
          1,
          math.min(maxColumns, (constraints.maxWidth / minItemWidth).floor()),
        );
        final width =
            (constraints.maxWidth - (spacing * (columns - 1))) / columns;

        return Wrap(
          spacing: spacing,
          runSpacing: spacing,
          children: [
            for (final child in children) SizedBox(width: width, child: child),
          ],
        );
      },
    );
  }
}

class AppStatCard extends StatelessWidget {
  const AppStatCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
    this.subtitle,
    super.key,
  });

  final String label;
  final String value;
  final IconData icon;
  final Color color;
  final String? subtitle;

  @override
  Widget build(BuildContext context) {
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
                  color: AppDesignTokens.tint(color, 0.12),
                  borderRadius: AppDesignTokens.borderRadius,
                ),
                child: Icon(icon, color: color),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    value,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.headlineSmall?.copyWith(
                      color: AppDesignTokens.ink,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: AppDesignTokens.muted,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  if (subtitle != null) ...[
                    const SizedBox(height: 2),
                    Text(
                      subtitle!,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: AppDesignTokens.muted,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class AppStatusBadge extends StatelessWidget {
  const AppStatusBadge({
    required this.label,
    this.color = AppDesignTokens.primary,
    this.icon,
    super.key,
  });

  final String label;
  final Color color;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 28,
      padding: EdgeInsets.only(
        left: icon == null ? 10 : 8,
        right: 10,
      ),
      decoration: BoxDecoration(
        color: AppDesignTokens.tint(color),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, color: color, size: 15),
            const SizedBox(width: 5),
          ],
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: color,
              fontWeight: FontWeight.w900,
            ),
          ),
        ],
      ),
    );
  }
}

class AppEmptyState extends StatelessWidget {
  const AppEmptyState({
    required this.message,
    this.icon = Icons.inbox_outlined,
    this.action,
    super.key,
  });

  final String message;
  final IconData icon;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox.square(
              dimension: 48,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: AppDesignTokens.tint(AppDesignTokens.primary),
                  borderRadius: AppDesignTokens.borderRadius,
                ),
                child: Icon(
                  icon,
                  color: AppDesignTokens.primary,
                ),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: AppDesignTokens.muted,
                height: 1.45,
                fontWeight: FontWeight.w600,
              ),
            ),
            if (action != null) ...[const SizedBox(height: 16), action!],
          ],
        ),
      ),
    );
  }
}

class AppPaginationBar extends StatelessWidget {
  const AppPaginationBar({
    required this.page,
    required this.totalPages,
    required this.onPrevious,
    required this.onNext,
    super.key,
  });

  final int page;
  final int totalPages;
  final VoidCallback? onPrevious;
  final VoidCallback? onNext;

  @override
  Widget build(BuildContext context) {
    final safeTotal = math.max(1, totalPages);

    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Text(
          'Page ${page + 1} of $safeTotal',
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
            color: AppDesignTokens.muted,
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(width: 12),
        IconButton.outlined(
          tooltip: 'Previous page',
          onPressed: onPrevious,
          icon: const Icon(Icons.chevron_left),
        ),
        const SizedBox(width: 8),
        IconButton.outlined(
          tooltip: 'Next page',
          onPressed: onNext,
          icon: const Icon(Icons.chevron_right),
        ),
      ],
    );
  }
}
