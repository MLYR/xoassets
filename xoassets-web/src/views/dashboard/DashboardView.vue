<!-- 首页仪表盘：按资产小块、收支汇总和趋势大图展示真实财务概览。 -->
<template>
  <div class="page">
    <section class="dashboard-hero-grid" v-loading="loading">
      <div class="asset-card-grid">
        <div v-for="metric in assetMetrics" :key="metric.title" class="asset-mini-card panel panel-padding">
          <span class="asset-mini-title">{{ metric.title }}</span>
          <AmountText class="asset-mini-value" :value="metric.value" :precision="2" />
          <small>{{ metric.description }}</small>
        </div>
        <button class="snapshot-marker" type="button" title="生成今日快照" aria-label="生成今日快照" :aria-busy="snapshotGenerating" :disabled="snapshotGenerating" @click="handleGenerateSnapshot">
          <el-icon :class="{ 'is-spinning': snapshotGenerating }"><RefreshRight /></el-icon>
        </button>
      </div>

      <div class="panel panel-padding finance-summary-card">
        <div class="panel-head compact">
          <div>
            <h3>收支与盈亏</h3>
            <p>当月、当日和投资盈亏汇总</p>
          </div>
        </div>
        <div class="finance-summary-grid">
          <div v-for="item in financeSummaryItems" :key="item.title" class="finance-summary-item">
            <span>{{ item.title }}</span>
            <AmountText :value="item.value" :with-sign="item.withSign" :precision="2" />
            <small>{{ item.description }}</small>
          </div>
        </div>
      </div>
    </section>

    <section class="panel panel-padding chart-panel hero-chart-panel">
      <div class="panel-head">
        <div>
          <h3>资产趋势</h3>
          <p>{{ range }}资产变化</p>
        </div>
        <div class="chart-actions">
          <el-segmented v-model="assetTrendMode" :options="assetTrendOptions" />
          <el-segmented v-model="range" :options="['7天', '30天', '90天']" />
        </div>
      </div>
      <el-empty v-if="!loading && assetTrend.length === 0" description="暂无净资产趋势数据" />
      <BaseChart v-else :option="assetOption" height="360px" />
    </section>

  </div>
</template>

<script setup lang="ts">
// 首页从真实 dashboard/statistics 接口取数，避免继续依赖 mock。
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { RefreshRight } from '@element-plus/icons-vue';
import type { EChartsOption } from 'echarts';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import { dashboardApi, type DashboardOverview } from '@/services/dashboardApi';
import { snapshotApi, type AssetSnapshotItem } from '@/services/snapshotApi';

const range = ref('30天');
const assetTrendMode = ref<'totalAsset' | 'cashAsset' | 'investmentAsset'>('totalAsset');
const loading = ref(false);
const snapshotGenerating = ref(false);
const overview = reactive<DashboardOverview>({
  totalAssets: 0,
  netAssets: 0,
  todayIncome: 0,
  todayExpense: 0,
  yesterdayIncome: 0,
  yesterdayExpense: 0,
  monthlyIncome: 0,
  monthlyExpense: 0,
  todayBalance: 0,
  monthlyBalance: 0,
  todayBalanceRateByIncome: null,
  todayBalanceRateByExpense: null,
  monthlyBalanceRateByIncome: null,
  monthlyBalanceRateByExpense: null,
  investmentMarketValue: 0,
  investmentFloatingProfit: 0,
  investmentTotalProfit: 0,
  investmentTodayProfit: null,
  budgetUsageRate: 0,
  assetTrendRate: null,
  incomeTrendRate: null,
  expenseTrendRate: null,
  balanceTrendRate: null
});
const assetTrend = ref<AssetSnapshotItem[]>([]);
const assetTrendOptions = [
  { label: '总资产', value: 'totalAsset' },
  { label: '账户', value: 'cashAsset' },
  { label: '投资', value: 'investmentAsset' }
];

onMounted(() => {
  loadDashboard();
});

watch(range, () => {
  loadTrend();
});

const accountAsset = computed(() => {
  // 首页四块资产必须来自同一实时口径：总资产 = 账户资产 + 投资资产。
  return Number(overview.totalAssets || 0) - Number(overview.investmentMarketValue || 0);
});

const assetMetrics = computed(() => [
  { title: '总资产', value: overview.totalAssets, description: '账户 + 投资' },
  // 净资产不复用其他趋势率，避免把收支变化冒充资产变化。
  { title: '净资产', value: overview.netAssets, description: '总资产 - 负债' },
  { title: '账户资产', value: accountAsset.value, description: '现金类账户余额' },
  { title: '投资资产', value: overview.investmentMarketValue, description: '当前持仓市值' }
]);

