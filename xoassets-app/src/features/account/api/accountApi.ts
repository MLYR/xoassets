import { request } from '@/api/http';

import type {
  AccountBalanceAdjustmentRequest,
  AccountFlowStatisticsQueryParams,
  AccountFlowStatistics,
  AccountItem,
  AccountLedgerQueryParams,
  AccountLedgerPage,
  AccountOverview,
  AccountRequest
} from './accountTypes';

export const accountApi = {
  overview() {
    return request<AccountOverview>({
      url: '/api/accounts/overview',
      method: 'GET'
    });
  },
  list() {
    return request<AccountItem[]>({
      url: '/api/accounts',
      method: 'GET'
    });
  },
  create(data: AccountRequest) {
    return request<AccountItem>({
      url: '/api/accounts',
      method: 'POST',
      data
    });
  },
  update(id: string, data: AccountRequest) {
    return request<AccountItem>({
      url: `/api/accounts/${id}`,
      method: 'PUT',
      data
    });
  },
  ledger(id: string, params: AccountLedgerQueryParams = {}) {
    return request<AccountLedgerPage>({
      url: `/api/accounts/${id}/ledger`,
      method: 'GET',
      params: {
        pageNo: params.pageNo ?? 1,
        pageSize: params.pageSize ?? 30,
        startDate: params.startDate,
        endDate: params.endDate
      }
    });
  },
  flowStatistics(id: string, params: AccountFlowStatisticsQueryParams = {}) {
    return request<AccountFlowStatistics>({
      url: `/api/accounts/${id}/flow-statistics`,
      method: 'GET',
      params
    });
  },
  deleteAccount(id: string) {
    return request<void>({
      url: `/api/accounts/${id}`,
      method: 'DELETE'
    });
  },
  deleteTransaction(id: string) {
    return request<void>({
      url: `/api/transactions/${id}`,
      method: 'DELETE'
    });
  },
  revokeInvestmentTransaction(id: string) {
    return request<void>({
      url: `/api/investment-transactions/${id}/revoke`,
      method: 'PUT'
    });
  },
  adjustBalance(id: string, data: AccountBalanceAdjustmentRequest) {
    return request<unknown>({
      url: `/api/accounts/${id}/balance-adjustments`,
      method: 'POST',
      data
    });
  }
};
