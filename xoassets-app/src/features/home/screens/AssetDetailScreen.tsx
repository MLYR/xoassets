import { useQuery } from '@tanstack/react-query';
import { Redirect, router } from 'expo-router';
import { ChevronLeft, LineChart, WalletCards } from 'lucide-react-native';
import { useEffect, useMemo } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Card, CardContent, Separator, Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';
import { accountApi } from '@/features/account/api/accountApi';
import type { AccountCategorySummary, AccountItem } from '@/features/account/api/accountTypes';
import { investmentApi } from '@/features/investment/api/investmentApi';
import type { InvestmentModuleAsset } from '@/features/investment/api/investmentTypes';
import { useAuthStore } from '@/stores/authStore';

import { homeApi } from '../api/homeApi';
import { formatMoney, formatPercent, formatSignedMoney } from '../utils/formatters';

export function AssetDetailScreen() {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const { isHydrated, isLoggedIn, restoreToken } = useAuthStore();

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  const snapshotQuery = useQuery({
    queryKey: ['asset-detail-snapshot'],
    queryFn: homeApi.latestSnapshot,
    enabled: isLoggedIn
  });
  const accountOverviewQuery = useQuery({
    queryKey: ['asset-detail-account-overview'],
    queryFn: accountApi.overview,
    enabled: isLoggedIn
  });
  const investmentOverviewQuery = useQuery({
    queryKey: ['asset-detail-investment-overview'],
    queryFn: investmentApi.overview,
    enabled: isLoggedIn
  });

  const snapshot = snapshotQuery.data;
  const accountOverview = accountOverviewQuery.data;
  const investmentOverview = investmentOverviewQuery.data;
  const accounts = accountOverview?.accounts ?? [];
  const categories = accountOverview?.categories ?? [];
  const modules = investmentOverview?.moduleAssets ?? [];
  const loading = snapshotQuery.isLoading || accountOverviewQuery.isLoading || investmentOverviewQuery.isLoading;
  const hasError = snapshotQuery.isError || accountOverviewQuery.isError || investmentOverviewQuery.isError;

  if (!isHydrated) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator color={theme.primary} />
      </View>
    );
  }

  if (!isLoggedIn) {
    return <Redirect href="/login" />;
  }

  return (
    <SafeAreaView style={styles.page}>
      <GridBackdrop color={theme.border} />
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Pressable style={styles.iconButton} onPress={() => router.back()}>
            <ChevronLeft color={theme.foreground} size={21} />
          </Pressable>
          <View style={styles.headerCopy}>
            <Text style={styles.title}>资产详情</Text>
            <Text variant="muted">账户余额与投资资产汇总</Text>
          </View>
        </View>

        {hasError ? <ErrorCard message="资产详情加载失败，请稍后重试。" /> : null}
        {loading ? <ActivityIndicator color={theme.primary} /> : null}

        <Card>
          <CardContent style={styles.heroCard}>
            <Text variant="muted">总资产</Text>
            <Text style={styles.totalAmount} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>
              {formatMoney(snapshot?.latest?.totalAsset)}
            </Text>
            <View style={styles.metricGrid}>
              <Metric label="账户余额" value={formatMoney(accountOverview?.totalAsset)} />
              <Metric label="投资资产" value={formatMoney(investmentOverview?.totalInvestmentAsset)} />
              <Metric label="账户净资产" value={formatMoney(snapshot?.latest?.netAsset)} />
            </View>
            <Text variant="caption">总资产以资产快照为准；账户余额和投资资产分别来自账户、投资聚合接口。</Text>
          </CardContent>
        </Card>

        <Card>
          <CardContent style={styles.sectionContent}>
            <SectionTitle icon={WalletCards} title="账户余额" subtitle={`${accountOverview?.accountCount ?? accounts.length} 个账户`} />
            {categories.length > 0 ? (
              <View style={styles.categoryGrid}>
                {categories.map((item) => (
                  <CategoryStat key={item.name || item.type || 'category'} item={item} />
                ))}
              </View>
            ) : null}
            {accounts.map((account, index) => (
              <View key={String(account.id)}>
                <Pressable style={styles.assetRow} onPress={() => router.push(`/account/${account.id}`)}>
                  <View style={styles.rowIcon}>
                    <Text style={styles.rowIconText}>{(account.name || accountTypeLabel(account.type)).slice(0, 1)}</Text>
                  </View>
                  <View style={styles.rowCopy}>
                    <Text style={styles.rowTitle}>{account.name || '未命名账户'}</Text>
                    <Text variant="caption">{accountTypeLabel(account.type)} · {account.currency || 'CNY'}</Text>
                  </View>
                  <Text style={styles.rowAmount}>{formatMoney(account.balance)}</Text>
                </Pressable>
                {index < accounts.length - 1 ? <Separator /> : null}
              </View>
            ))}
            {accounts.length === 0 ? <Text variant="muted">暂无账户余额。</Text> : null}
          </CardContent>
        </Card>

        <Card>
          <CardContent style={styles.sectionContent}>
            <SectionTitle icon={LineChart} title="投资资产" subtitle={`${holdingCount(modules)} 个持仓`} />
            {moduleItems(modules).map((item, index) => (
              <View key={item.module || item.name || index}>
                <Pressable style={styles.assetRow} onPress={() => router.push('/investment')}>
                  <View style={styles.rowIcon}>
                    <Text style={styles.rowIconText}>{moduleBadge(item.module)}</Text>
                  </View>
                  <View style={styles.rowCopy}>
                    <Text style={styles.rowTitle}>{item.name || moduleLabel(item.module)}</Text>
                    <Text variant="caption">{item.holdingCount ?? 0} 个持仓 · 占比 {formatPercent(item.assetRatio)}</Text>
                  </View>
                  <View style={styles.investAmountBlock}>
                    <Text style={styles.rowAmount}>{formatMoney(item.assetAmount)}</Text>
                    <Text variant="caption">{item.primaryProfitLabel || '收益'} {formatSignedMoney(item.primaryProfitAmount)}</Text>
                  </View>
                </Pressable>
                {index < moduleItems(modules).length - 1 ? <Separator /> : null}
              </View>
            ))}
          </CardContent>
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}

