<!-- 投资持仓详情页：聚焦单个基金、股票或虚拟货币的收益、交易和价格记录。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button class="back-button" @click="router.push(ROUTES.investmentDetails)">返回投资明细</el-button>
        <h1 class="page-title">{{ holding?.assetName || '持仓详情' }}</h1>
        <p class="page-subtitle">
          {{ holding?.symbol || '-' }} · {{ holding?.market || '-' }} · {{ typeLabel(holding?.assetType) }} · {{ holding?.currency || '-' }} · {{ holding?.latestPriceSource || holding?.quoteSource || '暂无行情' }} · 最新报价 {{ formatTableTime(holding?.latestPriceTime) || '暂无' }}
        </p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openTradeDialog('BUY')">买入</el-button>
        <el-button type="primary" plain @click="openTradeDialog('SELL')">卖出</el-button>
        <el-button @click="openQuoteDialog">手动价格</el-button>
        <el-button :loading="refreshingQuote" @click="handleRefreshQuote">刷新行情</el-button>
      </div>
    </div>

    <section v-loading="loading" class="summary-grid">
      <MetricCard title="当前市值" :value="holding?.marketValue || 0" :trend="holding?.floatingProfitRate || 0" description="数量 × 最新价格" :precision="4" :currency-symbol="currencySymbol" />
      <MetricCard title="持仓数量" :value="holding?.quantity || 0" :trend="0" description="当前剩余持仓" :precision="quantityPrecision" currency-symbol="" />
      <MetricCard title="持仓成本" :value="holding?.totalCost || 0" :trend="0" description="移动平均成本口径" :precision="4" :currency-symbol="currencySymbol" />
      <MetricCard title="今日收益" :value="holding?.todayProfit || 0" :trend="holding?.todayChangeRate || 0" :description="todayProfitDescription" :precision="4" :currency-symbol="currencySymbol" :tone="profitTone(holding?.todayProfit || 0)" />
      <MetricCard title="总收益" :value="summary?.totalProfit || 0" :trend="summary?.totalProfitRate || 0" description="已实现 + 浮动盈亏" :precision="4" :currency-symbol="currencySymbol" :tone="profitTone(summary?.totalProfit || 0)" />
      <div class="rate-card panel panel-padding">
        <span>总收益率</span>
        <strong :class="profitClass(summary?.totalProfitRate || 0)">{{ formatPercent(summary?.totalProfitRate || 0) }}</strong>
        <small>总收益 / 累计买入成本</small>
      </div>
      <MetricCard title="已实现盈亏" :value="summary?.realizedProfit || 0" :trend="summary?.realizedProfit || 0" description="正常卖出交易合计" :precision="4" :currency-symbol="currencySymbol" :tone="profitTone(summary?.realizedProfit || 0)" />
      <div class="break-even-card panel panel-padding">
        <span>回本涨幅</span>
        <strong :class="breakEvenClass(holding?.breakEvenRate)">{{ formatBreakEven(holding?.breakEvenRate) }}</strong>
        <small>基于当前价和平均成本计算</small>
      </div>
    </section>

    <section class="panel chart-panel">
      <div class="section-head">
        <div>
          <h2>持仓走势</h2>
          <span class="muted-text">最近 30 条价格快照</span>
        </div>
        <el-segmented v-model="trendMode" :options="trendModeOptions" />
      </div>
      <el-empty v-if="!loading && priceSnapshots.length === 0" description="暂无价格记录" />
      <BaseChart v-else :option="priceChartOption" height="300px" />
    </section>

    <section v-loading="loading" class="panel transactions-panel">
      <div class="section-head">
        <h2>交易记录</h2>
        <span class="muted-text">撤销记录保留展示，但不参与汇总统计</span>
      </div>
      <el-empty v-if="!loading && transactions.length === 0" description="暂无该持仓的交易记录" />
      <el-table v-else :data="transactions" stripe height="420">
        <el-table-column label="时间" prop="transactionTime" min-width="170" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }"><StatusBadge :label="row.type === 'BUY' ? '买入' : '卖出'" /></template>
        </el-table-column>
        <el-table-column label="资金账户" prop="accountName" min-width="140" />
        <el-table-column label="数量" min-width="120" align="right" header-align="right">
          <template #default="{ row }"><span class="numeric-cell">{{ formatQuantity(row.quantity) }}</span></template>
        </el-table-column>
        <el-table-column label="成交价" min-width="130" align="right" header-align="right">
          <template #default="{ row }"><AmountText class="numeric-cell" :value="row.price" :precision="pricePrecision" :currency-symbol="currencySymbol" /></template>
        </el-table-column>
        <el-table-column label="确认信息" min-width="190">
          <template #default="{ row }">{{ formatConfirmInfo(row) }}</template>
        </el-table-column>
        <el-table-column label="成交金额" min-width="140" align="right" header-align="right">
          <template #default="{ row }"><AmountText class="numeric-cell" :value="row.amount" :precision="4" :currency-symbol="currencySymbol" /></template>
        </el-table-column>
        <el-table-column label="手续费" min-width="120" align="right" header-align="right">
          <template #default="{ row }"><AmountText class="numeric-cell" :value="row.fee" :precision="4" :currency-symbol="currencySymbol" /></template>
        </el-table-column>
        <el-table-column label="已实现盈亏" min-width="150" align="right" header-align="right">
          <template #default="{ row }">
            <AmountText v-if="row.realizedProfit !== null && row.realizedProfit !== undefined" class="numeric-cell" :value="row.realizedProfit" with-sign :precision="4" :currency-symbol="currencySymbol" />
            <span v-else class="numeric-cell muted-text">暂无</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusBadge :label="statusLabel(row.status)" /></template>
        </el-table-column>
        <el-table-column label="备注" min-width="180">
          <template #default="{ row }">{{ row.status === 'REVOKED' ? (row.revokeReason || '已撤销') : (row.note || '-') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" :disabled="row.status === 'REVOKED' || row.status === 'CANCELLED'" @click="handleRevokeTransaction(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="tradeDialogVisible" :title="tradeForm.type === 'BUY' ? '买入' : '卖出'" width="440px">
      <el-form label-position="top" @submit.prevent="handleCreateTrade">
        <el-form-item label="持仓"><el-input :model-value="holding?.assetName || '-'" disabled /></el-form-item>
        <el-form-item :label="tradeForm.type === 'BUY' ? '扣款账户' : '到账账户'">
          <el-select v-model="tradeForm.accountId" class="full-width" placeholder="请选择资金账户">
            <el-option v-for="account in accounts" :key="account.id" :label="`${account.name} · ${account.balance.toFixed(2)} ${account.currency}`" :value="account.id" />
          </el-select>
        </el-form-item>
        <template v-if="isFundAmountBuy">
          <el-form-item label="买入总金额"><el-input-number v-model="tradeForm.tradeAmount" class="full-width" :min="0.01" :precision="2" /></el-form-item>
          <el-form-item label="交易日期"><el-date-picker v-model="tradeForm.transactionTime" type="date" class="full-width" /></el-form-item>
          <small class="form-tip">基金买入会按确认净值自动反推确认份额；当天净值未出时先保存为待确认。</small>
        </template>
        <template v-else>
          <el-form-item label="数量"><el-input-number v-model="tradeForm.quantity" class="full-width" :min="quantityMin" :precision="quantityPrecision" /></el-form-item>
          <el-form-item label="价格"><el-input-number v-model="tradeForm.price" class="full-width" :min="0.000001" :precision="pricePrecision" /></el-form-item>
          <el-form-item label="交易时间"><el-date-picker v-model="tradeForm.transactionTime" type="datetime" class="full-width" /></el-form-item>
        </template>
        <el-form-item label="手续费"><el-input-number v-model="tradeForm.fee" class="full-width" :min="0" :precision="4" /></el-form-item>
        <el-form-item label="备注"><el-input v-model.trim="tradeForm.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tradeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateTrade">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quoteDialogVisible" title="手动更新价格" width="420px">
      <el-form label-position="top" @submit.prevent="handleManualQuote">
        <el-form-item label="持仓"><el-input :model-value="holding?.assetName || '-'" disabled /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="quoteForm.price" class="full-width" :min="0.000001" :precision="pricePrecision" /></el-form-item>
        <el-form-item label="币种"><el-input v-model.trim="quoteForm.currency" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quoteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleManualQuote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// 页面只读取当前 holdingId 的详情接口，买入卖出后重新拉取详情保证汇总口径来自后端。
