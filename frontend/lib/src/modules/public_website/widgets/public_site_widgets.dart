import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../data/public_website_data.dart';
import '../models/public_website_models.dart';
import 'public_site_theme.dart';

class PublicSection extends StatelessWidget {
  const PublicSection({
    required this.child,
    this.eyebrow,
    this.title,
    this.subtitle,
    this.backgroundColor = PublicSiteColors.background,
    this.padding = const EdgeInsets.symmetric(horizontal: 24, vertical: 64),
    this.maxWidth = 1180,
    super.key,
  });

  final String? eyebrow;
  final String? title;
  final String? subtitle;
  final Widget child;
  final Color backgroundColor;
  final EdgeInsetsGeometry padding;
  final double maxWidth;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return ColoredBox(
      color: backgroundColor,
      child: Center(
        child: ConstrainedBox(
          constraints: BoxConstraints(maxWidth: maxWidth),
          child: Padding(
            padding: padding,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (eyebrow != null) ...[
                  Text(
                    eyebrow!.toUpperCase(),
                    style: theme.textTheme.labelLarge?.copyWith(
                      color: PublicSiteColors.green,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 0,
                    ),
                  ),
                  const SizedBox(height: 10),
                ],
                if (title != null) ...[
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 760),
                    child: Text(
                      title!,
                      style: theme.textTheme.headlineMedium?.copyWith(
                        color: PublicSiteColors.ink,
                        fontWeight: FontWeight.w900,
                        height: 1.12,
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                ],
                if (subtitle != null) ...[
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 780),
                    child: Text(
                      subtitle!,
                      style: theme.textTheme.bodyLarge?.copyWith(
                        color: PublicSiteColors.muted,
                        height: 1.55,
                      ),
                    ),
                  ),
                  const SizedBox(height: 28),
                ],
                child,
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class PublicResponsiveGrid extends StatelessWidget {
  const PublicResponsiveGrid({
    required this.children,
    this.minItemWidth = 260,
    this.maxColumns = 3,
    this.spacing = 18,
    this.runSpacing = 18,
    super.key,
  });

  final List<Widget> children;
  final double minItemWidth;
  final int maxColumns;
  final double spacing;
  final double runSpacing;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final availableWidth = constraints.maxWidth;
        final columns = math.max(
          1,
          math.min(maxColumns, (availableWidth / minItemWidth).floor()),
        );
        final itemWidth =
            (availableWidth - (spacing * (columns - 1))) / columns;

        return Wrap(
          spacing: spacing,
          runSpacing: runSpacing,
          children: [
            for (final child in children)
              SizedBox(width: itemWidth, child: child),
          ],
        );
      },
    );
  }
}

class PublicFeatureCard extends StatelessWidget {
  const PublicFeatureCard({
    required this.title,
    required this.description,
    required this.icon,
    required this.color,
    super.key,
  });

