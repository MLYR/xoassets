<!-- 投资持仓详情页：聚焦单个基金、股票或虚拟货币的收益、交易和价格记录。 -->
<template>
  <div class="page">
    <div class="page-actions page-actions-between">
      <el-button class="back-button" @click="goBack">返回</el-button>
      <div class="header-actions">
        <el-button type="primary" @click="openTradeDialog('BUY')">买入</el-button>
        <el-button type="primary" plain @click="openTradeDialog('SELL')">卖出</el-button>
        <el-button @click="openQuoteDialog">手动价格</el-button>
        <el-button :loading="refreshingQuote" @click="handleRefreshQuote">刷新行情</el-button>
      </div>
    </div>

    <section v-loading="loading" class="summary-grid">
      <MetricCard title="当前市值" :value="holding?.marketValue ?? 0" :trend="holding?.floatingProfitRate ?? 0" description="数量 × 最新价格" :precision="4" :currency-symbol="currencySymbol" />
      <MetricCard title="持仓数量" :value="holding?.quantity ?? 0" :trend="null" description="" :precision="quantityPrecision" currency-symbol="">
        <template #extra>
          <!-- 数量本身没有涨跌率，底部留空避免展示无意义的 0.0%。 -->
          <span class="metric-empty-extra" aria-hidden="true"></span>
        </template>
      </MetricCard>
      <MetricCard title="持仓成本" :value="holding?.totalCost ?? 0" :trend="null" description="" :precision="4" :currency-symbol="currencySymbol">
        <template #extra>
          <!-- 成本卡底部展示券商常见的成本价，避免把 0.0% 误读成收益率。 -->
          <div class="metric-extra-row">
            <span>成本价</span>
            <AmountText :value="holding?.avgCost ?? null" :precision="pricePrecision" :currency-symbol="currencySymbol" />
          </div>
        </template>
      </MetricCard>
      <MetricCard title="今日收益" :value="holding?.todayProfit ?? null" :trend="primaryProfitTrend" :description="primaryProfitDescription" :precision="4" :currency-symbol="currencySymbol" :tone="todayProfitTone">
        <template #extra>
          <!-- 今日收益主值仍按今日价门禁，辅助区固定展示独立的昨日收益。 -->
          <div class="metric-extra-row">
            <span>昨日收益</span>
            <AmountText :value="holding?.yesterdayProfit ?? null" with-sign :precision="4" :currency-symbol="currencySymbol" />
          </div>
        </template>
      </MetricCard>
      <MetricCard title="总收益" :value="summary?.totalProfit ?? 0" :trend="null" description="" :precision="4" :currency-symbol="currencySymbol" :tone="profitTone(summary?.totalProfit ?? 0)">
        <template #extra>
          <!-- 总收益率单独放在右侧卡片，金额卡只解释金额组成，减少重复百分比。 -->
          <span class="metric-text-extra">已实现盈亏 + 浮动盈亏</span>
        </template>
      </MetricCard>
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

    <section v-loading="calendarLoading" class="panel calendar-panel">
      <div class="section-head">
        <div>
          <h2>收益日历</h2>
          <span class="muted-text">{{ calendarMonthLabel }} · 日期下直接展示当日收益</span>
        </div>
        <div class="calendar-month-actions">
          <el-button-group>
            <el-button :icon="ArrowLeft" aria-label="上一月" @click="changeCalendarMonth(-1)" />
            <el-button :disabled="isCurrentCalendarMonth" @click="resetCalendarMonth">本月</el-button>
            <el-button :icon="ArrowRight" :disabled="!canGoNextCalendarMonth" aria-label="下一月" @click="changeCalendarMonth(1)" />
          </el-button-group>
        </div>
      </div>
      <div class="calendar-weekdays">
        <span v-for="day in calendarWeekdays" :key="day">{{ day }}</span>
      </div>
      <div class="profit-calendar-grid">
        <div v-for="cell in calendarCells" :key="cell.key" class="calendar-cell" :class="[cell.empty ? 'empty' : '', cell.marketClosed ? 'calendar-closed' : calendarProfitClass(cell.profitAmount)]">
          <template v-if="!cell.empty">
            <div class="calendar-date">{{ formatDay(cell.date) }}</div>
            <div class="calendar-profit">{{ calendarProfitText(cell) }}</div>
            <small>{{ calendarStatusText(cell) }}</small>
          </template>
        </div>
      </div>
    </section>

    <section v-loading="loading" class="panel transactions-panel">
      <div class="section-head">
        <h2>交易记录</h2>
        <span class="muted-text">撤销记录保留展示，但不参与汇总统计</span>
      </div>
      <el-empty v-if="!loading && transactions.length === 0" description="暂无该持仓的交易记录" />
      <el-table v-else :data="pagedTransactions" stripe height="420">
        <el-table-column label="时间" prop="transactionTime" min-width="170" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }"><el-tag round effect="light" size="small" :type="transactionTypeTagType(row.type)">{{ transactionTypeLabel(row.type) }}</el-tag></template>
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
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag round effect="light" size="small" :type="transactionStatusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="备注" min-width="180">
          <template #default="{ row }">{{ row.status === 'REVOKED' ? (row.revokeReason || '已撤销') : (row.note || '-') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" :disabled="row.status === 'REVOKED' || row.status === 'CANCELLED'" @click.stop="handleRevokeTransaction(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="transactions.length > 0" class="table-pagination">
        <el-pagination v-model:current-page="transactionPage" v-model:page-size="transactionPageSize" :page-sizes="transactionPageSizes" :total="transactions.length" layout="total, sizes, prev, pager, next" background />
      </div>
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
          <el-form-item label="实际买入时间"><el-date-picker v-model="tradeForm.transactionTime" type="datetime" class="full-width" /></el-form-item>
          <small class="form-tip">{{ fundConfirmPreviewText }}</small>
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
import { computed, onMounted, reactive, ref, watch } from 'vue';
import type { EChartsOption } from 'echarts';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import { ROUTES } from '@/constants/routes';
import { accountApi, type AccountItem } from '@/services/accountApi';
import { investmentApi, type AssetPriceItem, type FundConfirmPreview, type HoldingDetailSummary, type HoldingItem, type InvestmentCalendarDayProfit, type InvestmentTransactionItem, type InvestmentTransactionType } from '@/services/investmentApi';
import { useThemeStore } from '@/stores/theme';
import { readThemeVar } from '@/utils/theme';

