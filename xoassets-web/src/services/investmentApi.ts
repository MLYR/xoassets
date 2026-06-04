// 投资 API：封装资产、持仓、投资交易和手动报价接口。
import { request } from './http';

export type AssetType = 'STOCK' | 'FUND' | 'CRYPTO' | 'OTHER';
export type QuoteSource = 'MANUAL' | 'COINGECKO' | 'EASTMONEY' | 'SINA' | 'YAHOO' | 'ALPHA_VANTAGE' | 'TUSHARE' | 'AKSHARE';
export type InvestmentTransactionType = 'BUY' | 'SELL';

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
  latestPriceSource?: string | null;
  marketStatus?: string | null;
  marketValue: number;
  todayProfit?: number | null;
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
  todayProfit?: number | null;
  yesterdayProfit: number;
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

export interface HoldingDetail {
  holding: HoldingItem;
  summary: HoldingDetailSummary;
  transactions: InvestmentTransactionItem[];
  priceSnapshots: AssetPriceItem[];
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
  quantity: number;
  price: number;
  amount: number;
  fee: number;
  costAmount?: number | null;
  realizedProfit?: number | null;
  status?: 'NORMAL' | 'REVOKED' | null;
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
  quantity: number;
  price: number;
  fee: number;
  transactionTime: string;
  note?: string;
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
