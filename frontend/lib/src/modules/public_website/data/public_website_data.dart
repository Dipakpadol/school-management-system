import 'package:flutter/material.dart';

import '../../../app/router/app_routes.dart';
import '../models/public_website_models.dart';

abstract final class PublicWebsiteAssets {
  static const schoolBuilding = 'assets/images/website/school-building.jpg';
  static const schoolGroup = 'assets/images/website/school-group.jpg';
  static const officePractical = 'assets/images/website/office-practical.jpg';
  static const staffTeam = 'assets/images/website/staff-team.jpg';
  static const staffTeamCropped =
      'assets/images/website/staff-team-cropped.jpg';
  static const sportsPractice = 'assets/images/website/sports-practice.jpg';
  static const sportsGroundAction =
      'assets/images/website/sports-ground-action.jpg';
  static const culturalProgram = 'assets/images/website/cultural-program.jpg';
  static const medicalRoleplay = 'assets/images/website/medical-roleplay.jpg';
  static const scienceExhibition =
      'assets/images/website/science-exhibition.jpg';
  static const scienceExhibitionGuests =
      'assets/images/website/science-exhibition-guests.jpg';
  static const scienceExhibitionPresentation =
      'assets/images/website/science-exhibition-presentation.jpg';
  static const scienceExhibitionDisplay =
      'assets/images/website/science-exhibition-display.jpg';
  static const classGroup = 'assets/images/website/class-group.jpg';
  static const educationalTrip = 'assets/images/website/educational-trip.jpg';
  static const campusEvent = 'assets/images/website/campus-event.jpg';
  static const welcomeBannerWide =
      'assets/images/website/welcome-banner-wide.jpg';
  static const welcomePoster = 'assets/images/website/welcome-poster.jpg';
  static const schoolCommunityCropped =
      'assets/images/website/school-community-cropped.jpg';
  static const awardNotice = 'assets/images/website/award-notice.jpg';
  static const ashadhiCelebration =
      'assets/images/website/ashadhi-celebration.jpg';
}

const schoolProfile = SchoolProfile(
  name: 'Star International School',
  tagline: 'Learning with character, curiosity, and care.',
  admissionText:
      'Admissions are open for Nursery to Class X for the current academic year.',
  address:
      'Kandari pati, Waghrul, Taluka: Badnapur, District: Jalna, Maharashtra, India, 431202',
  phone: '',
  email: 'info@starinternationalschool.com',
  officeHours: 'Monday to Saturday, 8:30 AM - 4:30 PM',
  heroImageAsset: PublicWebsiteAssets.schoolBuilding,
  socialLinks: [],
);

const publicNavItems = [
  PublicNavItem(
    label: 'Home',
    path: AppRoutes.publicHome,
    icon: Icons.home_outlined,
  ),
  PublicNavItem(
    label: 'About',
    path: AppRoutes.publicAbout,
    icon: Icons.info_outline,
  ),
  PublicNavItem(
    label: 'Admissions',
    path: AppRoutes.publicAdmissions,
    icon: Icons.assignment_turned_in_outlined,
  ),
  PublicNavItem(
    label: 'Academics',
    path: AppRoutes.publicAcademics,
    icon: Icons.menu_book_outlined,
  ),
  PublicNavItem(
    label: 'Facilities',
    path: AppRoutes.publicFacilities,
    icon: Icons.apartment_outlined,
  ),
  PublicNavItem(
    label: 'Gallery',
    path: AppRoutes.publicGallery,
    icon: Icons.photo_library_outlined,
  ),
  PublicNavItem(
    label: 'Events',
    path: AppRoutes.publicEvents,
    icon: Icons.event_note_outlined,
  ),
  PublicNavItem(
    label: 'Contact',
    path: AppRoutes.publicContact,
    icon: Icons.contact_mail_outlined,
  ),
];

