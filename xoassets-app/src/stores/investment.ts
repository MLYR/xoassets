/* 投资 Store */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  investmentApi,
  type HoldingItem,
  type HoldingSummary,
  type HoldingDetail,
  type InvestmentTrendPoint,
  type InvestmentTransactionRequest,
  type InvestmentConvertRequest
} from '@/services/investmentApi'

export const useInvestmentStore = defineStore('investment', () => {
  const holdings = ref<HoldingItem[]>([])
  const summary = ref<HoldingSummary | null>(null)
  const trend = ref<InvestmentTrendPoint[]>([])
  const loading = ref(false)

  async function fetchHoldings() {
    loading.value = true
    try {
      const [hs, sm] = await Promise.all([
        investmentApi.listHoldings(),
        investmentApi.summaryHoldings()
      ])
      holdings.value = hs
      summary.value = sm
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(id: string): Promise<HoldingDetail> {
    return investmentApi.detailHolding(id)
  }

  async function fetchTrend(params: { startDate?: string; endDate?: string }) {
    trend.value = await investmentApi.trend(params)
    return trend.value
  }

  async function createTransaction(data: InvestmentTransactionRequest) {
    return investmentApi.createTransaction(data)
  }

  async function convertTransaction(data: InvestmentConvertRequest) {
    return investmentApi.convertTransaction(data)
  }

  return { holdings, summary, trend, loading, fetchHoldings, fetchDetail, fetchTrend, createTransaction, convertTransaction }
})
