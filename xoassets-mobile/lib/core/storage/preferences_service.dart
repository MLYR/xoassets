import 'package:shared_preferences/shared_preferences.dart';

/// SharedPreferences 保存非敏感偏好，不用于 Token。
class PreferencesService {
  const PreferencesService(this._preferences);

  static const hideAmountKey = 'hideAmount';
  static const darkModeKey = 'darkMode';
  static const lastSelectedAccountIdKey = 'lastSelectedAccountId';
  static const lastSelectedCategoryIdKey = 'lastSelectedCategoryId';

  final SharedPreferences _preferences;

  bool get hideAmount => _preferences.getBool(hideAmountKey) ?? false;

  Future<void> setHideAmount(bool value) {
    return _preferences.setBool(hideAmountKey, value);
  }

  bool get darkMode => _preferences.getBool(darkModeKey) ?? false;

  Future<void> setDarkMode(bool value) {
    return _preferences.setBool(darkModeKey, value);
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
