import 'package:dio/dio.dart';

import 'app_exception.dart';

/// 网络和业务异常统一转换，避免页面直接依赖 Dio 错误结构。
class ErrorHandler {
  static AppException fromObject(Object error) {
    if (error is AppException) {
      return error;
    }

    if (error is DioException) {
      final message = error.response?.data is Map<String, dynamic>
          ? error.response?.data['message']?.toString()
          : null;
      return AppException(
        message ?? error.message ?? '网络请求失败',
        code: error.response?.statusCode?.toString(),
      );
    }

    return AppException(error.toString());
  }
}
