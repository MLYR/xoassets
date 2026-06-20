import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { budgetApi } from '../api/budgetApi';
import type { BudgetRequest } from '../api/budgetTypes';

export function useBudget(month: string, enabled: boolean) {
  const queryClient = useQueryClient();

  const summaryQuery = useQuery({
    queryKey: ['budget-summary', month],
    queryFn: () => budgetApi.summary(month),
    enabled
  });

  const listQuery = useQuery({
    queryKey: ['budgets', month],
    queryFn: () => budgetApi.list(month),
    enabled
  });

  const categoriesQuery = useQuery({
    queryKey: ['budget-expense-categories'],
    queryFn: budgetApi.expenseCategories,
    enabled
  });

  const invalidateBudget = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['budget-summary'] }),
      queryClient.invalidateQueries({ queryKey: ['budgets'] }),
      queryClient.invalidateQueries({ queryKey: ['asset-overview'] }),
      queryClient.invalidateQueries({ queryKey: ['latest-snapshot'] })
    ]);
  };

  const createMutation = useMutation({
    mutationFn: (data: BudgetRequest) => budgetApi.create(data),
    onSuccess: invalidateBudget
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: BudgetRequest }) => budgetApi.update(id, data),
    onSuccess: invalidateBudget
  });

  return {
    summaryQuery,
    listQuery,
    categoriesQuery,
    createMutation,
    updateMutation
  };
}
