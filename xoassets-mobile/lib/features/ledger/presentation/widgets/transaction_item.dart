import 'package:flutter/material.dart';

import '../../../../core/design/xo_colors.dart';
import '../../../../core/widgets/xo_money_text.dart';

/// 流水列表项占位组件。
class TransactionItem extends StatelessWidget {
  const TransactionItem({required this.title, required this.amount, super.key});

  final String title;
  final String amount;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: const CircleAvatar(
        backgroundColor: XoColors.primaryLight,
        child: Icon(Icons.receipt_long, color: XoColors.primary),
      ),
      title: Text(title),
      subtitle: const Text(
        '今天',
        style: TextStyle(color: XoColors.textSecondary),
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
