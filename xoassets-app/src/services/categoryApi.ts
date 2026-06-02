/* 分类 API */
import { request } from './http'

export type CategoryType = 'INCOME' | 'EXPENSE'

export interface CategoryItem {
  id: string
  name: string
  type: CategoryType
  icon?: string | null
  color?: string | null
  status: number
  sortOrder: number
}

export const categoryApi = {
  list(type?: CategoryType) {
    return request<CategoryItem[]>({
      url: '/categories',
      method: 'GET',
      data: type ? { type } : undefined
    })
  }
}
