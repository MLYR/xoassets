import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/transaction_form_repository.dart';

/// 新增流水页下拉数据随流水类型变化，转账不加载分类。
final transactionFormOptionsProvider = FutureProvider.autoDispose
    .family<TransactionFormOptions, String>((ref, type) {
      return ref.read(transactionFormRepositoryProvider).load(type);
    });
