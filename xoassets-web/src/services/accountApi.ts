// 账户 API：封装账户列表、新增、编辑和删除接口。
import { request } from './http';

export interface AccountItem {
  id: number;
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
  currency: string;
  status: number;
  sortOrder: number;
  remark?: string;
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
  // 编辑账户基础信息，当前余额仍由流水负责修正。
  update(id: number, data: AccountRequest) {
    return request<AccountItem>({
      url: `/accounts/${id}`,
      method: 'PUT',
      data
    });
  },
  // 删除账户；如果后端拒绝删除，错误会透传给页面提示。
  remove(id: number) {
    return request<void>({
      url: `/accounts/${id}`,
      method: 'DELETE'
    });
  }
};
