import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/errors/error_handler.dart';
import '../../data/models/auth_user.dart';
import '../../data/repositories/auth_repository.dart';

enum AuthStatus { unknown, authenticated, unauthenticated }

/// 登录状态模型，集中承载登录态、用户资料、加载和错误。
class AuthState {
  const AuthState({
    required this.status,
    this.accessToken,
    this.user,
    this.isLoading = false,
    this.errorMessage,
  });

  final AuthStatus status;
  final String? accessToken;
  final AuthUser? user;
  final bool isLoading;
  final String? errorMessage;

  bool get isAuthenticated => status == AuthStatus.authenticated;

  AuthState copyWith({
    AuthStatus? status,
    String? accessToken,
    AuthUser? user,
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
  }) {
    return AuthState(
      status: status ?? this.status,
      accessToken: accessToken ?? this.accessToken,
      user: user ?? this.user,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    );
  }

  static const initial = AuthState(status: AuthStatus.unknown);
}

/// Auth provider 负责真实登录、登录态恢复和退出登录。
class AuthController extends Notifier<AuthState> {
  @override
  AuthState build() {
    return AuthState.initial;
  }

  Future<void> restoreSession() async {
    if (state.status != AuthStatus.unknown) {
      return;
    }

    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final session = await ref.read(authRepositoryProvider).restoreSession();
      if (session == null) {
        state = const AuthState(status: AuthStatus.unauthenticated);
        return;
      }
      state = AuthState(
        status: AuthStatus.authenticated,
        accessToken: session.accessToken,
        user: session.user,
      );
    } catch (error) {
      final appError = ErrorHandler.fromObject(error);
      state = AuthState(
        status: AuthStatus.unauthenticated,
        errorMessage: appError.message,
      );
    }
  }

  Future<bool> login({
    required String username,
    required String password,
  }) async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final session = await ref
          .read(authRepositoryProvider)
          .login(username: username, password: password);
      state = AuthState(
        status: AuthStatus.authenticated,
        accessToken: session.accessToken,
        user: session.user,
      );
      return true;
    } catch (error) {
      final appError = ErrorHandler.fromObject(error);
      state = AuthState(
        status: AuthStatus.unauthenticated,
        errorMessage: appError.message,
      );
      return false;
    }
  }

  Future<void> logout() async {
    await ref.read(authRepositoryProvider).logout();
    state = const AuthState(status: AuthStatus.unauthenticated);
  }
}

final authProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);
