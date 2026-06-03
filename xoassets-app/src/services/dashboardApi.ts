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

export interface AssetSnapshot {
  id: string
  snapshotDate: string
  cashAsset: number
  investmentAsset: number
  totalAsset: number
  liability: number
  netAsset: number
  investmentCost: number
  investmentProfit: number
  investmentProfitRate: number
  monthlyIncome: number
  monthlyExpense: number
  monthlyBalance: number
  budgetUsedAmount: number
  budgetTotalAmount: number
  budgetUsageRate: number
}

export interface AssetSnapshotLatest {
  latest: AssetSnapshot | null
  netAssetChangeFromYesterday: number
  netAssetChangeFromMonthStart: number
}

export interface AssetTrendPoint {
  date: string
  value: number
}

export const dashboardApi = {
  overview(month?: string) {
    return request<DashboardOverview>({
      url: '/dashboard/overview',
      method: 'GET',
      data: month ? { month } : undefined
    })
  },
  latestSnapshot() {
    return request<AssetSnapshotLatest>({
      url: '/snapshots/latest',
      method: 'GET'
    })
  },
  netAssetsTrend(startDate: string, endDate: string) {
    return request<AssetTrendPoint[]>({
      url: '/statistics/net-assets-trend',
      method: 'GET',
      data: { startDate, endDate }
    })
  }
}
