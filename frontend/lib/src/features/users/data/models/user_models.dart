class RoleModel {
  const RoleModel({
    required this.id,
    required this.name,
    required this.displayName,
    this.description,
  });

  factory RoleModel.fromJson(Map<String, dynamic> json) {
    return RoleModel(
      id: json['id'] as String,
      name: json['name'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      description: json['description'] as String?,
    );
  }

  final String id;
  final String name;
  final String displayName;
  final String? description;
}

class UserModel {
  const UserModel({
    required this.id,
    required this.email,
    required this.username,
    required this.firstName,
    required this.displayName,
    required this.status,
    required this.source,
    required this.roles,
    this.middleName,
    this.lastName,
    this.phoneNumber,
    this.lastLoginAt,
    this.createdAt,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as String,
      email: json['email'] as String? ?? '',
      username: json['username'] as String? ?? '',
      firstName: json['firstName'] as String? ?? '',
      middleName: json['middleName'] as String?,
      lastName: json['lastName'] as String?,
      displayName: json['displayName'] as String? ?? '',
      phoneNumber: json['phoneNumber'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
      source: json['source'] as String? ?? 'ADMIN_CREATED',
      roles: _list(json['roles'], RoleModel.fromJson),
      lastLoginAt: json['lastLoginAt'] is String
          ? DateTime.parse(json['lastLoginAt'] as String)
          : null,
      createdAt: json['createdAt'] is String
          ? DateTime.parse(json['createdAt'] as String)
          : null,
    );
  }

  final String id;
  final String email;
  final String username;
  final String firstName;
  final String? middleName;
  final String? lastName;
  final String displayName;
  final String? phoneNumber;
  final String status;
  final String source;
  final List<RoleModel> roles;
  final DateTime? lastLoginAt;
  final DateTime? createdAt;

  Map<String, dynamic> toUpdatePayload({
    String? email,
    String? username,
    String? firstName,
    String? middleName,
    String? lastName,
    String? phoneNumber,
    List<String>? roles,
  }) {
    return {
      'email': email ?? this.email,
      'username': username ?? this.username,
      'firstName': firstName ?? this.firstName,
      'middleName': middleName ?? this.middleName,
      'lastName': lastName ?? this.lastName,
      'phoneNumber': phoneNumber ?? this.phoneNumber,
      'roles': roles ?? this.roles.map((role) => role.name).toList(),
    };
  }
}

List<T> _list<T>(Object? value, T Function(Map<String, dynamic>) mapper) {
  if (value is! List) {
    return const [];
  }
  return value.whereType<Map<String, dynamic>>().map(mapper).toList();
}
