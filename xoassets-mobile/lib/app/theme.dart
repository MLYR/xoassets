import 'package:flutter/material.dart';

import '../core/design/xo_colors.dart';
import '../core/design/xo_radius.dart';
import '../core/design/xo_text_styles.dart';

/// Material 3 主题入口，当前落地 light theme，dark theme 先给出稳定预留。
class AppTheme {
  static ThemeData get lightTheme {
    final scheme = ColorScheme.fromSeed(
      seedColor: XoColors.primary,
      brightness: Brightness.light,
    );

    return ThemeData(
      useMaterial3: true,
      fontFamily: 'PingFang SC',
      fontFamilyFallback: const [
        'Hiragino Sans GB',
        'Microsoft YaHei',
        'Noto Sans SC',
        'Arial',
      ],
      colorScheme: scheme.copyWith(
        primary: XoColors.primary,
        surface: XoColors.cardBg,
        surfaceContainerHighest: XoColors.pageBg,
      ),
      scaffoldBackgroundColor: XoColors.pageBg,
      textTheme: const TextTheme(
        titleLarge: XoTextStyles.titleLarge,
        titleMedium: XoTextStyles.titleMedium,
        bodyMedium: XoTextStyles.body,
        labelSmall: XoTextStyles.caption,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: XoColors.pageBg,
        foregroundColor: XoColors.textMain,
        centerTitle: false,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      cardTheme: CardThemeData(
        color: XoColors.cardBg,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(XoRadius.card),
        ),
      ),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: XoColors.cardBg,
        selectedItemColor: XoColors.primary,
        unselectedItemColor: XoColors.textSecondary,
        type: BottomNavigationBarType.fixed,
        elevation: 0,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        labelStyle: const TextStyle(color: XoColors.textSecondary),
        floatingLabelStyle: const TextStyle(
          color: XoColors.primary,
          fontWeight: FontWeight.w600,
        ),
        hintStyle: const TextStyle(color: XoColors.textPlaceholder),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 14,
          vertical: 14,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(XoRadius.md),
          borderSide: const BorderSide(color: XoColors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(XoRadius.md),
          borderSide: const BorderSide(color: Color(0xFFCBD5E1)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(XoRadius.md),
          borderSide: const BorderSide(color: XoColors.primary, width: 1.4),
        ),
        disabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(XoRadius.md),
          borderSide: const BorderSide(color: XoColors.border),
        ),
      ),
    );
  }

  static ThemeData get darkTheme {
    final scheme = ColorScheme.fromSeed(
      seedColor: XoColors.primary,
      brightness: Brightness.dark,
    );

    return ThemeData(
      useMaterial3: true,
      fontFamily: 'PingFang SC',
      fontFamilyFallback: const [
        'Hiragino Sans GB',
        'Microsoft YaHei',
        'Noto Sans SC',
        'Arial',
      ],
      colorScheme: scheme,
      scaffoldBackgroundColor: const Color(0xFF111827),
    );
  }
}
