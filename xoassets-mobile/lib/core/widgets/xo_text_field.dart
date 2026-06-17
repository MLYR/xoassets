import 'package:flutter/material.dart';

import '../design/xo_colors.dart';
import '../design/xo_radius.dart';

/// XO 表单输入框，统一高度、圆角、线性图标和聚焦态边框。
class XoTextField extends StatelessWidget {
  const XoTextField({
    required this.controller,
    required this.hintText,
    super.key,
    this.icon,
    this.trailing,
    this.enabled = true,
    this.obscureText = false,
    this.keyboardType,
    this.textInputAction,
    this.onSubmitted,
  });

  final TextEditingController controller;
  final String hintText;
  final IconData? icon;
  final Widget? trailing;
  final bool enabled;
  final bool obscureText;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final ValueChanged<String>? onSubmitted;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 56,
      child: TextField(
        controller: controller,
        enabled: enabled,
        obscureText: obscureText,
        keyboardType: keyboardType,
        textInputAction: textInputAction,
        onSubmitted: onSubmitted,
        style: const TextStyle(
          color: XoColors.textMain,
          fontSize: 16,
          fontWeight: FontWeight.w500,
        ),
        decoration: InputDecoration(
          hintText: hintText,
          hintStyle: const TextStyle(color: XoColors.textPlaceholder),
          prefixIcon: icon == null
              ? null
              : Icon(icon, color: XoColors.textSecondary, size: 22),
          suffixIcon: trailing,
          contentPadding: const EdgeInsets.symmetric(horizontal: 16),
          filled: true,
          fillColor: XoColors.inputBg,
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(XoRadius.input),
            borderSide: const BorderSide(color: XoColors.border),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(XoRadius.input),
            borderSide: const BorderSide(color: XoColors.primary, width: 1.4),
          ),
          disabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(XoRadius.input),
            borderSide: const BorderSide(color: XoColors.border),
          ),
        ),
      ),
    );
  }
}
