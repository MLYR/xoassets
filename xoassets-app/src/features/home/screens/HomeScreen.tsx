import { Redirect, router } from 'expo-router';
import {
  BriefcaseBusiness,
  ChevronRight,
  CreditCard,
  Eye,
  EyeOff,
  LineChart,
  Settings,
  Sparkles,
  Utensils,
  WalletCards
} from 'lucide-react-native';
import { useEffect, useMemo, useRef, useState, type ComponentType } from 'react';
import { ActivityIndicator, Animated, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useHomeOverview, formatMoney, formatPercent, formatSignedMoney } from '@/features/home';
import { Card, CardContent, Separator, Text } from '@/components/ui';
import { AuthLogo } from '@/features/auth/components/AuthLogo';
import { useTheme } from '@/core/design/theme';
import { useAuthStore } from '@/stores/authStore';
import type { InvestmentModuleAsset, RecentTransaction } from '@/shared/types/asset';

type XoIcon = ComponentType<{ color?: string; size?: number; strokeWidth?: number; fill?: string }>;

export function HomeScreen() {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const { userInfo, isHydrated, isLoggedIn, restoreToken } = useAuthStore();
  const [amountVisible, setAmountVisible] = useState(true);

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  const { overviewQuery, snapshotQuery, investmentOverviewQuery, transactionsQuery, budgetSummaryQuery, reportsQuery } = useHomeOverview(isLoggedIn);

  const overview = overviewQuery.data;
  const snapshot = snapshotQuery.data;
  const investmentOverview = investmentOverviewQuery.data;
  const budgetSummary = budgetSummaryQuery.data;
  const reports = reportsQuery.data ?? [];
  const recentTransactions = (transactionsQuery.data?.records ?? transactionsQuery.data?.list ?? []).slice(0, 5);
  const isInitialLoading = overviewQuery.isLoading || snapshotQuery.isLoading || investmentOverviewQuery.isLoading || transactionsQuery.isLoading;
  const hasHomeError = overviewQuery.isError || snapshotQuery.isError || transactionsQuery.isError || investmentOverviewQuery.isError;
  const totalAssets = snapshot?.latest?.totalAsset ?? overview?.totalAssets;
  const netAssets = snapshot?.latest?.netAsset ?? overview?.netAssets;
  const investmentAsset = snapshot?.latest?.investmentAsset ?? overview?.investmentMarketValue;
  const monthlyIncome = snapshot?.latest?.monthlyIncome ?? overview?.monthlyIncome;
  const monthlyExpense = overview?.monthlyExpense;
  const monthlyChange = snapshot?.netAssetChangeFromMonthStart ?? null;
  const budgetUsageRate = budgetSummary?.usageRate ?? snapshot?.latest?.budgetUsageRate ?? overview?.budgetUsageRate;
  const aiReport = reports[0];
  const investmentModules = investmentOverview?.moduleAssets ?? [];

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
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <View style={styles.brandBlock}>
            <View style={styles.logoMark}>
              <AuthLogo size={56} />
            </View>
            <View style={styles.headerCopy}>
              <Text style={styles.brandTitle}>小〇财迹</Text>
              <Text style={styles.greeting}>{getGreeting()}，{userInfo?.nickname || userInfo?.username || 'XOer'}</Text>
              <Text variant="muted" style={styles.headerSub}>今天也把资产看清楚</Text>
            </View>
          </View>
          <PressableAnimated style={styles.bellButton} onPress={() => router.push('/settings')}>
            <Settings color={theme.foreground} size={24} strokeWidth={2.2} />
          </PressableAnimated>
        </View>

        {hasHomeError ? <ErrorCard message="首页数据加载失败，请稍后下拉刷新或重新进入。" /> : null}
        {isInitialLoading ? <LoadingSkeleton /> : null}

        <Card style={styles.heroCard}>
          <CardContent style={styles.heroContent}>
            <View style={styles.rowBetween}>
              <PressableAnimated style={styles.inlineAction} onPress={() => setAmountVisible((visible) => !visible)}>
                <Text style={styles.sectionTitle}>总资产</Text>
                {amountVisible ? (
                  <Eye color={theme.mutedForeground} size={18} strokeWidth={2.2} />
                ) : (
                  <EyeOff color={theme.mutedForeground} size={18} strokeWidth={2.2} />
                )}
              </PressableAnimated>
              <PressableAnimated style={styles.linkButton} onPress={() => router.push('/account')}>
                <Text style={styles.linkText}>资产详情</Text>
                <ChevronRight color={theme.mutedForeground} size={18} strokeWidth={2} />
              </PressableAnimated>
            </View>
            <Text style={styles.assetAmount} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>{maskMoney(formatMoney(totalAssets), amountVisible)}</Text>
            <View style={styles.heroSummary}>
              <MiniStat label="净资产" value={maskMoney(formatMoney(netAssets), amountVisible)} />
              <View style={styles.verticalDivider} />
              <MiniStat
                label="本月变化"
                value={maskMoney(formatSignedMoney(monthlyChange), amountVisible)}
                subValue={formatPercent(overview?.assetTrendRate)}
              />
            </View>
          </CardContent>
        </Card>

        <View style={styles.metricGrid}>
          <MetricCard icon={WalletCards} label="净资产" value={maskMoney(formatMoney(netAssets), amountVisible)} trend={`较上月 ${formatPercent(overview?.balanceTrendRate)}`} />
          <MetricCard icon={LineChart} label="投资市值" value={maskMoney(formatMoney(investmentAsset), amountVisible)} trend={`较上月 ${formatPercent(overview?.assetTrendRate)}`} />
          <MetricCard icon={BriefcaseBusiness} label="本月收入" value={maskMoney(formatMoney(monthlyIncome), amountVisible)} trend={`较上月 ${formatPercent(overview?.incomeTrendRate)}`} />
          <MetricCard icon={CreditCard} label="本月支出" value={maskMoney(formatMoney(monthlyExpense), amountVisible)} trend={`较上月 ${formatPercent(overview?.expenseTrendRate)}`} />
        </View>

        <Card style={styles.sectionCard}>
          <CardContent style={styles.sectionContent}>
            <View style={styles.rowBetween}>
              <Text style={styles.sectionTitle}>投资概览</Text>
              <PressableAnimated style={styles.linkButton} onPress={() => router.push('/investment')}>
                <Text style={styles.linkText}>全部</Text>
                <ChevronRight color={theme.mutedForeground} size={18} strokeWidth={2} />
              </PressableAnimated>
            </View>
            <View style={styles.investmentRow}>
              {investmentModuleItems(investmentModules).map((item, index) => (
                <View key={item.module || item.name || index} style={styles.investmentItemWrap}>
                  <InvestmentStat item={item} amountVisible={amountVisible} />
                  {index < 2 ? <InvestmentDivider /> : null}
                </View>
              ))}
            </View>
          </CardContent>
        </Card>

        <Card style={styles.sectionCard}>
          <CardContent style={styles.sectionContent}>
            <View style={styles.rowBetween}>
              <Text style={styles.sectionTitle}>本月预算</Text>
              <PressableAnimated style={styles.statusPill} onPress={() => router.push('/budget')}>
                <Sparkles color={theme.foreground} size={13} fill={theme.foreground} strokeWidth={2} />
                <Text style={styles.statusText}>{budgetSummary?.usageStatusLabel || '预算健康'}</Text>
                <ChevronRight color={theme.mutedForeground} size={16} strokeWidth={2} />
              </PressableAnimated>
            </View>
            <ProgressBar value={budgetUsageRate} />
            <View style={styles.budgetRow}>
              <MiniStat label="已用" value={maskMoney(formatMoney(budgetSummary?.totalUsed), amountVisible)} compact />
              <MiniStat label="占比" value={formatPercent(budgetUsageRate)} compact align="center" />
              <MiniStat label="剩余" value={maskMoney(formatMoney(budgetSummary?.totalRemaining), amountVisible)} compact align="right" />
            </View>
            <Text variant="caption">预算总额 {maskMoney(formatMoney(budgetSummary?.totalBudget), amountVisible)}</Text>
          </CardContent>
        </Card>

        <Card style={styles.sectionCard}>
          <CardContent style={styles.sectionContent}>
            <View style={styles.rowBetween}>
              <Text style={styles.sectionTitle}>AI 今日总结</Text>
              <Sparkles color={theme.foreground} size={18} strokeWidth={2.2} />
            </View>
            <Text variant="muted" style={styles.aiText}>{aiReport?.title || aiReport?.content || '今天的财务复盘还没有生成，可以先看资产与流水变化。'}</Text>
          </CardContent>
        </Card>

        <Card style={styles.sectionCard}>
          <CardContent style={styles.sectionContent}>
            <View style={styles.rowBetween}>
              <Text style={styles.sectionTitle}>最近记录</Text>
              <PressableAnimated style={styles.linkButton} onPress={() => router.push('/ledger')}>
                <Text style={styles.linkText}>查看全部</Text>
                <ChevronRight color={theme.mutedForeground} size={18} strokeWidth={2} />
              </PressableAnimated>
            </View>
            {recentTransactions.length > 0 ? (
              recentTransactions.map((item, index) => (
                <View key={item.id}>
                  <TransactionRow item={item} amountVisible={amountVisible} onPress={() => router.push('/ledger')} />
                  {index < recentTransactions.length - 1 ? <Separator /> : null}
                </View>
              ))
            ) : (
              <Text variant="muted">暂无最近记录</Text>
            )}
          </CardContent>
        </Card>
      </ScrollView>
    </SafeAreaView>
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

function PressableAnimated({ children, style, onPress }: { children: React.ReactNode; style?: object; onPress?: () => void }) {
  const scale = useRef(new Animated.Value(1)).current;

  return (
    <Pressable
      onPress={onPress}
      onPressIn={() => Animated.spring(scale, { toValue: 0.96, useNativeDriver: true }).start()}
      onPressOut={() => Animated.spring(scale, { toValue: 1, friction: 5, tension: 180, useNativeDriver: true }).start()}
    >
      <Animated.View style={[style, { transform: [{ scale }] }]}>{children}</Animated.View>
    </Pressable>
  );
}

function MiniStat({ label, value, subValue, compact, align = 'left' }: { label: string; value: string; subValue?: string; compact?: boolean; align?: 'left' | 'center' | 'right' }) {
  return (
    <View style={[stylesStatic.miniStat, align === 'center' && stylesStatic.centerText, align === 'right' && stylesStatic.rightText]}>
      <Text variant="muted" style={compact ? stylesStatic.compactLabel : undefined}>{label}</Text>
      <Text style={compact ? stylesStatic.compactValue : stylesStatic.miniValue} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.76}>{value} {subValue ? <Text variant="muted">{subValue}</Text> : null}</Text>
    </View>
  );
}

