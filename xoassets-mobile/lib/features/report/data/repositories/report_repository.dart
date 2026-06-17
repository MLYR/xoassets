import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/network/dio_provider.dart';
import '../../../../core/utils/json_utils.dart';
import '../../../home/data/repositories/home_repository.dart';

/// AI 报告接口 repository；后端当前为模板化报告，不在 App 端调用第三方 AI。
class ReportRepository {
  const ReportRepository(this._ref);

  final Ref _ref;

  Future<List<AiReportModel>> list() {
    return _ref
        .read(apiClientProvider)
        .getData<List<AiReportModel>>(
          '/reports',
          mapper: (value) => JsonUtils.asMapList(
            value,
          ).map(AiReportModel.fromJson).toList(growable: false),
        );
  }

  Future<AiReportModel> generateDailyPreview({DateTime? date}) {
    final reportDate = DateFormat('yyyy-MM-dd').format(date ?? DateTime.now());
    return _ref
        .read(apiClientProvider)
        .postData<AiReportModel>(
          '/reports/generate-preview',
          data: {'reportType': 'DAILY', 'reportDate': reportDate},
          mapper: (value) => AiReportModel.fromJson(JsonUtils.asMap(value)),
        );
  }
}

final reportRepositoryProvider = Provider<ReportRepository>(
  ReportRepository.new,
);
