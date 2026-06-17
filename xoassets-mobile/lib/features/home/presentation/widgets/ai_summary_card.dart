import 'package:flutter/material.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';

/// AI 总结卡，仅展示后端模板化报告摘要，不在 App 端调用真实 AI。
class AiSummaryCard extends StatelessWidget {
  const AiSummaryCard({
    required this.summary,
    super.key,
    this.title = 'AI 今日总结',
  });

  final String title;
  final String summary;

  @override
  Widget build(BuildContext context) {
    return XoCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: XoSpacing.sm),
          Text(summary),
        ],
      ),
    );
  }
}
