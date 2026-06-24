import { Redirect, router, useLocalSearchParams } from 'expo-router';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import { BarChart3, Camera, ChevronDown, ChevronLeft, ChevronRight, ChevronUp, Edit3, PieChart, ReceiptText, Trash2, WalletCards, X } from 'lucide-react-native';
import DateTimePicker from 'react-native-dates-picker';
import type { DateType } from 'react-native-dates-picker';
import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Alert, KeyboardAvoidingView, Modal, PanResponder, Platform, Pressable, ScrollView, StyleSheet, Text as RNText, useWindowDimensions, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Svg, { Circle, G, Line, Polyline, Text as SvgText } from 'react-native-svg';
import Animated, { Easing as ReanimatedEasing, runOnJS, useAnimatedStyle, useSharedValue, withTiming } from 'react-native-reanimated';

import { Card, CardContent, Input, Separator, Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';
import { formatMoney } from '@/features/home';
import { useAuthStore } from '@/stores/authStore';
import type { LedgerAccount, LedgerCategory, LedgerTransaction, LedgerTransactionRequest, LedgerTransactionType } from '../api/ledgerTypes';
import { useLedger } from '../hooks/useLedger';

type LedgerViewMode = 'calendar' | 'stats';
type StatsMode = 'week' | 'month' | 'year';
type StatsDirection = 'EXPENSE' | 'INCOME';

const transactionTypes: Array<{ label: string; value: LedgerTransactionType }> = [
  { label: '支出', value: 'EXPENSE' },
  { label: '收入', value: 'INCOME' },
  { label: '转账', value: 'TRANSFER' }
];

const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
const emptyLedgerAccounts: LedgerAccount[] = [];
const emptyLedgerCategories: LedgerCategory[] = [];
dayjs.locale('zh-cn');

interface LedgerFormState {
  id?: string;
  type: LedgerTransactionType;
  amount: string;
  accountId: string;
  targetAccountId: string;
  categoryId: string;
  date: string;
  time: string;
  note: string;
}

export function LedgerScreen() {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const params = useLocalSearchParams<{ view?: LedgerViewMode; period?: StatsMode; date?: string }>();
  const { isHydrated, isLoggedIn, restoreToken } = useAuthStore();
  const [selectedDate, setSelectedDate] = useState(formatDate(new Date()));
  const [viewMode, setViewMode] = useState<LedgerViewMode>('stats');
  const [statsMode, setStatsMode] = useState<StatsMode>('month');
  const [statsDirection, setStatsDirection] = useState<StatsDirection>('EXPENSE');
  const [composerOpen, setComposerOpen] = useState(false);
  const [selectedTransaction, setSelectedTransaction] = useState<LedgerTransaction | null>(null);
  const [form, setForm] = useState<LedgerFormState>(() => createEmptyForm());
  const [formError, setFormError] = useState<string | null>(null);
  const [animatingMonth, setAnimatingMonth] = useState(false);
  const { width: screenWidth } = useWindowDimensions();
  const slideDistance = Math.max(280, screenWidth - 32);
  const slide = useSharedValue(0);
  const calendarAnimatedStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: slide.value }]
  }));

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  useEffect(() => {
    if (params.view === 'calendar' || params.view === 'stats') {
      setViewMode(params.view);
    }
    if (params.period === 'week' || params.period === 'month' || params.period === 'year') {
      setStatsMode(params.period);
    }
    if (typeof params.date === 'string' && isValidDateInput(params.date)) {
      setSelectedDate(params.date);
    }
  }, [params.date, params.period, params.view]);

  const statsRange = useMemo(() => getStatsRange(selectedDate, statsMode), [selectedDate, statsMode]);
  const {
    accountsQuery,
    expenseCategoriesQuery,
    incomeCategoriesQuery,
    transactionsQuery,
    monthTransactionsQuery,
    statsTransactionsQuery,
    createMutation,
    updateMutation,
    deleteMutation
  } = useLedger(selectedDate, isLoggedIn, statsRange);

  const accounts = accountsQuery.data ?? emptyLedgerAccounts;
  const categories = form.type === 'INCOME' ? incomeCategoriesQuery.data ?? emptyLedgerCategories : expenseCategoriesQuery.data ?? emptyLedgerCategories;
  const transactions = transactionsQuery.data?.records ?? transactionsQuery.data?.list ?? [];
  const monthTransactions = monthTransactionsQuery.data?.records ?? monthTransactionsQuery.data?.list ?? [];
  const statsTransactions = statsTransactionsQuery.data?.records ?? statsTransactionsQuery.data?.list ?? [];
  const isSubmitting = createMutation.isPending || updateMutation.isPending;
  const monthCells = useMemo(() => buildMonthCells(selectedDate, monthTransactions), [selectedDate, monthTransactions]);
  const daySummary = summarizeTransactions(transactions);
  const statsSummary = summarizeTransactions(statsTransactions);
  const directionShares = useMemo(() => buildCategoryShares(statsTransactions, statsDirection), [statsTransactions, statsDirection]);

  useEffect(() => {
    setForm((current) => ({
      ...current,
      accountId: current.accountId || String(accounts[0]?.id ?? ''),
      targetAccountId: current.targetAccountId || String(accounts.find((account) => String(account.id) !== current.accountId)?.id ?? '')
    }));
  }, [accounts]);

  useEffect(() => {
    setForm((current) => {
      if (current.type === 'TRANSFER') {
        return { ...current, categoryId: '' };
      }
      return {
        ...current,
        categoryId: current.categoryId || String(categories[0]?.id ?? '')
      };
    });
  }, [categories, form.type]);

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

  function changeMonth(offset: number) {
    setSelectedDate((current) => formatDate(addMonths(parseDate(current), offset)));
    setForm((current) => ({ ...current, id: undefined }));
    setFormError(null);
  }

  function finishSwitchMonth(direction: -1 | 1) {
    changeMonth(direction);
    slide.value = direction * slideDistance;
    slide.value = withTiming(0, { duration: 260, easing: ReanimatedEasing.out(ReanimatedEasing.cubic) }, () => runOnJS(setAnimatingMonth)(false));
  }

  function switchMonth(direction: -1 | 1) {
    if (animatingMonth) {
      return;
    }
    // 月份切换采用左右滑动：先滑出旧月份，再回弹展示新月份，和投资页日历保持一致。
    setAnimatingMonth(true);
    slide.value = withTiming(direction * -slideDistance, { duration: 220, easing: ReanimatedEasing.in(ReanimatedEasing.cubic) }, () => runOnJS(finishSwitchMonth)(direction));
  }

  const calendarPanResponder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => false,
        onStartShouldSetPanResponderCapture: () => false,
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
      }),
    [slideDistance]
  );

  function changeStatsPeriod(offset: number) {
    setSelectedDate((current) => {
      const date = parseDate(current);
      if (statsMode === 'week') {
        return formatDate(addDays(date, offset * 7));
      }
      if (statsMode === 'year') {
        return formatDate(addYears(date, offset));
      }
      return formatDate(addMonths(date, offset));
    });
  }

  function selectDate(date: string) {
    setSelectedDate(date);
    setForm((current) => ({ ...current, id: undefined }));
    setFormError(null);
  }

  function updateForm(patch: Partial<LedgerFormState>) {
    setForm((current) => ({ ...current, ...patch }));
    setFormError(null);
  }

  async function handleSubmit() {
    const payload = buildRequest(form);
    if ('error' in payload) {
      setFormError(payload.error);
      return;
    }

    try {
      if (form.id) {
        await updateMutation.mutateAsync({ id: form.id, data: payload.data });
      } else {
        await createMutation.mutateAsync(payload.data);
      }
      closeComposer();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '保存流水失败');
    }
  }

  function openComposer(item?: LedgerTransaction) {
    if (item) {
      setForm({
        id: String(item.id),
        type: normalizeType(item.type),
        amount: item.amount === null || item.amount === undefined ? '' : String(item.amount),
        accountId: String(item.accountId ?? ''),
        targetAccountId: String(item.targetAccountId ?? ''),
        categoryId: String(item.categoryId ?? ''),
        date: item.transactionTime?.slice(0, 10) || selectedDate,
        time: formatTimeInput(item.transactionTime),
        note: item.note ?? ''
      });
    } else {
      setForm(createEmptyForm(accounts, selectedDate));
    }
    setFormError(null);
    setComposerOpen(true);
  }

  function closeComposer() {
    setComposerOpen(false);
    setForm(createEmptyForm(accounts, selectedDate));
    setFormError(null);
  }

  function handleDelete(item: LedgerTransaction) {
    Alert.alert('删除流水', '删除后会回滚账户余额影响，确定删除吗？', [
      { text: '取消', style: 'cancel' },
      {
        text: '删除',
        style: 'destructive',
        onPress: async () => {
          try {
            await deleteMutation.mutateAsync(String(item.id));
          } catch (error) {
            setFormError(error instanceof Error ? error.message : '删除流水失败');
          }
        }
      }
    ]);
  }

  return (
    <SafeAreaView style={styles.page}>
      <GridBackdrop color={theme.border} />
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <View>
            <Text style={styles.title}>记账</Text>
            <Text variant="muted">日历流水与本周、本月、本年统计</Text>
          </View>
        </View>

        <View style={styles.controlBar}>
          <View style={styles.topSegmented}>
            <Pressable style={[styles.topSegmentButton, viewMode === 'calendar' ? styles.segmentActive : null]} onPress={() => setViewMode('calendar')}>
              <Text style={[styles.topSegmentText, viewMode === 'calendar' ? styles.segmentTextActive : null]}>日历</Text>
            </Pressable>
            <Pressable style={[styles.topSegmentButton, viewMode === 'stats' ? styles.segmentActive : null]} onPress={() => setViewMode('stats')}>
              <Text style={[styles.topSegmentText, viewMode === 'stats' ? styles.segmentTextActive : null]}>统计</Text>
            </Pressable>
          </View>
          <View style={styles.directionSegmented}>
            <Pressable style={[styles.directionButton, statsDirection === 'EXPENSE' ? styles.segmentActive : null]} onPress={() => setStatsDirection('EXPENSE')}>
              <Text style={[styles.topSegmentText, statsDirection === 'EXPENSE' ? styles.segmentTextActive : null]}>支出</Text>
            </Pressable>
            <Pressable style={[styles.directionButton, statsDirection === 'INCOME' ? styles.segmentActive : null]} onPress={() => setStatsDirection('INCOME')}>
              <Text style={[styles.topSegmentText, statsDirection === 'INCOME' ? styles.segmentTextActive : null]}>收入</Text>
            </Pressable>
          </View>
        </View>

        {viewMode === 'calendar' ? (
          <>
            <Card>
              <CardContent style={styles.calendarCard}>
                <MonthHeader selectedDate={selectedDate} onPrev={() => switchMonth(-1)} onNext={() => switchMonth(1)} />
                <View style={styles.calendarAnimated} {...calendarPanResponder.panHandlers}>
                  <Animated.View style={calendarAnimatedStyle}>
                    <View style={styles.weekHeader}>
                      {weekdays.map((day) => (
                        <Text key={day} variant="caption" style={styles.weekText}>
                          {day}
                        </Text>
                      ))}
                    </View>
                    <View style={styles.calendarGrid}>
                      {chunkMonthCells(monthCells).map((row, rowIndex) => (
                        <View key={`week-${rowIndex}`} style={styles.calendarWeekRow}>
                          {row.map((cell) => (
                            <DayCell key={cell.date} cell={cell} selected={cell.date === selectedDate} onPress={() => selectDate(cell.date)} />
                          ))}
                        </View>
                      ))}
                    </View>
                  </Animated.View>
                </View>
              </CardContent>
            </Card>

            <Card>
              <CardContent style={styles.dayPanel}>
                <View style={styles.sectionHeader}>
                  <View>
                    <Text style={styles.sectionTitle}>{formatReadableDate(selectedDate)}</Text>
                    <Text variant="caption">收入 {formatMoney(daySummary.income)} · 支出 {formatMoney(daySummary.expense)}</Text>
                  </View>
                  <Text
                    style={[
                      styles.dayBalance,
                      // 右上角结余跟随正负变色，正数绿色、负数红色，方便快速扫一眼当天结果。
                      daySummary.income - daySummary.expense >= 0 ? styles.positiveAmount : styles.negativeAmount
                    ]}
                  >
                    {formatMoney(daySummary.income - daySummary.expense)}
                  </Text>
                </View>
                {transactionsQuery.isLoading ? <ActivityIndicator color={theme.primary} /> : null}
                {transactionsQuery.isError ? <Text variant="error">流水加载失败，请稍后重试。</Text> : null}
                {transactions.length > 0 ? (
                  transactions.map((item, index) => (
                    <View key={String(item.id)}>
                      <TransactionItem
                        item={item}
                        onEdit={openComposer}
                        onDelete={handleDelete}
                        // 日历下方流水行点击直接打开详情，方便从首页跳转过来后继续查看。
                        onSelectDetail={setSelectedTransaction}
                      />
                      {index < transactions.length - 1 ? <Separator /> : null}
                    </View>
                  ))
                ) : (
                  <Text variant="muted">这一天还没有流水。</Text>
                )}
              </CardContent>
            </Card>
          </>
        ) : (
          <StatsPanel
            selectedDate={selectedDate}
            statsMode={statsMode}
            statsDirection={statsDirection}
            transactions={statsTransactions}
            statsSummary={statsSummary}
            directionShares={directionShares}
            onModeChange={setStatsMode}
            onPrev={() => changeStatsPeriod(-1)}
            onNext={() => changeStatsPeriod(1)}
          />
        )}
      </ScrollView>

      <ComposerSheet
        visible={composerOpen}
        form={form}
        formError={formError}
        accounts={accounts}
        categories={categories}
        isSubmitting={isSubmitting}
        onClose={closeComposer}
        onChange={updateForm}
        onSubmit={handleSubmit}
      />

      <TransactionDetailModal
        transaction={selectedTransaction}
        onClose={() => setSelectedTransaction(null)}
      />
    </SafeAreaView>
  );
}

