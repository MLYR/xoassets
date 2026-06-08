<!-- 首页仪表盘：按资产小块、收支汇总和趋势大图展示真实财务概览。 -->
<template>
  <div class="page dashboard-page">
    <section class="dashboard-hero-grid" v-loading="loading">
      <div class="asset-card-grid">
        <div v-for="(metric, index) in assetMetrics" :key="metric.title" :class="['premium-shell', 'asset-mini-shell', `tone-${metric.tone}`]" :style="{ animationDelay: `${index * 70}ms` }">
          <div class="premium-core asset-mini-card">
            <div class="asset-mini-top">
              <span class="asset-mini-title">{{ metric.title }}</span>
              <i class="asset-mini-dot" aria-hidden="true"></i>
            </div>
            <div>
              <AmountText class="asset-mini-value" :value="metric.value" :precision="2" />
              <small>{{ metric.description }}</small>
            </div>
          </div>
        </div>
        <button class="snapshot-marker" type="button" title="生成今日快照" aria-label="生成今日快照" :aria-busy="snapshotGenerating" :disabled="snapshotGenerating" @click="handleGenerateSnapshot">
          <el-icon :class="{ 'is-spinning': snapshotGenerating }"><RefreshRight /></el-icon>
        </button>
      </div>

      <div class="premium-shell finance-summary-shell" style="animation-delay: 140ms;">
        <div class="premium-core finance-summary-card">
          <div class="panel-head compact">
            <div>
              <span class="dashboard-eyebrow">Cashflow</span>
              <h3>收支与盈亏</h3>
              <p>当月、昨日、当日和投资盈亏汇总</p>
            </div>
          </div>
          <div class="finance-summary-grid">
            <div v-for="(item, index) in financeSummaryItems" :key="item.title" :class="['finance-summary-item', `tone-${item.tone}`]" :style="{ animationDelay: `${180 + index * 45}ms` }">
              <span>{{ item.title }}</span>
              <AmountText :value="item.value" :with-sign="item.withSign" :precision="2" />
              <small>{{ item.description }}</small>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="premium-shell hero-chart-shell" style="animation-delay: 220ms;">
      <div class="premium-core chart-panel hero-chart-panel">
        <div class="panel-head">
          <div>
            <span class="dashboard-eyebrow">Trajectory</span>
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
      </div>
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
import { investmentApi } from '@/services/investmentApi';
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
  investmentYesterdayProfit: null,
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
  { title: '总资产', value: overview.totalAssets, description: '账户 + 投资', tone: 'primary' },
  // 净资产不复用其他趋势率，避免把收支变化冒充资产变化。
  { title: '净资产', value: overview.netAssets, description: '总资产 - 负债', tone: 'success' },
  { title: '账户资产', value: accountAsset.value, description: '现金类账户余额', tone: 'cyan' },
  { title: '投资资产', value: overview.investmentMarketValue, description: '当前持仓市值', tone: 'purple' }
]);

const financeSummaryItems = computed(() => [
  { title: '当月支出', value: overview.monthlyExpense, description: '普通流水支出', withSign: false, tone: 'danger' },
  { title: '当月收入', value: overview.monthlyIncome, description: '普通流水收入', withSign: false, tone: 'success' },
  { title: '昨日支出', value: overview.yesterdayExpense, description: '不含转账', withSign: false, tone: 'danger' },
  { title: '昨日投资盈亏', value: overview.investmentYesterdayProfit, description: '上一交易日', withSign: true, tone: 'purple' },
  { title: '当日支出', value: overview.todayExpense, description: '不含转账', withSign: false, tone: 'danger' },
  { title: '当日收入', value: overview.todayIncome, description: '普通流水收入', withSign: false, tone: 'success' },
  // 投资盈亏(总)按已实现 + 当前持仓浮动收益展示，和“持有收益”区分开。
  { title: '投资总收益', value: overview.investmentTotalProfit, description: '已实现 + 浮盈', withSign: true, tone: 'purple' },
  // 今日盈亏按投资今日收益展示，不再使用普通流水当日结余。
  { title: '投资今日收益', value: overview.investmentTodayProfit, description: '今日有效价', withSign: true, tone: 'primary' }
]);

