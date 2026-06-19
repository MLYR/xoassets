import type { ApiPage } from '@/shared/types/api';

import { request } from '@/api/http';
import type { LedgerAccount, LedgerCategory, LedgerTransaction, LedgerTransactionRequest } from './ledgerTypes';

export const ledgerApi = {
  accounts() {
    return request<LedgerAccount[]>({
      url: '/api/accounts',
      method: 'GET'
    });
  },
  categories(type?: 'EXPENSE' | 'INCOME') {
    return request<LedgerCategory[]>({
      url: '/api/categories',
      method: 'GET',
      params: type ? { type } : undefined
    });
  },
  transactions(startDate: string, endDate = startDate, pageSize = 50) {
    return request<ApiPage<LedgerTransaction>>({
      url: '/api/transactions',
      method: 'GET',
      params: {
        pageNo: 1,
        pageSize,
        startDate,
        endDate
      }
    });
  },
  createTransaction(data: LedgerTransactionRequest) {
    return request<LedgerTransaction>({
      url: '/api/transactions',
      method: 'POST',
      data
    });
  },
  updateTransaction(id: string, data: LedgerTransactionRequest) {
    return request<LedgerTransaction>({
      url: `/api/transactions/${id}`,
      method: 'PUT',
      data
    });
  },
  deleteTransaction(id: string) {
    return request<void>({
      url: `/api/transactions/${id}`,
      method: 'DELETE'
    });
  }
};
