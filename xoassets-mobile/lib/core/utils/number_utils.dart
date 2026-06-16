import 'package:intl/intl.dart';

/// 普通数字格式化工具，收益率和计数类展示复用这里。
class NumberUtils {
  static final NumberFormat percentFormatter = NumberFormat(
    '+#,##0.00%;-#,##0.00%',
  );
}
