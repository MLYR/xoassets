import { Redirect, useLocalSearchParams } from 'expo-router';
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Eye,
  EyeOff,
  LineChart,
  MoreHorizontal,
  RefreshCw,
  Search,
  WalletCards,
  X
} from 'lucide-react-native';
import { useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, Alert, Animated, Easing, KeyboardAvoidingView, Modal, PanResponder, Platform, Pressable, ScrollView, StyleSheet, View, useWindowDimensions } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AnimatedReanimated, { Easing as ReanimatedEasing, runOnJS, useAnimatedStyle, useSharedValue, withTiming } from 'react-native-reanimated';
import Svg, { Circle, Line, Polyline, Rect, Text as SvgText } from 'react-native-svg';

import { Button, Card, CardContent, Input, Separator, Text } from '@/components/ui';
import { formatMoney, formatSignedMoney } from '@/features/home';
import { useTheme } from '@/core/design/theme';
import { useAuthStore } from '@/stores/authStore';
import { investmentApi } from '../api/investmentApi';
import type {
  AssetLookupItem,
  AssetType,
  HoldingItem,
  InvestmentCalendarDayProfit,
  InvestmentModule,
  InvestmentModuleAsset,
  InvestmentOverview,
  InvestmentPeriod,
  InvestmentTransactionItem,
  InvestmentTransactionRequest,
  LedgerAccount
} from '../api/investmentTypes';
import { useInvestment } from '../hooks/useInvestment';

const moduleTabs: Array<{ label: string; value: InvestmentModule }> = [
  { label: '总资产', value: 'ALL' },
  { label: '基金', value: 'FUND' },
  { label: '股票', value: 'STOCK' },
  { label: '虚拟货币', value: 'CRYPTO' }
];

const periods: Array<{ label: string; value: InvestmentPeriod }> = [
  { label: '近7天', value: 'WEEK' },
  { label: '近30天', value: 'MONTH' },
  { label: '近90天', value: 'QUARTER' },
  { label: '近1年', value: 'YEAR' },
  { label: '全部', value: 'ALL' }
];

const weekdays = ['日', '一', '二', '三', '四', '五', '六'];

type ChartMode = 'asset' | 'profit' | 'calendar';
type HoldingSortMode = 'MARKET_DESC' | 'MARKET_ASC' | 'PROFIT_DESC' | 'PROFIT_ASC';

interface TransactionFormState {
  source: 'HOLDING' | 'LOOKUP';
  type: 'BUY' | 'SELL';
  assetType: AssetType;
  holdingId: string;
  assetId: string;
  accountId: string;
  keyword: string;
  market: string;
  inputMode: 'QUANTITY_PRICE' | 'AMOUNT_NAV';
  tradeAmount: string;
  quantity: string;
  price: string;
  fee: string;
  time: string;
  note: string;
}

