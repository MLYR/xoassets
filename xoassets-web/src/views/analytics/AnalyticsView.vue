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
        <InvestmentAnalysis :loading="loading" :investment-trend="investmentTrend" />
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
import AnalysisToolbar from './components/AnalysisToolbar.vue';
import AnalysisKpiGrid from './components/AnalysisKpiGrid.vue';
import OverviewAnalysis from './components/OverviewAnalysis.vue';
import AssetAnalysis from './components/AssetAnalysis.vue';
import CashflowAnalysis from './components/CashflowAnalysis.vue';
import InvestmentAnalysis from './components/InvestmentAnalysis.vue';
import BudgetAnalysis from './components/BudgetAnalysis.vue';
import { useAnalyticsData } from './composables/useAnalyticsData';

const activeTab = ref('overview');
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
  loadAnalytics
} = useAnalyticsData();

onMounted(() => {
  loadAnalytics();
});

watch([period, selectedMonth], () => {
  loadAnalytics();
});

const latestNetAsset = computed(() => latestOf(netAssetsTrend.value)?.netAsset ?? null);
const periodBalance = computed(() => incomeExpenseTrend.value.reduce((sum, item) => sum + Number(item.balance), 0));
const latestInvestmentProfit = computed(() => latestOf(investmentTrend.value)?.floatingProfit ?? null);
const budgetRemaining = computed(() => budgetSummary.value.totalRemaining);

// 取趋势最后一个点作为最新 KPI，空数组返回 null 供金额组件展示 --。
function latestOf<T>(items: T[]): T | null {
  return items.length > 0 ? items[items.length - 1] : null;
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
