/// 当前登录用户资料，业务 ID 始终以字符串保存，避免 Long 精度问题。
class AuthUser {
  const AuthUser({
    required this.id,
    required this.username,
    required this.nickname,
    this.avatarUrl,
  });

  final String id;
  final String username;
  final String nickname;
  final String? avatarUrl;

  factory AuthUser.fromJson(Object? value) {
    final json = value as Map<String, dynamic>;
    return AuthUser(
      id: json['id']?.toString() ?? '',
      username: json['username']?.toString() ?? '',
      nickname: json['nickname']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString(),
    );
  }
}
