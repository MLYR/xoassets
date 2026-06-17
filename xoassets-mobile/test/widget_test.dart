import 'package:flutter/material.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:xoassets_mobile/app/app.dart';
import 'package:xoassets_mobile/core/network/api_client.dart';
import 'package:xoassets_mobile/core/network/dio_provider.dart';
import 'package:xoassets_mobile/core/errors/app_exception.dart';
import 'package:xoassets_mobile/core/storage/secure_storage_service.dart';

void main() {
  testWidgets('shows login when no token can be restored', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          secureStorageServiceProvider.overrideWithValue(
            _MemorySecureStorageService(),
          ),
          apiClientProvider.overrideWithValue(_FakeApiClient()),
        ],
        child: const XoAssetsApp(),
      ),
    );

    expect(find.text('小〇财迹'), findsOneWidget);
    expect(find.byType(MaterialApp), findsOneWidget);

    await tester.pumpAndSettle();

    expect(find.text('欢迎回来'), findsOneWidget);
    expect(find.text('立即登录'), findsOneWidget);
  });

  testWidgets('restores token and reaches main tabs', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          secureStorageServiceProvider.overrideWithValue(
            _MemorySecureStorageService(accessToken: 'test-token'),
          ),
          apiClientProvider.overrideWithValue(_FakeApiClient()),
        ],
        child: const XoAssetsApp(),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('早上好，看看今天的钱包'), findsOneWidget);
    expect(find.text('首页'), findsOneWidget);
  });

  testWidgets('logs out from profile tab and returns to login', (tester) async {
    final storage = _MemorySecureStorageService(accessToken: 'test-token');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          secureStorageServiceProvider.overrideWithValue(storage),
          apiClientProvider.overrideWithValue(_FakeApiClient()),
        ],
        child: const XoAssetsApp(),
      ),
    );

    await tester.pumpAndSettle();
    await tester.tap(find.text('我的'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.widgetWithText(ListTile, '退出登录'));
    await tester.tap(find.widgetWithText(ListTile, '退出登录'));
    await tester.pumpAndSettle();

    expect(find.text('退出后需要重新登录才能查看资产数据。'), findsOneWidget);

    await tester.tap(find.text('退出'));
    await tester.pumpAndSettle();

    expect(await storage.readAccessToken(), isNull);
    expect(find.text('欢迎回来'), findsOneWidget);
  });

  testWidgets('logs out from settings page and returns to login', (
    tester,
  ) async {
    final storage = _MemorySecureStorageService(accessToken: 'test-token');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          secureStorageServiceProvider.overrideWithValue(storage),
          apiClientProvider.overrideWithValue(_FakeApiClient()),
        ],
        child: const XoAssetsApp(),
      ),
    );

    await tester.pumpAndSettle();
    await tester.tap(find.text('我的'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('设置'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.widgetWithText(ListTile, '退出登录'));
    await tester.tap(find.widgetWithText(ListTile, '退出登录'));
    await tester.pumpAndSettle();

    expect(find.text('退出后需要重新登录才能查看资产数据。'), findsOneWidget);

    await tester.tap(find.text('退出'));
    await tester.pumpAndSettle();

    expect(await storage.readAccessToken(), isNull);
    expect(find.text('欢迎回来'), findsOneWidget);
  });

  testWidgets('keeps login input and shows dialog after invalid credentials', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          secureStorageServiceProvider.overrideWithValue(
            _MemorySecureStorageService(),
          ),
          apiClientProvider.overrideWithValue(
            _FakeApiClient(loginShouldFail: true),
          ),
        ],
        child: const XoAssetsApp(),
      ),
    );

    await tester.pumpAndSettle();
    await tester.enterText(
      find.widgetWithText(TextField, '手机号/邮箱'),
      'bad-user',
    );
    await tester.enterText(find.widgetWithText(TextField, '密码'), 'bad-pass');
    await tester.ensureVisible(find.text('立即登录'));
    await tester.tap(find.text('立即登录'));
    await tester.pumpAndSettle();

    expect(find.text('登录失败'), findsOneWidget);
    expect(find.text('用户名或密码错误'), findsOneWidget);
    expect(find.text('bad-user'), findsOneWidget);
    expect(find.text('bad-pass'), findsOneWidget);
  });
}

class _MemorySecureStorageService extends SecureStorageService {
  _MemorySecureStorageService({this._accessToken}) : super();

  String? _accessToken;
  String? _refreshToken;

  @override
  Future<String?> readAccessToken() async => _accessToken;

  @override
  Future<String?> readRefreshToken() async => _refreshToken;

  @override
  Future<void> saveTokens({
    required String accessToken,
    String? refreshToken,
  }) async {
    _accessToken = accessToken;
    _refreshToken = refreshToken;
  }

  @override
  Future<void> clearTokens() async {
    _accessToken = null;
    _refreshToken = null;
  }
}

class _FakeApiClient extends ApiClient {
  _FakeApiClient({this.loginShouldFail = false}) : super(Dio());

  final bool loginShouldFail;

  @override
  Future<T> getData<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    required T Function(Object? value) mapper,
  }) async {
    if (path == '/auth/me') {
      return mapper({
        'id': '1',
        'username': 'demo',
        'nickname': 'Demo 用户',
        'avatarUrl': null,
      });
    }
    throw UnsupportedError('Unhandled GET $path');
  }

  @override
  Future<T> postData<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    required T Function(Object? value) mapper,
  }) async {
    if (path == '/auth/login') {
      if (loginShouldFail) {
        // 后端业务错误必须留在登录页弹窗展示，不能触发整页重建清空输入。
        throw const AppException('用户名或密码错误', code: '40001');
      }
      return mapper({
        'token': 'test-token',
        'user': {
          'id': '1',
          'username': 'demo',
          'nickname': 'Demo 用户',
          'avatarUrl': null,
        },
      });
    }
    throw UnsupportedError('Unhandled POST $path');
  }
}
