import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_button.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_empty.dart';
import '../../../../core/widgets/xo_error_view.dart';
import '../../../../core/widgets/xo_loading.dart';
import '../../../../core/widgets/xo_page.dart';
import '../providers/report_provider.dart';
import '../../data/repositories/report_repository.dart';

/// AI 报告页接入模板化报告接口，App 不做 AI 聊天入口。
class ReportPage extends ConsumerStatefulWidget {
  const ReportPage({super.key});

  @override
  ConsumerState<ReportPage> createState() => _ReportPageState();
}

class _ReportPageState extends ConsumerState<ReportPage> {
  bool _generating = false;

  @override
  Widget build(BuildContext context) {
    final reports = ref.watch(reportListProvider);

    return XoPage(
      appBar: AppBar(title: const Text('AI 财务报告')),
      child: Column(
        children: [
          XoButton(
            label: '生成今日复盘',
            isLoading: _generating,
            onPressed: _generateReport,
          ),
          const SizedBox(height: XoSpacing.md),
          reports.when(
            loading: () => const XoLoading(message: '正在加载报告'),
            error: (error, _) => XoErrorView(
              message: error.toString(),
              onRetry: () => ref.invalidate(reportListProvider),
            ),
            data: (items) => XoCard(
              child: items.isEmpty
                  ? const XoEmpty(message: '暂无 AI 报告，点击上方生成今日复盘')
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        for (final item in items)
                          Padding(
                            padding: const EdgeInsets.only(
                              bottom: XoSpacing.md,
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  item.title,
                                  style: Theme.of(
                                    context,
                                  ).textTheme.titleMedium,
                                ),
                                const SizedBox(height: XoSpacing.xs),
                                Text(item.reportDate ?? ''),
                                const SizedBox(height: XoSpacing.sm),
                                Text(item.content),
                              ],
                            ),
                          ),
                      ],
                    ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _generateReport() async {
    setState(() => _generating = true);
    try {
      await ref.read(reportRepositoryProvider).generateDailyPreview();
      ref.invalidate(reportListProvider);
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('今日复盘已生成')));
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) {
        setState(() => _generating = false);
      }
    }
  }
}
