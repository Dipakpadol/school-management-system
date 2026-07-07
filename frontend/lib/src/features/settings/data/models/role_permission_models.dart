class PermissionOptionModel {
  const PermissionOptionModel({
    required this.id,
    required this.code,
    required this.name,
    required this.moduleName,
    required this.status,
    required this.assigned,
    this.description,
  });

  factory PermissionOptionModel.fromJson(Map<String, dynamic> json) {
    return PermissionOptionModel(
      id: json['id'] as String? ?? '',
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      moduleName:
          json['moduleName'] as String? ??
          _moduleNameFromCode(json['code'] as String? ?? ''),
      description: json['description'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
      assigned: json['assigned'] as bool? ?? false,
    );
  }

  final String id;
  final String code;
  final String name;
  final String moduleName;
  final String? description;
  final String status;
  final bool assigned;
}

class RolePermissionMatrixModel {
  const RolePermissionMatrixModel({
    required this.roleId,
    required this.roleName,
    required this.displayName,
    required this.permissions,
    required this.status,
    required this.systemRole,
    this.description,
  });

  factory RolePermissionMatrixModel.fromJson(Map<String, dynamic> json) {
    return RolePermissionMatrixModel(
      roleId: json['roleId'] as String? ?? '',
      roleName: json['roleName'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      description: json['description'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
      systemRole: json['systemRole'] as bool? ?? false,
      permissions: _permissions(json['permissions']),
    );
  }

  final String roleId;
  final String roleName;
  final String displayName;
  final String? description;
  final String status;
  final bool systemRole;
  final List<PermissionOptionModel> permissions;
}

List<PermissionOptionModel> _permissions(Object? value) {
  if (value is! List) {
    return const [];
  }
  return value
      .whereType<Map<String, dynamic>>()
      .map(PermissionOptionModel.fromJson)
      .toList(growable: false);
}

String _moduleNameFromCode(String code) {
  final parts = code.split('_');
  return parts.isEmpty ? '' : parts.first;
}
