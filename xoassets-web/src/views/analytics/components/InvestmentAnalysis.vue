<!-- 投资分析：接入投资模块接口，拆分总览、模块、趋势和每日收益日历。 -->
<template>
  <div class="investment-analysis">
    <section v-loading="loading" class="grid-4">
      <MetricCard title="投资资产总额" :value="overview?.totalInvestmentAsset ?? null" :trend="overview?.holdingProfitRate ?? null" description="基金 + 股票 + 虚拟货币" tone="primary" :precision="4" />
      <MetricCard title="持有收益" :value="overview?.holdingProfit ?? null" :trend="overview?.holdingProfitRate ?? null" description="当前市值 - 持仓成本" :tone="profitTone(overview?.holdingProfit)" :precision="4" with-sign />
      <MetricCard title="今日收益" :value="todayProfitValue" :trend="null" :description="todayProfitDescription" :tone="profitTone(todayProfitValue)" :precision="4" with-sign />
      <MetricCard title="昨日收益" :value="overview?.yesterdayProfit ?? null" :trend="null" :description="overview?.yesterdayProfitAssetScope || '收益日历聚合'" :tone="profitTone(overview?.yesterdayProfit)" :precision="4" with-sign />
    </section>

    <section v-if="failed" class="panel panel-padding">
      <el-empty description="投资分析加载失败，资产、收支和预算分析仍可查看">
        <el-button type="primary" @click="$emit('refresh')">重新加载投资分析</el-button>
      </el-empty>
    </section>

    <template v-else>
      <section v-loading="loading" class="module-card-grid">
        <div v-for="item in moduleAssets" :key="item.module" class="module-card panel panel-padding" @click="openInvestmentModule(item.module)">
          <div class="module-card-top">
            <span>{{ item.name }}</span>
            <el-tag round>{{ formatRatio(item.assetRatio) }}</el-tag>
          </div>
          <strong>{{ formatAmount(item.assetAmount, false, 4) }}</strong>
          <div class="module-card-meta">
            <span>{{ item.primaryProfitLabel }}</span>
            <AmountText :value="item.primaryProfitAvailable === false ? null : item.primaryProfitAmount" with-sign :precision="4" />
          </div>
          <div class="module-card-meta muted-text">
            <span>收益状态</span>
            <em>{{ item.primaryProfitStatusLabel || '按模块口径展示' }}</em>
          </div>
          <div class="module-card-meta muted-text">
            <span>持仓数量</span>
            <em>{{ item.holdingCount }} 个</em>
          </div>
        </div>
      </section>

      <section class="panel panel-padding wide-panel">
        <div class="panel-head">
          <div>
            <h3>投资趋势</h3>
            <p>按模块和周期查看市值、总收益、当日收益与持有收益。</p>
          </div>
          <div class="trend-actions">
            <el-segmented :model-value="investmentModule" :options="ANALYTICS_INVESTMENT_MODULE_OPTIONS" @update:model-value="updateInvestmentModule" />
            <el-segmented :model-value="investmentPeriod" :options="ANALYTICS_INVESTMENT_PERIOD_OPTIONS" @update:model-value="updateInvestmentPeriod" />
          </div>
        </div>
        <el-empty v-if="!loading && trendPoints.length === 0" description="暂无投资趋势数据" />
        <BaseChart v-else :option="trendOption" height="340px" />
      </section>

      <section class="panel panel-padding wide-panel">
        <div class="panel-head">
          <div>
            <h3>每日收益日历</h3>
            <p>按所选月份展示全持仓每日收益、休市和价格状态。</p>
          </div>
        </div>
        <el-empty v-if="!loading && calendarCells.length === 0" description="暂无每日收益数据" />
        <div v-else class="daily-profit-calendar">
          <div class="calendar-weekdays">
            <span v-for="day in weekdays" :key="day">{{ day }}</span>
          </div>
          <div class="calendar-grid">
            <div v-for="cell in calendarCells" :key="cell.key" class="calendar-cell" :class="calendarCellClass(cell)">
              <template v-if="!cell.empty">
                <div class="calendar-date">{{ cell.dayNumber }}</div>
                <AmountText v-if="cell.profitAmount !== null && cell.profitAmount !== undefined" :value="cell.profitAmount" with-sign :precision="4" />
                <span v-else class="calendar-status">{{ calendarStatusText(cell) }}</span>
              </template>
            </div>
          </div>
        </div>
      </section>

      <section class="panel panel-padding wide-panel">
        <div class="panel-head">
          <div>
            <h3>{{ moduleLabel(investmentModule) }}持仓</h3>
            <p>用于核对当前模块趋势的持仓来源。</p>
          </div>
        </div>
        <el-empty v-if="!loading && holdings.length === 0" description="暂无模块持仓数据" />
        <el-table v-else :data="holdings" stripe>
          <el-table-column label="资产" min-width="180">
            <template #default="{ row }">
              <div class="holding-name-cell">
                <strong>{{ row.assetName || row.symbol || '-' }}</strong>
                <span>{{ row.symbol || '-' }} · {{ row.assetType || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="市值" min-width="130" align="right" header-align="right">
            <template #default="{ row }"><AmountText :value="row.marketValue" :precision="4" /></template>
          </el-table-column>
          <el-table-column label="持有收益" min-width="130" align="right" header-align="right">
            <template #default="{ row }"><AmountText :value="row.floatingProfit" with-sign :precision="4" /></template>
          </el-table-column>
          <el-table-column label="主收益" min-width="160" align="right" header-align="right">
            <template #default="{ row }">
              <div class="profit-cell">
                <AmountText :value="row.primaryProfitAmount" with-sign :precision="4" />
                <small>{{ row.primaryProfitLabel || '收益' }}</small>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
// 今日收益严格遵守 overview.todayProfitAvailable，缺少今日有效价时展示状态文案。
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import type { EChartsOption, SeriesOption } from 'echarts';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import { ROUTES } from '@/constants/routes';
import { formatAmount } from '@/utils/format';
import type { HoldingItem, InvestmentCalendarDayProfit, InvestmentModuleAsset, InvestmentOverview, InvestmentTrend } from '@/services/investmentApi';
import { ANALYTICS_INVESTMENT_MODULE_OPTIONS, ANALYTICS_INVESTMENT_PERIOD_OPTIONS, type AnalyticsInvestmentModule, type AnalyticsInvestmentPeriod } from '../composables/useAnalyticsData';

interface CalendarCell extends Partial<InvestmentCalendarDayProfit> {
  key: string;
  empty: boolean;
  dayNumber?: number;
}

const props = defineProps<{
  loading: boolean;
  failed: boolean;
  investmentModule: AnalyticsInvestmentModule;
  investmentPeriod: AnalyticsInvestmentPeriod;
  overview: InvestmentOverview | null;
  moduleTrend: InvestmentTrend | null;
  dailyProfit: InvestmentCalendarDayProfit[];
  holdings: HoldingItem[];
}>();

const emit = defineEmits<{
  'update:investmentModule': [value: AnalyticsInvestmentModule];
  'update:investmentPeriod': [value: AnalyticsInvestmentPeriod];
  refresh: [];
}>();
const router = useRouter();

const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
const moduleAssets = computed<InvestmentModuleAsset[]>(() => props.overview?.moduleAssets || []);
const trendPoints = computed(() => props.moduleTrend?.points || []);
const todayProfitValue = computed(() => props.overview?.todayProfitAvailable === false ? null : props.overview?.todayProfit ?? null);
const todayProfitDescription = computed(() => {
  if (!props.overview) {
    return '等待投资总览数据';
  }
  if (props.overview.todayProfitAvailable === false) {
    return props.overview.todayProfitStatusLabel || '今日收益暂不可用';
  }
  return props.overview.todayProfitStatusLabel || props.overview.todayProfitAssetScope || '今日有效价资产';
});

const trendOption = computed<EChartsOption>(() => {
  const hasDailyProfit = trendPoints.value.some((item) => item.dailyProfit !== null && item.dailyProfit !== undefined);
  const hasHoldingProfit = trendPoints.value.some((item) => item.holdingProfit !== null && item.holdingProfit !== undefined);
  const series: SeriesOption[] = [
    lineSeries('市值', trendPoints.value.map((item) => item.marketValue), '--xo-chart-blue'),
    lineSeries('总收益', trendPoints.value.map((item) => item.totalProfit), '--xo-chart-green')
  ];

  if (hasDailyProfit) {
    series.push(lineSeries('当日收益', trendPoints.value.map((item) => item.dailyProfit ?? null), '--xo-chart-yellow'));
  }
  if (hasHoldingProfit) {
    series.push(lineSeries('持有收益', trendPoints.value.map((item) => item.holdingProfit ?? null), '--xo-chart-purple'));
  }

  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 52, right: 18, top: 42, bottom: 38 },
    xAxis: { type: 'category', data: trendPoints.value.map((item) => item.date) },
    yAxis: { type: 'value' },
    series
  };
});

