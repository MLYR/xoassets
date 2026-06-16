import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_page.dart';
import '../providers/auth_provider.dart';

/// 登录页骨架：本阶段只做 mock 登录，不调用真实接口。
class LoginPage extends ConsumerWidget {
  const LoginPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return XoPage(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: XoSpacing.xl),
          Text('欢迎回来', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: XoSpacing.xs),
          const Text('登录小〇财迹，继续查看你的资产复盘'),
          const SizedBox(height: XoSpacing.lg),
          XoCard(
            child: Column(
              children: [
                const TextField(
                  keyboardType: TextInputType.emailAddress,
                  decoration: InputDecoration(labelText: '邮箱'),
                ),
                const SizedBox(height: XoSpacing.md),
                const TextField(
                  obscureText: true,
                  decoration: InputDecoration(labelText: '验证码 / 密码'),
                ),
                const SizedBox(height: XoSpacing.lg),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: () async {
                      await ref.read(authProvider.notifier).mockLogin();
                      if (context.mounted) {
                        context.go(AppRoutes.main);
                      }
                    },
                    child: const Text('Mock 登录'),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
