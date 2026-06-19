import { request } from '@/api/http';
import type {
  AssetItem,
  AssetLookupItem,
  AssetRequest,
  AssetType,
  BatchRefreshQuoteRequest,
  HoldingItem,
  InvestmentCalendarDayProfit,
  InvestmentModule,
  InvestmentOverview,
  InvestmentPeriod,
  InvestmentTransactionItem,
  InvestmentTransactionRequest,
  InvestmentTrend,
  LedgerAccount
} from './investmentTypes';

function trendParams(module: InvestmentModule, period: InvestmentPeriod) {
  if (period !== 'ALL') {
    return { module, period };
  }

  // 后端 period 只定义具体窗口；“全部”用显式日期范围表达，避免前端重算趋势。
  return {
    module,
    period: 'YEAR',
    startDate: '1970-01-01',
    endDate: formatDate(new Date())
  };
}

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export const investmentApi = {
  overview() {
    return request<InvestmentOverview>({
      url: '/api/investments/overview',
      method: 'GET'
    });
  },
  holdings(module: InvestmentModule) {
    return request<HoldingItem[]>({
      url: '/api/investments/holdings',
      method: 'GET',
      params: { module }
    });
  },
  trend(module: InvestmentModule, period: InvestmentPeriod) {
    return request<InvestmentTrend>({
      url: '/api/investments/trend',
      method: 'GET',
      params: trendParams(module, period)
    });
  },
  dailyProfit(year: number, month: number) {
    return request<InvestmentCalendarDayProfit[]>({
      url: '/api/investments/daily-profit',
      method: 'GET',
      params: { year, month }
    });
  },
  holdingProfitCalendar(holdingId: string, year: number, month: number) {
    return request<InvestmentCalendarDayProfit[]>({
      url: `/api/investments/holdings/${holdingId}/profit-calendar`,
      method: 'GET',
      params: { year, month }
    });
  },
  transactions(holdingId?: string | null) {
    return request<InvestmentTransactionItem[]>({
      url: '/api/investment-transactions',
      method: 'GET',
      params: holdingId ? { holdingId } : undefined
    });
  },
  accounts() {
    return request<LedgerAccount[]>({
      url: '/api/accounts',
      method: 'GET'
    });
  },
  lookupAssets(type: AssetType, keyword: string, market?: string) {
    return request<AssetLookupItem[]>({
      url: '/api/assets/lookup',
      method: 'GET',
      params: { type, keyword, market }
    });
  },
  searchAssets(type: AssetType, keyword?: string) {
    return request<AssetItem[]>({
      url: '/api/assets/search',
      method: 'GET',
      params: { type, keyword }
    });
  },
  createAsset(data: AssetRequest) {
    return request<AssetItem>({
      url: '/api/assets',
      method: 'POST',
      data
    });
  },
  createTransaction(data: InvestmentTransactionRequest) {
    return request<InvestmentTransactionItem>({
      url: '/api/investment-transactions',
      method: 'POST',
      data
    });
  },
  refreshQuotes(data: BatchRefreshQuoteRequest) {
    return request<void>({
      url: '/api/quotes/refresh-batch',
      method: 'POST',
      data
    });
  }
};
