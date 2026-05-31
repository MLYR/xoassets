// 资产目标 API：封装目标 CRUD 和汇总接口。
import { request } from './http';

export type GoalStatus = 'ACTIVE' | 'DONE';

export interface GoalItem {
  id: string;
  name: string;
  targetAmount: number;
  currentAmount: number;
  targetDate?: string | null;
  status: GoalStatus;
  statusLabel: string;
  completionRate: number;
  remainingAmount: number;
  daysLeft: number;
  monthlyRequiredAmount: number;
}

export interface GoalSummary {
  totalTargetAmount: number;
  totalCurrentAmount: number;
  totalRemainingAmount: number;
  overallCompletionRate: number;
  activeGoalCount: number;
  completedGoalCount: number;
}

export interface GoalRequest {
  name: string;
  targetAmount: number;
  currentAmount: number;
  targetDate?: string | null;
  status: GoalStatus;
  useCurrentNetAssets: boolean;
}

export const goalApi = {
  list() {
    return request<GoalItem[]>({
      url: '/goals',
      method: 'GET'
    });
  },
  create(data: GoalRequest) {
    return request<GoalItem>({
      url: '/goals',
      method: 'POST',
      data
    });
  },
  update(id: string, data: GoalRequest) {
    return request<GoalItem>({
      url: `/goals/${id}`,
      method: 'PUT',
      data
    });
  },
  remove(id: string) {
    return request<void>({
      url: `/goals/${id}`,
      method: 'DELETE'
    });
  },
  summary() {
    return request<GoalSummary>({
      url: '/goals/summary',
      method: 'GET'
    });
  }
};
