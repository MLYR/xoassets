import type { HoldingItem, HoldingSummary } from '@/services/investmentApi'

export type DistributionKey = 'fund' | 'stock' | 'crypto' | 'cash' | 'other'

export type DistributionItem = {
  key: DistributionKey
  label: string
  amount: number
  percent: number
  color: string
}

export type HoldingRow = {
  id: string
  name: string
  code: string
  marketValue: number
  yesterdayProfit: number | null
  todayProfit: number | null
  floatingProfit: number
  floatingProfitRate: number
  raw: HoldingItem
}

export type TrendPoint = {
  label: string
  value: number
}

export const distributionOrder: DistributionKey[] = ['fund', 'stock', 'crypto', 'cash', 'other']

export const distributionMeta: Record<DistributionKey, { label: string }> = {
  fund: { label: '基金' },
  stock: { label: '股票' },
  crypto: { label: '加密货币' },
  cash: { label: '现金' },
  other: { label: '其他' }
}

export function fmtAmount(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function fmtSigned(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  const prefix = v >= 0 ? '+' : ''
  return prefix + v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function fmtPercent(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  return `${v >= 0 ? '+' : ''}${v.toFixed(2)}%`
}

export function fmtPercentNumber(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  return `${v.toFixed(1)}%`
}

export function fmtSignedOrFallback(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  return fmtSigned(v)
}

export function profitClass(v: number | null | undefined) {
  if (v == null || v === undefined) return ''
  return v >= 0 ? 'income' : 'expense'
}

export function calcRelativeRate(delta: number, total: number) {
  const base = total - delta
  if (!base) return 0
  return (delta / base) * 100
}

export function resolveDistributionKey(item: HoldingItem): DistributionKey {
  const assetType = String(item.assetType || '').toUpperCase()
  if (assetType.includes('FUND')) return 'fund'
  if (assetType.includes('STOCK')) return 'stock'
  if (assetType.includes('CRYPTO') || assetType.includes('COIN')) return 'crypto'
  if (assetType.includes('CASH')) return 'cash'
  return 'other'
}

export function buildSummaryMetrics(summary: HoldingSummary | null) {
  const totalAsset = summary?.totalMarketValue ?? 0
  const accumulatedProfit = summary?.floatingProfit ?? 0
  const accumulatedRate = summary?.floatingProfitRate ?? 0
  // 投资首页的“昨日收益 / 今日收益”直接取后端总收益字段，不用月度快照或前端反算冒充。
  const vsYesterdayAmount = summary?.yesterdayProfit ?? null
  const vsTodayAmount = summary?.todayProfit ?? null

  return {
    totalAsset,
    accumulatedProfit,
    accumulatedRate,
    vsYesterdayAmount,
    vsYesterdayRate: summary?.yesterdayProfitRate ?? null,
    vsLastMonthAmount: vsTodayAmount,
    vsLastMonthRate: summary?.todayProfitRate ?? null
  }
}

export function buildDistributionItems(
  holdings: HoldingItem[],
  palette: Record<DistributionKey, string>,
  totalMarketValue?: number | null
): DistributionItem[] {
  const grouped = holdings.reduce<Record<DistributionKey, number>>((acc, item) => {
    const key = resolveDistributionKey(item)
    acc[key] += Number(item.marketValue || 0)
    return acc
  }, {
    fund: 0,
    stock: 0,
    crypto: 0,
    cash: 0,
    other: 0
  })

  const total = (totalMarketValue ?? 0) || Object.values(grouped).reduce((acc, cur) => acc + cur, 0)

  return distributionOrder.map((key) => ({
    key,
    label: distributionMeta[key].label,
    amount: grouped[key],
    percent: total > 0 ? (grouped[key] / total) * 100 : 0,
    color: palette[key]
  }))
}

export function buildHoldingRows(holdings: HoldingItem[]): HoldingRow[] {
  return holdings.map((item) => ({
    id: item.id,
    name: item.assetName || item.symbol || '未知资产',
    code: item.symbol || '--',
    marketValue: item.marketValue ?? 0,
    // 昨日收益由后端同一组价格快照计算，缺失时展示占位，不在前端反算。
    yesterdayProfit: item.yesterdayProfit ?? null,
    todayProfit: item.todayProfit ?? null,
    floatingProfit: item.floatingProfit ?? 0,
    floatingProfitRate: item.floatingProfitRate ?? 0,
    raw: item
  }))
}

export function buildTrendSeries(summary: HoldingSummary | null): TrendPoint[] {
  const total = summary?.totalMarketValue ?? 0
  const step = Math.max(total * 0.04, 1200)

  // TODO: 资产变化趋势当前投资接口缺少历史序列，先用静态回推点位做结构占位。
  return [
    { label: '2月', value: Math.max(total - step * 4, 0) },
    { label: '3月', value: Math.max(total - step * 3.2, 0) },
    { label: '4月', value: Math.max(total - step * 2.1, 0) },
    { label: '5月', value: Math.max(total - step * 1.1, 0) },
    { label: '6月', value: total }
  ]
}

export function buildDistributionInsights(items: DistributionItem[]): string[] {
  const sorted = [...items].sort((a, b) => b.amount - a.amount)
  const top = sorted[0]
  const second = sorted[1]
  const crypto = items.find((item) => item.key === 'crypto')

  return [
    top && top.amount > 0 ? `${top.label}占比最高（${fmtPercentNumber(top.percent)}），是当前投资配置的核心。` : '当前暂无可分析的主要资产类别。',
    second && second.amount > 0 ? `${second.label}是第二大仓位，组合结构仍然较为集中。` : '当前组合分布较轻，适合继续补充核心资产。',
    crypto && crypto.percent >= 15 ? `加密货币占比 ${fmtPercentNumber(crypto.percent)}，波动暴露偏高，需关注回撤。`
      : '高波动资产占比暂时可控，整体风险分布仍在合理区间。'
  ].filter(Boolean) as string[]
}

export function buildRiskDistribution(items: DistributionItem[]) {
  const total = items.reduce((acc, item) => acc + item.amount, 0)
  const fund = items.find((item) => item.key === 'fund')?.amount || 0
  const stock = items.find((item) => item.key === 'stock')?.amount || 0
  const crypto = items.find((item) => item.key === 'crypto')?.amount || 0
  const other = items.find((item) => item.key === 'other')?.amount || 0

  if (!total) {
    return [
      { label: '稳健', percent: 0, color: '#2F7BFF' },
      { label: '均衡', percent: 0, color: '#19C2C8' },
      { label: '高波动', percent: 0, color: '#FF8A34' }
    ]
  }

  return [
    { label: '稳健', percent: (fund / total) * 100, color: '#2F7BFF' },
    { label: '均衡', percent: ((stock + other) / total) * 100, color: '#19C2C8' },
    { label: '高波动', percent: (crypto / total) * 100, color: '#FF8A34' }
  ]
}
