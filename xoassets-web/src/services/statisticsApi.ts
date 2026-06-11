// 统计 API：封装首页和数据分析页使用的图表数据。
import { request } from './http';
import type { BudgetSummary } from './budgetApi';

/** 趋势图点位。 */
export interface TrendPoint {
  /** 日期。 */
  date: string;
  /** 数值。 */
  value: number;
}

/** 支出分类统计项。 */
export interface ExpenseCategoryStat {
  /** 分类ID。 */
  categoryId?: string | null;
  /** 分类名称。 */
  categoryName?: string | null;
  /** 金额。 */
  amount: number;
  /** 占比。 */
  percent: number;
}

/** 收支趋势点。 */
export interface IncomeExpenseTrendPoint {
  /** 月份。 */
  month: string;
  /** 收入。 */
  income: number;
  /** 支出。 */
  expense: number;
  /** 余额。 */
  balance: number;
}

/** 资产分布项。 */
export interface AssetDistributionItem {
  /** 名称。 */
  name: string;
  /** 类型。 */
  type: string;
  /** 下钻引用ID。 */
  refId?: string | null;
  /** 下钻引用类型。 */
  refType?: 'ACCOUNT' | 'HOLDING' | string | null;
  /** 数值。 */
  value: number;
  /** 占比。 */
  percent: number;
}

/** 投资收益趋势点。 */
export interface InvestmentProfitTrendPoint {
  /** 月份。 */
  month: string;
  /** 市值。 */
  marketValue: number;
  /** 总成本。 */
  totalCost: number;
  /** 浮动盈亏。 */
  floatingProfit: number;
}

export const statisticsApi = {
  // 查询净资产趋势。
  netAssetsTrend(params?: { startDate?: string; endDate?: string }) {
    return request<TrendPoint[]>({
      url: '/statistics/net-assets-trend',
      method: 'GET',
      params
    });
  },
  // 查询收支趋势。
  incomeExpenseTrend(params?: { startMonth?: string; endMonth?: string }) {
    return request<IncomeExpenseTrendPoint[]>({
      url: '/statistics/income-expense-trend',
      method: 'GET',
      params
    });
  },
  // 查询支出分类。
  expenseCategory(month?: string) {
    return request<ExpenseCategoryStat[]>({
      url: '/statistics/expense-category',
      method: 'GET',
      params: month ? { month } : undefined
    });
  },
  // 查询资产分布。
  assetDistribution() {
    return request<AssetDistributionItem[]>({
      url: '/statistics/asset-distribution',
      method: 'GET'
    });
  },
  // 查询投资收益趋势。
  investmentProfitTrend(params?: { startMonth?: string; endMonth?: string }) {
    return request<InvestmentProfitTrendPoint[]>({
      url: '/statistics/investment-profit-trend',
      method: 'GET',
      params
    });
  },
  // 查询预算进度。
  budgetProgress(month?: string) {
    return request<BudgetSummary>({
      url: '/statistics/budget-progress',
      method: 'GET',
      params: month ? { month } : undefined
    });
  }
};
