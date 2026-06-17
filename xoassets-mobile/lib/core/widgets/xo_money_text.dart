import 'package:flutter/material.dart';

import '../design/xo_colors.dart';
import '../design/xo_text_styles.dart';
import '../utils/money_utils.dart';

enum XoMoneySize { large, medium, small }

enum XoMoneySemantic { normal, income, expense }

/// 金额展示统一组件，支持隐藏金额和收入/支出语义色。
class XoMoneyText extends StatelessWidget {
  const XoMoneyText(
    this.value, {
    super.key,
    this.size = XoMoneySize.medium,
    this.semantic = XoMoneySemantic.normal,
    this.hidden = false,
    this.forceColor,
  });

  final Object value;
  final XoMoneySize size;
  final XoMoneySemantic semantic;
  final bool hidden;
  final Color? forceColor;

  @override
  Widget build(BuildContext context) {
    final baseStyle = switch (size) {
      XoMoneySize.large => XoTextStyles.moneyLarge,
      XoMoneySize.medium => XoTextStyles.moneyMedium,
      XoMoneySize.small => XoTextStyles.body,
    };

    final color =
        forceColor ??
        switch (semantic) {
          XoMoneySemantic.income => XoColors.income,
          XoMoneySemantic.expense => XoColors.expense,
          XoMoneySemantic.normal => Theme.of(context).colorScheme.onSurface,
        };

    return Text(
      hidden ? '¥****' : MoneyUtils.format(value),
      style: baseStyle.copyWith(color: color),
    );
  }
}
