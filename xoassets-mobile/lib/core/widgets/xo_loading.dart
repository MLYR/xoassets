import 'package:flutter/material.dart';

import '../design/xo_colors.dart';
import '../design/xo_spacing.dart';

/// 全局加载态组件。
class XoLoading extends StatelessWidget {
  const XoLoading({super.key, this.message = '加载中'});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const CircularProgressIndicator(color: XoColors.primary),
          const SizedBox(height: XoSpacing.sm),
          Text(message, style: const TextStyle(color: XoColors.textSecondary)),
        ],
      ),
    );
  }
}
