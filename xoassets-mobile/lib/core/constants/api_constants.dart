/// API 常量集中在网络层读取；App 绝不直接连接 MySQL。
class ApiConstants {
  // Android 模拟器访问宿主机后端时可用 --dart-define 覆盖为 10.0.2.2。
  static const baseUrl = String.fromEnvironment(
    'XO_API_BASE_URL',
    defaultValue: 'http://localhost:8080/api',
  );
  static const connectTimeout = Duration(seconds: 10);
  static const receiveTimeout = Duration(seconds: 15);
}
