import 'package:flutter/material.dart';

import '../../../app/router/app_routes.dart';
import '../models/public_website_models.dart';

const schoolProfile = SchoolProfile(
  name: 'Star International School',
  tagline: 'Learning with character, curiosity, and care.',
  admissionText:
      'Admissions are open for Nursery to Class X for the 2026-27 academic year.',
  address:
      'Kandari pati, Waghrul, Taluka: Badnapur, District: Jalna, Maharashtra, India, 431202',
  phone: '+91 XXXXXXXXXX',
  email: 'info@starinternationalschool.com',
  officeHours: 'Monday to Saturday, 8:30 AM - 4:30 PM',
  heroImageUrl:
      'https://images.unsplash.com/photo-1523050854058-8df90110c9f1?auto=format&fit=crop&w=1800&q=80',
  socialLinks: [
    SocialLink(label: 'Facebook', icon: Icons.public_outlined),
    SocialLink(label: 'Instagram', icon: Icons.camera_alt_outlined),
    SocialLink(label: 'YouTube', icon: Icons.smart_display_outlined),
  ],
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
    icon: Icons.directions_bus_filled_outlined,
    color: Color(0xFF2563EB),
  ),
  FacilityItem(
    title: 'Hostel',
    description:
        'Separate supervised residences with nutritious meals and study hours.',
    icon: Icons.meeting_room_outlined,
    color: Color(0xFF7C3AED),
  ),
  FacilityItem(
    title: 'Library',
    description:
        'Age-wise reading collections, reference material, and digital catalog.',
    icon: Icons.local_library_outlined,
    color: Color(0xFFBE123C),
  ),
  FacilityItem(
    title: 'Computer Lab',
    description:
        'Modern systems for coding, digital literacy, research, and projects.',
    icon: Icons.computer_outlined,
    color: Color(0xFF0F766E),
  ),
  FacilityItem(
    title: 'Sports Ground',
    description:
        'Outdoor fields and indoor games for fitness, teamwork, and discipline.',
    icon: Icons.sports_cricket_outlined,
    color: Color(0xFF15803D),
  ),
  FacilityItem(
    title: 'Digital Classroom',
    description:
        'Smart boards and multimedia support for engaging daily lessons.',
    icon: Icons.connected_tv_outlined,
    color: Color(0xFFB45309),
  ),
  FacilityItem(
    title: 'Safety and CCTV',
    description:
        'Visitor checks, supervised entry points, CCTV, and trained staff.',
    icon: Icons.health_and_safety_outlined,
    color: Color(0xFF4338CA),
  ),
];

const events = [
  PublicEventItem(
    title: 'Admission enquiry desk is open',
    category: 'Notice',
    dateLabel: 'July 15, 2026',
    description:
        'Parents can visit the school office between 9:00 AM and 3:00 PM for admission guidance.',
  ),
  PublicEventItem(
    title: 'Inter-house sports selections',
    category: 'Event',
    dateLabel: 'August 2, 2026',
    description:
        'Students from Classes VI to XII can register for athletics, football, basketball, and cricket.',
  ),
  PublicEventItem(
    title: 'Parent orientation program',
    category: 'Circular',
    dateLabel: 'August 10, 2026',
    description:
        'An orientation session for new parents will introduce academic planning, ERP access, and safety policies.',
  ),
  PublicEventItem(
    title: 'Science exhibition registrations',
    category: 'Announcement',
    dateLabel: 'August 25, 2026',
    description:
        'Project registrations are open for the annual science exhibition and innovation showcase.',
  ),
];

const galleryItems = [
  GalleryItem(
    title: 'Morning Assembly',
    category: 'Campus Life',
    imageUrl:
        'https://images.unsplash.com/photo-1509062522246-3755977927d7?auto=format&fit=crop&w=900&q=80',
  ),
  GalleryItem(
    title: 'Digital Classroom',
    category: 'Academics',
    imageUrl:
        'https://images.unsplash.com/photo-1588072432836-e10032774350?auto=format&fit=crop&w=900&q=80',
  ),
  GalleryItem(
    title: 'Library Reading Hour',
    category: 'Library',
    imageUrl:
        'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=900&q=80',
  ),
  GalleryItem(
    title: 'Sports Practice',
    category: 'Sports',
    imageUrl:
        'https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=900&q=80',
  ),
  GalleryItem(
    title: 'Science Activity',
    category: 'Activities',
    imageUrl:
        'https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=900&q=80',
  ),
  GalleryItem(
    title: 'Art and Culture',
    category: 'Activities',
    imageUrl:
        'https://images.unsplash.com/photo-1513475382585-d06e58bcb0e0?auto=format&fit=crop&w=900&q=80',
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
