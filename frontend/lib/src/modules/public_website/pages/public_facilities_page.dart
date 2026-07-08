import 'package:flutter/material.dart';

import '../../../app/router/app_routes.dart';
import '../data/public_website_data.dart';
import '../widgets/public_site_layout.dart';
import '../widgets/public_site_widgets.dart';

class PublicFacilitiesPage extends StatelessWidget {
  const PublicFacilitiesPage({super.key});

  @override
  Widget build(BuildContext context) {
    return PublicWebsiteLayout(
      currentPath: AppRoutes.publicFacilities,
      child: Column(
        children: [
          const PublicPageHero(
            title: 'Facilities',
            subtitle:
                'Transport, hostel, library, computer lab, sports ground, digital classrooms, safety, and CCTV.',
            icon: Icons.apartment_outlined,
          ),
          PublicSection(
            eyebrow: 'Campus facilities',
            title: 'Spaces and services that support daily school life.',
            child: PublicResponsiveGrid(
              children: [
                for (final facility in facilities)
                  PublicFeatureCard(
                    title: facility.title,
                    description: facility.description,
                    icon: facility.icon,
                    color: facility.color,
                  ),
              ],
            ),
          ),
          PublicSection(
            backgroundColor: Colors.white,
            child: const PublicInfoBand(
              title: 'Student safety comes first',
              description:
                  'Visitor entry, transport discipline, staff supervision, campus CCTV, and clear routines help keep students secure throughout the day.',
              icon: Icons.health_and_safety_outlined,
            ),
          ),
        ],
      ),
    );
  }
}
