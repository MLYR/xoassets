import 'package:flutter/material.dart';

import '../../../../core/design/xo_assets.dart';
import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_radius.dart';
import '../../../../core/design/xo_spacing.dart';

/// 登录/注册统一品牌头部，品牌 Logo 使用本次视觉稿裁切资源。
class XoAuthHeader extends StatelessWidget {
  const XoAuthHeader({super.key, this.compact = false});

  final bool compact;

  @override
  Widget build(BuildContext context) {
    final logoSize = compact ? 56.0 : 72.0;
    return Row(
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(XoRadius.lg),
          child: Image.asset(
            XoAssets.authLogo,
            width: logoSize,
            height: logoSize,
            fit: BoxFit.cover,
          ),
        ),
        const SizedBox(width: XoSpacing.md),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'XOAssets',
              style: TextStyle(
                color: XoColors.textMain,
                fontSize: compact ? 24 : 28,
                fontWeight: FontWeight.w800,
                letterSpacing: -0.6,
              ),
            ),
            const SizedBox(height: 2),
            const Text(
              '小 〇 财 迹',
              style: TextStyle(
                color: XoColors.textMain,
                fontSize: 16,
                letterSpacing: 2,
              ),
            ),
          ],
        ),
      ],
    );
  }
}
