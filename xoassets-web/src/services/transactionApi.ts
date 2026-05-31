// 流水 API：封装分页查询、新增、编辑和删除接口。
import { request } from './http';

export type TransactionApiType = 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'REFUND';

export interface TransactionItem {
  id: string;
  type: TransactionApiType;
  amount: number;
  accountId: string;
  accountName: string | null;
  targetAccountId?: string | null;
  targetAccountName?: string | null;
  categoryId?: string | null;
  categoryName?: string | null;
  originalTransactionId?: string | null;
  transactionTime: string;
  note?: string | null;
  imageUrl?: string | null;
  status: number;
}

export interface TransactionRequest {
  type: TransactionApiType;
  amount: number;
  accountId: string;
  targetAccountId?: string | null;
  categoryId?: string | null;
  originalTransactionId?: string | null;
  transactionTime: string;
  note?: string;
  imageUrl?: string | null;
}

export interface TransactionQuery {
  pageNo?: number;
  pageSize?: number;
  type?: TransactionApiType;
  accountId?: string;
  categoryId?: string;
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
