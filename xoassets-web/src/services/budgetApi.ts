// 预算 API：封装月度总预算和分类预算接口。
import { request } from './http';

/** 预算类型。 */
export type BudgetType = 'TOTAL' | 'CATEGORY';
/** 预算使用状态。 */
export type BudgetUsageStatus = 'NORMAL' | 'WARNING' | 'OVER';

/** 预算列表项。 */
export interface BudgetItem {
  /** ID。 */
  id: string;
  /** 月份。 */
  month: string;
  /** 分类ID。 */
  categoryId?: string | null;
  /** 分类名称。 */
  categoryName?: string | null;
  /** 预算类型。 */
  budgetType: BudgetType;
  /** 金额。 */
  amount: number;
  /** 已用金额。 */
  usedAmount: number;
  /** 剩余金额。 */
  remainingAmount: number;
  /** 使用率。 */
  usageRate: number;
  /** 使用状态。 */
  usageStatus: BudgetUsageStatus;
  /** 使用状态文案。 */
  usageStatusLabel: string;
  /** 状态。 */
  status: number;
}

/** 预算汇总。 */
export interface BudgetSummary {
  /** 月份。 */
  month: string;
  /** 预算总额。 */
  totalBudget: number;
  /** 已用总额。 */
  totalUsed: number;
  /** 剩余总额。 */
  totalRemaining: number;
  /** 使用率。 */
  usageRate: number;
  /** 使用状态。 */
  usageStatus: BudgetUsageStatus;
  /** 使用状态文案。 */
  usageStatusLabel: string;
  /** 预算明细。 */
  items: BudgetItem[];
}

/** 预算保存参数。 */
export interface BudgetRequest {
  /** 月份。 */
  month: string;
  /** 分类ID。 */
  categoryId?: string | null;
  /** 预算类型。 */
  budgetType: BudgetType;
  /** 金额。 */
  amount: number;
  /** 状态。 */
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
