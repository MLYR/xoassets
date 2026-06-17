import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/storage/preferences_service.dart';

enum XoThemeModeSetting { system, light, dark }

/// App 设置状态，普通配置使用 SharedPreferences 持久化。
class AppSettingsState {
  const AppSettingsState({
    required this.hideAmount,
    required this.themeModeSetting,
  });

  final bool hideAmount;
  final XoThemeModeSetting themeModeSetting;

  bool get darkMode => themeModeSetting == XoThemeModeSetting.dark;

  ThemeMode get materialThemeMode {
    return switch (themeModeSetting) {
      XoThemeModeSetting.system => ThemeMode.system,
      XoThemeModeSetting.light => ThemeMode.light,
      XoThemeModeSetting.dark => ThemeMode.dark,
    };
  }

  AppSettingsState copyWith({
    bool? hideAmount,
    XoThemeModeSetting? themeModeSetting,
  }) {
    return AppSettingsState(
      hideAmount: hideAmount ?? this.hideAmount,
      themeModeSetting: themeModeSetting ?? this.themeModeSetting,
    );
  }
}

class AppSettingsController extends Notifier<AppSettingsState> {
  @override
  AppSettingsState build() {
    ref.listen(preferencesServiceProvider, (_, next) {
      if (!next.hasValue) {
        return;
      }
      final service = next.requireValue;
      state = AppSettingsState(
        hideAmount: service.hideAmount,
        themeModeSetting: _parseThemeMode(service.themeMode),
      );
    });
    return const AppSettingsState(
      hideAmount: false,
      themeModeSetting: XoThemeModeSetting.system,
    );
  }

  Future<void> setHideAmount(bool value) async {
    state = state.copyWith(hideAmount: value);
    final service = await ref.read(preferencesServiceProvider.future);
    await service.setHideAmount(value);
  }

  Future<void> setDarkMode(bool value) {
    return setThemeMode(
      value ? XoThemeModeSetting.dark : XoThemeModeSetting.light,
    );
  }

  Future<void> setThemeMode(XoThemeModeSetting value) async {
    state = state.copyWith(themeModeSetting: value);
    final service = await ref.read(preferencesServiceProvider.future);
    await service.setThemeMode(value.name);
  }

  XoThemeModeSetting _parseThemeMode(String value) {
    return XoThemeModeSetting.values.firstWhere(
      (item) => item.name == value,
      orElse: () => XoThemeModeSetting.system,
    );
  }
}

final appSettingsProvider =
    NotifierProvider<AppSettingsController, AppSettingsState>(
      AppSettingsController.new,
    );