import { computed, onMounted, reactive, ref } from 'vue';
import type { EChartsOption } from 'echarts';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { ROUTES } from '@/constants/routes';
import { accountApi, type AccountItem } from '@/services/accountApi';
import { investmentApi, type AssetPriceItem, type HoldingDetailSummary, type HoldingItem, type InvestmentTransactionItem, type InvestmentTransactionType } from '@/services/investmentApi';

const route = useRoute();
const router = useRouter();
const holding = ref<HoldingItem | null>(null);
const summary = ref<HoldingDetailSummary | null>(null);
const transactions = ref<InvestmentTransactionItem[]>([]);
const priceSnapshots = ref<AssetPriceItem[]>([]);
const accounts = ref<AccountItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
const refreshingQuote = ref(false);
const trendMode = ref<'MARKET_VALUE' | 'PRICE'>('MARKET_VALUE');
const tradeDialogVisible = ref(false);
const quoteDialogVisible = ref(false);
const tradeForm = reactive({ type: 'BUY' as InvestmentTransactionType, accountId: '', quantity: 0, price: 0, tradeAmount: 0, fee: 0, transactionTime: new Date(), note: '' });
const quoteForm = reactive({ price: 0, currency: 'CNY' });
const trendModeOptions = [
  { label: '总市值', value: 'MARKET_VALUE' },
  { label: '价格', value: 'PRICE' }
];

