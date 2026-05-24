abstract final class ApiPaths {
  static const systemStatus = '/v1/system/status';
  static const dashboardSummary = '/v1/dashboard/summary';

  static const login = '/v1/auth/login';
  static const logout = '/v1/auth/logout';
  static const refresh = '/v1/auth/refresh';
  static const me = '/v1/auth/me';

  static const students = '/v1/students';
  static const studentAdmissions = '/v1/students/admissions';

  static const users = '/v1/users';
  static const roles = '/v1/users/roles';

  static const auditLogs = '/v1/audit-logs';

  static const feeCategories = '/v1/fees/categories';
  static const feeStructures = '/v1/fees/structures';
  static const lateFeeRules = '/v1/fees/late-fee-rules';
  static const feeAssignments = '/v1/fees/assignments';
  static const feeDefaulters = '/v1/fees/reports/defaulters';
  static const feeSummary = '/v1/fees/reports/summary';

  static String feeStructure(String id) => '/v1/fees/structures/$id';

  static String feeAssignment(String id) => '/v1/fees/assignments/$id';

  static String feeAssignmentPayments(String id) =>
      '/v1/fees/assignments/$id/payments';

  static String feeReceipt(String receiptNumber) =>
      '/v1/fees/receipts/$receiptNumber';

  static String student(String id) => '/v1/students/$id';

  static String studentProfile(String id) => '/v1/students/$id/profile';

  static String studentActivate(String id) => '/v1/students/$id/activate';

  static String studentDeactivate(String id) => '/v1/students/$id/deactivate';

  static String user(String id) => '/v1/users/$id';

  static String userActivate(String id) => '/v1/users/$id/activate';

  static String userDeactivate(String id) => '/v1/users/$id/deactivate';

  static String userResetPassword(String id) => '/v1/users/$id/reset-password';

  static String auditLog(String id) => '/v1/audit-logs/$id';
}