function MetricCard({ icon: Icon, label, value, trend }: { icon: XoIcon; label: string; value: string; trend: string }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Card style={styles.metricCard}>
      <CardContent style={styles.metricContent}>
        <View style={styles.iconBubble}>
          <Icon color={theme.foreground} size={21} strokeWidth={2.3} />
        </View>
        <View style={styles.metricTextBlock}>
          <Text variant="muted" style={styles.metricLabel}>{label}</Text>
          <Text style={styles.metricValue} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>{value}</Text>
          <Text variant="caption">{trend}</Text>
        </View>
      </CardContent>
    </Card>
  );
}

function ProgressBar({ value }: { value?: number | null }) {
  const theme = useTheme();
  const percent = typeof value === 'number' && Number.isFinite(value) ? Math.max(0, Math.min(value, 100)) : 0;

  return (
    <View style={[stylesStatic.progressTrack, { backgroundColor: theme.secondary }]}>
      <View style={[stylesStatic.progressFill, { width: `${percent}%`, backgroundColor: theme.foreground }]} />
    </View>
  );
}

function InvestmentStat({ item, amountVisible }: { item: InvestmentModuleAsset; amountVisible: boolean }) {
  return (
    <View style={stylesStatic.investmentStat}>
      <View style={stylesStatic.investmentTitleRow}>
        <Text variant="muted" numberOfLines={1}>{item.name || moduleLabel(item.module)}</Text>
        <Text variant="caption">昨/今收益</Text>
      </View>
      <Text style={stylesStatic.investmentValue} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>{maskMoney(formatSignedMoney(item.yesterdayProfit), amountVisible)}</Text>
      <Text style={stylesStatic.investmentValue} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>{maskMoney(formatSignedMoney(item.primaryProfitAmount), amountVisible)}</Text>
    </View>
  );
}

