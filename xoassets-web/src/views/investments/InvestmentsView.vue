<!-- 投资主页：只展示投资统计和分布，持仓明细与操作集中到投资明细页。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">投资持仓</h1>
        <p class="page-subtitle">按基金、股票和虚拟货币查看投资规模与收益</p>
      </div>
      <div class="header-actions">
        <el-select v-model="displayCurrency" class="currency-select" placeholder="展示币种">
          <el-option v-for="item in currencyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="$router.push(ROUTES.investmentDetails)">投资明细</el-button>
      </div>
    </div>

    <section class="grid-4">
      <MetricCard title="总投资" :value="totalMarketValue" :trend="totalProfitRate" description="全部持仓市值" :tone="totalProfit >= 0 ? 'success' : 'danger'" :precision="4" :currency-symbol="currencySymbol" />
      <MetricCard title="基金" :value="typeStats.FUND.marketValue" :trend="typeStats.FUND.profitRate" description="基金持仓" tone="primary" :precision="4" :currency-symbol="currencySymbol" />
      <MetricCard title="股票" :value="typeStats.STOCK.marketValue" :trend="typeStats.STOCK.profitRate" description="股票持仓" tone="warning" :precision="4" :currency-symbol="currencySymbol" />
      <MetricCard title="虚拟货币" :value="typeStats.CRYPTO.marketValue" :trend="typeStats.CRYPTO.profitRate" description="币种持仓" tone="success" :precision="4" :currency-symbol="currencySymbol" />
    </section>

    <section class="grid-2">
      <div class="panel panel-padding">
        <div class="panel-head">
          <h3>投资分布</h3>
        </div>
        <el-empty v-if="!loading && holdings.length === 0" description="暂无投资分布数据" />
        <BaseChart v-else :option="allocationOption" />
      </div>
      <div class="panel panel-padding">
        <div class="panel-head">
          <h3>总投资资产曲线</h3>
        </div>
        <el-empty v-if="!loading && investmentTrend.length === 0" description="暂无投资资产曲线数据" />
        <BaseChart v-else :option="investmentTrendOption" />
      </div>
    </section>

    <section class="panel panel-padding contribution-panel">
      <div class="panel-head">
        <h3>收益贡献</h3>
        <el-segmented v-model="contributionMode" :options="contributionOptions" />
      </div>
      <el-empty v-if="!loading && contributionData.length === 0" description="暂无收益贡献数据" />
      <BaseChart v-else :option="profitOption" height="320px" />
    </section>
  </div>
</template>

<script setup lang="ts">
// 主页只做聚合展示；所有持仓增删改和交易操作放在明细页，减少首页干扰。
import { computed, onMounted, ref } from 'vue';
import type { EChartsOption } from 'echarts';
import { ElMessage } from 'element-plus';
import BaseChart from '@/components/charts/BaseChart.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import { ROUTES } from '@/constants/routes';
import { exchangeRateApi } from '@/services/exchangeRateApi';
import { investmentApi, type AssetType, type HoldingItem, type InvestmentTransactionItem } from '@/services/investmentApi';
import { snapshotApi, type AssetSnapshotItem } from '@/services/snapshotApi';

type DisplayCurrency = 'CNY' | 'USD';
type ContributionMode = 'TOTAL' | 'DAY' | 'MONTH' | 'YEAR';

const holdings = ref<HoldingItem[]>([]);
const investmentTrend = ref<AssetSnapshotItem[]>([]);
const transactions = ref<InvestmentTransactionItem[]>([]);
const loading = ref(false);
const displayCurrency = ref<DisplayCurrency>('CNY');
const contributionMode = ref<ContributionMode>('TOTAL');
const usdCnyRate = ref(7.2);
const currencyOptions = [
  { label: '人民币', value: 'CNY' },
  { label: 'USD', value: 'USD' }
];
const contributionOptions = [
  { label: '总', value: 'TOTAL' },
  { label: '当日', value: 'DAY' },
  { label: '当月', value: 'MONTH' },
  { label: '当年', value: 'YEAR' }
];

onMounted(() => {
  loadHoldings();
  loadExchangeRate();
});

