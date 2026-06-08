// 首页 API：封装首页聚合概览。
import { request } from './http';

export interface DashboardOverview {
  totalAssets: number;
  netAssets: number;
  todayIncome: number;
  todayExpense: number;
  yesterdayIncome: number;
  yesterdayExpense: number;
  monthlyIncome: number;
  monthlyExpense: number;
  todayBalance: number;
  monthlyBalance: number;
  todayBalanceRateByIncome?: number | null;
  todayBalanceRateByExpense?: number | null;
  monthlyBalanceRateByIncome?: number | null;
  monthlyBalanceRateByExpense?: number | null;
  investmentMarketValue: number;
  investmentFloatingProfit: number;
  investmentTotalProfit: number;
  investmentYesterdayProfit?: number | null;
  investmentTodayProfit: number | null;
  budgetUsageRate: number;
  assetTrendRate?: number | null;
  incomeTrendRate?: number | null;
  expenseTrendRate?: number | null;
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