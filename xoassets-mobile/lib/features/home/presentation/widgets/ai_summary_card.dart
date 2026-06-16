import 'package:flutter/material.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';

/// AI 总结卡，本阶段展示 mock 文案，不调用真实 AI。
class AiSummaryCard extends StatelessWidget {
  const AiSummaryCard({required this.summary, super.key});

  final String summary;

  @override
  Widget build(BuildContext context) {
    return XoCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('AI 今日总结', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: XoSpacing.sm),
          Text(summary),
        ],
      ),
    );
  }
}
