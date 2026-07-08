import 'package:flutter/material.dart';

import '../../../app/router/app_routes.dart';
import '../data/public_website_data.dart';
import '../widgets/public_site_layout.dart';
import '../widgets/public_site_widgets.dart';

class PublicGalleryPage extends StatelessWidget {
  const PublicGalleryPage({super.key});

  @override
  Widget build(BuildContext context) {
    return PublicWebsiteLayout(
      currentPath: AppRoutes.publicGallery,
      child: Column(
        children: [
          const PublicPageHero(
            title: 'Gallery',
            subtitle:
                'Responsive placeholder image grid for campus life, classrooms, library, sports, and activities.',
            icon: Icons.photo_library_outlined,
          ),
          PublicSection(
            eyebrow: 'Campus moments',
            title: 'Replace these placeholders with real school images later.',
            child: PublicResponsiveGrid(
              maxColumns: 3,
              minItemWidth: 260,
              children: [
                for (final item in galleryItems) PublicGalleryTile(item: item),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
