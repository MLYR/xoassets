import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/investment_repository.dart';

/// 投资页真实总览和持仓列表。
final investmentDashboardProvider =
    FutureProvider.autoDispose<InvestmentDashboardData>((ref) {
      return ref.read(investmentRepositoryProvider).loadDashboard();
    });

/// 投资交易页表单下拉选项。
final investmentFormOptionsProvider =
    FutureProvider.autoDispose<InvestmentFormOptions>((ref) {
      return ref.read(investmentRepositoryProvider).loadFormOptions();
    });
