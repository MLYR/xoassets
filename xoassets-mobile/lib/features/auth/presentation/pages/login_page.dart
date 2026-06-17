import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_button.dart';
import '../../../../core/widgets/xo_text_field.dart';
import '../providers/auth_provider.dart';
import '../widgets/xo_auth_scaffold.dart';

/// 登录页：复刻视觉稿，并通过 AuthController 接入真实 `/api/auth/login`。
class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _rememberMe = true;
  bool _obscurePassword = true;

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    return XoAuthScaffold(
      title: '欢迎回来',
      subtitle: '登录您的账户，开启智能资产管理之旅',
      form: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          XoTextField(
            controller: _usernameController,
            enabled: !authState.isLoading,
            hintText: '手机号/邮箱',
            icon: Icons.person_outline,
            keyboardType: TextInputType.emailAddress,
            textInputAction: TextInputAction.next,
          ),
          const SizedBox(height: XoSpacing.formGap),
          XoTextField(
            controller: _passwordController,
            enabled: !authState.isLoading,
            hintText: '密码',
            icon: Icons.lock_outline,
            obscureText: _obscurePassword,
            textInputAction: TextInputAction.done,
            trailing: IconButton(
              onPressed: () => setState(() {
                _obscurePassword = !_obscurePassword;
              }),
              icon: Icon(
                _obscurePassword
                    ? Icons.visibility_outlined
                    : Icons.visibility_off_outlined,
                color: XoColors.textSecondary,
              ),
            ),
            onSubmitted: (_) => _submit(context),
          ),
          const SizedBox(height: XoSpacing.md),
          Row(
            children: [
              _RememberCheckbox(
                value: _rememberMe,
                onChanged: authState.isLoading
                    ? null
                    : (value) => setState(() => _rememberMe = value),
              ),
              const SizedBox(width: XoSpacing.sm),
              const Text('记住我', style: TextStyle(color: XoColors.textMain)),
              const Spacer(),
              TextButton(
                onPressed: authState.isLoading
                    ? null
                    : () => _showLoginDialog(
                        context,
                        title: '找回密码',
                        message: '请先联系管理员重置密码，后续会接入自助找回能力。',
                      ),
                child: const Text('忘记密码？'),
              ),
            ],
          ),
          const SizedBox(height: XoSpacing.lg),
          XoButton(
            label: '立即登录',
            isLoading: authState.isLoading,
            onPressed: authState.isLoading ? null : () => _submit(context),
          ),
          const SizedBox(height: XoSpacing.lg),
          const _OtherLoginDivider(),
          const SizedBox(height: XoSpacing.md),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _RoundAuthAction(
                icon: Icons.chat_bubble_outline,
                color: const Color(0xFF1AAD19),
                onTap: () => _showLoginDialog(
                  context,
                  title: '微信登录',
                  message: '当前版本请使用手机号/邮箱与密码登录，微信账号可作为后续增强。',
                ),
              ),
              const SizedBox(width: XoSpacing.xl),
              _RoundAuthAction(
                icon: Icons.phone_iphone_outlined,
                color: XoColors.primary,
                onTap: () => FocusScope.of(context).requestFocus(FocusNode()),
              ),
            ],
          ),
        ],
      ),
      bottomLink: _AuthSwitchLink(
        prefix: '还没有账号？',
        action: '立即注册',
        onTap: () => context.go(AppRoutes.register),
      ),
    );
  }

  Future<void> _submit(BuildContext context) async {
    final username = _usernameController.text.trim();
    final password = _passwordController.text;
    if (username.isEmpty || password.isEmpty) {
      await _showLoginDialog(
        context,
        title: '登录信息不完整',
        message: '请输入手机号/邮箱和密码。',
      );
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

  /// 登录失败和辅助动作统一用弹窗提示，避免网络错误挤在表单里破坏版式。
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

class _RememberCheckbox extends StatelessWidget {
  const _RememberCheckbox({required this.value, required this.onChanged});

  final bool value;
  final ValueChanged<bool>? onChanged;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onChanged == null ? null : () => onChanged!(!value),
      borderRadius: BorderRadius.circular(14),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 160),
        width: 26,
        height: 26,
        decoration: BoxDecoration(
          color: value ? XoColors.primary : Colors.white,
          borderRadius: BorderRadius.circular(13),
          border: Border.all(color: value ? XoColors.primary : XoColors.border),
        ),
        child: value
            ? const Icon(Icons.check, color: Colors.white, size: 18)
            : null,
      ),
    );
  }
}

class _OtherLoginDivider extends StatelessWidget {
  const _OtherLoginDivider();

  @override
  Widget build(BuildContext context) {
    return const Row(
      children: [
        Expanded(child: Divider(color: XoColors.divider)),
        Padding(
          padding: EdgeInsets.symmetric(horizontal: XoSpacing.md),
          child: Text(
            '其他方式登录',
            style: TextStyle(color: XoColors.textSecondary),
          ),
        ),
        Expanded(child: Divider(color: XoColors.divider)),
      ],
    );
  }
}

class _RoundAuthAction extends StatelessWidget {
  const _RoundAuthAction({
    required this.icon,
    required this.color,
    required this.onTap,
  });

  final IconData icon;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(28),
      child: Container(
        width: 56,
        height: 56,
        decoration: BoxDecoration(
          color: Colors.white,
          shape: BoxShape.circle,
          border: Border.all(color: XoColors.divider),
        ),
        child: Icon(icon, color: color),
      ),
    );
  }
}

class _AuthSwitchLink extends StatelessWidget {
  const _AuthSwitchLink({
    required this.prefix,
    required this.action,
    required this.onTap,
  });

  final String prefix;
  final String action;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(prefix, style: const TextStyle(color: XoColors.textSecondary)),
        TextButton.icon(
          onPressed: onTap,
          label: Text(action),
          icon: const Icon(Icons.chevron_right, size: 18),
        ),
      ],
    );
  }
}
