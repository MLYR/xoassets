import 'package:dio/dio.dart';

import '../../storage/secure_storage_service.dart';

/// 给请求自动附加 Bearer Token，并在 401 时统一清除本地登录态。
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
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    final data = err.response?.data;
    final businessCode = data is Map<String, dynamic>
        ? data['code']?.toString()
        : null;
    if (err.response?.statusCode == 401 || businessCode == '40100') {
      await _secureStorage.clearTokens();
    }
    handler.next(err);
  }
}
