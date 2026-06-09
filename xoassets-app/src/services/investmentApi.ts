/* 投资 API */
import { request } from './http'

export interface HoldingItem {
  id: string
  assetId: string
  assetName: string | null
  symbol: string | null
  assetType: string | null
  assetSubType?: string | null
  profitDisplayMode?: string | null
  valuationMode?: string | null
  tradeVenue?: string | null
  currency: string | null
  quantity: number
  avgCost: number
  totalCost: number
  latestPrice: number
  priceScale?: number | null
  marketValue: number
  todayProfit?: number | null
  todayChangeRate?: number | null
  yesterdayProfit?: number | null
  yesterdayChangeRate?: number | null
  todayPriceAvailable?: boolean | null
  todayProfitAvailable?: boolean | null
  priceDate?: string | null
  primaryProfitLabel?: string | null
  primaryProfitAmount?: number | null
  secondaryProfitLabel?: string | null
  secondaryProfitAmount?: number | null
  priceLabel?: string | null
  floatingProfit: number
  floatingProfitRate: number
  breakEvenRate?: number | null
  marketStatus?: string | null
  remark?: string | null
  status: number
}

export interface HoldingSummary {
  totalMarketValue: number
  totalCost: number
  todayProfit?: number | null
  todayProfitRate?: number | null
  yesterdayProfit?: number | null
  yesterdayProfitRate?: number | null
  lastMonthProfit?: number | null
  lastMonthProfitRate?: number | null
  floatingProfit: number
  floatingProfitRate: number
  holdingCount: number
}

export interface HoldingDetailSummary {
  totalBuyAmount: number
  totalSellAmount: number
  totalFee: number
  pendingConfirmAmount?: number | null
  realizedProfit: number
  floatingProfit: number
  totalProfit: number
  totalProfitRate: number
  buyCount: number
  sellCount: number
  firstBuyTime?: string | null
  lastTradeTime?: string | null
}

export interface InvestmentTransactionItem {
  id: string
  holdingId: string
  assetId: string
  accountId: string
  accountName: string | null
  assetName: string | null
  symbol: string | null
  type: 'BUY' | 'SELL'
  inputMode?: string | null
  tradeAmount?: number | null
  tradeQuantity?: number | null
  tradePrice?: number | null
  quantity: number
  price: number
  amount: number
  fee: number
  costAmount?: number | null
  realizedProfit?: number | null
  tradeDate?: string | null
  confirmedDate?: string | null
  confirmedNav?: number | null
  confirmedQuantity?: number | null
  status?: string | null
  revokeTime?: string | null
  revokeReason?: string | null
  transactionTime: string
  note?: string | null
}

export interface AssetPriceItem {
  price: number
  currency: string
  previousClose?: number | null
  changeAmount?: number | null
  changePercent?: number | null
  source: string
  quoteTime: string
  marketStatus?: string | null
}

export interface HoldingChartPoint {
  quoteTime: string
  totalAssetAmount: number
  totalProfitAmount: number
}

export interface InvestmentCalendarDayProfit {
  date: string
  profitAmount?: number | null
  profitRate?: number | null
  marketValue?: number | null
  price?: number | null
  previousPrice?: number | null
  hasPrice: boolean
  tradingDay?: boolean | null
  marketClosed?: boolean | null
  statusLabel?: string | null
  priceLabel?: string | null
}

export interface HoldingDetail {
  holding: HoldingItem
  summary: HoldingDetailSummary
  transactions: InvestmentTransactionItem[]
  priceSnapshots: AssetPriceItem[]
  chartPoints: HoldingChartPoint[]
  profitCalendar?: InvestmentCalendarDayProfit[]
}

export interface InvestmentTrendPoint {
  date: string
  marketValue: number
  totalProfit: number
  assetAmount?: number | null
  holdingProfit?: number | null
  dailyProfit?: number | null
  dailyProfitRate?: number | null
  primaryProfitLabel?: string | null
  primaryProfitAmount?: number | null
}

export interface InvestmentModuleAsset {
  module: 'FUND' | 'STOCK' | 'CRYPTO'
  name: string
  assetAmount: number
  assetRatio: number
  primaryProfitLabel: string
  primaryProfitAvailable?: boolean | null
  primaryProfitAmount?: number | null
  primaryProfitStatusLabel?: string | null
  yesterdayProfit?: number | null
  yesterdayProfitRate?: number | null
  holdingProfit: number
  holdingProfitRate: number
  holdingCount: number
}

export interface InvestmentOverview {
  totalInvestmentAsset: number
  totalCost: number
  holdingProfit: number
  holdingProfitRate: number
  todayProfit?: number | null
  todayProfitAvailable?: boolean | null
  todayProfitStatusLabel?: string | null
  todayProfitAssetScope: string
  yesterdayProfit?: number | null
  yesterdayProfitAssetScope: string
  moduleAssets: InvestmentModuleAsset[]
}

export interface InvestmentTrend {
  module: string
  period: string
  points: InvestmentTrendPoint[]
}

export interface InvestmentTransactionRequest {
  holdingId?: string | null
  assetId: string
  accountId: string
  type: 'BUY' | 'SELL'
  inputMode?: 'QUANTITY_PRICE' | 'AMOUNT_NAV'
  tradeAmount?: number
  quantity?: number
  price?: number
  confirmedDate?: string
  fee: number
  transactionTime: string
  note?: string
}

export interface InvestmentConvertRequest {
  sourceHoldingId: string
  targetHoldingId: string
  accountId: string
  sourceQuantity: number
  sourcePrice: number
  targetQuantity: number
  targetPrice: number
  fee: number
  transactionTime: string
  note?: string
}

export const investmentApi = {
  listHoldings() {
    return request<HoldingItem[]>({ url: '/holdings', method: 'GET' })
  },
  overviewInvestments() {
    return request<InvestmentOverview>({ url: '/investments/overview', method: 'GET' })
  },
  listInvestmentHoldings(params?: { module?: 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO' }) {
    return request<HoldingItem[]>({ url: '/investments/holdings', method: 'GET', data: params })
  },
  summaryHoldings() {
    return request<HoldingSummary>({ url: '/holdings/summary', method: 'GET' })
  },
  detailHolding(id: string) {
    return request<HoldingDetail>({ url: `/holdings/${id}/detail`, method: 'GET' })
  },
  trend(params: { startDate?: string; endDate?: string }) {
    return request<InvestmentTrendPoint[]>({ url: '/holdings/trend', method: 'GET', data: params })
  },
  trendInvestments(params: { module?: 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO'; period?: 'WEEK' | 'MONTH' | 'QUARTER' | 'YEAR'; startDate?: string; endDate?: string }) {
    return request<InvestmentTrend>({ url: '/investments/trend', method: 'GET', data: params })
  },
  profitCalendar(id: string, params?: { year?: number; month?: number }) {
    return request<InvestmentCalendarDayProfit[]>({ url: `/investments/holdings/${id}/profit-calendar`, method: 'GET', data: params })
  },
  createTransaction(data: InvestmentTransactionRequest) {
    return request<InvestmentTransactionItem>({
      url: '/investment-transactions',
      method: 'POST',
      data
    })
  },
  convertTransaction(data: InvestmentConvertRequest) {
    return request<InvestmentTransactionItem[]>({
      url: '/investment-transactions/convert',
      method: 'POST',
      data
    })
  }
}
