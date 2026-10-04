import '../../auth/domain/entities/auth_user.dart';
import 'erp_module.dart';
import 'module_registry.dart';

const _allModuleIds = {
  'student-portal',
  'parent-portal',
  'teacher-portal',
  'students',
  'fees',
  'users',
  'roles',
  'audit-logs',
  'academic',
  'hostel',
  'transport',
  'teachers',
  'staff',
  'attendance',
  'exams',
  'reports',
  'notifications',
  'communications',
  'library',
  'backup',
  'settings',
};

const _modulePermissions = <String, Set<String>>{
  'student-portal': {'PORTAL_STUDENT_READ'},
  'parent-portal': {'PORTAL_PARENT_READ'},
  'teacher-portal': {
    'PORTAL_TEACHER_READ',
    'PORTAL_TEACHER_ATTENDANCE',
    'PORTAL_TEACHER_MARKS',
  },
  'students': {'STUDENTS_READ'},
  'fees': {'FEES_READ', 'FEES_MANAGE'},
  'users': {'USERS_READ'},
  'roles': {'USERS_READ', 'SETTINGS_READ'},
  'audit-logs': {'AUDIT_LOGS_READ'},
  'academic': {'ACADEMIC_READ', 'ACADEMIC_MANAGE'},
  'hostel': {'HOSTEL_READ', 'HOSTEL_MANAGE'},
  'transport': {'TRANSPORT_READ', 'TRANSPORT_MANAGE'},
  'teachers': {'TEACHERS_READ', 'TEACHERS_MANAGE'},
  'staff': {'STAFF_READ', 'LEAVE_READ', 'PAYROLL_READ', 'PAYROLL_PROCESS'},
  'attendance': {'ATTENDANCE_READ', 'ATTENDANCE_MARK'},
  'exams': {'EXAMS_READ', 'EXAMS_MANAGE'},
  'reports': {'REPORTS_READ', 'REPORTS_MANAGE'},
  'notifications': {
    'NOTIFICATIONS_READ',
    'NOTIFICATIONS_MANAGE',
    'NOTIFICATIONS_SEND',
  },
  'communications': {
    'COMMUNICATION_READ',
    'COMMUNICATION_CREATE',
    'COMMUNICATION_UPDATE',
    'COMMUNICATION_PUBLISH',
  },
  'library': {
    'LIBRARY_READ',
    'LIBRARY_CREATE',
    'LIBRARY_UPDATE',
    'LIBRARY_ISSUE',
    'LIBRARY_RETURN',
    'LIBRARY_FINE',
  },
  'backup': {
    'BACKUP_READ',
    'BACKUP_CREATE',
    'BACKUP_DOWNLOAD',
    'BACKUP_DELETE',
    'BACKUP_RESTORE',
  },
  'settings': {'SETTINGS_READ', 'SETTINGS_UPDATE'},
};

const _roleModuleFallback = <String, Set<String>>{
  'SUPER_ADMIN': _allModuleIds,
  'ADMIN': _allModuleIds,
  'PRINCIPAL': {
    'students',
    'academic',
    'attendance',
    'exams',
    'reports',
    'notifications',
    'transport',
    'teachers',
    'staff',
    'roles',
    'settings',
    'library',
    'backup',
  },
  'TEACHER': {'teacher-portal'},
  'ACCOUNTANT': {'students', 'fees', 'reports', 'staff', 'communications'},
  'RECEPTIONIST': {
    'students',
    'fees',
    'notifications',
    'transport',
    'staff',
    'communications',
    'library',
  },
  'WARDEN': {'students', 'hostel', 'attendance', 'reports', 'library'},
  'STUDENT': {'student-portal'},
  'PARENT': {'parent-portal'},
};

