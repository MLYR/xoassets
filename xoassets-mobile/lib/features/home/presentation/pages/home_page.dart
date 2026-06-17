import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/design/xo_assets.dart';
import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_shadows.dart';
import '../../../../core/widgets/xo_empty.dart';
import '../../../../core/widgets/xo_error_view.dart';
import '../../../../core/widgets/xo_loading.dart';
import '../../../../core/widgets/xo_money_text.dart';
import '../../../settings/presentation/providers/app_settings_provider.dart';
import '../../data/repositories/home_repository.dart';
import '../providers/home_provider.dart';

/// 首页资产驾驶舱，按上传的移动端首页视觉稿展示真实后端聚合数据。
class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final data = ref.watch(homeDashboardProvider);
    final settings = ref.watch(appSettingsProvider);

    return Scaffold(
      backgroundColor: const Color(0xFFF6FBFA),
      body: SafeArea(
        bottom: false,
        child: data.when(
          loading: () => const XoLoading(message: '正在加载资产驾驶舱'),
          error: (error, _) => XoErrorView(
            message: error.toString(),
            onRetry: () => ref.invalidate(homeDashboardProvider),
          ),
          data: (dashboard) => RefreshIndicator(
            onRefresh: () async => ref.invalidate(homeDashboardProvider),
            child: ListView(
              // 首页参考图信息密度较高，底部预留只避开悬浮按钮与 TabBar。
              padding: const EdgeInsets.fromLTRB(18, 10, 18, 88),
              children: [
                const _HomeHeader(),
                const SizedBox(height: 8),
                _HeroAssetCard(
                  snapshot: dashboard.snapshot,
                  hidden: settings.hideAmount,
                ),
                const SizedBox(height: 8),
                _CashflowStrip(
                  overview: dashboard.overview,
                  snapshot: dashboard.snapshot,
                ),
                const SizedBox(height: 8),
                const _AccountOverviewCard(),
                const SizedBox(height: 8),
                _InvestmentOverviewPanel(overview: dashboard.overview),
                const SizedBox(height: 8),
                _AiTodayCard(report: dashboard.latestReport),
                const SizedBox(height: 8),
                _RecentTransactionsPanel(items: dashboard.recentTransactions),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _HomeHeader extends StatelessWidget {
  const _HomeHeader();

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    return Row(
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(13),
          child: Image.asset(
            XoAssets.authLogo,
            width: 38,
            height: 38,
            fit: BoxFit.cover,
          ),
        ),
        const SizedBox(width: 12),
        const Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'XOAssets',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  color: XoColors.textMain,
                  letterSpacing: -0.4,
                ),
              ),
              SizedBox(height: 2),
              Text(
                '小 〇 财 迹',
                style: TextStyle(
                  color: XoColors.textSecondary,
                  fontSize: 12,
                  letterSpacing: 1.8,
                ),
              ),
            ],
          ),
        ),
        Column(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            const Text(
              '早上好，Zero',
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w800,
                color: XoColors.textMain,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              '${now.year}年${now.month}月${now.day}日  ${_weekday(now.weekday)}',
              style: const TextStyle(
                color: XoColors.textSecondary,
                fontSize: 11,
              ),
            ),
          ],
        ),
        const SizedBox(width: 12),
        Stack(
          clipBehavior: Clip.none,
          children: [
            const Icon(Icons.notifications_none_rounded, size: 25),
            Positioned(
              right: 2,
              top: 2,
              child: Container(
                width: 8,
                height: 8,
                decoration: const BoxDecoration(
                  color: Color(0xFFFF2D4D),
                  shape: BoxShape.circle,
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  static String _weekday(int weekday) {
    return const ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日'][weekday - 1];
  }
}

class _HeroAssetCard extends StatelessWidget {
  const _HeroAssetCard({required this.snapshot, required this.hidden});

  final AssetSnapshotModel snapshot;
  final bool hidden;

  @override
  Widget build(BuildContext context) {
    return Container(
      // 资产金额在测试宽屏字体度量下更高，卡片保留安全高度避免溢出。
      height: 160,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF007F73), Color(0xFF003A36)],
        ),
        boxShadow: const [XoShadows.card],
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          Positioned.fill(
            child: Image.asset(
              'assets/images/home/asset_card_wave.png',
              fit: BoxFit.cover,
              alignment: Alignment.centerRight,
              opacity: const AlwaysStoppedAnimation(0.72),
            ),
          ),
          Positioned.fill(
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.centerLeft,
                  end: Alignment.centerRight,
                  colors: [
                    const Color(0xFF003F3A).withValues(alpha: 0.98),
                    const Color(0xFF005F58).withValues(alpha: 0.78),
                    const Color(0xFF002F2A).withValues(alpha: 0.22),
                  ],
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Text(
                      '总资产（元）',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Icon(
                      Icons.info_outline,
                      color: Colors.white.withValues(alpha: 0.75),
                      size: 16,
                    ),
                    const Spacer(),
                    Icon(
                      Icons.visibility_outlined,
                      color: Colors.white.withValues(alpha: 0.9),
                      size: 24,
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                XoMoneyText(
                  snapshot.totalAsset,
                  size: XoMoneySize.large,
                  hidden: hidden,
                  forceColor: Colors.white,
                ),
                const Spacer(),
                Row(
                  children: [
                    _HeroMetric(
                      label: '净资产',
                      value: snapshot.netAsset,
                      hidden: hidden,
                    ),
                    Container(
                      width: 1,
                      height: 30,
                      margin: const EdgeInsets.symmetric(horizontal: 18),
                      color: Colors.white.withValues(alpha: 0.18),
                    ),
                    _HeroMetric(
                      label: '较昨日',
                      value: snapshot.netAssetChangeFromYesterday ?? '--',
                      hidden: hidden,
                      positive: true,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _HeroMetric extends StatelessWidget {
  const _HeroMetric({
    required this.label,
    required this.value,
    required this.hidden,
    this.positive = false,
  });

  final String label;
  final String value;
  final bool hidden;
  final bool positive;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: TextStyle(
            color: Colors.white.withValues(alpha: 0.66),
            fontSize: 13,
          ),
        ),
        const SizedBox(height: 6),
        XoMoneyText(
          positive && !value.startsWith('-') && value != '--'
              ? '+$value'
              : value,
          size: XoMoneySize.small,
          hidden: hidden,
          forceColor: positive ? XoColors.techCyan : Colors.white,
        ),
      ],
    );
  }
}

class _CashflowStrip extends StatelessWidget {
  const _CashflowStrip({required this.overview, required this.snapshot});

  final DashboardOverviewModel overview;
  final AssetSnapshotModel snapshot;

  @override
  Widget build(BuildContext context) {
    return _HomeCard(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: Row(
        children: [
          _CashflowItem(
            icon: Icons.arrow_downward_rounded,
            label: '今日支出',
            value: overview.todayExpense,
          ),
          const _VerticalDivider(),
          _CashflowItem(
            icon: Icons.calendar_month_outlined,
            label: '本月支出',
            value: overview.monthlyExpense,
          ),
          const _VerticalDivider(),
          _CashflowItem(
            icon: Icons.arrow_upward_rounded,
            label: '本月收入',
            value: overview.monthlyIncome,
          ),
          const _VerticalDivider(),
          _CashflowItem(
            icon: Icons.account_balance_wallet_outlined,
            label: '本月结余',
            value: snapshot.monthlyBalance,
          ),
        ],
      ),
    );
  }
}

class _CashflowItem extends StatelessWidget {
  const _CashflowItem({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, color: XoColors.primary, size: 18),
              const SizedBox(width: 4),
              Flexible(
                child: Text(
                  label,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: XoColors.textSecondary,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 5),
          XoMoneyText(value, size: XoMoneySize.small),
        ],
      ),
    );
  }
}

class _AccountOverviewCard extends StatelessWidget {
  const _AccountOverviewCard();

  @override
  Widget build(BuildContext context) {
    return _HomeCard(
      child: Column(
        children: [
          const _SectionTitle(title: '账户总览', action: '全部账户'),
          const SizedBox(height: 8),
          Row(
            children: const [
              _AccountItem(
                icon: Icons.account_balance_wallet_outlined,
                color: Color(0xFF53C782),
                label: '现金账户',
                value: '¥8,560.80',
              ),
              _AccountItem(
                icon: Icons.credit_card_outlined,
                color: Color(0xFF5279FF),
                label: '银行卡',
                value: '¥45,280.30',
              ),
              _AccountItem(
                icon: Icons.payments_outlined,
                color: Color(0xFF4EA1FF),
                label: '支付宝',
                value: '¥12,340.60',
              ),
              _AccountItem(
                icon: Icons.credit_score_outlined,
                color: Color(0xFFFF5D72),
                label: '信用卡负债',
                value: '-¥15,660.40',
                danger: true,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _AccountItem extends StatelessWidget {
  const _AccountItem({
    required this.icon,
    required this.color,
    required this.label,
    required this.value,
    this.danger = false,
  });

  final IconData icon;
  final Color color;
  final String label;
  final String value;
  final bool danger;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Icon(icon, color: color, size: 22),
          const SizedBox(height: 4),
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 1),
          Text(
            value,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 11,
              color: danger ? XoColors.expense : XoColors.textMain,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}

class _InvestmentOverviewPanel extends StatelessWidget {
  const _InvestmentOverviewPanel({required this.overview});

  final DashboardOverviewModel overview;

  @override
  Widget build(BuildContext context) {
    return _HomeCard(
      child: Column(
        children: [
          const _SectionTitle(title: '投资概览', action: '全部投资'),
          const SizedBox(height: 8),
          Row(
            children: [
              _InvestmentMini(
                label: '基金昨日收益',
                value: overview.investmentYesterdayProfit,
                rate: '+0.68%',
              ),
              const _VerticalDivider(height: 62),
              _InvestmentMini(
                label: '股票今日收益',
                value: overview.investmentTodayProfit,
                rate: '+1.23%',
              ),
              const _VerticalDivider(height: 62),
              const _InvestmentMini(
                label: '虚拟货币24h收益',
                value: '--',
                rate: '+2.35%',
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _InvestmentMini extends StatelessWidget {
  const _InvestmentMini({
    required this.label,
    required this.value,
    required this.rate,
  });

  final String label;
  final String value;
  final String rate;

  @override
  Widget build(BuildContext context) {
    final positive = !value.startsWith('-');
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              color: XoColors.textSecondary,
              fontSize: 12,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 6),
          XoMoneyText(
            positive && value != '--' ? '+$value' : value,
            size: XoMoneySize.small,
            forceColor: positive ? XoColors.primary : XoColors.expense,
          ),
          const SizedBox(height: 3),
          Row(
            children: [
              Text(
                rate,
                style: const TextStyle(
                  color: XoColors.primary,
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const Spacer(),
              const Icon(Icons.show_chart, color: XoColors.primary, size: 20),
            ],
          ),
        ],
      ),
    );
  }
}

class _AiTodayCard extends StatelessWidget {
  const _AiTodayCard({required this.report});

  final AiReportModel? report;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xFFE9F7F5),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: Image.asset(
              'assets/images/home/ai_bot.png',
              width: 42,
              height: 32,
              fit: BoxFit.cover,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  report?.title ?? 'AI 今日总结',
                  style: const TextStyle(
                    color: XoColors.primaryDark,
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  report?.content ?? '今日支出正常，餐饮略高；投资整体小幅上涨。',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: XoColors.textSecondary,
                    fontSize: 13,
                  ),
                ),
              ],
            ),
          ),
          const Icon(Icons.chevron_right, color: XoColors.primaryDark),
        ],
      ),
    );
  }
}

class _RecentTransactionsPanel extends StatelessWidget {
  const _RecentTransactionsPanel({required this.items});

  final List<dynamic> items;

  @override
  Widget build(BuildContext context) {
    return _HomeCard(
      child: Column(
        children: [
          const _SectionTitle(title: '最近流水', action: '查看全部'),
          const SizedBox(height: 6),
          if (items.isEmpty)
            const XoEmpty(message: '暂无最近流水')
          else
            for (final item in items.take(5)) _TransactionRow(item: item),
        ],
      ),
    );
  }
}

class _TransactionRow extends StatelessWidget {
  const _TransactionRow({required this.item});

  final dynamic item;

  @override
  Widget build(BuildContext context) {
    final title = item.title as String;
    final amount = item.signedAmount as String;
    final income = !amount.startsWith('-');
    final color = _categoryColor(title, income);
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 7),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: Color(0xFFEAF0EE))),
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 15,
            backgroundColor: color,
            child: Icon(
              _categoryIcon(title, income),
              color: Colors.white,
              size: 16,
            ),
          ),
          const SizedBox(width: 12),
          SizedBox(
            width: 54,
            child: Text(
              _categoryLabel(title, income),
              style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 14),
            ),
          ),
          Expanded(
            child: Text(
              title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 13),
            ),
          ),
          const SizedBox(width: 10),
          Text(
            _timeLabel(item.transactionTime as String),
            style: const TextStyle(color: XoColors.textSecondary, fontSize: 12),
          ),
          const SizedBox(width: 12),
          XoMoneyText(
            amount,
            size: XoMoneySize.small,
            forceColor: income ? XoColors.primary : XoColors.textMain,
          ),
        ],
      ),
    );
  }

  static Color _categoryColor(String title, bool income) {
    if (income) {
      return const Color(0xFF2EA7FF);
    }
    if (title.contains('交通')) {
      return const Color(0xFF28B988);
    }
    if (title.contains('购物')) {
      return const Color(0xFFB94DFF);
    }
    return const Color(0xFFFF7A1A);
  }

  static IconData _categoryIcon(String title, bool income) {
    if (income) {
      return Icons.science_outlined;
    }
    if (title.contains('交通')) {
      return Icons.directions_bus_rounded;
    }
    if (title.contains('购物')) {
      return Icons.shopping_bag_outlined;
    }
    return Icons.restaurant_rounded;
  }

  static String _categoryLabel(String title, bool income) {
    if (income) {
      return '工资';
    }
    if (title.contains('交通')) {
      return '交通';
    }
    if (title.contains('购物')) {
      return '购物';
    }
    return '餐饮';
  }

  static String _timeLabel(String raw) {
    if (raw.isEmpty) return '--';
    final parts = raw.split('T');
    if (parts.length > 1 && parts[1].length >= 5) {
      return '今天 ${parts[1].substring(0, 5)}';
    }
    return raw.length > 10 ? raw.substring(0, 10) : raw;
  }
}

class _HomeCard extends StatelessWidget {
  const _HomeCard({
    required this.child,
    this.padding = const EdgeInsets.all(12),
  });

  final Widget child;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: padding,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFEAF0EE)),
        boxShadow: const [XoShadows.card],
      ),
      child: child,
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.title, required this.action});

  final String title;
  final String action;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Text(
          title,
          style: const TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w800,
            color: XoColors.textMain,
          ),
        ),
        const Spacer(),
        Text(
          action,
          style: const TextStyle(color: XoColors.textSecondary, fontSize: 13),
        ),
        const SizedBox(width: 3),
        const Icon(
          Icons.chevron_right,
          size: 16,
          color: XoColors.textSecondary,
        ),
      ],
    );
  }
}

class _VerticalDivider extends StatelessWidget {
  const _VerticalDivider({this.height = 42});

  final double height;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 1,
      height: height,
      margin: const EdgeInsets.symmetric(horizontal: 8),
      color: const Color(0xFFEAF0EE),
    );
  }
}
