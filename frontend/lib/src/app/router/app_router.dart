import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/controllers/auth_controller.dart';
import '../../features/auth/presentation/pages/login_page.dart';
import '../../features/audit_logs/presentation/pages/audit_logs_page.dart';
import '../../features/dashboard/domain/menu_policy.dart';
import '../../features/dashboard/presentation/dashboard_page.dart';
import '../../features/dashboard/presentation/module_placeholder_page.dart';
import '../../features/fees/presentation/pages/fee_structure_form_page.dart';
import '../../features/fees/presentation/pages/fees_management_page.dart';
import '../../features/fees/presentation/pages/payment_collection_page.dart';
import '../../features/fees/presentation/pages/student_fee_assignment_page.dart';
import '../../features/settings/presentation/pages/settings_page.dart';
import '../../features/students/presentation/pages/students_page.dart';
import '../../features/users/presentation/pages/users_page.dart';
import 'app_routes.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final authState = ref.watch(authControllerProvider);

  return GoRouter(
    initialLocation: AppRoutes.login,
    redirect: (context, state) {
      final onLogin = state.matchedLocation == AppRoutes.login;

      if (authState.status == AuthStatus.checking) {
        return onLogin ? null : AppRoutes.login;
      }
      if (!authState.isAuthenticated) {
        return onLogin ? null : AppRoutes.login;
      }
      if (onLogin) {
        return AppRoutes.dashboard;
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
        path: AppRoutes.dashboard,
        name: AppRouteName.dashboard,
        builder: (context, state) => const DashboardPage(),
      ),
      GoRoute(
        path: AppRoutes.students,
        name: AppRouteName.students,
        builder: (context, state) => const StudentsPage(),
      ),
      GoRoute(
        path: AppRoutes.fees,
        name: AppRouteName.fees,
        builder: (context, state) => const FeesManagementPage(),
      ),
      GoRoute(
        path: AppRoutes.users,
        name: AppRouteName.users,
        builder: (context, state) => const UsersPage(),
      ),
      GoRoute(
        path: AppRoutes.auditLogs,
        name: AppRouteName.auditLogs,
        builder: (context, state) => const AuditLogsPage(),
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
        path: AppRoutes.newFeeAssignment,
        name: AppRouteName.newFeeAssignment,
        builder: (context, state) => const StudentFeeAssignmentPage(),
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
