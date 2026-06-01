import 'package:dio/dio.dart';

import '../../constants/api_paths.dart';
import '../../storage/token_storage.dart';

class AuthInterceptor extends QueuedInterceptor {
  AuthInterceptor(this._tokenStorage);

  final TokenStorage _tokenStorage;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (options.extra['skipAuth'] == true || _isPublicAuthPath(options.path)) {
      handler.next(options);
      return;
    }

    final accessToken = await _tokenStorage.readAccessToken();
    if (accessToken != null && accessToken.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $accessToken';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    if (err.response?.statusCode == 401 &&
        err.requestOptions.extra['skipAuth'] != true) {
      await _tokenStorage.clear();
    }
    handler.next(err);
  }

  bool _isPublicAuthPath(String path) {
    return path.endsWith(ApiPaths.login) ||
        path.endsWith(ApiPaths.signup) ||
        path.endsWith(ApiPaths.refresh) ||
        path.endsWith(ApiPaths.forgotPassword) ||
        path.endsWith(ApiPaths.resetPassword);
  }
}
