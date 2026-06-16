import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../constants/api_constants.dart';
import '../storage/secure_storage_service.dart';
import 'api_client.dart';
import 'interceptors/auth_interceptor.dart';
import 'interceptors/log_interceptor.dart';

/// Dio 实例提供者，统一配置 baseUrl、超时、Token 和日志拦截器。
final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(
    BaseOptions(
      baseUrl: ApiConstants.baseUrl,
      connectTimeout: ApiConstants.connectTimeout,
      receiveTimeout: ApiConstants.receiveTimeout,
      headers: const {'Accept': 'application/json'},
    ),
  );

  dio.interceptors.add(AuthInterceptor(ref.read(secureStorageServiceProvider)));
  dio.interceptors.add(XoLogInterceptor());
  return dio;
});

/// Feature 层使用的请求客户端。
final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(ref.read(dioProvider));
});
