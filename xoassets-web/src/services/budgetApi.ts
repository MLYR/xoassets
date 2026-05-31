// 预算 API：封装月度总预算和分类预算接口。
import { request } from './http';

export type BudgetType = 'TOTAL' | 'CATEGORY';
export type BudgetUsageStatus = 'NORMAL' | 'WARNING' | 'OVER';

export interface BudgetItem {
  id: string;
  month: string;
  categoryId?: string | null;
  categoryName?: string | null;
  budgetType: BudgetType;
  amount: number;
  usedAmount: number;
  remainingAmount: number;
  usageRate: number;
  usageStatus: BudgetUsageStatus;
  usageStatusLabel: string;
  status: number;
}

export interface BudgetSummary {
  month: string;
  totalBudget: number;
  totalUsed: number;
  totalRemaining: number;
  usageRate: number;
  usageStatus: BudgetUsageStatus;
  usageStatusLabel: string;
  items: BudgetItem[];
}

export interface BudgetRequest {
  month: string;
  categoryId?: string | null;
  budgetType: BudgetType;
  amount: number;
  status: number;
}

export const budgetApi = {
  // 查询某个月的预算列表。
  list(month: string) {
    return request<BudgetItem[]>({
      url: '/budgets',
      method: 'GET',
      params: { month }
    });
  },
  // 新增总预算或分类预算。
  create(data: BudgetRequest) {
    return request<BudgetItem>({
      url: '/budgets',
      method: 'POST',
      data
    });
  },
  // 修改当前用户自己的预算。
  update(id: string, data: BudgetRequest) {
    return request<BudgetItem>({
      url: `/budgets/${id}`,
      method: 'PUT',
      data
    });
  },
  // 删除当前用户自己的预算。
  remove(id: string) {
    return request<void>({
      url: `/budgets/${id}`,
      method: 'DELETE'
    });
  },
  // 查询某个月的整体预算汇总。
  summary(month: string) {
    return request<BudgetSummary>({
      url: '/budgets/summary',
      method: 'GET',
      params: { month }
    });
  }
};
