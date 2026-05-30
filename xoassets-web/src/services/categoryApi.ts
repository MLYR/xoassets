// 分类 API：封装当前用户分类查询，流水表单按类型加载分类。
import { request } from './http';

export type CategoryType = 'INCOME' | 'EXPENSE';

export interface CategoryItem {
  id: number;
  name: string;
  type: CategoryType;
  icon?: string | null;
  color?: string | null;
  status: number;
  sortOrder: number;
}

export const categoryApi = {
  // 查询当前登录用户分类，可按收入或支出类型过滤。
  list(type?: CategoryType) {
    return request<CategoryItem[]>({
      url: '/categories',
      method: 'GET',
      params: type ? { type } : undefined
    });
  }
};
