import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_page.dart';
import '../providers/auth_provider.dart';

/// 登录页：通过 AuthController 接入真实 `/api/auth/login`。
class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    return XoPage(
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 430),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: XoSpacing.lg),
              Text(
                '欢迎回来',
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  color: XoColors.textMain,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: XoSpacing.xs),
              const Text(
                '登录小〇财迹，继续查看你的资产复盘',
                style: TextStyle(
                  color: XoColors.textSecondary,
                  fontSize: 14,
                  height: 1.35,
                ),
              ),
              const SizedBox(height: XoSpacing.md),
              XoCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    TextField(
                      controller: _usernameController,
                      enabled: !authState.isLoading,
                      textInputAction: TextInputAction.next,
                      decoration: const InputDecoration(
                        labelText: '用户名',
                        labelStyle: TextStyle(color: XoColors.textSecondary),
                      ),
                    ),
                    const SizedBox(height: XoSpacing.md),
                    TextField(
                      controller: _passwordController,
                      enabled: !authState.isLoading,
                      obscureText: true,
                      textInputAction: TextInputAction.done,
                      decoration: const InputDecoration(
                        labelText: '密码',
                        labelStyle: TextStyle(color: XoColors.textSecondary),
                      ),
                      onSubmitted: (_) => _submit(context),
                    ),
                    const SizedBox(height: XoSpacing.lg),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton(
                        onPressed: authState.isLoading
                            ? null
                            : () => _submit(context),
                        child: authState.isLoading
                            ? const SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                ),
                              )
                            : const Text(
                                '登录',
                                style: TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _submit(BuildContext context) async {
    final username = _usernameController.text.trim();
    final password = _passwordController.text;
    if (username.isEmpty || password.isEmpty) {
      await _showLoginDialog(context, title: '登录信息不完整', message: '请输入用户名和密码。');
      return;
    }

    final success = await ref
        .read(authProvider.notifier)
        .login(username: username, password: password);
    if (success && context.mounted) {
      context.go(AppRoutes.main);
      return;
    }

    if (context.mounted) {
      final message = ref.read(authProvider).errorMessage ?? '登录失败，请检查账号和密码';
      await _showLoginDialog(context, title: '登录失败', message: message);
    }
  }

  /// 登录失败统一用弹窗提示，避免 Flutter Web 网络错误直接挤在表单里。
  Future<void> _showLoginDialog(
    BuildContext context, {
    required String title,
    required String message,
  }) {
    return showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('知道了'),
          ),
        ],
      ),
    );
  }
}
