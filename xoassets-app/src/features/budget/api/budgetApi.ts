import { request } from '@/api/http';

import type { BudgetItem, BudgetRequest, BudgetSummary, ExpenseCategoryItem } from './budgetTypes';

export const budgetApi = {
  summary(month: string) {
    return request<BudgetSummary>({
      url: '/api/budgets/summary',
      method: 'GET',
      params: { month }
    });
  },
  list(month: string) {
    return request<BudgetItem[]>({
      url: '/api/budgets',
      method: 'GET',
      params: { month }
    });
  },
  create(data: BudgetRequest) {
    return request<BudgetItem>({
      url: '/api/budgets',
      method: 'POST',
      data
    });
  },
  update(id: string, data: BudgetRequest) {
    return request<BudgetItem>({
      url: `/api/budgets/${id}`,
      method: 'PUT',
      data
    });
  },
  expenseCategories() {
    return request<ExpenseCategoryItem[]>({
      url: '/api/categories',
      method: 'GET',
      params: { type: 'EXPENSE' }
    });
  }
};
