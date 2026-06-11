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
      <BaseChart v-else :option="expenseOption" />
    </section>

    <section class="panel panel-padding wide">
      <h3>支出分类排行</h3>
      <el-empty v-if="!loading && expenseCategories.length === 0" description="暂无支出排行数据" />
      <div v-else class="rank-list">
        <div v-for="item in expenseCategories" :key="item.categoryId || item.categoryName || 'unknown'" class="rank-row">
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
import type { EChartsOption } from 'echarts';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import type { ExpenseCategoryStat, IncomeExpenseTrendPoint } from '@/services/statisticsApi';

const props = defineProps<{
  loading: boolean;
  selectedMonth: string;
  incomeExpenseTrend: IncomeExpenseTrendPoint[];
  expenseCategories: ExpenseCategoryStat[];
}>();

const incomeExpenseOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 44, right: 18, top: 36, bottom: 36 },
  xAxis: { type: 'category', data: props.incomeExpenseTrend.map((item) => item.month) },
  yAxis: { type: 'value' },
  series: [
    { name: '收入', type: 'bar', data: props.incomeExpenseTrend.map((item) => item.income), itemStyle: { color: chartColor('--xo-chart-green'), borderRadius: [10, 10, 0, 0] } },
    { name: '支出', type: 'bar', data: props.incomeExpenseTrend.map((item) => item.expense), itemStyle: { color: chartColor('--xo-chart-red'), borderRadius: [10, 10, 0, 0] } }
  ]
}));

const balanceOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 44, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: props.incomeExpenseTrend.map((item) => item.month) },
  yAxis: { type: 'value' },
  series: [{ name: '结余', type: 'line', smooth: true, data: props.incomeExpenseTrend.map((item) => item.balance), lineStyle: { color: chartColor('--xo-chart-blue'), width: 3 }, itemStyle: { color: chartColor('--xo-chart-blue') }, areaStyle: { color: chartColor('--xo-primary-soft') } }]
}));

const expenseOption = computed<EChartsOption>(() => ({
  color: [chartColor('--xo-chart-blue'), chartColor('--xo-chart-green'), chartColor('--xo-chart-purple'), chartColor('--xo-chart-yellow'), chartColor('--xo-chart-red')],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: '72%', data: props.expenseCategories.map((item) => ({ name: item.categoryName || '未分类', value: item.amount })) }]
}));

// 后端 percent 为业务占比，前端兜底到 0-100 避免进度条异常。
function safePercent(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return 0;
  }
  return Math.max(0, Math.min(100, Number(value)));
}

function chartColor(name: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
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
