import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/home_repository.dart';

/// 首页真实接口聚合数据，进入首页即拉取快照、统计、预算、AI 报告和最近流水。
final homeDashboardProvider = FutureProvider.autoDispose<HomeDashboardData>((
  ref,
) {
  return ref.read(homeRepositoryProvider).loadDashboard();
});
