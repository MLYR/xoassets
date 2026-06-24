export type InvestmentModule = 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO';
export type InvestmentPeriod = 'WEEK' | 'MONTH' | 'QUARTER' | 'YEAR' | 'ALL';
export type AssetType = 'FUND' | 'STOCK' | 'CRYPTO' | 'OTHER';
export type QuoteSource = 'MANUAL' | 'COINGECKO' | 'EASTMONEY' | 'SINA' | 'YAHOO' | 'ALPHA_VANTAGE' | 'TUSHARE' | 'AKSHARE' | string;
export type InvestmentTransactionType = 'BUY' | 'SELL';
export type InvestmentInputMode = 'QUANTITY_PRICE' | 'AMOUNT_NAV';

export interface InvestmentOverview {
  totalInvestmentAsset?: number | null;
  pendingConfirmAmount?: number | null;
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

export interface InvestmentModuleAsset {
  module?: 'FUND' | 'STOCK' | 'CRYPTO' | string | null;
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

export interface HoldingItem {
  id: string;
  assetId: string;
  assetName?: string | null;
  symbol?: string | null;
  assetType?: AssetType | string | null;
  assetSubType?: string | null;
  market?: string | null;
  currency?: string | null;
  quantity?: number | null;
  avgCost?: number | null;
  totalCost?: number | null;
  latestPrice?: number | null;
  priceScale?: number | null;
  latestPriceTime?: string | null;
  priceDate?: string | null;
  todayPriceAvailable?: boolean | null;
  todayProfitAvailable?: boolean | null;
  priceStatus?: string | null;
  marketStatus?: string | null;
  primaryProfitLabel?: string | null;
  primaryProfitAmount?: number | null;
  secondaryProfitLabel?: string | null;
  secondaryProfitAmount?: number | null;
  priceLabel?: string | null;
  marketValue?: number | null;
  todayProfit?: number | null;
  todayProfitRate?: number | null;
  todayChangeRate?: number | null;
  yesterdayProfit?: number | null;
  yesterdayChangeRate?: number | null;
  floatingProfit?: number | null;
  realizedProfit?: number | null;
  totalProfit?: number | null;
  totalProfitRate?: number | null;
  floatingProfitRate?: number | null;
  remark?: string | null;
  status?: number | null;
}

export interface InvestmentTrendPoint {
  date: string;
  marketValue?: number | null;
  totalProfit?: number | null;
  assetAmount?: number | null;
  holdingProfit?: number | null;
  dailyProfit?: number | null;
  dailyProfitRate?: number | null;
  primaryProfitLabel?: string | null;
  primaryProfitAmount?: number | null;
}

export interface InvestmentTrend {
  module?: string | null;
  period?: string | null;
  points?: InvestmentTrendPoint[] | null;
}

export interface InvestmentCalendarDayProfit {
  date: string;
  profitAmount?: number | null;
  profitRate?: number | null;
  marketValue?: number | null;
  price?: number | null;
  previousPrice?: number | null;
  hasPrice?: boolean | null;
  tradingDay?: boolean | null;
  marketClosed?: boolean | null;
  statusLabel?: string | null;
  priceLabel?: string | null;
}

export interface InvestmentTransactionItem {
  id: string;
  holdingId?: string | null;
  assetId?: string | null;
  accountId?: string | null;
  accountName?: string | null;
  assetName?: string | null;
  symbol?: string | null;
  type?: InvestmentTransactionType | string | null;
  inputMode?: InvestmentInputMode | string | null;
  tradeAmount?: number | null;
  tradeQuantity?: number | null;
  tradePrice?: number | null;
  quantity?: number | null;
  price?: number | null;
  amount?: number | null;
  fee?: number | null;
  costAmount?: number | null;
  realizedProfit?: number | null;
  tradeDate?: string | null;
  confirmedDate?: string | null;
  status?: string | null;
  revokeTime?: string | null;
  transactionTime?: string | null;
  note?: string | null;
}

export interface LedgerAccount {
  id: string;
  name?: string | null;
  type?: string | null;
  balance?: number | null;
  currency?: string | null;
  status?: number | null;
}

export interface AssetLookupItem {
  name?: string | null;
  symbol?: string | null;
  assetType?: AssetType | string | null;
  market?: string | null;
  currency?: string | null;
  quoteSource?: QuoteSource | null;
  quoteKey?: string | null;
  latestPrice?: number | null;
  previousClose?: number | null;
  changePercent?: number | null;
  quoteTime?: string | null;
}

export interface AssetItem {
  id: string;
  symbol?: string | null;
  name?: string | null;
  type?: AssetType | string | null;
  market?: string | null;
  currency?: string | null;
  quoteSource?: QuoteSource | null;
  quoteKey?: string | null;
}

export interface AssetRequest {
  symbol: string;
  name: string;
  type: AssetType | string;
  market?: string;
  currency?: string;
  quoteSource?: QuoteSource;
  quoteKey?: string;
}

export interface InvestmentTransactionRequest {
  holdingId?: string | null;
  assetId: string;
  accountId: string;
  type: InvestmentTransactionType;
  inputMode?: InvestmentInputMode;
  tradeAmount?: string;
  quantity?: string;
  price?: string;
  confirmedDate?: string;
  fee: string;
  transactionTime: string;
  note?: string | null;
}

export interface BatchRefreshQuoteRequest {
  assetIds: string[];
}
