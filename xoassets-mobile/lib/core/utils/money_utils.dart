import 'package:intl/intl.dart';

/// 金额格式化统一入口，后端 null / 缺失值展示为 --，不能用 0 冒充。
class MoneyUtils {
  static final NumberFormat _formatter = NumberFormat.currency(
    locale: 'zh_CN',
    symbol: '¥',
    decimalDigits: 2,
  );

  static String format(Object? value) {
    if (value == null || value.toString().isEmpty || value.toString() == '--') {
      return '--';
    }
    final number = value is num ? value : num.tryParse(value.toString());
    if (number == null) {
      return '--';
    }
    return _formatter.format(number);
  }
}
