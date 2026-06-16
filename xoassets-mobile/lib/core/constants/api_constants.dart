/// API 常量集中在网络层读取；App 绝不直接连接 MySQL。
class ApiConstants {
  static const baseUrl = 'http://localhost:8080/api';
  static const connectTimeout = Duration(seconds: 10);
  static const receiveTimeout = Duration(seconds: 15);
}
