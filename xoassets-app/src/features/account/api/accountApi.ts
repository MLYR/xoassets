import { request } from '@/api/http';

import type {
  AccountBalanceAdjustmentRequest,
  AccountFlowStatistics,
  AccountItem,
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
  ledger(id: string, pageNo = 1, pageSize = 20) {
    return request<AccountLedgerPage>({
      url: `/api/accounts/${id}/ledger`,
      method: 'GET',
      params: { pageNo, pageSize }
    });
  },
  flowStatistics(id: string) {
    return request<AccountFlowStatistics>({
      url: `/api/accounts/${id}/flow-statistics`,
      method: 'GET'
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
