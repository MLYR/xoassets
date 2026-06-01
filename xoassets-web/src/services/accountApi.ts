// 账户 API：封装账户列表、新增、编辑和删除接口。
import { request } from './http';

export interface AccountItem {
  id: string;
  name: string;
  type: string;
  balance: number;
  initialBalance: number;
  currency: string;
  status: number;
  sortOrder: number;
  remark?: string | null;
}

export interface AccountRequest {
  name: string;
  type: string;
  initialBalance: number;
  balance?: number;
  currency: string;
  status: number;
  sortOrder: number;
  remark?: string;
}

export type AccountLedgerSourceType = 'TRANSACTION' | 'INVESTMENT';
export type AccountLedgerBizType = 'INCOME' | 'EXPENSE' | 'TRANSFER_OUT' | 'TRANSFER_IN' | 'REFUND' | 'INVEST_BUY' | 'INVEST_SELL';

export interface AccountLedgerItem {
  id: string;
  sourceType: AccountLedgerSourceType;
  bizType: AccountLedgerBizType;
  title: string;
  amount: number;
  accountId: string;
  accountName: string | null;
  relatedAccountId?: string | null;
  relatedAccountName?: string | null;
  categoryId?: string | null;
  categoryName?: string | null;
  assetId?: string | null;
  assetName?: string | null;
  symbol?: string | null;
  status?: string | null;
  transactionTime: string;
  note?: string | null;
}

export interface AccountLedgerSummary {
  currentBalance: number;
  initialBalance: number;
  totalInflow: number;
  totalOutflow: number;
  netInflow: number;
  transactionCount: number;
}

export interface AccountLedgerQuery {
  pageNo?: number;
  pageSize?: number;
  type?: AccountLedgerBizType | '';
  startDate?: string;
  endDate?: string;
  keyword?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export interface AccountLedgerPage {
  account: AccountItem;
  summary: AccountLedgerSummary;
  page: PageResult<AccountLedgerItem>;
}

export interface AccountFlowStatistics {
  incomeAmount: number;
  expenseAmount: number;
  transferInAmount: number;
  transferOutAmount: number;
  investmentBuyAmount: number;
  investmentSellAmount: number;
  netFlowAmount: number;
  categoryExpenseStats: Array<{ name: string; amount: number }>;
  investmentFlowStats: Array<{ name: string; amount: number }>;
  dailyFlowTrend: Array<{ date: string; inflow: number; outflow: number; netFlow: number }>;
}

export interface AccountFlowStatisticsQuery {
  month?: string;
  startDate?: string;
  endDate?: string;
}

export const accountApi = {
  // 查询当前登录用户的账户列表。
  list() {
    return request<AccountItem[]>({
      url: '/accounts',
      method: 'GET'
    });
  },
  // 新增账户，初始余额由后端同步为当前余额。
  create(data: AccountRequest) {
    return request<AccountItem>({
      url: '/accounts',
      method: 'POST',
      data
    });
  },
  // 编辑账户基础信息和当前余额，余额可用于利息、漏记流水等现实差异校准。
  update(id: string, data: AccountRequest) {
    return request<AccountItem>({
      url: `/accounts/${id}`,
      method: 'PUT',
      data
    });
  },
  // 删除账户；如果后端拒绝删除，错误会透传给页面提示。
  remove(id: string) {
    return request<void>({
      url: `/accounts/${id}`,
      method: 'DELETE'
    });
  },
  // 查询账户资金明细，聚合普通流水和投资交易。
  ledger(id: string, params: AccountLedgerQuery) {
    return request<AccountLedgerPage>({
      url: `/accounts/${id}/ledger`,
      method: 'GET',
      params
    });
  },
  // 查询账户资金流向统计。
  flowStatistics(id: string, params: AccountFlowStatisticsQuery) {
    return request<AccountFlowStatistics>({
      url: `/accounts/${id}/flow-statistics`,
      method: 'GET',
      params
    });
  }
};
