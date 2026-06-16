import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 底部导航当前索引：0 首页、1 记账、2 投资、3 我的。
class MainTabController extends Notifier<int> {
  @override
  int build() => 0;

  void setIndex(int index) {
    state = index;
  }
}

final mainTabProvider = NotifierProvider<MainTabController, int>(
  MainTabController.new,
);
