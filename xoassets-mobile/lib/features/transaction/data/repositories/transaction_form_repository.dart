import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/dio_provider.dart';
import '../../../../core/utils/json_utils.dart';

/// 表单下拉项统一模型，业务 ID 保持字符串，提交时再原样传给后端。
class FormOptionModel {
  const FormOptionModel({required this.id, required this.name, this.type});

  final String id;
  final String name;
  final String? type;

  factory FormOptionModel.fromJson(Map<String, dynamic> json) {
    return FormOptionModel(
      id: JsonUtils.string(json['id']) ?? '',
      name: JsonUtils.string(json['name']) ?? '',
      type: JsonUtils.string(json['type']),
    );
  }
}

class TransactionFormOptions {
  const TransactionFormOptions({
    required this.accounts,
    required this.categories,
  });

  final List<FormOptionModel> accounts;
  final List<FormOptionModel> categories;
}

/// 新增流水页表单依赖账户与分类接口，避免用户手填业务 ID。
class TransactionFormRepository {
  const TransactionFormRepository(this._ref);

  final Ref _ref;

  Future<TransactionFormOptions> load(String type) async {
    final accounts = await _ref
        .read(apiClientProvider)
        .getData<List<FormOptionModel>>(
          '/accounts',
          mapper: (value) => JsonUtils.asMapList(
            value,
          ).map(FormOptionModel.fromJson).toList(growable: false),
        );
    final categories = type == 'TRANSFER'
        ? <FormOptionModel>[]
        : await _ref
              .read(apiClientProvider)
              .getData<List<FormOptionModel>>(
                '/categories',
                queryParameters: {'type': type},
                mapper: (value) => JsonUtils.asMapList(
                  value,
                ).map(FormOptionModel.fromJson).toList(growable: false),
              );
    return TransactionFormOptions(accounts: accounts, categories: categories);
  }
}

final transactionFormRepositoryProvider = Provider<TransactionFormRepository>(
  TransactionFormRepository.new,
);