const holdingId = computed(() => String(route.params.id || ''));
const currencySymbol = computed(() => (holding.value?.currency === 'USD' ? '$' : '¥'));
const pricePrecision = computed(() => holding.value?.priceScale || (holding.value?.assetType === 'CRYPTO' ? 6 : 4));
const quantityPrecision = computed(() => holding.value?.assetType === 'CRYPTO' ? 10 : 4);
const quantityMin = computed(() => holding.value?.assetType === 'CRYPTO' ? 0.0000000001 : 0.0001);
const isFundAmountBuy = computed(() => holding.value?.assetType === 'FUND' && tradeForm.type === 'BUY');
const todayProfitDescription = computed(() => holding.value?.todayPriceAvailable === false ? '今日净值未更新' : '今日有效价对比昨价');
const priceChartOption = computed<EChartsOption>(() => {
  const points = [...priceSnapshots.value].reverse();
  const quantity = Number(holding.value?.quantity || 0);
  const isMarketValue = trendMode.value === 'MARKET_VALUE';
  return {
    tooltip: { trigger: 'axis', valueFormatter: (value) => formatChartValue(Number(value), isMarketValue ? 4 : pricePrecision.value) },
    grid: { left: 48, right: 24, top: 28, bottom: 42 },
    xAxis: { type: 'category', data: points.map((item) => formatTableTime(item.quoteTime)), axisLabel: { color: '#6b7280' } },
    yAxis: { type: 'value', scale: true, axisLabel: { color: '#6b7280', formatter: (value: number) => formatChartAxis(value) } },
    series: [
      {
        name: isMarketValue ? '总市值' : '价格',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        // 当前没有历史持仓数量快照，市值走势使用当前持仓数量乘以历史价格快照。
        data: points.map((item) => roundTo(isMarketValue ? Number(item.price) * quantity : item.price, isMarketValue ? 4 : pricePrecision.value)),
        lineStyle: { width: 3, color: '#2563eb' },
        itemStyle: { color: '#2563eb' },
        areaStyle: { color: 'rgba(37, 99, 235, 0.08)' }
      }
    ]
  };
});

onMounted(() => {
  loadPageData();
});

async function loadPageData() {
  if (!holdingId.value) {
    ElMessage.error('持仓不存在');
    router.replace(ROUTES.investmentDetails);
    return;
  }
  loading.value = true;
  try {
    const [detail, accountList] = await Promise.all([
      investmentApi.detailHolding(holdingId.value),
      accountApi.list()
    ]);
    holding.value = detail.holding;
    summary.value = detail.summary;
    transactions.value = detail.transactions;
    priceSnapshots.value = detail.priceSnapshots;
    accounts.value = accountList;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '持仓详情加载失败');
  } finally {
    loading.value = false;
  }
}

