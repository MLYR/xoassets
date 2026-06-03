<!-- 首页仪表盘：展示真实资产指标、趋势图、支出结构和最近交易。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">首页</h1>
        <p class="page-subtitle">欢迎回来，这是您的财务概览</p>
      </div>
      <el-button type="primary" :loading="snapshotGenerating" @click="handleGenerateSnapshot">生成今日快照</el-button>
    </div>

    <section class="dashboard-metrics" v-loading="loading">
      <MetricCard v-for="metric in dashboardMetrics" :key="metric.title" v-bind="metric" />
    </section>

    <section class="dashboard-grid">
      <div class="panel panel-padding chart-panel">
        <div class="panel-head">
          <div>
            <h3>净资产趋势</h3>
            <p>近 30 天资产变化</p>
          </div>
          <el-segmented v-model="range" :options="['7天', '30天', '90天']" />
        </div>
        <el-empty v-if="!loading && assetTrend.length === 0" description="暂无净资产趋势数据" />
        <BaseChart v-else :option="assetOption" />
      </div>
      <div class="panel panel-padding">
        <div class="panel-head">
          <div>
            <h3>支出分析</h3>
            <p>本月分类占比</p>
          </div>
        </div>
        <el-empty v-if="!loading && expenseCategories.length === 0" description="暂无支出分类数据" />
        <BaseChart v-else :option="expenseOption" height="210px" />
        <div class="legend-list">
          <div v-for="item in expenseBreakdown" :key="item.name" class="legend-row">
            <span><i />{{ item.name }}</span>
            <AmountText :value="item.value" muted />
          </div>
        </div>
      </div>
    </section>

    <section class="recent-grid">
      <div class="panel panel-padding">
        <div class="panel-head">
          <h3>最近交易</h3>
          <el-button link type="primary" @click="$router.push(ROUTES.transactions)">查看全部</el-button>
        </div>
        <el-empty v-if="overview.recentTransactions.length === 0" description="暂无最近交易" />
        <el-table v-else :data="overview.recentTransactions" stripe>
          <el-table-column label="日期" min-width="150">
            <template #default="{ row }">{{ formatDateTime(row.transactionTime) }}</template>
          </el-table-column>
          <el-table-column label="类型" width="90">
            <template #default="{ row }"><StatusBadge :label="transactionTypeLabel(row.type)" /></template>
          </el-table-column>
          <el-table-column label="分类">
            <template #default="{ row }">{{ row.categoryName || '-' }}</template>
          </el-table-column>
          <el-table-column label="账户" min-width="150">
            <template #default="{ row }">{{ row.accountName || '-' }}</template>
          </el-table-column>
          <el-table-column label="金额" align="right">
            <template #default="{ row }"><AmountText :value="signedTransactionAmount(row)" with-sign /></template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel panel-padding">
        <div class="panel-head">
          <h3>最近投资交易</h3>
          <el-button link type="primary" @click="$router.push(ROUTES.investments)">查看持仓</el-button>
        </div>
        <el-empty v-if="overview.recentInvestmentTransactions.length === 0" description="暂无投资交易" />
        <el-table v-else :data="overview.recentInvestmentTransactions" stripe>
          <el-table-column label="时间" min-width="150">
            <template #default="{ row }">{{ formatDateTime(row.transactionTime) }}</template>
          </el-table-column>
          <el-table-column label="类型" width="90">
            <template #default="{ row }"><StatusBadge :label="row.type === 'BUY' ? '买入' : '卖出'" /></template>
          </el-table-column>
          <el-table-column label="资产">
            <template #default="{ row }">{{ row.assetName || row.symbol || '-' }}</template>
          </el-table-column>
          <el-table-column label="金额" align="right">
            <template #default="{ row }"><AmountText :value="row.amount" /></template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
// 首页从真实 dashboard/statistics 接口取数，避免继续依赖 mock。
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import type { EChartsOption } from 'echarts';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { ROUTES } from '@/constants/routes';
import { dashboardApi, type DashboardOverview } from '@/services/dashboardApi';
import { snapshotApi, type AssetSnapshotItem, type AssetSnapshotLatest } from '@/services/snapshotApi';
import { statisticsApi, type ExpenseCategoryStat } from '@/services/statisticsApi';
import type { TransactionItem } from '@/services/transactionApi';

const range = ref('30天');
const loading = ref(false);
const snapshotGenerating = ref(false);
const overview = reactive<DashboardOverview>({
  totalAssets: 0,
  netAssets: 0,
  todayExpense: 0,
  monthlyIncome: 0,
  monthlyExpense: 0,
  monthlyBalance: 0,
  investmentMarketValue: 0,
  investmentFloatingProfit: 0,
  budgetUsageRate: 0,
  assetTrendRate: 0,
  incomeTrendRate: 0,
  expenseTrendRate: 0,
  balanceTrendRate: 0,
  recentTransactions: [],
  recentInvestmentTransactions: []
});
const snapshotLatest = ref<AssetSnapshotLatest | null>(null);
const assetTrend = ref<AssetSnapshotItem[]>([]);
const expenseCategories = ref<ExpenseCategoryStat[]>([]);

