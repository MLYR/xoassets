import { Redirect, useLocalSearchParams } from 'expo-router';
import { BarChart3, Camera, ChevronLeft, ChevronRight, Edit3, PieChart, ReceiptText, Trash2, WalletCards, X } from 'lucide-react-native';
import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Alert, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Svg, { Circle, G, Line, Polyline, Text as SvgText } from 'react-native-svg';

import { Button, Card, CardContent, Input, Separator, Text } from '@/components/ui';
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

interface LedgerFormState {
  id?: string;
  type: LedgerTransactionType;
  amount: string;
  accountId: string;
  targetAccountId: string;
  categoryId: string;
  time: string;
  note: string;
}

export function LedgerScreen({ initialCompose = false }: { initialCompose?: boolean } = {}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const params = useLocalSearchParams<{ compose?: string }>();
  const { isHydrated, isLoggedIn, restoreToken } = useAuthStore();
  const [selectedDate, setSelectedDate] = useState(formatDate(new Date()));
  const [viewMode, setViewMode] = useState<LedgerViewMode>('stats');
  const [statsMode, setStatsMode] = useState<StatsMode>('month');
  const [statsDirection, setStatsDirection] = useState<StatsDirection>('EXPENSE');
  const [composerOpen, setComposerOpen] = useState(false);
  const [form, setForm] = useState<LedgerFormState>(() => createEmptyForm());
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  useEffect(() => {
    if (params.compose || initialCompose) {
      setComposerOpen(true);
    }
  }, [initialCompose, params.compose]);

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

  const accounts = accountsQuery.data ?? [];
  const categories = form.type === 'INCOME' ? incomeCategoriesQuery.data ?? [] : expenseCategoriesQuery.data ?? [];
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
    const payload = buildRequest(form, selectedDate);
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
        time: formatTimeInput(item.transactionTime),
        note: item.note ?? ''
      });
    } else {
      setForm(createEmptyForm(accounts));
    }
    setFormError(null);
    setComposerOpen(true);
  }

  function closeComposer() {
    setComposerOpen(false);
    setForm(createEmptyForm(accounts));
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
          <Text style={styles.title}>记账</Text>
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
                <MonthHeader selectedDate={selectedDate} onPrev={() => changeMonth(-1)} onNext={() => changeMonth(1)} />
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
              </CardContent>
            </Card>

            <Card>
              <CardContent style={styles.dayPanel}>
                <View style={styles.sectionHeader}>
                  <View>
                    <Text style={styles.sectionTitle}>{formatReadableDate(selectedDate)}</Text>
                    <Text variant="caption">收入 {formatMoney(daySummary.income)} · 支出 {formatMoney(daySummary.expense)}</Text>
                  </View>
                  <Text style={styles.dayBalance}>{formatMoney(daySummary.income - daySummary.expense)}</Text>
                </View>
                {transactionsQuery.isLoading ? <ActivityIndicator color={theme.primary} /> : null}
                {transactionsQuery.isError ? <Text variant="error">流水加载失败，请稍后重试。</Text> : null}
                {transactions.length > 0 ? (
                  transactions.map((item, index) => (
                    <View key={String(item.id)}>
                      <TransactionItem item={item} onEdit={openComposer} onDelete={handleDelete} />
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
  const selectedCategoryTransactions = useMemo(
    () => selectedCategoryShare
      ? transactions.filter((item) => item.type === statsDirection && sameCategory(item, selectedCategoryShare))
      : [],
    [selectedCategoryShare, statsDirection, transactions]
  );

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
              <View style={[styles.rankingIcon, { backgroundColor: share.color }]}>
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
            <Text variant="muted">更多 ›</Text>
          </View>
          {transactions.filter((item) => item.type === statsDirection).slice(0, 5).map((item, index) => (
            <View key={String(item.id)}>
              <Pressable style={styles.detailRow} onPress={() => setSelectedDetailTransaction(item)}>
                <View style={[styles.categoryIcon, { backgroundColor: pickCategoryColor(item) }]}>
                  <Text style={styles.categoryIconText}>{categoryInitial(item)}</Text>
                </View>
                <View style={styles.detailInfo}>
                  <Text style={styles.detailTitle} numberOfLines={1}>
                    {item.categoryName || directionLabel(statsDirection)}{item.note ? ` · ${item.note}` : ''}
                  </Text>
                  <Text variant="muted">{formatShortDateTime(item.transactionTime)}</Text>
                </View>
                <Text style={styles.detailAmount}>{formatSignedAmount(item)}</Text>
              </Pressable>
              {index < Math.min(transactions.filter((item) => item.type === statsDirection).length, 5) - 1 ? <Separator /> : null}
            </View>
          ))}
          {transactions.filter((item) => item.type === statsDirection).length === 0 ? <Text variant="muted">当前范围没有明细。</Text> : null}
        </CardContent>
      </Card>

      <CategoryTransactionsModal
        share={selectedCategoryShare}
        transactions={selectedCategoryTransactions}
        onSelectTransaction={setSelectedDetailTransaction}
        onClose={() => setSelectedCategoryShare(null)}
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
  const width = 320;
  const height = 210;
  const left = 48;
  const right = 12;
  const top = 18;
  const bottom = 34;
  const chartWidth = width - left - right;
  const chartHeight = height - top - bottom;
  const field = direction === 'EXPENSE' ? 'expense' : 'income';
  const maxAmount = Math.max(...points.map((point) => point[field]), 1);
  const line = buildPolyline(points, field, maxAmount, left, top, chartWidth, chartHeight);
  const axisValues = [maxAmount, maxAmount / 2, 0];
  const labelIndexes = getAxisLabelIndexes(points.length);

  return (
    <View style={stylesStatic.chartWrap}>
      <Svg width="100%" height={height} viewBox={`0 0 ${width} ${height}`}>
        {axisValues.map((value, index) => {
          const y = top + (index / 2) * chartHeight;
          return (
            <G key={`axis-${index}`}>
              <Line x1={left} y1={y} x2={width - right} y2={y} stroke={theme.border} strokeWidth={1} strokeDasharray="4 6" />
              <SvgText x={0} y={y + 4} fontSize={10} fill={theme.mutedForeground}>
                {formatAxisMoney(value)}
              </SvgText>
            </G>
          );
        })}
        <Polyline points={line} fill="none" stroke={theme.foreground} strokeWidth={2.5} strokeLinejoin="round" strokeLinecap="round" />
        {points.map((point, index) => {
          const x = left + (points.length === 1 ? 0 : (index / (points.length - 1)) * chartWidth);
          const y = top + chartHeight - (point[field] / maxAmount) * chartHeight;
          return <Circle key={`${point.label}-dot`} cx={x} cy={y} r={3} fill={theme.foreground} />;
        })}
        {points.map((point, index) => {
          if (!labelIndexes.includes(index)) {
            return null;
          }
          const x = left + (points.length === 1 ? 0 : (index / (points.length - 1)) * chartWidth);
          return (
            <SvgText key={point.label} x={x - 10} y={height - 8} fontSize={10} fill={theme.mutedForeground}>
              {point.label}
            </SvgText>
          );
        })}
      </Svg>
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
                  <View style={[styles.categoryIcon, { backgroundColor: pickCategoryColor(item) }]}>
                    <Text style={styles.categoryIconText}>{categoryInitial(item)}</Text>
                  </View>
                  <View style={styles.detailInfo}>
                    <Text style={styles.detailTitle} numberOfLines={1}>{item.note || item.categoryName || transactionTypeLabel(item.type)}</Text>
                    <Text variant="muted">{formatShortDateTime(item.transactionTime)} · {item.accountName || '--'}</Text>
                  </View>
                  <Text style={styles.detailAmount}>{formatSignedAmount(item)}</Text>
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
              <>
                <DetailMetric label="金额" value={formatSignedAmount(transaction)} />
                <DetailMetric label="类型" value={transactionTypeLabel(transaction.type)} />
                <DetailMetric label="分类" value={transaction.categoryName || '--'} />
                <DetailMetric label="账户" value={transaction.accountName || '--'} />
                {transaction.targetAccountName ? <DetailMetric label="转入账户" value={transaction.targetAccountName} /> : null}
                <DetailMetric label="时间" value={transaction.transactionTime?.replace('T', ' ').slice(0, 16) || '--'} />
                <DetailMetric label="备注" value={transaction.note || '--'} />
              </>
            ) : null}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

function DetailMetric({ label, value }: { label: string; value: string }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <View style={styles.sheetField}>
      <Text style={styles.sheetFieldLabel}>{label}</Text>
      <Text variant="muted">{value}</Text>
    </View>
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

function TransactionItem({ item, onEdit, onDelete }: { item: LedgerTransaction; onEdit: (item: LedgerTransaction) => void; onDelete: (item: LedgerTransaction) => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <View style={styles.transactionItem}>
      <View style={[styles.categoryIcon, { backgroundColor: pickCategoryColor(item) }]}>
        <Text style={styles.categoryIconText}>{categoryInitial(item)}</Text>
      </View>
      <View style={styles.transactionInfo}>
        <Text style={styles.transactionTitle}>{item.categoryName || transactionTypeLabel(item.type)}</Text>
        <Text variant="caption">
          {transactionTypeLabel(item.type)} · {formatTimeInput(item.transactionTime)}
        </Text>
      </View>
      <View style={styles.transactionRight}>
        <Text style={styles.transactionAmount} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.75}>
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
    </View>
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
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.modalRoot}>
        <Pressable style={styles.modalBackdrop} onPress={onClose} />
        <View style={styles.sheet}>
          <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.sheetContent}>
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>{form.id ? '编辑记录' : '添加记录'}</Text>
              <Pressable onPress={onClose}>
                <X color={theme.foreground} size={24} />
              </Pressable>
            </View>
            <View style={styles.segmented}>
              {transactionTypes.map((item) => (
                <Pressable
                  key={item.value}
                  style={[styles.segmentButton, form.type === item.value ? styles.segmentActive : null]}
                  onPress={() => onChange({ type: item.value, categoryId: '', targetAccountId: '' })}
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
            <SheetField label="分类" value={form.type === 'TRANSFER' ? '转账' : selectedName(categories, form.categoryId) || '请选择分类'} />
            <ChoiceWrap>
              {form.type === 'TRANSFER'
                ? null
                : categories.map((category) => (
                    <ChoiceChip
                      key={String(category.id)}
                      label={category.name || '未命名分类'}
                      selected={String(category.id) === form.categoryId}
                      onPress={() => onChange({ categoryId: String(category.id) })}
                    />
                  ))}
            </ChoiceWrap>
            <SheetField label="账户" value={selectedName(accounts, form.accountId) || '默认账户'} />
            <ChoiceWrap>
              {accounts.map((account) => (
                <ChoiceChip
                  key={String(account.id)}
                  label={account.name || '未命名账户'}
                  selected={String(account.id) === form.accountId}
                  onPress={() => onChange({ accountId: String(account.id) })}
                />
              ))}
            </ChoiceWrap>
            {form.type === 'TRANSFER' ? (
              <>
                <SheetField label="转入账户" value={selectedName(accounts, form.targetAccountId) || '请选择账户'} />
                <ChoiceWrap>
                  {accounts
                    .filter((account) => String(account.id) !== form.accountId)
                    .map((account) => (
                      <ChoiceChip
                        key={String(account.id)}
                        label={account.name || '未命名账户'}
                        selected={String(account.id) === form.targetAccountId}
                        onPress={() => onChange({ targetAccountId: String(account.id) })}
                      />
                    ))}
                </ChoiceWrap>
              </>
            ) : null}
            <SheetField label="时间" value={form.time} />
            <Input placeholder="HH:mm" value={form.time} onChangeText={(time) => onChange({ time })} />
            <Input label="备注" placeholder="可选" value={form.note} onChangeText={(note) => onChange({ note })} />
            {formError ? <Text variant="error">{formError}</Text> : null}
            <Button loading={isSubmitting} onPress={onSubmit}>
              保存
            </Button>
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
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

function ChoiceWrap({ children }: { children: React.ReactNode }) {
  return <View style={stylesStatic.choiceWrap}>{children}</View>;
}

function ChoiceChip({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Pressable style={[styles.choiceChip, selected ? styles.choiceChipSelected : null]} onPress={onPress}>
      <Text style={[styles.choiceText, selected ? styles.choiceTextSelected : null]} numberOfLines={1}>
        {label}
      </Text>
    </Pressable>
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

function createEmptyForm(accounts: LedgerAccount[] = []): LedgerFormState {
  return {
    type: 'EXPENSE',
    amount: '',
    accountId: String(accounts[0]?.id ?? ''),
    targetAccountId: String(accounts[1]?.id ?? ''),
    categoryId: '',
    time: formatTimeInput(new Date().toISOString()),
    note: ''
  };
}

function buildRequest(form: LedgerFormState, selectedDate: string): { data: LedgerTransactionRequest } | { error: string } {
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
      transactionTime: `${selectedDate}T${form.time}:00`,
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
  // 原生端百分比宽度偶发换行会挤出第 7 行，按周切块能固定只渲染 6 行。
  return Array.from({ length: 6 }).map((_, index) => cells.slice(index * 7, index * 7 + 7));
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

function getAxisLabelIndexes(length: number) {
  if (length <= 3) {
    return Array.from({ length }).map((_, index) => index);
  }
  return [0, Math.floor((length - 1) / 2), length - 1];
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
      padding: 16,
      paddingBottom: 28
    },
    header: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between',
      paddingTop: 6
    },
    title: {
      color: theme.foreground,
      fontSize: 24,
      fontWeight: '800'
    },
    topSegmented: {
      backgroundColor: theme.secondary,
      borderRadius: 18,
      flexDirection: 'row',
      padding: 3,
      width: 128
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
      width: 108
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
    transactionItem: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 10,
      paddingVertical: 10
    },
    categoryIcon: {
      alignItems: 'center',
      borderRadius: 20,
      height: 40,
      justifyContent: 'center',
      width: 40
    },
    categoryIconText: {
      color: '#ffffff',
      fontSize: 15,
      fontWeight: '800'
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
      borderRadius: 18,
      height: 36,
      justifyContent: 'center',
      width: 36
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
      maxHeight: '86%'
    },
    sheetContent: {
      gap: 12,
      padding: 16,
      paddingBottom: 28
    },
    sheetHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    sheetTitle: {
      color: theme.foreground,
      fontSize: 22,
      fontWeight: '800'
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
    choiceChip: {
      backgroundColor: theme.secondary,
      borderColor: theme.border,
      borderRadius: 16,
      borderWidth: 1,
      paddingHorizontal: 12,
      paddingVertical: 8
    },
    choiceChipSelected: {
      backgroundColor: theme.primary,
      borderColor: theme.primary
    },
    choiceText: {
      color: theme.foreground,
      fontSize: 13,
      fontWeight: '700'
    },
    choiceTextSelected: {
      color: theme.primaryForeground
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
  choiceWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8
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
    overflow: 'hidden'
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
