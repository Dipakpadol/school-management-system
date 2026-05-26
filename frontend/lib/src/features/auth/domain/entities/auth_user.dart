class AuthUser {
  const AuthUser({
    required this.id,
    required this.email,
    required this.displayName,
    required this.roles,
    required this.permissions,
  });

  final String id;
  final String email;
  final String displayName;
  final List<String> roles;
  final List<String> permissions;

  bool hasRole(String role) {
    return roles.any((value) => value.toUpperCase() == role.toUpperCase());
  }

  bool hasAnyPermission(Iterable<String> requiredPermissions) {
    final normalized = permissions.map((value) => value.toUpperCase()).toSet();
    return requiredPermissions.any(
      (permission) => normalized.contains(permission.toUpperCase()),
    );
  }
}