function MonthHeader({ selectedDate, onPrev, onNext }: { selectedDate: string; onPrev: () => void; onNext: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <View style={styles.monthHeader}>
      <Text style={styles.monthTitle}>{formatMonthLabel(selectedDate)}</Text>
      <View style={styles.monthActions}>
        <Pressable style={styles.iconButton} onPress={onPrev}>
          <ChevronLeft color={theme.foreground} size={18} />
        </Pressable>
        <Pressable style={styles.iconButton} onPress={onNext}>
          <ChevronRight color={theme.foreground} size={18} />
        </Pressable>
      </View>
    </View>
  );
}

function DayCell({ cell, selected, onPress }: { cell: MonthCell; selected: boolean; onPress: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Pressable style={[styles.dayCell, selected ? styles.dayCellSelected : null]} onPress={onPress}>
      <Text style={[styles.dayNumber, !cell.inMonth ? styles.mutedDay : null, selected ? styles.dayNumberSelected : null]}>
        {parseDate(cell.date).getDate()}
      </Text>
      {cell.income > 0 ? <Text style={styles.incomeTiny} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.45}>+{formatCalendarAmount(cell.income)}</Text> : null}
      {cell.expense > 0 ? <Text style={styles.expenseTiny} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.45}>-{formatCalendarAmount(cell.expense)}</Text> : null}
    </Pressable>
  );
}

function StatsPanel({
  selectedDate,
  statsMode,
  statsDirection,
  transactions,
  statsSummary,
  directionShares,
  onModeChange,
  onPrev,
  onNext
}: {
  selectedDate: string;
  statsMode: StatsMode;
  statsDirection: StatsDirection;
  transactions: LedgerTransaction[];
  statsSummary: { income: number; expense: number };
  directionShares: CategoryShare[];
  onModeChange: (mode: StatsMode) => void;
  onPrev: () => void;
  onNext: () => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const trendPoints = useMemo(() => buildTrendPoints(selectedDate, statsMode, transactions), [selectedDate, statsMode, transactions]);
  const directionTotal = statsDirection === 'EXPENSE' ? statsSummary.expense : statsSummary.income;
  const periodCount = countDirectionTransactions(transactions, statsDirection);
  const averageAmount = periodCount > 0 ? directionTotal / periodCount : 0;
  const [selectedCategoryShare, setSelectedCategoryShare] = useState<CategoryShare | null>(null);
  const [selectedDetailTransaction, setSelectedDetailTransaction] = useState<LedgerTransaction | null>(null);
  const [allDetailsOpen, setAllDetailsOpen] = useState(false);
  const selectedCategoryTransactions = useMemo(
    () => selectedCategoryShare
      ? transactions.filter((item) => item.type === statsDirection && sameCategory(item, selectedCategoryShare))
      : [],
    [selectedCategoryShare, statsDirection, transactions]
  );
  const directionTransactions = useMemo(() => {
    const filtered = transactions.filter((item) => item.type === statsDirection);
    return filtered.sort((a, b) => (b.amount ?? 0) - (a.amount ?? 0));
  }, [statsDirection, transactions]);

  return (
    <>
      <View style={styles.periodSegmented}>
        {(['week', 'month', 'year'] as const).map((mode) => (
          <Pressable key={mode} style={[styles.periodButton, statsMode === mode ? styles.segmentActive : null]} onPress={() => onModeChange(mode)}>
            <Text style={[styles.periodText, statsMode === mode ? styles.segmentTextActive : null]}>{statsModeLabel(mode)}</Text>
          </Pressable>
        ))}
      </View>

      <View style={styles.periodHeader}>
        <Pressable style={styles.roundArrow} onPress={onPrev}>
          <ChevronLeft color={theme.foreground} size={21} />
        </Pressable>
        <Text style={styles.monthTitle}>{formatStatsRangeLabel(selectedDate, statsMode)}</Text>
        <Pressable style={styles.roundArrow} onPress={onNext}>
          <ChevronRight color={theme.foreground} size={21} />
        </Pressable>
      </View>

      <Card>
        <CardContent style={styles.balanceCard}>
          <StatsCardTitle icon={WalletCards} title={`${statsModeLabel(statsMode)}结余`} />
          <Text style={styles.balanceAmount} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>
            {formatStatsMoney(statsSummary.income - statsSummary.expense)}
          </Text>
          <ProgressLine label={`${statsModeLabel(statsMode)}支出`} value={statsSummary.expense} max={Math.max(statsSummary.income, statsSummary.expense, 1)} color={theme.foreground} />
          <ProgressLine label={`${statsModeLabel(statsMode)}收入`} value={statsSummary.income} max={Math.max(statsSummary.income, statsSummary.expense, 1)} color={theme.success} />
        </CardContent>
      </Card>

      <Card>
        <CardContent style={styles.chartCard}>
          <StatsCardTitle icon={BarChart3} title={`${statsModeLabel(statsMode)}收支趋势`} />
          <View style={styles.trendMetaRow}>
            <SummaryBlock label={`${statsModeLabel(statsMode)}平均${directionLabel(statsDirection)}`} value={formatStatsMoney(averageAmount)} />
            <View style={styles.metaDivider} />
            <SummaryBlock label={`${statsModeLabel(statsMode)}累计${directionLabel(statsDirection)}笔数`} value={`${periodCount}笔`} />
          </View>
          <TrendLineChart points={trendPoints} direction={statsDirection} />
        </CardContent>
      </Card>

      <CategoryShareCard title={`${directionLabel(statsDirection)}分类占比`} shares={directionShares} total={directionTotal} />

      <Card>
        <CardContent style={styles.rankingContent}>
          <StatsCardTitle icon={PieChart} title={`${directionLabel(statsDirection)}分类排行`} />
          {directionShares.slice(0, 5).map((share) => (
            <Pressable key={share.key} style={styles.rankingRow} onPress={() => setSelectedCategoryShare(share)}>
              <View style={styles.rankingIcon}>
                <Text style={styles.categoryIconText}>{share.name.slice(0, 1)}</Text>
              </View>
              <View style={styles.rankingInfo}>
                <View style={styles.rankingTitleRow}>
                  <Text style={styles.rankingName} numberOfLines={1}>{share.name}</Text>
                  <Text variant="caption">{formatPercentExact(share.amount / directionTotal)}</Text>
                </View>
                <View style={styles.rankingTrack}>
                  <View style={[styles.rankingFill, { width: `${directionTotal > 0 ? (share.amount / directionTotal) * 100 : 0}%`, backgroundColor: theme.foreground }]} />
                </View>
                <Text variant="caption">{statsModeLabel(statsMode)}{directionLabel(statsDirection)}：{formatStatsMoney(share.amount)}</Text>
              </View>
            </Pressable>
          ))}
          {directionShares.length === 0 ? <Text variant="muted">当前范围没有数据。</Text> : null}
        </CardContent>
      </Card>

      <Card>
        <CardContent style={styles.detailCard}>
          <View style={styles.sectionHeader}>
            <StatsCardTitle icon={ReceiptText} title={`${directionLabel(statsDirection)}明细排行`} />
            <Pressable style={styles.linkButton} onPress={() => setAllDetailsOpen(true)}>
              <Text style={styles.linkText}>更多</Text>
              <ChevronRight color={theme.mutedForeground} size={17} />
            </Pressable>
          </View>
          {directionTransactions.slice(0, 5).map((item, index) => (
            <View key={String(item.id)}>
              <Pressable style={styles.detailRow} onPress={() => setSelectedDetailTransaction(item)}>
                <View style={styles.categoryIcon}>
                  <Text style={styles.categoryIconText}>{categoryInitial(item)}</Text>
                </View>
                <View style={styles.detailInfo}>
                  <Text style={styles.detailTitle} numberOfLines={1}>
                    {item.categoryName || directionLabel(statsDirection)}{item.note ? ` · ${item.note}` : ''}
                  </Text>
                  <Text variant="muted">{formatShortDateTime(item.transactionTime)}</Text>
                </View>
                <Text
                  style={[
                    styles.detailAmount,
                    // 支出红色、收入绿色，和金额方向保持一致，列表扫一眼更直观。
                    transactionAmountColor(item, theme)
                  ]}
                >
                  {formatSignedAmount(item)}
                </Text>
              </Pressable>
              {index < Math.min(directionTransactions.length, 5) - 1 ? <Separator /> : null}
            </View>
          ))}
          {directionTransactions.length === 0 ? <Text variant="muted">当前范围没有明细。</Text> : null}
        </CardContent>
      </Card>

      <CategoryTransactionsModal
        share={selectedCategoryShare}
        transactions={selectedCategoryTransactions}
        onSelectTransaction={setSelectedDetailTransaction}
        onClose={() => setSelectedCategoryShare(null)}
      />
      <DirectionTransactionsModal
        visible={allDetailsOpen}
        title={`${directionLabel(statsDirection)}明细`}
        transactions={directionTransactions}
        onSelectTransaction={setSelectedDetailTransaction}
        onClose={() => setAllDetailsOpen(false)}
      />
      <TransactionDetailModal
        transaction={selectedDetailTransaction}
        onClose={() => setSelectedDetailTransaction(null)}
      />
    </>
  );
}

function ProgressLine({ label, value, max, color }: { label: string; value: number; max: number; color: string }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <View style={styles.progressRow}>
      <Text style={styles.progressLabel}>{label}</Text>
      <View style={styles.progressTrack}>
        <View style={[styles.progressFill, { width: `${Math.min((value / max) * 100, 100)}%`, backgroundColor: color }]} />
      </View>
      <Text style={styles.progressValue}>{formatStatsMoney(value)}</Text>
    </View>
  );
}