export function InvestmentScreen() {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const params = useLocalSearchParams<{ compose?: string }>();
  const { isHydrated, isLoggedIn, restoreToken } = useAuthStore();
  const [module, setModule] = useState<InvestmentModule>('ALL');
  const [period, setPeriod] = useState<InvestmentPeriod>('MONTH');
  const [chartMode, setChartMode] = useState<ChartMode>('asset');
  const [calendarMonth, setCalendarMonth] = useState(() => monthKey(new Date()));
  const [amountVisible, setAmountVisible] = useState(true);
  const [selectedHolding, setSelectedHolding] = useState<HoldingItem | null>(null);
  const [selectedHoldingCalendarMonth, setSelectedHoldingCalendarMonth] = useState(() => monthKey(new Date()));
  const [selectedTransaction, setSelectedTransaction] = useState<InvestmentTransactionItem | null>(null);
  const [allTransactionsOpen, setAllTransactionsOpen] = useState(false);
  const [holdingSortOpen, setHoldingSortOpen] = useState(false);
  const [holdingSort, setHoldingSort] = useState<HoldingSortMode>('MARKET_DESC');
  const [refreshTip, setRefreshTip] = useState<string | null>(null);
  const refreshSpin = useRef(new Animated.Value(0)).current;
  const [composerOpen, setComposerOpen] = useState(false);
  const [form, setForm] = useState<TransactionFormState>(() => createEmptyForm());
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  useEffect(() => {
    if (params.compose) {
      setComposerOpen(true);
    }
  }, [params.compose]);

  const [calendarYear, calendarMonthNumber] = calendarMonth.split('-').map(Number);
  const {
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
  } = useInvestment(module, period, calendarYear, calendarMonthNumber, isLoggedIn);

  const overview = overviewQuery.data;
  const holdings = holdingsQuery.data ?? [];
  const transactions = transactionsQuery.data ?? [];
  const sortedHoldings = useMemo(() => sortHoldings(holdings, holdingSort), [holdings, holdingSort]);
  const accounts = accountsQuery.data ?? [];
  const selectedModuleAsset = module === 'ALL' ? null : overview?.moduleAssets?.find((item) => item.module === module) ?? null;
  const summary = buildSummary(module, overview, selectedModuleAsset, holdings);
  const trendPoints = trendQuery.data?.points ?? [];
  const calendarRows = calendarQuery.data ?? [];
  const isInitialLoading = overviewQuery.isLoading || holdingsQuery.isLoading || trendQuery.isLoading;
  const hasError = overviewQuery.isError || holdingsQuery.isError || trendQuery.isError || calendarQuery.isError || transactionsQuery.isError;
  const selectedHoldingTransactions = useMemo(
    () => transactions.filter((item) => String(item.holdingId ?? '') === String(selectedHolding?.id ?? '')),
    [selectedHolding?.id, transactions]
  );

  const refreshRotate = refreshSpin.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg']
  });


  useEffect(() => {
    if (selectedHolding) {
      setSelectedHoldingCalendarMonth(calendarMonth);
    }
  }, [calendarMonth, selectedHolding]);

  useEffect(() => {
    setForm((current) => ({
      ...current,
      accountId: current.accountId || String(accounts[0]?.id ?? ''),
      holdingId: current.holdingId || String(holdings[0]?.id ?? ''),
      assetId: current.assetId || String(holdings[0]?.assetId ?? ''),
      assetType: module === 'ALL' ? current.assetType : (module as AssetType)
    }));
  }, [accounts, holdings, module]);

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

  function changeCalendarMonth(offset: number) {
    const [year, month] = calendarMonth.split('-').map(Number);
    setCalendarMonth(monthKey(new Date(year, month - 1 + offset, 1)));
  }

  async function handleRefreshQuotes() {
    const assetIds = Array.from(new Set(holdings.map((item) => String(item.assetId ?? '')).filter(Boolean)));
    // 刷新结果改为卡片内轻提示，避免系统弹框打断用户查看投资页。
    refreshSpin.setValue(0);
    Animated.timing(refreshSpin, {
      toValue: 1,
      duration: 650,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: true
    }).start();
    if (!assetIds.length) {
      setRefreshTip('暂无可刷新的持仓');
      setTimeout(() => setRefreshTip(null), 1800);
      return;
    }
    try {
      await refreshQuotesMutation.mutateAsync(assetIds);
      setRefreshTip('行情刷新已提交');
    } catch (error) {
      setRefreshTip(error instanceof Error ? error.message : '刷新失败，请稍后重试');
    } finally {
      setTimeout(() => setRefreshTip(null), 2200);
    }
  }

  async function handleLookup() {
    const keyword = form.keyword.trim();
    if (!keyword) {
      setFormError('请输入资产名称或代码');
      return;
    }
    setFormError(null);
    try {
      const result = await lookupMutation.mutateAsync({ type: form.assetType, keyword, market: form.market.trim() || undefined });
      const first = result[0];
      if (!first) {
        setFormError('没有识别到资产，请换一个代码或名称');
        return;
      }
      setForm((current) => ({
        ...current,
        keyword: assetName(first),
        market: first.market || current.market,
        price: first.latestPrice === null || first.latestPrice === undefined ? current.price : String(first.latestPrice),
        assetId: '',
        holdingId: ''
      }));
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '资产识别失败');
    }
  }

  async function handleSubmitTransaction() {
    setFormError(null);
    try {
      const request = await buildTransactionRequest(form, holdings, accounts, lookupMutation.data ?? []);
      await createTransactionMutation.mutateAsync(request);
      setComposerOpen(false);
      setForm(createEmptyForm());
      Alert.alert('已保存', '投资交易已写入后端。');
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '保存失败');
    }
  }

  async function buildTransactionRequest(
    current: TransactionFormState,
    holdingList: HoldingItem[],
    accountList: LedgerAccount[],
    lookupItems: AssetLookupItem[]
  ): Promise<InvestmentTransactionRequest> {
    const accountId = current.accountId || String(accountList[0]?.id ?? '');
    if (!accountId) {
      throw new Error('请选择资金账户');
    }

    const holding = holdingList.find((item) => String(item.id) === current.holdingId);
    let assetId = current.source === 'HOLDING' ? String(holding?.assetId ?? '') : current.assetId;
    let holdingId = current.source === 'HOLDING' ? String(holding?.id ?? '') : null;

    // 新资产交易必须先通过后端资产接口创建公共资产，再提交投资交易，避免 App 私造资产 ID。
    if (current.source === 'LOOKUP') {
      const selectedLookup = lookupItems[0];
      if (!selectedLookup) {
        throw new Error('请先识别资产');
      }
      const symbol = selectedLookup.symbol || current.keyword.trim();
      const assetType = (selectedLookup.assetType || current.assetType) as AssetType;
      const existingAssets = await investmentApi.searchAssets(assetType, symbol);
      const existingAsset = existingAssets.find((item) => item.symbol === symbol && (!selectedLookup.market || item.market === selectedLookup.market)) ?? existingAssets[0];
      const createdAsset = existingAsset ?? await createAssetMutation.mutateAsync({
        symbol,
        name: selectedLookup.name || current.keyword.trim(),
        type: assetType,
        market: selectedLookup.market || current.market.trim() || 'UNKNOWN',
        currency: selectedLookup.currency || 'CNY',
        quoteSource: selectedLookup.quoteSource || 'MANUAL',
        quoteKey: selectedLookup.quoteKey || selectedLookup.symbol || current.keyword.trim()
      });
      assetId = String(createdAsset.id);
      holdingId = null;
    }

    if (!assetId) {
      throw new Error('请选择持仓或识别资产');
    }
    if (current.type === 'SELL' && !holdingId) {
      throw new Error('卖出必须选择已有持仓');
    }

    const isFundAmount = current.assetType === 'FUND' && current.type === 'BUY' && current.inputMode === 'AMOUNT_NAV';
    if (isFundAmount) {
      if (!positive(current.tradeAmount)) {
        throw new Error('请输入买入总金额');
      }
    } else {
      if (!positive(current.quantity) || !positive(current.price)) {
        throw new Error('请输入数量和价格');
      }
    }

    return {
      holdingId,
      assetId,
      accountId,
      type: current.type,
      inputMode: isFundAmount ? 'AMOUNT_NAV' : 'QUANTITY_PRICE',
      tradeAmount: isFundAmount ? current.tradeAmount : undefined,
      quantity: isFundAmount ? undefined : current.quantity,
      price: isFundAmount ? undefined : current.price,
      fee: current.fee || '0',
      transactionTime: toLocalDateTime(current.time),
      note: current.note.trim() || null
    };
  }

  return (
    <SafeAreaView style={styles.page}>
      <GridBackdrop color={theme.border} />
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <Text style={styles.screenTitle}>投资</Text>
          <View style={styles.headerSpacer} />
        </View>

        <View style={styles.moduleTabs}>
          {moduleTabs.map((item) => (
            <Pressable key={item.value} style={[styles.moduleTab, module === item.value ? styles.moduleTabActive : null]} onPress={() => setModule(item.value)}>
              <Text style={[styles.moduleTabText, module === item.value ? styles.moduleTabTextActive : null]}>{item.label}</Text>
            </Pressable>
          ))}
        </View>

        {hasError ? <ErrorCard message="投资数据加载失败，请检查后端服务或登录状态。" /> : null}

        <Card style={[styles.summaryCard, !amountVisible ? styles.summaryCardHidden : null]}>
          <CardContent style={styles.summaryContent}>
            <View style={styles.rowBetween}>
              <Pressable style={styles.inlineAction} onPress={() => setAmountVisible((value) => !value)}>
                <Text style={styles.cardTitle}>{summary.title}</Text>
                <View style={[styles.eyeStateButton, !amountVisible ? styles.eyeStateButtonHidden : null]}>
                  {amountVisible ? (
                    <Eye color={theme.mutedForeground} size={16} strokeWidth={2.2} />
                  ) : (
                    <EyeOff color={theme.background} size={16} strokeWidth={2.2} />
                  )}
                </View>
              </Pressable>
              {/* 总投资资产右侧只保留刷新入口图标，减少标题区文字拥挤。 */}
              <Pressable style={({ pressed }) => [styles.refreshButton, pressed ? styles.pressed : null]} disabled={refreshQuotesMutation.isPending} onPress={handleRefreshQuotes}>
                <Animated.View style={{ transform: [{ rotate: refreshRotate }] }}>
                  <RefreshCw color={theme.foreground} size={16} strokeWidth={2.2} />
                </Animated.View>
              </Pressable>
            </View>
            {/* 加载态放在总投资资产卡片内部，避免首屏额外插入卡片导致布局跳动。 */}
            {isInitialLoading ? (
              <>
                <View style={[styles.summaryAmountSkeleton, { backgroundColor: theme.secondary }]} />
                <View style={styles.summaryStats}>
                  {[0, 1, 2].map((item, index) => (
                    <View key={item} style={styles.summaryStatWrap}>
                      <View style={styles.summaryMetricSkeleton}>
                        <View style={[styles.summaryMetricLabelSkeleton, { backgroundColor: theme.secondary }]} />
                        <View style={[styles.summaryMetricValueSkeleton, { backgroundColor: theme.secondary }]} />
                      </View>
                      {index < 2 ? <View style={styles.verticalDivider} /> : null}
                    </View>
                  ))}
                </View>
              </>
            ) : (
              <>
                <Text style={styles.summaryAmount} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.68}>{maskAmount(formatMoney(summary.amount), amountVisible)}</Text>
                <View style={styles.summaryStats}>
                  {summary.stats.map((item, index) => (
                    <View key={item.label} style={styles.summaryStatWrap}>
                      <Metric
                        label={item.label}
                        value={maskAmount(item.value, amountVisible)}
                        secondaryValue={item.secondaryValue ? maskAmount(item.secondaryValue, amountVisible) : undefined}
                        positive={item.positive}
                        secondaryPositive={item.secondaryPositive}
                      />
                      {index < summary.stats.length - 1 ? <View style={styles.verticalDivider} /> : null}
                    </View>
                  ))}
                </View>
              </>
            )}
            {module === 'ALL' ? <AssetDistribution items={overview?.moduleAssets ?? []} amountVisible={amountVisible} /> : null}
          </CardContent>
        </Card>

        <Card style={styles.sectionCard}>
          <CardContent style={styles.sectionContent}>
            <ChartModeTabs active={chartMode} onChange={setChartMode} />
            {chartMode === 'calendar' ? (
              <CalendarProfitCard
                rows={calendarRows}
                month={calendarMonth}
                amountVisible={amountVisible}
                loading={calendarQuery.isLoading || calendarQuery.isFetching}
                onPrev={() => changeCalendarMonth(-1)}
                onNext={() => changeCalendarMonth(1)}
                embedded
              />
            ) : (
              <>
                <View style={styles.periodRow}>
                  {periods.map((item) => (
                    <Pressable key={item.value} style={[styles.periodPill, period === item.value ? styles.periodPillActive : null]} onPress={() => setPeriod(item.value)}>
                      <Text style={[styles.periodText, period === item.value ? styles.periodTextActive : null]}>{item.label}</Text>
                    </Pressable>
                  ))}
                </View>
                <InvestmentLineChart mode={chartMode} points={trendPoints} amountVisible={amountVisible} />
              </>
            )}
          </CardContent>
        </Card>
        <Card style={styles.sectionCard}>
          <CardContent style={styles.sectionContent}>
            <View style={styles.rowBetween}>
              <Text style={styles.sectionTitle}>持仓详情</Text>
              <Pressable style={({ pressed }) => [styles.sortButton, pressed ? styles.pressed : null]} onPress={() => setHoldingSortOpen(true)}>
                <Text style={styles.sortText}>{holdingSortLabel(holdingSort)}</Text>
              </Pressable>
            </View>
            <HoldingTable module={module} holdings={sortedHoldings} amountVisible={amountVisible} onSelect={setSelectedHolding} />
          </CardContent>
        </Card>

        <Card style={styles.sectionCard}>
          <CardContent style={styles.sectionContent}>
            <View style={styles.rowBetween}>
              <Text style={styles.sectionTitle}>交易记录</Text>
              <Pressable style={({ pressed }) => [styles.moreIconButton, pressed ? styles.pressed : null]} onPress={() => setAllTransactionsOpen(true)}>
                <MoreHorizontal color={theme.foreground} size={18} strokeWidth={2.4} />
              </Pressable>
            </View>
            <TransactionList transactions={transactions.slice(0, 6)} amountVisible={amountVisible} onSelect={setSelectedTransaction} />
          </CardContent>
        </Card>
      </ScrollView>
      <FloatingToast message={refreshTip} />

      <HoldingDetailModal
        holding={selectedHolding}
        transactions={selectedHoldingTransactions}
        amountVisible={amountVisible}
        month={selectedHoldingCalendarMonth}
        onMonthChange={setSelectedHoldingCalendarMonth}
        onTransactionSelect={setSelectedTransaction}
        onClose={() => setSelectedHolding(null)}
      />
      <HoldingSortModal
        visible={holdingSortOpen}
        value={holdingSort}
        onChange={(value) => {
          setHoldingSort(value);
          setHoldingSortOpen(false);
        }}
        onClose={() => setHoldingSortOpen(false)}
      />
      <TransactionsModal
        visible={allTransactionsOpen}
        transactions={transactions}
        amountVisible={amountVisible}
        onSelect={setSelectedTransaction}
        onClose={() => setAllTransactionsOpen(false)}
      />
      <TransactionDetailModal
        transaction={selectedTransaction}
        amountVisible={amountVisible}
        onClose={() => setSelectedTransaction(null)}
      />
      <TransactionComposer
        visible={composerOpen}
        form={form}
        formError={formError}
        holdings={holdings}
        accounts={accounts}
        lookupItems={lookupMutation.data ?? []}
        loading={createTransactionMutation.isPending || createAssetMutation.isPending}
        lookupLoading={lookupMutation.isPending}
        onClose={() => {
          setComposerOpen(false);
          setFormError(null);
        }}
        onLookup={handleLookup}
        onSubmit={handleSubmitTransaction}
        onChange={setForm}
      />
    </SafeAreaView>
  );
}

function GridBackdrop({ color }: { color: string }) {
  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill}>
      {Array.from({ length: 16 }).map((_, index) => (
        <View key={`v-${index}`} style={[stylesStatic.gridLineVertical, { left: index * 30, backgroundColor: color }]} />
      ))}
      {Array.from({ length: 34 }).map((_, index) => (
        <View key={`h-${index}`} style={[stylesStatic.gridLineHorizontal, { top: index * 30, backgroundColor: color }]} />
      ))}
    </View>
  );
}

function FloatingToast({ message }: { message: string | null }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  if (!message) return null;
  return (
    <View pointerEvents="none" style={styles.toastWrap}>
      <View style={styles.toastBox}>
        <Text style={styles.toastText}>{message}</Text>
      </View>
    </View>
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
        <View style={[stylesStatic.skeletonLine, { width: '42%', backgroundColor: theme.secondary }]} />
        <View style={[stylesStatic.skeletonLine, { width: '78%', height: 34, backgroundColor: theme.secondary }]} />
        <View style={[stylesStatic.skeletonLine, { width: '100%', backgroundColor: theme.secondary }]} />
      </CardContent>
    </Card>
  );
}

