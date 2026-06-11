// 数据分析页数据中枢：统一管理筛选条件、日期区间和现有统计接口请求。
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { analyticsApi, type AnalyticsInvestment, type AnalyticsOverviewParams } from '@/services/analyticsApi';
import { snapshotApi, type AssetSnapshotItem } from '@/services/snapshotApi';
import { statisticsApi, type AssetDistributionItem, type ExpenseCategoryStat, type IncomeExpenseTrendPoint, type InvestmentProfitTrendPoint } from '@/services/statisticsApi';
import { investmentApi, type HoldingItem, type InvestmentCalendarDayProfit, type InvestmentOverview, type InvestmentTrend } from '@/services/investmentApi';
import type { BudgetSummary } from '@/services/budgetApi';

export type AnalyticsPeriod = '本月' | '近三月' | '近半年' | '全年';
export type AnalyticsInvestmentModule = 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO';
export type AnalyticsInvestmentPeriod = 'WEEK' | 'MONTH' | 'QUARTER' | 'YEAR';

export const ANALYTICS_PERIOD_OPTIONS: AnalyticsPeriod[] = ['本月', '近三月', '近半年', '全年'];
export const ANALYTICS_INVESTMENT_MODULE_OPTIONS: Array<{ label: string; value: AnalyticsInvestmentModule }> = [
  { label: '全部', value: 'ALL' },
  { label: '基金', value: 'FUND' },
  { label: '股票', value: 'STOCK' },
  { label: '虚拟货币', value: 'CRYPTO' }
];
export const ANALYTICS_INVESTMENT_PERIOD_OPTIONS: Array<{ label: string; value: AnalyticsInvestmentPeriod }> = [
  { label: '周', value: 'WEEK' },
  { label: '月', value: 'MONTH' },
  { label: '季', value: 'QUARTER' },
  { label: '年', value: 'YEAR' }
];

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
  const investmentLoading = ref(false);
  const investmentFailed = ref(false);
  const currentInvestmentModule = ref<AnalyticsInvestmentModule>('ALL');
  const currentInvestmentPeriod = ref<AnalyticsInvestmentPeriod>('MONTH');
  const investmentOverview = ref<InvestmentOverview | null>(null);
  const investmentModuleTrend = ref<InvestmentTrend | null>(null);
  const investmentDailyProfit = ref<InvestmentCalendarDayProfit[]>([]);
  const investmentModuleHoldings = ref<HoldingItem[]>([]);

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
    investmentLoading.value = true;
    try {
      const overview = await analyticsApi.overview(analyticsParams());
      applyAnalyticsOverview(overview);
    } catch (error) {
      await loadLegacyAnalytics();
    } finally {
      loading.value = false;
      investmentLoading.value = false;
    }
  }

  async function loadInvestmentAnalytics() {
    investmentLoading.value = true;
    investmentFailed.value = false;
    try {
      const overview = await analyticsApi.overview(analyticsParams());
      applyInvestmentAnalytics(overview.investment);
      investmentLoading.value = false;
      return;
    } catch {
      // 聚合接口失败时回退投资模块旧接口，保证第三阶段上线可渐进。
    }

    await loadLegacyInvestmentAnalytics();
    investmentLoading.value = false;
  }

  async function loadLegacyAnalytics() {
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
      await loadLegacyInvestmentAnalytics();
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '统计数据加载失败');
    }
  }

  async function loadLegacyInvestmentAnalytics() {
    investmentFailed.value = false;
    const { year, month } = selectedMonthParts(selectedMonth.value);

    const [overview, trend, dailyProfit, holdings] = await Promise.all([
      safeInvestmentRequest(investmentApi.overviewInvestments()),
      safeInvestmentRequest(investmentApi.trendInvestments({ module: currentInvestmentModule.value, period: currentInvestmentPeriod.value })),
      safeInvestmentRequest(investmentApi.dailyProfitCalendar({ year, month })),
      safeInvestmentRequest(investmentApi.listInvestmentHoldings({ module: currentInvestmentModule.value }))
    ]);

    investmentOverview.value = overview.value ?? null;
    investmentModuleTrend.value = trend.value ?? null;
    investmentDailyProfit.value = Array.isArray(dailyProfit.value) ? dailyProfit.value : [];
    investmentModuleHoldings.value = Array.isArray(holdings.value) ? holdings.value : [];

    const failedCount = [overview, trend, dailyProfit, holdings].filter((item) => item.failed).length;
    investmentFailed.value = failedCount === 4;
    if (failedCount > 0) {
      ElMessage.warning('部分投资分析数据加载失败，已隐藏不可用区域');
    }
  }

  // 生成聚合接口参数，确保筛选条件和旧接口回退使用同一范围。
  function analyticsParams(): AnalyticsOverviewParams {
    const range = selectedRange.value;
    return {
      startDate: range.startDate,
      endDate: range.endDate,
      startMonth: range.startMonth,
      endMonth: range.endMonth,
      selectedMonth: selectedMonth.value,
      investmentModule: currentInvestmentModule.value,
      investmentPeriod: currentInvestmentPeriod.value
    };
  }

  // 应用后端聚合返回，所有数组继续兜底为空数组。
  function applyAnalyticsOverview(overview: {
    assetTrend?: AssetSnapshotItem[];
    expenseCategories?: ExpenseCategoryStat[];
    incomeExpenseTrend?: IncomeExpenseTrendPoint[];
    assetDistribution?: AssetDistributionItem[];
    budgetSummary?: BudgetSummary | null;
    investment?: AnalyticsInvestment | null;
  }) {
    netAssetsTrend.value = Array.isArray(overview.assetTrend) ? overview.assetTrend : [];
    expenseCategories.value = Array.isArray(overview.expenseCategories) ? overview.expenseCategories : [];
    incomeExpenseTrend.value = Array.isArray(overview.incomeExpenseTrend) ? overview.incomeExpenseTrend : [];
    assetDistribution.value = Array.isArray(overview.assetDistribution) ? overview.assetDistribution : [];
    budgetSummary.value = overview.budgetSummary ?? emptyBudgetSummary(selectedMonth.value);
    investmentTrend.value = [];
    applyInvestmentAnalytics(overview.investment ?? null);
  }

  // 把聚合投资对象映射到现有投资 Tab 使用的数据形状。
  function applyInvestmentAnalytics(investment: AnalyticsInvestment | null) {
    if (!investment) {
      investmentFailed.value = true;
      investmentOverview.value = null;
      investmentModuleTrend.value = null;
      investmentDailyProfit.value = [];
      investmentModuleHoldings.value = [];
      return;
    }
    investmentFailed.value = false;
    investmentOverview.value = {
      totalInvestmentAsset: investment.totalInvestmentAsset,
      totalCost: 0,
      holdingProfit: investment.holdingProfit,
      holdingProfitRate: investment.holdingProfitRate,
      todayProfit: investment.todayProfit,
      todayProfitAvailable: investment.todayProfitAvailable,
      todayProfitAssetScope: investment.todayProfitStatusLabel || '今日有效价资产',
      todayProfitStatusLabel: investment.todayProfitStatusLabel,
      yesterdayProfit: investment.yesterdayProfit,
      yesterdayProfitAssetScope: '收益日历聚合',
      moduleAssets: Array.isArray(investment.moduleAssets) ? investment.moduleAssets : []
    };
    investmentModuleTrend.value = investment.trend ?? null;
    investmentDailyProfit.value = Array.isArray(investment.dailyProfitCalendar) ? investment.dailyProfitCalendar : [];
    investmentModuleHoldings.value = Array.isArray(investment.holdings) ? investment.holdings : [];
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

// 投资分析允许局部失败，避免一个投资接口异常拖垮整个分析页。
async function safeInvestmentRequest<T>(request: Promise<T>): Promise<{ value: T | null; failed: boolean }> {
  try {
    return { value: await request, failed: false };
  } catch {
    return { value: null, failed: true };
  }
}

// 解析 YYYY-MM 给投资收益日历接口使用，异常时回到当前月。
function selectedMonthParts(value: string) {
  const [yearText, monthText] = value.split('-');
  const year = Number(yearText);
  const month = Number(monthText);
  if (Number.isNaN(year) || Number.isNaN(month) || month < 1 || month > 12) {
    const now = new Date();
    return { year: now.getFullYear(), month: now.getMonth() + 1 };
  }
  return { year, month };
}
