// 投资 API：封装资产、持仓、投资交易和手动报价接口。
import { request } from './http';

export type AssetType = 'STOCK' | 'FUND' | 'CRYPTO' | 'OTHER';
export type QuoteSource = 'MANUAL' | 'COINGECKO' | 'EASTMONEY' | 'SINA' | 'YAHOO' | 'ALPHA_VANTAGE' | 'TUSHARE' | 'AKSHARE';
export type InvestmentTransactionType = 'BUY' | 'SELL';
export type InvestmentInputMode = 'QUANTITY_PRICE' | 'AMOUNT_NAV';
export type InvestmentTransactionStatus = 'NORMAL' | 'CONFIRMED' | 'PENDING_CONFIRM' | 'FAILED' | 'CANCELLED' | 'REVOKED';

export interface AssetItem {
  id: string;
  symbol: string;
  name: string;
  type: AssetType;
  market?: string | null;
  currency: string;
  quoteSource: QuoteSource;
  quoteKey?: string | null;
}

export interface AssetLookupItem {
  name: string;
  symbol: string;
  assetType: AssetType;
  market?: string | null;
  currency: string;
  quoteSource: QuoteSource;
  quoteKey: string;
  latestPrice?: number | null;
  previousClose?: number | null;
  changePercent?: number | null;
  quoteTime?: string | null;
}

export interface AssetRequest {
  symbol: string;
  name: string;
  type: AssetType;
  market?: string;
  currency: string;
  quoteSource: QuoteSource;
  quoteKey?: string;
}

export interface HoldingItem {
  id: string;
  assetId: string;
  assetName: string | null;
  symbol: string | null;
  assetType: AssetType | null;
  assetSubType?: string | null;
  profitDisplayMode?: string | null;
  valuationMode?: string | null;
  tradeVenue?: string | null;
  market?: string | null;
  quoteSource: QuoteSource | null;
  currency: string | null;
  quantity: number;
  avgCost: number;
  totalCost: number;
  latestPrice: number;
  previousPrice?: number | null;
  beforePreviousPrice?: number | null;
  priceScale?: number | null;
  latestPriceTime?: string | null;
  previousPriceTime?: string | null;
  priceDate?: string | null;
  todayPriceAvailable?: boolean | null;
  todayProfitAvailable?: boolean | null;
  priceStatus?: string | null;
  latestPriceSource?: string | null;
  marketStatus?: string | null;
  primaryProfitLabel?: string | null;
  primaryProfitAmount?: number | null;
  secondaryProfitLabel?: string | null;
  secondaryProfitAmount?: number | null;
  priceLabel?: string | null;
  marketValue: number;
  todayProfit?: number | null;
  todayProfitByCurrentQuantity?: number | null;
  todayProfitRateByCurrentQuantity?: number | null;
  todayProfitByPreviousSnapshotQuantity?: number | null;
  todayProfitRateByPreviousSnapshotQuantity?: number | null;
  todayChangeRate?: number | null;
  yesterdayProfit?: number | null;
  yesterdayChangeRate?: number | null;
  floatingProfit: number;
  floatingProfitRate: number;
  breakEvenRate?: number | null;
  remark?: string | null;
  status: number;
}

export interface HoldingSummary {
  totalMarketValue: number;
  totalCost: number;
  todayProfitAvailable?: boolean | null;
  todayProfit?: number | null;
  todayProfitRate?: number | null;
  yesterdayProfit: number;
  yesterdayProfitRate?: number | null;
  lastMonthProfit?: number | null;
  lastMonthProfitRate?: number | null;
  floatingProfit: number;
  floatingProfitRate: number;
  holdingCount: number;
}

export interface HoldingDetailSummary {
  totalBuyAmount: number;
  totalSellAmount: number;
  totalFee: number;
  pendingConfirmAmount?: number | null;
  realizedProfit: number;
  floatingProfit: number;
  totalProfit: number;
  totalProfitRate: number;
  buyCount: number;
  sellCount: number;
  firstBuyTime?: string | null;
  lastTradeTime?: string | null;
}