const highlights = [
  HighlightItem(
    title: 'Experienced Teachers',
    description: 'Mentors who blend strong subject knowledge with care.',
    icon: Icons.badge_outlined,
    color: Color(0xFF2563EB),
  ),
  HighlightItem(
    title: 'Digital Classrooms',
    description: 'Interactive learning spaces for visual and hands-on study.',
    icon: Icons.cast_for_education_outlined,
    color: Color(0xFF0F766E),
  ),
  HighlightItem(
    title: 'Transport Facility',
    description: 'Route-managed buses with trained staff and tracking.',
    icon: Icons.directions_bus_outlined,
    color: Color(0xFFB45309),
  ),
  HighlightItem(
    title: 'Hostel Facility',
    description: 'Supervised residential support with healthy routines.',
    icon: Icons.apartment_outlined,
    color: Color(0xFF7C3AED),
  ),
  HighlightItem(
    title: 'Library',
    description: 'Reading corners, references, periodicals, and quiet study.',
    icon: Icons.local_library_outlined,
    color: Color(0xFFBE123C),
  ),
  HighlightItem(
    title: 'Sports & Activities',
    description: 'Athletics, games, clubs, arts, and leadership programs.',
    icon: Icons.sports_soccer_outlined,
    color: Color(0xFF15803D),
  ),
];

const facilities = [
  FacilityItem(
    title: 'Transport',
    description:
        'GPS-ready bus routes, attendant support, and planned pickup points.',
    imageAsset: PublicWebsiteAssets.schoolBuilding,
    icon: Icons.directions_bus_filled_outlined,
    color: Color(0xFF2563EB),
  ),
  FacilityItem(
    title: 'Hostel',
    description:
        'Separate supervised residences with nutritious meals and study hours.',
    imageAsset: PublicWebsiteAssets.schoolGroup,
    icon: Icons.meeting_room_outlined,
    color: Color(0xFF7C3AED),
  ),
  FacilityItem(
    title: 'Library',
    description:
        'Age-wise reading collections, reference material, and digital catalog.',
    imageAsset: PublicWebsiteAssets.classGroup,
    icon: Icons.local_library_outlined,
    color: Color(0xFFBE123C),
  ),
  FacilityItem(
    title: 'Computer Lab',
    description:
        'Modern systems for coding, digital literacy, research, and projects.',
    imageAsset: PublicWebsiteAssets.scienceExhibition,
    icon: Icons.computer_outlined,
    color: Color(0xFF0F766E),
  ),
  FacilityItem(
    title: 'Sports Ground',
    description:
        'Outdoor fields and indoor games for fitness, teamwork, and discipline.',
    imageAsset: PublicWebsiteAssets.sportsPractice,
    icon: Icons.sports_cricket_outlined,
    color: Color(0xFF15803D),
  ),
  FacilityItem(
    title: 'Digital Classroom',
    description:
        'Smart boards and multimedia support for engaging daily lessons.',
    imageAsset: PublicWebsiteAssets.educationalTrip,
    icon: Icons.connected_tv_outlined,
    color: Color(0xFFB45309),
  ),
  FacilityItem(
    title: 'Safety and CCTV',
    description:
        'Visitor checks, supervised entry points, CCTV, and trained staff.',
    imageAsset: PublicWebsiteAssets.campusEvent,
    icon: Icons.health_and_safety_outlined,
    color: Color(0xFF4338CA),
  ),
];

const events = [
  PublicEventItem(
    title: 'Admission enquiry desk is open',
    category: 'Notice',
    infoLabel: 'Office notice',
    description:
        'Parents can visit or contact the school office for admission guidance and application details.',
  ),
  PublicEventItem(
    title: 'Inter-house sports selections',
    category: 'Event',
    infoLabel: 'School calendar',
    description:
        'Sports selections, practice schedules, and house events are published through school circulars.',
  ),
  PublicEventItem(
    title: 'Parent orientation program',
    category: 'Circular',
    infoLabel: 'Parent update',
    description:
        'Orientation sessions introduce academic planning, ERP access, communication channels, and safety policies.',
  ),
  PublicEventItem(
    title: 'Science exhibition registrations',
    category: 'Announcement',
    infoLabel: 'Activity notice',
    description:
        'Science exhibition and project activity details are shared with eligible classes through circulars.',
  ),
];

