/* 预算 API */
import { request } from './http'

export interface BudgetItem {
  id: string
  month: string
  categoryId?: string | null
  categoryName?: string | null
  budgetType: 'TOTAL' | 'CATEGORY'
  amount: number
  usedAmount: number
  remainingAmount: number
  usageRate: number
  usageStatus: string
  usageStatusLabel: string
  status: number
}

export interface BudgetSummary {
  month: string
  totalBudget: number
  totalUsed: number
  usageRate: number
}

export const budgetApi = {
  list(month: string) {
    return request<BudgetItem[]>({
      url: '/budgets',
      method: 'GET',
      data: { month }
    })
  },
  summary(month: string) {
    return request<BudgetSummary>({
      url: '/budgets/summary',
      method: 'GET',
      data: { month }
    })
  }
}