const assetOption = computed<EChartsOption>(() => ({
  grid: { left: 44, right: 16, top: 24, bottom: 32 },
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(15, 23, 42, 0.88)', borderWidth: 0, textStyle: { color: '#ffffff' } },
  xAxis: { type: 'category', data: assetTrend.value.map((item) => item.snapshotDate), axisLine: { lineStyle: { color: '#dbe7f5' } }, axisLabel: { color: '#94a3b8' } },
  yAxis: { type: 'value', axisLabel: { color: '#64748b', formatter: (value: number) => `${Math.round(value / 1000)}k` }, splitLine: { lineStyle: { color: '#e8eef7' } } },
  series: [{ type: 'line', smooth: true, data: assetTrend.value.map((item) => item[assetTrendMode.value]), symbolSize: 8, lineStyle: { color: '#2563eb', width: 3 }, itemStyle: { color: '#2563eb', borderColor: '#ffffff', borderWidth: 2 }, areaStyle: { color: 'rgba(37, 99, 235, 0.10)' } }]
}));

async function loadDashboard() {
  loading.value = true;
  try {
    const [overviewData, investmentOverview] = await Promise.all([
      dashboardApi.overview(),
      investmentApi.overviewInvestments().catch(() => null)
    ]);
    Object.assign(overview, overviewData);
    // 运行中的旧后端可能还没有 dashboard 字段；这里直接兜底取投资总览同源数据，避免昨日投资盈亏显示 --。
    overview.investmentYesterdayProfit = overviewData.investmentYesterdayProfit ?? investmentOverview?.yesterdayProfit ?? null;
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
/* 首页采用 Soft Structuralism + Asymmetrical Bento：浅色金融底、双层卡片和弹簧式动效。 */
.dashboard-page {
  --dashboard-ease: cubic-bezier(0.32, 0.72, 0, 1);
  --dashboard-ease-soft: cubic-bezier(0.2, 0.8, 0.16, 1);
  position: relative;
  gap: 30px;
  font-family: "Plus Jakarta Sans", "Geist", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.dashboard-page::before {
  content: '';
  position: absolute;
  top: 2px;
  right: 24px;
  left: 24px;
  height: 240px;
  border-radius: 40px;
  background:
    radial-gradient(circle at 16% 20%, rgba(37, 99, 235, 0.16), transparent 32%),
    radial-gradient(circle at 78% 4%, rgba(45, 212, 191, 0.18), transparent 30%),
    radial-gradient(circle at 52% 42%, rgba(139, 92, 246, 0.08), transparent 34%),
    linear-gradient(135deg, rgba(239, 246, 255, 0.86), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.dashboard-hero-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(500px, 0.82fr) minmax(640px, 1.18fr);
  gap: 26px;
  align-items: stretch;
}

.asset-card-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.premium-shell {
  position: relative;
  padding: 6px;
  border: 1px solid rgba(203, 218, 238, 0.72);
  border-radius: 30px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(235, 243, 255, 0.62)),
    rgba(248, 251, 255, 0.76);
  box-shadow: 0 24px 62px rgba(31, 41, 55, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.88);
  opacity: 0;
  transform: translateY(28px) scale(0.985);
  animation: dashboard-reveal 860ms var(--dashboard-ease) forwards;
}

.premium-core {
  position: relative;
  height: 100%;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.9);
  overflow: hidden;
}

.asset-mini-shell {
  --card-accent: linear-gradient(90deg, #2563eb, #60a5fa);
  --card-glow: rgba(37, 99, 235, 0.10);
  transition: transform 720ms var(--dashboard-ease), box-shadow 720ms var(--dashboard-ease), border-color 720ms var(--dashboard-ease);
}

.asset-mini-shell::before {
  content: '';
  position: absolute;
  inset: 5px 5px auto;
  z-index: 2;
  height: 4px;
  border-radius: 999px;
  background: var(--card-accent);
  opacity: 0.96;
  pointer-events: none;
}

.asset-mini-shell:hover {
  border-color: rgba(37, 99, 235, 0.24);
  box-shadow: 0 30px 76px rgba(37, 99, 235, 0.13), inset 0 1px 0 rgba(255, 255, 255, 0.92);
  transform: translateY(-4px) scale(1.006);
}

.asset-mini-shell.tone-primary {
  --card-accent: linear-gradient(90deg, #2563eb, #60a5fa);
  --card-glow: rgba(37, 99, 235, 0.12);
}

.asset-mini-shell.tone-success {
  --card-accent: linear-gradient(90deg, #12b981, #5eead4);
  --card-glow: rgba(18, 185, 129, 0.12);
}

.asset-mini-shell.tone-cyan {
  --card-accent: linear-gradient(90deg, #0ea5e9, #67e8f9);
  --card-glow: rgba(14, 165, 233, 0.12);
}

.asset-mini-shell.tone-purple {
  --card-accent: linear-gradient(90deg, #8b5cf6, #c4b5fd);
  --card-glow: rgba(139, 92, 246, 0.12);
}

.asset-mini-card {
  min-height: 168px;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 18px;
  padding: 24px;
  background:
    radial-gradient(circle at 88% 82%, var(--card-glow), transparent 35%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.82));
}

.asset-mini-card::after {
  content: '';
  position: absolute;
  right: -38px;
  bottom: -54px;
  width: 144px;
  height: 144px;
  border-radius: 48px;
  background: var(--card-glow);
  transform: rotate(18deg);
  pointer-events: none;
}

.asset-mini-card > * {
  position: relative;
  z-index: 1;
}

.asset-mini-card > div:last-child {
  align-self: center;
  width: 100%;
  transform: translateY(-2px);
}

.asset-mini-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.asset-mini-dot {
  width: 10px;
  height: 10px;
  border: 2px solid rgba(255, 255, 255, 0.94);
  border-radius: 999px;
  background: var(--card-accent);
  box-shadow: 0 0 0 5px rgba(37, 99, 235, 0.08);
}

.asset-mini-title {
  color: #263449;
  font-size: 17px;
  font-weight: 900;
  letter-spacing: -0.018em;
}

.asset-mini-card small,
.finance-summary-item small {
  color: var(--xo-muted);
  font-size: 12px;
  line-height: 1.45;
}

.asset-mini-value {
  display: block;
  margin: 0 0 8px;
  font-size: clamp(30px, 1.86vw, 35px);
  font-weight: 900;
  line-height: 1.08;
  letter-spacing: -0.055em;
}

.asset-mini-card :deep(.asset-mini-value.amount-text) {
  font-size: clamp(30px, 1.86vw, 35px);
  font-weight: 900;
}

.snapshot-marker {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 3;
  display: inline-grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border: 1px solid rgba(37, 99, 235, 0.20);
  border-radius: 999px;
  background:
    radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.98), rgba(239, 246, 255, 0.86)),
    rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 34px rgba(37, 99, 235, 0.16), inset 0 0 0 7px rgba(37, 99, 235, 0.055);
  color: var(--xo-primary);
  cursor: pointer;
  transform: translate(-50%, -50%);
  transition: transform 680ms var(--dashboard-ease), box-shadow 680ms var(--dashboard-ease), border-color 680ms var(--dashboard-ease);
}

.snapshot-marker:hover {
  border-color: rgba(37, 99, 235, 0.42);
  box-shadow: 0 24px 48px rgba(37, 99, 235, 0.23), inset 0 0 0 7px rgba(37, 99, 235, 0.075);
  transform: translate(-50%, -50%) scale(1.08) rotate(8deg);
}

.snapshot-marker:active {
  transform: translate(-50%, -50%) scale(0.96) rotate(4deg);
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
  animation: snapshot-spin 1.1s cubic-bezier(0.32, 0.72, 0, 1) infinite;
}

.finance-summary-shell,
.hero-chart-shell {
  transition: transform 760ms var(--dashboard-ease), box-shadow 760ms var(--dashboard-ease), border-color 760ms var(--dashboard-ease);
}

.finance-summary-shell:hover,
.hero-chart-shell:hover {
  border-color: rgba(37, 99, 235, 0.20);
  box-shadow: 0 30px 82px rgba(15, 23, 42, 0.10), inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transform: translateY(-3px);
}

.finance-summary-card {
  min-height: 294px;
  padding: 24px;
  background:
    radial-gradient(circle at 8% 0%, rgba(37, 99, 235, 0.12), transparent 30%),
    radial-gradient(circle at 92% 8%, rgba(18, 185, 129, 0.12), transparent 28%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.84));
}

.finance-summary-card > * {
  position: relative;
  z-index: 1;
}

.finance-summary-grid {
  display: grid;
  /* 收支项增加到 8 个后，大屏保持 4×2，避免右侧卡片被撑得过高。 */
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.finance-summary-item {
  --summary-accent: var(--xo-primary);
  --summary-glow: rgba(37, 99, 235, 0.08);
  position: relative;
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 13px 10px;
  border: 1px solid rgba(217, 228, 244, 0.82);
  border-radius: 17px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(248, 251, 255, 0.72)),
    rgba(248, 251, 255, 0.76);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.88);
  opacity: 0;
  transform: translateY(16px) scale(0.98);
  animation: dashboard-reveal 760ms var(--dashboard-ease) forwards;
  transition: transform 640ms var(--dashboard-ease), border-color 640ms var(--dashboard-ease), box-shadow 640ms var(--dashboard-ease);
}

.finance-summary-item::before {
  content: '';
  position: absolute;
  top: 13px;
  right: 13px;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--summary-accent);
  box-shadow: 0 0 0 4px var(--summary-glow);
}

.finance-summary-item:hover {
  border-color: rgba(37, 99, 235, 0.20);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.07), inset 0 1px 0 rgba(255, 255, 255, 0.92);
  transform: translateY(-3px) scale(1.01);
}

