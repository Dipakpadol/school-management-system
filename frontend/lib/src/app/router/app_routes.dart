abstract final class AppRoutes {
  static const login = '/login';
  static const signup = '/signup';
  static const forgotPassword = '/forgot-password';
  static const resetPassword = '/reset-password';
  static const dashboard = '/';
  static const students = '/students';
  static const studentClasses = '/students/classes';
  static const academic = '/academic';
  static const fees = '/fees';
  static const hostel = '/hostels';
  static const transport = '/transport';
  static const teachers = '/teachers';
  static const teacherAttendance = '/teachers/attendance';
  static const users = '/users';
  static const roles = '/roles';
  static const auditLogs = '/audit-logs';
  static const attendance = '/attendance';
  static const attendanceDaily = '/attendance/daily';
  static const attendanceReport = '/attendance/report';
  static const exams = '/exams';
  static const examTypes = '/exams/types';
  static const examSchedules = '/exams/schedules';
  static const examMarks = '/exams/marks';
  static const examResults = '/exams/results';
  static const reports = '/reports';
  static const notifications = '/notifications';
  static const settings = '/settings';
  static const newFeeStructure = '/fees/structures/new';
  static const feeAssignments = '/fees/assignments';
  static const feeDefaulters = '/fees/defaulters';
  static const legacyNewFeeAssignment = '/fees/assignments/new';
  static const newFeeAssignment = feeAssignments;
  static const feePayments = '/fees/payments';
  static const collectFeePayment = '/fees/payments/collect';
  static const moduleDetail = '/modules/:moduleId';

  static String module(String moduleId) {
    return switch (moduleId) {
      'students' => students,
      'academic' => academic,
      'fees' => fees,
      'hostel' => hostel,
      'transport' => transport,
      'teachers' => teachers,
      'users' => users,
      'roles' => roles,
      'audit-logs' => auditLogs,
      'attendance' => attendance,
      'exams' => exams,
      'reports' => reports,
      'notifications' => notifications,
      'settings' => settings,
      _ => '/modules/$moduleId',
    };
  }

  static String editFeeStructure(String structureId) {
    return '/fees/structures/$structureId/edit';
  }

  static String collectFeePaymentForAssignment(String assignmentId) {
    return '$feePayments?assignmentId=$assignmentId';
  }

  static String feeClassAssignments({
    required String academicYearId,
    required String classId,
  }) {
    return '/fees/assignments/$academicYearId/$classId';
  }

  static String studentFeePaymentCollection(String studentId) {
    return '/fees/students/$studentId/payment-collection';
  }

  static String paymentReceipt(String paymentId) {
    return '/fees/payments/$paymentId/receipt';
  }

  static String classStudentFeePayment({
    required String academicYearId,
    required String classId,
    required String studentId,
  }) {
    return '/fees/assignments/$academicYearId/$classId/students/$studentId/pay';
  }

  static String studentClassSections(String classId, {String? academicYearId}) {
    final query = academicYearId == null
        ? ''
        : '?academicYearId=$academicYearId';
    return '/students/classes/$classId/sections$query';
  }

  static String studentSectionDetail(
    String classId,
    String sectionId, {
    String? academicYearId,
  }) {
    final query = academicYearId == null
        ? ''
        : '?academicYearId=$academicYearId';
    return '/students/classes/$classId/sections/$sectionId$query';
  }

  static String studentProfile(String studentId) {
    return '/students/$studentId/profile';
  }
}

abstract final class AppRouteName {
  static const login = 'login';
  static const signup = 'signup';
  static const forgotPassword = 'forgot-password';
  static const resetPassword = 'reset-password';
  static const dashboard = 'dashboard';
  static const students = 'students';
  static const studentClasses = 'student-classes';
  static const studentSections = 'student-sections';
  static const studentSectionDetail = 'student-section-detail';
  static const studentProfile = 'student-profile';
  static const academic = 'academic';
  static const fees = 'fees';
  static const hostel = 'hostel';
  static const transport = 'transport';
  static const teachers = 'teachers';
  static const teacherAttendance = 'teacher-attendance';
  static const users = 'users';
  static const roles = 'roles';
  static const auditLogs = 'audit-logs';
  static const attendance = 'attendance';
  static const attendanceDaily = 'attendance-daily';
  static const attendanceReport = 'attendance-report';
  static const exams = 'exams';
  static const examTypes = 'exam-types';
  static const examSchedules = 'exam-schedules';
  static const examMarks = 'exam-marks';
  static const examResults = 'exam-results';
  static const reports = 'reports';
  static const notifications = 'notifications';
  static const settings = 'settings';
  static const newFeeStructure = 'new-fee-structure';
  static const editFeeStructure = 'edit-fee-structure';
  static const feeAssignments = 'fee-assignments';
  static const feeDefaulters = 'fee-defaulters';
  static const newFeeAssignment = 'new-fee-assignment';
  static const feePayments = 'fee-payments';
  static const collectFeePayment = 'collect-fee-payment';
  static const feeClassAssignments = 'fee-class-assignments';
  static const paymentReceipt = 'payment-receipt';
  static const studentFeePaymentCollection = 'student-fee-payment-collection';
  static const classStudentFeePayment = 'class-student-fee-payment';
  static const moduleDetail = 'module-detail';
}