function SectionTitle({ icon: Icon, title, subtitle }: { icon: typeof WalletCards; title: string; subtitle: string }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  return (
    <View style={styles.sectionHeader}>
      <View style={styles.sectionTitleRow}>
        <View style={styles.sectionIcon}>
          <Icon color={theme.foreground} size={19} />
        </View>
        <Text style={styles.sectionTitle}>{title}</Text>
      </View>
      <Text variant="muted">{subtitle}</Text>
    </View>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <View style={stylesStatic.metric}>
      <Text variant="muted">{label}</Text>
      <Text style={stylesStatic.metricValue} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>{value}</Text>
    </View>
  );
}

function CategoryStat({ item }: { item: AccountCategorySummary }) {
  return (
    <View style={stylesStatic.categoryStat}>
      <Text variant="caption">{item.name || accountTypeLabel(item.type)}</Text>
      <Text style={stylesStatic.categoryValue}>{formatMoney(item.amount)}</Text>
      <Text variant="caption">{formatPercent(item.ratio)} · {item.count ?? 0} 个</Text>
    </View>
  );
}

function ErrorCard({ message }: { message: string }) {
  return (
    <Card>
      <CardContent>
        <Text variant="error">{message}</Text>
      </CardContent>
    </Card>
  );
}

function GridBackdrop({ color }: { color: string }) {
  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill}>
      {Array.from({ length: 18 }).map((_, index) => (
        <View key={`v-${index}`} style={[stylesStatic.gridLineVertical, { left: index * 28, backgroundColor: color }]} />
      ))}
      {Array.from({ length: 36 }).map((_, index) => (
        <View key={`h-${index}`} style={[stylesStatic.gridLineHorizontal, { top: index * 28, backgroundColor: color }]} />
      ))}
    </View>
  );
}

