<!-- 投资主页：总览拆分基金、股票、虚拟货币收益口径，避免把净值型资产硬塞到今日收益。 -->
<template>
  <div class="page investments-overview-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">投资持仓</h1>
        <p class="page-subtitle">总览看全局，基金 / 股票 / 虚拟货币按各自收益规则展示</p>
      </div>
      <div class="header-actions">
        <el-select v-model="displayCurrency" class="currency-select" placeholder="展示币种">
          <el-option v-for="item in currencyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="$router.push(ROUTES.investmentDetails)">投资明细</el-button>
      </div>
    </div>

    <el-tabs v-model="activeModule" class="investment-module-tabs" @tab-change="handleModuleChange">
      <el-tab-pane v-for="item in moduleTabs" :key="item.value" :label="item.label" :name="item.value" />
    </el-tabs>

    <template v-if="activeModule === 'ALL'">
      <section v-loading="loading" class="grid-4">
        <MetricCard title="投资总资产" :value="overview?.totalInvestmentAsset || totalMarketValue" :trend="overview?.holdingProfitRate || totalProfitRate" description="基金 + 股票 + 虚拟货币" tone="primary" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard title="持有收益" :value="overview?.holdingProfit || totalProfit" :trend="overview?.holdingProfitRate || totalProfitRate" description="全部资产当前市值 - 总成本" :tone="profitTone(overview?.holdingProfit || totalProfit)" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard title="今日收益" :value="overview?.todayProfit || 0" :trend="0" :description="overview?.todayProfitAssetScope || '股票 / ETF / 虚拟货币'" :tone="profitTone(overview?.todayProfit || 0)" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard title="昨日收益" :value="overview?.yesterdayProfit || 0" :trend="0" :description="overview?.yesterdayProfitAssetScope || '场外基金 / 债基 / QDII / 货币基金'" :tone="profitTone(overview?.yesterdayProfit || 0)" :precision="4" :currency-symbol="currencySymbol" />
      </section>

      <section v-loading="loading" class="module-card-grid">
        <div v-for="item in moduleAssets" :key="item.module" class="module-card panel panel-padding" @click="switchModule(item.module)">
          <div class="module-card-top">
            <span>{{ item.name }}</span>
            <el-tag round>{{ formatRatio(item.assetRatio) }}</el-tag>
          </div>
          <strong>{{ formatMoney(convertAmount(item.assetAmount, 'CNY')) }}</strong>
          <div class="module-card-meta">
            <span>{{ item.primaryProfitLabel }}</span>
            <AmountText :value="convertAmount(item.primaryProfitAmount, 'CNY')" with-sign :precision="4" :currency-symbol="currencySymbol" />
          </div>
          <div class="module-card-meta muted-text">
            <span>持有收益</span>
            <AmountText :value="convertAmount(item.holdingProfit, 'CNY')" with-sign :precision="4" :currency-symbol="currencySymbol" />
          </div>
        </div>
      </section>

      <section class="grid-2">
        <div class="panel panel-padding">
          <div class="panel-head">
            <h3>资产趋势</h3>
            <el-segmented v-model="trendModule" :options="moduleTabs" @change="loadTrend" />
          </div>
          <el-empty v-if="!loading && investmentTrend.length === 0" description="暂无投资资产曲线数据" />
          <BaseChart v-else :option="investmentTrendOption" />
        </div>
        <div class="panel panel-padding">
          <div class="panel-head">
            <h3>投资分布</h3>
          </div>
          <el-empty v-if="!loading && holdings.length === 0" description="暂无投资分布数据" />
          <BaseChart v-else :option="allocationOption" />
        </div>
      </section>

      <section class="panel panel-padding contribution-panel">
        <div class="panel-head">
          <div>
            <h3>收益贡献</h3>
            <p class="panel-subtitle">主收益模式按后端 primaryProfitLabel 计算，不在前端硬套今日收益</p>
          </div>
          <el-segmented v-model="contributionMode" :options="contributionOptions" />
        </div>
        <el-empty v-if="!loading && contributionData.length === 0" description="暂无收益贡献数据" />
        <BaseChart v-else :option="profitOption" height="320px" />
      </section>
    </template>

    <template v-else>
      <section v-loading="loading" class="grid-3 module-summary-grid">
        <MetricCard :title="`${moduleLabel(activeModule)}总资产`" :value="currentModuleAsset?.assetAmount || 0" :trend="currentModuleAsset?.assetRatio || 0" description="当前模块持仓市值" tone="primary" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard :title="currentModuleAsset?.primaryProfitLabel || modulePrimaryLabel(activeModule)" :value="currentModuleAsset?.primaryProfitAmount || 0" :trend="0" description="按模块收益口径展示" :tone="profitTone(currentModuleAsset?.primaryProfitAmount || 0)" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard title="持有收益" :value="currentModuleAsset?.holdingProfit || 0" :trend="currentModuleAsset?.holdingProfitRate || 0" description="当前市值 - 持仓成本" :tone="profitTone(currentModuleAsset?.holdingProfit || 0)" :precision="4" :currency-symbol="currencySymbol" />
      </section>

      <section class="panel panel-padding module-holdings-panel">
        <div class="panel-head module-panel-head">
          <div>
            <h3>{{ moduleLabel(activeModule) }}持仓</h3>
            <p class="panel-subtitle">主收益字段统一展示 primaryProfitLabel + primaryProfitAmount</p>
          </div>
          <el-segmented v-model="activeSubType" :options="subTypeOptions" />
        </div>
        <el-table :data="filteredModuleHoldings" stripe>
          <el-table-column label="名称" min-width="220">
            <template #default="{ row }">
              <div class="holding-name-cell">
                <strong>{{ row.assetName || row.symbol || '-' }}</strong>
                <span>{{ row.symbol || '-' }} · {{ subTypeLabel(row.assetSubType) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="持有市值" min-width="140" align="right" header-align="right">
            <template #default="{ row }"><AmountText :value="convertAmount(row.marketValue, row.currency)" :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column :label="activeModule === 'FUND' ? '昨日/今日收益' : modulePrimaryLabel(activeModule)" min-width="160" align="right" header-align="right">
            <template #default="{ row }">
              <div class="primary-profit-cell">
                <small>{{ row.primaryProfitLabel || modulePrimaryLabel(activeModule) }}</small>
                <AmountText v-if="row.primaryProfitAmount !== null && row.primaryProfitAmount !== undefined" :value="convertAmount(row.primaryProfitAmount, row.currency)" with-sign :precision="4" :currency-symbol="currencySymbol" />
                <span v-else class="muted-text">--</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="持有收益" min-width="160" align="right" header-align="right">
            <template #default="{ row }"><AmountText :value="convertAmount(row.floatingProfit, row.currency)" with-sign :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column :label="modulePriceLabel(activeModule)" min-width="130" align="right" header-align="right">
            <template #default="{ row }">{{ formatPrice(row.latestPrice, row.priceScale) }}</template>
          </el-table-column>
          <el-table-column label="价格日期" min-width="120" align="right" header-align="right">
            <template #default="{ row }">{{ row.priceDate || '--' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="130" align="center">
            <template #default="{ row }"><el-tag round :type="row.todayPriceAvailable === false && row.profitDisplayMode === 'TODAY' ? 'warning' : 'success'">{{ priceStatusLabel(row) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }"><el-button link type="primary" @click="$router.push(ROUTES.holdingDetail.replace(':id', row.id))">收益日历</el-button></template>
          </el-table-column>
        </el-table>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
// Web 投资页只负责模块化展示；交易录入和持仓维护仍集中到投资明细页。
import { computed, onMounted, ref, watch } from 'vue';
import type { EChartsOption } from 'echarts';
import { ElMessage } from 'element-plus';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import { ROUTES } from '@/constants/routes';
import { exchangeRateApi } from '@/services/exchangeRateApi';
import { investmentApi, type AssetType, type HoldingItem, type InvestmentModuleAsset, type InvestmentOverview, type InvestmentTransactionItem, type InvestmentTrendPoint } from '@/services/investmentApi';

type DisplayCurrency = 'CNY' | 'USD';
type ContributionMode = 'TOTAL' | 'PRIMARY' | 'MONTH' | 'YEAR';
type InvestmentModule = 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO';

const moduleTabs: Array<{ label: string; value: InvestmentModule }> = [
  { label: '总览', value: 'ALL' },
  { label: '基金', value: 'FUND' },
  { label: '股票', value: 'STOCK' },
  { label: '虚拟货币', value: 'CRYPTO' }
];
const currencyOptions = [
  { label: '人民币', value: 'CNY' },
  { label: 'USD', value: 'USD' }
];
const contributionOptions = [
  { label: '持有', value: 'TOTAL' },
  { label: '主收益', value: 'PRIMARY' },
  { label: '当月', value: 'MONTH' },
  { label: '当年', value: 'YEAR' }
];

const overview = ref<InvestmentOverview | null>(null);
const holdings = ref<HoldingItem[]>([]);
const moduleHoldings = ref<HoldingItem[]>([]);
const investmentTrend = ref<InvestmentTrendPoint[]>([]);
const transactions = ref<InvestmentTransactionItem[]>([]);
const loading = ref(false);
const displayCurrency = ref<DisplayCurrency>('CNY');
const activeModule = ref<InvestmentModule>('ALL');
const trendModule = ref<InvestmentModule>('ALL');
const activeSubType = ref('ALL');
const contributionMode = ref<ContributionMode>('TOTAL');
const usdCnyRate = ref(7.2);

onMounted(() => {
  loadPageData();
  loadExchangeRate();
});

watch(activeModule, () => {
  activeSubType.value = 'ALL';
});

const currencySymbol = computed(() => (displayCurrency.value === 'CNY' ? '¥' : '$'));
const totalMarketValue = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.marketValue, item.currency), 0)));
const totalCost = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.totalCost, item.currency), 0)));
const totalProfit = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.floatingProfit, item.currency), 0)));
const totalProfitRate = computed(() => rate4(totalProfit.value, totalCost.value));
const moduleAssets = computed(() => overview.value?.moduleAssets || fallbackModuleAssets.value);
const currentModuleAsset = computed(() => moduleAssets.value.find((item) => item.module === activeModule.value) || null);
const fallbackModuleAssets = computed<InvestmentModuleAsset[]>(() => (['FUND', 'STOCK', 'CRYPTO'] as const).map((module) => {
  const items = holdings.value.filter((item) => item.assetType === module);
  const assetAmount = round4(items.reduce((sum, item) => sum + convertAmount(item.marketValue, item.currency), 0));
  const cost = round4(items.reduce((sum, item) => sum + convertAmount(item.totalCost, item.currency), 0));
  const holdingProfit = round4(items.reduce((sum, item) => sum + convertAmount(item.floatingProfit, item.currency), 0));
  return {
    module,
    name: moduleLabel(module),
    assetAmount,
    assetRatio: totalMarketValue.value > 0 ? round4((assetAmount / totalMarketValue.value) * 100) : 0,
    primaryProfitLabel: modulePrimaryLabel(module),
    primaryProfitAmount: round4(items.reduce((sum, item) => sum + convertAmount(item.primaryProfitAmount || 0, item.currency), 0)),
    holdingProfit,
    holdingProfitRate: rate4(holdingProfit, cost),
    holdingCount: items.length
  };
}));
const subTypeOptions = computed(() => {
  if (activeModule.value === 'FUND') {
    return [
      { label: '全部', value: 'ALL' },
      { label: '场外基金', value: 'OTC_FUND' },
      { label: '货币基金', value: 'MONEY_FUND' },
      { label: '债券基金', value: 'BOND_FUND' },
      { label: 'QDII', value: 'QDII_FUND' },
      { label: 'ETF', value: 'ETF' }
    ];
  }
  if (activeModule.value === 'STOCK') {
    return [
      { label: '全部', value: 'ALL' },
      { label: 'A股', value: 'CN_STOCK' },
      { label: '港股', value: 'HK_STOCK' },
      { label: '美股', value: 'US_STOCK' },
      { label: 'ETF', value: 'ETF' }
    ];
  }
  return [
    { label: '全部', value: 'ALL' },
    { label: '现货', value: 'CRYPTO_SPOT' }
  ];
});
const filteredModuleHoldings = computed(() => {
  const rows = moduleHoldings.value.length ? moduleHoldings.value : holdings.value.filter((item) => item.assetType === activeModule.value);
  return activeSubType.value === 'ALL' ? rows : rows.filter((item) => item.assetSubType === activeSubType.value);
});
const allocationOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#2dd4bf', '#8b5cf6', '#f6c453', '#fb7185', '#60a5fa', '#a78bfa'],
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['45%', '72%'],
    data: moduleAssets.value.map((item) => ({ name: item.name, value: round4(convertAmount(item.assetAmount, 'CNY')) })).filter((item) => item.value > 0)
  }]
}));
const investmentTrendOption = computed<EChartsOption>(() => ({
  grid: { left: 54, right: 18, top: 24, bottom: 36 },
  tooltip: { trigger: 'axis', valueFormatter: (value) => formatMoney(Number(value)) },
  xAxis: { type: 'category', data: investmentTrend.value.map((item) => item.date), axisLine: { lineStyle: { color: '#e2e8f0' } } },
  yAxis: { type: 'value', axisLabel: { formatter: (value: number) => compactMoney(value) }, splitLine: { lineStyle: { color: '#e2e8f0' } } },
  series: [{
    name: moduleLabel(trendModule.value),
    type: 'line',
    smooth: true,
    symbolSize: 6,
    data: investmentTrend.value.map((item) => round4(convertAmount(item.assetAmount ?? item.marketValue, 'CNY'))),
    lineStyle: { color: '#2563eb', width: 3 },
    itemStyle: { color: '#2563eb' },
    areaStyle: { color: 'rgba(37, 99, 235, 0.12)' }
  }]
}));
const profitOption = computed<EChartsOption>(() => ({
  grid: { left: 50, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: contributionData.value.map((item) => item.name), axisLabel: { interval: 0, rotate: contributionData.value.length > 6 ? 25 : 0 } },
  yAxis: { type: 'value', axisLabel: { formatter: (value: number) => compactMoney(value) } },
  tooltip: { trigger: 'axis', valueFormatter: (value) => formatMoney(Number(value)) },
  series: [{ type: 'bar', data: contributionData.value.map((item) => item.value), itemStyle: { color: '#3b82f6', borderRadius: [10, 10, 0, 0] } }]
}));
const contributionData = computed(() => contributionRows()
  .map((item) => ({ name: item.name, value: round4(item.value) }))
  .filter((item) => item.value !== 0)
  .sort((a, b) => Math.abs(b.value) - Math.abs(a.value)));

