import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/transaction_repository.dart';

/// 记账页流水列表，后端分页结果作为唯一数据源。
final ledgerTransactionsProvider = FutureProvider.autoDispose<TransactionPage>((
  ref,
) {
  return ref.read(transactionRepositoryProvider).page(pageSize: 20);
});
