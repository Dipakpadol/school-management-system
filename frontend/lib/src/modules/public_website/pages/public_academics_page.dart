import 'package:flutter/material.dart';

import '../../../app/router/app_routes.dart';
import '../data/public_website_data.dart';
import '../widgets/public_site_layout.dart';
import '../widgets/public_site_theme.dart';
import '../widgets/public_site_widgets.dart';

class PublicAcademicsPage extends StatelessWidget {
  const PublicAcademicsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return PublicWebsiteLayout(
      currentPath: AppRoutes.publicAcademics,
      child: Column(
        children: [
          const PublicPageHero(
            title: 'Academics',
            subtitle:
                'Academic planning, classes, subjects, activities, examinations, and result communication.',
            icon: Icons.menu_book_outlined,
          ),
          PublicSection(
            backgroundColor: Colors.white,
            child: const PublicInfoBand(
              title: 'Academic Year 2026-27',
              description:
                  'The academic year is planned with term-wise learning goals, activities, assessments, parent meetings, and result publication through the ERP workflow.',
              icon: Icons.calendar_month_outlined,
            ),
          ),
          PublicSection(
            eyebrow: 'Classes and divisions',
            title: 'A school structure ready for ERP-backed data.',
            child: PublicResponsiveGrid(
              maxColumns: 4,
              minItemWidth: 220,
              children: const [
                PublicFeatureCard(
                  title: 'Pre-primary',
                  description: 'Nursery, LKG, UKG with play-based learning.',
                  icon: Icons.child_care_outlined,
                  color: PublicSiteColors.gold,
                ),
                PublicFeatureCard(
                  title: 'Primary',
                  description: 'Classes I - V with strong academic basics.',
                  icon: Icons.school_outlined,
                  color: PublicSiteColors.primary,
                ),
                PublicFeatureCard(
                  title: 'Middle',
                  description: 'Classes VI - VIII with labs and clubs.',
                  icon: Icons.science_outlined,
                  color: PublicSiteColors.green,
                ),
                PublicFeatureCard(
                  title: 'Secondary',
                  description: 'Classes IX - XII with board preparation.',
                  icon: Icons.workspace_premium_outlined,
                  color: PublicSiteColors.violet,
                ),
              ],
            ),
          ),
          PublicSection(
            eyebrow: 'Subjects and activities',
            title: 'A balanced curriculum.',
            backgroundColor: Colors.white,
            child: PublicOutlineList(
              icon: Icons.task_alt_outlined,
              items: academicSubjects,
            ),
          ),
          PublicSection(
            eyebrow: 'Examination and results',
            title: 'Assessment with clarity.',
            child: PublicResponsiveGrid(
              maxColumns: 3,
              children: const [
                PublicFeatureCard(
                  title: 'Periodic Assessment',
                  description:
                      'Class tests, assignments, projects, and notebook reviews help track learning regularly.',
                  icon: Icons.fact_check_outlined,
                  color: PublicSiteColors.primary,
                ),
                PublicFeatureCard(
                  title: 'Term Examination',
                  description:
                      'Term-wise examinations are scheduled with marks entry and result preparation workflows.',
                  icon: Icons.assignment_outlined,
                  color: PublicSiteColors.coral,
                ),
                PublicFeatureCard(
                  title: 'Result Communication',
                  description:
                      'Parents receive progress updates, report cards, and follow-up guidance through school channels.',
                  icon: Icons.insights_outlined,
                  color: PublicSiteColors.green,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
