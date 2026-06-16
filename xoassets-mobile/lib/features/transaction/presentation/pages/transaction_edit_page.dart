import 'package:flutter/material.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_page.dart';

/// 流水录入页骨架，本阶段不保存真实数据。
class TransactionEditPage extends StatefulWidget {
  const TransactionEditPage({required this.initialType, super.key});

  final String initialType;

  @override
  State<TransactionEditPage> createState() => _TransactionEditPageState();
}

class _TransactionEditPageState extends State<TransactionEditPage> {
  late String _type = widget.initialType;

  @override
  Widget build(BuildContext context) {
    return XoPage(
      appBar: AppBar(title: const Text('流水录入')),
      child: XoCard(
        child: Column(
          children: [
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'expense', label: Text('支出')),
                ButtonSegment(value: 'income', label: Text('收入')),
                ButtonSegment(value: 'transfer', label: Text('转账')),
              ],
              selected: {_type},
              onSelectionChanged: (value) =>
                  setState(() => _type = value.first),
            ),
            const SizedBox(height: XoSpacing.md),
            const TextField(
              keyboardType: TextInputType.number,
              decoration: InputDecoration(labelText: '金额'),
            ),
            const SizedBox(height: XoSpacing.md),
            const TextField(decoration: InputDecoration(labelText: '分类选择')),
            const SizedBox(height: XoSpacing.md),
            const TextField(decoration: InputDecoration(labelText: '账户选择')),
            const SizedBox(height: XoSpacing.md),
            const TextField(decoration: InputDecoration(labelText: '时间选择')),
            const SizedBox(height: XoSpacing.md),
            const TextField(decoration: InputDecoration(labelText: '备注')),
            const SizedBox(height: XoSpacing.md),
            OutlinedButton.icon(
              onPressed: () {},
              icon: const Icon(Icons.image_outlined),
              label: const Text('图片附件占位'),
            ),
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
