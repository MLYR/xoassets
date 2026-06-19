import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { investmentApi } from '../api/investmentApi';
import type { AssetRequest, AssetType, InvestmentModule, InvestmentPeriod, InvestmentTransactionRequest } from '../api/investmentTypes';

export function useInvestment(module: InvestmentModule, period: InvestmentPeriod, year: number, month: number, enabled: boolean) {
  const queryClient = useQueryClient();

  const overviewQuery = useQuery({
    queryKey: ['investment-overview'],
    queryFn: investmentApi.overview,
    enabled
  });
  const holdingsQuery = useQuery({
    queryKey: ['investment-holdings', module],
    queryFn: () => investmentApi.holdings(module),
    enabled
  });
  const trendQuery = useQuery({
    queryKey: ['investment-trend', module, period],
    queryFn: () => investmentApi.trend(module, period),
    enabled
  });
  const calendarQuery = useQuery({
    queryKey: ['investment-calendar', year, month],
    queryFn: () => investmentApi.dailyProfit(year, month),
    enabled
  });
  const transactionsQuery = useQuery({
    queryKey: ['investment-transactions'],
    queryFn: () => investmentApi.transactions(),
    enabled
  });
  const accountsQuery = useQuery({
    queryKey: ['investment-accounts'],
    queryFn: investmentApi.accounts,
    enabled
  });

  const invalidateInvestment = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['investment-overview'] }),
      queryClient.invalidateQueries({ queryKey: ['investment-holdings'] }),
      queryClient.invalidateQueries({ queryKey: ['investment-trend'] }),
      queryClient.invalidateQueries({ queryKey: ['investment-calendar'] }),
      queryClient.invalidateQueries({ queryKey: ['investment-transactions'] }),
      queryClient.invalidateQueries({ queryKey: ['asset-overview'] }),
      queryClient.invalidateQueries({ queryKey: ['latest-snapshot'] })
    ]);
  };

  const lookupMutation = useMutation({
    mutationFn: ({ type, keyword, market }: { type: AssetType; keyword: string; market?: string }) => investmentApi.lookupAssets(type, keyword, market)
  });
  const createAssetMutation = useMutation({
    mutationFn: (data: AssetRequest) => investmentApi.createAsset(data)
  });
  const createTransactionMutation = useMutation({
    mutationFn: (data: InvestmentTransactionRequest) => investmentApi.createTransaction(data),
    onSuccess: invalidateInvestment
  });
  const refreshQuotesMutation = useMutation({
    mutationFn: (assetIds: string[]) => investmentApi.refreshQuotes({ assetIds }),
    onSuccess: invalidateInvestment
  });

  return {
    overviewQuery,
    holdingsQuery,
    trendQuery,
    calendarQuery,
    transactionsQuery,
    accountsQuery,
    lookupMutation,
    createAssetMutation,
    createTransactionMutation,
    refreshQuotesMutation
  };
}