String portalHomeForUser(AuthUser? user) {
  if (user == null) {
    return '/';
  }
  final hasAdministrativeRole =
      user.hasRole('SUPER_ADMIN') ||
      user.hasRole('ADMIN') ||
      user.hasRole('PRINCIPAL') ||
      user.hasRole('ACCOUNTANT') ||
      user.hasRole('RECEPTIONIST') ||
      user.hasRole('WARDEN');
  if (hasAdministrativeRole) {
    return '/';
  }
  if (user.hasRole('STUDENT')) {
    return '/portal/student';
  }
  if (user.hasRole('PARENT')) {
    return '/portal/parent';
  }
  if (user.hasRole('TEACHER')) {
    return '/portal/teacher';
  }
  return '/';
}

List<ErpModule> visibleErpModules(AuthUser? user) {
  if (user == null) {
    return const [];
  }
  return erpModules
      .where((module) => canAccessModule(user, module.id))
      .toList(growable: false);
}

List<ErpModule> visibleErpModulesFromMenuIds(
  AuthUser? user,
  Iterable<String> menuModuleIds,
) {
  if (user == null) {
    return const [];
  }
  final menuIds = menuModuleIds.map((value) => value.toLowerCase()).toSet();
  return erpModules
      .where((module) => menuIds.contains(module.id))
      .where((module) => canAccessModule(user, module.id))
      .toList(growable: false);
}

bool canAccessModule(AuthUser? user, String moduleId) {
  if (user == null) {
    return false;
  }
  final normalizedModule = moduleId.toLowerCase();
  if (!_allModuleIds.contains(normalizedModule)) {
    return false;
  }
  if (user.hasRole('SUPER_ADMIN')) {
    return true;
  }
  final permissions = _modulePermissions[normalizedModule] ?? const {};
  if (user.permissions.isNotEmpty) {
    return user.hasAnyPermission(permissions);
  }
  final roles = user.roles.map((role) => role.toUpperCase());
  return roles.any(
    (role) => _roleModuleFallback[role]?.contains(normalizedModule) ?? false,
  );
}

String? moduleIdForPath(String path) {
  if (path == '/portal/student' || path.startsWith('/portal/student/')) {
    return 'student-portal';
  }
  if (path == '/portal/parent' || path.startsWith('/portal/parent/')) {
    return 'parent-portal';
  }
  if (path == '/portal/teacher' || path.startsWith('/portal/teacher/')) {
    return 'teacher-portal';
  }
  if (path == '/students' || path.startsWith('/students/')) {
    return 'students';
  }
  if (path == '/academic' || path.startsWith('/academic/')) {
    return 'academic';
  }
  if (path == '/fees' || path.startsWith('/fees/')) {
    return 'fees';
  }
  if (path == '/hostels' || path.startsWith('/hostels/')) {
    return 'hostel';
  }
  if (path == '/transport' || path.startsWith('/transport/')) {
    return 'transport';
  }
  if (path == '/teachers' || path.startsWith('/teachers/')) {
    return 'teachers';
  }
  if (path == '/modules/staff' || path.startsWith('/modules/staff/')) {
    return 'staff';
  }
  if (path == '/modules/communications' ||
      path.startsWith('/modules/communications/')) {
    return 'communications';
  }
  if (path == '/modules/library' || path.startsWith('/modules/library/')) {
    return 'library';
  }
  if (path == '/modules/backup' || path.startsWith('/modules/backup/')) {
    return 'backup';
  }
  if (path == '/users' || path.startsWith('/users/')) {
    return 'users';
  }
  if (path == '/roles' || path.startsWith('/roles/')) {
    return 'roles';
  }
  if (path == '/audit-logs' || path.startsWith('/audit-logs/')) {
    return 'audit-logs';
  }
  if (path == '/attendance' || path.startsWith('/attendance/')) {
    return 'attendance';
  }
  if (path == '/exams' || path.startsWith('/exams/')) {
    return 'exams';
  }
  if (path == '/reports' || path.startsWith('/reports/')) {
    return 'reports';
  }
  if (path == '/notifications' || path.startsWith('/notifications/')) {
    return 'notifications';
  }
  if (path == '/settings' || path.startsWith('/settings/')) {
    return 'settings';
  }
  if (!path.startsWith('/modules/')) {
    return null;
  }
  final segment = path.substring('/modules/'.length).split('/').first;
  return segment.isEmpty ? null : segment;
}
