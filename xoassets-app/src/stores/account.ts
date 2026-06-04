/* 账户 Store */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { accountApi, type AccountItem, type AccountLedgerPage, type AccountOverview, type AccountRequest } from '@/services/accountApi'

export const useAccountStore = defineStore('account', () => {
  const accounts = ref<AccountItem[]>([])
  const overview = ref<AccountOverview | null>(null)
  const loading = ref(false)

  async function fetchAccounts() {
    loading.value = true
    try {
      accounts.value = await accountApi.list()
    } finally {
      loading.value = false
    }
  }

  async function fetchOverview() {
    loading.value = true
    try {
      overview.value = await accountApi.overview()
      accounts.value = overview.value.accounts
      return overview.value
    } finally {
      loading.value = false
    }
  }

  async function fetchLedger(id: string, params: any) {
    return accountApi.ledger(id, params)
  }

  async function fetchFlowStatistics(id: string, params: any) {
    return accountApi.flowStatistics(id, params)
  }

  async function updateAccount(id: string, data: AccountRequest) {
    const updated = await accountApi.update(id, data)
    const index = accounts.value.findIndex(item => item.id === id)
    if (index >= 0) accounts.value[index] = updated
    return updated
  }

  return { accounts, overview, loading, fetchAccounts, fetchOverview, fetchLedger, fetchFlowStatistics, updateAccount }
})