.finance-summary-item.tone-danger {
  --summary-accent: var(--xo-danger);
  --summary-glow: rgba(240, 93, 79, 0.10);
}

.finance-summary-item.tone-success {
  --summary-accent: var(--xo-success);
  --summary-glow: rgba(18, 185, 129, 0.11);
}

.finance-summary-item.tone-purple {
  --summary-accent: var(--xo-purple);
  --summary-glow: rgba(139, 92, 246, 0.11);
}

.finance-summary-item.tone-primary {
  --summary-accent: var(--xo-primary);
  --summary-glow: rgba(37, 99, 235, 0.10);
}

.finance-summary-item span {
  max-width: calc(100% - 18px);
  color: #3f4f66;
  font-size: 13px;
  font-weight: 850;
  letter-spacing: -0.01em;
}

.finance-summary-item :deep(.amount-text) {
  display: block;
  max-width: 100%;
  font-size: clamp(16px, 0.94vw, 18px);
  font-weight: 900;
  letter-spacing: -0.035em;
}

.hero-chart-shell {
  position: relative;
  z-index: 1;
}

.hero-chart-panel {
  min-height: 500px;
  padding: 24px;
  background:
    radial-gradient(circle at 18% 0%, rgba(37, 99, 235, 0.09), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.88));
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