export interface AssetPriceItem {
  id?: string;
  assetId?: string;
  price: number;
  currency: string;
  previousClose?: number | null;
  changeAmount?: number | null;
  changePercent?: number | null;
  source: string;
  quoteTime: string;
  marketStatus?: string | null;
}

export interface HoldingChartPoint {
  quoteTime: string;
  totalAssetAmount: number;
  totalProfitAmount: number;
}

export interface InvestmentCalendarDayProfit {
  date: string;
  profitAmount?: number | null;
  profitRate?: number | null;
  marketValue?: number | null;
  price?: number | null;
  previousPrice?: number | null;
  hasPrice: boolean;
  tradingDay?: boolean | null;
  marketClosed?: boolean | null;
  statusLabel?: string | null;
  priceLabel?: string | null;
}

export interface HoldingDetail {
  holding: HoldingItem;
  summary: HoldingDetailSummary;
  transactions: InvestmentTransactionItem[];
  priceSnapshots: AssetPriceItem[];
  chartPoints: HoldingChartPoint[];
  profitCalendar?: InvestmentCalendarDayProfit[];
}

export interface InvestmentTrendPoint {
  date: string;
  marketValue: number;
  totalProfit: number;
  assetAmount?: number | null;
  holdingProfit?: number | null;
  primaryProfitLabel?: string | null;
  primaryProfitAmount?: number | null;
}

export interface InvestmentModuleAsset {
  module: 'FUND' | 'STOCK' | 'CRYPTO';
  name: string;
  assetAmount: number;
  assetRatio: number;
  primaryProfitLabel: string;
  primaryProfitAvailable?: boolean | null;
  primaryProfitAmount?: number | null;
  primaryProfitStatusLabel?: string | null;
  holdingProfit: number;
  holdingProfitRate: number;
  holdingCount: number;
}

export interface InvestmentOverview {
  totalInvestmentAsset: number;
  totalCost: number;
  holdingProfit: number;
  holdingProfitRate: number;
  todayProfit?: number | null;
  todayProfitAvailable?: boolean | null;
  todayProfitAssetScope: string;
  todayProfitStatusLabel?: string | null;
  yesterdayProfit: number;
  yesterdayProfitAssetScope: string;
  moduleAssets: InvestmentModuleAsset[];
}

export interface InvestmentTrend {
  module: string;
  period: string;
  points: InvestmentTrendPoint[];
}

export interface HoldingRequest {
  assetId?: string | null;
  assetName?: string;
  symbol?: string;
  assetType?: AssetType;
  market?: string;
  currency?: string;
  quoteSource?: QuoteSource;
  quoteKey?: string;
  latestPrice?: number;
  previousClose?: number | null;
  changePercent?: number | null;
  quoteTime?: string | null;
  marketStatus?: string;
  quantity: number;
  avgCost: number;
  remark?: string;
}

export interface InvestmentTransactionItem {
  id: string;
  holdingId: string;
  assetId: string;
  accountId: string;
  accountName: string | null;
  assetName: string | null;
  symbol: string | null;
  type: InvestmentTransactionType;
  inputMode?: InvestmentInputMode | null;
  tradeAmount?: number | null;
  tradeQuantity?: number | null;
  tradePrice?: number | null;
  quantity: number;
  price: number;
  amount: number;
  fee: number;
  costAmount?: number | null;
  realizedProfit?: number | null;
  tradeDate?: string | null;
  confirmedDate?: string | null;
  confirmedNav?: number | null;
  confirmedQuantity?: number | null;
  status?: InvestmentTransactionStatus | null;
  revokeTime?: string | null;
  revokeReason?: string | null;
  transactionTime: string;
  note?: string | null;
}

export interface InvestmentTransactionRequest {
  holdingId?: string | null;
  assetId: string;
  accountId: string;
  type: InvestmentTransactionType;
  inputMode?: InvestmentInputMode;
  tradeAmount?: number;
  quantity?: number;
  price?: number;
  confirmedDate?: string;
  fee: number;
  transactionTime: string;
  note?: string;
}

export interface FundConfirmPreview {
  tradeDate: string;
  effectiveTradeDate: string;
  confirmedDate: string;
  qdii: boolean;
  shifted: boolean;
  shiftReason?: string | null;
}

export interface ManualQuoteRequest {
  assetId: string;
  price: number;
  currency: string;
  quoteTime?: string;
}

