import 'package:flutter/material.dart';

import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_money_text.dart';

/// 投资摘要卡明确区分基金昨日、股票今日和虚拟货币 24h 收益口径。
class InvestmentSummaryCard extends StatelessWidget {
  const InvestmentSummaryCard({
    required this.fundYesterdayProfit,
    required this.stockTodayProfit,
    required this.crypto24hProfit,
    super.key,
  });

  final String fundYesterdayProfit;
  final String stockTodayProfit;
  final String crypto24hProfit;

  @override
  Widget build(BuildContext context) {
    return XoCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('投资摘要', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: XoSpacing.md),
          _ProfitRow(label: '基金昨日收益', value: fundYesterdayProfit),
          _ProfitRow(label: '股票今日收益', value: stockTodayProfit),
          _ProfitRow(label: '虚拟货币 24h 收益', value: crypto24hProfit),
        ],
      ),
    );
  }
}

class _ProfitRow extends StatelessWidget {
  const _ProfitRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final amount = num.tryParse(value) ?? 0;
    return Padding(
      padding: const EdgeInsets.only(bottom: XoSpacing.sm),
      child: Row(
        children: [
          Expanded(
            child: Text(
              label,
              style: const TextStyle(color: XoColors.textSecondary),
            ),
          ),
          XoMoneyText(
            value,
            size: XoMoneySize.small,
            semantic: amount >= 0
                ? XoMoneySemantic.income
                : XoMoneySemantic.expense,
          ),
        ],
      ),
    );
  }
}
