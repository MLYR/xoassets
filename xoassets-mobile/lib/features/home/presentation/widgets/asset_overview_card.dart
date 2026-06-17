import 'package:flutter/material.dart';

import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_money_text.dart';

/// 首页总资产卡，承载最核心的资产数字。
class AssetOverviewCard extends StatelessWidget {
  const AssetOverviewCard({
    required this.totalAsset,
    required this.netAsset,
    super.key,
    this.hidden = false,
  });

  final String totalAsset;
  final String netAsset;
  final bool hidden;

  @override
  Widget build(BuildContext context) {
    return XoCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('总资产', style: TextStyle(color: XoColors.textSecondary)),
          const SizedBox(height: XoSpacing.xs),
          XoMoneyText(totalAsset, size: XoMoneySize.large, hidden: hidden),
          const SizedBox(height: XoSpacing.md),
          Row(
            children: [
              const Text(
                '净资产',
                style: TextStyle(color: XoColors.textSecondary),
              ),
              const SizedBox(width: XoSpacing.sm),
              XoMoneyText(netAsset, size: XoMoneySize.small, hidden: hidden),
            ],
          ),
        ],
      ),
    );
  }
}
