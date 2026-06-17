import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/dio_provider.dart';
import '../../../../core/utils/json_utils.dart';
import '../../../transaction/data/repositories/transaction_form_repository.dart';

class InvestmentOverviewModel {
  const InvestmentOverviewModel({
    required this.totalInvestmentAsset,
    required this.totalCost,
    required this.holdingProfit,
    required this.yesterdayProfit,
    required this.todayProfit,
    this.todayProfitStatusLabel,
  });

  final String totalInvestmentAsset;
  final String totalCost;
  final String holdingProfit;
  final String yesterdayProfit;
  final String todayProfit;
  final String? todayProfitStatusLabel;

  factory InvestmentOverviewModel.fromJson(Object? value) {
    final map = JsonUtils.asMap(value);
    return InvestmentOverviewModel(
      totalInvestmentAsset: JsonUtils.money(map['totalInvestmentAsset']),
      totalCost: JsonUtils.money(map['totalCost']),
      holdingProfit: JsonUtils.money(map['holdingProfit']),
      yesterdayProfit: JsonUtils.money(map['yesterdayProfit']),
      todayProfit: JsonUtils.money(map['todayProfit']),
      todayProfitStatusLabel: JsonUtils.string(map['todayProfitStatusLabel']),
    );
  }
}

class HoldingModel {
  const HoldingModel({
    required this.id,
    required this.assetId,
    required this.assetName,
    required this.assetType,
    required this.marketValue,
    required this.profitLabel,
    required this.profit,
    this.symbol,
  });

  final String id;
  final String assetId;
  final String assetName;
  final String assetType;
  final String marketValue;
  final String profitLabel;
  final String profit;
  final String? symbol;

  factory HoldingModel.fromJson(Map<String, dynamic> json) {
    return HoldingModel(
      id: JsonUtils.string(json['id']) ?? '',
      assetId: JsonUtils.string(json['assetId']) ?? '',
      assetName: JsonUtils.string(json['assetName']) ?? '持仓',
      assetType: JsonUtils.string(json['assetType']) ?? 'OTHER',
      marketValue: JsonUtils.money(json['marketValue']),
      profitLabel: JsonUtils.string(json['primaryProfitLabel']) ?? '持有收益',
      profit: JsonUtils.money(
        json['primaryProfitAmount'] ?? json['totalProfit'],
      ),
      symbol: JsonUtils.string(json['symbol']),
    );
  }
}

class InvestmentDashboardData {
  const InvestmentDashboardData({
    required this.overview,
    required this.holdings,
  });

  final InvestmentOverviewModel overview;
  final List<HoldingModel> holdings;

  List<HoldingModel> holdingsByType(String type) {
    return holdings
        .where((item) => item.assetType == type)
        .toList(growable: false);
  }
}

class InvestmentFormOptions {
  const InvestmentFormOptions({required this.accounts, required this.holdings});

  final List<FormOptionModel> accounts;
  final List<HoldingModel> holdings;
}

/// 投资接口 repository：持仓、交易和行情刷新全部通过后端 API。
class InvestmentRepository {
  const InvestmentRepository(this._ref);

  final Ref _ref;

  Future<InvestmentDashboardData> loadDashboard() async {
    final overview = await _ref
        .read(apiClientProvider)
        .getData<InvestmentOverviewModel>(
          '/investments/overview',
          mapper: InvestmentOverviewModel.fromJson,
        );
    final holdingList = await holdings();
    return InvestmentDashboardData(overview: overview, holdings: holdingList);
  }

  Future<List<HoldingModel>> holdings({String module = 'ALL'}) {
    return _ref
        .read(apiClientProvider)
        .getData<List<HoldingModel>>(
          '/investments/holdings',
          queryParameters: {'module': module},
          mapper: (value) => JsonUtils.asMapList(
            value,
          ).map(HoldingModel.fromJson).toList(growable: false),
        );
  }

  Future<InvestmentFormOptions> loadFormOptions() async {
    final accounts = await _ref
        .read(apiClientProvider)
        .getData<List<FormOptionModel>>(
          '/accounts',
          mapper: (value) => JsonUtils.asMapList(
            value,
          ).map(FormOptionModel.fromJson).toList(growable: false),
        );
    return InvestmentFormOptions(
      accounts: accounts,
      holdings: await holdings(),
    );
  }

  Future<void> createTransaction(Map<String, dynamic> payload) async {
    await _ref
        .read(apiClientProvider)
        .postData<Object?>(
          '/investment-transactions',
          data: payload,
          mapper: (value) => value,
        );
  }

  Future<void> refreshQuotes(List<String> assetIds) async {
    await _ref
        .read(apiClientProvider)
        .postData<Object?>(
          '/quotes/refresh-batch',
          data: {'assetIds': assetIds},
          mapper: (value) => value,
        );
  }
}

final investmentRepositoryProvider = Provider<InvestmentRepository>(
  InvestmentRepository.new,
);
