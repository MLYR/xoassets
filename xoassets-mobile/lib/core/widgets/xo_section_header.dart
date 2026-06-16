import 'package:flutter/material.dart';

import '../design/xo_text_styles.dart';

/// 页面区块标题，统一标题和右侧操作排版。
class XoSectionHeader extends StatelessWidget {
  const XoSectionHeader({required this.title, super.key, this.action});

  final String title;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(child: Text(title, style: XoTextStyles.titleMedium)),
        ?action,
      ],
    );
  }
}