function accountTypeLabel(type?: string | null) {
  const labels: Record<string, string> = {
    CASH: '现金',
    BANK_CARD: '银行卡',
    CREDIT_CARD: '信用卡',
    ALIPAY: '支付宝',
    WECHAT: '微信',
    OTHER: '其他'
  };
  return type ? labels[type] || type : '账户';
}

function moduleItems(items: InvestmentModuleAsset[]) {
  const map = new Map(items.map((item) => [item.module, item]));
  return ['FUND', 'STOCK', 'CRYPTO'].map((module) => map.get(module) ?? { module, name: moduleLabel(module), holdingCount: 0 });
}

function moduleLabel(module?: string | null) {
  if (module === 'FUND') return '基金';
  if (module === 'STOCK') return '股票';
  if (module === 'CRYPTO') return '虚拟货币';
  return '投资资产';
}

function moduleBadge(module?: string | null) {
  if (module === 'FUND') return '基';
  if (module === 'STOCK') return '股';
  if (module === 'CRYPTO') return '币';
  return '投';
}

function holdingCount(items: InvestmentModuleAsset[]) {
  return items.reduce((sum, item) => sum + (item.holdingCount ?? 0), 0);
}

const stylesStatic = StyleSheet.create({
  gridLineVertical: {
    bottom: 0,
    opacity: 0.14,
    position: 'absolute',
    top: 0,
    width: 1
  },
  gridLineHorizontal: {
    height: 1,
    left: 0,
    opacity: 0.14,
    position: 'absolute',
    right: 0
  },
  metric: {
    flex: 1,
    gap: 5,
    minWidth: 0
  },
  metricValue: {
    fontSize: 16,
    fontWeight: '900'
  },
  categoryStat: {
    borderRadius: 12,
    gap: 5,
    minWidth: '30%'
  },
  categoryValue: {
    fontSize: 15,
    fontWeight: '900'
  }
});

const createStyles = (theme: ReturnType<typeof useTheme>) =>
  StyleSheet.create({
    page: {
      backgroundColor: theme.background,
      flex: 1
    },
    loading: {
      alignItems: 'center',
      backgroundColor: theme.background,
      flex: 1,
      justifyContent: 'center'
    },
    content: {
      gap: 14,
      padding: 18,
      paddingBottom: 112
    },
    header: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12
    },
    iconButton: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      height: 36,
      justifyContent: 'center',
      width: 36
    },
    headerCopy: {
      flex: 1,
      gap: 3,
      minWidth: 0
    },
    title: {
      fontSize: 26,
      fontWeight: '900'
    },
    heroCard: {
      gap: 14
    },
    totalAmount: {
      fontSize: 36,
      fontWeight: '900'
    },
    metricGrid: {
      flexDirection: 'row',
      gap: 12
    },
    sectionContent: {
      gap: 14
    },
    sectionHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    sectionTitleRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 8
    },
    sectionIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 15,
      height: 30,
      justifyContent: 'center',
      width: 30
    },
    sectionTitle: {
      fontSize: 18,
      fontWeight: '900'
    },
    categoryGrid: {
      flexDirection: 'row',
      gap: 12,
      justifyContent: 'space-between'
    },
    assetRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12,
      paddingVertical: 12
    },
    rowIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      height: 36,
      justifyContent: 'center',
      width: 36
    },
    rowIconText: {
      fontSize: 15,
      fontWeight: '900'
    },
    rowCopy: {
      flex: 1,
      gap: 3,
      minWidth: 0
    },
    rowTitle: {
      fontSize: 16,
      fontWeight: '800'
    },
    rowAmount: {
      fontSize: 15,
      fontWeight: '900',
      textAlign: 'right'
    },
    investAmountBlock: {
      alignItems: 'flex-end',
      maxWidth: 138
    }
  });