const route = useRoute();
const router = useRouter();
const themeStore = useThemeStore();
const holding = ref<HoldingItem | null>(null);
const summary = ref<HoldingDetailSummary | null>(null);
const transactions = ref<InvestmentTransactionItem[]>([]);
const priceSnapshots = ref<AssetPriceItem[]>([]);
const profitCalendar = ref<InvestmentCalendarDayProfit[]>([]);
const accounts = ref<AccountItem[]>([]);
const transactionPage = ref(1);
const transactionPageSize = ref(10);
const transactionPageSizes = [10, 50, 100, 300];
const loading = ref(false);
const calendarLoading = ref(false);
const submitting = ref(false);
const refreshingQuote = ref(false);
const trendMode = ref<'MARKET_VALUE' | 'PRICE'>('MARKET_VALUE');
const tradeDialogVisible = ref(false);
const quoteDialogVisible = ref(false);
const tradeForm = reactive({ type: 'BUY' as InvestmentTransactionType, accountId: '', quantity: 0, price: 0, tradeAmount: 0, fee: 0, transactionTime: new Date(), note: '' });
const quoteForm = reactive({ price: 0, currency: 'CNY' });
const fundConfirmPreview = ref<FundConfirmPreview | null>(null);
const calendarMonth = ref(startOfMonth(new Date()));
let fundConfirmPreviewSeq = 0;
const trendModeOptions = [
  { label: '总市值', value: 'MARKET_VALUE' },
  { label: '价格', value: 'PRICE' }
];
const calendarWeekdays = ['日', '一', '二', '三', '四', '五', '六'];

