export interface DashboardOverview {
  totalAssets?: number | null;
  netAssets?: number | null;
  todayIncome?: number | null;
  todayExpense?: number | null;
  monthlyIncome?: number | null;
  monthlyExpense?: number | null;
  investmentMarketValue?: number | null;
  investmentTodayProfit?: number | null;
  investmentYesterdayProfit?: number | null;
  budgetUsageRate?: number | null;
  assetTrendRate?: number | null;
  incomeTrendRate?: number | null;
  expenseTrendRate?: number | null;
  balanceTrendRate?: number | null;
}

export interface AssetSnapshotLatest {
  latest?: {
    totalAsset?: number | null;
    netAsset?: number | null;
    investmentAsset?: number | null;
    monthlyIncome?: number | null;
    budgetUsageRate?: number | null;
  } | null;
  netAssetChangeFromYesterday?: number | null;
  netAssetChangeFromMonthStart?: number | null;
}

export interface InvestmentModuleAsset {
  module?: string | null;
  name?: string | null;
  assetAmount?: number | null;
  assetRatio?: number | null;
  primaryProfitLabel?: string | null;
  primaryProfitAvailable?: boolean | null;
  primaryProfitAmount?: number | null;
  primaryProfitStatusLabel?: string | null;
  yesterdayProfit?: number | null;
  yesterdayProfitRate?: number | null;
  holdingProfit?: number | null;
  holdingProfitRate?: number | null;
  holdingCount?: number | null;
}

export interface InvestmentOverview {
  totalInvestmentAsset?: number | null;
  totalCost?: number | null;
  holdingProfit?: number | null;
  holdingProfitRate?: number | null;
  todayProfitAvailable?: boolean | null;
  todayProfit?: number | null;
  todayProfitAssetScope?: string | null;
  todayProfitStatusLabel?: string | null;
  yesterdayProfit?: number | null;
  yesterdayProfitAssetScope?: string | null;
  moduleAssets?: InvestmentModuleAsset[] | null;
}

export interface RecentTransaction {
  id: string;
  type?: string | null;
  amount?: number | null;
  categoryName?: string | null;
  accountName?: string | null;
  remark?: string | null;
  note?: string | null;
  targetAccountName?: string | null;
  transactionTime?: string | null;
}

export interface BudgetSummary {
  month?: string | null;
  totalBudget?: number | null;
  totalUsed?: number | null;
  totalRemaining?: number | null;
  usageRate?: number | null;
  usageStatus?: string | null;
  usageStatusLabel?: string | null;
}

export interface AiReport {
  id: string;
  reportType?: string | null;
  reportDate?: string | null;
  title?: string | null;
  content?: string | null;
  summaryJson?: string | null;
  status?: string | null;
  statusLabel?: string | null;
  createdAt?: string | null;
}