.dashboard-eyebrow {
  display: inline-flex;
  width: fit-content;
  margin-bottom: 8px;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: var(--xo-primary);
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.panel-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: -0.035em;
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

.chart-actions :deep(.el-segmented) {
  --el-segmented-item-selected-bg-color: #ffffff;
  --el-segmented-item-selected-color: var(--xo-primary);
  padding: 4px;
  border: 1px solid rgba(217, 228, 244, 0.82);
  border-radius: 15px;
  background: rgba(239, 246, 255, 0.78);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.86);
}

.chart-actions :deep(.el-segmented__item) {
  border-radius: 11px;
  font-weight: 800;
  transition: transform 520ms var(--dashboard-ease), color 520ms var(--dashboard-ease), background 520ms var(--dashboard-ease);
}

.chart-actions :deep(.el-segmented__item:hover) {
  transform: translateY(-1px);
}

@keyframes dashboard-reveal {
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes snapshot-spin {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1080px) {
  .dashboard-hero-grid {
    grid-template-columns: 1fr;
  }

  .finance-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .dashboard-page {
    gap: 22px;
    padding-inline: 16px;
  }

  .dashboard-page::before {
    right: 14px;
    left: 14px;
  }

  .asset-card-grid,
  .finance-summary-grid {
    grid-template-columns: 1fr;
  }

  .asset-mini-value,
  .asset-mini-card :deep(.asset-mini-value.amount-text) {
    font-size: 30px;
  }

  .panel-head {
    flex-direction: column;
  }

  .chart-actions {
    justify-content: flex-start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .premium-shell,
  .finance-summary-item {
    opacity: 1;
    transform: none;
    animation: none;
  }

  .asset-mini-shell,
  .finance-summary-shell,
  .hero-chart-shell,
  .finance-summary-item,
  .snapshot-marker,
  .chart-actions :deep(.el-segmented__item) {
    transition: none;
  }
}
</style>
