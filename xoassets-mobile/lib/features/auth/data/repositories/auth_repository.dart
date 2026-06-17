import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client.dart';
import '../../../../core/network/dio_provider.dart';
import '../../../../core/storage/secure_storage_service.dart';
import '../models/auth_session.dart';
import '../models/auth_user.dart';

/// 认证仓储：页面不直接操作 Dio 或 Secure Storage，统一从这里走后端接口。
class AuthRepository {
  const AuthRepository({required this.apiClient, required this.secureStorage});

  final ApiClient apiClient;
  final SecureStorageService secureStorage;

  Future<AuthSession> login({
    required String username,
    required String password,
  }) async {
    final session = await apiClient.postData<AuthSession>(
      '/auth/login',
      data: {'username': username, 'password': password},
      mapper: AuthSession.fromJson,
    );

    await secureStorage.saveTokens(
      accessToken: session.accessToken,
      refreshToken: session.refreshToken,
    );
    return session;
  }

  Future<AuthUser> register({
    required String username,
    required String password,
    String? nickname,
  }) {
    // 注册接口只创建用户，登录态仍沿用 login 签发 JWT，避免前端伪造 token。
    return apiClient.postData<AuthUser>(
      '/auth/register',
      data: {'username': username, 'password': password, 'nickname': nickname},
      mapper: AuthUser.fromJson,
    );
  }

  Future<AuthSession?> restoreSession() async {
    final accessToken = await secureStorage.readAccessToken();
    if (accessToken == null || accessToken.isEmpty) {
      return null;
    }

    final user = await me();
    return AuthSession(accessToken: accessToken, user: user);
  }

  Future<AuthUser> me() {
    return apiClient.getData<AuthUser>('/auth/me', mapper: AuthUser.fromJson);
  }

  Future<void> logout() {
    // 当前后端没有 logout 接口，退出登录只清除本地敏感 token。
    return secureStorage.clearTokens();
  }
}

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(
    apiClient: ref.read(apiClientProvider),
    secureStorage: ref.read(secureStorageServiceProvider),
  );
});
