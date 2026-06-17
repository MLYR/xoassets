import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:xoassets_mobile/core/errors/app_exception.dart';
import 'package:xoassets_mobile/core/errors/error_handler.dart';
import 'package:xoassets_mobile/core/network/api_client.dart';

void main() {
  test('throws backend business message before mapping nullable data', () {
    final client = ApiClient(Dio());

    expect(
      () => client.decodeForTest<String>({
        'code': 40001,
        'message': '用户名或密码错误',
        'data': null,
      }, (_) => throw StateError('mapper should not run')),
      throwsA(
        isA<AppException>()
            .having((error) => error.code, 'code', '40001')
            .having((error) => error.message, 'message', '用户名或密码错误'),
      ),
    );
  });

  test('maps Flutter Web network errors to friendly message', () {
    final error = ErrorHandler.fromObject(
      DioException(
        requestOptions: RequestOptions(path: '/auth/login'),
        type: DioExceptionType.connectionError,
        message: 'The XMLHttpRequest onError callback was called.',
      ),
    );

    expect(error.message, contains('无法连接后端服务'));
  });
}
