import 'package:flutter/material.dart';

import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_money_text.dart';

/// 首页小型统计卡，用于收支摘要。
class QuickStatCard extends StatelessWidget {
  const QuickStatCard({
    required this.label,
    required this.value,
    super.key,
    this.semantic = XoMoneySemantic.normal,
  });

  final String label;
  final String value;
  final XoMoneySemantic semantic;

  @override
  Widget build(BuildContext context) {
    return XoCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(color: XoColors.textSecondary)),
          const SizedBox(height: XoSpacing.xs),
          XoMoneyText(value, size: XoMoneySize.small, semantic: semantic),
        ],
      ),
    );
  }
}
