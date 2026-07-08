import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router/app_routes.dart';
import '../data/public_website_data.dart';
import '../widgets/public_site_layout.dart';
import '../widgets/public_site_theme.dart';
import '../widgets/public_site_widgets.dart';

class PublicHomePage extends StatelessWidget {
  const PublicHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return PublicWebsiteLayout(
      currentPath: AppRoutes.publicHome,
      child: Column(
        children: [
          const _HomeHero(),
          PublicSection(
            eyebrow: 'About the school',
            title: 'A caring campus for confident learners.',
            subtitle:
                'Green Valley Public School combines strong academics, values, activity-based learning, and parent communication through the School ERP platform.',
            backgroundColor: Colors.white,
            child: LayoutBuilder(
              builder: (context, constraints) {
                final stacked = constraints.maxWidth < 820;
                final image = const PublicNetworkImage(
                  imageUrl:
                      'https://images.unsplash.com/photo-1577896851231-70ef18881754?auto=format&fit=crop&w=1200&q=80',
                  label: 'Classroom learning',
                );
                final content = Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const PublicOutlineList(
                      icon: Icons.check_circle_outline,
                      items: [
                        'Student-centered teaching for every stage of school life.',
                        'Modern classrooms, library, labs, transport, hostel, and sports support.',
                        'Clear communication with parents for attendance, fees, notices, and progress.',
                      ],
                    ),
                    const SizedBox(height: 18),
                    PublicSecondaryButton(
                      label: 'Learn More',
                      icon: Icons.info_outline,
                      onPressed: () => context.go(AppRoutes.publicAbout),
                    ),
                  ],
                );

                if (stacked) {
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      AspectRatio(aspectRatio: 16 / 10, child: image),
                      const SizedBox(height: 28),
                      content,
                    ],
                  );
                }

                return Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Expanded(
                      child: AspectRatio(aspectRatio: 16 / 10, child: image),
                    ),
                    const SizedBox(width: 42),
                    Expanded(child: content),
                  ],
                );
              },
            ),
          ),
          PublicSection(
            eyebrow: 'Highlights',
            title: 'Everything students need to learn, grow, and belong.',
            subtitle:
                'The public site starts with dummy content, but the structure is ready for real school data and future APIs.',
            child: PublicResponsiveGrid(
              children: [
                for (final item in highlights)
                  PublicFeatureCard(
                    title: item.title,
                    description: item.description,
                    icon: item.icon,
                    color: item.color,
                  ),
              ],
            ),
          ),
          PublicSection(
            backgroundColor: Colors.white,
            child: PublicInfoBand(
              title: 'Admissions Open',
              description: schoolProfile.admissionText,
              icon: Icons.campaign_outlined,
              action: PublicActionButton(
                label: 'Admission Enquiry',
                icon: Icons.assignment_turned_in_outlined,
                onPressed: () => context.go(AppRoutes.publicAdmissions),
              ),
            ),
          ),
          PublicSection(
            eyebrow: 'Events and announcements',
            title: 'What is happening on campus.',
            subtitle:
                'Notices, circulars, and events are represented with dummy data for now.',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                PublicResponsiveGrid(
                  children: [
                    for (final event in events.take(3))
                      PublicEventCard(event: event, compact: true),
                  ],
                ),
                const SizedBox(height: 22),
                PublicSecondaryButton(
                  label: 'View All Events',
                  icon: Icons.event_note_outlined,
                  onPressed: () => context.go(AppRoutes.publicEvents),
                ),
              ],
            ),
          ),
          PublicSection(
            eyebrow: 'Gallery preview',
            title: 'A quick look at school life.',
            backgroundColor: Colors.white,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                PublicResponsiveGrid(
                  children: [
                    for (final item in galleryItems.take(3))
                      PublicGalleryTile(item: item),
                  ],
                ),
                const SizedBox(height: 22),
                PublicSecondaryButton(
                  label: 'Open Gallery',
                  icon: Icons.photo_library_outlined,
                  onPressed: () => context.go(AppRoutes.publicGallery),
                ),
              ],
            ),
          ),
          PublicSection(
            eyebrow: 'Contact',
            title: 'Visit or contact the admission office.',
            child: LayoutBuilder(
              builder: (context, constraints) {
                final stacked = constraints.maxWidth < 820;
                final info = Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const ContactInfoList(),
                    PublicActionButton(
                      label: 'Contact Us',
                      icon: Icons.contact_mail_outlined,
                      onPressed: () => context.go(AppRoutes.publicContact),
                    ),
                  ],
                );

                if (stacked) {
                  return Column(
                    children: [
                      info,
                      const SizedBox(height: 24),
                      const MapPlaceholder(),
                    ],
                  );
                }

                return Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(child: info),
                    const SizedBox(width: 32),
                    const Expanded(child: MapPlaceholder()),
                  ],
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _HomeHero extends StatelessWidget {
  const _HomeHero();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final media = MediaQuery.sizeOf(context);
    final compact = media.width < 720;
    final height = (media.height * (compact ? 0.62 : 0.68)).clamp(
      compact ? 420.0 : 500.0,
      compact ? 520.0 : 640.0,
    );

    return SizedBox(
      height: height,
      width: double.infinity,
      child: Stack(
        fit: StackFit.expand,
        children: [
          Image.network(
            schoolProfile.heroImageUrl,
            fit: BoxFit.cover,
            errorBuilder: (context, error, stackTrace) {
              return const ColoredBox(color: PublicSiteColors.primaryDark);
            },
          ),
          DecoratedBox(
            decoration: BoxDecoration(
              color: Colors.black.withValues(alpha: 0.42),
            ),
          ),
          Align(
            alignment: Alignment.center,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 1180),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 780),
                      child: Text(
                        schoolProfile.name,
                        style: theme.textTheme.displayMedium?.copyWith(
                          color: Colors.white,
                          fontWeight: FontWeight.w900,
                          height: 1.05,
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 650),
                      child: Text(
                        schoolProfile.tagline,
                        style: theme.textTheme.titleLarge?.copyWith(
                          color: const Color(0xFFE5EEF9),
                          height: 1.45,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                    const SizedBox(height: 28),
                    Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: [
                        PublicActionButton(
                          label: 'Admission Enquiry',
                          icon: Icons.assignment_turned_in_outlined,
                          onPressed: () =>
                              context.go(AppRoutes.publicAdmissions),
                        ),
                        SizedBox(
                          height: 48,
                          child: OutlinedButton.icon(
                            onPressed: () =>
                                context.go(AppRoutes.publicAcademics),
                            icon: const Icon(Icons.menu_book_outlined),
                            label: const Text('Academics'),
                            style: OutlinedButton.styleFrom(
                              foregroundColor: Colors.white,
                              side: const BorderSide(color: Colors.white),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(8),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
