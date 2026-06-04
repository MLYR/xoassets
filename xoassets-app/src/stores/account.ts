/* 账户 Store */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { accountApi, type AccountItem, type AccountLedgerPage, type AccountOverview } from '@/services/accountApi'

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

  return { accounts, overview, loading, fetchAccounts, fetchOverview, fetchLedger }
})
