import { useQuery } from '@tanstack/react-query';

import { homeApi } from '../api/homeApi';

export function useHomeOverview(enabled: boolean) {
  const overviewQuery = useQuery({
    queryKey: ['asset-overview'],
    queryFn: homeApi.overview,
    enabled
  });
  const snapshotQuery = useQuery({
    queryKey: ['latest-snapshot'],
    queryFn: homeApi.latestSnapshot,
    enabled
  });
  const transactionsQuery = useQuery({
    queryKey: ['recent-transactions'],
    queryFn: homeApi.recentTransactions,
    enabled
  });

  return {
    overviewQuery,
    snapshotQuery,
    transactionsQuery
  };
}
