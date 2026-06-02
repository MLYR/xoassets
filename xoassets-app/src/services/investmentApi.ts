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
  todayProfit: number
  yesterdayProfit: number
  floatingProfit: number
  floatingProfitRate: number
  holdingCount: number
}

export interface HoldingDetailSummary {
  totalBuyAmount: number
  totalSellAmount: number
  totalFee: number
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
  quantity: number
  price: number
  amount: number
  fee: number
  costAmount?: number | null
  realizedProfit?: number | null
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

export interface HoldingDetail {
  holding: HoldingItem
  summary: HoldingDetailSummary
  transactions: InvestmentTransactionItem[]
  priceSnapshots: AssetPriceItem[]
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
  }
}