  final String title;
  final String description;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      color: Colors.white,
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox.square(
              dimension: 46,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(icon, color: color),
              ),
            ),
            const SizedBox(height: 18),
            Text(
              title,
              style: theme.textTheme.titleMedium?.copyWith(
                color: PublicSiteColors.ink,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              description,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: PublicSiteColors.muted,
                height: 1.45,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class PublicEventCard extends StatelessWidget {
  const PublicEventCard({required this.event, this.compact = false, super.key});

  final PublicEventItem event;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      color: Colors.white,
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: EdgeInsets.all(compact ? 16 : 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Wrap(
              spacing: 10,
              runSpacing: 8,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                _Tag(label: event.category),
                Text(
                  event.dateLabel,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: PublicSiteColors.muted,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              event.title,
              style: theme.textTheme.titleMedium?.copyWith(
                color: PublicSiteColors.ink,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              event.description,
              maxLines: compact ? 3 : null,
              overflow: compact ? TextOverflow.ellipsis : null,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: PublicSiteColors.muted,
                height: 1.5,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class PublicGalleryTile extends StatelessWidget {
  const PublicGalleryTile({required this.item, super.key});

  final GalleryItem item;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      clipBehavior: Clip.antiAlias,
      color: Colors.white,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AspectRatio(
            aspectRatio: 4 / 3,
            child: PublicNetworkImage(
              imageUrl: item.imageUrl,
              label: item.title,
              borderRadius: BorderRadius.zero,
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.category,
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: PublicSiteColors.green,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  item.title,
                  style: theme.textTheme.titleSmall?.copyWith(
                    color: PublicSiteColors.ink,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class PublicNetworkImage extends StatelessWidget {
  const PublicNetworkImage({
    required this.imageUrl,
    required this.label,
    this.fit = BoxFit.cover,
    this.borderRadius = const BorderRadius.all(Radius.circular(8)),
    super.key,
  });

  final String imageUrl;
  final String label;
  final BoxFit fit;
  final BorderRadius borderRadius;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: borderRadius,
      child: Image.network(
        imageUrl,
        fit: fit,
        loadingBuilder: (context, child, loadingProgress) {
          if (loadingProgress == null) {
            return child;
          }
          return _ImageFallback(label: label, loading: true);
        },
        errorBuilder: (context, error, stackTrace) {
          return _ImageFallback(label: label);
        },
      ),
    );
  }
}

class PublicPageHero extends StatelessWidget {
  const PublicPageHero({
    required this.title,
    required this.subtitle,
    required this.icon,
    this.action,
    super.key,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return PublicSection(
      backgroundColor: Colors.white,
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 52),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final stacked = constraints.maxWidth < 760;
          final intro = Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox.square(
                dimension: 54,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: PublicSiteColors.primary.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(icon, color: PublicSiteColors.primary, size: 30),
                ),
              ),
              const SizedBox(height: 20),
              Text(
                title,
                style: theme.textTheme.displaySmall?.copyWith(
                  color: PublicSiteColors.ink,
                  fontWeight: FontWeight.w900,
                  height: 1.08,
                ),
              ),
              const SizedBox(height: 14),
              Text(
                subtitle,
                style: theme.textTheme.titleMedium?.copyWith(
                  color: PublicSiteColors.muted,
                  height: 1.55,
                ),
              ),
              if (action != null) ...[const SizedBox(height: 24), action!],
            ],
          );

          final image = AspectRatio(
            aspectRatio: stacked ? 16 / 10 : 16 / 9,
            child: const PublicNetworkImage(
              imageUrl:
                  'https://images.unsplash.com/photo-1577896851231-70ef18881754?auto=format&fit=crop&w=1200&q=80',
              label: 'Students in classroom',
            ),
          );

          if (stacked) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [intro, const SizedBox(height: 32), image],
            );
          }

          return Row(
            children: [
              Expanded(child: intro),
              const SizedBox(width: 44),
              Expanded(child: image),
            ],
          );
        },
      ),
    );
  }
}

class PublicInfoBand extends StatelessWidget {
  const PublicInfoBand({
    required this.title,
    required this.description,
    required this.icon,
    this.action,
    super.key,
  });

  final String title;
  final String description;
  final IconData icon;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: PublicSiteColors.primaryDark,
        borderRadius: BorderRadius.circular(8),
      ),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final stacked = constraints.maxWidth < 720;
          final content = Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox.square(
                dimension: 48,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(icon, color: Colors.white),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: theme.textTheme.headlineSmall?.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      description,
                      style: theme.textTheme.bodyLarge?.copyWith(
                        color: const Color(0xFFD9E7FF),
                        height: 1.5,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          );

          if (stacked || action == null) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                content,
                if (action != null) ...[const SizedBox(height: 20), action!],
              ],
            );
          }

          return Row(
            children: [
              Expanded(child: content),
              const SizedBox(width: 24),
              action!,
            ],
          );
        },
      ),
    );
  }
}

class PublicOutlineList extends StatelessWidget {
  const PublicOutlineList({required this.items, required this.icon, super.key});

