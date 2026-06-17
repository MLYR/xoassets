import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_page.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../../main/presentation/providers/main_tab_provider.dart';
import '../providers/app_settings_provider.dart';

/// 设置页骨架，包含金额隐藏和深色模式预留开关。
class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(appSettingsProvider);

    return XoPage(
      appBar: AppBar(title: const Text('设置')),
      child: XoCard(
        child: Column(
          children: [
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('金额隐藏'),
              value: settings.hideAmount,
              onChanged: ref.read(appSettingsProvider.notifier).setHideAmount,
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('深色模式'),
              subtitle: const Text('主题能力已预留，后续接入持久化'),
              value: settings.darkMode,
              onChanged: ref.read(appSettingsProvider.notifier).setDarkMode,
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.cleaning_services_outlined),
              title: const Text('清理缓存'),
              onTap: () {},
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.logout),
              title: const Text('退出登录'),
              onTap: () => _confirmLogout(context, ref),
            ),
            const SizedBox(height: XoSpacing.md),
            const Text('版本 ${AppConstants.versionLabel}'),
          ],
        ),
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