const financeSummaryItems = computed(() => [
  { title: '当月支出', value: overview.monthlyExpense, description: '普通流水支出', withSign: false },
  { title: '当月收入', value: overview.monthlyIncome, description: '普通流水收入', withSign: false },
  { title: '当日支出', value: overview.todayExpense, description: '不含转账', withSign: false },
  { title: '当日收入', value: overview.todayIncome, description: '普通流水收入', withSign: false },
  // 投资盈亏(总)按已实现 + 当前持仓浮动收益展示，和“持有收益”区分开。
  { title: '投资盈亏(总)', value: overview.investmentTotalProfit, description: '投资总收益', withSign: true },
  // 今日盈亏按投资今日收益展示，不再使用普通流水当日结余。
  { title: '今日盈亏', value: overview.investmentTodayProfit, description: '投资今日收益', withSign: true }
]);

const assetOption = computed<EChartsOption>(() => ({
  grid: { left: 44, right: 16, top: 24, bottom: 32 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: assetTrend.value.map((item) => item.snapshotDate), axisLine: { lineStyle: { color: '#e2e8f0' } } },
  yAxis: { type: 'value', axisLabel: { formatter: (value: number) => `${Math.round(value / 1000)}k` }, splitLine: { lineStyle: { color: '#e2e8f0' } } },
  series: [{ type: 'line', smooth: true, data: assetTrend.value.map((item) => item[assetTrendMode.value]), symbolSize: 7, lineStyle: { color: '#2563eb', width: 3 }, itemStyle: { color: '#2563eb' }, areaStyle: { color: 'rgba(37, 99, 235, 0.12)' } }]
}));

async function loadDashboard() {
  loading.value = true;
  try {
    const overviewData = await dashboardApi.overview();
    Object.assign(overview, overviewData);
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
  try {
    await ElMessageBox.confirm(
      '生成今日快照会按当前账户资产、投资资产、负债和收支汇总覆盖今天唯一一条资产快照，用于首页趋势、资产目标和复盘统计。确认生成吗？',
      '生成今日快照',
      {
        type: 'info',
        confirmButtonText: '确认生成',
        cancelButtonText: '取消'
      }
    );
  } catch {
    return;
  }
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

function dateBefore(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`;
}
</script>

<style scoped>
/* 首页首屏按草图分为左侧 2×2 资产小块，右侧收支与盈亏汇总。 */
.dashboard-hero-grid {
  display: grid;
  grid-template-columns: minmax(520px, 0.9fr) minmax(520px, 1.1fr);
  gap: 24px;
  align-items: stretch;
}

.asset-card-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.snapshot-marker {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 2;
  display: inline-grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid rgba(37, 99, 235, 0.22);
  border-radius: 999px;
  background:
    radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.95), rgba(239, 246, 255, 0.86)),
    rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 30px rgba(37, 99, 235, 0.14), inset 0 0 0 6px rgba(37, 99, 235, 0.06);
  color: var(--xo-primary);
  cursor: pointer;
  transform: translate(-50%, -50%);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.snapshot-marker:hover {
  border-color: rgba(37, 99, 235, 0.42);
  box-shadow: 0 18px 36px rgba(37, 99, 235, 0.20), inset 0 0 0 6px rgba(37, 99, 235, 0.08);
  transform: translate(-50%, -50%) scale(1.05);
}

.snapshot-marker:focus-visible {
  outline: 3px solid rgba(37, 99, 235, 0.24);
  outline-offset: 4px;
}

.snapshot-marker:disabled {
  cursor: wait;
  opacity: 0.72;
}

.snapshot-marker .el-icon {
  font-size: 20px;
}

.snapshot-marker .is-spinning {
  animation: snapshot-spin 0.8s linear infinite;
}

@keyframes snapshot-spin {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}

.asset-mini-card {
  min-height: 152px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}

.asset-mini-title,
.finance-summary-item span {
  color: #475569;
  font-size: 13px;
  font-weight: 800;
}

.asset-mini-card small,
.finance-summary-item small {
  color: var(--xo-muted);
  font-size: 12px;
  line-height: 1.5;
}

.asset-mini-value {
  display: block;
  margin: 10px 0;
  font-size: clamp(28px, 1.6vw, 30px);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.asset-mini-card :deep(.asset-mini-value.amount-text) {
  font-size: clamp(28px, 1.6vw, 30px);
  font-weight: 800;
}

.finance-summary-card {
  min-height: 294px;
}

.finance-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.finance-summary-item {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid rgba(226, 232, 240, 0.78);
  border-radius: 16px;
  background: rgba(248, 251, 255, 0.76);
}

.finance-summary-item :deep(.amount-text) {
  font-size: 20px;
  font-weight: 850;
}

.hero-chart-panel {
  min-height: 500px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-head.compact {
  margin-bottom: 14px;
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

.chart-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 1080px) {
  .dashboard-hero-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .asset-card-grid,
  .finance-summary-grid {
    grid-template-columns: 1fr;
  }

  .asset-mini-value {
    font-size: 30px;
  }
}
</style>
