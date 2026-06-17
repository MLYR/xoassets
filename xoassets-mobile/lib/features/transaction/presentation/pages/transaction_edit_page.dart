import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_button.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_error_view.dart';
import '../../../../core/widgets/xo_loading.dart';
import '../../../../core/widgets/xo_page.dart';
import '../../../ledger/presentation/providers/ledger_provider.dart';
import '../../../ledger/data/repositories/transaction_repository.dart';
import '../providers/transaction_form_provider.dart';

/// 流水录入页接入真实新增接口，账户和分类来自后端下拉数据。
class TransactionEditPage extends ConsumerStatefulWidget {
  const TransactionEditPage({required this.initialType, super.key});

  final String initialType;

  @override
  ConsumerState<TransactionEditPage> createState() =>
      _TransactionEditPageState();
}

class _TransactionEditPageState extends ConsumerState<TransactionEditPage> {
  late String _type = _normalizeType(widget.initialType);
  final _amountController = TextEditingController();
  final _noteController = TextEditingController();
  String? _accountId;
  String? _targetAccountId;
  String? _categoryId;
  bool _saving = false;

  @override
  void dispose() {
    _amountController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final options = ref.watch(transactionFormOptionsProvider(_type));

    return XoPage(
      appBar: AppBar(title: const Text('流水录入')),
      child: XoCard(
        child: Column(
          children: [
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'EXPENSE', label: Text('支出')),
                ButtonSegment(value: 'INCOME', label: Text('收入')),
                ButtonSegment(value: 'TRANSFER', label: Text('转账')),
              ],
              selected: {_type},
              onSelectionChanged: (value) {
                setState(() {
                  _type = value.first;
                  _categoryId = null;
                  _targetAccountId = null;
                });
              },
            ),
            const SizedBox(height: XoSpacing.md),
            TextField(
              controller: _amountController,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              decoration: const InputDecoration(labelText: '金额'),
            ),
            const SizedBox(height: XoSpacing.md),
            options.when(
              loading: () => const XoLoading(message: '正在加载表单选项'),
              error: (error, _) => XoErrorView(
                message: error.toString(),
                onRetry: () =>
                    ref.invalidate(transactionFormOptionsProvider(_type)),
              ),
              data: (data) => Column(
                children: [
                  DropdownButtonFormField<String>(
                    initialValue: _accountId,
                    decoration: const InputDecoration(labelText: '账户'),
                    items: data.accounts
                        .map(
                          (item) => DropdownMenuItem(
                            value: item.id,
                            child: Text(item.name),
                          ),
                        )
                        .toList(growable: false),
                    onChanged: (value) => setState(() => _accountId = value),
                  ),
                  if (_type == 'TRANSFER') ...[
                    const SizedBox(height: XoSpacing.md),
                    DropdownButtonFormField<String>(
                      initialValue: _targetAccountId,
                      decoration: const InputDecoration(labelText: '转入账户'),
                      items: data.accounts
                          .where((item) => item.id != _accountId)
                          .map(
                            (item) => DropdownMenuItem(
                              value: item.id,
                              child: Text(item.name),
                            ),
                          )
                          .toList(growable: false),
                      onChanged: (value) =>
                          setState(() => _targetAccountId = value),
                    ),
                  ] else ...[
                    const SizedBox(height: XoSpacing.md),
                    DropdownButtonFormField<String>(
                      initialValue: _categoryId,
                      decoration: const InputDecoration(labelText: '分类'),
                      items: data.categories
                          .map(
                            (item) => DropdownMenuItem(
                              value: item.id,
                              child: Text(item.name),
                            ),
                          )
                          .toList(growable: false),
                      onChanged: (value) => setState(() => _categoryId = value),
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(height: XoSpacing.md),
            TextField(
              controller: _noteController,
              decoration: const InputDecoration(labelText: '备注'),
            ),
            const SizedBox(height: XoSpacing.md),
            OutlinedButton.icon(
              onPressed: null,
              icon: const Icon(Icons.image_outlined),
              label: const Text('图片附件将在附件阶段接入'),
            ),
            const SizedBox(height: XoSpacing.lg),
            XoButton(label: '保存', isLoading: _saving, onPressed: _submit),
          ],
        ),
      ),
    );
  }

  Future<void> _submit() async {
    final amount = double.tryParse(_amountController.text.trim());
    if (amount == null || amount <= 0) {
      _showMessage('金额必须大于 0');
      return;
    }
    if (_accountId == null) {
      _showMessage('请选择账户');
      return;
    }
    if (_type == 'TRANSFER' && _targetAccountId == null) {
      _showMessage('请选择转入账户');
      return;
    }
    if (_type != 'TRANSFER' && _categoryId == null) {
      _showMessage('请选择分类');
      return;
    }

    setState(() => _saving = true);
    try {
      // App 端只提交用户输入，账户余额和统计重算全部以后端事务为准。
      await ref.read(transactionRepositoryProvider).create({
        'type': _type,
        'amount': _amountController.text.trim(),
        'accountId': _accountId,
        if (_type == 'TRANSFER') 'targetAccountId': _targetAccountId,
        if (_type != 'TRANSFER') 'categoryId': _categoryId,
        'transactionTime': DateTime.now().toIso8601String(),
        'note': _noteController.text.trim(),
      });
      ref.invalidate(ledgerTransactionsProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('流水已保存')));
      context.pop();
    } catch (error) {
      if (mounted) {
        _showMessage(error.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _saving = false);
      }
    }
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  String _normalizeType(String value) {
    return switch (value.toUpperCase()) {
      'INCOME' => 'INCOME',
      'TRANSFER' => 'TRANSFER',
      _ => 'EXPENSE',
    };
  }
}
