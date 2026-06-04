/* 账户 API */
import { request } from './http'

export interface AccountItem {
  id: string
  name: string
  type: string
  balance: number
  initialBalance: number
  currency: string
  status: number
  sortOrder: number
  remark?: string | null
}

export interface AccountRequest {
  name: string
  type: string
  initialBalance: number
  balance?: number
  currency?: string
  status?: number
  sortOrder?: number
  remark?: string | null
}

export interface AccountDisplayItem extends AccountItem {
  displayType: string
  maskedNo?: string | null
  group: AccountGroup
  isDefault: boolean
  tagText?: string | null
  availableCredit?: number | null
}

export type AccountGroup = 'BANK_CARD' | 'CASH' | 'THIRD_PARTY'

export interface AccountCategorySummary {
  group: AccountGroup
  label: string
  amount: number
  ratio: number
  count: number
  colorKey: 'bankCard' | 'cash' | 'thirdParty' | string
}

export interface AccountOverview {
  totalAsset: number
  lastMonthChangeAmount: number
  lastMonthChangeRate: number
  compareAvailable: boolean
  accountCount: number
  nonCreditAssetTotal: number
  nonZeroAccountCount: number
  categories: AccountCategorySummary[]
  accounts: AccountDisplayItem[]
}

export type AccountLedgerBizType = 'INCOME' | 'EXPENSE' | 'TRANSFER_OUT' | 'TRANSFER_IN' | 'REFUND' | 'INVEST_BUY' | 'INVEST_SELL'

export interface AccountLedgerItem {
  id: string
  sourceType: 'TRANSACTION' | 'INVESTMENT'
  bizType: AccountLedgerBizType
  title: string
  amount: number
  accountName: string | null
  relatedAccountName?: string | null
  categoryName?: string | null
  assetName?: string | null
  symbol?: string | null
  transactionTime: string
  note?: string | null
}

export interface AccountLedgerSummary {
  currentBalance: number
  initialBalance: number
  totalInflow: number
  totalOutflow: number
  netInflow: number
  transactionCount: number
}

export interface AccountFlowNameAmountItem {
  name: string
  amount: number
}

export interface AccountDailyFlowItem {
  date: string
  inflow: number
  outflow: number
  netFlow: number
}

export interface AccountFlowStatistics {
  incomeAmount: number
  expenseAmount: number
  transferInAmount: number
  transferOutAmount: number
  investmentBuyAmount: number
  investmentSellAmount: number
  netFlowAmount: number
  categoryExpenseStats: AccountFlowNameAmountItem[]
  investmentFlowStats: AccountFlowNameAmountItem[]
  dailyFlowTrend: AccountDailyFlowItem[]
}

export interface AccountLedgerPage {
  account: AccountItem
  summary: AccountLedgerSummary
  page: PageResult<AccountLedgerItem>
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNo: number
  pageSize: number
}

export const accountApi = {
  list() {
    return request<AccountItem[]>({ url: '/accounts', method: 'GET' })
  },
  overview() {
    return request<AccountOverview>({ url: '/accounts/overview', method: 'GET' })
  },
  create(data: AccountRequest) {
    return request<AccountItem>({
      url: '/accounts',
      method: 'POST',
      data
    })
  },
  update(id: string, data: AccountRequest) {
    return request<AccountItem>({
      url: `/accounts/${id}`,
      method: 'PUT',
      data
    })
  },
  ledger(id: string, params: {
    pageNo?: number
    pageSize?: number
    type?: AccountLedgerBizType | ''
    startDate?: string
    endDate?: string
    keyword?: string
  }) {
    return request<AccountLedgerPage>({
      url: `/accounts/${id}/ledger`,
      method: 'GET',
      data: params
    })
  },
  flowStatistics(id: string, params: {
    month?: string
    startDate?: string
    endDate?: string
  }) {
    return request<AccountFlowStatistics>({
      url: `/accounts/${id}/flow-statistics`,
      method: 'GET',
      data: params
    })
  }
}
