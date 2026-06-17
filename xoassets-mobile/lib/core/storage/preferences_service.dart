import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// SharedPreferences 保存非敏感偏好，不用于 Token。
class PreferencesService {
  const PreferencesService(this._preferences);

  static const hideAmountKey = 'hideAmount';
  static const darkModeKey = 'darkMode';
  static const themeModeKey = 'themeMode';
  static const lastSelectedAccountIdKey = 'lastSelectedAccountId';
  static const lastSelectedCategoryIdKey = 'lastSelectedCategoryId';

  final SharedPreferences _preferences;

  bool get hideAmount => _preferences.getBool(hideAmountKey) ?? false;

  Future<void> setHideAmount(bool value) {
    return _preferences.setBool(hideAmountKey, value);
  }

  /// 兼容旧 bool 深色配置；新版本优先读取 themeMode。
  bool get darkMode => _preferences.getBool(darkModeKey) ?? false;

  Future<void> setDarkMode(bool value) {
    return _preferences.setBool(darkModeKey, value);
  }

  String get themeMode {
    return _preferences.getString(themeModeKey) ??
        (darkMode ? 'dark' : 'system');
  }

  Future<void> setThemeMode(String value) async {
    await _preferences.setString(themeModeKey, value);
    await _preferences.setBool(darkModeKey, value == 'dark');
  }

  String? get lastSelectedAccountId {
    return _preferences.getString(lastSelectedAccountIdKey);
  }

  Future<void> setLastSelectedAccountId(String value) {
    return _preferences.setString(lastSelectedAccountIdKey, value);
  }

  String? get lastSelectedCategoryId {
    return _preferences.getString(lastSelectedCategoryIdKey);
  }

  Future<void> setLastSelectedCategoryId(String value) {
    return _preferences.setString(lastSelectedCategoryIdKey, value);
  }
}

/// 普通配置异步加载，避免阻塞 App 启动；初始状态按系统主题展示。
final preferencesServiceProvider = FutureProvider<PreferencesService>((
  ref,
) async {
  final preferences = await SharedPreferences.getInstance();
  return PreferencesService(preferences);
});