function openTradeDialog(type: InvestmentTransactionType) {
  if (!holding.value) {
    return;
  }
  tradeForm.type = type;
  tradeForm.accountId = accounts.value[0]?.id || '';
  tradeForm.quantity = 0;
  tradeForm.price = roundTo(Number(holding.value.latestPrice || holding.value.avgCost || 0), pricePrecision.value);
  tradeForm.tradeAmount = 0;
  tradeForm.fee = 0;
  tradeForm.transactionTime = new Date();
  tradeForm.note = '';
  tradeDialogVisible.value = true;
}

function openQuoteDialog() {
  if (!holding.value) {
    return;
  }
  quoteForm.price = roundTo(Number(holding.value.latestPrice || holding.value.avgCost || 0), pricePrecision.value);
  quoteForm.currency = holding.value.currency || 'CNY';
  quoteDialogVisible.value = true;
}

async function handleCreateTrade() {
  if (!holding.value || !tradeForm.accountId) {
    ElMessage.warning('请选择资金账户');
    return;
  }
  if (isFundAmountBuy.value && tradeForm.tradeAmount <= 0) {
    ElMessage.warning('请输入有效的基金买入总金额');
    return;
  }
  if (!isFundAmountBuy.value && (tradeForm.quantity <= 0 || tradeForm.price <= 0)) {
    ElMessage.warning('请输入有效的数量和价格');
    return;
  }
  submitting.value = true;
  try {
    await investmentApi.createTransaction({
      holdingId: holding.value.id,
      assetId: holding.value.assetId,
      accountId: tradeForm.accountId,
      type: tradeForm.type,
      inputMode: isFundAmountBuy.value ? 'AMOUNT_NAV' : 'QUANTITY_PRICE',
      tradeAmount: isFundAmountBuy.value ? roundTo(tradeForm.tradeAmount, 2) : undefined,
      quantity: isFundAmountBuy.value ? undefined : roundQuantity(tradeForm.quantity),
      price: isFundAmountBuy.value ? undefined : roundTo(tradeForm.price, pricePrecision.value),
      fee: round4(tradeForm.fee),
      transactionTime: formatDateTime(tradeForm.transactionTime),
      note: tradeForm.note
    });
    tradeDialogVisible.value = false;
    ElMessage.success(tradeForm.type === 'BUY' ? '买入已记录' : '卖出已记录');
    await loadPageData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '交易保存失败');
  } finally {
    submitting.value = false;
  }
}

async function handleManualQuote() {
  if (!holding.value || quoteForm.price <= 0) {
    ElMessage.warning('请输入有效价格');
    return;
  }
  submitting.value = true;
  try {
    await investmentApi.manualQuote({ assetId: holding.value.assetId, price: roundTo(quoteForm.price, pricePrecision.value), currency: quoteForm.currency });
    quoteDialogVisible.value = false;
    ElMessage.success('价格已更新');
    await loadPageData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '价格更新失败');
  } finally {
    submitting.value = false;
  }
}

async function handleRefreshQuote() {
  if (!holding.value) {
    return;
  }
  refreshingQuote.value = true;
  try {
    await investmentApi.refreshQuote({ assetId: holding.value.assetId });
    ElMessage.success('行情已刷新');
    await loadPageData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '行情刷新失败');
  } finally {
    refreshingQuote.value = false;
  }
}

async function handleRevokeTransaction(transaction: InvestmentTransactionItem) {
  try {
    await ElMessageBox.confirm('撤销后会反向恢复账户余额和持仓，确认继续吗？', '撤销投资交易', { type: 'warning', confirmButtonText: '撤销', cancelButtonText: '取消' });
    await investmentApi.revokeTransaction(transaction.id, '录入错误');
    ElMessage.success('投资交易已撤销');
    await loadPageData();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '投资交易撤销失败');
    }
  }
}

function typeLabel(type?: string | null) {
  return ({ FUND: '基金', STOCK: '股票', CRYPTO: '虚拟货币', OTHER: '其他' } as Record<string, string>)[type || ''] || '-';
}

