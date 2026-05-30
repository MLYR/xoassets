// 流水 API：封装分页查询、新增、编辑和删除接口。
import { request } from './http';

export type TransactionApiType = 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'REFUND';

export interface TransactionItem {
  id: number;
  type: TransactionApiType;
  amount: number;
  accountId: number;
  accountName: string | null;
  targetAccountId?: number | null;
  targetAccountName?: string | null;
  categoryId?: number | null;
  categoryName?: string | null;
  originalTransactionId?: number | null;
  transactionTime: string;
  note?: string | null;
  status: number;
}

export interface TransactionRequest {
  type: TransactionApiType;
  amount: number;
  accountId: number;
  targetAccountId?: number | null;
  categoryId?: number | null;
  originalTransactionId?: number | null;
  transactionTime: string;
  note?: string;
}

export interface TransactionQuery {
  pageNo?: number;
  pageSize?: number;
  type?: TransactionApiType;
  accountId?: number;
  categoryId?: number;
  keyword?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export const transactionApi = {
  // 分页查询当前登录用户的流水列表。
  page(params: TransactionQuery) {
    return request<PageResult<TransactionItem>>({
      url: '/transactions',
      method: 'GET',
      params
    });
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
  update(id: number, data: TransactionRequest) {
    return request<TransactionItem>({
      url: `/transactions/${id}`,
      method: 'PUT',
      data
    });
  },
  // 删除流水，后端会反向恢复账户余额。
  remove(id: number) {
    return request<void>({
      url: `/transactions/${id}`,
      method: 'DELETE'
    });
  }
};
