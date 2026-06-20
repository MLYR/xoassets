import { Redirect } from 'expo-router';
import { ChevronRight, Edit3, Plus, SlidersHorizontal, WalletCards, X } from 'lucide-react-native';
import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button, Card, CardContent, Input, Separator, Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';
import { formatMoney, formatPercent, formatSignedMoney } from '@/features/home';
import { useAuthStore } from '@/stores/authStore';

import type { AccountItem, AccountLedgerItem, AccountRequest } from '../api/accountTypes';
import { useAccount } from '../hooks/useAccount';

type AccountSheetMode = 'create' | 'edit' | 'adjust';

interface AccountFormState {
  id?: string;
  name: string;
  type: string;
  initialBalance: string;
  balance: string;
  currency: string;
  remark: string;
}

interface AdjustmentFormState {
  afterBalance: string;
  reason: string;
}

const accountTypes = [
  { label: '现金', value: 'CASH' },
  { label: '银行卡', value: 'BANK_CARD' },
  { label: '信用卡', value: 'CREDIT_CARD' },
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信', value: 'WECHAT' },
  { label: '其他', value: 'OTHER' }
];

export function AccountScreen({ initialCreate = false }: { initialCreate?: boolean } = {}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const { isHydrated, isLoggedIn, restoreToken } = useAuthStore();
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
  const [sheetMode, setSheetMode] = useState<AccountSheetMode | null>(null);
  const [form, setForm] = useState<AccountFormState>(() => createEmptyForm());
  const [adjustForm, setAdjustForm] = useState<AdjustmentFormState>({ afterBalance: '', reason: '' });
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  useEffect(() => {
    if (initialCreate) {
      setForm(createEmptyForm());
      setAdjustForm({ afterBalance: '', reason: '' });
      setFormError(null);
      setSheetMode('create');
    }
  }, [initialCreate]);

  const {
    overviewQuery,
    listQuery,
    ledgerQuery,
    flowStatisticsQuery,
    createMutation,
    updateMutation,
    adjustBalanceMutation
  } = useAccount(isLoggedIn, selectedAccountId);

  const overview = overviewQuery.data;
  const accounts = overview?.accounts ?? listQuery.data ?? [];
  const selectedAccount = accounts.find((item) => String(item.id) === selectedAccountId) ?? accounts[0] ?? null;
  const ledgerItems = ledgerQuery.data?.page?.records ?? ledgerQuery.data?.page?.list ?? [];
  const flowStats = flowStatisticsQuery.data;
  const groupedAccounts = useMemo(() => groupAccounts(accounts), [accounts]);
  const isSubmitting = createMutation.isPending || updateMutation.isPending || adjustBalanceMutation.isPending;

  useEffect(() => {
    if (!selectedAccountId && accounts.length > 0) {
      setSelectedAccountId(String(accounts[0].id));
    }
  }, [accounts, selectedAccountId]);

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

  function openCreate() {
    setForm(createEmptyForm());
    setAdjustForm({ afterBalance: '', reason: '' });
    setFormError(null);
    setSheetMode('create');
  }

  function openEdit(account: AccountItem) {
    setForm({
      id: String(account.id),
      name: account.name ?? '',
      type: account.type ?? 'BANK_CARD',
      initialBalance: account.initialBalance === null || account.initialBalance === undefined ? '' : String(account.initialBalance),
      balance: account.balance === null || account.balance === undefined ? '' : String(account.balance),
      currency: account.currency ?? 'CNY',
      remark: account.remark ?? ''
    });
    setAdjustForm({ afterBalance: '', reason: '' });
    setFormError(null);
    setSheetMode('edit');
  }

  function openAdjust(account: AccountItem) {
    setForm({
      id: String(account.id),
      name: account.name ?? '',
      type: account.type ?? 'BANK_CARD',
      initialBalance: account.initialBalance === null || account.initialBalance === undefined ? '' : String(account.initialBalance),
      balance: account.balance === null || account.balance === undefined ? '' : String(account.balance),
      currency: account.currency ?? 'CNY',
      remark: account.remark ?? ''
    });
    setAdjustForm({ afterBalance: account.balance === null || account.balance === undefined ? '' : String(account.balance), reason: '' });
    setFormError(null);
    setSheetMode('adjust');
  }

  function closeSheet() {
    setSheetMode(null);
    setFormError(null);
  }

  async function submitAccount() {
    const payload = buildAccountRequest(form);
    if ('error' in payload) {
      setFormError(payload.error);
      return;
    }

    try {
      if (sheetMode === 'edit' && form.id) {
        await updateMutation.mutateAsync({ id: form.id, data: payload.data });
      } else {
        const created = await createMutation.mutateAsync(payload.data);
        setSelectedAccountId(String(created.id));
      }
      closeSheet();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '保存账户失败');
    }
  }

  async function submitAdjustment() {
    if (!form.id) {
      setFormError('请选择账户');
      return;
    }
    const amount = Number(adjustForm.afterBalance);
    if (!Number.isFinite(amount)) {
      setFormError('修正后余额必须是有效数字');
      return;
    }

    try {
      await adjustBalanceMutation.mutateAsync({
        id: form.id,
        data: {
          afterBalance: String(amount),
          reason: adjustForm.reason.trim() || null,
          bizDate: formatDate(new Date()),
          bizTime: `${formatDate(new Date())}T${formatTime(new Date())}:00`
        }
      });
      closeSheet();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '余额修正失败');
    }
  }

  return (
    <SafeAreaView style={styles.page}>
      <GridBackdrop color={theme.border} />
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <View>
            <Text style={styles.title}>账户</Text>
            <Text variant="muted">现金、银行卡与信用账户统一管理</Text>
          </View>
          <Pressable style={styles.headerButton} onPress={openCreate}>
            <Plus color={theme.primaryForeground} size={20} strokeWidth={2.4} />
          </Pressable>
        </View>

        {(overviewQuery.isError || listQuery.isError) ? <ErrorCard message="账户数据加载失败，请稍后重试。" /> : null}
        {(overviewQuery.isLoading || listQuery.isLoading) ? <ActivityIndicator color={theme.primary} /> : null}

        <Card>
          <CardContent style={styles.summaryCard}>
            <View style={styles.summaryTop}>
              <View>
                <Text variant="muted">账户总资产</Text>
                <Text style={styles.totalAsset} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>{formatMoney(overview?.totalAsset)}</Text>
              </View>
              <View style={styles.countPill}>
                <WalletCards color={theme.foreground} size={17} strokeWidth={2.2} />
                <Text style={styles.countText}>{overview?.accountCount ?? accounts.length} 个账户</Text>
              </View>
            </View>
            <View style={styles.summaryGrid}>
              <MiniStat label="非信用资产" value={formatMoney(overview?.nonCreditAssetTotal)} />
              <MiniStat label="较上月" value={overview?.compareAvailable ? formatSignedMoney(overview?.lastMonthChangeAmount) : '--'} subValue={overview?.compareAvailable ? formatPercent(overview?.lastMonthChangeRate) : undefined} />
              <MiniStat label="非零账户" value={`${overview?.nonZeroAccountCount ?? accounts.filter((item) => (item.balance ?? 0) !== 0).length} 个`} />
            </View>
          </CardContent>
        </Card>

        {groupedAccounts.map((group) => (
          <Card key={group.name}>
            <CardContent style={styles.groupCard}>
              <View style={styles.sectionHeader}>
                <Text style={styles.sectionTitle}>{group.name}</Text>
                <Text variant="muted">{group.items.length} 个</Text>
              </View>
              {group.items.map((account, index) => (
                <View key={String(account.id)}>
                  <Pressable style={styles.accountRow} onPress={() => setSelectedAccountId(String(account.id))}>
                    <View style={styles.accountIcon}>
                      <Text style={styles.accountIconText}>{accountInitial(account)}</Text>
                    </View>
                    <View style={styles.accountInfo}>
                      <Text style={styles.accountName}>{account.name || '未命名账户'}</Text>
                      <Text variant="caption">{accountTypeLabel(account.type)} · {account.currency || 'CNY'}{account.tagText ? ` · ${account.tagText}` : ''}</Text>
                    </View>
                    <View style={styles.accountRight}>
                      <Text style={styles.accountBalance} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>{formatMoney(account.balance)}</Text>
                      <ChevronRight color={theme.mutedForeground} size={18} />
                    </View>
                  </Pressable>
                  {index < group.items.length - 1 ? <Separator /> : null}
                </View>
              ))}
            </CardContent>
          </Card>
        ))}

        {selectedAccount ? (
          <Card>
            <CardContent style={styles.detailCard}>
              <View style={styles.sectionHeader}>
                <View>
                  <Text style={styles.sectionTitle}>{selectedAccount.name || '账户详情'}</Text>
                  <Text variant="muted">{accountTypeLabel(selectedAccount.type)} · 当前余额 {formatMoney(selectedAccount.balance)}</Text>
                </View>
                <View style={styles.actionRow}>
                  <Pressable style={styles.iconButton} onPress={() => openEdit(selectedAccount)}>
                    <Edit3 color={theme.foreground} size={18} />
                  </Pressable>
                  <Pressable style={styles.iconButton} onPress={() => openAdjust(selectedAccount)}>
                    <SlidersHorizontal color={theme.foreground} size={18} />
                  </Pressable>
                </View>
              </View>

              <View style={styles.summaryGrid}>
                <MiniStat label="收入" value={formatMoney(flowStats?.incomeAmount)} />
                <MiniStat label="支出" value={formatMoney(flowStats?.expenseAmount)} />
                <MiniStat label="净流入" value={formatSignedMoney(flowStats?.netFlowAmount)} />
              </View>
              <View style={styles.summaryGrid}>
                <MiniStat label="投资买入" value={formatMoney(flowStats?.investmentBuyAmount)} />
                <MiniStat label="投资卖出" value={formatMoney(flowStats?.investmentSellAmount)} />
                <MiniStat label="余额修正" value={formatSignedMoney(flowStats?.adjustmentAmount)} />
              </View>

              <Text style={styles.sectionTitle}>账户流水</Text>
              {ledgerQuery.isLoading ? <ActivityIndicator color={theme.primary} /> : null}
              {ledgerQuery.isError ? <Text variant="error">账户流水加载失败。</Text> : null}
              {ledgerItems.length > 0 ? (
                ledgerItems.slice(0, 12).map((item, index) => (
                  <View key={`${item.sourceType}-${item.id}`}>
                    <LedgerRow item={item} />
                    {index < Math.min(ledgerItems.length, 12) - 1 ? <Separator /> : null}
                  </View>
                ))
              ) : (
                <Text variant="muted">暂无账户流水。</Text>
              )}
            </CardContent>
          </Card>
        ) : null}
      </ScrollView>

      <AccountSheet
        visible={sheetMode !== null}
        mode={sheetMode}
        form={form}
        adjustForm={adjustForm}
        formError={formError}
        isSubmitting={isSubmitting}
        onClose={closeSheet}
        onFormChange={(patch) => {
          setForm((current) => ({ ...current, ...patch }));
          setFormError(null);
        }}
        onAdjustChange={(patch) => {
          setAdjustForm((current) => ({ ...current, ...patch }));
          setFormError(null);
        }}
        onSubmit={sheetMode === 'adjust' ? submitAdjustment : submitAccount}
      />
    </SafeAreaView>
  );
}

