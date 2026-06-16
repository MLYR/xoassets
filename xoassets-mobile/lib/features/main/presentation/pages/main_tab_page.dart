import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/routes.dart';
import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_bottom_sheet.dart';
import '../../../home/presentation/pages/home_page.dart';
import '../../../investment/presentation/pages/investment_page.dart';
import '../../../ledger/presentation/pages/ledger_page.dart';
import '../../../profile/presentation/pages/profile_page.dart';
import '../providers/main_tab_provider.dart';

/// 主 Tab 容器：中间悬浮按钮只打开快捷操作面板。
class MainTabPage extends ConsumerWidget {
  const MainTabPage({super.key});

  static const _pages = [
    HomePage(),
    LedgerPage(),
    InvestmentPage(),
    ProfilePage(),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tabIndex = ref.watch(mainTabProvider);

    return Scaffold(
      backgroundColor: XoColors.pageBg,
      body: IndexedStack(index: tabIndex, children: _pages),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showQuickActions(context),
        backgroundColor: XoColors.primary,
        foregroundColor: Colors.white,
        shape: const CircleBorder(),
        child: const Icon(Icons.add),
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.centerDocked,
      bottomNavigationBar: BottomAppBar(
        color: XoColors.cardBg,
        elevation: 0,
        shape: const CircularNotchedRectangle(),
        notchMargin: 8,
        child: SizedBox(
          height: 64,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _TabButton(index: 0, icon: Icons.home_outlined, label: '首页'),
              _TabButton(
                index: 1,
                icon: Icons.receipt_long_outlined,
                label: '记账',
              ),
              const SizedBox(width: 48),
              _TabButton(
                index: 2,
                icon: Icons.trending_up_outlined,
                label: '投资',
              ),
              _TabButton(index: 3, icon: Icons.person_outline, label: '我的'),
            ],
          ),
        ),
      ),
    );
  }

  void _showQuickActions(BuildContext context) {
    XoBottomSheet.show<void>(
      context,
      title: '快捷操作',
      child: Column(
        children: [
          _QuickActionTile(
            icon: Icons.edit_note,
            label: '记一笔',
            onTap: () {
              context.pop();
              context.push('${AppRoutes.transactionEdit}?type=expense');
            },
          ),
          _QuickActionTile(
            icon: Icons.swap_horiz,
            label: '转账',
            onTap: () {
              context.pop();
              context.push('${AppRoutes.transactionEdit}?type=transfer');
            },
          ),
          _QuickActionTile(
            icon: Icons.show_chart,
            label: '投资交易',
            onTap: () {
              context.pop();
              context.push(AppRoutes.investmentTrade);
            },
          ),
          _QuickActionTile(
            icon: Icons.account_balance_wallet_outlined,
            label: '新增账户',
            onTap: () {
              context.pop();
              ScaffoldMessenger.of(
                context,
              ).showSnackBar(const SnackBar(content: Text('新增账户功能将在下一阶段接入')));
            },
          ),
          _QuickActionTile(
            icon: Icons.savings_outlined,
            label: '新增预算',
            onTap: () {
              context.pop();
              context.push(AppRoutes.budget);
            },
          ),
        ],
      ),
    );
  }
}

class _TabButton extends ConsumerWidget {
  const _TabButton({
    required this.index,
    required this.icon,
    required this.label,
  });

  final int index;
  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selected = ref.watch(mainTabProvider) == index;
    final color = selected ? XoColors.primary : XoColors.textSecondary;

    return InkWell(
      onTap: () => ref.read(mainTabProvider.notifier).setIndex(index),
      borderRadius: BorderRadius.circular(18),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: XoSpacing.sm,
          vertical: XoSpacing.xs,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: color),
            const SizedBox(height: 2),
            Text(label, style: TextStyle(color: color, fontSize: 12)),
          ],
        ),
      ),
    );
  }
}

class _QuickActionTile extends StatelessWidget {
  const _QuickActionTile({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: XoColors.primary),
      title: Text(label),
      trailing: const Icon(Icons.chevron_right),
      onTap: onTap,
    );
  }
}