onMounted(() => {
  loadDashboard();
});

watch(range, () => {
  loadTrend();
});

const dashboardMetrics = computed(() => [
  { title: '总资产', value: overview.totalAssets, trend: overview.assetTrendRate, description: '含投资市值', tone: 'primary' as const },
  { title: '净资产', value: overview.netAssets, trend: overview.balanceTrendRate, description: '当前估算', tone: 'success' as const },
  { title: '较昨日变化', value: snapshotLatest.value?.netAssetChangeFromYesterday || 0, trend: 0, description: '基于资产快照', tone: changeTone(snapshotLatest.value?.netAssetChangeFromYesterday || 0) },
  { title: '较本月初变化', value: snapshotLatest.value?.netAssetChangeFromMonthStart || 0, trend: 0, description: '基于资产快照', tone: changeTone(snapshotLatest.value?.netAssetChangeFromMonthStart || 0) },
  { title: '今日支出', value: overview.todayExpense, trend: overview.expenseTrendRate, description: '不含转账', tone: 'warning' as const },
  { title: '投资盈亏', value: overview.investmentFloatingProfit, trend: 0, description: '浮动盈亏', tone: overview.investmentFloatingProfit >= 0 ? 'success' as const : 'danger' as const }
]);

const expenseBreakdown = computed(() => expenseCategories.value.map((item) => ({ name: item.categoryName || '未分类', value: item.amount })));

const assetOption = computed<EChartsOption>(() => ({
  grid: { left: 44, right: 16, top: 24, bottom: 32 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: assetTrend.value.map((item) => item.snapshotDate), axisLine: { lineStyle: { color: '#e2e8f0' } } },
  yAxis: { type: 'value', axisLabel: { formatter: (value: number) => `${Math.round(value / 1000)}k` }, splitLine: { lineStyle: { color: '#e2e8f0' } } },
  series: [{ type: 'line', smooth: true, data: assetTrend.value.map((item) => item.netAsset), symbolSize: 7, lineStyle: { color: '#2563eb', width: 3 }, itemStyle: { color: '#2563eb' }, areaStyle: { color: 'rgba(37, 99, 235, 0.12)' } }]
}));

const expenseOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#2dd4bf', '#8b5cf6', '#f6c453', '#fb7185'],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['56%', '78%'], avoidLabelOverlap: true, label: { show: false }, data: expenseBreakdown.value }]
}));

async function loadDashboard() {
  loading.value = true;
  try {
    const [overviewData, expenseData, snapshotData] = await Promise.all([dashboardApi.overview(), statisticsApi.expenseCategory(currentMonth()), snapshotApi.latest()]);
    Object.assign(overview, overviewData);
    expenseCategories.value = expenseData;
    snapshotLatest.value = snapshotData;
    await loadTrend();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '首页数据加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadTrend() {
  try {
    const days = range.value === '7天' ? 7 : range.value === '90天' ? 90 : 30;
    assetTrend.value = await snapshotApi.trend({ startDate: dateBefore(days - 1), endDate: dateBefore(0) });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资产趋势加载失败');
  }
}

async function handleGenerateSnapshot() {
  snapshotGenerating.value = true;
  try {
    // 后端按 userId + snapshotDate 做当天快照覆盖更新，避免一天生成多份快照。
    await snapshotApi.generateToday();
    ElMessage.success('今日资产快照已生成');
    await loadDashboard();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资产快照生成失败');
  } finally {
    snapshotGenerating.value = false;
  }
}

function transactionTypeLabel(type: TransactionItem['type']) {
  return ({ INCOME: '收入', EXPENSE: '支出', TRANSFER: '转账', REFUND: '退款' } as Record<TransactionItem['type'], string>)[type];
}

function signedTransactionAmount(row: TransactionItem) {
  return row.type === 'EXPENSE' || row.type === 'TRANSFER' ? -Number(row.amount) : Number(row.amount);
}

function formatDateTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function changeTone(value: number) {
  return value >= 0 ? 'success' as const : 'danger' as const;
}

function currentMonth() {
  const date = new Date();
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}`;
}

function dateBefore(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`;
}
</script>

<style scoped>
/* 首页核心布局为 2:1 图表区，卡片留白和圆角对齐原型。 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.dashboard-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.recent-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--xo-muted);
  font-size: 13px;
}

.legend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.legend-row span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--xo-text);
  font-size: 14px;
}

.legend-row i {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
}

@media (max-width: 1080px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
