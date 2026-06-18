import { Redirect, router } from 'expo-router';
import { useEffect } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, View } from 'react-native';

import { useHomeOverview, formatMoney, formatPercent, formatSignedMoney } from '@/features/home';
import { Badge, Button, Card, CardContent, Separator, Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';
import { useAuthStore } from '@/stores/authStore';

export function HomeScreen() {
  const theme = useTheme();
  const { userInfo, logout, isHydrated, isLoggedIn, restoreToken } = useAuthStore();

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  const { overviewQuery, snapshotQuery, transactionsQuery } = useHomeOverview(isLoggedIn);

  const overview = overviewQuery.data;
  const snapshot = snapshotQuery.data;
  const recentTransactions = transactionsQuery.data?.records ?? transactionsQuery.data?.list ?? [];
  const totalAssets = snapshot?.latest?.totalAsset ?? overview?.totalAssets;
  const netAssets = snapshot?.latest?.netAsset ?? overview?.netAssets;
  const investmentAsset = snapshot?.latest?.investmentAsset ?? overview?.investmentMarketValue;
  const monthlyIncome = snapshot?.latest?.monthlyIncome ?? overview?.monthlyIncome;
  const budgetUsageRate = snapshot?.latest?.budgetUsageRate ?? overview?.budgetUsageRate;
  const todayChange = snapshot?.netAssetChangeFromYesterday ?? null;

  async function handleLogout() {
    await logout();
    router.replace('/login');
  }

  if (!isHydrated) {
    return (
      <View style={[styles.loading, { backgroundColor: theme.background }]}>
        <ActivityIndicator color={theme.primary} />
      </View>
    );
  }

  if (!isLoggedIn) {
    return <Redirect href="/login" />;
  }

  return (
    <ScrollView style={[styles.page, { backgroundColor: theme.background }]} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <View style={styles.headerText}>
          <Text variant="caption">欢迎回来</Text>
          <Text variant="title">{userInfo?.nickname || userInfo?.username || 'XOAssets'}</Text>
        </View>
        <Button variant="ghost" size="sm" onPress={handleLogout}>
          退出
        </Button>
      </View>

      <Card style={styles.heroCard}>
        <CardContent style={styles.heroContent}>
          <View style={styles.rowBetween}>
            <Text variant="muted">总资产</Text>
            <Badge variant="outline">Overview</Badge>
          </View>
          <Text variant="title" style={styles.assetAmount}>
            {formatMoney(totalAssets)}
          </Text>
          <View style={styles.rowBetween}>
            <Text variant="muted">较昨日变化</Text>
            <Text
              variant="subtitle"
              style={{
                color: todayChange && todayChange < 0 ? theme.destructive : theme.success
              }}
            >
              {formatSignedMoney(todayChange)}
            </Text>
          </View>
        </CardContent>
      </Card>

      <View style={styles.grid}>
        <MetricCard label="净资产" value={formatMoney(netAssets)} />
        <MetricCard label="投资市值" value={formatMoney(investmentAsset)} />
        <MetricCard label="当月收入" value={formatMoney(monthlyIncome)} />
        <MetricCard label="预算使用率" value={formatPercent(budgetUsageRate)} />
      </View>

      <Card>
        <CardContent style={styles.section}>
          <View style={styles.rowBetween}>
            <Text variant="subtitle">资产分类</Text>
            <Text variant="caption">后端口径</Text>
          </View>
          <Separator />
          <CategoryRow label="现金与净资产" value={formatMoney(netAssets)} />
          <CategoryRow label="投资资产" value={formatMoney(investmentAsset)} />
        </CardContent>
      </Card>

      <Card>
        <CardContent style={styles.section}>
          <View style={styles.rowBetween}>
            <Text variant="subtitle">最近记录</Text>
            <Text variant="caption">最新 3 条</Text>
          </View>
          <Separator />
          {recentTransactions.length > 0 ? (
            recentTransactions.map((item) => (
              <CategoryRow
                key={item.id}
                label={item.categoryName || item.remark || item.type || '未命名记录'}
                value={formatMoney(item.amount)}
              />
            ))
          ) : (
            <Text variant="muted">暂无最近记录</Text>
          )}
        </CardContent>
      </Card>
    </ScrollView>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <Card style={styles.metricCard}>
      <CardContent style={styles.metricContent}>
        <Text variant="caption">{label}</Text>
        <Text variant="subtitle">{value}</Text>
      </CardContent>
    </Card>
  );
}

function CategoryRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.rowBetween}>
      <Text variant="body">{label}</Text>
      <Text variant="subtitle">{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  page: {
    flex: 1
  },
  loading: {
    alignItems: 'center',
    flex: 1,
    justifyContent: 'center'
  },
  content: {
    gap: 16,
    padding: 20,
    paddingTop: 64
  },
  header: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between'
  },
  headerText: {
    gap: 4
  },
  heroCard: {
    overflow: 'hidden'
  },
  heroContent: {
    gap: 18
  },
  assetAmount: {
    fontSize: 34
  },
  rowBetween: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between'
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12
  },
  metricCard: {
    flexBasis: '48%',
    flexGrow: 1
  },
  metricContent: {
    gap: 8
  },
  section: {
    gap: 14
  }
});
