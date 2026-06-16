import 'package:flutter_riverpod/flutter_riverpod.dart';

/// App 设置状态，第一阶段以内存状态驱动 UI，持久化服务已预留。
class AppSettingsState {
  const AppSettingsState({required this.hideAmount, required this.darkMode});

  final bool hideAmount;
  final bool darkMode;

  AppSettingsState copyWith({bool? hideAmount, bool? darkMode}) {
    return AppSettingsState(
      hideAmount: hideAmount ?? this.hideAmount,
      darkMode: darkMode ?? this.darkMode,
    );
  }
}

class AppSettingsController extends Notifier<AppSettingsState> {
  @override
  AppSettingsState build() {
    return const AppSettingsState(hideAmount: false, darkMode: false);
  }

  void setHideAmount(bool value) {
    state = state.copyWith(hideAmount: value);
  }

  void setDarkMode(bool value) {
    state = state.copyWith(darkMode: value);
  }
}

final appSettingsProvider =
    NotifierProvider<AppSettingsController, AppSettingsState>(
      AppSettingsController.new,
    );
