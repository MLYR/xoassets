import type { ApiPage } from '@/shared/types/api';
import type { AiReport, AssetSnapshotLatest, BudgetSummary, DashboardOverview, RecentTransaction } from '@/shared/types/asset';

import { request } from '@/api/http';

export const homeApi = {
  overview() {
    return request<DashboardOverview>({
      url: '/api/dashboard/overview',
      method: 'GET'
    });
  },
  latestSnapshot() {
    return request<AssetSnapshotLatest>({
      url: '/api/snapshots/latest',
      method: 'GET'
    });
  },
  budgetSummary(month: string) {
    return request<BudgetSummary>({
      url: '/api/budgets/summary',
      method: 'GET',
      params: {
        month
      }
    });
  },
  reports() {
    return request<AiReport[]>({
      url: '/api/reports',
      method: 'GET'
    });
  },
  recentTransactions() {
    return request<ApiPage<RecentTransaction>>({
      url: '/api/transactions',
      method: 'GET',
      params: {
        current: 1,
        size: 5
      }
    });
  }
};
