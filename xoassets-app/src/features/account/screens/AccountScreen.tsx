import { Redirect, router } from 'expo-router';
import { ChevronDown, ChevronLeft, ChevronRight, ChevronUp, Plus, WalletCards, X } from 'lucide-react-native';
import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, StyleSheet, Text as RNText, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Card, CardContent, Input, Separator, Text } from '@/components/ui';
import { SubmitActionButton as LedgerSubmitActionButton } from '@/components/ui/SubmitActionButton';
import { useTheme } from '@/core/design/theme';
import { formatMoney, formatPercent, formatSignedMoney } from '@/features/home';
import { useAuthStore } from '@/stores/authStore';

import type { AccountItem, AccountRequest } from '../api/accountTypes';
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

const currencyOptions = [
  { label: '人民币 CNY', value: 'CNY' },
  { label: '美元 USD', value: 'USD' },
  { label: '港币 HKD', value: 'HKD' },
  { label: '欧元 EUR', value: 'EUR' }
];

export function AccountScreen({ initialCreate = false }: { initialCreate?: boolean } = {}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const { isHydrated, isLoggedIn, restoreToken } = useAuthStore();
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
    }
  }, [initialCreate]);

  const {
    overviewQuery,
    listQuery,
    createMutation,
    updateMutation,
    adjustBalanceMutation
  } = useAccount(isLoggedIn);

  const overview = overviewQuery.data;
  const accounts = overview?.accounts ?? listQuery.data ?? [];
  const groupedAccounts = useMemo(() => groupAccounts(accounts), [accounts]);
  const isSubmitting = createMutation.isPending || updateMutation.isPending || adjustBalanceMutation.isPending;

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

  function closeCreatePage() {
    // 新增账户现在是独立页面：优先关闭当前栈页，避免直接 back 时抛 GO_BACK 错误。
    if (router.canDismiss()) {
      router.dismiss();
      return;
    }
    router.replace('/account');
  }

  if (initialCreate) {
    return (
      <AccountCreatePage
        form={form}
        formError={formError}
        isSubmitting={isSubmitting}
        onClose={closeCreatePage}
        onFormChange={(patch) => {
          setForm((current) => ({ ...current, ...patch }));
          setFormError(null);
        }}
        onSubmit={submitAccount}
      />
    );
  }

  function openCreate() {
    router.push('/account-new');
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
        await createMutation.mutateAsync(payload.data);
      }
      if (initialCreate) {
        closeCreatePage();
        return;
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
              <View style={styles.summaryMain}>
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
                  <Pressable style={styles.accountRow} onPress={() => router.push(`/account/${account.id}`)}>
                    <View style={styles.accountIcon}>
                      <Text style={styles.accountIconText}>{accountInitial(account)}</Text>
                    </View>
                    <View style={styles.accountInfo}>
                      <Text style={styles.accountName}>{account.name || '未命名账户'}</Text>
                      <Text variant="caption">{accountTypeLabel(account.type)} · {currencyLabel(account.currency)}{account.tagText ? ` · ${account.tagText}` : ''}</Text>
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

function AccountCreatePage({
  form,
  formError,
  isSubmitting,
  onClose,
  onFormChange,
  onSubmit
}: {
  form: AccountFormState;
  formError: string | null;
  isSubmitting: boolean;
  onClose: () => void;
  onFormChange: (patch: Partial<AccountFormState>) => void;
  onSubmit: () => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const [currencyOpen, setCurrencyOpen] = useState(false);
  const [submitPressed, setSubmitPressed] = useState(false);

  return (
    <SafeAreaView style={styles.page}>
      <GridBackdrop color={theme.border} />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.pageRoot}>
        <View style={styles.pageHeader}>
          <Pressable style={styles.pageHeaderSide} onPress={onClose}>
            <ChevronLeft color={theme.foreground} size={22} />
          </Pressable>
          <Text style={styles.pageTitle}>新增账户</Text>
          <View style={styles.pageHeaderSide} />
        </View>
        <ScrollView
          style={styles.pageScroller}
          contentContainerStyle={styles.pageContent}
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
        >
          <Input label="账户名称" value={form.name} onChangeText={(name) => onFormChange({ name })} />
          <Text style={styles.sheetFieldLabel}>账户类型</Text>
          <View style={styles.choiceWrap}>
            {accountTypes.map((item) => (
              <Pressable key={item.value} style={[styles.choiceChip, form.type === item.value ? styles.choiceChipSelected : null]} onPress={() => onFormChange({ type: item.value })}>
                <Text style={[styles.choiceText, form.type === item.value ? styles.choiceTextSelected : null]}>{item.label}</Text>
              </Pressable>
            ))}
          </View>
          <Input label="账户余额" keyboardType="decimal-pad" value={form.balance} onChangeText={(balance) => onFormChange({ balance })} />
          <DropdownField
            label="币种"
            value={currencyLabel(form.currency)}
            open={currencyOpen}
            options={currencyOptions}
            selectedValue={form.currency}
            onToggle={() => setCurrencyOpen((open) => !open)}
            onSelect={(currency) => {
              onFormChange({ currency });
              setCurrencyOpen(false);
            }}
          />
          <Input label="备注" placeholder="可选" value={form.remark} onChangeText={(remark) => onFormChange({ remark })} />
          <View style={styles.submitActionBlock}>
            <SubmitActionButton label="保存" loading={isSubmitting} pressed={submitPressed} onPressedChange={setSubmitPressed} onPress={onSubmit} />
          </View>
          {formError ? <Text variant="error" style={styles.sheetError}>{formError}</Text> : null}
        </ScrollView>
      </KeyboardAvoidingView>
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
  const [currencyOpen, setCurrencyOpen] = useState(false);
  const [submitPressed, setSubmitPressed] = useState(false);

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.modalRoot}>
        <Pressable style={styles.modalBackdrop} onPress={onClose} />
        <View style={styles.sheet}>
          <ScrollView
            style={styles.sheetScroller}
            contentContainerStyle={styles.sheetContent}
            showsVerticalScrollIndicator={false}
            keyboardShouldPersistTaps="handled"
          >
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
                <Input label={mode === 'edit' ? '余额' : '账户余额'} keyboardType="decimal-pad" value={form.balance} onChangeText={(balance) => onFormChange({ balance })} />
                <DropdownField
                  label="币种"
                  value={currencyLabel(form.currency)}
                  open={currencyOpen}
                  options={currencyOptions}
                  selectedValue={form.currency}
                  onToggle={() => setCurrencyOpen((open) => !open)}
                  onSelect={(currency) => {
                    onFormChange({ currency });
                    setCurrencyOpen(false);
                  }}
                />
                <Input label="备注" placeholder="可选" value={form.remark} onChangeText={(remark) => onFormChange({ remark })} />
              </>
            )}
            <View style={styles.submitActionBlock}>
              <SubmitActionButton label="保存" loading={isSubmitting} pressed={submitPressed} onPressedChange={setSubmitPressed} onPress={onSubmit} />
            </View>
            {formError ? <Text variant="error" style={styles.sheetError}>{formError}</Text> : null}
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

