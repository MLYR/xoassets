// 账户 API：封装账户列表、新增、编辑和删除接口。
import { request } from './http';

/** 账户列表项。 */
export interface AccountItem {
  /** ID。 */
  id: string;
  /** 名称。 */
  name: string;
  /** 类型。 */
  type: string;
  /** 余额。 */
  balance: number;
  /** 初始余额。 */
  initialBalance: number;
  /** 币种。 */
  currency: string;
  /** 状态。 */
  status: number;
  /** 排序。 */
  sortOrder: number;
  /** 备注。 */
  remark?: string | null;
}

/** 账户保存参数。 */
export interface AccountRequest {
  /** 名称。 */
  name: string;
  /** 类型。 */
  type: string;
  /** 初始余额。 */
  initialBalance: number;
  /** 余额。 */
  balance?: number;
  /** 币种。 */
  currency: string;
  /** 状态。 */
  status: number;
  /** 排序。 */
  sortOrder: number;
  /** 备注。 */
  remark?: string;
}

/** 账户账本来源类型。 */
export type AccountLedgerSourceType = 'TRANSACTION' | 'INVESTMENT' | 'ADJUSTMENT';
/** 账户账本业务类型。 */
export type AccountLedgerBizType = 'INCOME' | 'EXPENSE' | 'TRANSFER_OUT' | 'TRANSFER_IN' | 'REFUND' | 'INVEST_BUY' | 'INVEST_SELL' | 'BALANCE_ADJUSTMENT';

/** 账户账本明细。 */
export interface AccountLedgerItem {
  /** ID。 */
  id: string;
  /** 来源类型。 */
  sourceType: AccountLedgerSourceType;
  /** 业务类型。 */
  bizType: AccountLedgerBizType;
  /** 标题。 */
  title: string;
  /** 金额。 */
  amount: number;
  /** 账户ID。 */
  accountId: string;
  /** 账户名称。 */
  accountName: string | null;
  /** 关联账户ID。 */
  relatedAccountId?: string | null;
  /** 关联账户名称。 */
  relatedAccountName?: string | null;
  /** 分类ID。 */
  categoryId?: string | null;
  /** 分类名称。 */
  categoryName?: string | null;
  /** 资产ID。 */
  assetId?: string | null;
  /** 资产名称。 */
  assetName?: string | null;
  /** 代码。 */
  symbol?: string | null;
  /** 状态。 */
  status?: string | null;
  /** 交易时间。 */
  transactionTime: string;
  /** 备注。 */
  note?: string | null;
}

/** 账户账本汇总。 */
export interface AccountLedgerSummary {
  /** 当前余额。 */
  currentBalance: number;
  /** 初始余额。 */
  initialBalance: number;
  /** 累计流入。 */
  totalInflow: number;
  /** 累计流出。 */
  totalOutflow: number;
  /** 净流入。 */
  netInflow: number;
  /** 流水数量。 */
  transactionCount: number;
}

/** 账户账本查询参数。 */
export interface AccountLedgerQuery {
  /** 页码。 */
  pageNo?: number;
  /** 每页条数。 */
  pageSize?: number;
  /** 类型。 */
  type?: AccountLedgerBizType | '';
  /** 开始日期。 */
  startDate?: string;
  /** 结束日期。 */
  endDate?: string;
  /** 关键词。 */
  keyword?: string;
}

/** 分页结果。 */
export interface PageResult<T> {
  /** 分页记录。 */
  records: T[];
  /** 总数。 */
  total: number;
  /** 页码。 */
  pageNo: number;
  /** 每页条数。 */
  pageSize: number;
}

/** 账户账本分页数据。 */
export interface AccountLedgerPage {
  /** 账户。 */
  account: AccountItem;
  /** 摘要。 */
  summary: AccountLedgerSummary;
  /** 分页数据。 */
  page: PageResult<AccountLedgerItem>;
}

type RawPageResult<T> = Omit<PageResult<T>, 'total' | 'pageNo' | 'pageSize'> & {
  total: number | string;
  pageNo: number | string;
  pageSize: number | string;
};

type RawAccountLedgerPage = Omit<AccountLedgerPage, 'page'> & {
  page: RawPageResult<AccountLedgerItem>;
};

// 后端 Long 会序列化成字符串；分页组件要求 number，这里只转换分页元数据。
function normalizePageResult<T>(result: RawPageResult<T>): PageResult<T> {
  return {
    ...result,
    total: Number(result.total || 0),
    pageNo: Number(result.pageNo || 1),
    pageSize: Number(result.pageSize || 10)
  };
}

/** 账户资金统计。 */
export interface AccountFlowStatistics {
  /** 修正金额。 */
  adjustmentAmount: number;
  /** 分类支出统计。 */
  categoryExpenseStats: Array<{ name: string; amount: number }>;
  /** 每日余额趋势。 */
  dailyBalanceTrend: Array<{ date: string; endBalance: number; inflow: number; outflow: number; adjustmentAmount: number }>;
}

/** 账户资金统计查询参数。 */
export interface AccountFlowStatisticsQuery {
  /** 开始日期。 */
  startDate?: string;
  /** 结束日期。 */
  endDate?: string;
}

/** 余额修正参数。 */
export interface AccountBalanceAdjustmentRequest {
  /** 修正后余额。 */
  afterBalance: number;
  /** 原因。 */
  reason?: string;
  /** 业务日期。 */
  bizDate?: string;
  /** 业务时间。 */
  bizTime?: string;
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
  // 余额修正生成专用调整事件，不计入普通收支。
  adjustBalance(id: string, data: AccountBalanceAdjustmentRequest) {
    return request({
      url: `/accounts/${id}/balance-adjustments`,
      method: 'POST',
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
  async ledger(id: string, params: AccountLedgerQuery) {
    const result = await request<RawAccountLedgerPage>({
      url: `/accounts/${id}/ledger`,
      method: 'GET',
      params
    });
    return {
      ...result,
      page: normalizePageResult(result.page)
    };
  },
  // 查询账户详情统计，供余额曲线和支出分类图使用。
  flowStatistics(id: string, params: AccountFlowStatisticsQuery) {
    return request<AccountFlowStatistics>({
      url: `/accounts/${id}/flow-statistics`,
      method: 'GET',
      params
    });
  }
};