function AccountSheet({
  visible,
  mode,
  form,
  adjustForm,
  formError,
  isSubmitting,
  onClose,
  onFormChange,
  onAdjustChange,
  onSubmit
}: {
  visible: boolean;
  mode: AccountSheetMode | null;
  form: AccountFormState;
  adjustForm: AdjustmentFormState;
  formError: string | null;
  isSubmitting: boolean;
  onClose: () => void;
  onFormChange: (patch: Partial<AccountFormState>) => void;
  onAdjustChange: (patch: Partial<AdjustmentFormState>) => void;
  onSubmit: () => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.modalRoot}>
        <Pressable style={styles.modalBackdrop} onPress={onClose} />
        <View style={styles.sheet}>
          <ScrollView contentContainerStyle={styles.sheetContent} showsVerticalScrollIndicator={false}>
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>{mode === 'create' ? '新增账户' : mode === 'edit' ? '编辑账户' : '余额修正'}</Text>
              <Pressable onPress={onClose}>
                <X color={theme.foreground} size={24} />
              </Pressable>
            </View>

            {mode === 'adjust' ? (
              <>
                <Input label="修正后余额" keyboardType="decimal-pad" value={adjustForm.afterBalance} onChangeText={(afterBalance) => onAdjustChange({ afterBalance })} />
                <Input label="修正原因" placeholder="例如：补录历史余额" value={adjustForm.reason} onChangeText={(reason) => onAdjustChange({ reason })} />
              </>
            ) : (
              <>
                <Input label="账户名称" value={form.name} onChangeText={(name) => onFormChange({ name })} />
                <Text style={styles.sheetFieldLabel}>账户类型</Text>
                <View style={styles.choiceWrap}>
                  {accountTypes.map((item) => (
                    <Pressable key={item.value} style={[styles.choiceChip, form.type === item.value ? styles.choiceChipSelected : null]} onPress={() => onFormChange({ type: item.value })}>
                      <Text style={[styles.choiceText, form.type === item.value ? styles.choiceTextSelected : null]}>{item.label}</Text>
                    </Pressable>
                  ))}
                </View>
                <Input label="初始余额" keyboardType="decimal-pad" value={form.initialBalance} onChangeText={(initialBalance) => onFormChange({ initialBalance })} />
                <Input label="当前余额" keyboardType="decimal-pad" value={form.balance} onChangeText={(balance) => onFormChange({ balance })} />
                <Input label="币种" value={form.currency} onChangeText={(currency) => onFormChange({ currency })} />
                <Input label="备注" placeholder="可选" value={form.remark} onChangeText={(remark) => onFormChange({ remark })} />
              </>
            )}

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

function MiniStat({ label, value, subValue }: { label: string; value: string; subValue?: string }) {
  return (
    <View style={stylesStatic.miniStat}>
      <Text variant="muted">{label}</Text>
      <Text style={stylesStatic.miniValue} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>
        {value} {subValue ? <Text variant="muted">{subValue}</Text> : null}
      </Text>
    </View>
  );
}

function LedgerRow({ item }: { item: AccountLedgerItem }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <View style={styles.ledgerRow}>
      <View style={styles.ledgerIcon}>
        <Text style={styles.accountIconText}>{ledgerInitial(item)}</Text>
      </View>
      <View style={styles.ledgerInfo}>
        <Text style={styles.ledgerTitle} numberOfLines={1}>{item.title || item.categoryName || item.assetName || ledgerTypeLabel(item.bizType)}</Text>
        <Text variant="caption">{ledgerTypeLabel(item.bizType)} · {formatShortDateTime(item.transactionTime)}</Text>
      </View>
      <Text style={styles.ledgerAmount}>{formatLedgerAmount(item)}</Text>
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

function createEmptyForm(): AccountFormState {
  return {
    name: '',
    type: 'BANK_CARD',
    initialBalance: '',
    balance: '',
    currency: 'CNY',
    remark: ''
  };
}

function buildAccountRequest(form: AccountFormState): { data: AccountRequest } | { error: string } {
  if (!form.name.trim()) {
    return { error: '账户名称不能为空' };
  }
  const initialBalance = Number(form.initialBalance);
  if (!Number.isFinite(initialBalance)) {
    return { error: '初始余额必须是有效数字' };
  }
  const balance = form.balance.trim() ? Number(form.balance) : initialBalance;
  if (!Number.isFinite(balance)) {
    return { error: '当前余额必须是有效数字' };
  }

  return {
    data: {
      name: form.name.trim(),
      type: form.type,
      initialBalance: String(initialBalance),
      balance: String(balance),
      currency: form.currency.trim() || 'CNY',
      status: 1,
      sortOrder: 0,
      remark: form.remark.trim() || null
    }
  };
}

function groupAccounts(accounts: AccountItem[]) {
  const groups = accounts.reduce<Record<string, AccountItem[]>>((map, account) => {
    const name = account.group || accountTypeLabel(account.type);
    map[name] = map[name] || [];
    map[name].push(account);
    return map;
  }, {});

  return Object.entries(groups).map(([name, items]) => ({ name, items }));
}

function accountTypeLabel(type?: string | null) {
  const item = accountTypes.find((candidate) => candidate.value === type);
  return item?.label || type || '账户';
}

function accountInitial(account: AccountItem) {
  return (account.name || accountTypeLabel(account.type)).slice(0, 1);
}

function ledgerInitial(item: AccountLedgerItem) {
  return (item.title || item.categoryName || item.assetName || ledgerTypeLabel(item.bizType)).slice(0, 1);
}

function ledgerTypeLabel(type?: string | null) {
  const labels: Record<string, string> = {
    INCOME: '收入',
    EXPENSE: '支出',
    TRANSFER: '转账',
    BUY: '投资买入',
    SELL: '投资卖出',
    BALANCE_ADJUSTMENT: '余额修正'
  };
  return type ? labels[type] || type : '资金明细';
}

function formatLedgerAmount(item: AccountLedgerItem) {
  const amount = item.amount;
  if (amount === null || amount === undefined || Number.isNaN(amount)) {
    return '--';
  }
  const prefix = item.bizType === 'EXPENSE' || item.bizType === 'BUY' ? '-' : '+';
  return `${prefix}${formatMoney(Math.abs(amount))}`;
}

function formatShortDateTime(value?: string | null) {
  if (!value) {
    return '--';
  }
  return value.replace('T', ' ').slice(5, 16);
}

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatTime(date: Date) {
  const hour = `${date.getHours()}`.padStart(2, '0');
  const minute = `${date.getMinutes()}`.padStart(2, '0');
  return `${hour}:${minute}`;
}

const stylesStatic = StyleSheet.create({
  gridLineVertical: {
    opacity: 0.14,
    position: 'absolute',
    top: 0,
    bottom: 0,
    width: 1
  },
  gridLineHorizontal: {
    left: 0,
    opacity: 0.14,
    position: 'absolute',
    right: 0,
    height: 1
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
      justifyContent: 'space-between',
      marginBottom: 4
    },
    title: {
      fontSize: 28,
      fontWeight: '900'
    },
    headerButton: {
      alignItems: 'center',
      backgroundColor: theme.primary,
      borderRadius: 20,
      height: 40,
      justifyContent: 'center',
      width: 40
    },
    summaryCard: {
      gap: 18
    },
    summaryTop: {
      alignItems: 'flex-start',
      flexDirection: 'row',
      gap: 12,
      justifyContent: 'space-between'
    },
    totalAsset: {
      fontSize: 36,
      fontWeight: '900',
      marginTop: 4
    },
    countPill: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 999,
      flexDirection: 'row',
      gap: 6,
      paddingHorizontal: 10,
      paddingVertical: 7
    },
    countText: {
      fontSize: 13,
      fontWeight: '700'
    },
    summaryGrid: {
      flexDirection: 'row',
      gap: 12
    },
    groupCard: {
      gap: 10
    },
    sectionHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    sectionTitle: {
      fontSize: 17,
      fontWeight: '800'
    },
    accountRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12,
      paddingVertical: 12
    },
    accountIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      height: 36,
      justifyContent: 'center',
      width: 36
    },
    accountIconText: {
      fontSize: 15,
      fontWeight: '900'
    },
    accountInfo: {
      flex: 1,
      gap: 3,
      minWidth: 0
    },
    accountName: {
      fontSize: 16,
      fontWeight: '800'
    },
    accountRight: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 4,
      maxWidth: 136
    },
    accountBalance: {
      fontSize: 16,
      fontWeight: '800'
    },
    detailCard: {
      gap: 16
    },
    actionRow: {
      flexDirection: 'row',
      gap: 8
    },
    iconButton: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 16,
      height: 34,
      justifyContent: 'center',
      width: 34
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
      fontWeight: '800'
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
    sheetContent: {
      gap: 14,
      padding: 18,
      paddingBottom: 28
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
    }
  });