const galleryItems = [
  GalleryItem(
    title: 'School Building',
    category: 'Campus Life',
    imageAsset: PublicWebsiteAssets.schoolBuilding,
  ),
  GalleryItem(
    title: 'School Community',
    category: 'Campus Life',
    imageAsset: PublicWebsiteAssets.schoolGroup,
  ),
  GalleryItem(
    title: 'Learning Activity',
    category: 'Academics',
    imageAsset: PublicWebsiteAssets.officePractical,
  ),
  GalleryItem(
    title: 'Outdoor Games',
    category: 'Sports',
    imageAsset: PublicWebsiteAssets.sportsGroundAction,
  ),
  GalleryItem(
    title: 'Staff and Guests',
    category: 'Campus Life',
    imageAsset: PublicWebsiteAssets.staffTeam,
  ),
  GalleryItem(
    title: 'Staff Group',
    category: 'Campus Life',
    imageAsset: PublicWebsiteAssets.staffTeamCropped,
  ),
  GalleryItem(
    title: 'Welcome Display',
    category: 'Campus Life',
    imageAsset: PublicWebsiteAssets.welcomeBannerWide,
  ),
  GalleryItem(
    title: 'Welcome Poster',
    category: 'Campus Life',
    imageAsset: PublicWebsiteAssets.welcomePoster,
  ),
  GalleryItem(
    title: 'Science Exhibition',
    category: 'Academics',
    imageAsset: PublicWebsiteAssets.scienceExhibition,
  ),
  GalleryItem(
    title: 'Science Guests',
    category: 'Academics',
    imageAsset: PublicWebsiteAssets.scienceExhibitionGuests,
  ),
  GalleryItem(
    title: 'Science Presentation',
    category: 'Academics',
    imageAsset: PublicWebsiteAssets.scienceExhibitionPresentation,
  ),
  GalleryItem(
    title: 'Science Display',
    category: 'Academics',
    imageAsset: PublicWebsiteAssets.scienceExhibitionDisplay,
  ),
  GalleryItem(
    title: 'Sports Practice',
    category: 'Sports',
    imageAsset: PublicWebsiteAssets.sportsPractice,
  ),
  GalleryItem(
    title: 'Cultural Program',
    category: 'Activities',
    imageAsset: PublicWebsiteAssets.culturalProgram,
  ),
  GalleryItem(
    title: 'Class Group',
    category: 'Activities',
    imageAsset: PublicWebsiteAssets.classGroup,
  ),
  GalleryItem(
    title: 'Community Group',
    category: 'Campus Life',
    imageAsset: PublicWebsiteAssets.schoolCommunityCropped,
  ),
  GalleryItem(
    title: 'Student Role Play',
    category: 'Activities',
    imageAsset: PublicWebsiteAssets.medicalRoleplay,
  ),
  GalleryItem(
    title: 'Achievement Notice',
    category: 'Achievements',
    imageAsset: PublicWebsiteAssets.awardNotice,
  ),
  GalleryItem(
    title: 'Ashadhi Celebration',
    category: 'Activities',
    imageAsset: PublicWebsiteAssets.ashadhiCelebration,
  ),
  GalleryItem(
    title: 'Educational Trip',
    category: 'Activities',
    imageAsset: PublicWebsiteAssets.educationalTrip,
  ),
  GalleryItem(
    title: 'Campus Event',
    category: 'Campus Life',
    imageAsset: PublicWebsiteAssets.campusEvent,
  ),
];

const admissionClasses = [
  AdmissionClass(
    name: 'Pre-primary',
    ageGroup: 'Nursery, LKG, UKG',
    description: 'Play-based foundation with language, numeracy, and habits.',
  ),
  AdmissionClass(
    name: 'Primary',
    ageGroup: 'Classes I - V',
    description: 'Core literacy, numeracy, environmental studies, and arts.',
  ),
  AdmissionClass(
    name: 'Middle School',
    ageGroup: 'Classes VI - VIII',
    description: 'Subject depth, clubs, labs, projects, and guided learning.',
  ),
  AdmissionClass(
    name: 'Secondary and Senior Secondary',
    ageGroup: 'Classes IX - XII',
    description: 'Board preparation, streams, practicals, and career guidance.',
  ),
];

const admissionSteps = [
  'Submit the admission enquiry form or visit the school office.',
  'Meet the admission counselor and collect the application details.',
  'Complete document verification and class readiness interaction.',
  'Confirm admission by completing fee payment and ERP onboarding.',
];

const requiredDocuments = [
  'Birth certificate',
  'Transfer certificate, if applicable',
  'Previous class report card',
  'Aadhaar or identity proof',
  'Passport-size photographs',
  'Address proof',
];

const academicSubjects = [
  'English, Hindi, Mathematics, Science, and Social Studies',
  'Computer education, general knowledge, value education, and art',
  'Sports, yoga, music, dance, clubs, and house activities',
  'Project work, practical learning, examinations, and remedial support',
];

const whyChooseUs = [
  'Balanced focus on academics, character, creativity, and wellbeing.',
  'Safe campus with attentive staff and structured daily routines.',
  'Parent communication through the existing School ERP platform.',
  'Regular assessments, mentoring, enrichment, and activity programs.',
];
