// 流水 API：封装分页查询、新增、编辑和删除接口。
import { request } from './http';

/** TransactionApiType 类型。 */
export type TransactionApiType = 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'REFUND';

/** 流水列表项。 */
export interface TransactionItem {
  /** ID。 */
  id: string;
  /** 类型。 */
  type: TransactionApiType;
  /** 金额。 */
  amount: number;
  /** 账户ID。 */
  accountId: string;
  /** 账户名称。 */
  accountName: string | null;
  /** 转入账户ID。 */
  targetAccountId?: string | null;
  /** 转入账户名称。 */
  targetAccountName?: string | null;
  /** 分类ID。 */
  categoryId?: string | null;
  /** 分类名称。 */
  categoryName?: string | null;
  /** 原流水ID。 */
  originalTransactionId?: string | null;
  /** 交易时间。 */
  transactionTime: string;
  /** 备注。 */
  note?: string | null;
  /** 图片地址。 */
  imageUrl?: string | null;
  /** 状态。 */
  status: number;
}

/** 流水保存参数。 */
export interface TransactionRequest {
  /** 类型。 */
  type: TransactionApiType;
  /** 金额。 */
  amount: number;
  /** 账户ID。 */
  accountId: string;
  /** 转入账户ID。 */
  targetAccountId?: string | null;
  /** 分类ID。 */
  categoryId?: string | null;
  /** 原流水ID。 */
  originalTransactionId?: string | null;
  /** 交易时间。 */
  transactionTime: string;
  /** 备注。 */
  note?: string;
  /** 图片地址。 */
  imageUrl?: string | null;
}

/** 流水查询参数。 */
export interface TransactionQuery {
  /** 页码。 */
  pageNo?: number;
  /** 每页条数。 */
  pageSize?: number;
  /** 类型。 */
  type?: TransactionApiType;
  /** 账户ID。 */
  accountId?: string;
  /** 分类ID。 */
  categoryId?: string;
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

type RawPageResult<T> = Omit<PageResult<T>, 'total' | 'pageNo' | 'pageSize'> & {
  total: number | string;
  pageNo: number | string;
  pageSize: number | string;
};

// 后端会把 Long 统一序列化成字符串；分页组件需要 number，这里只转换分页元数据，不碰业务 ID。
function normalizePageResult<T>(result: RawPageResult<T>): PageResult<T> {
  return {
    ...result,
    total: Number(result.total || 0),
    pageNo: Number(result.pageNo || 1),
    pageSize: Number(result.pageSize || 10)
  };
}

export const transactionApi = {
  // 分页查询当前登录用户的流水列表。
  async page(params: TransactionQuery) {
    const result = await request<RawPageResult<TransactionItem>>({
      url: '/transactions',
      method: 'GET',
      params
    });
    return normalizePageResult(result);
  },
  // 新增流水，后端会同步调整账户余额。
  create(data: TransactionRequest) {
    return request<TransactionItem>({
      url: '/transactions',
      method: 'POST',
      data
    });
  },
  // 编辑流水，后端会先反向恢复旧余额影响再应用新流水。
  update(id: string, data: TransactionRequest) {
    return request<TransactionItem>({
      url: `/transactions/${id}`,
      method: 'PUT',
      data
    });
  },
  // 删除流水，后端会反向恢复账户余额。
  remove(id: string) {
    return request<void>({
      url: `/transactions/${id}`,
      method: 'DELETE'
    });
  }
};
