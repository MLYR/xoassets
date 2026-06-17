import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/dio_provider.dart';
import '../../../../core/utils/json_utils.dart';

/// 分页结果模型，屏蔽后端 PageResult 内部结构。
class TransactionPage {
  const TransactionPage({required this.records, required this.total});

  final List<TransactionItemModel> records;
  final int total;

  factory TransactionPage.fromJson(Object? value) {
    final map = JsonUtils.asMap(value);
    return TransactionPage(
      records: JsonUtils.asMapList(
        map['records'],
      ).map(TransactionItemModel.fromJson).toList(growable: false),
      total: JsonUtils.integer(map['total']),
    );
  }
}

/// App 流水展示模型，金额保持字符串展示，最终计算以后端为准。
class TransactionItemModel {
  const TransactionItemModel({
    required this.id,
    required this.type,
    required this.amount,
    required this.transactionTime,
    this.accountName,
    this.targetAccountName,
    this.categoryName,
    this.note,
  });

  final String id;
  final String type;
  final String amount;
  final String transactionTime;
  final String? accountName;
  final String? targetAccountName;
  final String? categoryName;
  final String? note;

  factory TransactionItemModel.fromJson(Map<String, dynamic> json) {
    return TransactionItemModel(
      id: JsonUtils.string(json['id']) ?? '',
      type: JsonUtils.string(json['type']) ?? '',
      amount: JsonUtils.money(json['amount']),
      transactionTime: JsonUtils.string(json['transactionTime']) ?? '',
      accountName: JsonUtils.string(json['accountName']),
      targetAccountName: JsonUtils.string(json['targetAccountName']),
      categoryName: JsonUtils.string(json['categoryName']),
      note: JsonUtils.string(json['note']),
    );
  }

  String get title {
    if (type == 'TRANSFER') {
      return '${accountName ?? '转出账户'} → ${targetAccountName ?? '转入账户'}';
    }
    return note?.isNotEmpty == true
        ? note!
        : categoryName ?? accountName ?? '流水';
  }

  String get signedAmount {
    if (type == 'EXPENSE' || type == 'TRANSFER') {
      return '-$amount';
    }
    return amount;
  }
}

/// 流水接口 repository，页面不得直接调用 Dio。
class TransactionRepository {
  const TransactionRepository(this._ref);

  final Ref _ref;

  Future<TransactionPage> page({
    int pageNo = 1,
    int pageSize = 20,
    String? type,
  }) {
    return _ref
        .read(apiClientProvider)
        .getData<TransactionPage>(
          '/transactions',
          queryParameters: {
            'pageNo': pageNo,
            'pageSize': pageSize,
            // ignore: use_null_aware_elements
            if (type != null) 'type': type,
          },
          mapper: TransactionPage.fromJson,
        );
  }

  Future<TransactionItemModel> create(Map<String, dynamic> payload) {
    return _ref
        .read(apiClientProvider)
        .postData<TransactionItemModel>(
          '/transactions',
          data: payload,
          mapper: (value) =>
              TransactionItemModel.fromJson(JsonUtils.asMap(value)),
        );
  }
}

final transactionRepositoryProvider = Provider<TransactionRepository>(
  TransactionRepository.new,
);