  final List<String> items;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      children: [
        for (final item in items)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(icon, size: 21, color: PublicSiteColors.green),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    item,
                    style: theme.textTheme.bodyLarge?.copyWith(
                      color: PublicSiteColors.text,
                      height: 1.45,
                    ),
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

class PublicActionButton extends StatelessWidget {
  const PublicActionButton({
    required this.label,
    required this.onPressed,
    this.icon = Icons.arrow_forward,
    this.leading,
    this.expand = false,
    super.key,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData icon;
  final Widget? leading;
  final bool expand;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: expand ? double.infinity : null,
      height: 48,
      child: FilledButton.icon(
        onPressed: onPressed,
        icon: leading ?? Icon(icon, size: 20),
        label: Text(label, overflow: TextOverflow.ellipsis),
        style: FilledButton.styleFrom(
          backgroundColor: PublicSiteColors.primary,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
    );
  }
}

class PublicSecondaryButton extends StatelessWidget {
  const PublicSecondaryButton({
    required this.label,
    required this.onPressed,
    this.icon = Icons.arrow_forward,
    super.key,
  });

  final String label;
  final VoidCallback onPressed;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 48,
      child: OutlinedButton.icon(
        onPressed: onPressed,
        icon: Icon(icon, size: 20),
        label: Text(label, overflow: TextOverflow.ellipsis),
        style: OutlinedButton.styleFrom(
          foregroundColor: PublicSiteColors.primary,
          side: const BorderSide(color: PublicSiteColors.primary),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
    );
  }
}

class ContactInfoList extends StatelessWidget {
  const ContactInfoList({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _ContactInfoRow(
          icon: Icons.place_outlined,
          title: 'Address',
          value: schoolProfile.address,
        ),
        _ContactInfoRow(
          icon: Icons.call_outlined,
          title: 'Phone',
          value: schoolProfile.phone,
        ),
        _ContactInfoRow(
          icon: Icons.mail_outline,
          title: 'Email',
          value: schoolProfile.email,
        ),
        _ContactInfoRow(
          icon: Icons.schedule_outlined,
          title: 'Office Hours',
          value: schoolProfile.officeHours,
        ),
      ],
    );
  }
}

class MapPlaceholder extends StatelessWidget {
  const MapPlaceholder({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      width: double.infinity,
      constraints: const BoxConstraints(minHeight: 300),
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: const Color(0xFFE8F3F1),
        border: Border.all(color: PublicSiteColors.border),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.map_outlined,
            color: PublicSiteColors.green,
            size: 52,
          ),
          const SizedBox(height: 14),
          Text(
            'Map placeholder',
            style: theme.textTheme.titleMedium?.copyWith(
              color: PublicSiteColors.ink,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            schoolProfile.address,
            textAlign: TextAlign.center,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: PublicSiteColors.muted,
              height: 1.45,
            ),
          ),
        ],
      ),
    );
  }
}

class _ContactInfoRow extends StatelessWidget {
  const _ContactInfoRow({
    required this.icon,
    required this.title,
    required this.value,
  });

  final IconData icon;
  final String title;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.only(bottom: 18),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox.square(
            dimension: 42,
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: PublicSiteColors.primary.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(icon, color: PublicSiteColors.primary),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: theme.textTheme.labelLarge?.copyWith(
                    color: PublicSiteColors.ink,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  value,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: PublicSiteColors.muted,
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Tag extends StatelessWidget {
  const _Tag({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: PublicSiteColors.green.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelMedium?.copyWith(
          color: PublicSiteColors.green,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _ImageFallback extends StatelessWidget {
  const _ImageFallback({required this.label, this.loading = false});

  final String label;
  final bool loading;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return ColoredBox(
      color: const Color(0xFFE8F3F1),
      child: Center(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (loading)
                const SizedBox.square(
                  dimension: 28,
                  child: CircularProgressIndicator(strokeWidth: 2.4),
                )
              else
                const Icon(
                  Icons.image_outlined,
                  color: PublicSiteColors.green,
                  size: 34,
                ),
              const SizedBox(height: 10),
              Text(
                label,
                textAlign: TextAlign.center,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: PublicSiteColors.muted,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
