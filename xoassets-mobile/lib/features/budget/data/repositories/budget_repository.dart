import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/network/dio_provider.dart';
import '../../../home/data/repositories/home_repository.dart';

/// 预算接口 repository，复用首页预算展示模型。
class BudgetRepository {
  const BudgetRepository(this._ref);

  final Ref _ref;

  Future<BudgetSummaryModel> summary({DateTime? date}) {
    final month = DateFormat('yyyy-MM').format(date ?? DateTime.now());
    return _ref
        .read(apiClientProvider)
        .getData<BudgetSummaryModel>(
          '/budgets/summary',
          queryParameters: {'month': month},
          mapper: BudgetSummaryModel.fromJson,
        );
  }
}

final budgetRepositoryProvider = Provider<BudgetRepository>(
  BudgetRepository.new,
);
