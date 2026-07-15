import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/router/app_routes.dart';
import '../services/public_enquiry_service.dart';
import '../widgets/public_enquiry_form.dart';
import '../widgets/public_site_layout.dart';
import '../widgets/public_site_widgets.dart';

class PublicContactPage extends ConsumerWidget {
  const PublicContactPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final enquiryService = ref.watch(publicEnquiryServiceProvider);

    return PublicWebsiteLayout(
      currentPath: AppRoutes.publicContact,
      child: Column(
        children: [
          const PublicPageHero(
            title: 'Contact Us',
            subtitle:
                'School address, phone number, email, map placeholder, and admission enquiry form.',
            icon: Icons.contact_mail_outlined,
          ),
          PublicSection(
            eyebrow: 'Reach us',
            title: 'The admission office is ready to help.',
            child: LayoutBuilder(
              builder: (context, constraints) {
                final stacked = constraints.maxWidth < 900;
                const info = Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    ContactInfoList(),
                    SizedBox(height: 12),
                    MapPlaceholder(),
                  ],
                );
                final form = PublicEnquiryForm(enquiryService: enquiryService);

                if (stacked) {
                  return Column(
                    children: [info, const SizedBox(height: 24), form],
                  );
                }

                return Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(child: info),
                    const SizedBox(width: 32),
                    Expanded(child: form),
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