function TrendLineChart({ points, direction }: { points: TrendPoint[]; direction: StatsDirection }) {
  const theme = useTheme();
  const height = 210;
  const left = 48;
  const right = 12;
  const top = 18;
  const bottom = 34;
  const [chartWidth, setChartWidth] = useState(0);
  const [activeIndex, setActiveIndex] = useState<number | null>(null);
  const resolvedWidth = chartWidth > 0 ? chartWidth : 320;
  const chartInnerWidth = Math.max(resolvedWidth - left - right, 1);
  const chartHeight = height - top - bottom;
  const field = direction === 'EXPENSE' ? 'expense' : 'income';
  const maxAmount = Math.max(...points.map((point) => point[field]), 1);
  const line = buildPolyline(points, field, maxAmount, left, top, chartInnerWidth, chartHeight);
  const axisValues = [maxAmount, maxAmount / 2, 0];
  const labelIndexes = getAxisLabelIndexes(points.length);
  const activePoint = activeIndex === null ? null : points[activeIndex] ?? null;
  const activeDot = activePoint
    ? getTrendPointPosition(points, activePoint, field, maxAmount, left, top, chartInnerWidth, chartHeight)
    : null;

  const responder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => true,
        onMoveShouldSetPanResponder: () => true,
        onPanResponderGrant: (event) => {
          setActiveIndex(getNearestTrendPointIndex(event.nativeEvent.locationX, points.length, resolvedWidth, left, right));
        },
        onPanResponderMove: (event) => {
          setActiveIndex(getNearestTrendPointIndex(event.nativeEvent.locationX, points.length, resolvedWidth, left, right));
        },
        onPanResponderRelease: () => {
          setActiveIndex(null);
        },
        onPanResponderTerminate: () => {
          setActiveIndex(null);
        }
      }),
    [points.length, resolvedWidth]
  );

  return (
    <View style={stylesStatic.chartWrap} onLayout={(event) => setChartWidth(event.nativeEvent.layout.width)}>
      <View style={stylesStatic.chartTouchArea} {...responder.panHandlers} />
      <Svg width={resolvedWidth} height={height} viewBox={`0 0 ${resolvedWidth} ${height}`}>
        {axisValues.map((value, index) => {
          const y = top + (index / 2) * chartHeight;
          return (
            <G key={`axis-${index}`}>
              <Line x1={left} y1={y} x2={resolvedWidth - right} y2={y} stroke={theme.border} strokeWidth={1} strokeDasharray="4 6" />
              <SvgText x={0} y={y + 4} fontSize={10} fill={theme.mutedForeground}>
                {formatAxisMoney(value)}
              </SvgText>
            </G>
          );
        })}
        <Polyline points={line} fill="none" stroke={theme.foreground} strokeWidth={2.5} strokeLinejoin="round" strokeLinecap="round" />
        {points.map((point, index) => {
          const x = left + (points.length === 1 ? 0 : (index / (points.length - 1)) * chartInnerWidth);
          const y = top + chartHeight - (point[field] / maxAmount) * chartHeight;
          return <Circle key={`${point.label}-dot`} cx={x} cy={y} r={3} fill={theme.foreground} />;
        })}
        {points.map((point, index) => {
          if (!labelIndexes.includes(index)) {
            return null;
          }
          const x = left + (points.length === 1 ? 0 : (index / (points.length - 1)) * chartInnerWidth);
          return (
            <SvgText key={point.label} x={x - 10} y={height - 8} fontSize={10} fill={theme.mutedForeground}>
              {point.label}
            </SvgText>
          );
        })}
        {activeDot ? (
          <>
            <Line x1={activeDot.x} y1={top} x2={activeDot.x} y2={height - bottom} stroke={theme.foreground} strokeWidth={1} strokeDasharray="3 4" opacity={0.55} />
            <Circle cx={activeDot.x} cy={activeDot.y} r={5} fill={theme.background} stroke={theme.foreground} strokeWidth={2} />
          </>
        ) : null}
      </Svg>
      {activePoint && activeDot ? (
        <View
          style={[
            stylesStatic.chartTooltip,
            // 参考投资页的浮层：固定在图表上方，白底黑字，避免挡住折线主体。
            { left: clampTooltipLeft(activeDot.x, resolvedWidth), top: 8 }
          ]}
        >
          <Text style={stylesStatic.chartTooltipDate}>{formatTrendPointDate(activePoint.date)}</Text>
          <Text style={stylesStatic.chartTooltipAmount}>
            {formatStatsPlainAmount(activePoint[field])}
          </Text>
        </View>
      ) : null}
    </View>
  );
}

function CategoryShareCard({ title, shares, total }: { title: string; shares: CategoryShare[]; total: number }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Card>
      <CardContent style={styles.shareCard}>
        <StatsCardTitle icon={PieChart} title={title} />
        {shares.length > 0 ? (
          <View style={styles.shareContent}>
            <DonutChart shares={shares} total={total} />
            <View style={styles.shareLegend}>
              {shares.slice(0, 6).map((share) => (
                <View key={share.name} style={styles.shareLegendRow}>
                  <View style={[styles.shareDot, { backgroundColor: share.color }]} />
                  <Text variant="caption" style={styles.shareName} numberOfLines={1}>{share.name}</Text>
                  <Text variant="caption">{formatPercentExact(share.amount / total)}</Text>
                </View>
              ))}
            </View>
          </View>
        ) : (
          <Text variant="muted">当前范围没有数据。</Text>
        )}
      </CardContent>
    </Card>
  );
}