function ChartModeTabs({ active, onChange }: { active: ChartMode; onChange: (value: ChartMode) => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const tabs: Array<{ label: string; value: ChartMode }> = [
    { label: '总投资资产走势', value: 'asset' },
    { label: '收益走势', value: 'profit' },
    { label: '日历收益', value: 'calendar' }
  ];
  return (
    <View style={styles.chartTabs}>
      {tabs.map((item) => (
        <Pressable key={item.value} style={[styles.chartTab, active === item.value ? styles.chartTabActive : null]} onPress={() => onChange(item.value)}>
          <Text style={[styles.chartTabText, active === item.value ? styles.chartTabTextActive : null]} numberOfLines={1}>{item.label}</Text>
        </Pressable>
      ))}
    </View>
  );
}

function Metric({ label, value, secondaryValue, positive, secondaryPositive }: { label: string; value: string; secondaryValue?: string; positive?: boolean | null; secondaryPositive?: boolean | null }) {
  const theme = useTheme();
  const color = metricColor(value, positive, theme);
  const secondaryColor = metricColor(secondaryValue ?? '--', secondaryPositive, theme);
  return (
    <View style={stylesStatic.metricBlock}>
      <Text variant="muted" style={stylesStatic.metricLabel}>{label}</Text>
      <Text style={[stylesStatic.metricValue, { color }]} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.62}>{value}</Text>
      {secondaryValue ? <Text style={[stylesStatic.metricSecondaryValue, { color: secondaryColor }]} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.62}>{secondaryValue}</Text> : null}
    </View>
  );
}

function AssetDistribution({ items, amountVisible }: { items: InvestmentModuleAsset[]; amountVisible: boolean }) {
  const theme = useTheme();
  const totalRatio = items.reduce((sum, item) => sum + safeNumber(item.assetRatio), 0);
  const normalized = ['FUND', 'STOCK', 'CRYPTO'].map((moduleName) => items.find((item) => item.module === moduleName));
  return (
    <View style={stylesStatic.distributionBlock}>
      <Text style={stylesStatic.distributionTitle}>资产分布</Text>
      <View style={stylesStatic.distributionLegend}>
        {normalized.map((item, index) => (
          <View key={item?.module || index} style={stylesStatic.legendItem}>
            <View style={[stylesStatic.legendDot, { backgroundColor: distributionColor(index, theme.foreground, theme.mutedForeground, theme.border) }]} />
            <Text style={stylesStatic.legendText}>{moduleLabel((item?.module as InvestmentModule) || moduleTabs[index + 1]?.value)} {maskNumber(formatPercent2(item?.assetRatio), amountVisible)}</Text>
          </View>
        ))}
      </View>
      <View style={[stylesStatic.distributionTrack, { backgroundColor: theme.secondary }]}>
        {normalized.map((item, index) => {
          const width = (totalRatio > 0 ? `${Math.max(0, safeNumber(item?.assetRatio))}%` : '0%') as `${number}%`;
          return <View key={item?.module || index} style={[stylesStatic.distributionFill, { width, backgroundColor: distributionColor(index, theme.foreground, theme.mutedForeground, theme.border) }]} />;
        })}
      </View>
    </View>
  );
}

function InvestmentLineChart({ mode, points, amountVisible }: { mode: Exclude<ChartMode, 'calendar'>; points: Array<{ date: string; marketValue?: number | null; assetAmount?: number | null; totalProfit?: number | null; holdingProfit?: number | null; dailyProfit?: number | null; primaryProfitAmount?: number | null }>; amountVisible: boolean }) {
  const theme = useTheme();
  const [selectedIndex, setSelectedIndex] = useState(() => Math.max(points.length - 1, 0));
  const [isInspecting, setIsInspecting] = useState(false);
  const [layoutWidth, setLayoutWidth] = useState(330);
  const width = 330;
  const height = 180;
  const chartLeft = 48;
  const chartRight = 12;
  const chartTop = 14;
  const chartBottom = 34;
  const values = points.map((item) => chartPointValue(item, mode)).filter((value) => Number.isFinite(value));
  const min = values.length ? Math.min(...values) : 0;
  const max = values.length ? Math.max(...values) : 1;
  const range = max - min || 1;
  const coords = points.map((item, index) => {
    const value = chartPointValue(item, mode);
    const x = chartLeft + (index / Math.max(points.length - 1, 1)) * (width - chartLeft - chartRight);
    const y = chartTop + (1 - (value - min) / range) * (height - chartTop - chartBottom);
    return `${x},${y}`;
  });
  const activeIndex = Math.min(selectedIndex, Math.max(points.length - 1, 0));
  const selected = points[activeIndex] ?? points[points.length - 1];
  const selectedValue = selected ? chartPointValue(selected, mode) : null;
  const selectedCoord = coords[activeIndex];
  const panResponder = useMemo(() => PanResponder.create({
    onStartShouldSetPanResponder: () => true,
    onMoveShouldSetPanResponder: () => true,
    onPanResponderGrant: (event) => {
      setIsInspecting(true);
      updateSelectedPoint(event.nativeEvent.locationX, layoutWidth, points.length, setSelectedIndex);
    },
    onPanResponderMove: (event) => {
      setIsInspecting(true);
      updateSelectedPoint(event.nativeEvent.locationX, layoutWidth, points.length, setSelectedIndex);
    },
    onPanResponderRelease: () => setIsInspecting(false),
    onPanResponderTerminate: () => setIsInspecting(false)
  }), [layoutWidth, points.length]);
  const last = points[points.length - 1];
  const yTicks = [max, min + range / 2, min];
  const firstLabel = points[0]?.date?.slice(5) ?? '--';
  const middleLabel = points[Math.floor(points.length / 2)]?.date?.slice(5) ?? '--';
  const lastLabel = last?.date?.slice(5) ?? '--';

  if (!points.length) {
    return <EmptyState icon={LineChart} title="暂无趋势" description={mode === 'asset' ? '后端还没有返回总投资资产走势。' : '后端还没有返回收益走势。'} />;
  }

  return (
    <View
      style={stylesStatic.chartWrap}
      onLayout={(event) => setLayoutWidth(event.nativeEvent.layout.width || 330)}
      {...panResponder.panHandlers}
    >
      <Svg width="100%" height={height} viewBox={`0 0 ${width} ${height}`}>
        {[0, 1, 2].map((_, index) => {
          const y = chartTop + index * ((height - chartTop - chartBottom) / 2);
          return <Line key={index} x1={chartLeft} x2={width - chartRight} y1={y} y2={y} stroke={theme.border} strokeDasharray="4 6" strokeWidth="1" />;
        })}
        {yTicks.map((tick, index) => (
          <SvgText key={index} x="4" y={chartTop + index * ((height - chartTop - chartBottom) / 2) + 4} fontSize="11" fill={theme.mutedForeground}>
            {shortAmount(tick)}
          </SvgText>
        ))}
        <Polyline points={coords.join(' ')} fill="none" stroke={theme.foreground} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
        {coords.map((pair, index) => {
          const [x, y] = pair.split(',').map(Number);
          return <Circle key={index} cx={x} cy={y} r={2.7} fill={theme.foreground} />;
        })}
        <SvgText x={chartLeft} y={height - 10} fontSize="12" fill={theme.mutedForeground}>{firstLabel}</SvgText>
        <SvgText x={width / 2 - 16} y={height - 10} fontSize="12" fill={theme.mutedForeground}>{middleLabel}</SvgText>
        <SvgText x={width - 52} y={height - 10} fontSize="12" fill={theme.mutedForeground}>{lastLabel}</SvgText>
        {isInspecting && selectedCoord && selectedValue !== null ? (
          <>
            <Line x1={selectedCoord.split(',')[0]} x2={selectedCoord.split(',')[0]} y1={chartTop} y2={height - chartBottom} stroke={theme.mutedForeground} strokeDasharray="3 5" strokeWidth="1" />
            <Circle cx={Number(selectedCoord.split(',')[0])} cy={Number(selectedCoord.split(',')[1])} r={5} fill={theme.background} stroke={theme.foreground} strokeWidth="2" />
            <Rect x={Number(selectedCoord.split(',')[0]) > width - 110 ? width - 112 : Number(selectedCoord.split(',')[0]) + 8} y="8" width="104" height="42" rx="8" fill={theme.foreground} />
            <SvgText x={(Number(selectedCoord.split(',')[0]) > width - 110 ? width - 60 : Number(selectedCoord.split(',')[0]) + 60)} y="25" textAnchor="middle" fontSize="11" fontWeight="700" fill={theme.background}>
              {selected.date?.slice(5) ?? '--'}
            </SvgText>
            <SvgText x={(Number(selectedCoord.split(',')[0]) > width - 110 ? width - 60 : Number(selectedCoord.split(',')[0]) + 60)} y="42" textAnchor="middle" fontSize="11" fontWeight="700" fill={theme.background}>
              {amountVisible ? numberText(selectedValue) : '••••'}
            </SvgText>
          </>
        ) : null}
      </Svg>
    </View>
  );
}

