import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_money_text.dart';
import '../../../../core/widgets/xo_page.dart';
import '../../../../core/widgets/xo_section_header.dart';
import '../widgets/ledger_calendar.dart';
import '../widgets/transaction_item.dart';

/// 记账页骨架：日历、当天收支和流水列表先使用 mock 数据。
class LedgerPage extends StatelessWidget {
  const LedgerPage({super.key});

  @override
  Widget build(BuildContext context) {
    return XoPage(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          XoSectionHeader(
            title: '2026年06月',
            action: TextButton.icon(
              onPressed: () =>
                  context.push('${AppRoutes.transactionEdit}?type=expense'),
              icon: const Icon(Icons.add),
              label: const Text('记一笔'),
            ),
          ),
          const SizedBox(height: XoSpacing.md),
          const XoCard(child: LedgerCalendar()),
          const SizedBox(height: XoSpacing.md),
          const XoCard(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _LedgerStat(
                  label: '收入',
                  value: '320.00',
                  semantic: XoMoneySemantic.income,
                ),
                _LedgerStat(
                  label: '支出',
                  value: '128.50',
                  semantic: XoMoneySemantic.expense,
                ),
                _LedgerStat(label: '结余', value: '191.50'),
              ],
            ),
          ),
          const SizedBox(height: XoSpacing.md),
          const XoCard(
            child: Column(
              children: [
                TransactionItem(title: '午餐', amount: '-48.00'),
                TransactionItem(title: '咖啡', amount: '-22.00'),
                TransactionItem(title: '报销', amount: '320.00'),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _LedgerStat extends StatelessWidget {
  const _LedgerStat({
    required this.label,
    required this.value,
    this.semantic = XoMoneySemantic.normal,
  });

  final String label;
  final String value;
  final XoMoneySemantic semantic;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label),
        XoMoneyText(value, size: XoMoneySize.small, semantic: semantic),
      ],
    );
  }
}
