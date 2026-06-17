import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../features/settings/presentation/providers/app_settings_provider.dart';
import 'router.dart';
import 'theme.dart';

/// 应用根组件，只负责组合路由、主题和全局 Material 入口。
class XoAssetsApp extends ConsumerWidget {
  const XoAssetsApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(appRouterProvider);
    final settings = ref.watch(appSettingsProvider);
    return MaterialApp.router(
      title: 'XOAssets',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: settings.materialThemeMode,
      routerConfig: router,
    );
  }
}
