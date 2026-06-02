/* 账户 Store */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { accountApi, type AccountItem, type AccountLedgerPage } from '@/services/accountApi'

export const useAccountStore = defineStore('account', () => {
  const accounts = ref<AccountItem[]>([])
  const loading = ref(false)

  async function fetchAccounts() {
    loading.value = true
    try {
      accounts.value = await accountApi.list()
    } finally {
      loading.value = false
    }
  }

  async function fetchLedger(id: string, params: any) {
    return accountApi.ledger(id, params)
  }

  return { accounts, loading, fetchAccounts, fetchLedger }
})
