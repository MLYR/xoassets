/* 投资 Store */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  investmentApi,
  type HoldingItem,
  type HoldingSummary,
  type HoldingDetail
} from '@/services/investmentApi'

export const useInvestmentStore = defineStore('investment', () => {
  const holdings = ref<HoldingItem[]>([])
  const summary = ref<HoldingSummary | null>(null)
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

  return { holdings, summary, loading, fetchHoldings, fetchDetail }
})
