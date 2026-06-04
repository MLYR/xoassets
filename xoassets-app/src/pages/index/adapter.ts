import type {
  AssetSnapshotLatest,
  AssetTrendPoint,
  DashboardOverview,
  TransactionItem
} from '@/services/dashboardApi'
import type { BudgetSummary } from '@/services/budgetApi'
import type { GoalItem } from '@/services/goalApi'

export type HomeStatKey = 'income' | 'expense' | 'balance'
export type HomeQuickActionKey = 'record' | 'transfer' | 'invest' | 'budget'
export type HomeActivityType = 'income' | 'expense' | 'transfer' | 'refund'

export interface HomeStatCard {
  key: HomeStatKey
  label: string
  amount: number
  icon: string
  tone: 'positive' | 'negative' | 'neutral'
}

export interface HomeTrendPoint {
  label: string
  value: number
}

export interface HomeBudgetProgress {
  used: number
  total: number
  remaining: number
  usageRate: number
}

export interface HomeGoalProgress {
  id: string
  name: string
  targetAmount: number
  savedAmount: number
  progressRate: number
  dueText: string
}

export interface HomeQuickAction {
  key: HomeQuickActionKey
  label: string
  icon: string
}

export interface HomeActivity {
  id: string
  title: string
  account: string
  time: string
  amount: number
  displayAmount: string
  type: HomeActivityType
  iconType: HomeActivityType | 'investment'
  raw: TransactionItem
}

export interface HomeDashboardModel {
  netAssets: number
  todayChangeAmount: number
  todayChangeRate: number
  stats: HomeStatCard[]
  trend: HomeTrendPoint[]
  budget: HomeBudgetProgress
  goal: HomeGoalProgress | null
  quickActions: HomeQuickAction[]
  activities: HomeActivity[]
}

export interface HomeDashboardExternalData {
  budgetSummary?: BudgetSummary | null
  goals?: GoalItem[] | null
  latestSnapshot?: AssetSnapshotLatest | null
  netAssetsTrend?: AssetTrendPoint[] | null
}

const QUICK_ACTIONS: HomeQuickAction[] = [
  { key: 'record', label: '记账', icon: 'quickActions.record' },
  { key: 'transfer', label: '转账', icon: 'quickActions.transfer' },
  { key: 'invest', label: '投资', icon: 'quickActions.invest' },
  { key: 'budget', label: '预算', icon: 'quickActions.budget' }
]

export function buildHomeDashboardModel(
  overview: DashboardOverview | null,
  externalData: HomeDashboardExternalData = {}
): HomeDashboardModel {
  const monthlyIncome = overview?.monthlyIncome ?? 0
  const monthlyExpense = overview?.monthlyExpense ?? 0
  const monthlyBalance = monthlyIncome - monthlyExpense
  const netAssets = overview?.netAssets ?? 0
  const todayChangeAmount = resolveTodayChangeAmount(overview, externalData.latestSnapshot)

  return {
    netAssets,
    todayChangeAmount,
    todayChangeRate: calcRelativeRate(todayChangeAmount, netAssets),
    stats: buildStats(monthlyIncome, monthlyExpense, monthlyBalance),
    trend: buildAssetTrend(overview, externalData.netAssetsTrend),
    budget: buildBudgetFallback(overview, externalData.budgetSummary),
    goal: buildGoalModel(overview, externalData.goals),
    quickActions: QUICK_ACTIONS,
    activities: buildRecentActivities(overview?.recentTransactions || [])
  }
}

function buildStats(monthlyIncome: number, monthlyExpense: number, monthlyBalance: number): HomeStatCard[] {
  return [
    { key: 'income', label: '本月收入', amount: monthlyIncome, icon: 'home.income', tone: 'positive' },
    { key: 'expense', label: '本月支出', amount: monthlyExpense, icon: 'home.expense', tone: 'negative' },
    { key: 'balance', label: '本月结余', amount: monthlyBalance, icon: 'home.balance', tone: 'neutral' }
  ]
}

function resolveTodayChangeAmount(overview: DashboardOverview | null, latestSnapshot?: AssetSnapshotLatest | null): number {
  const extended = overview as (DashboardOverview & {
    todayNetAssetChange?: number | null
    todayChangeAmount?: number | null
  }) | null

  // 今日变化来自资产快照，避免用投资浮盈冒充净资产日变化。
  return latestSnapshot?.netAssetChangeFromYesterday ?? extended?.todayNetAssetChange ?? extended?.todayChangeAmount ?? 0
}

function buildAssetTrend(overview: DashboardOverview | null, netAssetsTrend?: AssetTrendPoint[] | null): HomeTrendPoint[] {
  if (netAssetsTrend?.length) {
    return sampleTrendPoints(netAssetsTrend, 6)
  }

  const extended = overview as (DashboardOverview & { assetTrend?: HomeTrendPoint[] | null }) | null
  if (extended?.assetTrend?.length) {
    return extended.assetTrend.slice(-6)
  }

  // TODO: 趋势接口无数据时显示空态，不再使用原型假曲线。
  return []
}

function sampleTrendPoints(points: AssetTrendPoint[], targetCount: number): HomeTrendPoint[] {
  const sorted = points
    .filter((point) => point.date)
    .slice()
    .sort((a, b) => a.date.localeCompare(b.date))

  if (!sorted.length) return []
  if (sorted.length <= targetCount) {
    return sorted.map((point) => ({
      label: dayLabel(point.date),
      value: Number(point.value || 0)
    }))
  }

  const bucketSize = Math.ceil(sorted.length / targetCount)
  const sampled: AssetTrendPoint[] = []
  for (let index = 0; index < sorted.length; index += bucketSize) {
    sampled.push(sorted[Math.min(index + bucketSize - 1, sorted.length - 1)])
  }

  return sampled.slice(-targetCount).map((point) => ({
    label: dayLabel(point.date),
    value: Number(point.value || 0)
  }))
}

