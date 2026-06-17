/// 后端金额与 Long ID 统一从动态 JSON 安全转字符串，避免 App 端精度丢失。
class JsonUtils {
  const JsonUtils._();

  static Map<String, dynamic> asMap(Object? value) {
    return value is Map<String, dynamic> ? value : <String, dynamic>{};
  }

  static List<Map<String, dynamic>> asMapList(Object? value) {
    if (value is! List) {
      return const [];
    }
    return value.whereType<Map<String, dynamic>>().toList(growable: false);
  }

  static String? string(Object? value) {
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  static String money(Object? value) {
    return value?.toString() ?? '--';
  }

  static int integer(Object? value, {int fallback = 0}) {
    if (value is num) {
      return value.toInt();
    }
    return int.tryParse(value?.toString() ?? '') ?? fallback;
  }

  static double ratio(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(value?.toString() ?? '') ?? 0;
  }
}
