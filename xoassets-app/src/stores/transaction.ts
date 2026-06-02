/* 流水 Store */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  transactionApi,
  type TransactionItem,
  type TransactionRequest,
  type TransactionType,
  type PageResult
} from '@/services/transactionApi'

export const useTransactionStore = defineStore('transaction', () => {
  const page = ref<PageResult<TransactionItem>>({ records: [], total: 0, pageNo: 1, pageSize: 20 })
  const loading = ref(false)

  async function fetchPage(params: {
    pageNo?: number
    pageSize?: number
    type?: TransactionType
    accountId?: string
    categoryId?: string
    keyword?: string
  }) {
    loading.value = true
    try {
      page.value = await transactionApi.page(params)
    } finally {
      loading.value = false
    }
  }

  async function create(data: TransactionRequest) {
    return transactionApi.create(data)
  }

  async function remove(id: string) {
    await transactionApi.remove(id)
  }

  return { page, loading, fetchPage, create, remove }
})
