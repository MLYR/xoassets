import 'auth_user.dart';

/// 登录会话，refreshToken 兼容后续后端扩展，当前后端可为空。
class AuthSession {
  const AuthSession({
    required this.accessToken,
    required this.user,
    this.refreshToken,
  });

  final String accessToken;
  final String? refreshToken;
  final AuthUser user;

  factory AuthSession.fromJson(Object? value) {
    final json = value as Map<String, dynamic>;
    return AuthSession(
      accessToken: json['token']?.toString() ?? '',
      refreshToken: json['refreshToken']?.toString(),
      user: AuthUser.fromJson(json['user']),
    );
  }
}
