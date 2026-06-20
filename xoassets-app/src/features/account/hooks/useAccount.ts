import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { accountApi } from '../api/accountApi';
import type { AccountBalanceAdjustmentRequest, AccountRequest } from '../api/accountTypes';

export function useAccount(enabled: boolean, selectedAccountId?: string | null) {
  const queryClient = useQueryClient();

  const overviewQuery = useQuery({
    queryKey: ['account-overview'],
    queryFn: accountApi.overview,
    enabled
  });

  const listQuery = useQuery({
    queryKey: ['accounts'],
    queryFn: accountApi.list,
    enabled
  });

  const ledgerQuery = useQuery({
    queryKey: ['account-ledger', selectedAccountId],
    queryFn: () => accountApi.ledger(String(selectedAccountId)),
    enabled: enabled && Boolean(selectedAccountId)
  });

  const flowStatisticsQuery = useQuery({
    queryKey: ['account-flow-statistics', selectedAccountId],
    queryFn: () => accountApi.flowStatistics(String(selectedAccountId)),
    enabled: enabled && Boolean(selectedAccountId)
  });

  const invalidateAccount = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['account-overview'] }),
      queryClient.invalidateQueries({ queryKey: ['accounts'] }),
      queryClient.invalidateQueries({ queryKey: ['account-ledger'] }),
      queryClient.invalidateQueries({ queryKey: ['account-flow-statistics'] }),
      queryClient.invalidateQueries({ queryKey: ['ledger-accounts'] }),
      queryClient.invalidateQueries({ queryKey: ['investment-accounts'] }),
      queryClient.invalidateQueries({ queryKey: ['asset-overview'] }),
      queryClient.invalidateQueries({ queryKey: ['latest-snapshot'] })
    ]);
  };

  const createMutation = useMutation({
    mutationFn: (data: AccountRequest) => accountApi.create(data),
    onSuccess: invalidateAccount
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: AccountRequest }) => accountApi.update(id, data),
    onSuccess: invalidateAccount
  });

  const adjustBalanceMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: AccountBalanceAdjustmentRequest }) => accountApi.adjustBalance(id, data),
    onSuccess: invalidateAccount
  });

  return {
    overviewQuery,
    listQuery,
    ledgerQuery,
    flowStatisticsQuery,
    createMutation,
    updateMutation,
    adjustBalanceMutation
  };
}
