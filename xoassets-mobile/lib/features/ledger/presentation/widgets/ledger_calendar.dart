import 'package:flutter/material.dart';

import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_radius.dart';
import '../../../../core/design/xo_spacing.dart';

/// 记账日历占位，后续替换为真实月份流水日历。
class LedgerCalendar extends StatelessWidget {
  const LedgerCalendar({super.key});

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      itemCount: 7,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 7,
        mainAxisSpacing: XoSpacing.xs,
        crossAxisSpacing: XoSpacing.xs,
      ),
      itemBuilder: (context, index) {
        final selected = index == 2;
        return Container(
          decoration: BoxDecoration(
            color: selected ? XoColors.primaryLight : XoColors.pageBg,
            borderRadius: BorderRadius.circular(XoRadius.sm),
          ),
          alignment: Alignment.center,
          child: Text('${index + 10}'),
        );
      },
    );
  }
}
