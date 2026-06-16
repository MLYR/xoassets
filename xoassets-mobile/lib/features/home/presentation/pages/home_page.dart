import 'package:flutter/material.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_page.dart';
import '../../../../core/widgets/xo_section_header.dart';
import '../widgets/ai_summary_card.dart';
import '../widgets/asset_overview_card.dart';
import '../widgets/investment_summary_card.dart';
import '../widgets/quick_stat_card.dart';
import '../widgets/recent_transaction_list.dart';

/// 首页资产驾驶舱骨架，数据全部来自本地 mock。
class HomePage extends StatelessWidget {
  const HomePage({super.key});

  static const _mock = {
    'totalAsset': '128560.80',
    'netAsset': '96520.30',
    'todayExpense': '128.50',
    'monthExpense': '4268.90',
    'monthIncome': '18000.00',
    'monthBalance': '13731.10',
    'fundYesterdayProfit': '126.30',
    'stockTodayProfit': '-58.20',
    'crypto24hProfit': '88.12',
    'aiSummary': '今日支出正常，餐饮支出略高，投资整体小幅上涨。',
  };

  @override
  Widget build(BuildContext context) {
    return XoPage(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const XoSectionHeader(title: '早上好，看看今天的钱包'),
          const SizedBox(height: XoSpacing.md),
          AssetOverviewCard(
            totalAsset: _mock['totalAsset']!,
            netAsset: _mock['netAsset']!,
          ),
          const SizedBox(height: XoSpacing.md),
          GridView.count(
            crossAxisCount: 2,
            childAspectRatio: 1.8,
            crossAxisSpacing: XoSpacing.md,
            mainAxisSpacing: XoSpacing.md,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            children: [
              QuickStatCard(label: '今日支出', value: _mock['todayExpense']!),
              QuickStatCard(label: '本月支出', value: _mock['monthExpense']!),
              QuickStatCard(label: '本月收入', value: _mock['monthIncome']!),
              QuickStatCard(label: '本月结余', value: _mock['monthBalance']!),
            ],
          ),
          const SizedBox(height: XoSpacing.md),
          InvestmentSummaryCard(
            fundYesterdayProfit: _mock['fundYesterdayProfit']!,
            stockTodayProfit: _mock['stockTodayProfit']!,
            crypto24hProfit: _mock['crypto24hProfit']!,
          ),
          const SizedBox(height: XoSpacing.md),
          AiSummaryCard(summary: _mock['aiSummary']!),
          const SizedBox(height: XoSpacing.md),
          const RecentTransactionList(),
        ],
      ),
    );
  }
}