function CalendarProfitCard({ rows, month, amountVisible, loading, onPrev, onNext, compact, embedded }: { rows: InvestmentCalendarDayProfit[]; month: string; amountVisible: boolean; loading?: boolean; onPrev: () => void; onNext: () => void; compact?: boolean; embedded?: boolean }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const cells = useMemo(() => buildCalendarCells(month, rows), [month, rows]);
  const [animatingMonth, setAnimatingMonth] = useState(false);
  const { width: screenWidth } = useWindowDimensions();
  const slideDistance = Math.max(280, screenWidth - 32);
  const slide = useSharedValue(0);
  const calendarAnimatedStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: slide.value }]
  }));

  function finishSwitchMonth(direction: -1 | 1) {
    direction < 0 ? onPrev() : onNext();
    slide.value = direction * slideDistance;
    slide.value = withTiming(0, { duration: 260, easing: ReanimatedEasing.out(ReanimatedEasing.cubic) }, () => runOnJS(setAnimatingMonth)(false));
  }

  function switchMonth(direction: -1 | 1) {
    // 轮播式切月：旧月份整屏滑出，新月份从反方向滑入；收益仍以后端月度数据为准。
    setAnimatingMonth(true);
    slide.value = withTiming(direction * -slideDistance, { duration: 220, easing: ReanimatedEasing.in(ReanimatedEasing.cubic) }, () => runOnJS(finishSwitchMonth)(direction));
  }

  const panResponder = useMemo(() => PanResponder.create({
    onStartShouldSetPanResponder: () => true,
    onStartShouldSetPanResponderCapture: () => true,
    onMoveShouldSetPanResponder: (_, gesture) => shouldHandleCalendarSwipe(gesture.dx, gesture.dy),
    onMoveShouldSetPanResponderCapture: (_, gesture) => shouldHandleCalendarSwipe(gesture.dx, gesture.dy),
    onPanResponderMove: (_, gesture) => {
      slide.value = Math.max(-slideDistance, Math.min(slideDistance, gesture.dx));
    },
    onPanResponderRelease: (_, gesture) => {
      if (gesture.dx > 46) {
        switchMonth(-1);
        return;
      }
      if (gesture.dx < -46) {
        switchMonth(1);
        return;
      }
      slide.value = withTiming(0, { duration: 160, easing: ReanimatedEasing.out(ReanimatedEasing.cubic) });
    },
    onPanResponderTerminate: () => {
      slide.value = withTiming(0, { duration: 160, easing: ReanimatedEasing.out(ReanimatedEasing.cubic) });
    }
  }), [onNext, onPrev, slideDistance]);

  const content = (
    <View style={styles.embeddedCalendar}>
      <View style={styles.rowBetween}>
        <Text style={styles.sectionTitle}>日历收益（{month.replace('-', '年')}月）</Text>
        <View style={styles.monthSwitch}>
          <Pressable style={styles.monthButton} onPress={() => switchMonth(-1)}><ChevronLeft color={theme.foreground} size={18} /></Pressable>
          <Pressable style={styles.monthButton} onPress={() => switchMonth(1)}><ChevronRight color={theme.foreground} size={18} /></Pressable>
        </View>
      </View>
      <View style={styles.calendarAnimated} {...panResponder.panHandlers}>
        <AnimatedReanimated.View style={calendarAnimatedStyle}>
          <View style={styles.calendarGrid}>
            {weekdays.map((day) => <Text key={day} style={styles.weekday}>{day}</Text>)}
            {cells.map((cell) => (
              <View key={cell.key} style={[styles.calendarCell, compact ? styles.calendarCellCompact : null]}>
                <Text style={[styles.dayText, cell.inMonth ? null : styles.outMonth]}>{cell.day}</Text>
                <Text style={[styles.profitText, cell.closed ? { color: theme.mutedForeground } : profitColorStyle(cell.profit, theme)]} numberOfLines={1} adjustsFontSizeToFit={false}>
                  {cell.closed ? '休市' : cell.profit === null || cell.profit === undefined ? '—' : maskNumber(signedNumber(cell.profit), amountVisible)}
                </Text>
              </View>
            ))}
          </View>
        </AnimatedReanimated.View>
        {loading && !animatingMonth ? (
          <View pointerEvents="none" style={styles.calendarLoading}>
            <ActivityIndicator color={theme.primary} />
          </View>
        ) : null}
      </View>
    </View>
  );

  if (embedded) return content;

  return (
    <Card style={styles.sectionCard}>
      <CardContent style={styles.sectionContent}>{content}</CardContent>
    </Card>
  );
}

function HoldingTable({ module, holdings, amountVisible, onSelect }: { module: InvestmentModule; holdings: HoldingItem[]; amountVisible: boolean; onSelect: (item: HoldingItem) => void }) {
  const theme = useTheme();
  const { width: screenWidth } = useWindowDimensions();
  const tableWidth = Math.max(300, screenWidth - 60);
  const valueRateWidth = Math.max(80, Math.min(94, screenWidth * 0.22));
  const profitWidth = Math.max(90, Math.min(106, screenWidth * 0.25));
  const metricsWidth = valueRateWidth + profitWidth + 14;
  const holdingNameWidth = Math.max(132, tableWidth - metricsWidth - 18);
  const assetNameWidth = Math.max(88, holdingNameWidth - 42);
  if (!holdings.length) {
    return <EmptyState icon={WalletCards} title="暂无持仓" description="通过底部加号新增一笔投资交易后，这里会展示真实持仓。" />;
  }
  return (
    <View style={stylesStatic.tableWrap}>
      <View style={stylesStatic.tableHeader}>
        <Text variant="caption" style={[stylesStatic.holdingNameCol, { width: holdingNameWidth }]}>资产名称 / 代码</Text>
        <View style={[stylesStatic.metricsHeaderWrap, { width: metricsWidth }]}>
          <Text variant="caption" style={[stylesStatic.valueRateCol, { width: valueRateWidth }]}>持仓市值/率</Text>
          <Text variant="caption" style={[stylesStatic.profitCol, { width: profitWidth }]}>{profitColumnLabel()}</Text>
        </View>
      </View>
      {holdings.map((item, index) => (
        <Pressable key={String(item.id)} style={({ pressed }) => [stylesStatic.holdingRow, pressed ? { opacity: 0.72 } : null]} onPress={() => onSelect(item)}>
          <View style={[stylesStatic.holdingNameCol, { width: holdingNameWidth }]}>
            <View style={stylesStatic.assetNameRow}>
              <View style={[stylesStatic.assetBadge, { backgroundColor: theme.foreground }]}><Text style={[stylesStatic.assetBadgeText, { color: theme.background }]}>{assetBadgeText(item)}</Text></View>
              <View style={[stylesStatic.assetTextBlock, { width: assetNameWidth }]}>
                <Text style={[stylesStatic.assetTitle, { width: assetNameWidth }]} numberOfLines={1} ellipsizeMode="clip">{item.assetName || '--'}</Text>
                <Text variant="caption" numberOfLines={1}>{item.symbol || item.market || '--'}</Text>
              </View>
            </View>
          </View>
          {/* 右侧指标固定在行顶部，三行收益内部使用固定行距，避免 Android 串行。 */}
          <View style={[stylesStatic.metricsWrap, { width: metricsWidth }]}>
            <View style={[stylesStatic.valueRateCol, stylesStatic.valueRateCell, { width: valueRateWidth }]}>
              <Text style={stylesStatic.valueRatePrimaryLine} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.52}>{maskAmount(formatMoney(item.marketValue), amountVisible)}</Text>
              <Text style={stylesStatic.valueRateSecondaryLine} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.52}>{maskNumber(formatPercent2(item.totalProfitRate ?? item.floatingProfitRate), amountVisible)}</Text>
            </View>
            <View style={[stylesStatic.profitCol, stylesStatic.profitCell, { width: profitWidth }]}>
              <Text style={[stylesStatic.profitPrimaryLine, profitColorStyle(totalHoldingProfitValue(item), theme)]} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.52}>{maskAmount(formatSignedMoney(totalHoldingProfitValue(item)), amountVisible)}</Text>
              <Text style={[stylesStatic.profitTodayLine, profitColorStyle(primaryTodayProfitValue(item, module), theme)]} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.52}>{maskAmount(formatSignedMoney(primaryTodayProfitValue(item, module)), amountVisible)}</Text>
              <Text style={[stylesStatic.profitPreviousLine, profitColorStyle(previousProfitValue(item), theme)]} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.52}>{maskAmount(formatSignedMoney(previousProfitValue(item)), amountVisible)}</Text>
            </View>
          </View>
          {index < holdings.length - 1 ? <View style={[stylesStatic.rowDivider, { backgroundColor: theme.border }]} /> : null}
        </Pressable>
      ))}
    </View>
  );
}

function TransactionList({ transactions, amountVisible, onSelect }: { transactions: InvestmentTransactionItem[]; amountVisible: boolean; onSelect?: (item: InvestmentTransactionItem) => void }) {
  const theme = useTheme();
  if (!transactions.length) {
    return <EmptyState icon={CalendarDays} title="暂无交易" description="投资买入、卖出记录会按后端返回展示。" />;
  }
  return (
    <View>
      {transactions.map((item, index) => (
        <View key={String(item.id)}>
          <Pressable style={({ pressed }) => [stylesStatic.transactionRow, pressed && onSelect ? { opacity: 0.72 } : null]} onPress={() => onSelect?.(item)}>
            <View style={stylesStatic.transactionInfoCol}>
              <Text style={stylesStatic.transactionTitle} numberOfLines={1} ellipsizeMode="tail">{item.assetName || item.symbol || '--'}</Text>
              <Text variant="caption" style={stylesStatic.transactionMeta} numberOfLines={1} ellipsizeMode="tail">
                {transactionTypeLabel(item.type)} · {formatDateTime(item.transactionTime)} · {transactionStatusLabel(item.status)}
              </Text>
            </View>
            <View style={stylesStatic.transactionAmountCol}>
              <Text style={[stylesStatic.transactionAmount, tradeAmountColor(item, theme)]} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.62}>{maskAmount(formatTradeAmount(item), amountVisible)}</Text>
            </View>
          </Pressable>
          {index < transactions.length - 1 ? <Separator /> : null}
        </View>
      ))}
    </View>
  );
}

