<template>
  <AppPage class="invest-page" safe-bottom gap="24rpx">
    <!-- 顶部资产卡片：优先走真实汇总字段，缺口字段仅做受控兜底。 -->
    <AppCard
      class="summary-card"
      :padding="theme.spacing.lg"
      :radius="theme.radius.xl"
      :shadow="false"
      :background="theme.backgrounds.investmentSummaryCard"
    >
      <view class="summary-card-line"></view>
      <view class="summary-main">
        <view class="summary-left">
          <text class="summary-title">投资总资产</text>
          <AppAmount :value="summaryMetrics.totalAsset" prefix="¥ " size="lg" :color="theme.colors.white" />
          <view class="summary-compare-list">
            <view class="summary-compare-row">
              <text class="summary-compare-label">较昨日</text>
              <text class="summary-compare-value">{{ fmtSigned(summaryMetrics.vsYesterdayAmount) }}</text>
              <text class="summary-compare-rate">{{ fmtPercent(summaryMetrics.vsYesterdayRate) }}</text>
            </view>
            <view class="summary-compare-row">
              <text class="summary-compare-label">较上月</text>
              <text class="summary-compare-value">{{ fmtSigned(summaryMetrics.vsLastMonthAmount) }}</text>
              <text class="summary-compare-rate">{{ fmtPercent(summaryMetrics.vsLastMonthRate) }}</text>
            </view>
          </view>
        </view>

        <view class="summary-divider"></view>

        <view class="summary-right">
          <text class="summary-side-label">累计收益率</text>
          <view class="summary-rate-line">
            <AppAmount :value="summaryMetrics.accumulatedRate" signed size="lg" :color="theme.colors.white" />
            <text class="summary-rate-unit">%</text>
          </view>
          <view class="summary-side-profit">
            <text class="summary-side-profit-label">累计收益</text>
            <AppAmount :value="summaryMetrics.accumulatedProfit" prefix="¥ " signed size="sm" :color="theme.colors.white" />
          </view>
        </view>
      </view>
    </AppCard>

    <!-- 资产分布：使用持仓实时聚合，现金类在当前接口缺失时显示 0。 -->
    <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="distribution-card">
      <AppSectionHeader title="资产分布" @action="handleDistributionMore">
        <template #action>
          <text class="section-action-text">更多 ›</text>
        </template>
      </AppSectionHeader>

      <view class="distribution-layout">
        <view class="distribution-donut" :style="distributionRingStyle">
          <view class="distribution-donut-center">
            <text class="distribution-center-label">总资产</text>
            <AppAmount :value="distributionTotalAmount" prefix="¥ " size="md" tone="neutral" />
          </view>
        </view>

        <view class="distribution-list">
          <view v-for="item in distributionItems" :key="item.key" class="distribution-item">
            <view class="distribution-item-main">
              <view class="distribution-item-name">
                <text class="distribution-dot" :style="{ background: item.color }"></text>
                <text class="distribution-name-text">{{ item.label }}</text>
              </view>
              <text class="distribution-amount">¥ {{ fmtAmount(item.amount) }}</text>
              <text class="distribution-percent">{{ fmtPercentNumber(item.percent) }}</text>
            </view>
          </view>
        </view>
      </view>
    </AppCard>

    <!-- 持仓卡片：保留现有持仓详情入口，只提升结构和视觉。 -->
    <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="holdings-card">
      <AppSectionHeader title="持仓" @action="handleAllHoldings">
        <template #action>
          <text class="section-action-text">全部持仓 ›</text>
        </template>
      </AppSectionHeader>

      <view class="holding-table">
        <view class="holding-header">
          <text class="holding-col holding-col-name">名称/代码</text>
          <text class="holding-col holding-col-amount">总金额/昨日/今日收益</text>
          <text class="holding-col holding-col-profit">持有收益 / 率</text>
        </view>

        <view v-if="holdingRows.length" class="holding-body">
          <view
            v-for="row in holdingRows"
            :key="row.id"
            class="holding-row"
            @click="goDetail(row)"
          >
            <view class="holding-cell holding-name-cell">
              <text class="holding-name">{{ row.name }}</text>
              <text class="holding-code">{{ row.code }}</text>
            </view>

            <view class="holding-cell holding-amount-cell">
              <text class="holding-market-value">¥ {{ fmtAmount(row.marketValue) }}</text>
              <view class="holding-daily-line">
                <text class="holding-daily-value" :class="profitClass(row.yesterdayProfit)">
                  {{ fmtSignedOrFallback(row.yesterdayProfit) }}
                </text>
                <text class="holding-daily-separator">/</text>
                <text class="holding-daily-value" :class="profitClass(row.todayProfit)">
                  {{ fmtSignedOrFallback(row.todayProfit) }}
                </text>
              </view>
            </view>

            <view class="holding-cell holding-profit-cell">
              <AppAmount :value="row.floatingProfit" signed size="sm" />
              <text class="holding-profit-rate" :class="profitClass(row.floatingProfitRate)">
                {{ fmtPercent(row.floatingProfitRate) }}
              </text>
            </view>
          </view>
        </view>

        <view v-else-if="!loading" class="empty-state">
          <text>暂无持仓</text>
        </view>
      </view>

      <view class="holding-analysis-entry" @click="handleAllHoldings">
        <text class="holding-analysis-text">持仓分析 ›</text>
      </view>
    </AppCard>

    <!-- 底部交易入口：先保留视觉和交互位，后续单独接交易页。 -->
    <view class="action-row">
      <AppActionButton
        class="trade-action-button"
        text="买入"
        type="primary"
        icon="investmentActions.buy"
        :radius="theme.radius.round"
        block
        @click="handleTradeAction('buy')"
      />
      <AppActionButton
        class="trade-action-button"
        text="转换"
        type="purple"
        icon="investmentActions.convert"
        :radius="theme.radius.round"
        block
        @click="handleTradeAction('convert')"
      />
      <AppActionButton
        class="trade-action-button"
        text="卖出"
        type="danger"
        icon="investmentActions.sell"
        :radius="theme.radius.round"
        block
        @click="handleTradeAction('sell')"
      />
    </view>
  </AppPage>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppActionButton from '@/components/app/AppActionButton.vue'
