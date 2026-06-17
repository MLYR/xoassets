import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../errors/app_exception.dart';
import 'api_response.dart';

/// Dio 的薄封装，后续 feature repository 只依赖这个入口发请求。
class ApiClient {
  const ApiClient(this._dio);

  final Dio _dio;

  Future<Response<T>> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) {
    return _dio.get<T>(path, queryParameters: queryParameters);
  }

  Future<Response<T>> post<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
  }) {
    return _dio.post<T>(path, data: data, queryParameters: queryParameters);
  }

  Future<Response<T>> put<T>(String path, {Object? data}) {
    return _dio.put<T>(path, data: data);
  }

  Future<Response<T>> delete<T>(String path, {Object? data}) {
    return _dio.delete<T>(path, data: data);
  }

  Future<T> getData<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    required T Function(Object? value) mapper,
  }) async {
    final response = await get<Object?>(path, queryParameters: queryParameters);
    return _decodeResult(response.data, mapper);
  }

  Future<T> postData<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    required T Function(Object? value) mapper,
  }) async {
    final response = await post<Object?>(
      path,
      data: data,
      queryParameters: queryParameters,
    );
    return _decodeResult(response.data, mapper);
  }

  T _decodeResult<T>(Object? value, T Function(Object? value) mapper) {
    if (value is Map<String, dynamic> && value.containsKey('code')) {
      final code = (value['code'] as num?)?.toInt() ?? 0;
      final message = value['message']?.toString() ?? '请求失败';
      if (code != 0) {
        // 后端业务失败时 data 可能为 null，必须先抛业务提示，避免模型解析吞掉错误文案。
        throw AppException(message, code: code.toString());
      }

      final result = ApiResponse<T>.fromJson(value, mapper);
      if (result.data == null) {
        throw const AppException('接口返回数据为空');
      }
      return result.data as T;
    }

    return mapper(value);
  }

  @visibleForTesting
  T decodeForTest<T>(Object? value, T Function(Object? value) mapper) {
    return _decodeResult(value, mapper);
  }
}
