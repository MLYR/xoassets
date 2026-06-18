import type { ApiPage } from '@/shared/types/api';
import type { AssetSnapshotLatest, DashboardOverview, RecentTransaction } from '@/shared/types/asset';

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
  recentTransactions() {
    return request<ApiPage<RecentTransaction>>({
      url: '/api/transactions',
      method: 'GET',
      params: {
        current: 1,
        size: 3
      }
    });
  }
};
