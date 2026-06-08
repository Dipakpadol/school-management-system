abstract final class ApiPaths {
  static const systemStatus = '/v1/system/status';
  static const dashboardSummary = '/v1/dashboard/summary';
  static const dashboardTodayAttendance = '/v1/dashboard/today-attendance';
  static const currentMenu = '/v1/menus/current';

  static const login = '/v1/auth/login';
  static const signup = '/v1/auth/signup';
  static const forgotPassword = '/v1/auth/forgot-password';
  static const resetPassword = '/v1/auth/reset-password';
  static const logout = '/v1/auth/logout';
  static const refresh = '/v1/auth/refresh';
  static const me = '/v1/auth/me';

  static const students = '/v1/students';
  static const studentAdmissions = '/v1/students/admissions';
  static const studentsExportExcel = '/v1/students/export/excel';
  static const studentsExportCsv = '/v1/students/export/csv';
  static const studentsTemplate = '/v1/students/template';
  static const studentsImportExcel = '/v1/students/import/excel';
  static const studentsImportCsv = '/v1/students/import/csv';
  static const academicYears = '/v1/academic-years';
  static const subjects = '/v1/subjects';
  static const teachers = '/v1/teachers';

  static const users = '/v1/users';
  static const roles = '/v1/users/roles';
  static const usersExportExcel = '/v1/users/export/excel';
  static const usersExportCsv = '/v1/users/export/csv';
  static const usersTemplate = '/v1/users/template';
  static const usersImportExcel = '/v1/users/import/excel';
  static const usersImportCsv = '/v1/users/import/csv';

  static const auditLogs = '/v1/audit-logs';
  static const auditLogsExportExcel = '/v1/audit-logs/export/excel';
  static const auditLogsExportCsv = '/v1/audit-logs/export/csv';
  static const auditLogsExportPdf = '/v1/audit-logs/export/pdf';
  static const auditLogFilterModules = '/v1/audit-logs/filter-options/modules';
  static const auditLogFilterActions = '/v1/audit-logs/filter-options/actions';
  static const auditLogFilterUsers = '/v1/audit-logs/filter-options/users';

  static const reportsOptions = '/v1/reports/options';
  static const reportsExport = '/v1/reports/export';

  static const feeCategories = '/v1/fees/categories';
  static const feeStructures = '/v1/fees/structures';
  static const lateFeeRules = '/v1/fees/late-fee-rules';
  static const feeAssignments = '/v1/fees/assignments';
  static const feeDefaulters = '/v1/fees/reports/defaulters';
  static const feeSummary = '/v1/fees/reports/summary';
  static const feeStructuresExportExcel = '/v1/fees/structures/export/excel';
  static const feeAssignmentsExportExcel = '/v1/fees/assignments/export/excel';
  static const feeStructureTemplate = '/v1/fees/structures/template';
  static const feeAssignmentTemplate = '/v1/fees/assignments/template';
  static const feeStructuresImportExcel = '/v1/fees/structures/import/excel';
  static const feeStructuresImportCsv = '/v1/fees/structures/import/csv';
  static const feeAssignmentsImportExcel = '/v1/fees/assignments/import/excel';
  static const feeAssignmentsImportCsv = '/v1/fees/assignments/import/csv';

  static String feeCategory(String id) => '/v1/fees/categories/$id';

  static String feeStructure(String id) => '/v1/fees/structures/$id';

  static String feeAssignment(String id) => '/v1/fees/assignments/$id';

  static String feeAssignmentPayments(String id) =>
      '/v1/fees/assignments/$id/payments';

  static String feeStudentSummary(String studentId) =>
      '/v1/fees/students/$studentId/summary';

  static String feeStudentPaymentHistory(String studentId) =>
      '/v1/fees/students/$studentId/payment-history';

  static String feeStudentPayments(String studentId) =>
      '/v1/fees/students/$studentId/payments';

  static String feeClassStudents(String classId) =>
      '/v1/fees/classes/$classId/students';

  static String feeClassAssign(String classId) =>
      '/v1/fees/classes/$classId/assign';

  static String feeReceipt(String receiptNumber) =>
      '/v1/fees/receipts/$receiptNumber';

  static String feeReceiptPdf(String receiptNumber) =>
      '/v1/fees/receipts/$receiptNumber/pdf';

  static String feePaymentReceiptPdf(String paymentId) =>
      '/v1/fees/payments/$paymentId/receipt';

  static String feePaymentReverse(String paymentId) =>
      '/v1/fees/payments/$paymentId/reverse';

  static String feePaymentVoid(String paymentId) =>
      '/v1/fees/payments/$paymentId/void';

  static String feePaymentRefund(String paymentId) =>
      '/v1/fees/payments/$paymentId/refund';

  static String feeDefaultersExport(String format) =>
      '/v1/fees/reports/defaulters/export/$format';

  static String feeCollectionExport(String format) =>
      '/v1/fees/reports/collection/export/$format';

  static String student(String id) => '/v1/students/$id';

  static String studentPdf(String id) => '/v1/students/$id/pdf';

  static String studentProfile(String id) => '/v1/students/$id/profile';

  static String studentAttendanceHistory(String id) =>
      '/v1/students/$id/attendance-history';

  static String studentAttendanceHistoryExport(String id) =>
      '/v1/students/$id/attendance-history/export';

  static String studentExamResults(String id) =>
      '/v1/students/$id/exam-results';

  static String studentExamReportCard(String id, String resultId) =>
      '/v1/students/$id/exam-results/$resultId/report-card';

  static String studentPhoto(String id) => '/v1/students/$id/photo';

  static String studentParents(String id) => '/v1/students/$id/parents';

  static String studentParent(String id, String mappingId) =>
      '/v1/students/$id/parents/$mappingId';

  static String studentDocuments(String id) => '/v1/students/$id/documents';

  static String studentDocument(String id, String documentId) =>
      '/v1/students/$id/documents/$documentId';

  static String studentClassAssignments(String id) =>
      '/v1/students/$id/class-assignments';

  static String studentClassAssignment(String id, String assignmentId) =>
      '/v1/students/$id/class-assignments/$assignmentId';

  static String academicYearClasses(String academicYearId) =>
      '/v1/academic-years/$academicYearId/classes';

  static String academicYear(String academicYearId) =>
      '/v1/academic-years/$academicYearId';

  static String classSections(String classId) =>
      '/v1/classes/$classId/sections';

  static String schoolClass(String classId) => '/v1/classes/$classId';

  static String classDivisions(String classId) =>
      '/v1/classes/$classId/divisions';

  static String division(String divisionId) => '/v1/divisions/$divisionId';

  static String divisionSubjects(String divisionId) =>
      '/v1/divisions/$divisionId/subjects';

  static String divisionSubject(String divisionId, String divisionSubjectId) =>
      '/v1/divisions/$divisionId/subjects/$divisionSubjectId';

  static String classSectionTeachers(String classId, String sectionId) =>
      '/v1/classes/$classId/sections/$sectionId/teachers';

  static String classTeacher(String classId, String sectionId) =>
      '/v1/classes/$classId/sections/$sectionId/class-teacher';

  static String subjectTeacher(
    String classId,
    String sectionId,
    String subjectId,
  ) => '/v1/classes/$classId/sections/$sectionId/subjects/$subjectId/teacher';

  static String studentActivate(String id) => '/v1/students/$id/activate';

  static String studentDeactivate(String id) => '/v1/students/$id/deactivate';

  static String user(String id) => '/v1/users/$id';

  static String userActivate(String id) => '/v1/users/$id/activate';

  static String userDeactivate(String id) => '/v1/users/$id/deactivate';

  static String userResetPassword(String id) => '/v1/users/$id/reset-password';

  static String rolePermissions(String id) => '/v1/users/roles/$id/permissions';

  static String auditLog(String id) => '/v1/audit-logs/$id';

  static const attendanceAcademicYears = '/v1/attendance/academic-years';
  static const attendanceStudents = '/v1/attendance/students';
  static const attendanceDaily = '/v1/attendance/daily';
  static const attendanceExport = '/v1/attendance/export';

  static String attendanceClasses(String academicYearId) =>
      '/v1/attendance/academic-years/$academicYearId/classes';

  static String attendanceSections(String classId) =>
      '/v1/attendance/classes/$classId/sections';

  static String attendanceStudentSummary(String studentId) =>
      '/v1/attendance/students/$studentId/summary';

  static const examAcademicYears = '/v1/exams/academic-years';
  static const examStudents = '/v1/exams/students';
  static const examTypes = '/v1/exams/types';
  static const examSchedules = '/v1/exams/schedules';
  static const examMarks = '/v1/exams/marks';
  static const examResultsGenerate = '/v1/exams/results/generate';

  static String examClasses(String academicYearId) =>
      '/v1/exams/academic-years/$academicYearId/classes';

  static String examSections(String classId) =>
      '/v1/exams/classes/$classId/sections';

  static String examSubjects(String classId, String sectionId) =>
      '/v1/exams/classes/$classId/sections/$sectionId/subjects';

  static String examType(String examTypeId) => '/v1/exams/types/$examTypeId';

  static String examSchedule(String scheduleId) =>
      '/v1/exams/schedules/$scheduleId';

  static String examStudentResult(String studentId) =>
      '/v1/exams/results/students/$studentId';

  static String examReportCard(String studentId) =>
      '/v1/exams/results/students/$studentId/report-card';
}
