import 'package:flutter/material.dart';

import '../../../../core/design/xo_colors.dart';
import '../../../../core/widgets/xo_money_text.dart';

/// 流水列表项，展示后端返回的账户、分类和发生时间。
class TransactionItem extends StatelessWidget {
  const TransactionItem({
    required this.title,
    required this.amount,
    super.key,
    this.subtitle = '今天',
  });

  final String title;
  final String amount;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: const CircleAvatar(
        backgroundColor: XoColors.primaryLight,
        child: Icon(Icons.receipt_long, color: XoColors.primary),
      ),
      title: Text(title),
      subtitle: Text(
        subtitle,
        style: const TextStyle(color: XoColors.textSecondary),
      ),
      trailing: XoMoneyText(
        amount,
        size: XoMoneySize.small,
        semantic: amount.startsWith('-')
            ? XoMoneySemantic.expense
            : XoMoneySemantic.income,
      ),
    );
  }
}
