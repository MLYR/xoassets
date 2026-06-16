import 'package:dio/dio.dart';

/// 开发期网络日志，只输出请求摘要，避免泄露 Token 和财务明细。
class XoLogInterceptor extends Interceptor {
  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    assert(() {
      // ignore: avoid_print
      print('[XO API] ${options.method} ${options.uri}');
      return true;
    }());
    handler.next(options);
  }
}