function HoldingDetailModal({ holding, transactions, amountVisible, month: detailMonth, onMonthChange, onTransactionSelect, onClose }: { holding: HoldingItem | null; transactions: InvestmentTransactionItem[]; amountVisible: boolean; month: string; onMonthChange: (value: string) => void; onTransactionSelect: (item: InvestmentTransactionItem) => void; onClose: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const [year, month] = detailMonth.split('-').map(Number);
  const [calendarRows, setCalendarRows] = useState<InvestmentCalendarDayProfit[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!holding?.id) {
      return;
    }
    let cancelled = false;
    setLoading(true);
    investmentApi.holdingProfitCalendar(String(holding.id), year, month)
      .then((rows) => {
        if (!cancelled) setCalendarRows(rows);
      })
      .catch(() => {
        if (!cancelled) setCalendarRows([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [holding?.id, month, year]);

  if (!holding) {
    return null;
  }

  return (
    <Modal visible transparent animationType="slide" onRequestClose={onClose}>
      <View style={styles.modalBackdrop} />
      <View style={styles.detailSheet}>
        <View style={styles.rowBetween}>
          <View>
            <Text style={styles.sheetTitle}>{holding.assetName || '--'}</Text>
            <Text variant="muted">{holding.symbol || holding.market || '--'}</Text>
          </View>
          <Pressable style={styles.closeButton} onPress={onClose}><X color={theme.foreground} size={22} /></Pressable>
        </View>
        <View style={styles.detailGrid}>
          <Metric label="持仓市值" value={maskAmount(formatMoney(holding.marketValue), amountVisible)} />
          <Metric label="持有收益" value={maskAmount(formatSignedMoney(holding.totalProfit ?? holding.floatingProfit), amountVisible)} positive={safeNumber(holding.totalProfit ?? holding.floatingProfit) >= 0} />
          <Metric label="收益率" value={maskNumber(formatPercent2(holding.totalProfitRate ?? holding.floatingProfitRate), amountVisible)} />
          <Metric label="最新价格" value={maskNumber(priceText(holding.latestPrice, holding.priceScale), amountVisible)} />
        </View>
        <CalendarProfitCard
          rows={calendarRows}
          month={detailMonth}
          amountVisible={amountVisible}
          loading={loading}
          onPrev={() => onMonthChange(shiftMonth(detailMonth, -1))}
          onNext={() => onMonthChange(shiftMonth(detailMonth, 1))}
          embedded
        />
        <Text style={styles.sectionTitle}>持仓交易</Text>
        <TransactionList transactions={transactions.slice(0, 5)} amountVisible={amountVisible} onSelect={onTransactionSelect} />
      </View>
    </Modal>
  );
}


function HoldingSortModal({ visible, value, onChange, onClose }: { visible: boolean; value: HoldingSortMode; onChange: (value: HoldingSortMode) => void; onClose: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const options: HoldingSortMode[] = ['MARKET_DESC', 'MARKET_ASC', 'PROFIT_DESC', 'PROFIT_ASC'];
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <Pressable style={styles.modalBackdrop} onPress={onClose} />
      <View style={styles.sortSheet}>
        <View style={styles.rowBetween}>
          <Text style={styles.sheetTitle}>持仓排序</Text>
          <Pressable style={styles.closeButton} onPress={onClose}><X color={theme.foreground} size={22} /></Pressable>
        </View>
        {options.map((option) => (
          <Pressable key={option} style={[styles.sortOption, value === option ? styles.sortOptionActive : null]} onPress={() => onChange(option)}>
            <Text style={[styles.sortOptionText, value === option ? styles.sortOptionTextActive : null]}>{holdingSortLabel(option)}</Text>
          </Pressable>
        ))}
      </View>
    </Modal>
  );
}

function TransactionsModal({ visible, transactions, amountVisible, onSelect, onClose }: { visible: boolean; transactions: InvestmentTransactionItem[]; amountVisible: boolean; onSelect: (item: InvestmentTransactionItem) => void; onClose: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <View style={styles.modalBackdrop} />
      <View style={styles.detailSheet}>
        <View style={styles.rowBetween}>
          <Text style={styles.sheetTitle}>全部交易记录</Text>
          <Pressable style={styles.closeButton} onPress={onClose}><X color={theme.foreground} size={22} /></Pressable>
        </View>
        <ScrollView showsVerticalScrollIndicator={false}>
          <TransactionList transactions={transactions} amountVisible={amountVisible} onSelect={(item) => { onClose(); onSelect(item); }} />
        </ScrollView>
      </View>
    </Modal>
  );
}

function TransactionDetailModal({ transaction, amountVisible, onClose }: { transaction: InvestmentTransactionItem | null; amountVisible: boolean; onClose: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  if (!transaction) return null;
  return (
    <Modal visible transparent animationType="slide" onRequestClose={onClose}>
      <View style={styles.modalBackdrop} />
      <View style={styles.detailSheet}>
        <View style={styles.rowBetween}>
          <View>
            <Text style={styles.sheetTitle}>{transaction.assetName || transaction.symbol || '交易详情'}</Text>
            <Text variant="muted">{transactionTypeLabel(transaction.type)} · {formatDateTime(transaction.transactionTime)}</Text>
          </View>
          <Pressable style={styles.closeButton} onPress={onClose}><X color={theme.foreground} size={22} /></Pressable>
        </View>
        <View style={styles.detailGrid}>
          <Metric label="交易金额" value={maskAmount(formatTradeAmount(transaction), amountVisible)} positive={transaction.type === 'SELL'} />
          <Metric label="数量" value={maskNumber(numberOrDash(transaction.tradeQuantity ?? transaction.quantity), amountVisible)} />
          <Metric label="价格" value={maskNumber(numberOrDash(transaction.tradePrice ?? transaction.price), amountVisible)} />
          <Metric label="手续费" value={maskAmount(formatMoney(transaction.fee), amountVisible)} />
        </View>
        <Card style={styles.sectionCard}>
          <CardContent style={styles.sectionContent}>
            <Text variant="muted">账户：{transaction.accountName || '--'}</Text>
            <Text variant="muted">确认日期：{transaction.confirmedDate || '--'}</Text>
            <Text variant="muted">状态：{transactionStatusLabel(transaction.status)}</Text>
            <Text variant="muted">备注：{transaction.note || '--'}</Text>
          </CardContent>
        </Card>
      </View>
    </Modal>
  );
}

function TransactionComposer({
  visible,
  form,
  formError,
  holdings,
  accounts,
  lookupItems,
  loading,
  lookupLoading,
  onClose,
  onLookup,
  onSubmit,
  onChange
}: {
  visible: boolean;
  form: TransactionFormState;
  formError: string | null;
  holdings: HoldingItem[];
  accounts: LedgerAccount[];
  lookupItems: AssetLookupItem[];
  loading: boolean;
  lookupLoading: boolean;
  onClose: () => void;
  onLookup: () => void;
  onSubmit: () => void;
  onChange: (value: TransactionFormState) => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const selectedHolding = holdings.find((item) => String(item.id) === form.holdingId);
  const isFundAmount = form.assetType === 'FUND' && form.type === 'BUY' && form.inputMode === 'AMOUNT_NAV';

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.modalRoot}>
        <View style={styles.modalBackdrop} />
        <View style={styles.composerSheet}>
          <View style={styles.rowBetween}>
            <View>
              <Text style={styles.sheetTitle}>投资交易</Text>
              <Text variant="muted">买入会扣减资金账户，卖出会回到账户余额。</Text>
            </View>
            <Pressable style={styles.closeButton} onPress={onClose}><X color={theme.foreground} size={22} /></Pressable>
          </View>
          <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.formContent}>
            <View style={styles.choiceRow}>
              <ChoicePill label="买入" active={form.type === 'BUY'} onPress={() => onChange({ ...form, type: 'BUY' })} />
              <ChoicePill label="卖出" active={form.type === 'SELL'} onPress={() => onChange({ ...form, type: 'SELL', source: 'HOLDING' })} />
            </View>
            <View style={styles.choiceRow}>
              <ChoicePill label="现有持仓" active={form.source === 'HOLDING'} onPress={() => onChange({ ...form, source: 'HOLDING' })} />
              <ChoicePill label="识别新资产" active={form.source === 'LOOKUP'} disabled={form.type === 'SELL'} onPress={() => onChange({ ...form, source: 'LOOKUP' })} />
            </View>

            {form.source === 'HOLDING' ? (
              <View style={styles.selectorBlock}>
                <Text variant="muted">选择持仓</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.selectorRow}>
                  {holdings.map((item) => (
                    <ChoicePill key={String(item.id)} label={item.assetName || item.symbol || '--'} active={form.holdingId === String(item.id)} onPress={() => onChange({ ...form, holdingId: String(item.id), assetId: String(item.assetId), assetType: normalizeAssetType(item.assetType) })} />
                  ))}
                </ScrollView>
                {selectedHolding ? <Text variant="caption">{selectedHolding.symbol || '--'} · {moduleLabel(normalizeAssetType(selectedHolding.assetType) as InvestmentModule)}</Text> : null}
              </View>
            ) : (
              <View style={styles.selectorBlock}>
                <Text variant="muted">资产类型</Text>
                <View style={styles.choiceRow}>
                  {(['FUND', 'STOCK', 'CRYPTO'] as AssetType[]).map((type) => <ChoicePill key={type} label={moduleLabel(type as InvestmentModule)} active={form.assetType === type} onPress={() => onChange({ ...form, assetType: type })} />)}
                </View>
                <View style={styles.lookupRow}>
                  <Input containerStyle={styles.lookupInput} label="资产名称 / 代码" value={form.keyword} onChangeText={(keyword) => onChange({ ...form, keyword })} placeholder="如 110020 / BTC" />
                  <Button style={styles.lookupButton} loading={lookupLoading} onPress={onLookup}><Search color={theme.primaryForeground} size={16} />识别</Button>
                </View>
                {lookupItems[0] ? <Text variant="caption">已识别：{assetName(lookupItems[0])} · {lookupItems[0].market || 'UNKNOWN'} · 最新价 {priceText(lookupItems[0].latestPrice)}</Text> : null}
              </View>
            )}

            <Text variant="muted">资金账户</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.selectorRow}>
              {accounts.map((account) => <ChoicePill key={String(account.id)} label={account.name || '--'} active={form.accountId === String(account.id)} onPress={() => onChange({ ...form, accountId: String(account.id) })} />)}
            </ScrollView>

            {form.assetType === 'FUND' && form.type === 'BUY' ? (
              <View style={styles.choiceRow}>
                <ChoicePill label="金额申购" active={form.inputMode === 'AMOUNT_NAV'} onPress={() => onChange({ ...form, inputMode: 'AMOUNT_NAV' })} />
                <ChoicePill label="数量价格" active={form.inputMode === 'QUANTITY_PRICE'} onPress={() => onChange({ ...form, inputMode: 'QUANTITY_PRICE' })} />
              </View>
            ) : null}

            {isFundAmount ? (
              <Input label="买入总金额" value={form.tradeAmount} onChangeText={(tradeAmount) => onChange({ ...form, tradeAmount })} keyboardType="decimal-pad" placeholder="0.00" />
            ) : (
              <View style={styles.twoColumns}>
                <Input containerStyle={styles.columnInput} label="数量" value={form.quantity} onChangeText={(quantity) => onChange({ ...form, quantity })} keyboardType="decimal-pad" placeholder="0" />
                <Input containerStyle={styles.columnInput} label="价格" value={form.price} onChangeText={(price) => onChange({ ...form, price })} keyboardType="decimal-pad" placeholder="0.0000" />
              </View>
            )}
            <View style={styles.twoColumns}>
              <Input containerStyle={styles.columnInput} label="手续费" value={form.fee} onChangeText={(fee) => onChange({ ...form, fee })} keyboardType="decimal-pad" placeholder="0" />
              <Input containerStyle={styles.columnInput} label="交易时间" value={form.time} onChangeText={(time) => onChange({ ...form, time })} placeholder="YYYY-MM-DD HH:mm" />
            </View>
            <Input label="备注" value={form.note} onChangeText={(note) => onChange({ ...form, note })} placeholder="可选" />
            {formError ? <Text variant="error">{formError}</Text> : null}
            <Button size="lg" loading={loading} onPress={onSubmit}>保存交易</Button>
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

function ChoicePill({ label, active, disabled, onPress }: { label: string; active?: boolean; disabled?: boolean; onPress: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  return (
    <Pressable disabled={disabled} style={[styles.choicePill, active ? styles.choicePillActive : null, disabled ? styles.disabled : null]} onPress={onPress}>
      <Text style={[styles.choiceText, active ? styles.choiceTextActive : null]} numberOfLines={1}>{label}</Text>
    </Pressable>
  );
}

function EmptyState({ icon: Icon, title, description }: { icon: typeof LineChart; title: string; description: string }) {
  const theme = useTheme();
  return (
    <View style={stylesStatic.emptyState}>
      <Icon color={theme.mutedForeground} size={28} strokeWidth={2} />
      <Text style={stylesStatic.emptyTitle}>{title}</Text>
      <Text variant="muted" style={stylesStatic.emptyDesc}>{description}</Text>
    </View>
  );
}

function metricColor(value: string, positive: boolean | null | undefined, theme: ReturnType<typeof useTheme>) {
  if (value === '--') return theme.foreground;
  if (positive === false) return theme.destructive;
  if (positive === true) return theme.success;
  return theme.foreground;
}

function overviewHoldingCount(overview: InvestmentOverview | undefined) {
  const modules = overview?.moduleAssets ?? [];
  if (!modules.length) return undefined;
  return modules.reduce((sum, item) => sum + safeNumber(item.holdingCount), 0);
}

function activeHoldingCount(holdings: HoldingItem[], fallback?: number | null) {
  if (!holdings.length) return fallback ?? 0;
  // 清仓持仓数量为 0 或状态非正常，资产卡片持仓数量不再计入。
  return holdings.filter((item) => item.status !== 0 && safeNumber(item.quantity) > 0).length;
}

function buildSummary(module: InvestmentModule, overview: InvestmentOverview | undefined, moduleAsset: InvestmentModuleAsset | null, holdings: HoldingItem[]) {
  const count = activeHoldingCount(holdings, module === 'ALL' ? overviewHoldingCount(overview) : moduleAsset?.holdingCount);
  if (module === 'ALL') {
    const todayProfit = overview?.todayProfitAvailable === false ? null : overview?.todayProfit;
    return {
      title: '总投资资产（元）',
      amount: overview?.totalInvestmentAsset,
      stats: [
        { label: '总收益/率', value: formatSignedMoney(overview?.holdingProfit), secondaryValue: formatPercent2(overview?.holdingProfitRate), positive: safeNumber(overview?.holdingProfit) >= 0, secondaryPositive: safeNumber(overview?.holdingProfitRate) >= 0 },
        { label: '今/昨收益', value: formatSignedMoney(todayProfit), secondaryValue: formatSignedMoney(overview?.yesterdayProfit), positive: safeNumber(todayProfit) >= 0, secondaryPositive: safeNumber(overview?.yesterdayProfit) >= 0 },
        { label: holdingCountLabel(module), value: String(count), positive: null }
      ]
    };
  }

  const todayProfit = moduleAsset?.primaryProfitAvailable === false ? null : moduleAsset?.primaryProfitAmount;
  return {
    title: `${moduleTitle(module)}（元）`,
    amount: moduleAsset?.assetAmount,
    stats: [
      { label: '总收益/率', value: formatSignedMoney(moduleAsset?.holdingProfit), secondaryValue: formatPercent2(moduleAsset?.holdingProfitRate), positive: safeNumber(moduleAsset?.holdingProfit) >= 0, secondaryPositive: safeNumber(moduleAsset?.holdingProfitRate) >= 0 },
      { label: '今/昨收益', value: formatSignedMoney(todayProfit), secondaryValue: formatSignedMoney(moduleAsset?.yesterdayProfit), positive: safeNumber(todayProfit) >= 0, secondaryPositive: safeNumber(moduleAsset?.yesterdayProfit) >= 0 },
      { label: holdingCountLabel(module), value: String(count), positive: null }
    ]
  };
}

function buildCalendarCells(month: string, rows: InvestmentCalendarDayProfit[]) {
  const [year, monthNumber] = month.split('-').map(Number);
  const first = new Date(year, monthNumber - 1, 1);
  const start = new Date(first);
  start.setDate(first.getDate() - first.getDay());
  const map = new Map(rows.map((item) => [item.date, item]));
  return Array.from({ length: 35 }).map((_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    const key = formatDate(date);
    const row = map.get(key);
    return {
      key,
      day: date.getDate(),
      inMonth: date.getMonth() === monthNumber - 1,
      profit: row?.profitAmount,
      closed: row ? row.tradingDay === false || row.marketClosed === true || row.statusLabel?.includes('休市') === true : false
    };
  });
}

function shouldHandleCalendarSwipe(dx: number, dy: number) {
  return Math.abs(dx) > 8 && Math.abs(dx) > Math.abs(dy) * 1.15;
}

function createEmptyForm(): TransactionFormState {
  const now = new Date();
  return {
    source: 'HOLDING',
    type: 'BUY',
    assetType: 'FUND',
    holdingId: '',
    assetId: '',
    accountId: '',
    keyword: '',
    market: '',
    inputMode: 'QUANTITY_PRICE',
    tradeAmount: '',
    quantity: '',
    price: '',
    fee: '0',
    time: `${formatDate(now)} ${`${now.getHours()}`.padStart(2, '0')}:${`${now.getMinutes()}`.padStart(2, '0')}`,
    note: ''
  };
}

function moduleTitle(module: InvestmentModule) {
  if (module === 'FUND') return '基金总资产';
  if (module === 'STOCK') return '股票市值';
  if (module === 'CRYPTO') return '加密资产';
  return '总投资资产';
}

function moduleLabel(module?: InvestmentModule | string | null) {
  if (module === 'FUND') return '基金';
  if (module === 'STOCK') return '股票';
  if (module === 'CRYPTO') return '虚拟货币';
  return '总资产';
}

function holdingCountLabel(module: InvestmentModule) {
  if (module === 'FUND') return '持有基金数';
  if (module === 'STOCK') return '持仓股票数';
  if (module === 'CRYPTO') return '持有币种数';
  return '持仓数';
}

function profitColumnLabel() {
  return '总/今/昨日收益';
}

function holdingSortLabel(sort: HoldingSortMode) {
  if (sort === 'MARKET_DESC') return '市值降序';
  if (sort === 'MARKET_ASC') return '市值升序';
  if (sort === 'PROFIT_DESC') return '收益降序';
  return '收益升序';
}

function sortHoldings(items: HoldingItem[], sort: HoldingSortMode) {
  return [...items].sort((a, b) => {
    const marketDiff = safeNumber(a.marketValue) - safeNumber(b.marketValue);
    const profitDiff = safeNumber(totalHoldingProfitValue(a)) - safeNumber(totalHoldingProfitValue(b));
    if (sort === 'MARKET_ASC') return marketDiff;
    if (sort === 'MARKET_DESC') return -marketDiff;
    if (sort === 'PROFIT_ASC') return profitDiff;
    return -profitDiff;
  });
}

function chartPointValue(item: { assetAmount?: number | null; marketValue?: number | null; totalProfit?: number | null; holdingProfit?: number | null; dailyProfit?: number | null; primaryProfitAmount?: number | null }, mode: Exclude<ChartMode, 'calendar'>) {
  if (mode === 'asset') return safeNumber(item.assetAmount ?? item.marketValue);
  return safeNumber(item.holdingProfit ?? item.totalProfit ?? item.dailyProfit ?? item.primaryProfitAmount);
}

function totalHoldingProfitValue(item: HoldingItem) {
  // 总收益以后端持仓收益字段为准，前端不反算。
  return item.totalProfit ?? item.floatingProfit ?? null;
}

function primaryTodayProfitValue(item: HoldingItem, module: InvestmentModule) {
  // 今日收益只展示后端明确标记可用的今日口径；休市/未更新不能用主收益或昨日收益冒充。
  if (!isTodayProfitDisplayable(item, module)) return null;
  return item.todayProfit ?? item.primaryProfitAmount ?? null;
}

function previousProfitValue(item: HoldingItem) {
  // 昨日收益字段由后端按昨日或最近一个收益日口径返回，前端只展示不反算。
  return item.yesterdayProfit ?? item.secondaryProfitAmount ?? null;
}

function isTodayProfitDisplayable(item: HoldingItem, module: InvestmentModule) {
  const assetType = item.assetType || module;
  if (assetType === 'CRYPTO') {
    return item.primaryProfitAmount !== null && item.primaryProfitAmount !== undefined;
  }
  if (item.todayProfitAvailable !== true) {
    return false;
  }
  return item.priceStatus !== 'MARKET_CLOSED' && item.priceStatus !== 'TODAY_PRICE_NOT_AVAILABLE';
}

function assetBadgeText(item: HoldingItem) {
  if (item.assetType === 'FUND') return '基';
  if (item.assetType === 'STOCK') return '股';
  if (item.assetType === 'CRYPTO') return '币';
  return (item.assetName || item.symbol || '投').slice(0, 1);
}

function normalizeAssetType(value?: string | null): AssetType {
  if (value === 'STOCK' || value === 'FUND' || value === 'CRYPTO') return value;
  return 'FUND';
}

function transactionTypeLabel(type?: string | null) {
  if (type === 'BUY') return '买入';
  if (type === 'SELL') return '卖出';
  return type || '--';
}

function transactionStatusLabel(status?: string | null) {
  // 后端投资交易状态返回 code，移动端详情和列表统一转成用户可读文案。
  if (status === 'NORMAL') return '正常';
  if (status === 'CONFIRMED') return '已确认';
  if (status === 'PENDING_CONFIRM') return '待确认';
  if (status === 'REVOKED') return '已撤销';
  if (status === 'CANCELLED') return '已取消';
  return status || '--';
}

function formatTradeAmount(item: InvestmentTransactionItem) {
  const amount = item.tradeAmount ?? item.amount;
  if (amount === null || amount === undefined || Number.isNaN(amount)) return '--';
  const prefix = item.type === 'SELL' ? '+' : '-';
  return `${prefix}${formatMoney(amount)}`;
}

function tradeAmountColor(item: InvestmentTransactionItem, theme: ReturnType<typeof useTheme>) {
  if (item.type === 'SELL') return { color: theme.success };
  if (item.type === 'BUY') return { color: theme.destructive };
  return { color: theme.foreground };
}

function profitColorStyle(value: number | null | undefined, theme: ReturnType<typeof useTheme>) {
  if (value === null || value === undefined || Number.isNaN(value)) return { color: theme.mutedForeground };
  if (value > 0) return { color: theme.success };
  if (value < 0) return { color: theme.destructive };
  return { color: theme.foreground };
}

function formatPercent2(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) return '--';
  return `${value.toFixed(2)}%`;
}

function maskAmount(value: string, visible: boolean) {
  return visible ? value : value === '--' ? '--' : '••••';
}

function maskNumber(value: string, visible: boolean) {
  return visible ? value : value === '--' || value === '—' ? value : '••••';
}

function safeNumber(value: number | null | undefined) {
  return value === null || value === undefined || Number.isNaN(value) ? 0 : value;
}

function positive(value: string) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0;
}

function numberText(value: number) {
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function numberOrDash(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) return '--';
  return value.toLocaleString('zh-CN', { maximumFractionDigits: 6 });
}

function signedNumber(value: number) {
  const prefix = value > 0 ? '+' : '';
  return `${prefix}${value.toFixed(2)}`;
}

function shortAmount(value: number) {
  if (Math.abs(value) >= 10000) return `${Math.round(value / 10000)}万`;
  return `${Math.round(value)}`;
}

function updateSelectedPoint(locationX: number, layoutWidth: number, count: number, onSelect: (index: number) => void) {
  if (count <= 0) return;
  const width = 330;
  const chartLeft = 48;
  const chartRight = 12;
  const viewBoxX = (Math.max(0, Math.min(locationX, layoutWidth)) / Math.max(layoutWidth, 1)) * width;
  const ratio = (viewBoxX - chartLeft) / Math.max(width - chartLeft - chartRight, 1);
  const nextIndex = Math.round(Math.max(0, Math.min(ratio, 1)) * Math.max(count - 1, 0));
  onSelect(nextIndex);
}

function priceText(value?: number | null, scale?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) return '--';
  const digits = Math.min(scale || 4, 6);
  return value.toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits });
}