function InvestmentDivider() {
  const theme = useTheme();
  return <View style={[stylesStatic.investmentDivider, { backgroundColor: theme.border }]} />;
}


function investmentModuleItems(items: InvestmentModuleAsset[]) {
  const map = new Map(items.map((item) => [item.module, item]));
  return ['FUND', 'STOCK', 'CRYPTO'].map((module) => map.get(module) ?? { module, name: moduleLabel(module), primaryProfitAvailable: false });
}

function moduleLabel(module?: string | null) {
  if (module === 'FUND') return '基金';
  if (module === 'STOCK') return '股票';
  if (module === 'CRYPTO') return '加密资产';
  return '投资';
}

function TransactionRow({ item, amountVisible, onPress }: { item: RecentTransaction; amountVisible: boolean; onPress?: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const Icon = pickTransactionIcon(item);

  return (
    <Pressable style={styles.transactionRow} onPress={onPress}>
      <View style={styles.transactionLeft}>
        <View style={styles.transactionIcon}>
          <Icon color={theme.foreground} size={22} strokeWidth={2.2} />
        </View>
        <View style={styles.transactionTitleBlock}>
          <Text style={styles.transactionTitle}>{item.note || item.remark || item.categoryName || item.type || '未命名记录'}</Text>
          <Text variant="muted">{transactionTypeLabel(item.type)}</Text>
        </View>
      </View>
      <View style={styles.transactionCenter}>
        <Text variant="muted">{formatTransactionTime(item.transactionTime)}</Text>
      </View>
      <View style={styles.transactionAmountBlock}>
        <Text style={styles.transactionAmount} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>{maskMoney(formatTransactionAmount(item), amountVisible)}</Text>
        <Text variant="muted" style={styles.transactionAccount}>{item.accountName || item.targetAccountName || '--'}</Text>
      </View>
    </Pressable>
  );
}

function ErrorCard({ message }: { message: string }) {
  return (
    <Card style={stylesStatic.errorCard}>
      <CardContent>
        <Text variant="error">{message}</Text>
      </CardContent>
    </Card>
  );
}

function LoadingSkeleton() {
  const theme = useTheme();

  return (
    <Card style={stylesStatic.skeletonCard}>
      <CardContent style={stylesStatic.skeletonContent}>
        <View style={[stylesStatic.skeletonLine, { backgroundColor: theme.secondary, width: '36%' }]} />
        <View style={[stylesStatic.skeletonLine, { backgroundColor: theme.secondary, width: '68%', height: 34 }]} />
        <View style={[stylesStatic.skeletonLine, { backgroundColor: theme.secondary, width: '100%' }]} />
      </CardContent>
    </Card>
  );
}

function maskMoney(value: string, visible: boolean) {
  return visible ? value : value === '--' ? '--' : '••••••';
}

function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 6) return '夜深了';
  if (hour < 12) return '上午好';
  if (hour < 18) return '下午好';
  return '晚上好';
}

