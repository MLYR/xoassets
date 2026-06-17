import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_page.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../../main/presentation/providers/main_tab_provider.dart';

/// 我的页骨架，聚合账户、分类、预算、报告和设置入口。
class ProfilePage extends ConsumerWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authProvider).user;
    return XoPage(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          XoCard(
            child: Row(
              children: [
                const CircleAvatar(
                  radius: 28,
                  backgroundColor: XoColors.primaryLight,
                  child: Icon(Icons.person, color: XoColors.primary),
                ),
                const SizedBox(width: XoSpacing.md),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      user?.nickname.isNotEmpty == true
                          ? user!.nickname
                          : user?.username ?? 'XOAssets 用户',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    Text(
                      user?.username ?? 'XOAssets Mobile V2',
                      style: TextStyle(color: XoColors.textSecondary),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: XoSpacing.md),
          XoCard(
            child: Column(
              children: [
                _ProfileEntry(
                  icon: Icons.account_balance_wallet,
                  label: '账户管理',
                  onTap: () {},
                ),
                _ProfileEntry(
                  icon: Icons.category_outlined,
                  label: '分类管理',
                  onTap: () {},
                ),
                _ProfileEntry(
                  icon: Icons.savings_outlined,
                  label: '预算管理',
                  onTap: () => context.push(AppRoutes.budget),
                ),
                _ProfileEntry(
                  icon: Icons.auto_awesome_outlined,
                  label: 'AI 报告',
                  onTap: () => context.push(AppRoutes.report),
                ),
                _ProfileEntry(
                  icon: Icons.backup_outlined,
                  label: '数据备份',
                  onTap: () {},
                ),
                _ProfileEntry(
                  icon: Icons.settings_outlined,
                  label: '设置',
                  onTap: () => context.push(AppRoutes.settings),
                ),
                _ProfileEntry(
                  icon: Icons.info_outline,
                  label: '关于 XOAssets',
                  onTap: () {},
                ),
                _ProfileEntry(
                  icon: Icons.logout,
                  label: '退出登录',
                  onTap: () => _confirmLogout(context, ref),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _confirmLogout(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('退出登录'),
        content: const Text('退出后需要重新登录才能查看资产数据。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('退出'),
          ),
        ],
      ),
    );
    if (confirmed != true) {
      return;
    }

    await ref.read(authProvider.notifier).logout();
    ref.read(mainTabProvider.notifier).setIndex(0);
    if (context.mounted) {
      context.go(AppRoutes.login);
    }
  }
}

class _ProfileEntry extends StatelessWidget {
  const _ProfileEntry({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Icon(icon, color: XoColors.primary),
      title: Text(label),
      trailing: const Icon(Icons.chevron_right),
      onTap: onTap,
    );
  }
}