const holdingId = computed(() => String(route.params.id || ''));
const currencySymbol = computed(() => (holding.value?.currency === 'USD' ? '$' : '¥'));
const pricePrecision = computed(() => holding.value?.priceScale || (holding.value?.assetType === 'CRYPTO' ? 6 : 4));
const quantityPrecision = computed(() => holding.value?.assetType === 'CRYPTO' ? 10 : 4);
const quantityMin = computed(() => holding.value?.assetType === 'CRYPTO' ? 0.0000000001 : 0.0001);
const isFundAmountBuy = computed(() => holding.value?.assetType === 'FUND' && tradeForm.type === 'BUY');
const fundConfirmPreviewText = computed(() => {
  if (!isFundAmountBuy.value) {
    return '';
  }
  if (!fundConfirmPreview.value) {
    return '基金买入会按确认净值自动反推确认份额；净值未出时先保存为待确认。';
  }
  const preview = fundConfirmPreview.value;
  const prefix = preview.shifted
    ? `${preview.shiftReason || '实际买入时间已顺延'}，将按 ${preview.effectiveTradeDate} 作为申请日`
    : `将按 ${preview.effectiveTradeDate} 作为申请日`;
  return `${prefix}，${preview.qdii ? 'QDII 预计' : '预计'} ${preview.confirmedDate} 确认；净值未出时先保存为待确认。`;
});
// 今日收益缺失时必须让 KPI 显示 --，不能把休市或净值未更新兜底成 0。
const primaryProfitTrend = computed(() => holding.value?.todayProfitRate ?? null);
const todayProfitTone = computed(() => {
  if (holding.value?.todayProfit === null || holding.value?.todayProfit === undefined) {
    return 'warning';
  }
  return profitTone(holding.value.todayProfit);
});
const primaryProfitDescription = computed(() => {
  if (holding.value?.priceStatus === 'MARKET_CLOSED') {
    return '今日休市';
  }
  if (holding.value?.todayPriceAvailable === false) {
    return holding.value?.assetType === 'FUND' ? '今日净值未更新' : '今日价格未更新';
  }
  return holding.value?.priceDate ? `价格日期 ${holding.value.priceDate}` : '今日有效价对比昨价';
});
const calendarMonthLabel = computed(() => `${calendarMonthKey(calendarMonth.value)} 月收益`);
const isCurrentCalendarMonth = computed(() => isSameCalendarMonth(calendarMonth.value, new Date()));
const canGoNextCalendarMonth = computed(() => !isAfterCalendarMonth(addMonths(calendarMonth.value, 1), new Date()));
const pagedTransactions = computed(() => {
  const start = (transactionPage.value - 1) * transactionPageSize.value;
  // 交易记录本地分页，详情接口仍一次返回完整交易，避免本轮扩大后端改动。
  return transactions.value.slice(start, start + transactionPageSize.value);
});
const calendarCells = computed(() => {
  if (!profitCalendar.value.length) {
    return [];
  }
  const firstDate = profitCalendar.value[0]?.date;
  const offset = firstDate ? new Date(`${firstDate}T00:00:00`).getDay() : 0;
  const blanks = Array.from({ length: offset }, (_, index) => ({ key: `blank-${index}`, empty: true, date: '', profitAmount: null, hasPrice: false, marketClosed: false, price: null }));
  // 收益日历保留空白占位，确保日期位置符合自然月日历。
  return [...blanks, ...profitCalendar.value.map((item) => ({ key: item.date, empty: false, ...item }))];
});
const priceChartOption = computed<EChartsOption>(() => {
  const points = [...priceSnapshots.value].reverse();
  const quantity = Number(holding.value?.quantity || 0);
  const isMarketValue = trendMode.value === 'MARKET_VALUE';
  const axisText = readThemeVar('--xo-muted', themeStore.resolvedTheme === 'dark' ? '#94a3b8' : '#6b7280');
  const chartLine = readThemeVar('--xo-primary', '#2563eb');
  return {
    tooltip: { trigger: 'axis', valueFormatter: (value) => formatChartValue(Number(value), isMarketValue ? 4 : pricePrecision.value) },
    grid: { left: 48, right: 24, top: 28, bottom: 42 },
    xAxis: { type: 'category', data: points.map((item) => formatTableTime(item.quoteTime)), axisLabel: { color: axisText } },
    yAxis: { type: 'value', scale: true, axisLabel: { color: axisText, formatter: (value: number) => formatChartAxis(value) } },
    series: [
      {
        name: isMarketValue ? '总市值' : '价格',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        // 当前没有历史持仓数量快照，市值走势使用当前持仓数量乘以历史价格快照。
        data: points.map((item) => roundTo(isMarketValue ? Number(item.price) * quantity : item.price, isMarketValue ? 4 : pricePrecision.value)),
        lineStyle: { width: 3, color: chartLine },
        itemStyle: { color: chartLine },
        areaStyle: { color: 'rgba(37, 99, 235, 0.08)' }
      }
    ]
  };
});

