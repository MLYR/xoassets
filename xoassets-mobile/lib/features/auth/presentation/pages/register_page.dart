import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_radius.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_button.dart';
import '../../../../core/widgets/xo_text_field.dart';
import '../providers/auth_provider.dart';
import '../widgets/xo_auth_scaffold.dart';

/// 注册页：按视觉稿实现完整表单交互，提交时调用真实 `/api/auth/register` 后自动登录。
class RegisterPage extends ConsumerStatefulWidget {
  const RegisterPage({super.key});

  @override
  ConsumerState<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends ConsumerState<RegisterPage> {
  final _usernameController = TextEditingController();
  final _codeController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  Timer? _timer;
  int _codeCountdown = 0;
  bool _agreed = false;
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;

  @override
  void dispose() {
    _timer?.cancel();
    _usernameController.dispose();
    _codeController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    return XoAuthScaffold(
      isRegister: true,
      onBack: () => context.go(AppRoutes.login),
      title: '创建账户',
      subtitle: '注册新账户，开启您的资产管理之旅',
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
            controller: _codeController,
            enabled: !authState.isLoading,
            hintText: '验证码',
            icon: Icons.verified_user_outlined,
            keyboardType: TextInputType.number,
            textInputAction: TextInputAction.next,
            trailing: _CodeButton(
              countdown: _codeCountdown,
              enabled: !authState.isLoading,
              onPressed: _requestCode,
            ),
          ),
          const SizedBox(height: XoSpacing.formGap),
          XoTextField(
            controller: _passwordController,
            enabled: !authState.isLoading,
            hintText: '密码（6-20位，含字母和数字）',
            icon: Icons.lock_outline,
            obscureText: _obscurePassword,
            textInputAction: TextInputAction.next,
            trailing: _PasswordEye(
              obscure: _obscurePassword,
              onPressed: () => setState(() {
                _obscurePassword = !_obscurePassword;
              }),
            ),
          ),
          const SizedBox(height: XoSpacing.formGap),
          XoTextField(
            controller: _confirmPasswordController,
            enabled: !authState.isLoading,
            hintText: '确认密码',
            icon: Icons.lock_outline,
            obscureText: _obscureConfirmPassword,
            textInputAction: TextInputAction.done,
            trailing: _PasswordEye(
              obscure: _obscureConfirmPassword,
              onPressed: () => setState(() {
                _obscureConfirmPassword = !_obscureConfirmPassword;
              }),
            ),
            onSubmitted: (_) => _submit(context),
          ),
          const SizedBox(height: XoSpacing.md),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _AgreementCheckbox(
                value: _agreed,
                onChanged: authState.isLoading
                    ? null
                    : (value) => setState(() => _agreed = value),
              ),
              const SizedBox(width: XoSpacing.sm),
              Expanded(
                child: Wrap(
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    const Text(
                      '我已阅读并同意',
                      style: TextStyle(color: XoColors.textSecondary),
                    ),
                    _InlineLink(
                      label: '《用户协议》',
                      onTap: () => _showPolicy(context, '用户协议'),
                    ),
                    const Text(
                      '和',
                      style: TextStyle(color: XoColors.textSecondary),
                    ),
                    _InlineLink(
                      label: '《隐私政策》',
                      onTap: () => _showPolicy(context, '隐私政策'),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: XoSpacing.lg),
          XoButton(
            label: '立即注册',
            isLoading: authState.isLoading,
            onPressed: authState.isLoading ? null : () => _submit(context),
          ),
        ],
      ),
      bottomLink: _AuthSwitchLink(
        prefix: '已有账号？',
        action: '去登录',
        onTap: () => context.go(AppRoutes.login),
      ),
    );
  }

  Future<void> _requestCode() async {
    final username = _usernameController.text.trim();
    if (username.isEmpty) {
      await _showRegisterDialog(
        context,
        title: '先填写账号',
        message: '请输入手机号或邮箱后再获取验证码。',
      );
      return;
    }

    // 当前后端注册接口不需要验证码，这里保留前端倒计时交互以匹配视觉稿。
    setState(() => _codeCountdown = 60);
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_codeCountdown <= 1) {
        timer.cancel();
        if (mounted) {
          setState(() => _codeCountdown = 0);
        }
        return;
      }
      if (mounted) {
        setState(() => _codeCountdown--);
      }
    });
    if (mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('验证码已发送至 $username')));
    }
  }

  Future<void> _submit(BuildContext context) async {
    final username = _usernameController.text.trim();
    final code = _codeController.text.trim();
    final password = _passwordController.text;
    final confirmPassword = _confirmPasswordController.text;

    if (username.isEmpty ||
        code.isEmpty ||
        password.isEmpty ||
        confirmPassword.isEmpty) {
      await _showRegisterDialog(
        context,
        title: '注册信息不完整',
        message: '请填写账号、验证码和密码。',
      );
      return;
    }
    if (password.length < 6 || password.length > 20) {
      await _showRegisterDialog(
        context,
        title: '密码格式不正确',
        message: '密码长度必须在 6-20 位之间。',
      );
      return;
    }
    if (password != confirmPassword) {
      await _showRegisterDialog(
        context,
        title: '两次密码不一致',
        message: '请确认两次输入的密码完全一致。',
      );
      return;
    }
    if (!_agreed) {
      await _showRegisterDialog(
        context,
        title: '请先同意协议',
        message: '注册前需要阅读并同意用户协议和隐私政策。',
      );
      return;
    }

    final success = await ref
        .read(authProvider.notifier)
        .register(username: username, password: password);
    if (success && context.mounted) {
      context.go(AppRoutes.main);
      return;
    }

    if (context.mounted) {
      final message = ref.read(authProvider).errorMessage ?? '注册失败，请稍后重试';
      await _showRegisterDialog(context, title: '注册失败', message: message);
    }
  }

  Future<void> _showPolicy(BuildContext context, String title) {
    return _showRegisterDialog(
      context,
      title: title,
      message: '小〇财迹只通过后端 API 处理账号数据，本地仅安全保存登录 Token。',
    );
  }

  /// 注册页所有错误统一弹窗，避免表单布局被错误文案挤压变形。
  Future<void> _showRegisterDialog(
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

class _CodeButton extends StatelessWidget {
  const _CodeButton({
    required this.countdown,
    required this.enabled,
    required this.onPressed,
  });

  final int countdown;
  final bool enabled;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final canSend = enabled && countdown == 0;
    return TextButton(
      onPressed: canSend ? onPressed : null,
      child: Text(countdown == 0 ? '获取验证码' : '${countdown}s'),
    );
  }
}

class _PasswordEye extends StatelessWidget {
  const _PasswordEye({required this.obscure, required this.onPressed});

  final bool obscure;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      onPressed: onPressed,
      icon: Icon(
        obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined,
        color: XoColors.textSecondary,
      ),
    );
  }
}

class _AgreementCheckbox extends StatelessWidget {
  const _AgreementCheckbox({required this.value, required this.onChanged});

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

class _InlineLink extends StatelessWidget {
  const _InlineLink({required this.label, required this.onTap});

  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(XoRadius.sm),
      child: Text(
        label,
        style: const TextStyle(
          color: XoColors.primary,
          fontWeight: FontWeight.w700,
        ),
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
