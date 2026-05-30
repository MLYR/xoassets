// 金融服务层：目前返回 mock 数据，后续可替换为真实 HTTP 请求。
import {
  accounts,
  assetTrend,
  budgets,
  dashboardMetrics,
  expenseBreakdown,
  goals,
  holdings,
  monthlyBalance,
  reports,
  transactions
} from '@/mock/finance';

// 页面统一从服务层取数，后续切换真实 API 时只需要改这里。
export const financeService = {
  // 首页聚合多个模块数据，最近流水只截取前 5 条。
  getDashboard() {
    return { dashboardMetrics, assetTrend, expenseBreakdown, transactions: transactions.slice(0, 5) };
  },
  // 获取流水列表。
  getTransactions() {
    return transactions;
  },
  // 获取账户列表。
  getAccounts() {
    return accounts;
  },
  // 获取投资持仓列表。
  getInvestments() {
    return holdings;
  },
  // 获取统计分析图表数据。
  getAnalytics() {
    return { assetTrend, expenseBreakdown, monthlyBalance };
  },
  // 获取 AI 报告列表。
  getReports() {
    return reports;
  },
  // 获取预算列表。
  getBudgets() {
    return budgets;
  },
  // 获取资产目标列表。
  getGoals() {
    return goals;
  }
};
