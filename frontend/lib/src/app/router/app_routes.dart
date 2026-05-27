abstract final class AppRoutes {
  static const login = '/login';
  static const dashboard = '/';
  static const students = '/students';
  static const studentClasses = '/students/classes';
  static const fees = '/fees';
  static const users = '/users';
  static const auditLogs = '/audit-logs';
  static const settings = '/settings';
  static const newFeeStructure = '/fees/structures/new';
  static const newFeeAssignment = '/fees/assignments/new';
  static const collectFeePayment = '/fees/payments/collect';
  static const moduleDetail = '/modules/:moduleId';

  static String module(String moduleId) {
    return switch (moduleId) {
      'students' => students,
      'fees' => fees,
      'users' => users,
      'audit-logs' => auditLogs,
      'settings' => settings,
      _ => '/modules/$moduleId',
    };
  }

  static String editFeeStructure(String structureId) {
    return '/fees/structures/$structureId/edit';
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
  static const dashboard = 'dashboard';
  static const students = 'students';
  static const studentClasses = 'student-classes';
  static const studentSections = 'student-sections';
  static const studentSectionDetail = 'student-section-detail';
  static const studentProfile = 'student-profile';
  static const fees = 'fees';
  static const users = 'users';
  static const auditLogs = 'audit-logs';
  static const settings = 'settings';
  static const newFeeStructure = 'new-fee-structure';
  static const editFeeStructure = 'edit-fee-structure';
  static const newFeeAssignment = 'new-fee-assignment';
  static const collectFeePayment = 'collect-fee-payment';
  static const moduleDetail = 'module-detail';
}