function CategoryTransactionsModal({
  share,
  transactions,
  onSelectTransaction,
  onClose
}: {
  share: CategoryShare | null;
  transactions: LedgerTransaction[];
  onSelectTransaction: (item: LedgerTransaction) => void;
  onClose: () => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Modal visible={Boolean(share)} animationType="slide" transparent onRequestClose={onClose}>
      <View style={styles.modalRoot}>
        <Pressable style={styles.modalBackdrop} onPress={onClose} />
        <View style={styles.sheet}>
          <ScrollView contentContainerStyle={styles.sheetContent} showsVerticalScrollIndicator={false}>
            <View style={styles.sheetHeader}>
              <View>
                <Text style={styles.sheetTitle}>{share?.name || '分类明细'}</Text>
                <Text variant="muted">合计 {formatStatsMoney(share?.amount ?? 0)} · {transactions.length} 笔</Text>
              </View>
              <Pressable onPress={onClose}>
                <X color={theme.foreground} size={24} />
              </Pressable>
            </View>
            {transactions.map((item, index) => (
              <View key={String(item.id)}>
                <Pressable
                  style={styles.detailRow}
                  onPress={() => {
                    onSelectTransaction(item);
                    onClose();
                  }}
                >
                  <View style={styles.categoryIcon}>
                    <Text style={styles.categoryIconText}>{categoryInitial(item)}</Text>
                  </View>
                  <View style={styles.detailInfo}>
                    <Text style={styles.detailTitle} numberOfLines={1}>{item.note || item.categoryName || transactionTypeLabel(item.type)}</Text>
                    <Text variant="muted">{formatShortDateTime(item.transactionTime)} · {item.accountName || '--'}</Text>
                  </View>
                  <Text
                    style={[
                      styles.detailAmount,
                      // 弹层列表同样按方向着色，避免支出和收入混在一起时看不清。
                      transactionAmountColor(item, theme)
                    ]}
                  >
                    {formatSignedAmount(item)}
                  </Text>
                </Pressable>
                {index < transactions.length - 1 ? <Separator /> : null}
              </View>
            ))}
            {transactions.length === 0 ? <Text variant="muted">当前分类没有明细。</Text> : null}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

function DirectionTransactionsModal({
  visible,
  title,
  transactions,
  onSelectTransaction,
  onClose
}: {
  visible: boolean;
  title: string;
  transactions: LedgerTransaction[];
  onSelectTransaction: (item: LedgerTransaction) => void;
  onClose: () => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const total = transactions.reduce((sum, item) => sum + Math.abs(item.amount ?? 0), 0);

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <View style={styles.modalRoot}>
        <Pressable style={styles.modalBackdrop} onPress={onClose} />
        <View style={styles.sheet}>
          <ScrollView contentContainerStyle={styles.sheetContent} showsVerticalScrollIndicator={false}>
            <View style={styles.sheetHeader}>
              <View>
                <Text style={styles.sheetTitle}>{title}</Text>
                <Text variant="muted">合计 {formatStatsMoney(total)} · {transactions.length} 笔</Text>
              </View>
              <Pressable onPress={onClose}>
                <X color={theme.foreground} size={24} />
              </Pressable>
            </View>
            {transactions.map((item, index) => (
              <View key={String(item.id)}>
                <Pressable
                  style={styles.detailRow}
                  onPress={() => {
                    onSelectTransaction(item);
                    onClose();
                  }}
                >
                  <View style={styles.categoryIcon}>
                    <Text style={styles.categoryIconText}>{categoryInitial(item)}</Text>
                  </View>
                  <View style={styles.detailInfo}>
                    <Text style={styles.detailTitle} numberOfLines={1}>{item.note || item.categoryName || transactionTypeLabel(item.type)}</Text>
                    <Text variant="muted">{formatShortDateTime(item.transactionTime)} · {item.accountName || '--'}</Text>
                  </View>
                  <Text
                    style={[
                      styles.detailAmount,
                      // 同上：明细列表金额按收支颜色区分。
                      transactionAmountColor(item, theme)
                    ]}
                  >
                    {formatSignedAmount(item)}
                  </Text>
                </Pressable>
                {index < transactions.length - 1 ? <Separator /> : null}
              </View>
            ))}
            {transactions.length === 0 ? <Text variant="muted">当前范围没有明细。</Text> : null}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

function TransactionDetailModal({ transaction, onClose }: { transaction: LedgerTransaction | null; onClose: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Modal visible={Boolean(transaction)} animationType="slide" transparent onRequestClose={onClose}>
      <View style={styles.modalRoot}>
        <Pressable style={styles.modalBackdrop} onPress={onClose} />
        <View style={styles.sheet}>
          <ScrollView contentContainerStyle={styles.sheetContent} showsVerticalScrollIndicator={false}>
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>明细详情</Text>
              <Pressable onPress={onClose}>
                <X color={theme.foreground} size={24} />
              </Pressable>
            </View>
            {transaction ? (
              <View style={styles.detailMetricGrid}>
                {chunkDetailMetrics([
                  { label: '金额', value: formatSignedAmount(transaction) },
                  { label: '类型', value: transactionTypeLabel(transaction.type) },
                  { label: '分类', value: transaction.categoryName || '--' },
                  { label: '账户', value: transaction.accountName || '--' },
                  transaction.targetAccountName ? { label: '转入账户', value: transaction.targetAccountName } : null,
                  { label: '时间', value: transaction.transactionTime?.replace('T', ' ').slice(0, 16) || '--' },
                  { label: '备注', value: transaction.note || '--' }
                ]).map((row, rowIndex) => (
                  <View key={`detail-row-${rowIndex}`} style={styles.detailMetricRow}>
                    {row.map((item, index) => (
                      item ? (
                        <View key={item.label} style={[styles.detailMetricCell, index === 0 ? styles.detailMetricCellLeft : styles.detailMetricCellRight]}>
                          <Text style={styles.detailMetricLabel}>{item.label}</Text>
                          <Text variant="muted" style={styles.detailMetricValue} numberOfLines={2}>
                            {item.value}
                          </Text>
                        </View>
                      ) : (
                        <View key={`empty-${rowIndex}-${index}`} style={[styles.detailMetricCell, index === 0 ? styles.detailMetricCellLeft : styles.detailMetricCellRight, styles.detailMetricEmpty]} />
                      )
                    ))}
                  </View>
                ))}
              </View>
            ) : null}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

function DonutChart({ shares, total, size = 116 }: { shares: CategoryShare[]; total: number; size?: number }) {
  const theme = useTheme();
  const radius = size / 2 - 16;
  const circumference = 2 * Math.PI * radius;
  let offset = 0;

  return (
    <Svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      <Circle cx={size / 2} cy={size / 2} r={radius} stroke={theme.secondary} strokeWidth={18} fill="none" />
      {shares.map((share) => {
        const length = total > 0 ? (share.amount / total) * circumference : 0;
        const segment = (
          <Circle
            key={share.name}
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke={share.color}
            strokeWidth={18}
            fill="none"
            strokeDasharray={`${length} ${circumference - length}`}
            strokeDashoffset={-offset}
            strokeLinecap="butt"
            transform={`rotate(-90 ${size / 2} ${size / 2})`}
          />
        );
        offset += length;
        return segment;
      })}
      <SvgText x={size / 2} y={size / 2 + 4} fontSize={12} fontWeight="700" fill={theme.foreground} textAnchor="middle">
        {formatStatsPlainAmount(total)}
      </SvgText>
    </Svg>
  );
}


function StatsCardTitle({ icon: Icon, title }: { icon: typeof BarChart3; title: string }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <View style={styles.statsTitleRow}>
      <View style={styles.statsIconBubble}>
        <Icon color={theme.foreground} size={19} strokeWidth={2.3} />
      </View>
      <Text style={styles.sectionTitle}>{title}</Text>
    </View>
  );
}

function SummaryBlock({ label, value, tone }: { label: string; value: string; tone?: 'income' | 'expense' }) {
  const theme = useTheme();
  const color = tone === 'income' ? theme.success : tone === 'expense' ? theme.destructive : theme.foreground;

  return (
    <View style={stylesStatic.summaryBlock}>
      <Text variant="muted">{label}</Text>
      <Text style={[stylesStatic.summaryValue, { color }]} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.76}>
        {value}
      </Text>
    </View>
  );
}

function TransactionItem({
  item,
  onEdit,
  onDelete,
  onSelectDetail
}: {
  item: LedgerTransaction;
  onEdit: (item: LedgerTransaction) => void;
  onDelete: (item: LedgerTransaction) => void;
  onSelectDetail: (item: LedgerTransaction) => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Pressable
      style={styles.transactionItem}
      onPress={() => onSelectDetail(item)}
    >
      <View style={[styles.categoryIcon, { backgroundColor: pickCategoryColor(item) }]}>
        <Text style={styles.categoryIconText}>{categoryInitial(item)}</Text>
      </View>
      <View style={styles.transactionInfo}>
        <Text style={styles.transactionTitle}>{item.categoryName || transactionTypeLabel(item.type)}</Text>
        <Text variant="caption">
          {/* 类型后面补上备注，列表里能直接看出这笔流水的补充说明。 */}
          {transactionTypeLabel(item.type)}
          {item.note ? ` · ${item.note}` : ''}
          {` · ${formatTimeInput(item.transactionTime)}`}
        </Text>
      </View>
      <View style={styles.transactionRight}>
        <Text
          style={[
            styles.transactionAmount,
            // 流水金额按收入/支出着色，和上方结余保持一致。
            transactionAmountColor(item, theme)
          ]}
          numberOfLines={1}
          adjustsFontSizeToFit
          minimumFontScale={0.75}
        >
          {formatSignedAmount(item)}
        </Text>
        <View style={styles.transactionActions}>
          <Pressable style={styles.smallIconButton} onPress={() => onEdit(item)}>
            <Edit3 color={theme.foreground} size={15} />
          </Pressable>
          <Pressable style={styles.smallIconButton} onPress={() => onDelete(item)}>
            <Trash2 color={theme.destructive} size={15} />
          </Pressable>
        </View>
      </View>
    </Pressable>
  );
}

function ComposerSheet({
  visible,
  form,
  formError,
  accounts,
  categories,
  isSubmitting,
  onClose,
  onChange,
  onSubmit
}: {
  visible: boolean;
  form: LedgerFormState;
  formError: string | null;
  accounts: LedgerAccount[];
  categories: LedgerCategory[];
  isSubmitting: boolean;
  onClose: () => void;
  onChange: (patch: Partial<LedgerFormState>) => void;
  onSubmit: () => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.modalRoot}>
        <Pressable style={styles.modalBackdrop} onPress={onClose} />
        <View style={[styles.sheet, styles.composerSheet]}>
          <ScrollView
            style={styles.sheetScroller}
            showsVerticalScrollIndicator={false}
            keyboardShouldPersistTaps="handled"
            contentContainerStyle={styles.sheetContent}
          >
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>{form.id ? '编辑记录' : '记一笔'}</Text>
              <View style={styles.sheetHeaderActions}>
                <Pressable onPress={onClose}>
                  <X color={theme.foreground} size={24} />
                </Pressable>
              </View>
            </View>
            <LedgerComposerFields
                form={form}
                accounts={accounts}
                categories={categories}
                onChange={onChange}
                submitLabel={isSubmitting ? '保存中' : '保存'}
                submitLoading={isSubmitting}
                onSubmit={onSubmit}
            />
            {formError ? <Text variant="error" style={styles.submitError}>{formError}</Text> : null}
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

function LedgerComposerFields({
  form,
  accounts,
  categories,
  onChange,
  submitLabel,
  submitLoading,
  onSubmit
}: {
  form: LedgerFormState;
  accounts: LedgerAccount[];
  categories: LedgerCategory[];
  onChange: (patch: Partial<LedgerFormState>) => void;
  submitLabel?: string;
  submitLoading?: boolean;
  onSubmit?: () => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const [openSelect, setOpenSelect] = useState<'category' | 'account' | 'targetAccount' | null>(null);
  const [openPicker, setOpenPicker] = useState<'datetime' | null>(null);
  const [submitPressed, setSubmitPressed] = useState(false);
  const accountOptions = useMemo(
    () => accounts.map((account) => ({ id: String(account.id), label: account.name || '未命名账户' })),
    [accounts]
  );
  const categoryOptions = useMemo(
    () => categories.map((category) => ({ id: String(category.id), label: category.name || '未命名分类' })),
    [categories]
  );
  const targetAccountOptions = useMemo(
    () => accounts
      .filter((account) => String(account.id) !== form.accountId)
      .map((account) => ({ id: String(account.id), label: account.name || '未命名账户' })),
    [accounts, form.accountId]
  );

  function toggleSelect(key: 'category' | 'account' | 'targetAccount') {
    setOpenPicker(null);
    setOpenSelect((current) => (current === key ? null : key));
  }

  function togglePicker() {
    setOpenSelect(null);
    setOpenPicker((current) => (current === 'datetime' ? null : 'datetime'));
  }

  return (
    <>
      <View style={styles.segmented}>
        {transactionTypes.map((item) => (
          <Pressable
            key={item.value}
            style={[styles.segmentButton, form.type === item.value ? styles.segmentActive : null]}
            onPress={() => {
              setOpenSelect(null);
              setOpenPicker(null);
              onChange({ type: item.value, categoryId: '', targetAccountId: '' });
            }}
          >
            <Text style={[styles.segmentText, form.type === item.value ? styles.segmentTextActive : null]}>{item.label}</Text>
          </Pressable>
        ))}
      </View>
      <View style={styles.amountRow}>
        <Input
          keyboardType="decimal-pad"
          placeholder="¥ 0.00"
          value={form.amount}
          onChangeText={(amount) => onChange({ amount })}
          inputStyle={styles.amountInput}
          containerStyle={styles.amountInputWrap}
        />
        <View style={styles.cameraBox}>
          <Camera color={theme.foreground} size={22} />
          <Text variant="caption">拍照</Text>
        </View>
      </View>
      {form.type === 'TRANSFER' ? (
        <SheetField label="分类" value="转账" />
      ) : (
        <SelectField
          label="分类"
          value={selectedName(categories, form.categoryId) || '请选择分类'}
          open={openSelect === 'category'}
          options={categoryOptions}
          selectedId={form.categoryId}
          emptyText="暂无可用分类"
          onToggle={() => toggleSelect('category')}
          onSelect={(categoryId) => {
            onChange({ categoryId });
            setOpenSelect(null);
          }}
        />
      )}
      <SelectField
        label="账户"
        value={selectedName(accounts, form.accountId) || '请选择账户'}
        open={openSelect === 'account'}
        options={accountOptions}
        selectedId={form.accountId}
        emptyText="暂无可用账户"
        onToggle={() => toggleSelect('account')}
        onSelect={(accountId) => {
          onChange({ accountId });
          setOpenSelect(null);
        }}
      />
      {form.type === 'TRANSFER' ? (
        <SelectField
          label="转入账户"
          value={selectedName(accounts, form.targetAccountId) || '请选择账户'}
          open={openSelect === 'targetAccount'}
          options={targetAccountOptions}
          selectedId={form.targetAccountId}
          emptyText="暂无可用转入账户"
          onToggle={() => toggleSelect('targetAccount')}
          onSelect={(targetAccountId) => {
            onChange({ targetAccountId });
            setOpenSelect(null);
          }}
        />
      ) : null}
      <DateTimeField
        date={form.date}
        time={form.time}
        open={openPicker === 'datetime'}
        onToggle={togglePicker}
        onChange={onChange}
      />
      <View style={styles.noteBlock}>
        <Input label="备注" placeholder="可选" value={form.note} onChangeText={(note) => onChange({ note })} />
      </View>
        {onSubmit ? (
          <View style={styles.submitActionBlock}>
            <Pressable
              accessibilityRole="button"
              disabled={submitLoading}
              style={[
                styles.submitActionButton,
                submitLoading ? styles.submitActionButtonDisabled : null,
                submitPressed && !submitLoading ? styles.submitActionButtonPressed : null
              ]}
              onPressIn={() => setSubmitPressed(true)}
              onPressOut={() => setSubmitPressed(false)}
              onPress={onSubmit}
            >
              {submitLoading ? <ActivityIndicator color="#ffffff" /> : null}
              <RNText style={styles.submitActionText}>{submitLabel || '保存'}</RNText>
            </Pressable>
        </View>
      ) : null}
    </>
  );
}

export function LedgerQuickComposer({ visible, onClose }: { visible: boolean; onClose: () => void }) {
  const { isLoggedIn } = useAuthStore();
  const selectedDate = useMemo(() => formatDate(new Date()), [visible]);
  const [form, setForm] = useState<LedgerFormState>(() => createEmptyForm());
  const [formError, setFormError] = useState<string | null>(null);
  const {
    accountsQuery,
    expenseCategoriesQuery,
    incomeCategoriesQuery,
    createMutation
  } = useLedger(selectedDate, visible && isLoggedIn, getStatsRange(selectedDate, 'month'));
  const accounts = accountsQuery.data ?? emptyLedgerAccounts;
  const categories = form.type === 'INCOME' ? incomeCategoriesQuery.data ?? emptyLedgerCategories : expenseCategoriesQuery.data ?? emptyLedgerCategories;

  useEffect(() => {
    if (visible) {
      setForm(createEmptyForm(accounts, selectedDate));
      setFormError(null);
    }
  }, [accounts, selectedDate, visible]);

  useEffect(() => {
    setForm((current) => ({
      ...current,
      accountId: current.accountId || String(accounts[0]?.id ?? ''),
      targetAccountId: current.targetAccountId || String(accounts.find((account) => String(account.id) !== current.accountId)?.id ?? '')
    }));
  }, [accounts]);

  useEffect(() => {
    setForm((current) => {
      if (current.type === 'TRANSFER') {
        return { ...current, categoryId: '' };
      }
      return {
        ...current,
        categoryId: current.categoryId || String(categories[0]?.id ?? '')
      };
    });
  }, [categories, form.type]);

  function updateForm(patch: Partial<LedgerFormState>) {
    setForm((current) => ({ ...current, ...patch }));
    setFormError(null);
  }

  async function handleSubmit() {
    const payload = buildRequest(form);
    if ('error' in payload) {
      setFormError(payload.error);
      return;
    }
    try {
      await createMutation.mutateAsync(payload.data);
      onClose();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '保存流水失败');
    }
  }

  return (
    <ComposerSheet
      visible={visible}
      form={form}
      formError={formError}
      accounts={accounts}
      categories={categories}
      isSubmitting={createMutation.isPending}
      onClose={onClose}
      onChange={updateForm}
      onSubmit={handleSubmit}
    />
  );
}

export function LedgerComposePage() {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const { isHydrated, isLoggedIn, restoreToken } = useAuthStore();
  const selectedDate = useMemo(() => formatDate(new Date()), []);
  const [form, setForm] = useState<LedgerFormState>(() => createEmptyForm());
  const [formError, setFormError] = useState<string | null>(null);
  const {
    accountsQuery,
    expenseCategoriesQuery,
    incomeCategoriesQuery,
    createMutation
  } = useLedger(selectedDate, isLoggedIn, getStatsRange(selectedDate, 'month'));
  const accounts = accountsQuery.data ?? emptyLedgerAccounts;
  const categories = form.type === 'INCOME' ? incomeCategoriesQuery.data ?? emptyLedgerCategories : expenseCategoriesQuery.data ?? emptyLedgerCategories;

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  useEffect(() => {
    setForm((current) => ({
      ...current,
      accountId: current.accountId || String(accounts[0]?.id ?? ''),
      targetAccountId: current.targetAccountId || String(accounts.find((account) => String(account.id) !== current.accountId)?.id ?? '')
    }));
  }, [accounts]);

  useEffect(() => {
    setForm((current) => {
      if (current.type === 'TRANSFER') {
        return { ...current, categoryId: '' };
      }
      return {
        ...current,
        categoryId: current.categoryId || String(categories[0]?.id ?? '')
      };
    });
  }, [categories, form.type]);

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

  function closeComposePage() {
    // 记一笔页面既可能从模态打开，也可能直接进入；没有历史栈时不能直接 back。
    if (router.canDismiss()) {
      router.dismiss();
      return;
    }
    router.replace('/ledger');
  }

  function updateForm(patch: Partial<LedgerFormState>) {
    setForm((current) => ({ ...current, ...patch }));
    setFormError(null);
  }

  async function handleSubmit() {
    const payload = buildRequest(form);
    if ('error' in payload) {
      setFormError(payload.error);
      return;
    }

    try {
      await createMutation.mutateAsync(payload.data);
      closeComposePage();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '保存流水失败');
    }
  }

  return (
    <SafeAreaView edges={['top', 'left', 'right']} style={styles.page}>
      <GridBackdrop color={theme.border} />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.pageComposerRoot}>
        <View style={styles.pageComposerHeader}>
          <Pressable style={styles.pageHeaderSide} onPress={closeComposePage}>
            <ChevronLeft color={theme.foreground} size={22} />
          </Pressable>
          <Text style={styles.sheetTitle}>记一笔</Text>
          <View style={styles.pageHeaderSide} />
        </View>
        <ScrollView
          style={styles.pageComposerScroll}
          contentContainerStyle={styles.pageComposerContent}
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
        >
          <LedgerComposerFields
            form={form}
            accounts={accounts}
            categories={categories}
            onChange={updateForm}
            submitLabel={createMutation.isPending ? '保存中' : '保存'}
            submitLoading={createMutation.isPending}
            onSubmit={handleSubmit}
          />
          {formError ? <Text variant="error">{formError}</Text> : null}
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function DateTimeField({
  date,
  time,
  open,
  onToggle,
  onChange
}: {
  date: string;
  time: string;
  open: boolean;
  onToggle: () => void;
  onChange: (patch: Partial<LedgerFormState>) => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const pickerValue = useMemo(() => toPickerValue(date, time), [date, time]);

  return (
    <View style={styles.pickerField}>
      <Pressable style={styles.sheetField} onPress={onToggle}>
        <Text style={styles.sheetFieldLabel}>日期时间</Text>
        <View style={styles.selectValue}>
          <Text variant="muted" style={styles.selectValueText} numberOfLines={1}>{formatDateTimeLabel(date, time)}</Text>
          {open ? <ChevronUp color={theme.mutedForeground} size={18} /> : <ChevronDown color={theme.mutedForeground} size={18} />}
        </View>
      </Pressable>
      {open ? (
        <View style={styles.pickerPanel}>
          <DateTimePicker
            mode="single"
            locale="zh-cn"
            date={pickerValue}
            format="YYYY-MM-DD HH:mm:ss"
            height={320}
            headerButtonsPosition="right"
            timePicker
            onChange={({ date: nextDate }) => onChange(toPickerPatch(nextDate, date, time))}
            containerStyle={styles.pickerContainer}
            headerTextStyle={styles.pickerHeaderText}
            weekDaysTextStyle={styles.pickerWeekdayText}
            calendarTextStyle={styles.pickerCalendarText}
            selectedTextStyle={styles.pickerSelectedText}
            selectedItemColor={theme.foreground}
            todayTextStyle={styles.pickerTodayText}
            wheelPickerContainerStyle={styles.wheelPickerContainer}
            wheelPickerTextStyle={styles.wheelPickerText}
            wheelPickerSelectedIndicatorStyle={styles.wheelPickerIndicator}
          />
        </View>
      ) : null}
    </View>
  );
}

function SelectField({
  label,
  value,
  open,
  options,
  selectedId,
  emptyText,
  onToggle,
  onSelect
}: {
  label: string;
  value: string;
  open: boolean;
  options: Array<{ id: string; label: string }>;
  selectedId: string;
  emptyText: string;
  onToggle: () => void;
  onSelect: (id: string) => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <View style={styles.selectBox}>
      <Pressable style={styles.sheetField} onPress={onToggle}>
        <Text style={styles.sheetFieldLabel}>{label}</Text>
        <View style={styles.selectValue}>
          <Text variant="muted" style={styles.selectValueText} numberOfLines={1}>{value}</Text>
          {open ? <ChevronUp color={theme.mutedForeground} size={18} /> : <ChevronDown color={theme.mutedForeground} size={18} />}
        </View>
      </Pressable>
      {open ? (
        <View style={styles.optionList}>
          {options.length > 0 ? (
            <ScrollView nestedScrollEnabled showsVerticalScrollIndicator={false}>
              {options.map((option) => (
                <Pressable
                  key={option.id}
                  style={[styles.optionRow, option.id === selectedId ? styles.optionRowSelected : null]}
                  onPress={() => onSelect(option.id)}
                >
                  <Text style={[styles.optionText, option.id === selectedId ? styles.optionTextSelected : null]}>{option.label}</Text>
                </Pressable>
              ))}
            </ScrollView>
          ) : (
            <Text variant="muted" style={styles.emptyOption}>{emptyText}</Text>
          )}
        </View>
      ) : null}
    </View>
  );
}

function SheetField({ label, value }: { label: string; value: string }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <View style={styles.sheetField}>
      <Text style={styles.sheetFieldLabel}>{label}</Text>
      <Text variant="muted">{value} ›</Text>
    </View>
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

function createEmptyForm(accounts: LedgerAccount[] = [], date = formatDate(new Date())): LedgerFormState {
  return {
    type: 'EXPENSE',
    amount: '',
    accountId: String(accounts[0]?.id ?? ''),
    targetAccountId: String(accounts[1]?.id ?? ''),
    categoryId: '',
    date,
    time: formatTimeInput(new Date().toISOString()),
    note: ''
  };
}

function buildRequest(form: LedgerFormState): { data: LedgerTransactionRequest } | { error: string } {
  const amount = Number(form.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    return { error: '金额必须大于 0' };
  }
  if (!form.accountId) {
    return { error: '请选择账户' };
  }
  if (form.type === 'TRANSFER') {
    if (!form.targetAccountId) {
      return { error: '请选择转入账户' };
    }
    if (form.accountId === form.targetAccountId) {
      return { error: '转出和转入账户不能相同' };
    }
  } else if (!form.categoryId) {
    return { error: '请选择分类' };
  }
  if (!isValidDateInput(form.date)) {
    return { error: '日期格式必须是 YYYY-MM-DD' };
  }
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(form.time)) {
    return { error: '时间格式必须是 HH:mm' };
  }

  return {
    data: {
      type: form.type,
      amount: amount.toFixed(2),
      accountId: form.accountId,
      targetAccountId: form.type === 'TRANSFER' ? form.targetAccountId : null,
      categoryId: form.type === 'TRANSFER' ? null : form.categoryId,
      transactionTime: `${form.date}T${form.time}:00`,
      note: form.note.trim() || null
    }
  };
}

interface MonthCell {
  date: string;
  inMonth: boolean;
  income: number;
  expense: number;
}

interface TrendPoint {
  label: string;
  date: string;
  income: number;
  expense: number;
}

interface CategoryShare {
  key: string;
  categoryId?: string | null;
  name: string;
  amount: number;
  color: string;
}

function buildMonthCells(selectedDate: string, transactions: LedgerTransaction[]): MonthCell[] {
  const base = parseDate(selectedDate);
  const first = new Date(base.getFullYear(), base.getMonth(), 1);
  const start = addDays(first, -first.getDay());
  const totals = transactions.reduce<Record<string, { income: number; expense: number }>>((map, item) => {
    const date = item.transactionTime?.slice(0, 10);
    if (!date) {
      return map;
    }
    map[date] = map[date] || { income: 0, expense: 0 };
    const amount = item.amount ?? 0;
    if (item.type === 'INCOME' || item.type === 'REFUND') {
      map[date].income += amount;
    }
    if (item.type === 'EXPENSE') {
      map[date].expense += amount;
    }
    return map;
  }, {});

  return Array.from({ length: 42 }).map((_, index) => {
    const date = addDays(start, index);
    const dateText = formatDate(date);
    return {
      date: dateText,
      inMonth: date.getMonth() === base.getMonth(),
      income: totals[dateText]?.income ?? 0,
      expense: totals[dateText]?.expense ?? 0
    };
  });
}


function chunkMonthCells(cells: MonthCell[]) {
  // 日历固定只渲染 5 行，避免月视图过高。
  return Array.from({ length: 5 }).map((_, index) => cells.slice(index * 7, index * 7 + 7));
}

function shouldHandleCalendarSwipe(dx: number, dy: number) {
  return Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 10;
}

function summarizeMonthCells(cells: MonthCell[]) {
  return cells.reduce(
    (sum, cell) => {
      if (cell.inMonth) {
        sum.income += cell.income;
        sum.expense += cell.expense;
      }
      return sum;
    },
    { income: 0, expense: 0 }
  );
}

function buildTrendPoints(selectedDate: string, statsMode: StatsMode, transactions: LedgerTransaction[]): TrendPoint[] {
  const totals = groupTransactionsByDate(transactions);
  if (statsMode === 'week') {
    const range = getStatsRange(selectedDate, 'week');
    const start = parseDate(range.startDate);
    return Array.from({ length: 7 }).map((_, index) => {
      const date = formatDate(addDays(start, index));
      return {
        label: weekdays[index],
        date,
        income: totals[date]?.income ?? 0,
        expense: totals[date]?.expense ?? 0
      };
    });
  }
  if (statsMode === 'year') {
    const year = parseDate(selectedDate).getFullYear();
    return Array.from({ length: 12 }).map((_, index) => {
      const prefix = `${year}-${`${index + 1}`.padStart(2, '0')}`;
      const monthTotals = Object.entries(totals).reduce(
        (sum, [date, value]) => {
          if (date.startsWith(prefix)) {
            sum.income += value.income;
            sum.expense += value.expense;
          }
          return sum;
        },
        { income: 0, expense: 0 }
      );
      return {
        label: `${index + 1}月`,
        date: prefix,
        income: monthTotals.income,
        expense: monthTotals.expense
      };
    });
  }

  const base = parseDate(selectedDate);
  const lastDay = new Date(base.getFullYear(), base.getMonth() + 1, 0).getDate();
  return Array.from({ length: lastDay }).map((_, index) => {
    const date = formatDate(new Date(base.getFullYear(), base.getMonth(), index + 1));
    return {
      label: `${index + 1}`,
      date,
      income: totals[date]?.income ?? 0,
      expense: totals[date]?.expense ?? 0
    };
  });
}

function groupTransactionsByDate(transactions: LedgerTransaction[]) {
  return transactions.reduce<Record<string, { income: number; expense: number }>>((map, item) => {
    const date = item.transactionTime?.slice(0, 10);
    if (!date) {
      return map;
    }
    map[date] = map[date] || { income: 0, expense: 0 };
    const amount = item.amount ?? 0;
    if (item.type === 'INCOME' || item.type === 'REFUND') {
      map[date].income += amount;
    }
    if (item.type === 'EXPENSE') {
      map[date].expense += amount;
    }
    return map;
  }, {});
}

function buildCategoryShares(transactions: LedgerTransaction[], type: 'EXPENSE' | 'INCOME'): CategoryShare[] {
  const colors = type === 'EXPENSE'
    ? ['#ff6b4a', '#ff9f43', '#7c6cff', '#55a6ff', '#f15f86', '#9ca3af']
    : ['#20b26b', '#42c8a0', '#7c6cff', '#55a6ff', '#ff9f43', '#9ca3af'];
  const grouped = transactions.reduce<Record<string, { name: string; categoryId?: string | null; amount: number }>>((map, item) => {
    if (item.type !== type) {
      return map;
    }
    const name = item.categoryName || transactionTypeLabel(item.type);
    const key = item.categoryId ? String(item.categoryId) : `name:${name}`;
    map[key] = map[key] || { name, categoryId: item.categoryId ? String(item.categoryId) : null, amount: 0 };
    map[key].amount += item.amount ?? 0;
    return map;
  }, {});

  return Object.entries(grouped)
    .sort((a, b) => b[1].amount - a[1].amount)
    .map(([key, value], index) => ({
      key,
      categoryId: value.categoryId,
      name: value.name,
      amount: value.amount,
      color: colors[index % colors.length]
    }));
}

function sameCategory(item: LedgerTransaction, share: CategoryShare) {
  if (share.categoryId) {
    return String(item.categoryId ?? '') === share.categoryId;
  }
  return (item.categoryName || transactionTypeLabel(item.type)) === share.name;
}

function buildPolyline(points: TrendPoint[], field: 'income' | 'expense', maxAmount: number, left: number, top: number, width: number, height: number) {
  return points
    .map((point, index) => {
      const x = left + (points.length === 1 ? 0 : (index / (points.length - 1)) * width);
      const y = top + height - (point[field] / maxAmount) * height;
      return `${x},${y}`;
    })
    .join(' ');
}

function getTrendPointPosition(
  points: TrendPoint[],
  targetPoint: TrendPoint,
  field: 'income' | 'expense',
  maxAmount: number,
  left: number,
  top: number,
  width: number,
  height: number
) {
  const index = points.findIndex((point) => point.date === targetPoint.date);
  const x = left + (points.length === 1 ? 0 : (index / (points.length - 1)) * width);
  const y = top + height - (targetPoint[field] / maxAmount) * height;
  return { x, y };
}

function getNearestTrendPointIndex(locationX: number, length: number, width: number, left: number, right: number) {
  if (length <= 1) {
    return 0;
  }
  const innerWidth = Math.max(width - left - right, 1);
  const x = Math.min(Math.max(locationX, left), width - right);
  const ratio = (x - left) / innerWidth;
  return Math.min(length - 1, Math.max(0, Math.round(ratio * (length - 1))));
}

function clampTooltipLeft(x: number, width: number) {
  const tooltipWidth = 104;
  return Math.min(Math.max(x - tooltipWidth / 2, 8), Math.max(width - tooltipWidth - 8, 8));
}

function getAxisLabelIndexes(length: number) {
  if (length <= 3) {
    return Array.from({ length }).map((_, index) => index);
  }
  return [0, Math.floor((length - 1) / 2), length - 1];
}

function formatTrendPointDate(value: string) {
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const date = parseDate(value);
    return `${date.getMonth() + 1}月${date.getDate()}日`;
  }
  if (/^\d{4}-\d{2}$/.test(value)) {
    return value.replace('-', '年') + '月';
  }
  return value;
}

function chunkDetailMetrics(items: Array<{ label: string; value: string } | null>) {
  const filtered = items.filter(Boolean) as Array<{ label: string; value: string }>;
  const rows: Array<Array<{ label: string; value: string } | null>> = [];
  for (let index = 0; index < filtered.length; index += 2) {
    rows.push([filtered[index], filtered[index + 1] ?? null]);
  }
  return rows;
}

function formatAxisMoney(value: number) {
  if (value >= 10000) {
    return `${formatStatsPlainAmount(value / 10000)}w`;
  }
  return formatStatsPlainAmount(value);
}

function statsModeLabel(mode: StatsMode) {
  if (mode === 'week') return '周';
  if (mode === 'year') return '年';
  return '月';
}

function getStatsRange(selectedDate: string, statsMode: StatsMode) {
  const date = parseDate(selectedDate);
  if (statsMode === 'week') {
    const start = addDays(date, -date.getDay());
    return {
      startDate: formatDate(start),
      endDate: formatDate(addDays(start, 6))
    };
  }
  if (statsMode === 'year') {
    return {
      startDate: `${date.getFullYear()}-01-01`,
      endDate: `${date.getFullYear()}-12-31`
    };
  }
  const first = new Date(date.getFullYear(), date.getMonth(), 1);
  const last = new Date(date.getFullYear(), date.getMonth() + 1, 0);
  return {
    startDate: formatDate(first),
    endDate: formatDate(last)
  };
}

function formatStatsRangeLabel(selectedDate: string, statsMode: StatsMode) {
  const range = getStatsRange(selectedDate, statsMode);
  if (statsMode === 'week') {
    return `${range.startDate.slice(5).replace('-', '/')} - ${range.endDate.slice(5).replace('-', '/')}`;
  }
  if (statsMode === 'year') {
    return `${parseDate(selectedDate).getFullYear()}年`;
  }
  return formatMonthLabel(selectedDate);
}

function summarizeTransactions(items: LedgerTransaction[]) {
  return items.reduce(
    (sum, item) => {
      const amount = item.amount ?? 0;
      if (item.type === 'INCOME' || item.type === 'REFUND') {
        sum.income += amount;
      }
      if (item.type === 'EXPENSE') {
        sum.expense += amount;
      }
      return sum;
    },
    { income: 0, expense: 0 }
  );
}

function countDirectionTransactions(items: LedgerTransaction[], direction: StatsDirection) {
  return items.filter((item) => item.type === direction).length;
}

function formatSignedAmount(item: LedgerTransaction) {
  if (item.amount === null || item.amount === undefined) {
    return '--';
  }
  const prefix = item.type === 'INCOME' || item.type === 'REFUND' ? '+' : item.type === 'TRANSFER' ? '' : '-';
  return `${prefix} ${formatMoney(item.amount)}`;
}

function normalizeType(type?: string | null): LedgerTransactionType {
  if (type === 'INCOME' || type === 'TRANSFER') {
    return type;
  }
  return 'EXPENSE';
}

function transactionTypeLabel(type?: string | null) {
  const map: Record<string, string> = {
    EXPENSE: '支出',
    INCOME: '收入',
    TRANSFER: '转账',
    REFUND: '退款'
  };
  return type ? map[type] || type : '流水';
}

function directionLabel(direction: StatsDirection) {
  return direction === 'EXPENSE' ? '支出' : '收入';
}

function formatShortDateTime(value?: string | null) {
  if (!value) {
    return '--';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  const time = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false });
  return `${month}-${day}  ${time}`;
}

function selectedName(items: Array<{ id: string; name?: string | null }>, id: string) {
  return items.find((item) => String(item.id) === id)?.name;
}

function categoryInitial(item: LedgerTransaction) {
  return (item.categoryName || transactionTypeLabel(item.type)).slice(0, 1);
}

function pickCategoryColor(item: LedgerTransaction) {
  if (item.type === 'INCOME') return '#20b26b';
  if (item.type === 'TRANSFER') return '#6b7cff';
  return '#ff7a45';
}

function transactionAmountColor(item: LedgerTransaction, theme: ReturnType<typeof useTheme>) {
  if (item.type === 'INCOME' || item.type === 'REFUND') {
    return { color: theme.success };
  }
  if (item.type === 'EXPENSE') {
    return { color: theme.destructive };
  }
  return { color: theme.foreground };
}

function formatStatsMoney(value: number) {
  // 统计页统一四舍五入到两位小数，避免真机窄屏出现长小数撑破布局。
  return formatMoney(Number.isFinite(value) ? value : null);
}

function formatStatsPlainAmount(value: number) {
  // 图表坐标和圆环中心不带币种，但仍按金额展示精度保留两位小数。
  if (!Number.isFinite(value)) {
    return '--';
  }
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatCalendarAmount(value: number) {
  if (!Number.isFinite(value)) {
    return '--';
  }
  return value.toFixed(2);
}

function formatPercentExact(value: number) {
  if (!Number.isFinite(value)) {
    return '--';
  }
  return `${(value * 100).toFixed(2)}%`;
}

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function parseDate(value: string) {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function isValidDateInput(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }
  const parsed = parseDate(value);
  return !Number.isNaN(parsed.getTime()) && formatDate(parsed) === value;
}

function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(date.getDate() + days);
  return next;
}

function addMonths(date: Date, months: number) {
  const next = new Date(date);
  next.setMonth(date.getMonth() + months);
  return next;
}

function addYears(date: Date, years: number) {
  const next = new Date(date);
  next.setFullYear(date.getFullYear() + years);
  return next;
}

function formatMonthLabel(value: string) {
  const date = parseDate(value);
  return `${date.getFullYear()}年${date.getMonth() + 1}月`;
}

function formatReadableDate(value: string) {
  return parseDate(value).toLocaleDateString('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  });
}

function formatTimeInput(value?: string | null) {
  if (!value) {
    return '09:00';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value.slice(11, 16) || '09:00';
  }
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false });
}

function toPickerValue(date: string, time: string) {
  const fallback = dayjs();
  const value = dayjs(`${date}T${time}:00`);
  return value.isValid() ? value : fallback;
}

function toPickerPatch(value: DateType, fallbackDate: string, fallbackTime: string): Partial<LedgerFormState> {
  const parsed = dayjs(value);
  if (!parsed.isValid()) {
    return { date: fallbackDate, time: fallbackTime };
  }
  return {
    date: parsed.format('YYYY-MM-DD'),
    time: parsed.format('HH:mm')
  };
}

function formatDateTimeLabel(date: string, time: string) {
  if (!isValidDateInput(date)) {
    return '请选择日期时间';
  }
  const parsed = parseDate(date);
  const today = formatDate(new Date());
  const tomorrow = formatDate(addDays(new Date(), 1));
  const yesterday = formatDate(addDays(new Date(), -1));
  const prefix = date === today ? '今天' : date === tomorrow ? '明天' : date === yesterday ? '昨天' : weekdays[parsed.getDay()];
  return `${prefix} ${parsed.getMonth() + 1}月${parsed.getDate()}日 ${time}`;
}

const createStyles = (theme: ReturnType<typeof useTheme>) =>
  (() => {
    const isDark = theme.background === '#09090b';
    const submitButtonBackground = isDark ? '#0f0f10' : '#ffffff';
    const submitButtonText = isDark ? '#ffffff' : '#111111';
    const submitButtonBorder = isDark ? '#2a2a2d' : '#e4e4e7';
    const submitButtonGlow = isDark
      ? '0 0 0 1px rgba(255,255,255,0.08), 0 0 24px rgba(96,165,250,0.16), 0 10px 24px rgba(0,0,0,0.38)'
      : '0 0 0 1px rgba(0,0,0,0.06), 0 0 24px rgba(37,99,235,0.12), 0 10px 24px rgba(37,99,235,0.08)';

    return StyleSheet.create({
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
      padding: 16,
      paddingBottom: 28
    },
    pageComposerRoot: {
      flex: 1,
      position: 'relative'
    },
    pageComposerHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between',
      paddingHorizontal: 16,
      paddingTop: 8,
      paddingBottom: 12
    },
    pageHeaderSide: {
      alignItems: 'center',
      height: 36,
      justifyContent: 'center',
      width: 36
    },
    pageComposerScroll: {
      flex: 1
    },
    pageComposerContent: {
      gap: 12,
      paddingHorizontal: 16,
      paddingBottom: 28
    },
    pageComposerSubmitHitArea: {
      alignSelf: 'stretch',
      marginTop: 4
    },
    pageComposerSubmitButton: {
      alignItems: 'center',
      backgroundColor: theme.primary,
      borderColor: theme.primary,
      borderRadius: 8,
      borderWidth: 1,
      flexDirection: 'row',
      gap: 8,
      justifyContent: 'center',
      minHeight: 52
    },
    pageComposerSubmitButtonDisabled: {
      opacity: 0.5
    },
    pageComposerSubmitText: {
      color: theme.primaryForeground,
      fontSize: theme.tokens.fontSize.subtitle,
      fontWeight: '700'
    },
    header: {
      gap: 4,
      paddingTop: 6
    },
    title: {
      color: theme.foreground,
      fontSize: 28,
      fontWeight: '900'
    },
    controlBar: {
      alignItems: 'center',
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderRadius: 16,
      borderWidth: 1,
      flexDirection: 'row',
      gap: 10,
      justifyContent: 'space-between',
      padding: 10
    },
    topSegmented: {
      backgroundColor: theme.secondary,
      borderRadius: 18,
      flexDirection: 'row',
      padding: 3,
      flex: 1
    },
    topSegmentButton: {
      alignItems: 'center',
      borderRadius: 15,
      flex: 1,
      paddingVertical: 7
    },
    topSegmentText: {
      color: theme.foreground,
      fontSize: 14,
      fontWeight: '800'
    },
    directionSegmented: {
      backgroundColor: theme.secondary,
      borderRadius: 18,
      flexDirection: 'row',
      padding: 3,
      flex: 0.86
    },
    directionButton: {
      alignItems: 'center',
      borderRadius: 15,
      flex: 1,
      paddingVertical: 7
    },
    segmented: {
      backgroundColor: theme.secondary,
      borderRadius: 18,
      flexDirection: 'row',
      padding: 3
    },
    segmentButton: {
      alignItems: 'center',
      borderRadius: 15,
      flex: 1,
      paddingVertical: 9
    },
    segmentActive: {
      backgroundColor: theme.foreground
    },
    segmentText: {
      color: theme.mutedForeground,
      fontSize: 14,
      fontWeight: '700'
    },
    segmentTextActive: {
      color: theme.primaryForeground
    },
    calendarCard: {
      gap: 14,
      padding: 16
    },
    monthHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    monthTitle: {
      color: theme.foreground,
      fontSize: 18,
      fontWeight: '800'
    },
    monthActions: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 14
    },
    iconButton: {
      alignItems: 'center',
      height: 32,
      justifyContent: 'center',
      width: 32
    },
    weekHeader: {
      flexDirection: 'row'
    },
    weekText: {
      flex: 1,
      textAlign: 'center'
    },
    calendarGrid: {
      gap: 8
    },
    calendarAnimated: {
      overflow: 'hidden',
      position: 'relative'
    },
    calendarWeekRow: {
      flexDirection: 'row'
    },
    dayCell: {
      alignItems: 'center',
      borderRadius: 14,
      gap: 2,
      minHeight: 54,
      paddingVertical: 5,
      flex: 1
    },
    dayCellSelected: {
      backgroundColor: theme.foreground
    },
    dayNumber: {
      color: theme.foreground,
      fontSize: 14,
      fontWeight: '700'
    },
    dayNumberSelected: {
      color: theme.primaryForeground
    },
    mutedDay: {
      color: theme.mutedForeground
    },
    incomeTiny: {
      color: theme.success,
      fontSize: 9,
      fontWeight: '700'
    },
    expenseTiny: {
      color: theme.destructive,
      fontSize: 9,
      fontWeight: '700'
    },
    dayPanel: {
      gap: 12,
      padding: 16
    },
    sectionHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    linkButton: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 3
    },
    linkText: {
      color: theme.mutedForeground,
      fontSize: 14,
      fontWeight: '700'
    },
    sectionTitle: {
      color: theme.foreground,
      fontSize: 18,
      fontWeight: '800'
    },
    dayBalance: {
      color: theme.success,
      fontSize: 14,
      fontWeight: '800'
    },
    positiveAmount: {
      color: theme.success
    },
    negativeAmount: {
      color: theme.destructive
    },
    transactionItem: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 10,
      paddingVertical: 10
    },
    categoryIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 17,
      height: 34,
      justifyContent: 'center',
      width: 34
    },
    categoryIconText: {
      color: theme.foreground,
      fontSize: 15,
      fontWeight: '900'
    },
    transactionInfo: {
      flex: 1,
      gap: 4,
      minWidth: 0
    },
    transactionTitle: {
      color: theme.foreground,
      fontSize: 16,
      fontWeight: '800'
    },
    transactionRight: {
      alignItems: 'flex-end',
      gap: 8,
      minWidth: 104
    },
    transactionAmount: {
      color: theme.foreground,
      fontSize: 15,
      fontWeight: '800'
    },
    transactionActions: {
      flexDirection: 'row',
      gap: 8
    },
    smallIconButton: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 8,
      height: 30,
      justifyContent: 'center',
      width: 30
    },
    periodSegmented: {
      alignItems: 'center',
      alignSelf: 'stretch',
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderRadius: 8,
      borderWidth: 1,
      flexDirection: 'row',
      justifyContent: 'space-around',
      padding: 0
    },
    periodButton: {
      alignItems: 'center',
      borderRadius: 7,
      flex: 1,
      paddingVertical: 10
    },
    periodText: {
      color: theme.foreground,
      fontSize: 15,
      fontWeight: '700'
    },
    periodHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    roundArrow: {
      alignItems: 'center',
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderRadius: 26,
      borderWidth: 1,
      height: 52,
      justifyContent: 'center',
      width: 52
    },
    statsSummary: {
      flexDirection: 'row',
      gap: 12,
      padding: 16
    },
    balanceCard: {
      gap: 14,
      padding: 16
    },
    balanceAmount: {
      color: theme.foreground,
      fontSize: 40,
      fontWeight: '900'
    },
    progressRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 10
    },
    progressLabel: {
      color: theme.foreground,
      fontSize: 15,
      minWidth: 62
    },
    progressTrack: {
      backgroundColor: theme.secondary,
      borderRadius: 999,
      flex: 1,
      height: 9,
      overflow: 'hidden'
    },
    progressFill: {
      borderRadius: 999,
      height: '100%'
    },
    progressValue: {
      color: theme.foreground,
      fontSize: 13,
      minWidth: 78,
      textAlign: 'right'
    },
    chartCard: {
      gap: 16,
      padding: 16
    },
    statsTitleRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 10
    },
    statsIconBubble: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 20,
      height: 38,
      justifyContent: 'center',
      width: 38
    },
    trendMetaRow: {
      flexDirection: 'row',
      gap: 16
    },
    metaDivider: {
      backgroundColor: theme.border,
      width: 1
    },
    legendRow: {
      flexDirection: 'row',
      gap: 12
    },
    shareCard: {
      gap: 14,
      padding: 16
    },
    rankingContent: {
      gap: 12,
      padding: 16
    },
    rankingRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 9
    },
    rankingIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 17,
      height: 34,
      justifyContent: 'center',
      width: 34
    },
    rankingInfo: {
      flex: 1,
      gap: 5,
      minWidth: 0
    },
    rankingTitleRow: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    rankingName: {
      color: theme.foreground,
      flex: 1,
      fontSize: 14,
      fontWeight: '800'
    },
    rankingTrack: {
      backgroundColor: theme.secondary,
      borderRadius: 999,
      height: 5,
      overflow: 'hidden'
    },
    rankingFill: {
      borderRadius: 999,
      height: '100%'
    },
    shareContent: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 16
    },
    shareLegend: {
      flex: 1,
      gap: 8
    },
    shareLegendRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 6
    },
    shareDot: {
      borderRadius: 4,
      height: 8,
      width: 8
    },
    shareName: {
      flex: 1
    },
    detailCard: {
      gap: 12,
      padding: 16
    },
    detailRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 10,
      paddingVertical: 9
    },
    detailInfo: {
      flex: 1,
      gap: 4,
      minWidth: 0
    },
    detailTitle: {
      color: theme.foreground,
      fontSize: 14,
      fontWeight: '800'
    },
    detailAmount: {
      color: theme.foreground,
      fontSize: 15,
      fontWeight: '800',
      textAlignVertical: 'center'
    },
    modalRoot: {
      flex: 1,
      justifyContent: 'flex-end'
    },
    modalBackdrop: {
      ...StyleSheet.absoluteFillObject,
      backgroundColor: 'rgba(0,0,0,0.18)'
    },
    sheet: {
      backgroundColor: theme.card,
      borderTopLeftRadius: 22,
      borderTopRightRadius: 22,
      // 底部弹层内容少时也至少占半屏，避免看起来像一条小抽屉。
      minHeight: '50%',
      maxHeight: '86%'
    },
    composerSheet: {
      height: '86%',
      overflow: 'hidden',
      position: 'relative'
    },
    sheetScroller: {
      flex: 1,
      marginBottom: 88
    },
    sheetContent: {
      gap: 12,
      padding: 16,
      paddingBottom: 24
    },
    // 真机小屏上保存按钮需要脱离内容滚动区，避免被下拉内容挤出可视范围。
    sheetFooter: {
      backgroundColor: theme.card,
      borderTopColor: theme.border,
      borderTopWidth: 1,
      bottom: 0,
      gap: 10,
      left: 0,
      padding: 16,
      paddingTop: 12,
      position: 'absolute',
      right: 0
    },
    sheetHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    sheetHeaderActions: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 16
    },
    sheetTitle: {
      color: theme.foreground,
      fontSize: 22,
      fontWeight: '800'
    },
    noteBlock: {
      gap: 6
    },
    submitActionBlock: {
      marginTop: 12
    },
    submitError: {
      marginTop: 10
    },
    submitActionButton: {
      alignItems: 'center',
      backgroundColor: submitButtonBackground,
      borderColor: submitButtonBorder,
      borderRadius: 14,
      borderCurve: 'continuous',
      borderWidth: 1,
      flexDirection: 'row',
      gap: 8,
      justifyContent: 'center',
      minHeight: 54,
      paddingHorizontal: 16,
      paddingVertical: 14,
      boxShadow: submitButtonGlow,
      shadowColor: isDark ? '#ffffff' : '#2563eb',
      shadowOffset: { width: 0, height: 6 },
      shadowOpacity: isDark ? 0.18 : 0.14,
      shadowRadius: 12,
      width: '100%',
      elevation: 6
    },
    submitActionButtonPressed: {
      opacity: 0.9,
      transform: [{ scale: 0.985 }]
    },
    submitActionButtonDisabled: {
      opacity: 0.55
    },
    submitActionText: {
      color: submitButtonText,
      fontSize: 16,
      fontWeight: '700',
      includeFontPadding: false,
      lineHeight: 18,
      textAlign: 'center'
    },
    amountRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12
    },
    amountInputWrap: {
      flex: 1
    },
    amountInput: {
      borderWidth: 0,
      fontSize: 32,
      fontWeight: '800',
      minHeight: 58,
      paddingHorizontal: 0
    },
    cameraBox: {
      alignItems: 'center',
      gap: 4,
      width: 48
    },
    sheetField: {
      alignItems: 'center',
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderRadius: 8,
      borderWidth: 1,
      flexDirection: 'row',
      justifyContent: 'space-between',
      minHeight: 52,
      paddingHorizontal: 12
    },
    sheetFieldLabel: {
      color: theme.foreground,
      fontSize: 15,
      fontWeight: '800'
    },
    detailMetricGrid: {
      gap: 10
    },
    detailMetricRow: {
      flexDirection: 'row',
      gap: 10
    },
    detailMetricCell: {
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderRadius: 8,
      borderWidth: 1,
      flex: 1,
      minHeight: 66,
      paddingHorizontal: 12,
      paddingVertical: 10
    },
    detailMetricCellLeft: {
      marginRight: 0
    },
    detailMetricCellRight: {
      marginLeft: 0
    },
    detailMetricEmpty: {
      opacity: 0
    },
    detailMetricLabel: {
      color: theme.foreground,
      fontSize: 13,
      fontWeight: '800'
    },
    detailMetricValue: {
      color: theme.mutedForeground,
      fontSize: 13,
      lineHeight: 18,
      marginTop: 8
    },
    selectValue: {
      alignItems: 'center',
      flexDirection: 'row',
      flexShrink: 1,
      gap: 6,
      justifyContent: 'flex-end',
      marginLeft: 12
    },
    selectValueText: {
      flexShrink: 1,
      maxWidth: 190
    },
    selectBox: {
      gap: 6
    },
    pickerField: {
      gap: 6
    },
    pickerPanel: {
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderRadius: 8,
      borderWidth: 1,
      overflow: 'hidden',
      paddingHorizontal: 4,
      paddingVertical: 2
    },
    pickerContainer: {
      backgroundColor: 'transparent'
    },
    pickerHeaderText: {
      color: theme.foreground,
      fontSize: 15,
      fontWeight: '800'
    },
    pickerWeekdayText: {
      color: theme.mutedForeground,
      fontSize: 12,
      fontWeight: '700'
    },
    pickerCalendarText: {
      color: theme.foreground,
      fontSize: 15,
      fontWeight: '700'
    },
    pickerSelectedText: {
      color: theme.primaryForeground,
      fontWeight: '800'
    },
    pickerTodayText: {
      color: theme.foreground,
      fontWeight: '800'
    },
    wheelPickerContainer: {
      minHeight: 160
    },
    wheelPickerText: {
      color: theme.foreground,
      fontSize: 15,
      fontWeight: '800'
    },
    wheelPickerIndicator: {
      backgroundColor: theme.secondary,
      borderColor: theme.border,
      borderRadius: 8,
      borderWidth: 1
    },
    optionList: {
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderRadius: 8,
      borderWidth: 1,
      // 下拉项内部滚动，避免分类或账户过多时撑高整张表单。
      maxHeight: 180,
      overflow: 'hidden'
    },
    optionRow: {
      justifyContent: 'center',
      minHeight: 44,
      paddingHorizontal: 12
    },
    optionRowSelected: {
      backgroundColor: theme.secondary
    },
    optionText: {
      color: theme.foreground,
      fontSize: 14,
      fontWeight: '700'
    },
    optionTextSelected: {
      color: theme.foreground
    },
    emptyOption: {
      paddingHorizontal: 12,
      paddingVertical: 12
    }
    });
  })();

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
  summaryBlock: {
    flex: 1,
    gap: 6
  },
  summaryValue: {
    fontSize: 18,
    fontWeight: '800'
  },
  chartWrap: {
    alignSelf: 'stretch',
    height: 210,
    overflow: 'hidden'
  },
  chartTouchArea: {
    ...StyleSheet.absoluteFillObject,
    zIndex: 2
  },
  chartTooltip: {
    backgroundColor: '#ffffff',
    borderColor: '#e5e7eb',
    borderRadius: 8,
    borderWidth: 1,
    alignItems: 'center',
    gap: 2,
    paddingHorizontal: 6,
    paddingVertical: 4,
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.12,
    shadowRadius: 6,
    position: 'absolute',
    width: 104,
    elevation: 3,
    zIndex: 3
  },
  chartTooltipDate: {
    color: '#111111',
    fontSize: 11,
    fontWeight: '700',
    textAlign: 'center'
  },
  chartTooltipAmount: {
    color: '#111111',
    fontSize: 11,
    fontWeight: '900',
    textAlign: 'center'
  },
  legendItem: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 4
  },
  legendDot: {
    borderRadius: 3,
    height: 7,
    width: 7
  }
});
