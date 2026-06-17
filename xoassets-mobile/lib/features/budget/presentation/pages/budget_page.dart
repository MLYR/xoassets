import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_empty.dart';
import '../../../../core/widgets/xo_error_view.dart';
import '../../../../core/widgets/xo_loading.dart';
import '../../../../core/widgets/xo_money_text.dart';
import '../../../../core/widgets/xo_page.dart';
import '../providers/budget_provider.dart';

/// 预算页接入 `/budgets/summary`，不在 App 端自行汇总预算。
class BudgetPage extends ConsumerWidget {
  const BudgetPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final summary = ref.watch(budgetSummaryProvider);

    return XoPage(
      appBar: AppBar(title: const Text('预算管理')),
      child: summary.when(
        loading: () => const XoLoading(message: '正在加载预算'),
        error: (error, _) => XoErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(budgetSummaryProvider),
        ),
        data: (data) => Column(
          children: [
            XoCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('本月总预算'),
                  const SizedBox(height: XoSpacing.xs),
                  XoMoneyText(data.totalBudget, size: XoMoneySize.large),
                  const SizedBox(height: XoSpacing.md),
                  LinearProgressIndicator(value: data.usageRate.clamp(0, 1)),
                  const SizedBox(height: XoSpacing.md),
                  Text('已使用 ${data.totalUsed}，剩余 ${data.totalRemaining}'),
                  if (data.usageStatusLabel?.isNotEmpty == true) ...[
                    const SizedBox(height: XoSpacing.xs),
                    Text(data.usageStatusLabel!),
                  ],
                ],
              ),
            ),
            const SizedBox(height: XoSpacing.md),
            XoCard(
              child: data.items.isEmpty
                  ? const XoEmpty(message: '暂无分类预算')
                  : Column(
                      children: [
                        for (final item in data.items)
                          ListTile(
                            contentPadding: EdgeInsets.zero,
                            title: Text(item.categoryName),
                            subtitle: LinearProgressIndicator(
                              value: item.usageRate.clamp(0, 1),
                            ),
                            trailing: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                XoMoneyText(
                                  item.usedAmount,
                                  size: XoMoneySize.small,
                                ),
                                Text('/ ${item.amount}'),
                              ],
                            ),
                          ),
                      ],
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