function transactionTypeLabel(type?: string | null) {
  const typeMap: Record<string, string> = {
    INCOME: '收入',
    EXPENSE: '消费',
    TRANSFER: '转账',
    REFUND: '退款',
    INVESTMENT: '投资'
  };

  return type ? typeMap[type] || type : '--';
}

function formatTransactionAmount(item: RecentTransaction) {
  if (item.amount === null || item.amount === undefined || Number.isNaN(item.amount)) {
    return '--';
  }

  const prefix = item.type === 'INCOME' || item.type === 'REFUND' ? '+' : item.type === 'TRANSFER' ? '' : '-';
  return `${prefix} ${formatMoney(item.amount)}`;
}

function formatTransactionTime(value?: string | null) {
  if (!value) {
    return '--';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  const dayText = isToday ? '今天' : date.toDateString() === yesterday.toDateString() ? '昨天' : `${date.getMonth() + 1}/${date.getDate()}`;
  const time = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false });
  return `${dayText} ${time}`;
}

function pickTransactionIcon(item: RecentTransaction): XoIcon {
  const text = `${item.categoryName || ''}${item.note || ''}${item.remark || ''}`;
  if (text.includes('餐') || text.includes('饭') || text.includes('食')) return Utensils;
  if (item.type === 'INCOME') return BriefcaseBusiness;
  if (item.type === 'TRANSFER') return WalletCards;
  if (item.type === 'INVESTMENT' || text.includes('基金') || text.includes('股票')) return LineChart;
  return CreditCard;
}

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
      paddingBottom: 24,
      paddingHorizontal: 16,
      paddingTop: 20
    },
    header: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between',
      marginBottom: 12
    },
    brandBlock: {
      alignItems: 'center',
      flex: 1,
      flexDirection: 'row',
      gap: 10
    },
    logoMark: {
      alignItems: 'center',
      backgroundColor: theme.foreground,
      borderRadius: 12,
      height: 56,
      justifyContent: 'center',
      shadowColor: theme.shadow,
      shadowOffset: { width: 0, height: 10 },
      shadowOpacity: 0.14,
      shadowRadius: 16,
      width: 56
    },
    headerCopy: {
      flexShrink: 1,
      gap: 3
    },
    brandTitle: {
      color: theme.foreground,
      fontSize: 26,
      fontWeight: '800',
      letterSpacing: -1.2,
      lineHeight: 30
    },
    greeting: {
      color: theme.foreground,
      fontSize: 16,
      fontWeight: '700'
    },
    headerSub: {
      fontSize: 13
    },
    bellButton: {
      alignItems: 'center',
      height: 46,
      justifyContent: 'center',
      position: 'relative',
      width: 46
    },
    bellDot: {
      backgroundColor: theme.foreground,
      borderRadius: 6,
      height: 10,
      position: 'absolute',
      right: 8,
      top: 8,
      width: 10
    },
    heroCard: {
      borderRadius: 18
    },
    heroContent: {
      gap: 12,
      padding: 14
    },
    rowBetween: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    inlineAction: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 10
    },
    sectionTitle: {
      color: theme.foreground,
      fontSize: 18,
      fontWeight: '800',
      letterSpacing: -0.4
    },
    linkButton: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 3
    },
    linkText: {
      color: theme.mutedForeground,
      fontSize: 14
    },
    assetAmount: {
      color: theme.foreground,
      fontSize: 34,
      fontWeight: '900',
      letterSpacing: -1.4,
      lineHeight: 42
    },
    heroSummary: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 14,
      flexDirection: 'row',
      gap: 10,
      paddingHorizontal: 12,
      paddingVertical: 12
    },
    verticalDivider: {
      backgroundColor: theme.border,
      height: 40,
      width: 1
    },
    metricGrid: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: 10
    },
    metricCard: {
      borderRadius: 15,
      flexBasis: '47%',
      flexGrow: 1
    },
    metricContent: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 8,
      padding: 12
    },
    iconBubble: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 22,
      height: 44,
      justifyContent: 'center',
      width: 44
    },
    metricTextBlock: {
      flex: 1,
      minWidth: 0,
      gap: 4
    },
    metricLabel: {
      fontSize: 13
    },
    metricValue: {
      color: theme.foreground,
      fontSize: 15,
      fontWeight: '800',
      letterSpacing: -0.4
    },
    sectionCard: {
      borderRadius: 16
    },
    sectionContent: {
      gap: 12,
      padding: 14
    },
    statusPill: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      flexDirection: 'row',
      gap: 5,
      paddingHorizontal: 8,
      paddingVertical: 6
    },
    statusText: {
      color: theme.foreground,
      fontSize: 12,
      fontWeight: '700'
    },
    budgetRow: {
      alignItems: 'flex-start',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    investmentRow: {
      alignItems: 'stretch',
      flexDirection: 'row'
    },
    investmentItemWrap: {
      flex: 1,
      flexDirection: 'row'
    },
    aiText: {
      lineHeight: 22
    },
    transactionRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 8,
      paddingVertical: 8
    },
    transactionLeft: {
      alignItems: 'center',
      flex: 1.2,
      flexDirection: 'row',
      gap: 8
    },
    transactionIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 22,
      height: 40,
      justifyContent: 'center',
      width: 40
    },
    transactionTitleBlock: {
      flex: 1,
      minWidth: 0,
      gap: 3
    },
    transactionTitle: {
      color: theme.foreground,
      fontSize: 15,
      fontWeight: '800'
    },
    transactionCenter: {
      flex: 0.72
    },
    transactionAmountBlock: {
      alignItems: 'flex-end',
      flex: 1,
      minWidth: 0
    },
    transactionAmount: {
      color: theme.foreground,
      fontSize: 15,
      fontWeight: '800'
    },
    transactionAccount: {
      marginTop: 3,
      textAlign: 'right'
    }
  });

