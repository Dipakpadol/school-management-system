import 'package:flutter/material.dart';

import '../../../app/router/app_routes.dart';
import '../data/public_website_data.dart';
import '../widgets/public_site_layout.dart';
import '../widgets/public_site_theme.dart';
import '../widgets/public_site_widgets.dart';

class PublicAboutPage extends StatelessWidget {
  const PublicAboutPage({super.key});

  @override
  Widget build(BuildContext context) {
    return PublicWebsiteLayout(
      currentPath: AppRoutes.publicAbout,
      child: Column(
        children: [
          const PublicPageHero(
            title: 'About Our School',
            subtitle:
                'A public-facing introduction to the school, its values, leadership, and learning environment.',
            icon: Icons.info_outline,
          ),
          PublicSection(
            eyebrow: 'Introduction',
            title: 'Rooted in care, built for modern learning.',
            backgroundColor: Colors.white,
            child: LayoutBuilder(
              builder: (context, constraints) {
                final stacked = constraints.maxWidth < 820;
                final text = Text(
                  'Green Valley Public School is a progressive campus for students from pre-primary to senior secondary classes. The school focuses on academic discipline, curiosity, confidence, and responsible citizenship. Facilities such as digital classrooms, transport, hostel, library, labs, sports spaces, and ERP-enabled communication support a complete school experience.',
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        color: PublicSiteColors.text,
                        height: 1.65,
                      ),
                );

                if (stacked) {
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const AspectRatio(
                        aspectRatio: 16 / 10,
                        child: PublicNetworkImage(
                          imageUrl:
                              'https://images.unsplash.com/photo-1509062522246-3755977927d7?auto=format&fit=crop&w=1200&q=80',
                          label: 'School classroom',
                        ),
                      ),
                      const SizedBox(height: 24),
                      text,
                    ],
                  );
                }

                return Row(
                  children: [
                    const Expanded(
                      child: AspectRatio(
                        aspectRatio: 16 / 10,
                        child: PublicNetworkImage(
                          imageUrl:
                              'https://images.unsplash.com/photo-1509062522246-3755977927d7?auto=format&fit=crop&w=1200&q=80',
                          label: 'School classroom',
                        ),
                      ),
                    ),
                    const SizedBox(width: 42),
                    Expanded(child: text),
                  ],
                );
              },
            ),
          ),
          PublicSection(
            eyebrow: 'Vision and mission',
            title: 'Guided by purpose.',
            child: PublicResponsiveGrid(
              maxColumns: 2,
              children: const [
                PublicFeatureCard(
                  title: 'Vision',
                  description:
                      'To nurture confident, compassionate, and capable learners who can contribute meaningfully to society.',
                  icon: Icons.visibility_outlined,
                  color: PublicSiteColors.primary,
                ),
                PublicFeatureCard(
                  title: 'Mission',
                  description:
                      'To provide strong academics, safe infrastructure, rich activities, and consistent parent partnership.',
                  icon: Icons.flag_outlined,
                  color: PublicSiteColors.green,
                ),
              ],
            ),
          ),
          PublicSection(
            eyebrow: 'Principal message',
            title: 'A message from the principal.',
            backgroundColor: Colors.white,
            child: Card(
              color: Colors.white,
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(
                      Icons.format_quote,
                      color: PublicSiteColors.gold.withValues(alpha: 0.9),
                      size: 42,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'Our school believes that every child can grow when learning is structured, joyful, and humane. We work with parents to help students build knowledge, habits, courage, and respect for others.',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            color: PublicSiteColors.text,
                            height: 1.6,
                          ),
                    ),
                    const SizedBox(height: 18),
                    Text(
                      'Principal, ${schoolProfile.name}',
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: PublicSiteColors.ink,
                            fontWeight: FontWeight.w900,
                          ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          PublicSection(
            eyebrow: 'Why choose us',
            title: 'Practical strengths parents can see.',
            child: PublicOutlineList(
              icon: Icons.check_circle_outline,
              items: whyChooseUs,
            ),
          ),
        ],
      ),
    );
  }
}
