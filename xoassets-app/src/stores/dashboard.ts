/* 首页 Store */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardApi, type DashboardOverview } from '@/services/dashboardApi'

export const useDashboardStore = defineStore('dashboard', () => {
  const overview = ref<DashboardOverview | null>(null)
  const loading = ref(false)

  async function fetchOverview(month?: string) {
    loading.value = true
    try {
      overview.value = await dashboardApi.overview(month)
    } finally {
      loading.value = false
    }
  }

  return { overview, loading, fetchOverview }
})
