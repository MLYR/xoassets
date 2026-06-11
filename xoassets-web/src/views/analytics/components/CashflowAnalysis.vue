<!-- 收支分析：展示趋势、结余、支出分类和排行。 -->
<template>
  <div class="analysis-grid">
    <section class="panel panel-padding wide">
      <h3>收入 / 支出趋势</h3>
      <el-empty v-if="!loading && incomeExpenseTrend.length === 0" description="暂无收支趋势数据" />
      <BaseChart v-else :option="incomeExpenseOption" height="320px" />
    </section>

    <section class="panel panel-padding">
      <h3>结余趋势</h3>
      <el-empty v-if="!loading && incomeExpenseTrend.length === 0" description="暂无结余趋势数据" />
      <BaseChart v-else :option="balanceOption" />
    </section>

    <section class="panel panel-padding">
      <h3>{{ selectedMonth }} 支出分类</h3>
      <el-empty v-if="!loading && expenseCategories.length === 0" description="暂无支出分类数据" />
      <BaseChart v-else :option="expenseOption" @chart-click="handleExpenseChartClick" />
    </section>

    <section class="panel panel-padding wide">
      <h3>支出分类排行</h3>
      <el-empty v-if="!loading && expenseCategories.length === 0" description="暂无支出排行数据" />
      <div v-else class="rank-list">
        <div v-for="item in expenseCategories" :key="item.categoryId || item.categoryName || 'unknown'" class="rank-row" @click="openExpenseTransactions(item)">
          <div class="rank-name">{{ item.categoryName || '未分类' }}</div>
          <el-progress :percentage="safePercent(item.percent)" :show-text="false" />
          <div class="rank-value">
            <AmountText :value="item.amount" />
            <span>{{ safePercent(item.percent).toFixed(1) }}%</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
// 收支 Tab 的月份只影响分类数据，趋势仍按顶部周期展示。
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import type { EChartsOption } from 'echarts';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import { ROUTES } from '@/constants/routes';
import type { ExpenseCategoryStat, IncomeExpenseTrendPoint } from '@/services/statisticsApi';
import { amountTooltip, categoryAxis, chartColor, chartLegend, valueAxis } from './chartFormat';

const props = defineProps<{
  loading: boolean;
  selectedMonth: string;
  incomeExpenseTrend: IncomeExpenseTrendPoint[];
  expenseCategories: ExpenseCategoryStat[];
}>();
const router = useRouter();

const incomeExpenseOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis', valueFormatter: amountTooltip },
  legend: chartLegend(),
  grid: { left: 44, right: 18, top: 36, bottom: 36 },
  xAxis: categoryAxis(props.incomeExpenseTrend.map((item) => item.month)),
  yAxis: valueAxis(),
  series: [
    { name: '收入', type: 'bar', data: props.incomeExpenseTrend.map((item) => item.income), itemStyle: { color: chartColor('--xo-chart-green'), borderRadius: [10, 10, 0, 0] } },
    { name: '支出', type: 'bar', data: props.incomeExpenseTrend.map((item) => item.expense), itemStyle: { color: chartColor('--xo-chart-red'), borderRadius: [10, 10, 0, 0] } }
  ]
}));

const balanceOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis', valueFormatter: amountTooltip },
  grid: { left: 44, right: 18, top: 24, bottom: 36 },
  xAxis: categoryAxis(props.incomeExpenseTrend.map((item) => item.month)),
  yAxis: valueAxis(),
  series: [{ name: '结余', type: 'line', smooth: true, data: props.incomeExpenseTrend.map((item) => item.balance), lineStyle: { color: chartColor('--xo-chart-blue'), width: 3 }, itemStyle: { color: chartColor('--xo-chart-blue') }, areaStyle: { color: chartColor('--xo-primary-soft') } }]
}));

const expenseOption = computed<EChartsOption>(() => ({
  color: [chartColor('--xo-chart-blue'), chartColor('--xo-chart-green'), chartColor('--xo-chart-purple'), chartColor('--xo-chart-yellow'), chartColor('--xo-chart-red')],
  tooltip: { trigger: 'item', valueFormatter: amountTooltip },
  series: [{ type: 'pie', radius: '72%', data: props.expenseCategories.map((item) => ({ name: item.categoryName || '未分类', value: item.amount, categoryId: item.categoryId || null })) }]
}));

// 后端 percent 为业务占比，前端兜底到 0-100 避免进度条异常。
function safePercent(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return 0;
  }
  return Math.max(0, Math.min(100, Number(value)));
}

function handleExpenseChartClick(params: unknown) {
  const data = (params as { data?: { categoryId?: string | null } }).data;
  const category = props.expenseCategories.find((item) => item.categoryId === data?.categoryId);
  if (category) {
    openExpenseTransactions(category);
  }
}

function openExpenseTransactions(item: ExpenseCategoryStat) {
  if (!item.categoryId) {
    return;
  }
  router.push({
    path: ROUTES.transactions,
    query: {
      type: 'EXPENSE',
      categoryId: item.categoryId,
      month: props.selectedMonth
    }
  });
}
</script>

<style scoped>
/* 分类排行使用紧凑行，服务于扫描而不是装饰。 */
.rank-list {
  display: grid;
  gap: 14px;
}

.rank-row {
  display: grid;
  grid-template-columns: minmax(90px, 160px) minmax(120px, 1fr) minmax(140px, auto);
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.rank-row:hover {
  border-radius: var(--xo-radius-inner);
  background: var(--xo-primary-softer);
}

.rank-name {
  color: var(--xo-text);
  font-weight: 700;
}

.rank-value {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  color: var(--xo-muted);
  font-size: 13px;
}

@media (max-width: 760px) {
  .rank-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .rank-value {
    justify-content: space-between;
  }
}
</style>