async function loadPageData() {
  loading.value = true;
  try {
    const [overviewResult, holdingList, trendResult, transactionList] = await Promise.all([
      investmentApi.overviewInvestments(),
      investmentApi.listInvestmentHoldings({ module: 'ALL' }),
      investmentApi.trendInvestments({ module: trendModule.value, period: 'MONTH' }),
      investmentApi.listTransactions()
    ]);
    overview.value = overviewResult;
    holdings.value = holdingList;
    moduleHoldings.value = activeModule.value === 'ALL' ? [] : await investmentApi.listInvestmentHoldings({ module: activeModule.value });
    investmentTrend.value = trendResult.points || [];
    transactions.value = transactionList;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '投资数据加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadTrend() {
  try {
    const result = await investmentApi.trendInvestments({ module: trendModule.value, period: 'MONTH' });
    investmentTrend.value = result.points || [];
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '趋势加载失败');
  }
}

async function handleModuleChange() {
  if (activeModule.value === 'ALL') {
    moduleHoldings.value = [];
    return;
  }
  loading.value = true;
  try {
    moduleHoldings.value = await investmentApi.listInvestmentHoldings({ module: activeModule.value });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '模块持仓加载失败');
  } finally {
    loading.value = false;
  }
}

function switchModule(module: string) {
  activeModule.value = module as InvestmentModule;
  handleModuleChange();
}

