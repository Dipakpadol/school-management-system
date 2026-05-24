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
}
