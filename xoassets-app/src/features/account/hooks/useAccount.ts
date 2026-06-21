import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { accountApi } from '../api/accountApi';
import type { AccountBalanceAdjustmentRequest, AccountFlowStatisticsQueryParams, AccountLedgerItem, AccountLedgerQueryParams, AccountRequest } from '../api/accountTypes';

export function useAccount(
  enabled: boolean,
  selectedAccountId?: string | null,
  ledgerParams?: AccountLedgerQueryParams,
  flowParams?: AccountFlowStatisticsQueryParams
) {
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
    queryKey: ['account-ledger', selectedAccountId, ledgerParams?.startDate, ledgerParams?.endDate, ledgerParams?.pageNo, ledgerParams?.pageSize],
    queryFn: () => accountApi.ledger(String(selectedAccountId), ledgerParams),
    enabled: enabled && Boolean(selectedAccountId)
  });

  const flowStatisticsQuery = useQuery({
    queryKey: ['account-flow-statistics', selectedAccountId, flowParams?.month, flowParams?.startDate, flowParams?.endDate],
    queryFn: () => accountApi.flowStatistics(String(selectedAccountId), flowParams),
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

  const invalidateHome = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['recent-transactions'] }),
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

  const deleteLedgerItemMutation = useMutation({
    mutationFn: (item: AccountLedgerItem) => {
      if (item.sourceType === 'TRANSACTION') {
        return accountApi.deleteTransaction(String(item.id));
      }
      if (item.sourceType === 'INVESTMENT') {
        // 投资交易后端保留审计记录，移动端删除入口按业务规则映射为撤销。
        return accountApi.revokeInvestmentTransaction(String(item.id));
      }
      throw new Error('该明细暂不支持删除');
    },
    onSuccess: async () => {
      await invalidateAccount();
      await invalidateHome();
    }
  });

  const deleteAccountMutation = useMutation({
    mutationFn: (id: string) => accountApi.deleteAccount(id),
    onSuccess: invalidateAccount
  });

  return {
    overviewQuery,
    listQuery,
    ledgerQuery,
    flowStatisticsQuery,
    createMutation,
    updateMutation,
    adjustBalanceMutation,
    deleteLedgerItemMutation,
    deleteAccountMutation
  };
}
