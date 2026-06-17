import 'package:flutter/material.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_money_text.dart';
import '../../data/repositories/investment_repository.dart';

/// 投资总览卡，展示后端投资模块汇总，收益不在 App 端计算。
class InvestmentOverviewCard extends StatelessWidget {
  const InvestmentOverviewCard({required this.overview, super.key});

  final InvestmentOverviewModel overview;

  @override
  Widget build(BuildContext context) {
    return XoCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('投资总资产'),
          const SizedBox(height: XoSpacing.xs),
          XoMoneyText(overview.totalInvestmentAsset, size: XoMoneySize.large),
          const SizedBox(height: XoSpacing.md),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _InvestmentMetric(label: '持仓浮盈', value: overview.holdingProfit),
              _InvestmentMetric(label: '昨日收益', value: overview.yesterdayProfit),
            ],
          ),
          if (overview.todayProfitStatusLabel?.isNotEmpty == true) ...[
            const SizedBox(height: XoSpacing.sm),
            Text(overview.todayProfitStatusLabel!),
          ],
        ],
      ),
    );
  }
}

class _InvestmentMetric extends StatelessWidget {
  const _InvestmentMetric({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final semantic = value.startsWith('-')
        ? XoMoneySemantic.expense
        : XoMoneySemantic.income;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label),
        XoMoneyText(value, size: XoMoneySize.small, semantic: semantic),
      ],
    );
  }
}
