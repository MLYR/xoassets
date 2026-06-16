import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/storage/secure_storage_service.dart';

/// 登录状态模型，本阶段只保存 mock token 和是否登录。
class AuthState {
  const AuthState({required this.isAuthenticated, this.accessToken});

  final bool isAuthenticated;
  final String? accessToken;

  AuthState copyWith({bool? isAuthenticated, String? accessToken}) {
    return AuthState(
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      accessToken: accessToken ?? this.accessToken,
    );
  }
}

/// Auth provider 先承载 mock 登录，后续可替换为真实 `/api/auth/**`。
class AuthController extends Notifier<AuthState> {
  @override
  AuthState build() {
    return const AuthState(isAuthenticated: false);
  }

  Future<void> mockLogin() async {
    const token = 'mock-xoassets-token';
    await ref.read(secureStorageServiceProvider).saveTokens(accessToken: token);
    state = const AuthState(isAuthenticated: true, accessToken: token);
  }

  Future<void> logout() async {
    await ref.read(secureStorageServiceProvider).clearTokens();
    state = const AuthState(isAuthenticated: false);
  }
}

final authProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);
