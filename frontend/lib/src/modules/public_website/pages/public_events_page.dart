import 'package:flutter/material.dart';

import '../../../app/router/app_routes.dart';
import '../data/public_website_data.dart';
import '../widgets/public_site_layout.dart';
import '../widgets/public_site_widgets.dart';

class PublicEventsPage extends StatelessWidget {
  const PublicEventsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return PublicWebsiteLayout(
      currentPath: AppRoutes.publicEvents,
      child: Column(
        children: [
          const PublicPageHero(
            title: 'Events and News',
            subtitle:
                'Announcements, events, circulars, and notices using reusable dummy data for future API integration.',
            icon: Icons.event_note_outlined,
          ),
          PublicSection(
            eyebrow: 'Latest updates',
            title: 'Notices, circulars, events, and announcements.',
            child: PublicResponsiveGrid(
              maxColumns: 2,
              minItemWidth: 320,
              children: [
                for (final event in events) PublicEventCard(event: event),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
