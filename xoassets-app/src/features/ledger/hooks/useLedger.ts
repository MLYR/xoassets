import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ledgerApi } from '../api/ledgerApi';
import type { LedgerTransactionRequest } from '../api/ledgerTypes';

function getMonthRange(date: string) {
  const [year, month] = date.split('-').map(Number);
  const first = new Date(year, month - 1, 1);
  const last = new Date(year, month, 0);
  return {
    startDate: formatDate(first),
    endDate: formatDate(last)
  };
}

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function useLedger(selectedDate: string, enabled: boolean, statsRange?: { startDate: string; endDate: string }) {
  const queryClient = useQueryClient();
  const transactionKey = ['ledger-transactions', selectedDate];
  const monthRange = getMonthRange(selectedDate);
  const monthTransactionKey = ['ledger-month-transactions', monthRange.startDate, monthRange.endDate];
  const statsStartDate = statsRange?.startDate ?? monthRange.startDate;
  const statsEndDate = statsRange?.endDate ?? monthRange.endDate;
  const statsTransactionKey = ['ledger-stats-transactions', statsStartDate, statsEndDate];

  const accountsQuery = useQuery({
    queryKey: ['ledger-accounts'],
    queryFn: ledgerApi.accounts,
    enabled
  });
  const expenseCategoriesQuery = useQuery({
    queryKey: ['ledger-categories', 'EXPENSE'],
    queryFn: () => ledgerApi.categories('EXPENSE'),
    enabled
  });
  const incomeCategoriesQuery = useQuery({
    queryKey: ['ledger-categories', 'INCOME'],
    queryFn: () => ledgerApi.categories('INCOME'),
    enabled
  });
  const transactionsQuery = useQuery({
    queryKey: transactionKey,
    queryFn: () => ledgerApi.transactions(selectedDate),
    enabled
  });
  const monthTransactionsQuery = useQuery({
    queryKey: monthTransactionKey,
    queryFn: () => ledgerApi.transactions(monthRange.startDate, monthRange.endDate, 500),
    enabled
  });
  const statsTransactionsQuery = useQuery({
    queryKey: statsTransactionKey,
    queryFn: () => ledgerApi.transactions(statsStartDate, statsEndDate, 1000),
    enabled
  });

  const invalidateLedger = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: transactionKey }),
      queryClient.invalidateQueries({ queryKey: monthTransactionKey }),
      queryClient.invalidateQueries({ queryKey: statsTransactionKey }),
      queryClient.invalidateQueries({ queryKey: ['recent-transactions'] }),
      queryClient.invalidateQueries({ queryKey: ['asset-overview'] }),
      queryClient.invalidateQueries({ queryKey: ['latest-snapshot'] })
    ]);
  };

  const createMutation = useMutation({
    mutationFn: (data: LedgerTransactionRequest) => ledgerApi.createTransaction(data),
    onSuccess: invalidateLedger
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: LedgerTransactionRequest }) => ledgerApi.updateTransaction(id, data),
    onSuccess: invalidateLedger
  });
  const deleteMutation = useMutation({
    mutationFn: (id: string) => ledgerApi.deleteTransaction(id),
    onSuccess: invalidateLedger
  });

  return {
    accountsQuery,
    expenseCategoriesQuery,
    incomeCategoriesQuery,
    transactionsQuery,
    monthTransactionsQuery,
    statsTransactionsQuery,
    createMutation,
    updateMutation,
    deleteMutation
  };
}
