import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_empty.dart';
import '../../../../core/widgets/xo_error_view.dart';
import '../../../../core/widgets/xo_loading.dart';
import '../../../../core/widgets/xo_money_text.dart';
import '../../../../core/widgets/xo_page.dart';
import '../../../../core/widgets/xo_section_header.dart';
import '../providers/ledger_provider.dart';
import '../widgets/ledger_calendar.dart';
import '../widgets/transaction_item.dart';

/// 记账页接入流水分页接口，日历保留轻量月份视图。
class LedgerPage extends ConsumerWidget {
  const LedgerPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final page = ref.watch(ledgerTransactionsProvider);

    return XoPage(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          XoSectionHeader(
            title: '本月记账',
            action: TextButton.icon(
              onPressed: () =>
                  context.push('${AppRoutes.transactionEdit}?type=EXPENSE'),
              icon: const Icon(Icons.add),
              label: const Text('记一笔'),
            ),
          ),
          const SizedBox(height: XoSpacing.md),
          const XoCard(child: LedgerCalendar()),
          const SizedBox(height: XoSpacing.md),
          page.when(
            loading: () => const XoLoading(message: '正在加载流水'),
            error: (error, _) => XoErrorView(
              message: error.toString(),
              onRetry: () => ref.invalidate(ledgerTransactionsProvider),
            ),
            data: (data) {
              final income = data.records
                  .where((item) => item.type == 'INCOME')
                  .fold<double>(
                    0,
                    (sum, item) => sum + (double.tryParse(item.amount) ?? 0),
                  );
              final expense = data.records
                  .where((item) => item.type == 'EXPENSE')
                  .fold<double>(
                    0,
                    (sum, item) => sum + (double.tryParse(item.amount) ?? 0),
                  );
              return Column(
                children: [
                  XoCard(
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        _LedgerStat(
                          label: '收入',
                          value: income.toStringAsFixed(2),
                          semantic: XoMoneySemantic.income,
                        ),
                        _LedgerStat(
                          label: '支出',
                          value: expense.toStringAsFixed(2),
                          semantic: XoMoneySemantic.expense,
                        ),
                        _LedgerStat(
                          label: '结余',
                          value: (income - expense).toStringAsFixed(2),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: XoSpacing.md),
                  XoCard(
                    child: data.records.isEmpty
                        ? const XoEmpty(message: '暂无流水，点右上角记一笔')
                        : Column(
                            children: [
                              for (final item in data.records)
                                TransactionItem(
                                  title: item.title,
                                  amount: item.signedAmount,
                                  subtitle: item.transactionTime,
                                ),
                            ],
                          ),
                  ),
                ],
              );
            },
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
