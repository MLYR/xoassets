import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_page.dart';
import '../../../../core/widgets/xo_section_header.dart';
import '../widgets/holding_item.dart';
import '../widgets/investment_overview_card.dart';

/// 投资页骨架，收益口径按资产类别明确区分。
class InvestmentPage extends StatelessWidget {
  const InvestmentPage({super.key});

  @override
  Widget build(BuildContext context) {
    return XoPage(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          XoSectionHeader(
            title: '投资',
            action: TextButton.icon(
              onPressed: () => context.push(AppRoutes.investmentTrade),
              icon: const Icon(Icons.add_chart),
              label: const Text('交易'),
            ),
          ),
          const SizedBox(height: XoSpacing.md),
          const InvestmentOverviewCard(),
          const SizedBox(height: XoSpacing.md),
          const XoCard(
            child: DefaultTabController(
              length: 3,
              child: Column(
                children: [
                  TabBar(
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
                        _HoldingList(
                          items: [
                            HoldingItem(
                              name: '易方达蓝筹精选',
                              marketValue: '26800.00',
                              profitLabel: '昨日收益',
                              profit: '126.30',
                            ),
                          ],
                        ),
                        _HoldingList(
                          items: [
                            HoldingItem(
                              name: '贵州茅台',
                              marketValue: '18500.00',
                              profitLabel: '今日收益',
                              profit: '-58.20',
                            ),
                          ],
                        ),
                        _HoldingList(
                          items: [
                            HoldingItem(
                              name: 'BTC',
                              marketValue: '11520.00',
                              profitLabel: '24h 收益',
                              profit: '88.12',
                            ),
                          ],
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
    );
  }
}

class _HoldingList extends StatelessWidget {
  const _HoldingList({required this.items});

  final List<Widget> items;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.only(top: XoSpacing.sm),
      children: items,
    );
  }
}
