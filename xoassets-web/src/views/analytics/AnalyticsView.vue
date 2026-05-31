<!-- 数据分析页：集中展示真实资产、收支、预算和投资图表。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">数据分析</h1>
        <p class="page-subtitle">从趋势、分类和结余三个维度观察财务表现</p>
      </div>
      <el-segmented v-model="period" :options="['本月', '近三月', '全年']" />
    </div>

    <section class="grid-3" v-loading="loading">
      <MetricCard title="平均日支出" :value="averageDailyExpense" :trend="0" description="当前周期" tone="warning" />
      <MetricCard title="本期结余" :value="periodBalance" :trend="0" description="收入 - 支出" :tone="periodBalance >= 0 ? 'success' : 'danger'" />
      <MetricCard title="预算剩余" :value="budgetRemaining" :trend="0" description="本月预算" tone="success" />
    </section>

    <section class="grid-2">
      <div class="panel panel-padding">
        <h3>净资产趋势</h3>
        <BaseChart :option="assetOption" />
      </div>
      <div class="panel panel-padding">
        <h3>支出分类</h3>
        <BaseChart :option="expenseOption" />
      </div>
      <div class="panel panel-padding">
        <h3>资产分布</h3>
        <BaseChart :option="assetDistributionOption" />
      </div>
      <div class="panel panel-padding">
        <h3>投资盈亏</h3>
        <BaseChart :option="investmentOption" />
      </div>
      <div class="panel panel-padding wide">
        <h3>收支趋势与预算</h3>
        <BaseChart :option="incomeExpenseOption" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
// 分析页使用 statistics 接口，所有统计都按当前登录用户由后端隔离。
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import type { EChartsOption } from 'echarts';
import BaseChart from '@/components/charts/BaseChart.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import { statisticsApi, type AssetDistributionItem, type ExpenseCategoryStat, type IncomeExpenseTrendPoint, type InvestmentProfitTrendPoint, type TrendPoint } from '@/services/statisticsApi';
import type { BudgetSummary } from '@/services/budgetApi';

const period = ref('本月');
const loading = ref(false);
const netAssetsTrend = ref<TrendPoint[]>([]);
const expenseCategories = ref<ExpenseCategoryStat[]>([]);
const incomeExpenseTrend = ref<IncomeExpenseTrendPoint[]>([]);
const assetDistribution = ref<AssetDistributionItem[]>([]);
const investmentTrend = ref<InvestmentProfitTrendPoint[]>([]);
const budgetSummary = ref<BudgetSummary>({ month: currentMonth(), totalBudget: 0, totalUsed: 0, totalRemaining: 0, usageRate: 0, usageStatus: 'NORMAL', usageStatusLabel: '正常', items: [] });

onMounted(() => {
  loadAnalytics();
});

watch(period, () => {
  loadAnalytics();
});

const periodBalance = computed(() => incomeExpenseTrend.value.reduce((sum, item) => sum + Number(item.balance), 0));
const periodExpense = computed(() => incomeExpenseTrend.value.reduce((sum, item) => sum + Number(item.expense), 0));
const averageDailyExpense = computed(() => periodExpense.value / Math.max(1, selectedDays()));
const budgetRemaining = computed(() => budgetSummary.value.totalRemaining);

const assetOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 44, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: netAssetsTrend.value.map((item) => item.date) },
  yAxis: { type: 'value' },
  series: [{ type: 'line', smooth: true, data: netAssetsTrend.value.map((item) => item.value), lineStyle: { color: '#3b82f6', width: 3 }, itemStyle: { color: '#3b82f6' } }]
}));

const expenseOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: '72%', data: expenseCategories.value.map((item) => ({ name: item.categoryName || '未分类', value: item.amount })) }]
}));

const assetDistributionOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6'],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['44%', '72%'], data: assetDistribution.value.map((item) => ({ name: item.name, value: item.value })) }]
}));

const investmentOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 44, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: investmentTrend.value.map((item) => item.month) },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: investmentTrend.value.map((item) => item.floatingProfit), itemStyle: { color: '#10b981', borderRadius: [6, 6, 0, 0] } }]
}));

const incomeExpenseOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 44, right: 18, top: 36, bottom: 36 },
  xAxis: { type: 'category', data: incomeExpenseTrend.value.map((item) => item.month) },
  yAxis: { type: 'value' },
  series: [
    { name: '收入', type: 'bar', data: incomeExpenseTrend.value.map((item) => item.income), itemStyle: { color: '#10b981' } },
    { name: '支出', type: 'bar', data: incomeExpenseTrend.value.map((item) => item.expense), itemStyle: { color: '#ef4444' } },
    { name: '预算使用率', type: 'line', data: incomeExpenseTrend.value.map(() => budgetSummary.value.usageRate), lineStyle: { color: '#f59e0b', width: 3 }, itemStyle: { color: '#f59e0b' } }
  ]
}));

async function loadAnalytics() {
  loading.value = true;
  try {
    const range = selectedRange();
    const [netAssets, expenses, incomeExpense, distribution, investment, budget] = await Promise.all([
      statisticsApi.netAssetsTrend({ startDate: range.startDate, endDate: range.endDate }),
      statisticsApi.expenseCategory(currentMonth()),
      statisticsApi.incomeExpenseTrend({ startMonth: range.startMonth, endMonth: range.endMonth }),
      statisticsApi.assetDistribution(),
      statisticsApi.investmentProfitTrend({ startMonth: range.startMonth, endMonth: range.endMonth }),
      statisticsApi.budgetProgress(currentMonth())
    ]);
    netAssetsTrend.value = netAssets;
    expenseCategories.value = expenses;
    incomeExpenseTrend.value = incomeExpense;
    assetDistribution.value = distribution;
    investmentTrend.value = investment;
    budgetSummary.value = budget;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '统计数据加载失败');
  } finally {
    loading.value = false;
  }
}

function selectedRange() {
  const days = selectedDays();
  return {
    startDate: dateBefore(days - 1),
    endDate: dateBefore(0),
    startMonth: monthBefore(period.value === '全年' ? 11 : period.value === '近三月' ? 2 : 0),
    endMonth: currentMonth()
  };
}

function selectedDays() {
  if (period.value === '全年') {
    return 365;
  }
  if (period.value === '近三月') {
    return 90;
  }
  return new Date().getDate();
}

function currentMonth() {
  const date = new Date();
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}`;
}

function monthBefore(months: number) {
  const date = new Date();
  date.setMonth(date.getMonth() - months);
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}`;
}

function dateBefore(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`;
}
</script>

<style scoped>
/* 分析页图表以双列为主，综合趋势跨两列形成主次层级。 */
h3 {
  margin: 0 0 16px;
  font-size: 18px;
}

.wide {
  grid-column: 1 / -1;
}
</style>
