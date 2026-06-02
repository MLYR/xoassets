/* 流水 API */
import { request } from './http'

export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'REFUND'

export interface TransactionItem {
  id: string
  type: TransactionType
  amount: number
  accountId: string
  accountName: string | null
  targetAccountId?: string | null
  targetAccountName?: string | null
  categoryId?: string | null
  categoryName?: string | null
  transactionTime: string
  note?: string | null
  imageUrl?: string | null
  status: number
}

export interface TransactionRequest {
  type: TransactionType
  amount: number
  accountId: string
  targetAccountId?: string | null
  categoryId?: string | null
  transactionTime: string
  note?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNo: number
  pageSize: number
}

export const transactionApi = {
  page(params: {
    pageNo?: number
    pageSize?: number
    type?: TransactionType
    accountId?: string
    categoryId?: string
    keyword?: string
  }) {
    return request<PageResult<TransactionItem>>({
      url: '/transactions',
      method: 'GET',
      data: params
    })
  },
  create(data: TransactionRequest) {
    return request<TransactionItem>({
      url: '/transactions',
      method: 'POST',
      data
    })
  },
  update(id: string, data: TransactionRequest) {
    return request<TransactionItem>({
      url: `/transactions/${id}`,
      method: 'PUT',
      data
    })
  },
  remove(id: string) {
    return request<void>({
      url: `/transactions/${id}`,
      method: 'DELETE'
    })
  }
}