onMounted(() => {
  loadPageData();
});

watch(
  () => [isFundAmountBuy.value, tradeForm.transactionTime, holding.value?.assetId],
  () => {
    loadFundConfirmPreview();
  }
);

watch(transactionPageSize, () => {
  clampTransactionPage();
});

// 加载页面数据。
async function loadPageData() {
  if (!holdingId.value) {
    ElMessage.error('持仓不存在');
    router.replace(ROUTES.investments);
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
    clampTransactionPage();
    priceSnapshots.value = detail.priceSnapshots;
    accounts.value = accountList;
    if (isSameCalendarMonth(calendarMonth.value, new Date())) {
      profitCalendar.value = detail.profitCalendar || [];
    } else {
      await loadProfitCalendar(false);
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '持仓详情加载失败');
  } finally {
    loading.value = false;
  }
}

// 交易记录刷新或撤销后校正页码，避免停留在已经不存在的空页。
function clampTransactionPage() {
  const totalPage = Math.max(1, Math.ceil(transactions.value.length / transactionPageSize.value));
  if (transactionPage.value > totalPage) {
    transactionPage.value = totalPage;
  }
}

// 加载收益日历。
async function loadProfitCalendar(showLoading = true) {
  if (!holdingId.value) {
    return;
  }
  if (showLoading) {
    calendarLoading.value = true;
  }
  try {
    // 收益日历按选中年月单独拉取，左右切换月份时不刷新整页详情。
    profitCalendar.value = await investmentApi.profitCalendar(holdingId.value, {
      year: calendarMonth.value.getFullYear(),
      month: calendarMonth.value.getMonth() + 1
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '收益日历加载失败');
  } finally {
    if (showLoading) {
      calendarLoading.value = false;
    }
  }
}

// 切换收益日历月份。
function changeCalendarMonth(delta: number) {
  const nextMonth = addMonths(calendarMonth.value, delta);
  if (delta > 0 && isAfterCalendarMonth(nextMonth, new Date())) {
    return;
  }
  calendarMonth.value = nextMonth;
  loadProfitCalendar();
}

// 回到当前月份。
function resetCalendarMonth() {
  const currentMonth = startOfMonth(new Date());
  if (isSameCalendarMonth(calendarMonth.value, currentMonth)) {
    return;
  }
  calendarMonth.value = currentMonth;
  loadProfitCalendar();
}

// 返回上一页。
function goBack() {
  // 持仓详情可能从基金 / 股票 / 虚拟货币模块进入，优先返回浏览器上一页以保留用户刚才所在模块。
  if (window.history.length > 1 && window.history.state?.back) {
    router.back();
    return;
  }
  const fromModule = typeof route.query.fromModule === 'string' ? route.query.fromModule : '';
  router.push({
    path: ROUTES.investments,
    query: ['FUND', 'STOCK', 'CRYPTO'].includes(fromModule) ? { module: fromModule } : {}
  });
}

// 打开交易弹窗。
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
  loadFundConfirmPreview();
}

// 加载基金确认预估。
async function loadFundConfirmPreview() {
  if (!isFundAmountBuy.value || !holding.value?.assetId || !(tradeForm.transactionTime instanceof Date)) {
    fundConfirmPreview.value = null;
    return;
  }
  const seq = ++fundConfirmPreviewSeq;
  try {
    const preview = await investmentApi.fundConfirmPreview({
      assetId: holding.value.assetId,
      transactionTime: formatDateTime(tradeForm.transactionTime)
    });
    if (seq === fundConfirmPreviewSeq) {
      fundConfirmPreview.value = preview;
    }
  } catch {
    if (seq === fundConfirmPreviewSeq) {
      fundConfirmPreview.value = null;
    }
  }
}

