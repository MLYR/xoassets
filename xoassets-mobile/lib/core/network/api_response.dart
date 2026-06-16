/// 后端通用响应包装；第一阶段只定义结构，不绑定具体业务接口。
class ApiResponse<T> {
  const ApiResponse({required this.code, required this.message, this.data});

  final int code;
  final String message;
  final T? data;

  factory ApiResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Object? value) mapper,
  ) {
    return ApiResponse<T>(
      code: (json['code'] as num?)?.toInt() ?? 0,
      message: json['message']?.toString() ?? '',
      data: mapper(json['data']),
    );
  }
}