function SubmitActionButton({
  label,
  loading,
  pressed,
  onPressedChange,
  onPress
}: {
  label: string;
  loading: boolean;
  pressed: boolean;
  onPressedChange: (value: boolean) => void;
  onPress: () => void;
}) {
  // 统一复用记一笔的提交按钮，避免各页面保存按钮样式继续漂移。
  return (
    <LedgerSubmitActionButton
      label={label}
      loading={loading}
      pressed={pressed}
      onPressedChange={onPressedChange}
      onPress={onPress}
    />
  );
}

function DropdownField({
  label,
  value,
  open,
  options,
  selectedValue,
  onToggle,
  onSelect
}: {
  label: string;
  value: string;
  open: boolean;
  options: Array<{ label: string; value: string }>;
  selectedValue: string;
  onToggle: () => void;
  onSelect: (value: string) => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <View style={styles.dropdownBox}>
      <Pressable style={styles.dropdownField} onPress={onToggle}>
        <Text style={styles.sheetFieldLabel}>{label}</Text>
        <View style={styles.dropdownValue}>
          <Text variant="muted" style={styles.dropdownText}>{value}</Text>
          {open ? <ChevronUp color={theme.mutedForeground} size={18} /> : <ChevronDown color={theme.mutedForeground} size={18} />}
        </View>
      </Pressable>
      {open ? (
        <View style={styles.optionList}>
          {options.map((option) => (
            <Pressable key={option.value} style={[styles.optionRow, option.value === selectedValue ? styles.optionRowSelected : null]} onPress={() => onSelect(option.value)}>
              <Text style={[styles.optionText, option.value === selectedValue ? styles.optionTextSelected : null]}>{option.label}</Text>
            </Pressable>
          ))}
        </View>
      ) : null}
    </View>
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
  const balance = Number(form.balance);
  if (!Number.isFinite(balance)) {
    return { error: '账户余额必须是有效数字' };
  }
  const initialBalance = form.initialBalance.trim() ? Number(form.initialBalance) : balance;
  if (!Number.isFinite(initialBalance)) {
    return { error: '账户初始数据异常，请重新进入页面' };
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
    const name = accountGroupLabel(account.group) || accountTypeLabel(account.type);
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

function accountGroupLabel(group?: string | null) {
  const labels: Record<string, string> = {
    bankCard: '银行卡',
    thirdParty: '电子钱包',
    cash: '现金',
    credit: '信用账户',
    other: '其他账户'
  };
  return group ? labels[group] || group : null;
}

function currencyLabel(currency?: string | null) {
  const item = currencyOptions.find((candidate) => candidate.value === currency);
  return item?.label || currency || '人民币 CNY';
}

function accountInitial(account: AccountItem) {
  return (account.name || accountTypeLabel(account.type)).slice(0, 1);
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

const createStyles = (theme: ReturnType<typeof useTheme>) => {
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
      padding: 18,
      paddingBottom: 112
    },
    pageRoot: {
      flex: 1,
      position: 'relative'
    },
    pageHeader: {
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
    pageScroller: {
      flex: 1
    },
    pageContent: {
      gap: 14,
      padding: 18,
      paddingBottom: 28
    },
    pageTitle: {
      fontSize: 22,
      fontWeight: '900'
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
    summaryMain: {
      flex: 1,
      minWidth: 0
    },
    countPill: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 999,
      flexShrink: 1,
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
      height: '86%',
      overflow: 'hidden',
      position: 'relative'
    },
    sheetScroller: {
      flex: 1
    },
    sheetContent: {
      gap: 14,
      padding: 18,
      paddingBottom: 24
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
    submitActionBlock: {
      marginTop: 12
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
    submitActionButtonContent: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 8,
      justifyContent: 'center'
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
    sheetError: {
      marginTop: 2
    },
    dropdownBox: {
      gap: 8
    },
    dropdownField: {
      borderColor: theme.input,
      borderRadius: 10,
      borderWidth: 1,
      gap: 6,
      minHeight: 58,
      paddingHorizontal: 12,
      paddingVertical: 9
    },
    dropdownValue: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    dropdownText: {
      flex: 1
    },
    optionList: {
      backgroundColor: theme.secondary,
      borderColor: theme.border,
      borderRadius: 12,
      borderWidth: 1,
      overflow: 'hidden'
    },
    optionRow: {
      paddingHorizontal: 12,
      paddingVertical: 11
    },
    optionRowSelected: {
      backgroundColor: theme.foreground
    },
    optionText: {
      color: theme.foreground,
      fontSize: 14,
      fontWeight: '700'
    },
    optionTextSelected: {
      color: theme.background
    }
  });
};
