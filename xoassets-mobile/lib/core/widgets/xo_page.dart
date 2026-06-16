import 'package:flutter/material.dart';

import '../design/xo_colors.dart';
import '../design/xo_spacing.dart';

/// 页面统一容器，负责背景、安全区、滚动和默认边距。
class XoPage extends StatelessWidget {
  const XoPage({
    required this.child,
    super.key,
    this.scrollable = true,
    this.padding = const EdgeInsets.all(XoSpacing.md),
    this.appBar,
  });

  final Widget child;
  final bool scrollable;
  final EdgeInsets padding;
  final PreferredSizeWidget? appBar;

  @override
  Widget build(BuildContext context) {
    final body = SafeArea(
      child: Padding(
        padding: padding,
        child: scrollable ? SingleChildScrollView(child: child) : child,
      ),
    );

    return Scaffold(
      backgroundColor: XoColors.pageBg,
      appBar: appBar,
      body: body,
    );
  }
}
