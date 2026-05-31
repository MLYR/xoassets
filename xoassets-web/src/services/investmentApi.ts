// 投资 API：封装资产、持仓、投资交易和手动报价接口。
import { request } from './http';

export type AssetType = 'STOCK' | 'FUND' | 'CRYPTO' | 'OTHER';
export type QuoteSource = 'MANUAL' | 'COINGECKO' | 'ALPHA_VANTAGE' | 'TUSHARE' | 'AKSHARE';
export type InvestmentTransactionType = 'BUY' | 'SELL';

export interface AssetItem {
  id: string;
  symbol: string;
  name: string;
  type: AssetType;
  currency: string;
  quoteSource: QuoteSource;
  quoteKey?: string | null;
}

export interface AssetRequest {
  symbol: string;
  name: string;
  type: AssetType;
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
  quoteSource: QuoteSource | null;
  currency: string | null;
  quantity: number;
  avgCost: number;
  totalCost: number;
  latestPrice: number;
  latestPriceTime?: string | null;
  marketValue: number;
  floatingProfit: number;
  floatingProfitRate: number;
  remark?: string | null;
  status: number;
}

export interface HoldingRequest {
  assetId: string;
  quantity: number;
  avgCost: number;
  remark?: string;
}

export interface InvestmentTransactionItem {
  id: string;
  holdingId: string;
  assetId: string;
  assetName: string | null;
  symbol: string | null;
  type: InvestmentTransactionType;
  quantity: number;
  price: number;
  amount: number;
  fee: number;
  transactionTime: string;
  note?: string | null;
}

export interface InvestmentTransactionRequest {
  holdingId?: string | null;
  assetId: string;
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

export const investmentApi = {
  searchAssets(params: { keyword?: string; type?: AssetType }) {
    return request<AssetItem[]>({
      url: '/assets/search',
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
  }
};
