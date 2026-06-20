export type AccountType = 'CASH' | 'BANK_CARD' | 'CREDIT_CARD' | 'ALIPAY' | 'WECHAT' | 'OTHER' | string;

export interface AccountItem {
  id: string;
  name?: string | null;
  type?: AccountType | null;
  balance?: number | null;
  initialBalance?: number | null;
  currency?: string | null;
  status?: number | null;
  sortOrder?: number | null;
  remark?: string | null;
  displayType?: string | null;
  maskedNo?: string | null;
  group?: string | null;
  isDefault?: boolean | null;
  tagText?: string | null;
  availableCredit?: number | null;
}

export interface AccountCategorySummary {
  type?: string | null;
  name?: string | null;
  amount?: number | null;
  ratio?: number | null;
  count?: number | null;
}

export interface AccountOverview {
  totalAsset?: number | null;
  lastMonthChangeAmount?: number | null;
  lastMonthChangeRate?: number | null;
  compareAvailable?: boolean | null;
  accountCount?: number | null;
  nonCreditAssetTotal?: number | null;
  nonZeroAccountCount?: number | null;
  categories?: AccountCategorySummary[] | null;
  accounts?: AccountItem[] | null;
}

export interface AccountRequest {
  name: string;
  type: string;
  initialBalance: string;
  balance?: string;
  currency?: string;
  status?: number;
  sortOrder?: number;
  remark?: string | null;
}

export interface AccountBalanceAdjustmentRequest {
  afterBalance: string;
  reason?: string | null;
  bizDate?: string;
  bizTime?: string;
}

export interface AccountLedgerItem {
  id: string;
  sourceType?: string | null;
  bizType?: string | null;
  title?: string | null;
  amount?: number | null;
  accountId?: string | null;
  accountName?: string | null;
  relatedAccountId?: string | null;
  relatedAccountName?: string | null;
  categoryId?: string | null;
  categoryName?: string | null;
  assetId?: string | null;
  assetName?: string | null;
  symbol?: string | null;
  status?: string | null;
  transactionTime?: string | null;
  note?: string | null;
}

export interface AccountLedgerSummary {
  incomeAmount?: number | null;
  expenseAmount?: number | null;
  transferInAmount?: number | null;
  transferOutAmount?: number | null;
  investmentBuyAmount?: number | null;
  investmentSellAmount?: number | null;
  adjustmentAmount?: number | null;
  netFlowAmount?: number | null;
}

export interface AccountLedgerPage {
  account?: AccountItem | null;
  summary?: AccountLedgerSummary | null;
  page?: {
    records?: AccountLedgerItem[];
    list?: AccountLedgerItem[];
    total?: number;
    pageNo?: number;
    pageSize?: number;
  } | null;
}

export interface AccountFlowStatistics {
  incomeAmount?: number | null;
  expenseAmount?: number | null;
  transferInAmount?: number | null;
  transferOutAmount?: number | null;
  investmentBuyAmount?: number | null;
  investmentSellAmount?: number | null;
  adjustmentAmount?: number | null;
  netFlowAmount?: number | null;
  categoryExpenseStats?: Array<{ name?: string | null; amount?: number | null }> | null;
  dailyBalanceTrend?: Array<{ date?: string | null; endBalance?: number | null; inflow?: number | null; outflow?: number | null; adjustmentAmount?: number | null }> | null;
}