import AppAmount from '@/components/app/AppAmount.vue'
import AppCard from '@/components/app/AppCard.vue'
import AppPage from '@/components/app/AppPage.vue'
import AppSectionHeader from '@/components/app/AppSectionHeader.vue'
import type { HoldingItem } from '@/services/investmentApi'
import { useInvestmentStore } from '@/stores/investment'
import { useTheme } from '@/theme/useTheme'

type DistributionKey = 'fund' | 'stock' | 'crypto' | 'cash' | 'other'

const distributionOrder: DistributionKey[] = ['fund', 'stock', 'crypto', 'cash', 'other']

const distributionMeta: Record<DistributionKey, { label: string }> = {
  fund: { label: '基金' },
  stock: { label: '股票' },
  crypto: { label: '加密货币' },
  cash: { label: '现金' },
  other: { label: '其他' }
}

const store = useInvestmentStore()
const { currentTheme } = useTheme()

const holdings = computed(() => store.holdings)
const summary = computed(() => store.summary)
const loading = computed(() => store.loading)
const theme = computed(() => currentTheme.value)

onShow(() => {
  store.fetchHoldings()
})

const summaryMetrics = computed(() => {
  const totalAsset = summary.value?.totalMarketValue ?? 0
  const accumulatedProfit = summary.value?.floatingProfit ?? 0
  const accumulatedRate = summary.value?.floatingProfitRate ?? 0
  const vsYesterdayAmount = summary.value?.todayProfit ?? 0

  // TODO: 投资汇总接口暂缺“较上月”字段，当前先用昨日收益或累计浮盈兜底结构位。
  const vsLastMonthAmount = summary.value?.yesterdayProfit ?? accumulatedProfit

  return {
    totalAsset,
    accumulatedProfit,
    accumulatedRate,
    vsYesterdayAmount,
    vsYesterdayRate: calcRelativeRate(vsYesterdayAmount, totalAsset),
    vsLastMonthAmount,
    vsLastMonthRate: calcRelativeRate(vsLastMonthAmount, totalAsset)
  }
})