// 打开价格弹窗。
function openQuoteDialog() {
  if (!holding.value) {
    return;
  }
  quoteForm.price = roundTo(Number(holding.value.latestPrice || holding.value.avgCost || 0), pricePrecision.value);
  quoteForm.currency = holding.value.currency || 'CNY';
  quoteDialogVisible.value = true;
}

// 创建投资交易。
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

// 保存手动价格。
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

// 刷新行情。
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

// 撤销投资交易。
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

// 格式化日期时间。
function formatDateTime(date: Date) {
  const pad = (value: number) => `${value}`.padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

// 获取月份开始日期。
function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

// 增减月份。
function addMonths(date: Date, delta: number) {
  return new Date(date.getFullYear(), date.getMonth() + delta, 1);
}

// 生成日历月份键。
function calendarMonthKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

// 判断是否同月。
function isSameCalendarMonth(left: Date, right: Date) {
  return left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth();
}

// 判断是否晚于指定月份。
function isAfterCalendarMonth(left: Date, right: Date) {
  return left.getFullYear() > right.getFullYear() || (left.getFullYear() === right.getFullYear() && left.getMonth() > right.getMonth());
}

// 格式化表格时间。
function formatTableTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '';
}

// 格式化数量。
function formatQuantity(value: number) {
  return roundTo(Number(value), quantityPrecision.value).toLocaleString('zh-CN', { minimumFractionDigits: holding.value?.assetType === 'CRYPTO' ? 0 : 4, maximumFractionDigits: quantityPrecision.value });
}

// 转换交易类型文案。
function transactionTypeLabel(type?: string | null) {
  return type === 'SELL' ? '卖出' : '买入';
}

// 交易类型两处交易表统一颜色：买入偏收益色，卖出偏提醒色。
function transactionTypeTagType(type?: string | null) {
  return type === 'SELL' ? 'warning' : 'success';
}

// 转换状态文案。
function statusLabel(status?: string | null) {
  return ({ NORMAL: '正常', CONFIRMED: '已确认', PENDING_CONFIRM: '待确认', FAILED: '确认失败', CANCELLED: '已取消', REVOKED: '已撤销' } as Record<string, string>)[status || ''] || '正常';
}

// 交易状态颜色和投资持仓页保持一致。
function transactionStatusTagType(status?: string | null) {
  return ({ NORMAL: 'success', CONFIRMED: 'success', PENDING_CONFIRM: 'warning', FAILED: 'danger', CANCELLED: 'info', REVOKED: 'info' } as Record<string, string>)[status || ''] || 'success';
}

// 格式化确认信息。
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

// 格式化日期。
function formatDay(value?: string | null) {
  return value ? String(Number(value.slice(8, 10))) : '';
}

// 格式化价格。
function formatPrice(value?: number | null) {
  if (value === null || value === undefined) {
    return '--';
  }
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: Math.min(pricePrecision.value, 4), maximumFractionDigits: pricePrecision.value });
}

