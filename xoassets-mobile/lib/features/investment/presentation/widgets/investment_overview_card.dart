import 'package:flutter/material.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_money_text.dart';

/// 投资总览卡，展示总资产、持仓浮盈和累计收益。
class InvestmentOverviewCard extends StatelessWidget {
  const InvestmentOverviewCard({super.key});

  @override
  Widget build(BuildContext context) {
    return const XoCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('投资总资产'),
          SizedBox(height: XoSpacing.xs),
          XoMoneyText('56820.00', size: XoMoneySize.large),
          SizedBox(height: XoSpacing.md),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _InvestmentMetric(label: '持仓浮盈', value: '2380.50'),
              _InvestmentMetric(label: '累计收益', value: '5680.20'),
            ],
          ),
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label),
        XoMoneyText(
          value,
          size: XoMoneySize.small,
          semantic: XoMoneySemantic.income,
        ),
      ],
    );
  }
}
