import 'package:flutter/material.dart';

import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_money_text.dart';

/// 最近流水列表，使用 mock 数据验证列表骨架。
class RecentTransactionList extends StatelessWidget {
  const RecentTransactionList({super.key});

  @override
  Widget build(BuildContext context) {
    const items = [('早餐', '-18.00'), ('地铁', '-7.00'), ('工资', '18000.00')];

    return XoCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('最近流水', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: XoSpacing.sm),
          for (final item in items)
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(item.$1),
              subtitle: const Text(
                '今天',
                style: TextStyle(color: XoColors.textSecondary),
              ),
              trailing: XoMoneyText(
                item.$2,
                size: XoMoneySize.small,
                semantic: item.$2.startsWith('-')
                    ? XoMoneySemantic.expense
                    : XoMoneySemantic.income,
              ),
            ),
        ],
      ),
    );
  }
}