// 格式化带符号金额。
function formatSignedAmount(value: number) {
  const prefix = value >= 0 ? '+' : '-';
  return `${prefix}${currencySymbol.value}${Math.abs(round4(value)).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

// 生成日历收益样式。
function calendarProfitClass(value?: number | null) {
  if (value === null || value === undefined) {
    return 'calendar-muted';
  }
  return value >= 0 ? 'calendar-positive' : 'calendar-negative';
}

// 生成日历收益文案。
function calendarProfitText(cell: InvestmentCalendarDayProfit) {
  if (cell.marketClosed) {
    return '休市';
  }
  return cell.profitAmount === null || cell.profitAmount === undefined ? '--' : formatSignedAmount(cell.profitAmount);
}

// 生成日历状态文案。
function calendarStatusText(cell: InvestmentCalendarDayProfit) {
  if (cell.marketClosed) {
    return cell.statusLabel || '休市';
  }
  return cell.hasPrice ? `${holding.value?.priceLabel || '价格'} ${formatPrice(cell.price)}` : (cell.statusLabel || '无价格');
}

// 格式化回本涨跌幅。
function formatBreakEven(value: number | null | undefined) {
  // 回本涨幅只在亏损时展示需要上涨比例；盈利或打平展示已盈利。
  if (value === null || value === undefined) {
    return '暂无';
  }
  return Number(value) > 0 ? `还需 +${round4(value).toFixed(4)}%` : '已盈利';
}

// 生成回本涨跌幅样式。
function breakEvenClass(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return 'muted-text';
  }
  return Number(value) > 0 ? 'warning-text' : 'success-text';
}

// 计算收益颜色语义。
function profitTone(value: number): 'success' | 'danger' | 'primary' {
  if (value > 0) {
    return 'success';
  }
  if (value < 0) {
    return 'danger';
  }
  return 'primary';
}

// 格式化展示文案。
function formatChartAxis(value: number) {
  const abs = Math.abs(value);
  if (abs >= 10000) {
    return `${currencySymbol.value}${round4(value / 10000)}万`;
  }
  return `${currencySymbol.value}${round4(value)}`;
}

// 格式化展示文案。
function formatChartValue(value: number, precision: number) {
  return `${currencySymbol.value}${roundTo(value, precision).toLocaleString('zh-CN', { minimumFractionDigits: precision > 4 ? 4 : 2, maximumFractionDigits: precision })}`;
}

// 保留四位小数。
function round4(value: number) {
  return roundTo(value, 4);
}

// 按资产类型处理数量精度。
function roundQuantity(value: number) {
  return roundTo(value, quantityPrecision.value);
}

// 按指定精度取值。
function roundTo(value: number, precision: number) {
  return Number(Number(value || 0).toFixed(precision));
}
</script>

<style scoped>
/* 详情页延续投资模块的玻璃面板、表格和数字排版风格。 */
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.break-even-card,
.break-even-card {
  min-height: 154px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
  overflow: hidden;
}

.break-even-card::after {
  position: absolute;
  inset: auto -28px -42px auto;
  width: 110px;
  height: 110px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  content: "";
}

.break-even-card span,
.break-even-card small {
  color: var(--xo-muted);
}

.break-even-card strong {
  font-size: 28px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.chart-panel,
.calendar-panel,
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

.calendar-month-actions {
  display: flex;
  justify-content: flex-end;
  min-width: max-content;
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

.metric-extra-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--xo-muted);
  font-size: 13px;
  font-weight: 500;
}

.metric-empty-extra {
  display: block;
  min-height: 18px;
}

.metric-text-extra {
  color: var(--xo-muted);
  font-size: 13px;
  font-weight: 500;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 18px 18px;
}

.calendar-weekdays,
.profit-calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  padding: 0 18px;
}

.calendar-weekdays {
  margin-top: 8px;
  color: var(--xo-muted);
  font-size: 13px;
  text-align: center;
}

.profit-calendar-grid {
  padding-bottom: 18px;
}

.calendar-cell {
  min-height: 86px;
  padding: 10px;
  border: 1px solid var(--xo-border);
  border-radius: 16px;
  background: var(--xo-card);
  box-sizing: border-box;
}

.calendar-cell.empty {
  border-color: transparent;
  background: transparent;
}

.calendar-date {
  color: var(--xo-text);
  font-weight: 800;
}

.calendar-profit {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.calendar-cell small {
  display: block;
  margin-top: 6px;
  color: var(--xo-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.calendar-positive {
  background: rgba(16, 185, 129, 0.08);
}

.calendar-positive .calendar-profit {
  color: var(--xo-success);
}

.calendar-negative {
  background: rgba(239, 68, 68, 0.08);
}

.calendar-negative .calendar-profit {
  color: var(--xo-danger);
}

.calendar-muted .calendar-profit {
  color: var(--xo-muted);
}

.calendar-closed {
  border-style: dashed;
  background: var(--xo-input-muted);
}

.calendar-closed .calendar-profit,
.calendar-closed small {
  color: var(--xo-muted);
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
  .section-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .calendar-month-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
