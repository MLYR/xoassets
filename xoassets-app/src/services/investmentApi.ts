/* 投资 API */
import { request } from './http'

export interface HoldingItem {
  id: string
  assetId: string
  assetName: string | null
  symbol: string | null
  assetType: string | null
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
  yesterdayProfit: number
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

export interface HoldingDetail {
  holding: HoldingItem
  summary: HoldingDetailSummary
  transactions: InvestmentTransactionItem[]
  priceSnapshots: AssetPriceItem[]
  chartPoints: HoldingChartPoint[]
}

export interface InvestmentTrendPoint {
  date: string
  marketValue: number
  totalProfit: number
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
  summaryHoldings() {
    return request<HoldingSummary>({ url: '/holdings/summary', method: 'GET' })
  },
  detailHolding(id: string) {
    return request<HoldingDetail>({ url: `/holdings/${id}/detail`, method: 'GET' })
  },
  trend(params: { startDate?: string; endDate?: string }) {
    return request<InvestmentTrendPoint[]>({ url: '/holdings/trend', method: 'GET', data: params })
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
