import 'package:flutter/material.dart';

import '../../../app/router/app_routes.dart';
import '../widgets/public_enquiry_form.dart';
import '../widgets/public_site_layout.dart';
import '../widgets/public_site_widgets.dart';

class PublicContactPage extends StatelessWidget {
  const PublicContactPage({super.key});

  @override
  Widget build(BuildContext context) {
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
                const form = PublicEnquiryForm();

                if (stacked) {
                  return Column(
                    children: [
                      info,
                      const SizedBox(height: 24),
                      form,
                    ],
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