const stylesStatic = StyleSheet.create({
  gridLineVertical: {
    bottom: 0,
    opacity: 0.45,
    position: 'absolute',
    top: 0,
    width: StyleSheet.hairlineWidth
  },
  gridLineHorizontal: {
    height: StyleSheet.hairlineWidth,
    left: 0,
    opacity: 0.45,
    position: 'absolute',
    right: 0
  },
  miniStat: {
    flex: 1,
    gap: 6
  },
  centerText: {
    alignItems: 'center'
  },
  rightText: {
    alignItems: 'flex-end'
  },
  miniValue: {
    fontSize: 15,
    fontWeight: '800',
    letterSpacing: -0.4
  },
  compactLabel: {
    fontSize: 12
  },
  compactValue: {
    fontSize: 15,
    fontWeight: '700'
  },
  progressTrack: {
    borderRadius: 999,
    height: 9,
    overflow: 'hidden'
  },
  progressFill: {
    borderRadius: 999,
    height: '100%'
  },
  investmentStat: {
    flex: 1,
    gap: 6
  },
  investmentTitleRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 4,
    justifyContent: 'space-between'
  },
  investmentValue: {
    fontSize: 15,
    fontWeight: '800'
  },
  investmentDivider: {
    marginHorizontal: 8,
    width: 1
  },
  errorCard: {
    borderRadius: 16
  },
  skeletonCard: {
    borderRadius: 18
  },
  skeletonContent: {
    gap: 14
  },
  skeletonLine: {
    borderRadius: 999,
    height: 18
  }
});
