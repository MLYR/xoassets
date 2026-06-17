import 'package:flutter/material.dart';

/// XO Design System 渐变 token，登录注册页和主按钮统一从这里取色。
class XoGradients {
  static const primaryButton = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFF008071), Color(0xFF004C43)],
  );

  static const authBackground = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [Color(0xFF002F2A), Color(0xFF004C43)],
  );

  static const lightCard = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFFFFFFFF), Color(0xFFF4FAF8)],
  );
}
