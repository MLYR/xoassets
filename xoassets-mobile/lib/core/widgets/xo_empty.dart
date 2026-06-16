import 'package:flutter/material.dart';

import '../design/xo_colors.dart';
import '../design/xo_spacing.dart';

/// 空状态组件，用于列表暂无数据的轻量展示。
class XoEmpty extends StatelessWidget {
  const XoEmpty({super.key, this.message = '暂无数据'});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(XoSpacing.lg),
      child: Column(
        children: [
          const Icon(Icons.inbox_outlined, color: XoColors.textPlaceholder),
          const SizedBox(height: XoSpacing.sm),
          Text(message, style: const TextStyle(color: XoColors.textSecondary)),
        ],
      ),
    );
  }
}
