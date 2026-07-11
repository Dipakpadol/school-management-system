import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/controllers/auth_controller.dart';
import '../../features/academic/presentation/pages/academic_management_page.dart';
import '../../features/auth/presentation/pages/forgot_password_page.dart';
import '../../features/auth/presentation/pages/login_page.dart';
import '../../features/auth/presentation/pages/reset_password_page.dart';
import '../../features/auth/presentation/pages/signup_page.dart';
import '../../features/audit_logs/presentation/pages/audit_logs_page.dart';
import '../../features/attendance/presentation/pages/attendance_page.dart';
import '../../features/dashboard/domain/menu_policy.dart';
import '../../features/dashboard/presentation/dashboard_page.dart';
import '../../features/dashboard/presentation/module_placeholder_page.dart';
import '../../features/fees/presentation/pages/fee_structure_form_page.dart';
import '../../features/fees/presentation/pages/fee_assignments_page.dart';
import '../../features/fees/presentation/pages/fee_defaulters_page.dart';
import '../../features/fees/presentation/pages/fees_management_page.dart';
import '../../features/fees/presentation/pages/payment_collection_page.dart';
import '../../features/fees/presentation/pages/student_fee_assignment_page.dart';
import '../../features/exams/presentation/pages/exams_page.dart';
import '../../features/hostel/presentation/pages/hostel_management_page.dart';
import '../../features/notifications/presentation/pages/notification_management_page.dart';
import '../../features/reports/presentation/pages/reports_page.dart';
import '../../features/settings/presentation/pages/settings_page.dart';
import '../../features/settings/presentation/pages/role_permission_page.dart';
import '../../features/students/presentation/pages/student_profile_page.dart';
import '../../features/students/presentation/pages/student_section_detail_page.dart';
import '../../features/students/presentation/pages/student_sections_page.dart';
import '../../features/students/presentation/pages/students_page.dart';
import '../../features/teachers/presentation/pages/teacher_management_page.dart';
import '../../features/teachers/presentation/pages/teacher_attendance_page.dart';
import '../../features/transport/presentation/pages/transport_management_page.dart';
import '../../features/users/presentation/pages/users_page.dart';
import '../../modules/public_website/pages/public_about_page.dart';
import '../../modules/public_website/pages/public_academics_page.dart';
import '../../modules/public_website/pages/public_admissions_page.dart';
import '../../modules/public_website/pages/public_contact_page.dart';
import '../../modules/public_website/pages/public_events_page.dart';
import '../../modules/public_website/pages/public_facilities_page.dart';
import '../../modules/public_website/pages/public_gallery_page.dart';
import '../../modules/public_website/pages/public_home_page.dart';
import 'app_routes.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final authState = ref.watch(authControllerProvider);

  return GoRouter(
    initialLocation: AppRoutes.publicHome,
    redirect: (context, state) {
      final publicAuthRoutes = {
        AppRoutes.login,
        AppRoutes.signup,
        AppRoutes.forgotPassword,
        AppRoutes.resetPassword,
      };
      final onPublicAuthRoute = publicAuthRoutes.contains(
        state.matchedLocation,
      );
      final onPublicWebsiteRoute = AppRoutes.isPublicWebsiteRoute(
        state.matchedLocation,
      );

      if (authState.status == AuthStatus.checking) {
        return onPublicAuthRoute || onPublicWebsiteRoute
            ? null
            : AppRoutes.login;
      }
      if (!authState.isAuthenticated) {
        return onPublicAuthRoute || onPublicWebsiteRoute
            ? null
            : AppRoutes.login;
      }
      if (onPublicAuthRoute) {
        return AppRoutes.dashboard;
      }
      if (onPublicWebsiteRoute) {
        return null;
      }
      final guardedModuleId = moduleIdForPath(state.uri.path);
      if (guardedModuleId != null &&
          !canAccessModule(authState.user, guardedModuleId)) {
        return AppRoutes.dashboard;
      }
      return null;
    },
    routes: [
      GoRoute(
        path: AppRoutes.login,
        name: AppRouteName.login,
        builder: (context, state) => const LoginPage(),
      ),
      GoRoute(
        path: AppRoutes.signup,
        name: AppRouteName.signup,
        builder: (context, state) => const SignupPage(),
      ),
      GoRoute(
        path: AppRoutes.forgotPassword,
        name: AppRouteName.forgotPassword,
        builder: (context, state) => const ForgotPasswordPage(),
      ),
      GoRoute(
        path: AppRoutes.resetPassword,
        name: AppRouteName.resetPassword,
        builder: (context, state) {
          return ResetPasswordPage(token: state.uri.queryParameters['token']);
        },
      ),
      GoRoute(
        path: AppRoutes.publicAbout,
        name: AppRouteName.publicAbout,
        builder: (context, state) => const PublicAboutPage(),
      ),
      GoRoute(
        path: AppRoutes.publicAdmissions,
        name: AppRouteName.publicAdmissions,
        builder: (context, state) => const PublicAdmissionsPage(),
      ),
      GoRoute(
        path: AppRoutes.publicAcademics,
        name: AppRouteName.publicAcademics,
        builder: (context, state) => const PublicAcademicsPage(),
      ),
      GoRoute(
        path: AppRoutes.publicFacilities,
        name: AppRouteName.publicFacilities,
        builder: (context, state) => const PublicFacilitiesPage(),
      ),
      GoRoute(
        path: AppRoutes.publicGallery,
        name: AppRouteName.publicGallery,
        builder: (context, state) => const PublicGalleryPage(),
      ),
      GoRoute(
        path: AppRoutes.publicEvents,
        name: AppRouteName.publicEvents,
        builder: (context, state) => const PublicEventsPage(),
      ),
      GoRoute(
        path: AppRoutes.publicContact,
        name: AppRouteName.publicContact,
        builder: (context, state) => const PublicContactPage(),
      ),
      GoRoute(
        path: AppRoutes.dashboard,
        name: AppRouteName.dashboard,
        builder: (context, state) {
          if (authState.isAuthenticated) {
            return const DashboardPage();
          }
          return const PublicHomePage();
        },
      ),
      GoRoute(
        path: AppRoutes.students,
        name: AppRouteName.students,
        builder: (context, state) => const StudentsPage(),
      ),
      GoRoute(
        path: AppRoutes.academic,
        name: AppRouteName.academic,
        builder: (context, state) => const AcademicManagementPage(),
      ),
      GoRoute(
        path: AppRoutes.studentClasses,
        name: AppRouteName.studentClasses,
        builder: (context, state) => const StudentsPage(),
      ),
      GoRoute(
        path: '/students/classes/:classId/sections',
        name: AppRouteName.studentSections,
        builder: (context, state) {
          return StudentSectionsPage(
            classId: state.pathParameters['classId'] ?? '',
            academicYearId: state.uri.queryParameters['academicYearId'],
          );
        },
      ),
      GoRoute(
        path: '/students/classes/:classId/sections/:sectionId',
        name: AppRouteName.studentSectionDetail,
        builder: (context, state) {
          return StudentSectionDetailPage(
            classId: state.pathParameters['classId'] ?? '',
            sectionId: state.pathParameters['sectionId'] ?? '',
            academicYearId: state.uri.queryParameters['academicYearId'],
          );
        },
      ),
      GoRoute(
        path: '/students/:studentId/profile',
        name: AppRouteName.studentProfile,
        builder: (context, state) {
          return StudentProfilePage(
            studentId: state.pathParameters['studentId'] ?? '',
          );
        },
      ),
      GoRoute(
        path: AppRoutes.fees,
        name: AppRouteName.fees,
        builder: (context, state) => const FeesManagementPage(),
      ),
      GoRoute(
        path: AppRoutes.hostel,
        name: AppRouteName.hostel,
        builder: (context, state) => const HostelManagementPage(),
      ),
      GoRoute(
        path: AppRoutes.transport,
        name: AppRouteName.transport,
        builder: (context, state) => const TransportManagementPage(),
      ),
      GoRoute(
        path: AppRoutes.teachers,
        name: AppRouteName.teachers,
        builder: (context, state) => const TeacherManagementPage(),
      ),
      GoRoute(
        path: AppRoutes.teacherAttendance,
        name: AppRouteName.teacherAttendance,
        builder: (context, state) => const TeacherAttendancePage(),
      ),
      GoRoute(
        path: AppRoutes.users,
        name: AppRouteName.users,
        builder: (context, state) => const UsersPage(),
      ),
      GoRoute(
        path: AppRoutes.roles,
        name: AppRouteName.roles,
        builder: (context, state) => const RolePermissionPage(),
      ),
      GoRoute(
        path: AppRoutes.auditLogs,
        name: AppRouteName.auditLogs,
        builder: (context, state) => const AuditLogsPage(),
      ),
      GoRoute(
        path: AppRoutes.attendance,
        name: AppRouteName.attendance,
        builder: (context, state) => const AttendancePage(),
      ),
      GoRoute(
        path: AppRoutes.attendanceDaily,
        name: AppRouteName.attendanceDaily,
        builder: (context, state) => const AttendancePage(),
      ),
      GoRoute(
        path: AppRoutes.attendanceReport,
        name: AppRouteName.attendanceReport,
        builder: (context, state) => const AttendancePage(),
      ),
      GoRoute(
        path: AppRoutes.exams,
        name: AppRouteName.exams,
        builder: (context, state) => const ExamsPage(),
      ),
      GoRoute(
        path: AppRoutes.examTypes,
        name: AppRouteName.examTypes,
        builder: (context, state) => const ExamsPage(),
      ),
      GoRoute(
        path: AppRoutes.examSchedules,
        name: AppRouteName.examSchedules,
        builder: (context, state) => const ExamsPage(),
      ),
      GoRoute(
        path: AppRoutes.examMarks,
        name: AppRouteName.examMarks,
        builder: (context, state) => const ExamsPage(),
      ),
      GoRoute(
        path: AppRoutes.examResults,
        name: AppRouteName.examResults,
        builder: (context, state) => const ExamsPage(),
      ),
      GoRoute(
        path: AppRoutes.reports,
        name: AppRouteName.reports,
        builder: (context, state) => const ReportsPage(),
      ),
      GoRoute(
        path: AppRoutes.notifications,
        name: AppRouteName.notifications,
        builder: (context, state) => const NotificationManagementPage(),
      ),
      GoRoute(
        path: AppRoutes.settings,
        name: AppRouteName.settings,
        builder: (context, state) => const SettingsPage(),
      ),
      GoRoute(
        path: AppRoutes.newFeeStructure,
        name: AppRouteName.newFeeStructure,
        builder: (context, state) => const FeeStructureFormPage(),
      ),
      GoRoute(
        path: '/fees/structures/:structureId/edit',
        name: AppRouteName.editFeeStructure,
        builder: (context, state) {
          return FeeStructureFormPage(
            structureId: state.pathParameters['structureId'],
          );
        },
      ),
      GoRoute(
        path: AppRoutes.feeAssignments,
        name: AppRouteName.feeAssignments,
        builder: (context, state) => const FeeAssignmentsPage(),
      ),
      GoRoute(
        path: AppRoutes.feeDefaulters,
        name: AppRouteName.feeDefaulters,
        builder: (context, state) => const FeeDefaultersPage(),
      ),
      GoRoute(
        path: '/fees/assignments/:academicYearId/:classId',
        name: AppRouteName.feeClassAssignments,
        builder: (context, state) {
          return StudentFeeAssignmentPage(
            initialAcademicYearId: state.pathParameters['academicYearId'],
            initialClassId: state.pathParameters['classId'],
          );
        },
      ),
      GoRoute(
        path: AppRoutes.legacyNewFeeAssignment,
        name: AppRouteName.newFeeAssignment,
        builder: (context, state) => const StudentFeeAssignmentPage(),
      ),
      GoRoute(
        path: AppRoutes.feePayments,
        name: AppRouteName.feePayments,
        builder: (context, state) {
          return PaymentCollectionPage(
            assignmentId: state.uri.queryParameters['assignmentId'],
          );
        },
      ),
      GoRoute(
        path: AppRoutes.collectFeePayment,
        name: AppRouteName.collectFeePayment,
        builder: (context, state) {
          return PaymentCollectionPage(
            assignmentId: state.uri.queryParameters['assignmentId'],
          );
        },
      ),
      GoRoute(
        path: '/fees/payments/:paymentId/receipt',
        name: AppRouteName.paymentReceipt,
        builder: (context, state) {
          return PaymentCollectionPage(
            paymentId: state.pathParameters['paymentId'],
          );
        },
      ),
      GoRoute(
        path: '/fees/students/:studentId/payment-collection',
        name: AppRouteName.studentFeePaymentCollection,
        builder: (context, state) {
          return PaymentCollectionPage(
            studentId: state.pathParameters['studentId'],
          );
        },
      ),
      GoRoute(
        path:
            '/fees/assignments/:academicYearId/:classId/students/:studentId/pay',
        name: AppRouteName.classStudentFeePayment,
        builder: (context, state) {
          return PaymentCollectionPage(
            studentId: state.pathParameters['studentId'],
          );
        },
      ),
      GoRoute(
        path: AppRoutes.moduleDetail,
        name: AppRouteName.moduleDetail,
        builder: (context, state) {
          final moduleId = state.pathParameters['moduleId'] ?? '';
          return ModulePlaceholderPage(moduleId: moduleId);
        },
      ),
    ],
  );
});
