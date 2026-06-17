import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/network/dio_provider.dart';
import '../../../../core/utils/json_utils.dart';
import '../../../ledger/data/repositories/transaction_repository.dart';

class AssetSnapshotModel {
  const AssetSnapshotModel({
    required this.totalAsset,
    required this.netAsset,
    required this.monthlyIncome,
    required this.monthlyExpense,
    required this.monthlyBalance,
    this.netAssetChangeFromYesterday,
  });

  final String totalAsset;
  final String netAsset;
  final String monthlyIncome;
  final String monthlyExpense;
  final String monthlyBalance;
  final String? netAssetChangeFromYesterday;

  factory AssetSnapshotModel.fromLatestJson(Object? value) {
    final map = JsonUtils.asMap(value);
    final latest = JsonUtils.asMap(map['latest']);
    return AssetSnapshotModel(
      totalAsset: JsonUtils.money(latest['totalAsset']),
      netAsset: JsonUtils.money(latest['netAsset']),
      monthlyIncome: JsonUtils.money(latest['monthlyIncome']),
      monthlyExpense: JsonUtils.money(latest['monthlyExpense']),
      monthlyBalance: JsonUtils.money(latest['monthlyBalance']),
      netAssetChangeFromYesterday: JsonUtils.string(
        map['netAssetChangeFromYesterday'],
      ),
    );
  }
}

class DashboardOverviewModel {
  const DashboardOverviewModel({
    required this.todayIncome,
    required this.todayExpense,
    required this.monthlyIncome,
    required this.monthlyExpense,
    required this.monthlyBalance,
    required this.investmentYesterdayProfit,
    required this.investmentTodayProfit,
  });

  final String todayIncome;
  final String todayExpense;
  final String monthlyIncome;
  final String monthlyExpense;
  final String monthlyBalance;
  final String investmentYesterdayProfit;
  final String investmentTodayProfit;

  factory DashboardOverviewModel.fromJson(Object? value) {
    final map = JsonUtils.asMap(value);
    return DashboardOverviewModel(
      todayIncome: JsonUtils.money(map['todayIncome']),
      todayExpense: JsonUtils.money(map['todayExpense']),
      monthlyIncome: JsonUtils.money(map['monthlyIncome']),
      monthlyExpense: JsonUtils.money(map['monthlyExpense']),
      monthlyBalance: JsonUtils.money(map['monthlyBalance']),
      investmentYesterdayProfit: JsonUtils.money(
        map['investmentYesterdayProfit'],
      ),
      investmentTodayProfit: JsonUtils.money(map['investmentTodayProfit']),
    );
  }
}

class BudgetSummaryModel {
  const BudgetSummaryModel({
    required this.totalBudget,
    required this.totalUsed,
    required this.totalRemaining,
    required this.usageRate,
    this.usageStatusLabel,
    this.items = const [],
  });

  final String totalBudget;
  final String totalUsed;
  final String totalRemaining;
  final double usageRate;
  final String? usageStatusLabel;
  final List<BudgetItemModel> items;

  factory BudgetSummaryModel.fromJson(Object? value) {
    final map = JsonUtils.asMap(value);
    return BudgetSummaryModel(
      totalBudget: JsonUtils.money(map['totalBudget']),
      totalUsed: JsonUtils.money(map['totalUsed']),
      totalRemaining: JsonUtils.money(map['totalRemaining']),
      usageRate: JsonUtils.ratio(map['usageRate']),
      usageStatusLabel: JsonUtils.string(map['usageStatusLabel']),
      items: JsonUtils.asMapList(
        map['items'],
      ).map(BudgetItemModel.fromJson).toList(growable: false),
    );
  }
}

class BudgetItemModel {
  const BudgetItemModel({
    required this.categoryName,
    required this.amount,
    required this.usedAmount,
    required this.usageRate,
    this.usageStatusLabel,
  });

  final String categoryName;
  final String amount;
  final String usedAmount;
  final double usageRate;
  final String? usageStatusLabel;

  factory BudgetItemModel.fromJson(Map<String, dynamic> json) {
    return BudgetItemModel(
      categoryName: JsonUtils.string(json['categoryName']) ?? '预算',
      amount: JsonUtils.money(json['amount']),
      usedAmount: JsonUtils.money(json['usedAmount']),
      usageRate: JsonUtils.ratio(json['usageRate']),
      usageStatusLabel: JsonUtils.string(json['usageStatusLabel']),
    );
  }
}

class AiReportModel {
  const AiReportModel({
    required this.title,
    required this.content,
    this.reportDate,
    this.statusLabel,
  });

  final String title;
  final String content;
  final String? reportDate;
  final String? statusLabel;

  factory AiReportModel.fromJson(Map<String, dynamic> json) {
    return AiReportModel(
      title: JsonUtils.string(json['title']) ?? 'AI 财务报告',
      content: JsonUtils.string(json['content']) ?? '暂无报告内容',
      reportDate: JsonUtils.string(json['reportDate']),
      statusLabel: JsonUtils.string(json['statusLabel']),
    );
  }
}

class HomeDashboardData {
  const HomeDashboardData({
    required this.snapshot,
    required this.overview,
    required this.budget,
    required this.recentTransactions,
    this.latestReport,
  });

  final AssetSnapshotModel snapshot;
  final DashboardOverviewModel overview;
  final BudgetSummaryModel budget;
  final List<TransactionItemModel> recentTransactions;
  final AiReportModel? latestReport;
}

/// 首页聚合多个后端接口，页面只消费聚合后的展示模型。
class HomeRepository {
  const HomeRepository(this._ref);

  final Ref _ref;

  Future<HomeDashboardData> loadDashboard() async {
    final now = DateTime.now();
    final month = DateFormat('yyyy-MM').format(now);
    final client = _ref.read(apiClientProvider);

    final results = await Future.wait<Object?>([
      client.getData<AssetSnapshotModel>(
        '/snapshots/latest',
        mapper: AssetSnapshotModel.fromLatestJson,
      ),
      client.getData<DashboardOverviewModel>(
        '/dashboard/overview',
        queryParameters: {'month': month},
        mapper: DashboardOverviewModel.fromJson,
      ),
      client.getData<BudgetSummaryModel>(
        '/budgets/summary',
        queryParameters: {'month': month},
        mapper: BudgetSummaryModel.fromJson,
      ),
      client.getData<List<AiReportModel>>(
        '/reports',
        mapper: (value) => JsonUtils.asMapList(
          value,
        ).map(AiReportModel.fromJson).toList(growable: false),
      ),
      _ref.read(transactionRepositoryProvider).page(pageSize: 5),
    ]);

    final reports = results[3] as List<AiReportModel>;
    final transactions = results[4] as TransactionPage;
    return HomeDashboardData(
      snapshot: results[0] as AssetSnapshotModel,
      overview: results[1] as DashboardOverviewModel,
      budget: results[2] as BudgetSummaryModel,
      latestReport: reports.isEmpty ? null : reports.first,
      recentTransactions: transactions.records,
    );
  }
}

final homeRepositoryProvider = Provider<HomeRepository>(HomeRepository.new);
