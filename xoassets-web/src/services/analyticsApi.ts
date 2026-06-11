// 数据分析聚合 API：优先用于 /analytics 页面，失败时页面回退到旧统计接口。
import { request } from './http';
import type { BudgetSummary } from './budgetApi';
import type { HoldingItem, InvestmentCalendarDayProfit, InvestmentModuleAsset, InvestmentTrend } from './investmentApi';
import type { AssetSnapshotItem } from './snapshotApi';
import type { AssetDistributionItem, ExpenseCategoryStat, IncomeExpenseTrendPoint } from './statisticsApi';

/** 数据分析聚合查询参数。 */
export interface AnalyticsOverviewParams {
  /** 资产趋势开始日期。 */
  startDate?: string;
  /** 资产趋势结束日期。 */
  endDate?: string;
  /** 收支趋势开始月份。 */
  startMonth?: string;
  /** 收支趋势结束月份。 */
  endMonth?: string;
  /** 支出分类、预算和每日收益月份。 */
  selectedMonth?: string;
  /** 投资模块。 */
  investmentModule?: 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO';
  /** 投资趋势周期。 */
  investmentPeriod?: 'WEEK' | 'MONTH' | 'QUARTER' | 'YEAR';
}

/** 数据分析 KPI。 */
export interface AnalyticsKpi {
  netAsset: number;
  totalAsset: number;
  periodIncome: number;
  periodExpense: number;
  periodBalance: number;
  investmentAsset: number;
  investmentProfit: number;
  budgetRemaining: number;
}

/** 聚合接口中的投资分析对象。 */
export interface AnalyticsInvestment {
  totalInvestmentAsset: number;
  holdingProfit: number;
  holdingProfitRate: number;
  todayProfit?: number | null;
  todayProfitAvailable?: boolean | null;
  todayProfitStatusLabel?: string | null;
  yesterdayProfit?: number | null;
  moduleAssets: InvestmentModuleAsset[];
  trend: InvestmentTrend;
  dailyProfitCalendar: InvestmentCalendarDayProfit[];
  holdings: HoldingItem[];
}

/** 数据分析聚合返回。 */
export interface AnalyticsOverview {
  kpi: AnalyticsKpi;
  assetTrend: AssetSnapshotItem[];
  incomeExpenseTrend: IncomeExpenseTrendPoint[];
  expenseCategories: ExpenseCategoryStat[];
  assetDistribution: AssetDistributionItem[];
  budgetSummary: BudgetSummary;
  investment: AnalyticsInvestment;
}

export const analyticsApi = {
  // 查询分析页聚合总览。
  overview(params: AnalyticsOverviewParams) {
    return request<AnalyticsOverview>({
      url: '/analytics/overview',
      method: 'GET',
      params
    });
  }
};
