import 'package:intl/intl.dart';

/// 金额格式化统一入口，页面展示金额必须优先走 XoMoneyText。
class MoneyUtils {
  static final NumberFormat _formatter = NumberFormat.currency(
    locale: 'zh_CN',
    symbol: '¥',
    decimalDigits: 2,
  );

  static String format(Object value) {
    final number = value is num ? value : num.tryParse(value.toString()) ?? 0;
    return _formatter.format(number);
  }
}
