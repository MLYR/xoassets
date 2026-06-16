import 'package:dio/dio.dart';

import '../../storage/secure_storage_service.dart';

/// 给请求自动附加 Bearer Token，并预留 401 回登录的扩展点。
class AuthInterceptor extends Interceptor {
  const AuthInterceptor(this._secureStorage);

  final SecureStorageService _secureStorage;

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final token = await _secureStorage.readAccessToken();
    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (err.response?.statusCode == 401) {
      // TODO: 真实登录接入后在这里触发清 token 和跳转登录。
    }
    handler.next(err);
  }
}