function buildBudgetFallback(overview: DashboardOverview | null, budgetSummary?: BudgetSummary | null): HomeBudgetProgress {
  const extended = overview as (DashboardOverview & {
    budgetUsedAmount?: number | null
    budgetTotalAmount?: number | null
  }) | null
  const used = budgetSummary?.totalUsed ?? extended?.budgetUsedAmount ?? overview?.monthlyExpense ?? 0

  const total = budgetSummary?.totalBudget ?? extended?.budgetTotalAmount ?? 0
  const remaining = Math.max(total - used, 0)
  return {
    used,
    total,
    remaining,
    usageRate: budgetSummary?.usageRate ?? (total > 0 ? Math.min((used / total) * 100, 100) : 0)
  }
}

function buildGoalModel(overview: DashboardOverview | null, goals?: GoalItem[] | null): HomeGoalProgress | null {
  const primaryGoal = pickPrimaryGoal(goals)
  if (primaryGoal) {
    return {
      id: primaryGoal.id,
      name: primaryGoal.name,
      targetAmount: primaryGoal.targetAmount,
      savedAmount: primaryGoal.currentAmount,
      progressRate: Math.min(primaryGoal.completionRate, 100),
      dueText: formatGoalDueText(primaryGoal)
    }
  }

  const extended = overview as (DashboardOverview & {
    primaryGoal?: Partial<HomeGoalProgress> | null
  }) | null
  const source = extended?.primaryGoal
  if (!source) return null
  const savedAmount = source.savedAmount ?? 0
  const targetAmount = source.targetAmount ?? 0

  // TODO: 若后续 /dashboard/overview 聚合 primaryGoal，可直接删除 goalApi.list 二次请求。
  return {
    id: source.id || 'primary-goal',
    name: source.name || '资产目标',
    targetAmount,
    savedAmount,
    progressRate: source.progressRate ?? (targetAmount > 0 ? Math.min((savedAmount / targetAmount) * 100, 100) : 0),
    dueText: source.dueText || ''
  }
}

function pickPrimaryGoal(goals?: GoalItem[] | null): GoalItem | null {
  if (!goals?.length) return null
  return goals.find((item) => item.status !== 'DONE') || goals[0]
}

function formatGoalDueText(goal: GoalItem): string {
  if (goal.targetDate) {
    const date = parseLocalDate(goal.targetDate)
    if (date) return `预计 ${date.getFullYear()}年${date.getMonth() + 1}月达成`
  }
  if (goal.daysLeft > 0) return `剩余 ${goal.daysLeft} 天`
  return goal.statusLabel || ''
}

function buildRecentActivities(transactions: TransactionItem[]): HomeActivity[] {
  return transactions.slice(0, 5).map((item) => {
    const type = activityType(item.type)
    return {
      id: item.id,
      title: item.categoryName || activityFallbackTitle(type),
      account: item.targetAccountName ? `${item.accountName || '账户'} -> ${item.targetAccountName}` : item.accountName || '默认账户',
      time: formatActivityTime(item.transactionTime),
      amount: item.amount,
      displayAmount: formatSignedActivityAmount(item),
      type,
      iconType: activityIconType(item, type),
      raw: item
    }
  })
}

function activityType(type: TransactionItem['type']): HomeActivityType {
  if (type === 'INCOME') return 'income'
  if (type === 'TRANSFER') return 'transfer'
  if (type === 'REFUND') return 'refund'
  return 'expense'
}

function activityIconType(item: TransactionItem, type: HomeActivityType): HomeActivity['iconType'] {
  const text = `${item.categoryName || ''}${item.accountName || ''}`
  // 首页最近动态暂由普通流水承接；投资聚合接口接入后可直接返回 investment 类型。
  if (text.includes('投资') || text.includes('基金') || text.includes('股票')) return 'investment'
  return type
}

function activityFallbackTitle(type: HomeActivityType): string {
  if (type === 'income') return '收入'
  if (type === 'transfer') return '转账'
  if (type === 'refund') return '退款'
  return '支出'
}

function formatSignedActivityAmount(item: TransactionItem): string {
  const prefix = item.type === 'INCOME' || item.type === 'REFUND' ? '+' : item.type === 'EXPENSE' ? '-' : ''
  return `${prefix}${formatAmount(item.amount)}`
}

function formatActivityTime(text: string): string {
  if (!text) return ''
  const date = parseLocalDate(text)
  if (!date) return text.slice(5, 16).replace('T', ' ')
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const activityDay = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  const time = `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
  if (activityDay === today) return `今天 ${time}`
  if (activityDay === today - 24 * 60 * 60 * 1000) return `昨天 ${time}`
  return `${date.getMonth() + 1}月${date.getDate()}日 ${time}`
}

function formatAmount(value: number): string {
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function calcRelativeRate(delta: number, total: number): number {
  const base = total - delta
  if (!base) return 0
  return (delta / base) * 100
}

function parseLocalDate(text: string): Date | null {
  // 后端可能返回 ISO 或空格分隔时间；统一本地解析以支持“今天/昨天”展示。
  const normalized = text.includes('T') ? text : text.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function pad2(value: number): string {
  return value.toString().padStart(2, '0')
}

function dayLabel(dateText: string): string {
  const date = parseLocalDate(dateText)
  if (!date) return dateText.slice(5, 10)
  return `${date.getMonth() + 1}/${date.getDate()}`
}
