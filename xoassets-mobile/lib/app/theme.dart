import 'package:flutter/material.dart';

import '../core/design/xo_colors.dart';
import '../core/design/xo_radius.dart';
import '../core/design/xo_text_styles.dart';

/// Material 3 主题入口，支持浅色、深色和系统主题跟随。
class AppTheme {
  static const _fontFamilyFallback = [
    'Hiragino Sans GB',
    'Microsoft YaHei',
    'Noto Sans SC',
    'Arial',
  ];

  static ThemeData get lightTheme {
    final scheme = ColorScheme.fromSeed(
      seedColor: XoColors.primary,
      brightness: Brightness.light,
    );

    return _baseTheme(scheme).copyWith(
      scaffoldBackgroundColor: XoColors.pageBg,
      appBarTheme: const AppBarTheme(
        backgroundColor: XoColors.pageBg,
        foregroundColor: XoColors.textMain,
        centerTitle: false,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      cardTheme: _cardTheme(XoColors.cardBg),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: XoColors.cardBg,
        selectedItemColor: XoColors.primary,
        unselectedItemColor: XoColors.textSecondary,
        type: BottomNavigationBarType.fixed,
        elevation: 0,
      ),
      inputDecorationTheme: _inputDecorationTheme(Colors.white),
    );
  }

  static ThemeData get darkTheme {
    final scheme =
        ColorScheme.fromSeed(
          seedColor: XoColors.primary,
          brightness: Brightness.dark,
        ).copyWith(
          surface: const Color(0xFF12322D),
          onSurface: const Color(0xFFE7F3EF),
          primary: const Color(0xFF2DD4BF),
        );

    return _baseTheme(scheme).copyWith(
      scaffoldBackgroundColor: const Color(0xFF071C19),
      dividerColor: const Color(0xFF31554F),
      appBarTheme: const AppBarTheme(
        backgroundColor: Color(0xFF071C19),
        foregroundColor: Color(0xFFE7F3EF),
        centerTitle: false,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      cardTheme: _cardTheme(const Color(0xFF12322D)),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: Color(0xFF12322D),
        selectedItemColor: Color(0xFF2DD4BF),
        unselectedItemColor: Color(0xFF94A3B8),
        type: BottomNavigationBarType.fixed,
        elevation: 0,
      ),
      inputDecorationTheme: _inputDecorationTheme(const Color(0xFF0F2925)),
    );
  }

  static ThemeData _baseTheme(ColorScheme scheme) {
    return ThemeData(
      useMaterial3: true,
      fontFamily: 'PingFang SC',
      fontFamilyFallback: _fontFamilyFallback,
      colorScheme: scheme,
      textTheme: const TextTheme(
        titleLarge: XoTextStyles.titleLarge,
        titleMedium: XoTextStyles.titleMedium,
        bodyMedium: XoTextStyles.body,
        labelSmall: XoTextStyles.caption,
      ).apply(bodyColor: scheme.onSurface, displayColor: scheme.onSurface),
    );
  }

  static CardThemeData _cardTheme(Color color) {
    return CardThemeData(
      color: color,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(XoRadius.card),
      ),
    );
  }

  static InputDecorationTheme _inputDecorationTheme(Color fillColor) {
    return InputDecorationTheme(
      filled: true,
      fillColor: fillColor,
      labelStyle: const TextStyle(color: XoColors.textSecondary),
      floatingLabelStyle: const TextStyle(
        color: XoColors.primary,
        fontWeight: FontWeight.w600,
      ),
      hintStyle: const TextStyle(color: XoColors.textPlaceholder),
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
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
    );
  }
}
