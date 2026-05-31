// 首页 API：封装概览、最近流水和最近投资交易。
import { request } from './http';
import type { InvestmentTransactionItem } from './investmentApi';
import type { TransactionItem } from './transactionApi';

export interface DashboardOverview {
  totalAssets: number;
  netAssets: number;
  todayExpense: number;
  monthlyIncome: number;
  monthlyExpense: number;
  monthlyBalance: number;
  investmentMarketValue: number;
  investmentFloatingProfit: number;
  budgetUsageRate: number;
  assetTrendRate: number;
  incomeTrendRate: number;
  expenseTrendRate: number;
  balanceTrendRate: number;
  recentTransactions: TransactionItem[];
  recentInvestmentTransactions: InvestmentTransactionItem[];
}

export const dashboardApi = {
  // 查询首页聚合概览，不传月份时后端默认当前月。
  overview(month?: string) {
    return request<DashboardOverview>({
      url: '/dashboard/overview',
      method: 'GET',
      params: month ? { month } : undefined
    });
  },
  // 保留独立最近流水接口，其他页面需要时可复用。
  recentTransactions(limit = 5) {
    return request<TransactionItem[]>({
      url: '/dashboard/recent-transactions',
      method: 'GET',
      params: { limit }
    });
  }
};