const calendarCells = computed<CalendarCell[]>(() => {
  if (props.dailyProfit.length === 0) {
    return [];
  }
  const firstDate = props.dailyProfit[0]?.date;
  if (!firstDate) {
    return [];
  }
  const [year, month] = firstDate.split('-').map(Number);
  const monthStart = new Date(year, month - 1, 1);
  const totalDays = new Date(year, month, 0).getDate();
  const entryMap = new Map(props.dailyProfit.map((item) => [item.date, item]));
  const blanks: CalendarCell[] = Array.from({ length: monthStart.getDay() }, (_, index) => ({
    key: `blank-${index}`,
    empty: true
  }));

  // 按自然月补齐日期格，接口没返回的日子也展示为暂无价格，便于复盘整月。
  const days: CalendarCell[] = Array.from({ length: totalDays }, (_, index) => {
    const dayNumber = index + 1;
    const date = `${year}-${pad(month)}-${pad(dayNumber)}`;
    const entry = entryMap.get(date);
    return {
      key: date,
      empty: false,
      dayNumber,
      date,
      profitAmount: entry?.profitAmount ?? null,
      hasPrice: entry?.hasPrice ?? false,
      marketClosed: entry?.marketClosed ?? false,
      statusLabel: entry?.statusLabel ?? null
    };
  });

  return [...blanks, ...days];
});

