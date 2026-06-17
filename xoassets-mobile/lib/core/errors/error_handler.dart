import 'package:dio/dio.dart';

import 'app_exception.dart';

/// 网络和业务异常统一转换，避免页面直接依赖 Dio 错误结构。
class ErrorHandler {
  static AppException fromObject(Object error) {
    if (error is AppException) {
      return error;
    }

    if (error is DioException) {
      final data = error.response?.data;
      final message = data is Map<String, dynamic>
          ? data['message']?.toString()
          : null;
      final businessCode = data is Map<String, dynamic>
          ? data['code']?.toString()
          : null;
      final fallbackMessage = _friendlyDioMessage(error);
      return AppException(
        message ?? fallbackMessage,
        code: businessCode ?? error.response?.statusCode?.toString(),
      );
    }

    return AppException(error.toString());
  }

  /// Flutter Web 的 CORS / 服务不可达会暴露 XMLHttpRequest 底层错误，这里统一转成人能读的提示。
  static String _friendlyDioMessage(DioException error) {
    final rawMessage = error.message ?? '';
    if (error.type == DioExceptionType.connectionError ||
        rawMessage.contains('XMLHttpRequest') ||
        rawMessage.contains('onError')) {
      return '无法连接后端服务，请确认后端已启动、API 地址正确，并已重启后端使 CORS 配置生效。';
    }

    return rawMessage.isEmpty ? '网络请求失败' : rawMessage;
  }
}
