import { Redirect, router } from 'expo-router';
import { ChevronLeft, ChevronRight, Edit3, Trash2, X } from 'lucide-react-native';
import { useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, Alert, Animated, KeyboardAvoidingView, Modal, PanResponder, Platform, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Card, CardContent, Input, Separator, Text } from '@/components/ui';
import { SubmitActionButton } from '@/components/ui/SubmitActionButton';
import { useTheme } from '@/core/design/theme';
import { formatMoney, formatSignedMoney } from '@/features/home';
import { useAuthStore } from '@/stores/authStore';

import type { AccountItem, AccountLedgerItem, AccountRequest } from '../api/accountTypes';
import { useAccount } from '../hooks/useAccount';

type PeriodMode = 'week' | 'month' | 'year';

interface AccountEditForm {
  id: string;
  name: string;
  type: string;
  initialBalance: string;
  balance: string;
  currency: string;
  remark: string;
}

const accountTypes = [
  { label: '现金', value: 'CASH' },
  { label: '银行卡', value: 'BANK_CARD' },
  { label: '信用卡', value: 'CREDIT_CARD' },
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信', value: 'WECHAT' },
  { label: '其他', value: 'OTHER' }
];

export function AccountDetailScreen({ accountId }: { accountId: string }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const { isHydrated, isLoggedIn, restoreToken } = useAuthStore();
  const [periodMode, setPeriodMode] = useState<PeriodMode>('month');
  const [anchorDate, setAnchorDate] = useState(() => formatDate(new Date()));
  const [selectedLedger, setSelectedLedger] = useState<AccountLedgerItem | null>(null);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<AccountEditForm>(() => emptyEditForm(accountId));
  const [formError, setFormError] = useState<string | null>(null);
  const range = useMemo(() => periodRange(anchorDate, periodMode), [anchorDate, periodMode]);

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  const {
    listQuery,
    ledgerQuery,
    flowStatisticsQuery,
    updateMutation,
    deleteLedgerItemMutation
  } = useAccount(
    isLoggedIn,
    accountId,
    { pageNo: 1, pageSize: 80, startDate: range.startDate, endDate: range.endDate },
    { startDate: range.startDate, endDate: range.endDate }
  );

  const account = ledgerQuery.data?.account ?? listQuery.data?.find((item) => String(item.id) === accountId) ?? null;
  const summary = ledgerQuery.data?.summary;
  const flowStats = flowStatisticsQuery.data;
  const ledgerItems = ledgerQuery.data?.page?.records ?? ledgerQuery.data?.page?.list ?? [];
  const groupedLedger = useMemo(() => groupLedgerByDay(ledgerItems), [ledgerItems]);

  useEffect(() => {
    if (account && !editing) {
      setForm(formFromAccount(account));
    }
  }, [account, editing]);

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

  function shiftPeriod(offset: number) {
    setAnchorDate((current) => shiftDate(current, periodMode, offset));
  }

  function openEdit() {
    if (account) {
      setForm(formFromAccount(account));
    }
    setFormError(null);
    setEditing(true);
  }

  async function submitEdit() {
    const payload = buildAccountRequest(form);
    if ('error' in payload) {
      setFormError(payload.error);
      return;
    }
    try {
      await updateMutation.mutateAsync({ id: form.id, data: payload.data });
      setEditing(false);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '保存账户失败');
    }
  }

  function confirmDelete(item: AccountLedgerItem) {
    if (item.sourceType === 'ADJUSTMENT') {
      Alert.alert('暂不支持删除', '余额修正暂无删除接口，不能在移动端直接删除。');
      return;
    }
    const title = item.sourceType === 'INVESTMENT' ? '撤销投资交易' : '删除流水';
    const message = item.sourceType === 'INVESTMENT'
      ? '后端会保留投资交易审计记录，并将这笔交易标记为撤销。'
      : '删除后会回滚账户余额影响，确定删除吗？';
    Alert.alert(title, message, [
      { text: '取消', style: 'cancel' },
      {
        text: item.sourceType === 'INVESTMENT' ? '撤销' : '删除',
        style: 'destructive',
        onPress: async () => {
          try {
            await deleteLedgerItemMutation.mutateAsync(item);
          } catch (error) {
            Alert.alert('操作失败', error instanceof Error ? error.message : '账户明细处理失败');
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
          <Pressable style={styles.iconButton} onPress={() => router.back()}>
            <ChevronLeft color={theme.foreground} size={21} />
          </Pressable>
          <View style={styles.headerCopy}>
            <Text style={styles.title}>{account?.name || '账户详情'}</Text>
            <Text variant="muted">{accountTypeLabel(account?.type)} · {account?.currency || 'CNY'}</Text>
          </View>
          <Pressable style={styles.iconButton} onPress={openEdit}>
            <Edit3 color={theme.foreground} size={18} />
          </Pressable>
        </View>

        {ledgerQuery.isError || flowStatisticsQuery.isError ? <ErrorCard message="账户详情加载失败，请稍后重试。" /> : null}
        {ledgerQuery.isLoading || flowStatisticsQuery.isLoading ? <ActivityIndicator color={theme.primary} /> : null}

        <Card>
          <CardContent style={styles.summaryCard}>
            <View>
              <Text variant="muted">总金额</Text>
              <Text style={styles.totalAmount} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>{formatMoney(summary?.currentBalance ?? account?.balance)}</Text>
            </View>
            <View style={styles.summaryGrid}>
              <MiniStat label="流入" value={formatMoney(summary?.totalInflow)} />
              <MiniStat label="流出" value={formatMoney(summary?.totalOutflow)} />
              <MiniStat label="净流入" value={formatSignedMoney(summary?.netInflow)} />
            </View>
            <View style={styles.summaryGrid}>
              <MiniStat label="收入" value={formatMoney(flowStats?.incomeAmount)} />
              <MiniStat label="支出" value={formatMoney(flowStats?.expenseAmount)} />
              <MiniStat label="明细数" value={`${summary?.transactionCount ?? ledgerItems.length} 笔`} />
            </View>
          </CardContent>
        </Card>

        <Card>
          <CardContent style={styles.periodCard}>
            <View style={styles.periodSegmented}>
              {(['week', 'month', 'year'] as const).map((mode) => (
                <Pressable key={mode} style={[styles.periodButton, periodMode === mode ? styles.periodButtonActive : null]} onPress={() => setPeriodMode(mode)}>
                  <Text style={[styles.periodText, periodMode === mode ? styles.periodTextActive : null]}>{periodLabel(mode)}</Text>
                </Pressable>
              ))}
            </View>
            <View style={styles.periodHeader}>
              <Pressable style={styles.roundButton} onPress={() => shiftPeriod(-1)}>
                <ChevronLeft color={theme.foreground} size={18} />
              </Pressable>
              <Text style={styles.periodTitle}>{formatPeriodTitle(range, periodMode)}</Text>
              <Pressable style={styles.roundButton} onPress={() => shiftPeriod(1)}>
                <ChevronRight color={theme.foreground} size={18} />
              </Pressable>
            </View>
          </CardContent>
        </Card>

        <Card>
          <CardContent style={styles.ledgerCard}>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>账户明细</Text>
              <Text variant="muted">按天归纳</Text>
            </View>
            {groupedLedger.length > 0 ? (
              groupedLedger.map((group) => (
                <View key={group.date} style={styles.dayGroup}>
                  <View style={styles.dayHeader}>
                    <Text style={styles.dayTitle}>{formatDayTitle(group.date)}</Text>
                    <Text variant="caption">{group.items.length} 笔</Text>
                  </View>
                  {group.items.map((item, index) => (
                    <View key={`${item.sourceType}-${item.id}`}>
                      <SwipeableLedgerRow
                        item={item}
                        deleting={deleteLedgerItemMutation.isPending}
                        onPress={() => setSelectedLedger(item)}
                        onDelete={() => confirmDelete(item)}
                      />
                      {index < group.items.length - 1 ? <Separator /> : null}
                    </View>
                  ))}
                </View>
              ))
            ) : (
              <Text variant="muted">当前时间范围没有账户明细。</Text>
            )}
          </CardContent>
        </Card>
      </ScrollView>

      <AccountEditSheet
        visible={editing}
        form={form}
        formError={formError}
        loading={updateMutation.isPending}
        onClose={() => {
          setEditing(false);
          setFormError(null);
        }}
        onChange={(patch) => {
          setForm((current) => ({ ...current, ...patch }));
          setFormError(null);
        }}
        onSubmit={submitEdit}
      />
      <LedgerDetailModal item={selectedLedger} onClose={() => setSelectedLedger(null)} />
    </SafeAreaView>
  );
}

function SwipeableLedgerRow({
  item,
  deleting,
  onPress,
  onDelete
}: {
  item: AccountLedgerItem;
  deleting: boolean;
  onPress: () => void;
  onDelete: () => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const translateX = useRef(new Animated.Value(0)).current;
  const [opened, setOpened] = useState(false);
  const panResponder = useMemo(() => PanResponder.create({
    onMoveShouldSetPanResponder: (_, gesture) => Math.abs(gesture.dx) > 8 && Math.abs(gesture.dx) > Math.abs(gesture.dy),
    onPanResponderMove: (_, gesture) => {
      translateX.setValue(Math.max(-86, Math.min(0, gesture.dx + (opened ? -86 : 0))));
    },
    onPanResponderRelease: (_, gesture) => {
      const shouldOpen = gesture.dx < -36 || (opened && gesture.dx < 24);
      setOpened(shouldOpen);
      Animated.timing(translateX, {
        toValue: shouldOpen ? -86 : 0,
        duration: 180,
        useNativeDriver: true
      }).start();
    }
  }), [opened, translateX]);
  const deleteOpacity = translateX.interpolate({
    inputRange: [-86, -24, 0],
    outputRange: [1, 0.55, 0],
    extrapolate: 'clamp'
  });

  return (
    <View style={styles.swipeWrap}>
      <Animated.View style={[styles.deletePane, { opacity: deleteOpacity }]}>
        <Pressable disabled={deleting} style={styles.deleteButton} onPress={onDelete}>
          <Trash2 color={theme.destructiveForeground} size={18} />
          <Text style={styles.deleteText}>{item.sourceType === 'INVESTMENT' ? '撤销' : '删除'}</Text>
        </Pressable>
      </Animated.View>
      <Animated.View style={[styles.swipeContent, { transform: [{ translateX }] }]} {...panResponder.panHandlers}>
        <Pressable style={styles.ledgerRow} onPress={onPress}>
          <View style={styles.ledgerIcon}>
            <Text style={styles.ledgerIconText}>{ledgerInitial(item)}</Text>
          </View>
          <View style={styles.ledgerInfo}>
            <Text style={styles.ledgerTitle} numberOfLines={1}>{item.title || item.categoryName || item.assetName || ledgerTypeLabel(item.bizType)}</Text>
            <Text variant="caption">{ledgerTypeLabel(item.bizType)} · {sourceTypeLabel(item.sourceType)} · {formatShortTime(item.transactionTime)}</Text>
          </View>
          <Text style={styles.ledgerAmount} numberOfLines={1}>{formatLedgerAmount(item)}</Text>
        </Pressable>
      </Animated.View>
    </View>
  );
}

function AccountEditSheet({
  visible,
  form,
  formError,
  loading,
  onClose,
  onChange,
  onSubmit
}: {
  visible: boolean;
  form: AccountEditForm;
  formError: string | null;
  loading: boolean;
  onClose: () => void;
  onChange: (patch: Partial<AccountEditForm>) => void;
  onSubmit: () => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const [submitPressed, setSubmitPressed] = useState(false);

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.modalRoot}>
        <Pressable style={styles.modalBackdrop} onPress={onClose} />
        <View style={styles.sheet}>
          <ScrollView style={styles.sheetScroller} contentContainerStyle={styles.sheetContent} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>编辑账户</Text>
              <Pressable onPress={onClose}>
                <X color={theme.foreground} size={24} />
              </Pressable>
            </View>
            <Input label="账户名称" value={form.name} onChangeText={(name) => onChange({ name })} />
            <Text style={styles.sheetFieldLabel}>账户类型</Text>
            <View style={styles.choiceWrap}>
              {accountTypes.map((item) => (
                <Pressable key={item.value} style={[styles.choiceChip, form.type === item.value ? styles.choiceChipSelected : null]} onPress={() => onChange({ type: item.value })}>
                  <Text style={[styles.choiceText, form.type === item.value ? styles.choiceTextSelected : null]}>{item.label}</Text>
                </Pressable>
              ))}
            </View>
            <Input label="余额" keyboardType="decimal-pad" value={form.balance} onChangeText={(balance) => onChange({ balance })} />
          </ScrollView>
          <View style={styles.sheetFooter}>
            {formError ? <Text variant="error">{formError}</Text> : null}
            <SubmitActionButton label={loading ? '保存中' : '保存'} loading={loading} pressed={submitPressed} onPressedChange={setSubmitPressed} onPress={onSubmit} />
          </View>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

function LedgerDetailModal({ item, onClose }: { item: AccountLedgerItem | null; onClose: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Modal visible={Boolean(item)} animationType="slide" transparent onRequestClose={onClose}>
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
            {item ? (
              <>
                <DetailMetric label="标题" value={item.title || item.categoryName || item.assetName || ledgerTypeLabel(item.bizType)} />
                <DetailMetric label="金额" value={formatLedgerAmount(item)} />
                <DetailMetric label="类型" value={ledgerTypeLabel(item.bizType)} />
                <DetailMetric label="来源" value={sourceTypeLabel(item.sourceType)} />
                <DetailMetric label="账户" value={item.accountName || '--'} />
                {item.relatedAccountName ? <DetailMetric label="关联账户" value={item.relatedAccountName} /> : null}
                {item.categoryName ? <DetailMetric label="分类" value={item.categoryName} /> : null}
                {item.assetName || item.symbol ? <DetailMetric label="资产" value={[item.assetName, item.symbol].filter(Boolean).join(' · ')} /> : null}
                <DetailMetric label="状态" value={ledgerStatusLabel(item.status)} />
                <DetailMetric label="时间" value={formatFullDateTime(item.transactionTime)} />
                <DetailMetric label="备注" value={item.note || '--'} />
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
    <View style={styles.detailMetric}>
      <Text style={styles.detailLabel}>{label}</Text>
      <Text variant="muted" style={styles.detailValue}>{value}</Text>
    </View>
  );
}

function MiniStat({ label, value }: { label: string; value: string }) {
  return (
    <View style={stylesStatic.miniStat}>
      <Text variant="muted">{label}</Text>
      <Text style={stylesStatic.miniValue} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>{value}</Text>
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

function emptyEditForm(accountId: string): AccountEditForm {
  return {
    id: accountId,
    name: '',
    type: 'BANK_CARD',
    initialBalance: '0',
    balance: '',
    currency: 'CNY',
    remark: ''
  };
}

function formFromAccount(account: AccountItem): AccountEditForm {
  return {
    id: String(account.id),
    name: account.name ?? '',
    type: account.type ?? 'BANK_CARD',
    initialBalance: account.initialBalance === null || account.initialBalance === undefined ? '0' : String(account.initialBalance),
    balance: account.balance === null || account.balance === undefined ? '' : String(account.balance),
    currency: account.currency ?? 'CNY',
    remark: account.remark ?? ''
  };
}

function buildAccountRequest(form: AccountEditForm): { data: AccountRequest } | { error: string } {
  if (!form.name.trim()) {
    return { error: '账户名称不能为空' };
  }
  const balance = Number(form.balance);
  if (!Number.isFinite(balance)) {
    return { error: '账户余额必须是有效数字' };
  }
  const initialBalance = Number(form.initialBalance);
  if (!Number.isFinite(initialBalance)) {
    return { error: '账户初始数据异常，请重新进入页面' };
  }
  return {
    data: {
      name: form.name.trim(),
      type: form.type,
      initialBalance: String(initialBalance),
      balance: String(balance),
      currency: form.currency,
      status: 1,
      sortOrder: 0,
      remark: form.remark.trim() || null
    }
  };
}

function groupLedgerByDay(items: AccountLedgerItem[]) {
  const groups = items.reduce<Record<string, AccountLedgerItem[]>>((map, item) => {
    const date = item.transactionTime?.slice(0, 10) || 'unknown';
    map[date] = map[date] || [];
    map[date].push(item);
    return map;
  }, {});
  return Object.entries(groups).map(([date, groupItems]) => ({ date, items: groupItems }));
}

function accountTypeLabel(type?: string | null) {
  const item = accountTypes.find((candidate) => candidate.value === type);
  return item?.label || type || '账户';
}

function ledgerInitial(item: AccountLedgerItem) {
  return (item.title || item.categoryName || item.assetName || ledgerTypeLabel(item.bizType)).slice(0, 1);
}

function ledgerTypeLabel(type?: string | null) {
  const labels: Record<string, string> = {
    INCOME: '收入',
    EXPENSE: '支出',
    REFUND: '退款',
    TRANSFER: '转账',
    TRANSFER_IN: '转入',
    TRANSFER_OUT: '转出',
    BUY: '买入',
    SELL: '卖出',
    INVEST_BUY: '投资买入',
    INVEST_SELL: '投资卖出',
    BALANCE_ADJUSTMENT: '余额修正'
  };
  return type ? labels[type] || type : '资金明细';
}

function sourceTypeLabel(type?: string | null) {
  const labels: Record<string, string> = {
    TRANSACTION: '记账流水',
    INVESTMENT: '投资交易',
    ADJUSTMENT: '余额修正'
  };
  return type ? labels[type] || type : '账户明细';
}

function ledgerStatusLabel(status?: string | null) {
  const labels: Record<string, string> = {
    NORMAL: '正常',
    CONFIRMED: '已确认',
    PENDING_CONFIRM: '待确认',
    REVOKED: '已撤销',
    CANCELLED: '已取消',
    '1': '正常',
    '0': '停用'
  };
  return status ? labels[status] || status : '--';
}

function formatLedgerAmount(item: AccountLedgerItem) {
  if (item.amount === null || item.amount === undefined || Number.isNaN(item.amount)) {
    return '--';
  }
  return formatSignedMoney(item.amount);
}

function formatShortTime(value?: string | null) {
  if (!value) {
    return '--';
  }
  return value.replace('T', ' ').slice(11, 16);
}

function formatFullDateTime(value?: string | null) {
  if (!value) {
    return '--';
  }
  return value.replace('T', ' ').slice(0, 16);
}

function formatDayTitle(date: string) {
  if (date === 'unknown') {
    return '未记录日期';
  }
  const today = formatDate(new Date());
  const yesterday = formatDate(addDays(new Date(), -1));
  if (date === today) {
    return '今天';
  }
  if (date === yesterday) {
    return '昨天';
  }
  return date.slice(5).replace('-', '月') + '日';
}

function periodLabel(mode: PeriodMode) {
  if (mode === 'week') return '周';
  if (mode === 'year') return '年';
  return '月';
}

function formatPeriodTitle(range: { startDate: string; endDate: string }, mode: PeriodMode) {
  if (mode === 'year') {
    return `${range.startDate.slice(0, 4)}年`;
  }
  if (mode === 'month') {
    return `${range.startDate.slice(0, 4)}年${Number(range.startDate.slice(5, 7))}月`;
  }
  return `${range.startDate.slice(5)} 至 ${range.endDate.slice(5)}`;
}

function periodRange(dateText: string, mode: PeriodMode) {
  const date = parseDate(dateText);
  if (mode === 'week') {
    const day = date.getDay() || 7;
    const start = addDays(date, 1 - day);
    const end = addDays(start, 6);
    return { startDate: formatDate(start), endDate: formatDate(end) };
  }
  if (mode === 'year') {
    const year = date.getFullYear();
    return { startDate: `${year}-01-01`, endDate: `${year}-12-31` };
  }
  const first = new Date(date.getFullYear(), date.getMonth(), 1);
  const last = new Date(date.getFullYear(), date.getMonth() + 1, 0);
  return { startDate: formatDate(first), endDate: formatDate(last) };
}

function shiftDate(dateText: string, mode: PeriodMode, offset: number) {
  const date = parseDate(dateText);
  if (mode === 'week') {
    return formatDate(addDays(date, offset * 7));
  }
  if (mode === 'year') {
    return formatDate(new Date(date.getFullYear() + offset, date.getMonth(), date.getDate()));
  }
  return formatDate(new Date(date.getFullYear(), date.getMonth() + offset, date.getDate()));
}

function parseDate(value: string) {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function addDays(date: Date, offset: number) {
  const next = new Date(date);
  next.setDate(date.getDate() + offset);
  return next;
}

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
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
  miniStat: {
    flex: 1,
    gap: 4,
    minWidth: 0
  },
  miniValue: {
    fontSize: 16,
    fontWeight: '800'
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
    headerCopy: {
      flex: 1,
      gap: 3,
      minWidth: 0
    },
    title: {
      fontSize: 24,
      fontWeight: '900'
    },
    iconButton: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      height: 36,
      justifyContent: 'center',
      width: 36
    },
    summaryCard: {
      gap: 16
    },
    totalAmount: {
      fontSize: 34,
      fontWeight: '900',
      marginTop: 4
    },
    summaryGrid: {
      flexDirection: 'row',
      gap: 12
    },
    periodCard: {
      gap: 12
    },
    periodSegmented: {
      backgroundColor: theme.secondary,
      borderRadius: 18,
      flexDirection: 'row',
      padding: 4
    },
    periodButton: {
      alignItems: 'center',
      borderRadius: 14,
      flex: 1,
      minHeight: 34,
      justifyContent: 'center'
    },
    periodButtonActive: {
      backgroundColor: theme.foreground
    },
    periodText: {
      color: theme.foreground,
      fontSize: 14,
      fontWeight: '800'
    },
    periodTextActive: {
      color: theme.background
    },
    periodHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    periodTitle: {
      fontSize: 17,
      fontWeight: '900'
    },
    roundButton: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 16,
      height: 32,
      justifyContent: 'center',
      width: 32
    },
    ledgerCard: {
      gap: 14
    },
    sectionHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    sectionTitle: {
      fontSize: 18,
      fontWeight: '900'
    },
    dayGroup: {
      gap: 8
    },
    dayHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between',
      paddingTop: 4
    },
    dayTitle: {
      fontSize: 15,
      fontWeight: '900'
    },
    swipeWrap: {
      overflow: 'hidden',
      position: 'relative'
    },
    deletePane: {
      bottom: 0,
      justifyContent: 'center',
      position: 'absolute',
      right: 0,
      top: 0,
      width: 86
    },
    deleteButton: {
      alignItems: 'center',
      alignSelf: 'stretch',
      backgroundColor: theme.destructive,
      borderRadius: 14,
      flexDirection: 'row',
      gap: 5,
      justifyContent: 'center',
      marginVertical: 8,
      minHeight: 46
    },
    deleteText: {
      color: theme.destructiveForeground,
      fontSize: 13,
      fontWeight: '900'
    },
    swipeContent: {
      backgroundColor: theme.card
    },
    ledgerRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12,
      paddingVertical: 12
    },
    ledgerIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 17,
      height: 34,
      justifyContent: 'center',
      width: 34
    },
    ledgerIconText: {
      fontSize: 15,
      fontWeight: '900'
    },
    ledgerInfo: {
      flex: 1,
      gap: 2,
      minWidth: 0
    },
    ledgerTitle: {
      fontSize: 15,
      fontWeight: '800'
    },
    ledgerAmount: {
      fontSize: 15,
      fontWeight: '900',
      maxWidth: 112,
      textAlign: 'right'
    },
    modalRoot: {
      flex: 1,
      justifyContent: 'flex-end'
    },
    modalBackdrop: {
      ...StyleSheet.absoluteFillObject,
      backgroundColor: 'rgba(0,0,0,0.2)'
    },
    sheet: {
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderTopLeftRadius: 22,
      borderTopRightRadius: 22,
      borderWidth: 1,
      maxHeight: '86%'
    },
    sheetScroller: {
      maxHeight: '100%'
    },
    sheetContent: {
      gap: 14,
      padding: 18,
      paddingBottom: 12
    },
    sheetFooter: {
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderTopWidth: StyleSheet.hairlineWidth,
      gap: 10,
      padding: 18,
      paddingTop: 12
    },
    sheetHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    sheetTitle: {
      fontSize: 20,
      fontWeight: '900'
    },
    sheetFieldLabel: {
      fontSize: 14,
      fontWeight: '800'
    },
    choiceWrap: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      gap: 8
    },
    choiceChip: {
      borderColor: theme.border,
      borderRadius: 999,
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
    },
    detailMetric: {
      borderColor: theme.border,
      borderRadius: 12,
      borderWidth: 1,
      gap: 6,
      padding: 12
    },
    detailLabel: {
      fontSize: 13,
      fontWeight: '900'
    },
    detailValue: {
      lineHeight: 20
    }
  });
