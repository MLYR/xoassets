import { useQuery } from '@tanstack/react-query';

import { homeApi } from '../api/homeApi';

function getCurrentMonth() {
  const now = new Date();
  const month = `${now.getMonth() + 1}`.padStart(2, '0');
  return `${now.getFullYear()}-${month}`;
}

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
  const budgetMonth = getCurrentMonth();
  const budgetSummaryQuery = useQuery({
    queryKey: ['budget-summary', budgetMonth],
    queryFn: () => homeApi.budgetSummary(budgetMonth),
    enabled
  });
  const reportsQuery = useQuery({
    queryKey: ['ai-reports'],
    queryFn: homeApi.reports,
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
    transactionsQuery,
    budgetSummaryQuery,
    reportsQuery
  };
}
