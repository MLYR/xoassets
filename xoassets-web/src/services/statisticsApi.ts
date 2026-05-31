// 统计 API：封装首页和数据分析页使用的图表数据。
import { request } from './http';
import type { BudgetSummary } from './budgetApi';

export interface TrendPoint {
  date: string;
  value: number;
}

export interface ExpenseCategoryStat {
  categoryId?: string | null;
  categoryName?: string | null;
  amount: number;
  percent: number;
}

export interface IncomeExpenseTrendPoint {
  month: string;
  income: number;
  expense: number;
  balance: number;
}

export interface AssetDistributionItem {
  name: string;
  type: string;
  value: number;
  percent: number;
}

export interface InvestmentProfitTrendPoint {
  month: string;
  marketValue: number;
  totalCost: number;
  floatingProfit: number;
}

export const statisticsApi = {
  netAssetsTrend(params?: { startDate?: string; endDate?: string }) {
    return request<TrendPoint[]>({
      url: '/statistics/net-assets-trend',
      method: 'GET',
      params
    });
  },
  incomeExpenseTrend(params?: { startMonth?: string; endMonth?: string }) {
    return request<IncomeExpenseTrendPoint[]>({
      url: '/statistics/income-expense-trend',
      method: 'GET',
      params
    });
  },
  expenseCategory(month?: string) {
    return request<ExpenseCategoryStat[]>({
      url: '/statistics/expense-category',
      method: 'GET',
      params: month ? { month } : undefined
    });
  },
  assetDistribution() {
    return request<AssetDistributionItem[]>({
      url: '/statistics/asset-distribution',
      method: 'GET'
    });
  },
  investmentProfitTrend(params?: { startMonth?: string; endMonth?: string }) {
    return request<InvestmentProfitTrendPoint[]>({
      url: '/statistics/investment-profit-trend',
      method: 'GET',
      params
    });
  },
  budgetProgress(month?: string) {
    return request<BudgetSummary>({
      url: '/statistics/budget-progress',
      method: 'GET',
      params: month ? { month } : undefined
    });
  }
};
