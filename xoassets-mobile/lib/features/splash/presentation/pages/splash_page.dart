import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_page.dart';
import '../../../auth/presentation/providers/auth_provider.dart';

/// 启动页：恢复 secure storage 中的 token，并按结果进入登录页或主页面。
class SplashPage extends ConsumerStatefulWidget {
  const SplashPage({super.key});

  @override
  ConsumerState<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends ConsumerState<SplashPage> {
  @override
  void initState() {
    super.initState();
    Future<void>.microtask(() async {
      await ref.read(authProvider.notifier).restoreSession();
      if (!mounted) {
        return;
      }
      final authState = ref.read(authProvider);
      context.go(authState.isAuthenticated ? AppRoutes.main : AppRoutes.login);
    });
  }

  @override
  Widget build(BuildContext context) {
    return const XoPage(
      scrollable: false,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircleAvatar(
              radius: 40,
              backgroundColor: XoColors.primaryLight,
              child: Text(
                'XO',
                style: TextStyle(
                  color: XoColors.primary,
                  fontWeight: FontWeight.w800,
                  fontSize: 24,
                ),
              ),
            ),
            SizedBox(height: XoSpacing.md),
            Text(AppConstants.appChineseName),
            SizedBox(height: XoSpacing.xs),
            Text(
              AppConstants.stageLabel,
              style: TextStyle(color: XoColors.textSecondary),
            ),
          ],
        ),
      ),
    );
  }
}
