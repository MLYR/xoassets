import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_empty.dart';
import '../../../../core/widgets/xo_error_view.dart';
import '../../../../core/widgets/xo_loading.dart';
import '../../../../core/widgets/xo_page.dart';
import '../../../../core/widgets/xo_section_header.dart';
import '../../data/repositories/investment_repository.dart';
import '../providers/investment_provider.dart';
import '../widgets/holding_item.dart';
import '../widgets/investment_overview_card.dart';

/// 投资页接入投资总览、持仓列表和行情刷新接口。
class InvestmentPage extends ConsumerWidget {
  const InvestmentPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final data = ref.watch(investmentDashboardProvider);

    return XoPage(
      child: data.when(
        loading: () => const XoLoading(message: '正在加载投资数据'),
        error: (error, _) => XoErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(investmentDashboardProvider),
        ),
        data: (dashboard) => Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            XoSectionHeader(
              title: '投资',
              action: Wrap(
                spacing: XoSpacing.xs,
                children: [
                  TextButton.icon(
                    onPressed: dashboard.holdings.isEmpty
                        ? null
                        : () =>
                              _refreshQuotes(context, ref, dashboard.holdings),
                    icon: const Icon(Icons.refresh),
                    label: const Text('刷新行情'),
                  ),
                  TextButton.icon(
                    onPressed: () => context.push(AppRoutes.investmentTrade),
                    icon: const Icon(Icons.add_chart),
                    label: const Text('交易'),
                  ),
                ],
              ),
            ),
            const SizedBox(height: XoSpacing.md),
            InvestmentOverviewCard(overview: dashboard.overview),
            const SizedBox(height: XoSpacing.md),
            XoCard(
              child: DefaultTabController(
                length: 3,
                child: Column(
                  children: [
                    const TabBar(
                      tabs: [
                        Tab(text: '基金'),
                        Tab(text: '股票'),
                        Tab(text: '虚拟货币'),
                      ],
                    ),
                    SizedBox(
                      height: 260,
                      child: TabBarView(
                        children: [
                          _HoldingList(items: dashboard.holdingsByType('FUND')),
                          _HoldingList(
                            items: dashboard.holdingsByType('STOCK'),
                          ),
                          _HoldingList(
                            items: dashboard.holdingsByType('CRYPTO'),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _refreshQuotes(
    BuildContext context,
    WidgetRef ref,
    List<HoldingModel> holdings,
  ) async {
    try {
      // 只传 XOAssets 内部资产 ID，第三方行情只能由后端 QuoteProvider 调用。
      await ref
          .read(investmentRepositoryProvider)
          .refreshQuotes(
            holdings
                .map((item) => item.assetId)
                .where((id) => id.isNotEmpty)
                .toSet()
                .toList(),
          );
      ref.invalidate(investmentDashboardProvider);
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('行情刷新已提交')));
      }
    } catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    }
  }
}

class _HoldingList extends StatelessWidget {
  const _HoldingList({required this.items});

  final List<HoldingModel> items;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const XoEmpty(message: '暂无该类型持仓');
    }
    return ListView(
      padding: const EdgeInsets.only(top: XoSpacing.sm),
      children: [
        for (final item in items)
          HoldingItem(
            name: item.symbol == null
                ? item.assetName
                : '${item.assetName} · ${item.symbol}',
            marketValue: item.marketValue,
            profitLabel: item.profitLabel,
            profit: item.profit,
          ),
      ],
    );
  }
}
