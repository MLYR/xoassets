/* 首页仪表盘 API */
import { request } from './http'

export interface TransactionItem {
  id: string
  type: 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'REFUND'
  amount: number
  accountName: string | null
  targetAccountName?: string | null
  categoryName?: string | null
  transactionTime: string
  note?: string | null
}

export interface DashboardOverview {
  totalAssets: number
  netAssets: number
  todayExpense: number
  monthlyIncome: number
  monthlyExpense: number
  investmentMarketValue: number
  investmentFloatingProfit: number
  recentTransactions: TransactionItem[]
}

export const dashboardApi = {
  overview(month?: string) {
    return request<DashboardOverview>({
      url: '/dashboard/overview',
      method: 'GET',
      data: month ? { month } : undefined
    })
  }
}
