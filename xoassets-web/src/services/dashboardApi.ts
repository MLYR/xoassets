// 首页 API：封装首页聚合概览。
import { request } from './http';

/** 首页概览数据。 */
export interface DashboardOverview {
  /** 总资产。 */
  totalAssets: number;
  /** 净资产。 */
  netAssets: number;
  /** 今日收入。 */
  todayIncome: number;
  /** 今日支出。 */
  todayExpense: number;
  /** 昨日收入。 */
  yesterdayIncome: number;
  /** 昨日支出。 */
  yesterdayExpense: number;
  /** 当月收入。 */
  monthlyIncome: number;
  /** 当月支出。 */
  monthlyExpense: number;
  /** 今日结余。 */
  todayBalance: number;
  /** 当月结余。 */
  monthlyBalance: number;
  /** 今日结余率，按收入作分母。 */
  todayBalanceRateByIncome?: number | null;
  /** 今日结余率，按支出作分母。 */
  todayBalanceRateByExpense?: number | null;
  /** 当月结余率，按收入作分母。 */
  monthlyBalanceRateByIncome?: number | null;
  /** 当月结余率，按支出作分母。 */
  monthlyBalanceRateByExpense?: number | null;
  /** 投资资产市值。 */
  investmentMarketValue: number;
  /** 投资浮动盈亏。 */
  investmentFloatingProfit: number;
  /** 投资总收益。 */
  investmentTotalProfit: number;
  /** 投资昨日收益。 */
  investmentYesterdayProfit?: number | null;
  /** 投资今日收益。 */
  investmentTodayProfit: number | null;
  /** 预算使用率。 */
  budgetUsageRate: number;
  /** 资产趋势率。 */
  assetTrendRate?: number | null;
  /** 收入趋势率。 */
  incomeTrendRate?: number | null;
  /** 支出趋势率。 */
  expenseTrendRate?: number | null;
  /** 结余趋势率。 */
  balanceTrendRate?: number | null;
}

export const dashboardApi = {
  // 查询首页聚合概览，不传月份时后端默认当前月。
  overview(month?: string) {
    return request<DashboardOverview>({
      url: '/dashboard/overview',
      method: 'GET',
      params: month ? { month } : undefined
    });
  }
};
