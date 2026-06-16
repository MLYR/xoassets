import 'package:intl/intl.dart';

/// 日期格式化工具，避免页面直接散落 DateFormat。
class XoDateUtils {
  static final DateFormat monthFormatter = DateFormat('yyyy年MM月', 'zh_CN');
  static final DateFormat dayFormatter = DateFormat('MM月dd日', 'zh_CN');
}