async function loadExchangeRate() {
  try {
    const result = await exchangeRateApi.usdCny();
    usdCnyRate.value = Number(result.rate || usdCnyRate.value);
  } catch {
    // 汇率接口失败时保留默认值，避免影响投资页主体展示。
  }
}

function contributionRows() {
  if (contributionMode.value === 'PRIMARY') {
    return holdings.value.map((item) => ({ name: item.assetName || item.symbol || '-', value: convertAmount(Number(item.primaryProfitAmount || 0), item.currency) }));
  }
  if (contributionMode.value === 'MONTH' || contributionMode.value === 'YEAR') {
    return realizedContributionRows(contributionMode.value);
  }
  return holdings.value.map((item) => ({ name: item.assetName || item.symbol || '-', value: convertAmount(Number(item.floatingProfit || 0), item.currency) }));
}

function realizedContributionRows(mode: Extract<ContributionMode, 'MONTH' | 'YEAR'>) {
  const begin = periodStart(mode);
  const holdingMap = new Map(holdings.value.map((item) => [item.assetId, item]));
  const result = new Map<string, number>();
  transactions.value
    .filter((item) => item.status !== 'REVOKED' && item.type === 'SELL' && item.realizedProfit !== null && item.realizedProfit !== undefined)
    .filter((item) => new Date(item.transactionTime).getTime() >= begin.getTime())
    .forEach((item) => {
      const holding = holdingMap.get(item.assetId);
      const name = item.assetName || holding?.assetName || item.symbol || '-';
      // 月/年贡献当前只统计已实现卖出盈亏，不用缺失的历史市值曲线冒充周期贡献。
      const value = convertAmount(Number(item.realizedProfit || 0), holding?.currency || 'CNY');
      result.set(name, round4((result.get(name) || 0) + value));
    });
  return [...result.entries()].map(([name, value]) => ({ name, value }));
}