const currencySymbol = computed(() => (displayCurrency.value === 'CNY' ? '¥' : '$'));
const totalMarketValue = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.marketValue, item.currency), 0)));
const totalCost = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.totalCost, item.currency), 0)));
const totalProfit = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.floatingProfit, item.currency), 0)));
const totalProfitRate = computed(() => rate4(totalProfit.value, totalCost.value));
const typeStats = computed(() => ({
  FUND: calcTypeStat('FUND'),
  STOCK: calcTypeStat('STOCK'),
  CRYPTO: calcTypeStat('CRYPTO')
}));
const allocationOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#2dd4bf', '#8b5cf6', '#f6c453', '#fb7185', '#60a5fa', '#a78bfa'],
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['45%', '72%'],
    // 投资分布按具体产品展示，避免只看到基金/股票/币的大类桶。
    data: holdings.value
      .map((item) => ({ name: item.assetName || item.symbol || typeLabel(item.assetType), value: round4(convertAmount(item.marketValue, item.currency)) }))
      .filter((item) => item.value > 0)
  }]
}));
const investmentTrendOption = computed<EChartsOption>(() => ({
  grid: { left: 54, right: 18, top: 24, bottom: 36 },
  tooltip: {
    trigger: 'axis',
    valueFormatter: (value) => formatMoney(Number(value))
  },
  xAxis: { type: 'category', data: investmentTrend.value.map((item) => item.snapshotDate), axisLine: { lineStyle: { color: '#e2e8f0' } } },
  yAxis: { type: 'value', axisLabel: { formatter: (value: number) => compactMoney(value) }, splitLine: { lineStyle: { color: '#e2e8f0' } } },
  series: [{
    name: '投资资产',
    type: 'line',
    smooth: true,
    symbolSize: 6,
    data: investmentTrend.value.map((item) => round4(convertAmount(item.investmentAsset, 'CNY'))),
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
const contributionData = computed(() => {
  const rows = contributionRows();
  return rows
    .map((item) => ({ name: item.name, value: round4(item.value) }))
    .filter((item) => item.value !== 0)
    .sort((a, b) => Math.abs(b.value) - Math.abs(a.value));
});

async function loadHoldings() {
  loading.value = true;
  try {
    const [holdingList, trendList, transactionList] = await Promise.all([
      investmentApi.listHoldings(),
      snapshotApi.trend({ startDate: dateBefore(89), endDate: dateBefore(0) }),
      investmentApi.listTransactions()
    ]);
    holdings.value = holdingList;
    investmentTrend.value = trendList;
    transactions.value = transactionList;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '持仓加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadExchangeRate() {
  try {
    const result = await exchangeRateApi.usdCny();
    usdCnyRate.value = Number(result.rate || usdCnyRate.value);
  } catch {
    // 汇率接口失败时保留默认值，币种切换仍可用。
  }
}

function calcTypeStat(type: AssetType) {
  const items = holdings.value.filter((item) => item.assetType === type);
  const marketValue = round4(items.reduce((sum, item) => sum + convertAmount(item.marketValue, item.currency), 0));
  const cost = round4(items.reduce((sum, item) => sum + convertAmount(item.totalCost, item.currency), 0));
  const profit = round4(items.reduce((sum, item) => sum + convertAmount(item.floatingProfit, item.currency), 0));
  return { marketValue, profitRate: rate4(profit, cost) };
}

function contributionRows() {
  if (contributionMode.value === 'DAY') {
    return holdings.value.map((item) => ({
      name: item.assetName || item.symbol || '-',
      value: convertAmount(Number(item.todayProfit || 0), item.currency)
    }));
  }
  if (contributionMode.value === 'MONTH' || contributionMode.value === 'YEAR') {
    return realizedContributionRows(contributionMode.value);
  }
  return holdings.value.map((item) => ({
    name: item.assetName || item.symbol || '-',
    value: convertAmount(Number(item.floatingProfit || 0), item.currency)
  }));
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
      // 月/年维度当前没有逐产品历史市值序列，先用卖出交易的已实现盈亏作为周期收益贡献。
      const value = convertAmount(Number(item.realizedProfit || 0), holding?.currency || 'CNY');
      result.set(name, round4((result.get(name) || 0) + value));
    });
  return [...result.entries()].map(([name, value]) => ({ name, value }));
}

function convertAmount(value: number, sourceCurrency?: string | null) {
  const source = sourceCurrency || 'CNY';
  if (source === displayCurrency.value) {
    return round4(Number(value));
  }
  if (source === 'USD' && displayCurrency.value === 'CNY') {
    return round4(Number(value) * usdCnyRate.value);
  }
  if (source === 'CNY' && displayCurrency.value === 'USD') {
    return round4(Number(value) / usdCnyRate.value);
  }
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
  if (abs >= 10000) {
    return `${currencySymbol.value}${round4(value / 10000)}万`;
  }
  return `${currencySymbol.value}${round4(value)}`;
}

function formatMoney(value: number) {
  return `${currencySymbol.value}${round4(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })}`;
}

function periodStart(mode: Extract<ContributionMode, 'MONTH' | 'YEAR'>) {
  const now = new Date();
  return mode === 'MONTH' ? new Date(now.getFullYear(), now.getMonth(), 1) : new Date(now.getFullYear(), 0, 1);
}

function dateBefore(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`;
}

function typeLabel(type?: string | null) {
  return ({ FUND: '基金', STOCK: '股票', CRYPTO: '虚拟货币', OTHER: '其他' } as Record<string, string>)[type || ''] || '-';
}
</script>

<style scoped>
/* 投资主页突出统计，操作入口集中到投资明细页。 */
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

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.currency-select {
  width: 120px;
}

.contribution-panel {
  margin-top: 24px;
}
</style>
