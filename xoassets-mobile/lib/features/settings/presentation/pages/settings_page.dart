import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_page.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../../main/presentation/providers/main_tab_provider.dart';
import '../providers/app_settings_provider.dart';

/// 设置页接入本地偏好持久化，主题支持跟随系统 / 浅色 / 深色。
class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(appSettingsProvider);

    return XoPage(
      appBar: AppBar(title: const Text('设置')),
      child: Column(
        children: [
          XoCard(
            child: Column(
              children: [
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('隐藏金额'),
                  subtitle: const Text('首页和资产卡片显示为 ¥****'),
                  value: settings.hideAmount,
                  onChanged: ref
                      .read(appSettingsProvider.notifier)
                      .setHideAmount,
                ),
                const Divider(height: XoSpacing.lg),
                _ThemeModeTile(
                  title: '跟随系统',
                  value: XoThemeModeSetting.system,
                  selected: settings.themeModeSetting,
                  onTap: (value) => _setTheme(ref, value),
                ),
                _ThemeModeTile(
                  title: '浅色模式',
                  value: XoThemeModeSetting.light,
                  selected: settings.themeModeSetting,
                  onTap: (value) => _setTheme(ref, value),
                ),
                _ThemeModeTile(
                  title: '深色模式',
                  value: XoThemeModeSetting.dark,
                  selected: settings.themeModeSetting,
                  onTap: (value) => _setTheme(ref, value),
                ),
              ],
            ),
          ),
          const SizedBox(height: XoSpacing.md),
          XoCard(
            child: Column(
              children: [
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.cleaning_services_outlined),
                  title: const Text('清理缓存'),
                  subtitle: const Text('第一版不缓存业务数据，仅保留登录态和偏好设置。'),
                  onTap: () => ScaffoldMessenger.of(
                    context,
                  ).showSnackBar(const SnackBar(content: Text('暂无可清理的业务缓存'))),
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.logout),
                  title: const Text('退出登录'),
                  onTap: () => _confirmLogout(context, ref),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _setTheme(WidgetRef ref, XoThemeModeSetting value) {
    ref.read(appSettingsProvider.notifier).setThemeMode(value);
  }

  Future<void> _confirmLogout(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('退出登录'),
        content: const Text('退出后需要重新登录才能查看资产数据。'),
        actions: [
          TextButton(
            onPressed: () => context.pop(false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => context.pop(true),
            child: const Text('退出'),
          ),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) {
      return;
    }
    await ref.read(authProvider.notifier).logout();
    ref.read(mainTabProvider.notifier).setIndex(0);
    if (context.mounted) {
      context.go(AppRoutes.login);
    }
  }
}

class _ThemeModeTile extends StatelessWidget {
  const _ThemeModeTile({
    required this.title,
    required this.value,
    required this.selected,
    required this.onTap,
  });

  final String title;
  final XoThemeModeSetting value;
  final XoThemeModeSetting selected;
  final ValueChanged<XoThemeModeSetting> onTap;

  @override
  Widget build(BuildContext context) {
    final active = value == selected;
    return ListTile(
      contentPadding: EdgeInsets.zero,
      title: Text(title),
      trailing: Icon(
        active ? Icons.check_circle : Icons.circle_outlined,
        color: active ? Theme.of(context).colorScheme.primary : null,
      ),
      onTap: () => onTap(value),
    );
  }
}
