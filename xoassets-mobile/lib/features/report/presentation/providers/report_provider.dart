import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../home/data/repositories/home_repository.dart';
import '../../data/repositories/report_repository.dart';

/// AI 报告列表来自后端 `/reports`。
final reportListProvider = FutureProvider.autoDispose<List<AiReportModel>>((
  ref,
) {
  return ref.read(reportRepositoryProvider).list();
});
