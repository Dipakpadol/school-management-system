import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../models/auth_session_dto.dart';

final authRemoteDataSourceProvider = Provider<AuthRemoteDataSource>((ref) {
  return AuthRemoteDataSource(ref.watch(apiClientProvider));
});

class AuthRemoteDataSource {
  const AuthRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<AuthSessionDto> login({
    required String email,
    required String password,
  }) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.login,
      data: {'email': email, 'password': password},
      options: Options(extra: {'skipAuth': true}),
    );
    final data = _unwrapData(response.data);
    return AuthSessionDto.fromJson(data);
  }

  Future<String> signup(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.signup,
      data: payload,
      options: Options(extra: {'skipAuth': true}),
    );
    return response.data?['message'] as String? ?? 'Registration successful.';
  }

  Future<String> forgotPassword(String emailOrMobile) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.forgotPassword,
      data: {'emailOrMobile': emailOrMobile},
      options: Options(extra: {'skipAuth': true}),
    );
    return response.data?['message'] as String? ??
        'If the account exists, password reset instructions have been sent.';
  }

  Future<String> resetPassword({
    required String token,
    required String newPassword,
    required String confirmPassword,
  }) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.resetPassword,
      data: {
        'token': token,
        'newPassword': newPassword,
        'confirmPassword': confirmPassword,
      },
      options: Options(extra: {'skipAuth': true}),
    );
    return response.data?['message'] as String? ??
        'Password reset successful. Please login.';
  }

  Future<void> logout(String refreshToken) async {
    await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.logout,
      data: {'refreshToken': refreshToken},
    );
  }

  Future<AuthUserDto> me() async {
    final response = await _apiClient.get<Map<String, dynamic>>(ApiPaths.me);
    return AuthUserDto.fromJson(_unwrapData(response.data));
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Authentication response is invalid.');
  }
}