export interface RefreshQuoteRequest {
  assetId: string;
}

export interface BatchRefreshQuoteRequest {
  assetIds: string[];
}

export const investmentApi = {
  searchAssets(params: { keyword?: string; type?: AssetType }) {
    return request<AssetItem[]>({
      url: '/assets/search',
      method: 'GET',
      params
    });
  },
  lookupAssets(params: { type: AssetType; keyword: string; market?: string }) {
    return request<AssetLookupItem[]>({
      url: '/assets/lookup',
      method: 'GET',
      params
    });
  },
  createAsset(data: AssetRequest) {
    return request<AssetItem>({
      url: '/assets',
      method: 'POST',
      data
    });
  },
  listHoldings() {
    return request<HoldingItem[]>({
      url: '/holdings',
      method: 'GET'
    });
  },
  overviewInvestments() {
    return request<InvestmentOverview>({
      url: '/investments/overview',
      method: 'GET'
    });
  },
  listInvestmentHoldings(params?: { module?: 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO' }) {
    return request<HoldingItem[]>({
      url: '/investments/holdings',
      method: 'GET',
      params
    });
  },
  summaryHoldings() {
    return request<HoldingSummary>({
      url: '/holdings/summary',
      method: 'GET'
    });
  },
  detailHolding(id: string) {
    return request<HoldingDetail>({
      url: `/holdings/${id}/detail`,
      method: 'GET'
    });
  },
  trendHoldings(params: { startDate?: string; endDate?: string }) {
    return request<InvestmentTrendPoint[]>({
      url: '/holdings/trend',
      method: 'GET',
      params
    });
  },
  trendInvestments(params: { module?: 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO'; period?: 'WEEK' | 'MONTH' | 'QUARTER' | 'YEAR'; startDate?: string; endDate?: string }) {
    return request<InvestmentTrend>({
      url: '/investments/trend',
      method: 'GET',
      params
    });
  },
  profitCalendar(id: string, params?: { year?: number; month?: number }) {
    return request<InvestmentCalendarDayProfit[]>({
      url: `/investments/holdings/${id}/profit-calendar`,
      method: 'GET',
      params
    });
  },
  generateInvestmentSnapshot(params?: { snapshotDate?: string }) {
    return request<void>({
      url: '/investments/snapshots/generate',
      method: 'POST',
      params
    });
  },
  createHolding(data: HoldingRequest) {
    return request<HoldingItem>({
      url: '/holdings',
      method: 'POST',
      data
    });
  },
  updateHolding(id: string, data: HoldingRequest) {
    return request<HoldingItem>({
      url: `/holdings/${id}`,
      method: 'PUT',
      data
    });
  },
  removeHolding(id: string) {
    return request<void>({
      url: `/holdings/${id}`,
      method: 'DELETE'
    });
  },
  createTransaction(data: InvestmentTransactionRequest) {
    return request<InvestmentTransactionItem>({
      url: '/investment-transactions',
      method: 'POST',
      data
    });
  },
  fundConfirmPreview(params: { assetId: string; transactionTime: string }) {
    return request<FundConfirmPreview>({
      url: '/investment-transactions/fund-confirm-preview',
      method: 'GET',
      params
    });
  },
  listTransactions(holdingId?: string) {
    return request<InvestmentTransactionItem[]>({
      url: '/investment-transactions',
      method: 'GET',
      params: holdingId ? { holdingId } : undefined
    });
  },
  revokeTransaction(id: string, reason?: string) {
    return request<InvestmentTransactionItem>({
      url: `/investment-transactions/${id}/revoke`,
      method: 'PUT',
      data: { reason }
    });
  },
  manualQuote(data: ManualQuoteRequest) {
    return request({
      url: '/quotes/manual',
      method: 'POST',
      data
    });
  },
  refreshQuote(data: RefreshQuoteRequest) {
    return request({
      url: '/quotes/refresh',
      method: 'POST',
      data
    });
  },
  refreshQuotes(data: BatchRefreshQuoteRequest) {
    return request<AssetPriceItem[]>({
      url: '/quotes/refresh-batch',
      method: 'POST',
      data
    });
  }
};