function assetName(item: AssetLookupItem) {
  return [item.name, item.symbol].filter(Boolean).join(' ') || '--';
}

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function monthKey(date: Date) {
  return formatDate(date).slice(0, 7);
}

function shiftMonth(value: string, offset: number) {
  const [year, month] = value.split('-').map(Number);
  return monthKey(new Date(year, month - 1 + offset, 1));
}

function toLocalDateTime(value: string) {
  const normalized = value.trim().replace(' ', 'T');
  return normalized.length === 16 ? `${normalized}:00` : normalized;
}

function formatDateTime(value?: string | null) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return `${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')} ${`${date.getHours()}`.padStart(2, '0')}:${`${date.getMinutes()}`.padStart(2, '0')}`;
}

function distributionColor(index: number, primary: string, muted: string, border: string) {
  if (index === 0) return primary;
  if (index === 1) return muted;
  return border;
}

const createStyles = (theme: ReturnType<typeof useTheme>) =>
  StyleSheet.create({
    page: { backgroundColor: theme.background, flex: 1 },
    loading: { alignItems: 'center', backgroundColor: theme.background, flex: 1, justifyContent: 'center' },
    content: { gap: 14, paddingBottom: 26, paddingHorizontal: 16, paddingTop: 16 },
    header: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 },
    headerSpacer: { width: 42 },
    screenTitle: { color: theme.foreground, fontSize: 28, fontWeight: '900', letterSpacing: 1 },
    iconButton: { alignItems: 'center', height: 42, justifyContent: 'center', width: 42 },
    moduleTabs: { backgroundColor: theme.secondary, borderRadius: 22, flexDirection: 'row', padding: 4 },
    moduleTab: { alignItems: 'center', borderRadius: 18, flex: 1, minHeight: 36, justifyContent: 'center' },
    moduleTabActive: { backgroundColor: theme.foreground },
    moduleTabText: { color: theme.foreground, fontSize: 14, fontWeight: '700' },
    moduleTabTextActive: { color: theme.background },
    summaryCard: { borderRadius: 18 },
    summaryCardHidden: { opacity: 0.92 },
    summaryContent: { gap: 14, padding: 16 },
    rowBetween: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between' },
    inlineAction: { alignItems: 'center', flexDirection: 'row', gap: 8 },
    cardTitle: { color: theme.foreground, fontSize: 17, fontWeight: '800' },
    eyeStateButton: { alignItems: 'center', backgroundColor: theme.secondary, borderRadius: 13, height: 26, justifyContent: 'center', width: 30 },
    eyeStateButtonHidden: { backgroundColor: theme.foreground },
    refreshButton: { alignItems: 'center', backgroundColor: theme.secondary, borderRadius: 16, height: 32, justifyContent: 'center', width: 32 },
    toastWrap: { alignItems: 'center', left: 0, position: 'absolute', right: 0, top: 78, zIndex: 30 },
    toastBox: { backgroundColor: theme.foreground, borderRadius: 999, paddingHorizontal: 14, paddingVertical: 9, shadowColor: theme.shadow, shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.16, shadowRadius: 14 },
    toastText: { color: theme.background, fontSize: 13, fontWeight: '800' },
    summaryAmount: { color: theme.foreground, fontSize: 38, fontWeight: '900', letterSpacing: -1.4, lineHeight: 46 },
    summaryAmountSkeleton: { borderRadius: 12, height: 46, width: '72%' },
    summaryMetricSkeleton: { alignItems: 'center', flex: 1, gap: 8, justifyContent: 'center', minHeight: 48, paddingHorizontal: 8 },
    summaryMetricLabelSkeleton: { borderRadius: 6, height: 12, width: '58%' },
    summaryMetricValueSkeleton: { borderRadius: 8, height: 18, width: '74%' },
    summaryStats: { flexDirection: 'row' },
    summaryStatWrap: { alignItems: 'center', flex: 1, flexDirection: 'row' },
    verticalDivider: { backgroundColor: theme.border, height: 44, width: 1 },
    sectionCard: { borderRadius: 18 },
    sectionContent: { gap: 14, padding: 14 },
    sectionTitle: { color: theme.foreground, fontSize: 18, fontWeight: '900', letterSpacing: -0.4 },
    embeddedCalendar: { gap: 14 },
    chartTabs: { backgroundColor: theme.secondary, borderRadius: 20, flexDirection: 'row', padding: 4 },
    chartTab: { alignItems: 'center', borderRadius: 16, flex: 1, minHeight: 34, justifyContent: 'center', paddingHorizontal: 4 },
    chartTabActive: { backgroundColor: theme.foreground },
    chartTabText: { color: theme.foreground, fontSize: 11, fontWeight: '800' },
    chartTabTextActive: { color: theme.background },
    periodRow: { flexDirection: 'row', gap: 8, justifyContent: 'space-between' },
    periodPill: { alignItems: 'center', backgroundColor: theme.secondary, borderRadius: 16, flex: 1, minHeight: 32, justifyContent: 'center' },
    periodPillActive: { backgroundColor: theme.foreground },
    periodText: { color: theme.foreground, fontSize: 12, fontWeight: '700' },
    periodTextActive: { color: theme.background },
    monthSwitch: { flexDirection: 'row', gap: 4 },
    monthButton: { alignItems: 'center', backgroundColor: theme.secondary, borderRadius: 14, height: 30, justifyContent: 'center', width: 30 },
    calendarAnimated: { overflow: 'hidden', position: 'relative' },
    calendarLoading: { ...StyleSheet.absoluteFillObject, alignItems: 'center', backgroundColor: `${theme.card}cc`, justifyContent: 'center' },
    calendarGrid: { flexDirection: 'row', flexWrap: 'wrap', rowGap: 5 },
    weekday: { color: theme.foreground, fontSize: 13, fontWeight: '800', textAlign: 'center', width: `${100 / 7}%` },
    calendarCell: { alignItems: 'center', gap: 3, height: 42, justifyContent: 'center', width: `${100 / 7}%` },
    calendarCellCompact: { height: 38 },
    dayText: { color: theme.foreground, fontSize: 13, fontWeight: '700' },
    outMonth: { color: theme.mutedForeground, fontWeight: '500' },
    profitText: { fontSize: 8.5, fontWeight: '800', includeFontPadding: false, lineHeight: 11, maxWidth: 52, textAlign: 'center' },
    linkButton: { alignItems: 'center', flexDirection: 'row', gap: 4 },
    linkText: { color: theme.foreground, fontSize: 13, fontWeight: '800' },
    moreIconButton: { alignItems: 'center', backgroundColor: theme.secondary, borderRadius: 14, height: 30, justifyContent: 'center', width: 34 },
    sortButton: { backgroundColor: theme.secondary, borderRadius: 14, paddingHorizontal: 10, paddingVertical: 6 },
    sortText: { color: theme.foreground, fontSize: 12, fontWeight: '800' },
    sortSheet: { backgroundColor: theme.card, borderColor: theme.border, borderRadius: 20, borderWidth: 1, gap: 10, left: 24, padding: 16, position: 'absolute', right: 24, top: '34%' },
    sortOption: { backgroundColor: theme.secondary, borderRadius: 14, paddingHorizontal: 14, paddingVertical: 12 },
    sortOptionActive: { backgroundColor: theme.foreground },
    sortOptionText: { color: theme.foreground, fontSize: 14, fontWeight: '800' },
    sortOptionTextActive: { color: theme.background },
    pressed: { opacity: 0.72, transform: [{ scale: 0.98 }] },
    modalRoot: { flex: 1, justifyContent: 'flex-end' },
    modalBackdrop: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.22)' },
    composerSheet: { backgroundColor: theme.card, borderColor: theme.border, borderTopLeftRadius: 24, borderTopRightRadius: 24, borderWidth: 1, maxHeight: '88%', padding: 16 },
    detailSheet: { backgroundColor: theme.card, borderColor: theme.border, borderTopLeftRadius: 24, borderTopRightRadius: 24, borderWidth: 1, bottom: 0, gap: 14, left: 0, maxHeight: '90%', padding: 16, position: 'absolute', right: 0 },
    sheetTitle: { color: theme.foreground, fontSize: 20, fontWeight: '900' },
    closeButton: { alignItems: 'center', height: 34, justifyContent: 'center', width: 34 },
    formContent: { gap: 13, paddingBottom: 20 },
    choiceRow: { flexDirection: 'row', gap: 8 },
    choicePill: { alignItems: 'center', backgroundColor: theme.secondary, borderRadius: 16, flex: 1, minHeight: 34, justifyContent: 'center', paddingHorizontal: 10 },
    choicePillActive: { backgroundColor: theme.foreground },
    choiceText: { color: theme.foreground, fontSize: 13, fontWeight: '800' },
    choiceTextActive: { color: theme.background },
    disabled: { opacity: 0.45 },
    selectorBlock: { gap: 8 },
    selectorRow: { gap: 8 },
    lookupRow: { alignItems: 'flex-end', flexDirection: 'row', gap: 10 },
    lookupInput: { flex: 1 },
    lookupButton: { minHeight: 46 },
    twoColumns: { flexDirection: 'row', gap: 10 },
    columnInput: { flex: 1 },
    detailGrid: { flexDirection: 'row', flexWrap: 'wrap', rowGap: 12 },
  });

