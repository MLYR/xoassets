import 'package:flutter/material.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_page.dart';

/// AI 报告页骨架，本阶段只展示模板化摘要。
class ReportPage extends StatelessWidget {
  const ReportPage({super.key});

  @override
  Widget build(BuildContext context) {
    return XoPage(
      appBar: AppBar(title: const Text('AI 财务报告')),
      child: const XoCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('今日复盘'),
            SizedBox(height: XoSpacing.sm),
            Text('今日支出正常，餐饮支出略高，投资整体小幅上涨。'),
            SizedBox(height: XoSpacing.md),
            Text('下一阶段将接入后端 AI 报告接口。'),
          ],
        ),
      ),
    );
  }
}
