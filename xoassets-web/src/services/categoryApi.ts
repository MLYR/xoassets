// 分类 API：封装当前用户分类查询，流水表单按类型加载分类。
import { request } from './http';

export type CategoryType = 'INCOME' | 'EXPENSE';

export interface CategoryItem {
  id: string;
  name: string;
  type: CategoryType;
  icon?: string | null;
  color?: string | null;
  status: number;
  sortOrder: number;
}

export interface CategoryRequest {
  name: string;
  type: CategoryType;
  icon?: string;
  color?: string;
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
  },
  // 新增当前用户自己的分类。
  create(data: CategoryRequest) {
    return request<CategoryItem>({
      url: '/categories',
      method: 'POST',
      data
    });
  },
  // 编辑分类名称、图标、颜色、排序等基础信息。
  update(id: string, data: CategoryRequest) {
    return request<CategoryItem>({
      url: `/categories/${id}`,
      method: 'PUT',
      data
    });
  },
  // 删除未被流水使用的分类，失败信息由后端返回。
  remove(id: string) {
    return request<void>({
      url: `/categories/${id}`,
      method: 'DELETE'
    });
  },
  // 单独启用或停用分类。
  updateStatus(id: string, status: number) {
    return request<CategoryItem>({
      url: `/categories/${id}/status`,
      method: 'PUT',
      data: { status }
    });
  }
};
