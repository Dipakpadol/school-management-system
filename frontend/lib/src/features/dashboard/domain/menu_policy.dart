import '../../auth/domain/entities/auth_user.dart';
import 'erp_module.dart';
import 'module_registry.dart';

const _allModuleIds = {
  'students',
  'fees',
  'users',
  'audit-logs',
  'academic',
  'hostel',
  'attendance',
  'exams',
  'reports',
  'notifications',
  'settings',
};

const _modulePermissions = <String, Set<String>>{
  'students': {'STUDENTS_READ'},
  'fees': {'FEES_READ', 'FEES_MANAGE'},
  'users': {'USERS_READ'},
  'audit-logs': {'AUDIT_LOGS_READ'},
  'academic': {'ACADEMIC_READ', 'ACADEMIC_MANAGE'},
  'hostel': {'HOSTEL_READ', 'HOSTEL_MANAGE'},
  'attendance': {'ATTENDANCE_READ', 'ATTENDANCE_MARK'},
  'exams': {'EXAMS_READ', 'EXAMS_MANAGE'},
  'reports': {'REPORTS_READ', 'REPORTS_MANAGE'},
  'notifications': {
    'NOTIFICATIONS_READ',
    'NOTIFICATIONS_MANAGE',
    'NOTIFICATIONS_SEND',
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
    'settings',
  },
  'TEACHER': {'students', 'academic', 'attendance', 'exams', 'reports'},
  'ACCOUNTANT': {'students', 'fees', 'reports'},
  'RECEPTIONIST': {'students', 'fees', 'notifications'},
  'WARDEN': {'students', 'hostel', 'attendance', 'reports'},
  'STUDENT': {'students', 'academic', 'attendance', 'fees'},
  'PARENT': {'students', 'academic', 'attendance', 'fees', 'notifications'},
};

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
  if (path == '/students' || path.startsWith('/students/')) {
    return 'students';
  }
  if (path == '/academic' || path.startsWith('/academic/')) {
    return 'academic';
  }
  if (path == '/fees' || path.startsWith('/fees/')) {
    return 'fees';
  }
  if (path == '/users' || path.startsWith('/users/')) {
    return 'users';
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
  if (path == '/settings' || path.startsWith('/settings/')) {
    return 'settings';
  }
  if (!path.startsWith('/modules/')) {
    return null;
  }
  final segment = path.substring('/modules/'.length).split('/').first;
  return segment.isEmpty ? null : segment;
}
