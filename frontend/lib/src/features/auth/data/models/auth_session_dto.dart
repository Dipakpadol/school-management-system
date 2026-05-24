import '../../domain/entities/auth_session.dart';
import '../../domain/entities/auth_user.dart';

class AuthSessionDto {
  const AuthSessionDto({
    required this.tokenType,
    required this.accessToken,
    required this.accessTokenExpiresAt,
    required this.refreshToken,
    required this.refreshTokenExpiresAt,
    required this.user,
  });

  factory AuthSessionDto.fromJson(Map<String, dynamic> json) {
    return AuthSessionDto(
      tokenType: json['tokenType'] as String? ?? 'Bearer',
      accessToken: json['accessToken'] as String,
      accessTokenExpiresAt: DateTime.parse(
        json['accessTokenExpiresAt'] as String,
      ),
      refreshToken: json['refreshToken'] as String,
      refreshTokenExpiresAt: DateTime.parse(
        json['refreshTokenExpiresAt'] as String,
      ),
      user: AuthUserDto.fromJson(json['user'] as Map<String, dynamic>),
    );
  }

  final String tokenType;
  final String accessToken;
  final DateTime accessTokenExpiresAt;
  final String refreshToken;
  final DateTime refreshTokenExpiresAt;
  final AuthUserDto user;

  AuthSession toDomain() {
    return AuthSession(
      tokenType: tokenType,
      accessToken: accessToken,
      accessTokenExpiresAt: accessTokenExpiresAt,
      refreshToken: refreshToken,
      refreshTokenExpiresAt: refreshTokenExpiresAt,
      user: user.toDomain(),
    );
  }
}

class AuthUserDto {
  const AuthUserDto({
    required this.id,
    required this.email,
    required this.displayName,
    required this.roles,
    required this.permissions,
  });

  factory AuthUserDto.fromJson(Map<String, dynamic> json) {
    return AuthUserDto(
      id: json['id'] as String,
      email: json['email'] as String,
      displayName: json['displayName'] as String? ?? json['email'] as String,
      roles: _strings(json['roles']),
      permissions: _strings(json['permissions']),
    );
  }

  final String id;
  final String email;
  final String displayName;
  final List<String> roles;
  final List<String> permissions;

  AuthUser toDomain() {
    return AuthUser(
      id: id,
      email: email,
      displayName: displayName,
      roles: roles,
      permissions: permissions,
    );
  }

  static List<String> _strings(Object? value) {
    if (value is! List) {
      return const [];
    }
    return value.whereType<String>().toList(growable: false);
  }
}
