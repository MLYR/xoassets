import 'package:flutter/material.dart';

/// XO Design System 阴影 token，统一使用深青绿透明阴影避免纯黑厚重感。
class XoShadows {
  static const card = BoxShadow(
    color: Color(0x14002F2A),
    blurRadius: 24,
    offset: Offset(0, 10),
  );

  static const button = BoxShadow(
    color: Color(0x33004C43),
    blurRadius: 18,
    offset: Offset(0, 8),
  );

  static const floating = BoxShadow(
    color: Color(0x26002F2A),
    blurRadius: 28,
    offset: Offset(0, 12),
  );
}
