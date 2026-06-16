import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_page.dart';

/// 启动页：后续用于检查 Token，本阶段 500ms 后进入主页面。
class SplashPage extends StatefulWidget {
  const SplashPage({super.key});

  @override
  State<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends State<SplashPage> {
  @override
  void initState() {
    super.initState();
    Future<void>.delayed(const Duration(milliseconds: 500), () {
      if (mounted) {
        context.go(AppRoutes.main);
      }
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
