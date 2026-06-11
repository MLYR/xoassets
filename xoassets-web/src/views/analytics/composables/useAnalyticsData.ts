// 数据分析页数据中枢：统一管理筛选条件、日期区间和现有统计接口请求。
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { snapshotApi, type AssetSnapshotItem } from '@/services/snapshotApi';
import { statisticsApi, type AssetDistributionItem, type ExpenseCategoryStat, type IncomeExpenseTrendPoint, type InvestmentProfitTrendPoint } from '@/services/statisticsApi';
import type { BudgetSummary } from '@/services/budgetApi';

export type AnalyticsPeriod = '本月' | '近三月' | '近半年' | '全年';

export const ANALYTICS_PERIOD_OPTIONS: AnalyticsPeriod[] = ['本月', '近三月', '近半年', '全年'];

const emptyBudgetSummary = (month: string): BudgetSummary => ({
  month,
  totalBudget: 0,
  totalUsed: 0,
  totalRemaining: 0,
  usageRate: 0,
  usageStatus: 'NORMAL',
  usageStatusLabel: '正常',
  items: []
});

// 统一处理数据分析页请求，避免各 Tab 分散请求导致口径不一致。
export function useAnalyticsData() {
  const loading = ref(false);
  const period = ref<AnalyticsPeriod>('本月');
  const selectedMonth = ref(currentMonth());
  const netAssetsTrend = ref<AssetSnapshotItem[]>([]);
  const expenseCategories = ref<ExpenseCategoryStat[]>([]);
  const incomeExpenseTrend = ref<IncomeExpenseTrendPoint[]>([]);
  const assetDistribution = ref<AssetDistributionItem[]>([]);
  const investmentTrend = ref<InvestmentProfitTrendPoint[]>([]);
  const budgetSummary = ref<BudgetSummary>(emptyBudgetSummary(selectedMonth.value));

  const selectedRange = computed(() => {
    const days = selectedDays(period.value);
    const monthOffsetMap: Record<AnalyticsPeriod, number> = {
      本月: 0,
      近三月: 2,
      近半年: 5,
      全年: 11
    };

    return {
      startDate: dateBefore(days - 1),
      endDate: dateBefore(0),
      startMonth: monthBefore(monthOffsetMap[period.value]),
      endMonth: currentMonth()
    };
  });

  async function loadAnalytics() {
    loading.value = true;
    try {
      const range = selectedRange.value;
      const [netAssets, expenses, incomeExpense, distribution, investment, budget] = await Promise.all([
        snapshotApi.trend({ startDate: range.startDate, endDate: range.endDate }),
        statisticsApi.expenseCategory(selectedMonth.value),
        statisticsApi.incomeExpenseTrend({ startMonth: range.startMonth, endMonth: range.endMonth }),
        statisticsApi.assetDistribution(),
        statisticsApi.investmentProfitTrend({ startMonth: range.startMonth, endMonth: range.endMonth }),
        statisticsApi.budgetProgress(selectedMonth.value)
      ]);

      netAssetsTrend.value = Array.isArray(netAssets) ? netAssets : [];
      expenseCategories.value = Array.isArray(expenses) ? expenses : [];
      incomeExpenseTrend.value = Array.isArray(incomeExpense) ? incomeExpense : [];
      assetDistribution.value = Array.isArray(distribution) ? distribution : [];
      investmentTrend.value = Array.isArray(investment) ? investment : [];
      budgetSummary.value = budget ?? emptyBudgetSummary(selectedMonth.value);
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '统计数据加载失败');
    } finally {
      loading.value = false;
    }
  }

  return {
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
  };
}

// 根据周期计算趋势天数，资产快照使用自然日范围。
function selectedDays(period: AnalyticsPeriod) {
  if (period === '全年') {
    return 365;
  }
  if (period === '近半年') {
    return 180;
  }
  if (period === '近三月') {
    return 90;
  }
  return new Date().getDate();
}

// 获取当前月份。
function currentMonth() {
  const date = new Date();
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`;
}

// 计算指定月数前的月份。
function monthBefore(months: number) {
  const date = new Date();
  date.setMonth(date.getMonth() - months);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`;
}

// 计算指定天数前的日期。
function dateBefore(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function pad(value: number) {
  return `${value}`.padStart(2, '0');
}
