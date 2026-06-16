import 'package:flutter/material.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_money_text.dart';
import '../../../../core/widgets/xo_page.dart';

/// 预算页骨架，供快捷操作和我的页入口跳转。
class BudgetPage extends StatelessWidget {
  const BudgetPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const XoPage(
      appBar: _BudgetAppBar(),
      child: XoCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('本月总预算'),
            SizedBox(height: XoSpacing.xs),
            XoMoneyText('6000.00', size: XoMoneySize.large),
            SizedBox(height: XoSpacing.md),
            LinearProgressIndicator(value: 0.62),
            SizedBox(height: XoSpacing.md),
            Text('已使用 62%，下阶段接入真实预算接口。'),
          ],
        ),
      ),
    );
  }
}

class _BudgetAppBar extends StatelessWidget implements PreferredSizeWidget {
  const _BudgetAppBar();

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    return AppBar(title: const Text('预算管理'));
  }
}
