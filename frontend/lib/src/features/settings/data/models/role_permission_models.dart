class PermissionOptionModel {
  const PermissionOptionModel({
    required this.id,
    required this.code,
    required this.name,
    required this.assigned,
    this.description,
  });

  factory PermissionOptionModel.fromJson(Map<String, dynamic> json) {
    return PermissionOptionModel(
      id: json['id'] as String? ?? '',
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      description: json['description'] as String?,
      assigned: json['assigned'] as bool? ?? false,
    );
  }

  final String id;
  final String code;
  final String name;
  final String? description;
  final bool assigned;
}

class RolePermissionMatrixModel {
  const RolePermissionMatrixModel({
    required this.roleId,
    required this.roleName,
    required this.displayName,
    required this.permissions,
    this.description,
  });

  factory RolePermissionMatrixModel.fromJson(Map<String, dynamic> json) {
    return RolePermissionMatrixModel(
      roleId: json['roleId'] as String? ?? '',
      roleName: json['roleName'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      description: json['description'] as String?,
      permissions: _permissions(json['permissions']),
    );
  }

  final String roleId;
  final String roleName;
  final String displayName;
  final String? description;
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
