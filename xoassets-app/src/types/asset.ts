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

export interface RecentTransaction {
  id: string;
  type?: string | null;
  amount?: number | null;
  categoryName?: string | null;
  accountName?: string | null;
  remark?: string | null;
  transactionTime?: string | null;
}