const distributionItems = computed(() => {
  const palette = theme.value.charts.investmentDistribution
  const grouped = holdings.value.reduce<Record<DistributionKey, number>>((acc, item) => {
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

  const total = (summary.value?.totalMarketValue ?? 0) || Object.values(grouped).reduce((acc, cur) => acc + cur, 0)

  return distributionOrder.map((key) => ({
    key,
    label: distributionMeta[key].label,
    amount: grouped[key],
    percent: total > 0 ? (grouped[key] / total) * 100 : 0,
    color: palette[key]
  }))
})

const distributionTotalAmount = computed(() => summary.value?.totalMarketValue ?? 0)

const distributionRingStyle = computed(() => {
  const hasValue = distributionItems.value.some((item) => item.percent > 0)
  if (!hasValue) {
    return {
      background: `conic-gradient(${theme.value.colors.border} 0% 100%)`
    }
  }

  const segments = distributionItems.value
    .map((item, index, list) => {
      const start = list.slice(0, index).reduce((acc, cur) => acc + cur.percent, 0)
      const end = start + item.percent
      return `${item.color} ${start}% ${end}%`
    })
    .join(', ')

  return {
    background: `conic-gradient(${segments})`
  }
})

const holdingRows = computed(() => {
  return holdings.value.map((item) => ({
    id: item.id,
    name: item.assetName || item.symbol || '未知资产',
    code: item.symbol || '--',
    marketValue: item.marketValue ?? 0,
    // TODO: 持仓列表接口暂无昨日收益字段，当前页面先用占位，避免伪造业务数字。
    yesterdayProfit: null as number | null,
    todayProfit: item.todayProfit ?? null,
    floatingProfit: item.floatingProfit ?? 0,
    floatingProfitRate: item.floatingProfitRate ?? 0,
    raw: item
  }))
})

function fmtAmount(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtSigned(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  const prefix = v >= 0 ? '+' : ''
  return prefix + v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtPercent(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  return `${v >= 0 ? '+' : ''}${v.toFixed(2)}%`
}

function fmtPercentNumber(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  return `${v.toFixed(1)}%`
}

function fmtSignedOrFallback(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  return fmtSigned(v)
}

function profitClass(v: number | null | undefined) {
  if (v == null || v === undefined) return ''
  return v >= 0 ? 'income' : 'expense'
}

function calcRelativeRate(delta: number, total: number) {
  const base = total - delta
  if (!base) return 0
  return (delta / base) * 100
}

function resolveDistributionKey(item: HoldingItem): DistributionKey {
  const assetType = String(item.assetType || '').toUpperCase()
  if (assetType.includes('FUND')) return 'fund'
  if (assetType.includes('STOCK')) return 'stock'
  if (assetType.includes('CRYPTO') || assetType.includes('COIN')) return 'crypto'
  if (assetType.includes('CASH')) return 'cash'
  return 'other'
}

function goDetail(row: { raw: HoldingItem }) {
  const h = row.raw
  uni.navigateTo({ url: `/pages/holding-detail/holding-detail?id=${h.id}&name=${encodeURIComponent(h.assetName || h.symbol || '')}` })
}

function handleDistributionMore() {
  uni.navigateTo({ url: '/pages/investment-distribution/investment-distribution' })
}

function handleAllHoldings() {
  uni.navigateTo({ url: '/pages/holding-analysis/holding-analysis' })
}

function handleTradeAction(action: 'buy' | 'convert' | 'sell') {
  const actionMap = {
    buy: '买入入口待接入',
    convert: '转换入口待接入',
    sell: '卖出入口待接入'
  }
  uni.showToast({ title: actionMap[action], icon: 'none' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.invest-page {
  min-height: 100vh;
}

.summary-card {
  position: relative;
  overflow: hidden;
}

.summary-card-line {
  position: absolute;
  right: -30rpx;
  bottom: -10rpx;
  width: 320rpx;
  height: 180rpx;
  opacity: 0.16;
  border-right: 4rpx solid rgba(255, 255, 255, 0.72);
  border-top: 4rpx solid rgba(255, 255, 255, 0.4);
  border-radius: 120rpx 0 0 0;
  transform: rotate(-18deg);
}

.summary-main {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: stretch;
  column-gap: 24rpx;
}

.summary-left,
.summary-right {
  display: flex;
  flex-direction: column;
}

.summary-left {
  flex: 1.3;
  row-gap: 16rpx;
}

.summary-right {
  flex: 0.9;
  justify-content: center;
  row-gap: 20rpx;
}

.summary-title,
.summary-side-label,
.summary-side-profit-label,
.summary-compare-label,
.summary-compare-value,
.summary-compare-rate {
  color: rgba(255, 255, 255, 0.94);
}

.summary-title,
.summary-side-label {
  font-size: $font-sm;
}

.summary-divider {
  width: 2rpx;
  background: rgba(255, 255, 255, 0.24);
}

.summary-compare-list {
  display: flex;
  flex-direction: column;
  row-gap: 10rpx;
}

.summary-compare-row {
  display: flex;
  align-items: center;
  column-gap: 16rpx;
  font-size: $font-sm;
}

.summary-compare-label {
  width: 86rpx;
  color: rgba(255, 255, 255, 0.8);
}

.summary-compare-value,
.summary-compare-rate {
  font-variant-numeric: tabular-nums;
}

.summary-side-profit {
  display: flex;
  flex-direction: column;
  row-gap: 8rpx;
}

.summary-rate-line {
  display: flex;
  align-items: flex-end;
  column-gap: 8rpx;
}

.summary-rate-unit {
  padding-bottom: 8rpx;
  font-size: $font-xl;
  color: rgba(255, 255, 255, 0.94);
  font-weight: 600;
}

.distribution-layout {
  display: flex;
  align-items: center;
  column-gap: 28rpx;
}

.section-action-text {
  font-size: $font-sm;
  color: var(--xo-text-regular);
}

.distribution-donut {
  width: 232rpx;
  height: 232rpx;
  border-radius: 50%;
  padding: 20rpx;
  box-sizing: border-box;
  flex-shrink: 0;
}

.distribution-donut-center {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--xo-component-card-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  row-gap: 8rpx;
}

.distribution-center-label {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.distribution-list {
  flex: 1;
  min-width: 0;
}

.distribution-item + .distribution-item {
  margin-top: 18rpx;
}

.distribution-item-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  column-gap: 12rpx;
}

.distribution-item-name {
  display: flex;
  align-items: center;
  min-width: 0;
}

.distribution-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 12rpx;
  flex-shrink: 0;
}

.distribution-name-text,
.distribution-amount,
.distribution-percent {
  font-size: $font-sm;
  color: var(--xo-text-primary);
  font-variant-numeric: tabular-nums;
}

.distribution-percent {
  color: var(--xo-text-regular);
}

.holdings-card {
  overflow: hidden;
}

.holding-analysis-entry {
  display: flex;
  justify-content: flex-end;
  padding-top: 18rpx;
}

.holding-analysis-text {
  font-size: $font-sm;
  color: var(--xo-primary);
}

.holding-header,
.holding-row {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr) minmax(0, 0.82fr);
  column-gap: 16rpx;
}

.holding-header {
  padding: 8rpx 0 18rpx;
  border-bottom: 1rpx solid var(--xo-border-color);
}

.holding-col {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.holding-col-amount,
.holding-col-profit {
  text-align: right;
}

.holding-body {
  display: flex;
  flex-direction: column;
}

.holding-row {
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid var(--xo-border-color);
}

.holding-row:last-child {
  border-bottom: none;
}

.holding-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  row-gap: 8rpx;
}

.holding-name {
  font-size: $font-lg;
  color: var(--xo-text-primary);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.holding-code {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.holding-market-value {
  font-size: $amount-sm;
  color: var(--xo-text-primary);
  font-weight: 700;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.holding-daily-line {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  column-gap: 10rpx;
}

.holding-daily-value,
.holding-daily-separator,
.holding-profit-rate {
  font-size: $font-sm;
  color: var(--xo-text-regular);
  font-variant-numeric: tabular-nums;
}

.holding-profit-cell {
  align-items: flex-end;
}

.holding-daily-value.income,
.holding-profit-rate.income {
  color: var(--xo-positive);
}

.holding-daily-value.expense,
.holding-profit-rate.expense {
  color: var(--xo-negative);
}

.action-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 18rpx;
  padding-bottom: calc(env(safe-area-inset-bottom, 0px) + 8rpx);
}

.trade-action-button {
  border-radius: 999rpx;
}

.empty-state {
  padding: 48rpx 0 16rpx;
  text-align: center;
  color: var(--xo-text-secondary);
}
</style>
