import 'package:flutter/material.dart';

import '../design/xo_radius.dart';
import '../design/xo_spacing.dart';

/// 统一卡片组件，集中控制背景、圆角、阴影和内边距。
class XoCard extends StatelessWidget {
  const XoCard({
    required this.child,
    super.key,
    this.padding = const EdgeInsets.all(XoSpacing.md),
    this.margin = EdgeInsets.zero,
  });

  final Widget child;
  final EdgeInsets padding;
  final EdgeInsets margin;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: margin,
      child: Material(
        color: Theme.of(context).colorScheme.surface,
        borderRadius: BorderRadius.circular(XoRadius.card),
        elevation: 0,
        shadowColor: const Color(0x14000000),
        child: Container(
          width: double.infinity,
          padding: padding,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(XoRadius.card),
            border: Border.all(
              color: Theme.of(context).dividerColor.withValues(alpha: 0.48),
            ),
          ),
          child: child,
        ),
      ),
    );
  }
}
