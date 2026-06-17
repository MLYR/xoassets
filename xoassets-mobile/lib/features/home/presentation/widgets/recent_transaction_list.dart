import 'package:flutter/material.dart';

import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_empty.dart';
import '../../../../core/widgets/xo_money_text.dart';
import '../../../ledger/data/repositories/transaction_repository.dart';

/// 最近流水列表，首页只展示后端返回的最近 5 条。
class RecentTransactionList extends StatelessWidget {
  const RecentTransactionList({required this.items, super.key});

  final List<TransactionItemModel> items;

  @override
  Widget build(BuildContext context) {
    return XoCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('最近流水', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: XoSpacing.sm),
          if (items.isEmpty)
            const XoEmpty(message: '暂无最近流水')
          else
            for (final item in items)
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(item.title),
                subtitle: Text(
                  item.transactionTime.isEmpty ? '暂无时间' : item.transactionTime,
                  style: const TextStyle(color: XoColors.textSecondary),
                ),
                trailing: XoMoneyText(
                  item.signedAmount,
                  size: XoMoneySize.small,
                  semantic: item.signedAmount.startsWith('-')
                      ? XoMoneySemantic.expense
                      : XoMoneySemantic.income,
                ),
              ),
        ],
      ),
    );
  }
}
