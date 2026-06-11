<!-- 数据分析页：集中展示真实资产、收支、预算和投资图表。 -->
<template>
  <div class="page analytics-page">
    <AnalysisToolbar v-model:period="period" v-model:selected-month="selectedMonth" :loading="loading" @refresh="loadAnalytics" />

    <AnalysisKpiGrid
      v-loading="loading"
      :latest-net-asset="latestNetAsset"
      :period-balance="periodBalance"
      :latest-investment-profit="latestInvestmentProfit"
      :budget-remaining="budgetRemaining"
    />

    <el-tabs v-model="activeTab" class="analytics-tabs">
      <el-tab-pane label="总览" name="overview">
        <OverviewAnalysis :loading="loading" :net-assets-trend="netAssetsTrend" :income-expense-trend="incomeExpenseTrend" :asset-distribution="assetDistribution" />
      </el-tab-pane>
      <el-tab-pane label="资产分析" name="asset">
        <AssetAnalysis :loading="loading" :net-assets-trend="netAssetsTrend" :asset-distribution="assetDistribution" />
      </el-tab-pane>
      <el-tab-pane label="收支分析" name="cashflow">
        <CashflowAnalysis :loading="loading" :selected-month="selectedMonth" :income-expense-trend="incomeExpenseTrend" :expense-categories="expenseCategories" />
      </el-tab-pane>
      <el-tab-pane label="投资分析" name="investment">
        <InvestmentAnalysis
          v-model:investment-module="currentInvestmentModule"
          v-model:investment-period="currentInvestmentPeriod"
          :loading="investmentLoading"
          :failed="investmentFailed"
          :overview="investmentOverview"
          :module-trend="investmentModuleTrend"
          :daily-profit="investmentDailyProfit"
          :holdings="investmentModuleHoldings"
          @refresh="loadInvestmentAnalytics"
        />
      </el-tab-pane>
      <el-tab-pane label="预算分析" name="budget">
        <BudgetAnalysis :loading="loading" :selected-month="selectedMonth" :budget-summary="budgetSummary" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
// 分析页容器只编排筛选、KPI 和 Tab，数据请求集中在 useAnalyticsData。
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AnalysisToolbar from './components/AnalysisToolbar.vue';
import AnalysisKpiGrid from './components/AnalysisKpiGrid.vue';
import OverviewAnalysis from './components/OverviewAnalysis.vue';
import AssetAnalysis from './components/AssetAnalysis.vue';
import CashflowAnalysis from './components/CashflowAnalysis.vue';
import InvestmentAnalysis from './components/InvestmentAnalysis.vue';
import BudgetAnalysis from './components/BudgetAnalysis.vue';
import { ANALYTICS_PERIOD_OPTIONS, useAnalyticsData, type AnalyticsPeriod } from './composables/useAnalyticsData';

const route = useRoute();
const router = useRouter();
const activeTab = ref(routeTab(route.query.tab));
const {
  loading,
  period,
  selectedMonth,
  netAssetsTrend,
  expenseCategories,
  incomeExpenseTrend,
  assetDistribution,
  investmentTrend,
  budgetSummary,
  investmentLoading,
  investmentFailed,
  currentInvestmentModule,
  currentInvestmentPeriod,
  investmentOverview,
  investmentModuleTrend,
  investmentDailyProfit,
  investmentModuleHoldings,
  loadAnalytics,
  loadInvestmentAnalytics
} = useAnalyticsData();

period.value = routePeriod(route.query.period);
selectedMonth.value = routeMonth(route.query.month);

onMounted(() => {
  loadAnalytics();
});

watch([period, selectedMonth], () => {
  loadAnalytics();
});

watch([currentInvestmentModule, currentInvestmentPeriod], () => {
  loadInvestmentAnalytics();
});

watch([activeTab, period, selectedMonth], () => {
  syncAnalyticsQuery();
});

const latestNetAsset = computed(() => latestOf(netAssetsTrend.value)?.netAsset ?? null);
const periodBalance = computed(() => incomeExpenseTrend.value.reduce((sum, item) => sum + Number(item.balance), 0));
const latestInvestmentProfit = computed(() => investmentOverview.value?.holdingProfit ?? latestOf(investmentTrend.value)?.floatingProfit ?? null);
const budgetRemaining = computed(() => budgetSummary.value.totalRemaining);

// 取趋势最后一个点作为最新 KPI，空数组返回 null 供金额组件展示 --。
function latestOf<T>(items: T[]): T | null {
  return items.length > 0 ? items[items.length - 1] : null;
}

// 从 query 恢复 Tab，非法值回到总览。
function routeTab(value: unknown) {
  const tab = Array.isArray(value) ? value[0] : value;
  return ['overview', 'asset', 'cashflow', 'investment', 'budget'].includes(String(tab)) ? String(tab) : 'overview';
}

// 从 query 恢复周期，非法值回到本月。
function routePeriod(value: unknown): AnalyticsPeriod {
  const periodValue = Array.isArray(value) ? value[0] : value;
  return ANALYTICS_PERIOD_OPTIONS.includes(periodValue as AnalyticsPeriod) ? periodValue as AnalyticsPeriod : '本月';
}

// 从 query 恢复月份，非法值回到当前月份。
function routeMonth(value: unknown) {
  const month = Array.isArray(value) ? value[0] : value;
  return typeof month === 'string' && /^\d{4}-\d{2}$/.test(month) ? month : currentMonth();
}

// 分析页筛选状态写入 URL，刷新或返回时保留当前分析上下文。
function syncAnalyticsQuery() {
  const nextQuery = {
    ...route.query,
    tab: activeTab.value,
    period: period.value,
    month: selectedMonth.value
  };
  if (route.query.tab === nextQuery.tab && route.query.period === nextQuery.period && route.query.month === nextQuery.month) {
    return;
  }
  router.replace({ path: route.path, query: nextQuery });
}

function currentMonth() {
  const date = new Date();
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}`;
}
</script>

<style>
/* 分析页 Tab 内容共用双列网格，子组件只专注具体图表和列表。 */
.analytics-page .analytics-tabs {
  min-width: 0;
}

.analytics-page .el-tabs__content {
  overflow: visible;
}

.analytics-page .analysis-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.analytics-page .analysis-grid h3 {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 800;
}

.analytics-page .analysis-grid .wide {
  grid-column: 1 / -1;
}

@media (max-width: 860px) {
  .analytics-page .analysis-grid {
    grid-template-columns: 1fr;
  }
}
</style>