function formatDateTime(date: Date) {
  const pad = (value: number) => `${value}`.padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function formatTableTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '';
}

function formatQuantity(value: number) {
  return roundTo(Number(value), quantityPrecision.value).toLocaleString('zh-CN', { minimumFractionDigits: holding.value?.assetType === 'CRYPTO' ? 0 : 4, maximumFractionDigits: quantityPrecision.value });
}

function statusLabel(status?: string | null) {
  return ({ NORMAL: '正常', CONFIRMED: '已确认', PENDING_CONFIRM: '待确认', FAILED: '确认失败', CANCELLED: '已取消', REVOKED: '已撤销' } as Record<string, string>)[status || ''] || '正常';
}

function formatConfirmInfo(row: InvestmentTransactionItem) {
  if (row.inputMode !== 'AMOUNT_NAV') {
    return '-';
  }
  if (row.status === 'PENDING_CONFIRM') {
    return `待确认 · ${row.confirmedDate || row.tradeDate || '-'}`;
  }
  if (row.confirmedNav && row.confirmedQuantity) {
    return `净值 ${roundTo(row.confirmedNav, 4).toFixed(4)} · 份额 ${roundTo(row.confirmedQuantity, 4).toFixed(4)}`;
  }
  return row.confirmedDate || '-';
}

function formatBreakEven(value: number | null | undefined) {
  // 回本涨幅只在亏损时展示需要上涨比例；盈利或打平展示已盈利。
  if (value === null || value === undefined) {
    return '暂无';
  }
  return Number(value) > 0 ? `还需 +${round4(value).toFixed(4)}%` : '已盈利';
}

function breakEvenClass(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return 'muted-text';
  }
  return Number(value) > 0 ? 'warning-text' : 'success-text';
}

function profitTone(value: number): 'success' | 'danger' | 'primary' {
  if (value > 0) {
    return 'success';
  }
  if (value < 0) {
    return 'danger';
  }
  return 'primary';
}

function profitClass(value: number) {
  if (value > 0) {
    return 'success-text';
  }
  if (value < 0) {
    return 'danger-text';
  }
  return 'muted-text';
}

function formatPercent(value: number) {
  return `${round4(value).toFixed(4)}%`;
}

function formatChartAxis(value: number) {
  const abs = Math.abs(value);
  if (abs >= 10000) {
    return `${currencySymbol.value}${round4(value / 10000)}万`;
  }
  return `${currencySymbol.value}${round4(value)}`;
}

function formatChartValue(value: number, precision: number) {
  return `${currencySymbol.value}${roundTo(value, precision).toLocaleString('zh-CN', { minimumFractionDigits: precision > 4 ? 4 : 2, maximumFractionDigits: precision })}`;
}

function round4(value: number) {
  return roundTo(value, 4);
}

function roundQuantity(value: number) {
  return roundTo(value, quantityPrecision.value);
}

function roundTo(value: number, precision: number) {
  return Number(Number(value || 0).toFixed(precision));
}
</script>

<style scoped>
/* 详情页延续投资明细页的玻璃面板、表格和数字排版风格。 */
.back-button {
  margin-bottom: 10px;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.break-even-card,
.rate-card {
  min-height: 154px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
  overflow: hidden;
}

.break-even-card::after,
.rate-card::after {
  position: absolute;
  inset: auto -28px -42px auto;
  width: 110px;
  height: 110px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  content: "";
}

.break-even-card span,
.break-even-card small,
.rate-card span,
.rate-card small {
  color: var(--xo-muted);
}

.break-even-card strong,
.rate-card strong {
  font-size: 28px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.chart-panel,
.transactions-panel {
  margin-top: 18px;
  overflow: hidden;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 18px 8px;
  gap: 16px;
}

.section-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.numeric-cell {
  display: inline-block;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.muted-text {
  color: var(--xo-muted);
}

.success-text {
  color: var(--xo-success);
}

.danger-text {
  color: var(--xo-danger);
}

.warning-text {
  color: var(--xo-warning);
}

.full-width {
  width: 100%;
}

@media (max-width: 960px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
