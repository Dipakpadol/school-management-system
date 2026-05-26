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
      data: {
        'email': email,
        'password': password,
      },
      options: Options(extra: {'skipAuth': true}),
    );
    final data = _unwrapData(response.data);
    return AuthSessionDto.fromJson(data);
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
