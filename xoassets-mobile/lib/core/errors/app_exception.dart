/// App 统一异常模型，后续真实接口接入时供页面展示稳定错误信息。
class AppException implements Exception {
  const AppException(this.message, {this.code});

  final String message;
  final String? code;

  @override
  String toString() => 'AppException(code: $code, message: $message)';
}