const stylesStatic = StyleSheet.create({
  gridLineVertical: { height: '100%', opacity: 0.18, position: 'absolute', width: StyleSheet.hairlineWidth },
  gridLineHorizontal: { opacity: 0.18, height: StyleSheet.hairlineWidth, position: 'absolute', width: '100%' },
  errorCard: { borderRadius: 14 },
  skeletonCard: { borderRadius: 18 },
  skeletonContent: { gap: 12 },
  skeletonLine: { borderRadius: 10, height: 16 },
  // Android 字体默认 padding 会让 -- 和数字看起来不在同一水平线，这里固定行高保证三列平齐。
  metricBlock: { alignItems: 'center', flex: 1, gap: 3, justifyContent: 'center', minHeight: 58, paddingHorizontal: 6 },
  metricLabel: { fontSize: 12, includeFontPadding: false, lineHeight: 16, textAlign: 'center' },
  metricValue: { fontSize: 13, fontWeight: '900', includeFontPadding: false, lineHeight: 16, textAlign: 'center' },
  metricSecondaryValue: { fontSize: 13, fontWeight: '900', includeFontPadding: false, lineHeight: 16, textAlign: 'center' },
  distributionBlock: { borderTopColor: '#e4e4e7', borderTopWidth: StyleSheet.hairlineWidth, gap: 10, paddingTop: 10 },
  distributionTitle: { fontSize: 15, fontWeight: '900' },
  distributionLegend: { flexDirection: 'row', justifyContent: 'space-between' },
  legendItem: { alignItems: 'center', flexDirection: 'row', gap: 6 },
  legendDot: { borderRadius: 4, height: 10, width: 10 },
  legendText: { fontSize: 12, fontWeight: '700' },
  distributionTrack: { borderRadius: 6, flexDirection: 'row', height: 12, overflow: 'hidden' },
  distributionFill: { height: 12 },
  chartWrap: { alignItems: 'center', minHeight: 180 },
  tableWrap: { gap: 4 },
  tableHeader: { alignItems: 'center', alignSelf: 'stretch', flexDirection: 'row', minHeight: 28, paddingBottom: 6, width: '100%' },
  // 持仓行固定高度，保证资产名、持仓市值和总/今/昨日收益在同一行区域内对齐。
  holdingRow: { alignItems: 'flex-start', alignSelf: 'stretch', flexDirection: 'row', height: 108, paddingTop: 0, position: 'relative', width: '100%' },
  holdingNameCol: { flexShrink: 0, minWidth: 0 },
  metricsHeaderWrap: { flexDirection: 'row', gap: 14, marginLeft: 'auto' },
  metricsWrap: { alignItems: 'flex-start', flexDirection: 'row', gap: 14, position: 'absolute', right: 0, top: 4 },
  valueRateCol: { flexShrink: 0, minWidth: 0 },
  profitCol: { flexShrink: 0, minWidth: 0 },
  stackedPrimary: { fontSize: 12, fontWeight: '900', lineHeight: 16, textAlign: 'right' },
  stackedSecondary: { fontSize: 12, fontWeight: '800', lineHeight: 16, marginTop: 5, textAlign: 'right' },
  valueRateCell: { height: 48, position: 'relative' },
  profitCell: { height: 62, position: 'relative' },
  valueRatePrimaryLine: { fontSize: 12, fontWeight: '900', includeFontPadding: false, left: 0, lineHeight: 14, position: 'absolute', right: 0, textAlign: 'right', top: 0 },
  valueRateSecondaryLine: { fontSize: 12, fontWeight: '800', includeFontPadding: false, left: 0, lineHeight: 14, position: 'absolute', right: 0, textAlign: 'right', top: 18 },
  profitPrimaryLine: { fontSize: 11.5, fontWeight: '900', includeFontPadding: false, left: 0, lineHeight: 12, position: 'absolute', right: 0, textAlign: 'right', top: 0 },
  profitTodayLine: { fontSize: 10.5, fontWeight: '800', includeFontPadding: false, left: 0, lineHeight: 12, position: 'absolute', right: 0, textAlign: 'right', top: 12 },
  profitPreviousLine: { fontSize: 10.5, fontWeight: '800', includeFontPadding: false, left: 0, lineHeight: 12, position: 'absolute', right: 0, textAlign: 'right', top: 24 },
  assetNameRow: { alignItems: 'center', flex: 1, flexDirection: 'row', gap: 8, minHeight: 46, minWidth: 0, overflow: 'hidden' },
  assetBadge: { alignItems: 'center', borderRadius: 9, height: 28, justifyContent: 'center', width: 28 },
  assetBadgeText: { fontSize: 13, fontWeight: '900' },
  assetTextBlock: { flex: 1, flexBasis: 0, flexShrink: 1, maxWidth: '100%', minWidth: 0, overflow: 'hidden' },
  assetTitle: { flexShrink: 1, fontSize: 13, fontWeight: '800', lineHeight: 17, maxWidth: '100%', minWidth: 0, width: '100%' },
  rowDivider: { bottom: 0, height: StyleSheet.hairlineWidth, left: 36, position: 'absolute', right: 0 },
  transactionRow: { minHeight: 70, paddingRight: 112, paddingVertical: 12, position: 'relative' },
  transactionInfoCol: { minWidth: 0, overflow: 'hidden', paddingRight: 10 },
  transactionTitle: { fontSize: 13, fontWeight: '800', lineHeight: 17, maxWidth: '100%' },
  transactionMeta: { fontSize: 11, lineHeight: 15, marginTop: 3, maxWidth: '100%' },
  transactionAmountCol: { bottom: 0, justifyContent: 'center', position: 'absolute', right: 0, top: 0, width: 104 },
  transactionAmount: { fontSize: 13, fontWeight: '900', textAlign: 'right' },
  emptyState: { alignItems: 'center', gap: 7, paddingVertical: 18 },
  emptyTitle: { fontSize: 16, fontWeight: '900' },
  emptyDesc: { fontSize: 13, lineHeight: 18, textAlign: 'center' }
});
