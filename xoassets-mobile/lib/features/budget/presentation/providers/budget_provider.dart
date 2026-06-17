import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../home/data/repositories/home_repository.dart';
import '../../data/repositories/budget_repository.dart';

/// 预算页真实月度汇总数据。
final budgetSummaryProvider = FutureProvider.autoDispose<BudgetSummaryModel>((
  ref,
) {
  return ref.read(budgetRepositoryProvider).summary();
});
