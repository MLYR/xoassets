import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_button.dart';
import '../../../../core/widgets/xo_card.dart';
import '../../../../core/widgets/xo_empty.dart';
import '../../../../core/widgets/xo_error_view.dart';
import '../../../../core/widgets/xo_loading.dart';
import '../../../../core/widgets/xo_page.dart';
import '../providers/investment_provider.dart';
import '../../data/repositories/investment_repository.dart';

/// 投资交易页接入真实投资交易接口，收益和资金变动全部以后端事务为准。
class InvestmentTradePage extends ConsumerStatefulWidget {
  const InvestmentTradePage({super.key});

  @override
  ConsumerState<InvestmentTradePage> createState() =>
      _InvestmentTradePageState();
}

class _InvestmentTradePageState extends ConsumerState<InvestmentTradePage> {
  String _type = 'BUY';
  String? _holdingId;
  String? _accountId;
  final _priceController = TextEditingController();
  final _quantityController = TextEditingController();
  final _feeController = TextEditingController(text: '0');
  final _noteController = TextEditingController();
  bool _saving = false;

  @override
  void dispose() {
    _priceController.dispose();
    _quantityController.dispose();
    _feeController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final options = ref.watch(investmentFormOptionsProvider);

    return XoPage(
      appBar: AppBar(title: const Text('投资交易')),
      child: XoCard(
        child: Column(
          children: [
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'BUY', label: Text('买入')),
                ButtonSegment(value: 'SELL', label: Text('卖出')),
              ],
              selected: {_type},
              onSelectionChanged: (value) =>
                  setState(() => _type = value.first),
            ),
            const SizedBox(height: XoSpacing.md),
            options.when(
              loading: () => const XoLoading(message: '正在加载交易选项'),
              error: (error, _) => XoErrorView(
                message: error.toString(),
                onRetry: () => ref.invalidate(investmentFormOptionsProvider),
              ),
              data: (data) {
                if (data.holdings.isEmpty) {
                  return const XoEmpty(message: '暂无持仓，请先在 Web 端或后续资产页创建持仓');
                }
                return Column(
                  children: [
                    DropdownButtonFormField<String>(
                      initialValue: _holdingId,
                      decoration: const InputDecoration(labelText: '持仓 / 资产'),
                      items: data.holdings
                          .map(
                            (item) => DropdownMenuItem(
                              value: item.id,
                              child: Text(item.assetName),
                            ),
                          )
                          .toList(growable: false),
                      onChanged: (value) => setState(() => _holdingId = value),
                    ),
                    const SizedBox(height: XoSpacing.md),
                    DropdownButtonFormField<String>(
                      initialValue: _accountId,
                      decoration: const InputDecoration(labelText: '资金账户'),
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
                  ],
                );
              },
            ),
            const SizedBox(height: XoSpacing.md),
            TextField(
              controller: _priceController,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              decoration: const InputDecoration(labelText: '成交价格'),
            ),
            const SizedBox(height: XoSpacing.md),
            TextField(
              controller: _quantityController,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              decoration: const InputDecoration(labelText: '数量 / 份额'),
            ),
            const SizedBox(height: XoSpacing.md),
            TextField(
              controller: _feeController,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              decoration: const InputDecoration(labelText: '手续费'),
            ),
            const SizedBox(height: XoSpacing.md),
            TextField(
              controller: _noteController,
              decoration: const InputDecoration(labelText: '备注'),
            ),
            const SizedBox(height: XoSpacing.lg),
            XoButton(label: '保存', isLoading: _saving, onPressed: _submit),
          ],
        ),
      ),
    );
  }

  Future<void> _submit() async {
    final asyncOptions = ref.read(investmentFormOptionsProvider);
    final data = asyncOptions.hasValue ? asyncOptions.requireValue : null;
    final selectedHolding = _findHolding(data?.holdings ?? const []);
    final price = double.tryParse(_priceController.text.trim());
    final quantity = double.tryParse(_quantityController.text.trim());
    if (selectedHolding == null) {
      _showMessage('请选择持仓');
      return;
    }
    if (_accountId == null) {
      _showMessage('请选择资金账户');
      return;
    }
    if (price == null || price <= 0) {
      _showMessage('成交价格必须大于 0');
      return;
    }
    if (quantity == null || quantity <= 0) {
      _showMessage('数量必须大于 0');
      return;
    }

    setState(() => _saving = true);
    try {
      await ref.read(investmentRepositoryProvider).createTransaction({
        'holdingId': selectedHolding.id,
        'assetId': selectedHolding.assetId,
        'accountId': _accountId,
        'type': _type,
        'quantity': _quantityController.text.trim(),
        'price': _priceController.text.trim(),
        'fee': _feeController.text.trim().isEmpty
            ? '0'
            : _feeController.text.trim(),
        'transactionTime': DateTime.now().toIso8601String(),
        'note': _noteController.text.trim(),
      });
      ref.invalidate(investmentDashboardProvider);
      ref.invalidate(investmentFormOptionsProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('投资交易已保存')));
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

  HoldingModel? _findHolding(List<HoldingModel> holdings) {
    for (final item in holdings) {
      if (item.id == _holdingId) {
        return item;
      }
    }
    return null;
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}
