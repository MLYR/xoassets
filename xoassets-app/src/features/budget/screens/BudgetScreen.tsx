import { Redirect } from 'expo-router';
import { ChevronLeft, ChevronRight, Edit3, Plus, Target, X } from 'lucide-react-native';
import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button, Card, CardContent, Input, Separator, Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';
import { formatMoney, formatPercent } from '@/features/home';
import { useAuthStore } from '@/stores/authStore';

import type { BudgetItem, BudgetRequest, BudgetType, ExpenseCategoryItem } from '../api/budgetTypes';
import { useBudget } from '../hooks/useBudget';

interface BudgetFormState {
  id?: string;
  budgetType: BudgetType;
  categoryId: string;
  amount: string;
}

const budgetTypeOptions: Array<{ label: string; value: BudgetType }> = [
  { label: '总预算', value: 'TOTAL' },
  { label: '分类预算', value: 'CATEGORY' }
];

export function BudgetScreen() {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const { isHydrated, isLoggedIn, restoreToken } = useAuthStore();
  const [month, setMonth] = useState(currentMonth());
  const [sheetOpen, setSheetOpen] = useState(false);
  const [form, setForm] = useState<BudgetFormState>(() => createEmptyForm());
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  const { summaryQuery, listQuery, categoriesQuery, createMutation, updateMutation } = useBudget(month, isLoggedIn);
  const summary = summaryQuery.data;
  const budgets = summary?.items ?? listQuery.data ?? [];
  const categories = categoriesQuery.data ?? [];
  const totalBudget = budgets.find((item) => item.budgetType === 'TOTAL') ?? null;
  const categoryBudgets = budgets.filter((item) => item.budgetType === 'CATEGORY');
  const isSubmitting = createMutation.isPending || updateMutation.isPending;

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
    setMonth((current) => shiftMonth(current, offset));
    setSheetOpen(false);
    setFormError(null);
  }

  function openCreate(type: BudgetType) {
    setForm({ budgetType: type, categoryId: type === 'CATEGORY' ? String(categories[0]?.id ?? '') : '', amount: '' });
    setFormError(null);
    setSheetOpen(true);
  }

  function openEdit(item: BudgetItem) {
    setForm({
      id: String(item.id),
      budgetType: item.budgetType === 'CATEGORY' ? 'CATEGORY' : 'TOTAL',
      categoryId: String(item.categoryId ?? ''),
      amount: item.amount === null || item.amount === undefined ? '' : String(item.amount)
    });
    setFormError(null);
    setSheetOpen(true);
  }

  function updateForm(patch: Partial<BudgetFormState>) {
    setForm((current) => {
      const next = { ...current, ...patch };
      // 总预算不能带分类，避免把上一次分类选择误提交给后端。
      if (patch.budgetType === 'TOTAL') {
        next.categoryId = '';
      }
      if (patch.budgetType === 'CATEGORY' && !next.categoryId) {
        next.categoryId = String(categories[0]?.id ?? '');
      }
      return next;
    });
    setFormError(null);
  }

  async function submitBudget() {
    const payload = buildBudgetRequest(form, month, categories);
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
      setSheetOpen(false);
      setFormError(null);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '预算保存失败');
    }
  }

  return (
    <SafeAreaView style={styles.page}>
      <GridBackdrop color={theme.border} />
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <View>
            <Text style={styles.title}>预算</Text>
            <Text variant="muted">月度总预算与分类预算</Text>
          </View>
          <Pressable style={styles.headerButton} onPress={() => openCreate('CATEGORY')}>
            <Plus color={theme.primaryForeground} size={20} strokeWidth={2.4} />
          </Pressable>
        </View>

        <View style={styles.monthBar}>
          <Pressable style={styles.monthButton} onPress={() => changeMonth(-1)}>
            <ChevronLeft color={theme.foreground} size={20} />
          </Pressable>
          <Text style={styles.monthTitle}>{month.replace('-', '年')}月</Text>
          <Pressable style={styles.monthButton} onPress={() => changeMonth(1)}>
            <ChevronRight color={theme.foreground} size={20} />
          </Pressable>
        </View>

        {(summaryQuery.isError || listQuery.isError) ? <ErrorCard message="预算数据加载失败，请稍后重试。" /> : null}
        {(summaryQuery.isLoading || listQuery.isLoading) ? <ActivityIndicator color={theme.primary} /> : null}

        <Card>
          <CardContent style={styles.summaryCard}>
            <View style={styles.summaryTop}>
              <View>
                <Text variant="muted">本月预算</Text>
                <Text style={styles.totalBudget} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>{formatMoney(summary?.totalBudget)}</Text>
              </View>
              <StatusPill label={summary?.usageStatusLabel || '暂无预算'} status={summary?.usageStatus} />
            </View>
            <BudgetProgress value={summary?.usageRate} />
            <View style={styles.summaryGrid}>
              <MiniStat label="已用" value={formatMoney(summary?.totalUsed)} />
              <MiniStat label="剩余" value={formatMoney(summary?.totalRemaining)} />
              <MiniStat label="使用率" value={formatPercent(summary?.usageRate)} />
            </View>
            <Button variant="outline" onPress={() => totalBudget ? openEdit(totalBudget) : openCreate('TOTAL')}>
              {totalBudget ? '编辑总预算' : '设置总预算'}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardContent style={styles.sectionCard}>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>分类预算</Text>
              <Button size="sm" variant="secondary" onPress={() => openCreate('CATEGORY')}>新增</Button>
            </View>
            {categoryBudgets.length > 0 ? (
              categoryBudgets.map((item, index) => (
                <View key={String(item.id)}>
                  <BudgetRow item={item} onEdit={() => openEdit(item)} />
                  {index < categoryBudgets.length - 1 ? <Separator /> : null}
                </View>
              ))
            ) : (
              <Text variant="muted">暂无分类预算，新增后可跟踪具体支出分类。</Text>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardContent style={styles.sectionCard}>
            <Text style={styles.sectionTitle}>预算提醒</Text>
            <Text variant="muted">{budgetHint(summary?.usageRate, summary?.usageStatusLabel)}</Text>
          </CardContent>
        </Card>
      </ScrollView>

      <BudgetSheet
        visible={sheetOpen}
        form={form}
        categories={categories}
        formError={formError}
        isSubmitting={isSubmitting}
        onClose={() => setSheetOpen(false)}
        onChange={updateForm}
        onSubmit={submitBudget}
      />
    </SafeAreaView>
  );
}

function BudgetSheet({
  visible,
  form,
  categories,
  formError,
  isSubmitting,
  onClose,
  onChange,
  onSubmit
}: {
  visible: boolean;
  form: BudgetFormState;
  categories: ExpenseCategoryItem[];
  formError: string | null;
  isSubmitting: boolean;
  onClose: () => void;
  onChange: (patch: Partial<BudgetFormState>) => void;
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
              <Text style={styles.sheetTitle}>{form.id ? '编辑预算' : '新增预算'}</Text>
              <Pressable onPress={onClose}>
                <X color={theme.foreground} size={24} />
              </Pressable>
            </View>

            <View style={styles.segmented}>
              {budgetTypeOptions.map((item) => (
                <Pressable key={item.value} style={[styles.segmentButton, form.budgetType === item.value ? styles.segmentActive : null]} onPress={() => onChange({ budgetType: item.value })}>
                  <Text style={[styles.segmentText, form.budgetType === item.value ? styles.segmentTextActive : null]}>{item.label}</Text>
                </Pressable>
              ))}
            </View>

            {form.budgetType === 'CATEGORY' ? (
              <>
                <Text style={styles.sheetFieldLabel}>支出分类</Text>
                <View style={styles.choiceWrap}>
                  {categories.map((category) => (
                    <Pressable key={String(category.id)} style={[styles.choiceChip, form.categoryId === String(category.id) ? styles.choiceChipSelected : null]} onPress={() => onChange({ categoryId: String(category.id) })}>
                      <Text style={[styles.choiceText, form.categoryId === String(category.id) ? styles.choiceTextSelected : null]}>{category.name || '未命名分类'}</Text>
                    </Pressable>
                  ))}
                </View>
              </>
            ) : null}

            <Input label="预算金额" keyboardType="decimal-pad" placeholder="0.00" value={form.amount} onChangeText={(amount) => onChange({ amount })} />
            {formError ? <Text variant="error">{formError}</Text> : null}
            <Button loading={isSubmitting} onPress={onSubmit}>保存</Button>
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

function BudgetRow({ item, onEdit }: { item: BudgetItem; onEdit: () => void }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Pressable style={styles.budgetRow} onPress={onEdit}>
      <View style={styles.categoryIcon}>
        <Text style={styles.categoryIconText}>{(item.categoryName || '预').slice(0, 1)}</Text>
      </View>
      <View style={styles.budgetInfo}>
        <View style={styles.rowBetween}>
          <Text style={styles.budgetName}>{item.categoryName || '分类预算'}</Text>
          <Edit3 color={theme.mutedForeground} size={16} />
        </View>
        <BudgetProgress value={item.usageRate} compact />
        <Text variant="caption">已用 {formatMoney(item.usedAmount)} / 预算 {formatMoney(item.amount)} · {item.usageStatusLabel || '正常'}</Text>
      </View>
    </Pressable>
  );
}

function StatusPill({ label, status }: { label: string; status?: string | null }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const color = status === 'OVER' || status === 'EXCEEDED' ? theme.destructive : status === 'WARNING' ? theme.warning : theme.foreground;

  return (
    <View style={[styles.statusPill, { borderColor: color }]}>
      <Target color={color} size={15} strokeWidth={2.2} />
      <Text style={[styles.statusText, { color }]}>{label}</Text>
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

function BudgetProgress({ value, compact }: { value?: number | null; compact?: boolean }) {
  const theme = useTheme();
  const percent = typeof value === 'number' && Number.isFinite(value) ? Math.max(0, Math.min(value, 100)) : 0;

  return (
    <View style={[stylesStatic.progressTrack, { backgroundColor: theme.secondary, height: compact ? 7 : 10 }]}>
      <View style={[stylesStatic.progressFill, { width: `${percent}%`, backgroundColor: percent >= 100 ? theme.destructive : percent >= 80 ? theme.warning : theme.foreground }]} />
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

function buildBudgetRequest(form: BudgetFormState, month: string, categories: ExpenseCategoryItem[]): { data: BudgetRequest } | { error: string } {
  const amount = Number(form.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    return { error: '预算金额必须大于 0' };
  }
  if (form.budgetType === 'CATEGORY' && !form.categoryId) {
    return { error: categories.length > 0 ? '请选择支出分类' : '暂无可用支出分类' };
  }

  return {
    data: {
      month,
      budgetType: form.budgetType,
      categoryId: form.budgetType === 'CATEGORY' ? form.categoryId : null,
      amount: amount.toFixed(2),
      status: 1
    }
  };
}

function createEmptyForm(): BudgetFormState {
  return {
    budgetType: 'TOTAL',
    categoryId: '',
    amount: ''
  };
}

function budgetHint(value?: number | null, label?: string | null) {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return '设置总预算或分类预算后，这里会展示本月预算使用提醒。';
  }
  if (value >= 100) {
    return `${label || '预算已超支'}，建议查看分类预算和支出明细，优先定位超支来源。`;
  }
  if (value >= 80) {
    return `${label || '预算接近上限'}，后续支出需要更谨慎。`;
  }
  return `${label || '预算正常'}，本月预算仍有余量。`;
}

function currentMonth() {
  const now = new Date();
  return `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}`;
}

function shiftMonth(month: string, offset: number) {
  const [year, monthIndex] = month.split('-').map(Number);
  const date = new Date(year, monthIndex - 1 + offset, 1);
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}`;
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
  },
  progressTrack: {
    borderRadius: 999,
    overflow: 'hidden',
    width: '100%'
  },
  progressFill: {
    borderRadius: 999,
    height: '100%'
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
    monthBar: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    monthButton: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      height: 36,
      justifyContent: 'center',
      width: 36
    },
    monthTitle: {
      fontSize: 18,
      fontWeight: '900'
    },
    summaryCard: {
      gap: 16
    },
    summaryTop: {
      alignItems: 'flex-start',
      flexDirection: 'row',
      gap: 12,
      justifyContent: 'space-between'
    },
    totalBudget: {
      fontSize: 36,
      fontWeight: '900',
      marginTop: 4
    },
    statusPill: {
      alignItems: 'center',
      borderRadius: 999,
      borderWidth: 1,
      flexDirection: 'row',
      gap: 6,
      paddingHorizontal: 10,
      paddingVertical: 7
    },
    statusText: {
      fontSize: 13,
      fontWeight: '800'
    },
    summaryGrid: {
      flexDirection: 'row',
      gap: 12
    },
    sectionCard: {
      gap: 12
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
    budgetRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12,
      paddingVertical: 12
    },
    categoryIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      height: 36,
      justifyContent: 'center',
      width: 36
    },
    categoryIconText: {
      fontSize: 15,
      fontWeight: '900'
    },
    budgetInfo: {
      flex: 1,
      gap: 7,
      minWidth: 0
    },
    rowBetween: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    budgetName: {
      fontSize: 16,
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
    segmented: {
      backgroundColor: theme.secondary,
      borderRadius: 14,
      flexDirection: 'row',
      padding: 4
    },
    segmentButton: {
      alignItems: 'center',
      borderRadius: 10,
      flex: 1,
      minHeight: 38,
      justifyContent: 'center'
    },
    segmentActive: {
      backgroundColor: theme.card
    },
    segmentText: {
      color: theme.mutedForeground,
      fontWeight: '800'
    },
    segmentTextActive: {
      color: theme.foreground
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
