import 'package:flutter/material.dart';

import '../design/xo_spacing.dart';

/// 错误态组件，保留重试入口。
class XoErrorView extends StatelessWidget {
  const XoErrorView({required this.message, super.key, this.onRetry});

  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline),
          const SizedBox(height: XoSpacing.sm),
          Text(message),
          if (onRetry != null) ...[
            const SizedBox(height: XoSpacing.md),
            FilledButton(onPressed: onRetry, child: const Text('重试')),
          ],
        ],
      ),
    );
  }
}
