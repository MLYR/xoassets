import 'package:flutter/material.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_page.dart';

/// 投资交易页骨架，本阶段不保存真实交易。
class InvestmentTradePage extends StatefulWidget {
  const InvestmentTradePage({super.key});

  @override
  State<InvestmentTradePage> createState() => _InvestmentTradePageState();
}

class _InvestmentTradePageState extends State<InvestmentTradePage> {
  String _type = 'buy';

  @override
  Widget build(BuildContext context) {
    return XoPage(
      appBar: AppBar(title: const Text('投资交易')),
      child: XoCard(
        child: Column(
          children: [
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'buy', label: Text('买入')),
                ButtonSegment(value: 'sell', label: Text('卖出')),
                ButtonSegment(value: 'dividend', label: Text('分红')),
              ],
              selected: {_type},
              onSelectionChanged: (value) =>
                  setState(() => _type = value.first),
            ),
            const SizedBox(height: XoSpacing.md),
            const TextField(decoration: InputDecoration(labelText: '资产选择')),
            const SizedBox(height: XoSpacing.md),
            const TextField(decoration: InputDecoration(labelText: '成交价格')),
            const SizedBox(height: XoSpacing.md),
            const TextField(decoration: InputDecoration(labelText: '数量 / 份额')),
            const SizedBox(height: XoSpacing.md),
            const TextField(decoration: InputDecoration(labelText: '手续费')),
            const SizedBox(height: XoSpacing.md),
            const TextField(decoration: InputDecoration(labelText: '交易日期')),
            const SizedBox(height: XoSpacing.lg),
            SizedBox(
              width: double.infinity,
              child: FilledButton(onPressed: () {}, child: const Text('保存')),
            ),
          ],
        ),
      ),
    );
  }
}
