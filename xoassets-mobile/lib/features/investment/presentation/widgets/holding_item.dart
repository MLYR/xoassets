import 'package:flutter/material.dart';

import '../../../../core/design/xo_colors.dart';
import '../../../../core/widgets/xo_money_text.dart';

/// 持仓列表项，占位展示名称、市值和收益。
class HoldingItem extends StatelessWidget {
  const HoldingItem({
    required this.name,
    required this.marketValue,
    required this.profitLabel,
    required this.profit,
    super.key,
  });

  final String name;
  final String marketValue;
  final String profitLabel;
  final String profit;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      title: Text(name),
      subtitle: Text(
        profitLabel,
        style: const TextStyle(color: XoColors.textSecondary),
      ),
      trailing: Column(
        crossAxisAlignment: CrossAxisAlignment.end,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          XoMoneyText(marketValue, size: XoMoneySize.small),
          XoMoneyText(
            profit,
            size: XoMoneySize.small,
            semantic: profit.startsWith('-')
                ? XoMoneySemantic.expense
                : XoMoneySemantic.income,
          ),
        ],
      ),
    );
  }
}
