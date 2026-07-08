import 'package:flutter/material.dart';

class PublicNavItem {
  const PublicNavItem({
    required this.label,
    required this.path,
    required this.icon,
  });

  final String label;
  final String path;
  final IconData icon;
}

class SchoolProfile {
  const SchoolProfile({
    required this.name,
    required this.tagline,
    required this.admissionText,
    required this.address,
    required this.phone,
    required this.email,
    required this.officeHours,
    required this.heroImageUrl,
    required this.socialLinks,
  });

  final String name;
  final String tagline;
  final String admissionText;
  final String address;
  final String phone;
  final String email;
  final String officeHours;
  final String heroImageUrl;
  final List<SocialLink> socialLinks;
}

class SocialLink {
  const SocialLink({
    required this.label,
    required this.icon,
  });

  final String label;
  final IconData icon;
}

class HighlightItem {
  const HighlightItem({
    required this.title,
    required this.description,
    required this.icon,
    required this.color,
  });

  final String title;
  final String description;
  final IconData icon;
  final Color color;
}

class FacilityItem {
  const FacilityItem({
    required this.title,
    required this.description,
    required this.icon,
    required this.color,
  });

  final String title;
  final String description;
  final IconData icon;
  final Color color;
}

class PublicEventItem {
  const PublicEventItem({
    required this.title,
    required this.category,
    required this.dateLabel,
    required this.description,
  });

  final String title;
  final String category;
  final String dateLabel;
  final String description;
}

class GalleryItem {
  const GalleryItem({
    required this.title,
    required this.category,
    required this.imageUrl,
  });

  final String title;
  final String category;
  final String imageUrl;
}

class AdmissionClass {
  const AdmissionClass({
    required this.name,
    required this.ageGroup,
    required this.description,
  });

  final String name;
  final String ageGroup;
  final String description;
}

class EnquiryRequest {
  const EnquiryRequest({
    required this.studentName,
    required this.parentName,
    required this.mobileNumber,
    required this.email,
    required this.classInterested,
    required this.message,
  });

  final String studentName;
  final String parentName;
  final String mobileNumber;
  final String email;
  final String classInterested;
  final String message;

  Map<String, String> toJson() {
    return {
      'studentName': studentName,
      'parentName': parentName,
      'mobileNumber': mobileNumber,
      'email': email,
      'classInterested': classInterested,
      'message': message,
    };
  }
}