function convertAmount(value: number, sourceCurrency?: string | null) {
  const source = sourceCurrency || 'CNY';
  if (source === displayCurrency.value) return round4(Number(value));
  if (source === 'USD' && displayCurrency.value === 'CNY') return round4(Number(value) * usdCnyRate.value);
  if (source === 'CNY' && displayCurrency.value === 'USD') return round4(Number(value) / usdCnyRate.value);
  return round4(Number(value));
}

function rate4(profit: number, cost: number) {
  return cost <= 0 ? 0 : round4((profit / cost) * 100);
}

function round4(value: number) {
  return Number(Number(value || 0).toFixed(4));
}

function compactMoney(value: number) {
  const abs = Math.abs(value);
  if (abs >= 10000) return `${currencySymbol.value}${round4(value / 10000)}万`;
  return `${currencySymbol.value}${round4(value)}`;
}

function formatMoney(value: number) {
  return `${currencySymbol.value}${round4(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })}`;
}

function formatRatio(value?: number | null) {
  return `${round4(Number(value || 0)).toFixed(2)}%`;
}

function formatPrice(value?: number | null, scale?: number | null) {
  if (value === null || value === undefined) return '--';
  const precision = scale || 4;
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: Math.min(precision, 4), maximumFractionDigits: precision });
}

