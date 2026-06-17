import 'package:flutter/material.dart';

import '../design/xo_radius.dart';
import '../design/xo_spacing.dart';

/// 统一底部弹窗样式。
class XoBottomSheet extends StatelessWidget {
  const XoBottomSheet({required this.child, super.key, this.title});

  final String? title;
  final Widget child;

  static Future<T?> show<T>(
    BuildContext context, {
    required Widget child,
    String? title,
  }) {
    return showModalBottomSheet<T>(
      context: context,
      showDragHandle: true,
      backgroundColor: Theme.of(context).colorScheme.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(XoRadius.xl)),
      ),
      builder: (_) => XoBottomSheet(title: title, child: child),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        XoSpacing.md,
        0,
        XoSpacing.md,
        XoSpacing.lg,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (title != null) ...[
            Text(title!, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: XoSpacing.md),
          ],
          child,
        ],
      ),
    );
  }
}
