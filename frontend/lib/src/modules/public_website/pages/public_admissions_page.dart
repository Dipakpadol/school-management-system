import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/router/app_routes.dart';
import '../data/public_website_data.dart';
import '../services/public_enquiry_service.dart';
import '../widgets/public_enquiry_form.dart';
import '../widgets/public_site_layout.dart';
import '../widgets/public_site_theme.dart';
import '../widgets/public_site_widgets.dart';

class PublicAdmissionsPage extends ConsumerWidget {
  const PublicAdmissionsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final enquiryService = ref.watch(publicEnquiryServiceProvider);

    return PublicWebsiteLayout(
      currentPath: AppRoutes.publicAdmissions,
      child: Column(
        children: [
          const PublicPageHero(
            title: 'Admissions',
            subtitle:
                'A simple admission journey for new families, with a ready-to-integrate enquiry form.',
            icon: Icons.assignment_turned_in_outlined,
          ),
          PublicSection(
            backgroundColor: Colors.white,
            child: PublicInfoBand(
              title: 'Admissions Open',
              description: schoolProfile.admissionText,
              icon: Icons.campaign_outlined,
            ),
          ),
          PublicSection(
            eyebrow: 'Process',
            title: 'Admission process steps.',
            child: PublicResponsiveGrid(
              children: [
                for (var index = 0; index < admissionSteps.length; index++)
                  _StepCard(
                    stepNumber: index + 1,
                    description: admissionSteps[index],
                  ),
              ],
            ),
          ),
          PublicSection(
            eyebrow: 'Classes available',
            title: 'Admissions are available across school levels.',
            backgroundColor: Colors.white,
            child: PublicResponsiveGrid(
              maxColumns: 4,
              minItemWidth: 220,
              children: [
                for (final admissionClass in admissionClasses)
                  PublicFeatureCard(
                    title: admissionClass.name,
                    description:
                        '${admissionClass.ageGroup}\n${admissionClass.description}',
                    icon: Icons.school_outlined,
                    color: PublicSiteColors.primary,
                  ),
              ],
            ),
          ),
          PublicSection(
            eyebrow: 'Required documents',
            title: 'Keep these documents ready.',
            child: PublicOutlineList(
              icon: Icons.description_outlined,
              items: requiredDocuments,
            ),
          ),
          PublicSection(
            eyebrow: 'Enquiry form',
            title: 'Send an admission enquiry.',
            subtitle:
                'The admission office will receive your enquiry and follow up.',
            backgroundColor: Colors.white,
            child: PublicEnquiryForm(enquiryService: enquiryService),
          ),
        ],
      ),
    );
  }
}

class _StepCard extends StatelessWidget {
  const _StepCard({required this.stepNumber, required this.description});

  final int stepNumber;
  final String description;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            CircleAvatar(
              backgroundColor: PublicSiteColors.green,
              foregroundColor: Colors.white,
              child: Text('$stepNumber'),
            ),
            const SizedBox(height: 16),
            Text(
              description,
              style: theme.textTheme.bodyLarge?.copyWith(
                color: PublicSiteColors.text,
                height: 1.5,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
