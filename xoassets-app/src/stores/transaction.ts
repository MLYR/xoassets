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
      const result = await transactionApi.page(params)
      // 翻页加载需要保留已读记录，避免移动端列表加载更多时覆盖第一页。
      if ((params.pageNo || 1) > 1) {
        page.value = {
          ...result,
          records: [...page.value.records, ...result.records]
        }
      } else {
        page.value = result
      }
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