function updateInvestmentModule(value: string | number | boolean) {
  emit('update:investmentModule', value as AnalyticsInvestmentModule);
}

function updateInvestmentPeriod(value: string | number | boolean) {
  emit('update:investmentPeriod', value as AnalyticsInvestmentPeriod);
}

function lineSeries(name: string, data: Array<number | null>, colorVar: string) {
  const color = chartColor(colorVar);
  return {
    name,
    type: 'line' as const,
    smooth: true,
    connectNulls: false,
    data,
    lineStyle: { color, width: 3 },
    itemStyle: { color },
    areaStyle: name === '市值' ? { color: chartColor('--xo-primary-soft') } : undefined
  };
}

function profitTone(value: number | null | undefined): 'success' | 'danger' | 'primary' {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return 'primary';
  }
  return value >= 0 ? 'success' : 'danger';
}

function formatRatio(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--';
  }
  return `${Number(value).toFixed(1)}%`;
}

function moduleLabel(module: AnalyticsInvestmentModule) {
  const matched = ANALYTICS_INVESTMENT_MODULE_OPTIONS.find((item) => item.value === module);
  return matched?.label || '投资';
}

function calendarStatusText(cell: CalendarCell) {
  if (cell.marketClosed) {
    return '休市';
  }
  if (cell.statusLabel) {
    return cell.statusLabel;
  }
  return cell.hasPrice ? '--' : '暂无价格';
}

function calendarCellClass(cell: CalendarCell) {
  if (cell.empty) {
    return 'is-empty';
  }
  if (cell.marketClosed) {
    return 'is-closed';
  }
  const amount = Number(cell.profitAmount || 0);
  if (amount > 0) {
    return 'is-positive';
  }
  if (amount < 0) {
    return 'is-negative';
  }
  return '';
}

function chartColor(name: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

function pad(value: number) {
  return `${value}`.padStart(2, '0');
}

function openInvestmentModule(module: string) {
  router.push({ path: ROUTES.investments, query: { module } });
}
</script>

<style scoped>
/* 投资分析使用独立竖向布局，避免趋势和日历被 Tab 双列网格压缩。 */
.investment-analysis {
  display: grid;
  gap: 24px;
}

.module-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.module-card {
  display: grid;
  gap: 14px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.module-card:hover {
  box-shadow: var(--xo-shadow-hover);
  transform: translateY(-2px);
}

.module-card-top,
.module-card-meta,
.panel-head,
.trend-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.module-card-top span,
.module-card strong {
  color: var(--xo-text);
  font-weight: 800;
}

.module-card strong {
  font-size: 24px;
  font-variant-numeric: tabular-nums;
}

.module-card-meta {
  color: var(--xo-muted);
  font-size: 13px;
}

.module-card-meta em {
  color: var(--xo-muted);
  font-style: normal;
}

.panel-head {
  margin-bottom: 18px;
}

.panel-head h3 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 800;
}

.panel-head p {
  margin: 0;
  color: var(--xo-muted);
  font-size: 13px;
}

.trend-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.daily-profit-calendar {
  display: grid;
  gap: 10px;
}

.calendar-weekdays,
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
}

.calendar-weekdays span {
  color: var(--xo-muted);
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}

.calendar-cell {
  min-height: 92px;
  padding: 10px;
  border: 1px solid var(--xo-border);
  border-radius: var(--xo-radius-inner);
  background: var(--xo-input-muted);
}

.calendar-cell.is-empty {
  border-color: transparent;
  background: transparent;
}

.calendar-cell.is-positive {
  border-color: rgba(18, 185, 129, 0.28);
}

.calendar-cell.is-negative {
  border-color: rgba(240, 93, 79, 0.28);
}

.calendar-cell.is-closed {
  opacity: 0.72;
}

.calendar-date {
  margin-bottom: 10px;
  color: var(--xo-muted);
  font-size: 12px;
  font-weight: 800;
}

.calendar-status {
  color: var(--xo-muted);
  font-size: 12px;
}

.holding-name-cell,
.profit-cell {
  display: grid;
  gap: 3px;
}

.holding-name-cell strong {
  color: var(--xo-text);
}

.holding-name-cell span,
.profit-cell small {
  color: var(--xo-muted);
  font-size: 12px;
}

@media (max-width: 1180px) {
  .module-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .module-card-grid,
  .calendar-weekdays,
  .calendar-grid {
    grid-template-columns: 1fr;
  }

  .panel-head,
  .trend-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
