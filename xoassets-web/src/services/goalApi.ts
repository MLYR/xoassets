// 资产目标 API：封装目标 CRUD 和汇总接口。
import { request } from './http';

/** 目标状态。 */
export type GoalStatus = 'ACTIVE' | 'DONE';

/** 目标列表项。 */
export interface GoalItem {
  /** ID。 */
  id: string;
  /** 名称。 */
  name: string;
  /** 目标金额。 */
  targetAmount: number;
  /** 当前金额。 */
  currentAmount: number;
  /** 目标日期。 */
  targetDate?: string | null;
  /** 状态。 */
  status: GoalStatus;
  /** 状态文案。 */
  statusLabel: string;
  /** 完成率。 */
  completionRate: number;
  /** 剩余金额。 */
  remainingAmount: number;
  /** 剩余天数。 */
  daysLeft: number;
  /** 每月需存金额。 */
  monthlyRequiredAmount: number;
}

/** 目标汇总。 */
export interface GoalSummary {
  /** 目标总额。 */
  totalTargetAmount: number;
  /** 当前总额。 */
  totalCurrentAmount: number;
  /** 剩余总额。 */
  totalRemainingAmount: number;
  /** 整体完成率。 */
  overallCompletionRate: number;
  /** 进行中目标数。 */
  activeGoalCount: number;
  /** 已完成目标数。 */
  completedGoalCount: number;
}

/** 目标保存参数。 */
export interface GoalRequest {
  /** 名称。 */
  name: string;
  /** 目标金额。 */
  targetAmount: number;
  /** 当前金额。 */
  currentAmount: number;
  /** 目标日期。 */
  targetDate?: string | null;
  /** 状态。 */
  status: GoalStatus;
  /** 是否使用当前净资产。 */
  useCurrentNetAssets: boolean;
}

export const goalApi = {
  // 查询列表。
  list() {
    return request<GoalItem[]>({
      url: '/goals',
      method: 'GET'
    });
  },
  // 创建数据。
  create(data: GoalRequest) {
    return request<GoalItem>({
      url: '/goals',
      method: 'POST',
      data
    });
  },
  // 更新数据。
  update(id: string, data: GoalRequest) {
    return request<GoalItem>({
      url: `/goals/${id}`,
      method: 'PUT',
      data
    });
  },
  // 删除数据。
  remove(id: string) {
    return request<void>({
      url: `/goals/${id}`,
      method: 'DELETE'
    });
  },
  // 查询汇总。
  summary() {
    return request<GoalSummary>({
      url: '/goals/summary',
      method: 'GET'
    });
  }
};