function periodStart(mode: Extract<ContributionMode, 'MONTH' | 'YEAR'>) {
  const now = new Date();
  return mode === 'MONTH' ? new Date(now.getFullYear(), now.getMonth(), 1) : new Date(now.getFullYear(), 0, 1);
}

function moduleLabel(module?: string | null) {
  return ({ ALL: '总览', FUND: '基金', STOCK: '股票', CRYPTO: '虚拟货币' } as Record<string, string>)[module || ''] || '-';
}

function modulePrimaryLabel(module?: string | null) {
  return module === 'FUND' ? '昨日收益' : module === 'CRYPTO' ? '24h收益' : '今日收益';
}

function modulePriceLabel(module?: string | null) {
  return module === 'FUND' ? '最新净值' : '当前价';
}

function subTypeLabel(value?: string | null) {
  return ({ OTC_FUND: '场外基金', MONEY_FUND: '货币基金', BOND_FUND: '债券基金', QDII_FUND: 'QDII', ETF: 'ETF', CN_STOCK: 'A股', HK_STOCK: '港股', US_STOCK: '美股', CRYPTO_SPOT: '现货' } as Record<string, string>)[value || ''] || '-';
}

function priceStatusLabel(row: HoldingItem) {
  if (row.profitDisplayMode === 'YESTERDAY') return row.priceDate ? `${row.priceLabel || '净值'} ${row.priceDate}` : '净值未更新';
  return row.todayPriceAvailable === false ? '今日价未更新' : '正常';
}

function profitTone(value: number): 'success' | 'danger' | 'primary' {
  if (value > 0) return 'success';
  if (value < 0) return 'danger';
  return 'primary';
}
</script>

<style scoped>
/* 投资总览采用项目 xo-design 卡片风格，模块卡保留轻量可点击入口。 */
.investment-module-tabs {
  margin-bottom: 18px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.panel-subtitle {
  margin: 4px 0 0;
  color: var(--xo-muted);
  font-size: 13px;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.currency-select {
  width: 120px;
}

.module-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin: 20px 0;
}

.module-card {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.module-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--xo-shadow-lg);
}

.module-card-top,
.module-card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.module-card-top span {
  font-size: 15px;
  color: var(--xo-muted);
}

.module-card strong {
  display: block;
  margin: 12px 0 16px;
  font-size: 28px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.module-card-meta {
  margin-top: 8px;
  font-size: 13px;
}

.module-summary-grid {
  margin-bottom: 20px;
}

.module-holdings-panel {
  overflow: hidden;
}

.module-panel-head {
  align-items: flex-start;
}

.holding-name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.holding-name-cell strong {
  color: var(--xo-text);
}

.holding-name-cell span,
.primary-profit-cell small,
.muted-text {
  color: var(--xo-muted);
}

.primary-profit-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.contribution-panel {
  margin-top: 24px;
}

@media (max-width: 1080px) {
  .module-card-grid,
  .module-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
