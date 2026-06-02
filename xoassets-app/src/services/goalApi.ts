/* 资产目标 API */
import { request } from './http'

export interface GoalItem {
  id: string
  name: string
  targetAmount: number
  currentAmount: number
  targetDate?: string | null
  status: string
  statusLabel: string
  completionRate: number
  remainingAmount: number
  daysLeft: number
  monthlyRequiredAmount: number
}

export const goalApi = {
  list() {
    return request<GoalItem[]>({ url: '/goals', method: 'GET' })
  },
  summary() {
    return request<{
      totalTargetAmount: number
      totalCurrentAmount: number
      overallCompletionRate: number
      activeGoalCount: number
      completedGoalCount: number
    }>({ url: '/goals/summary', method: 'GET' })
  }
}
