<template>
  <AppPage class="holding-page" safe-top safe-bottom gap="24rpx" :background="theme.backgrounds.accountsPage || theme.colors.pageBg">
    <view class="detail-nav">
      <view class="nav-icon" @click="goBack">
        <AppIcon name="common.back" size="40rpx" />
      </view>
      <text class="nav-title">持仓详情</text>
      <view class="nav-icon"></view>
    </view>

    <AppCard
      class="hero-card"
      :padding="theme.spacing.lg"
      :radius="theme.radius.xl"
      :shadow="false"
      :background="theme.backgrounds.investmentSummaryCard"
    >
      <view class="asset-head">
        <view class="asset-main">
          <text class="asset-name">{{ holdingName }}</text>
          <text class="asset-code">{{ holdingCode }}</text>
        </view>
        <view class="asset-pill">{{ assetTypeLabel }}</view>
      </view>

      <view class="hero-metrics">
        <view class="hero-metric primary">
          <text class="metric-label">今日收益</text>
          <text class="metric-value" :class="toneClass(holding?.todayProfit)">{{ fmtSignedMoney(holding?.todayProfit) }}</text>
        </view>
        <view class="hero-metric">
          <text class="metric-label">持有收益</text>
          <text class="metric-value" :class="toneClass(holding?.floatingProfit)">{{ fmtSignedMoney(holding?.floatingProfit) }}</text>
        </view>
        <view class="hero-metric">
          <text class="metric-label">持有收益率</text>
          <text class="metric-value" :class="toneClass(holding?.floatingProfitRate)">{{ fmtPercent(holding?.floatingProfitRate) }}</text>
        </view>
      </view>
    </AppCard>

    <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="position-card">
      <view class="info-grid">
        <view v-for="item in positionStats" :key="item.label" class="info-item">
          <text class="info-label">{{ item.label }}</text>
          <text class="info-value" :class="item.tone">{{ item.value }}</text>
        </view>
      </view>
    </AppCard>

    <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="chart-card">
      <view class="section-head">
        <text class="section-title">{{ chartMode === 'amount' ? '总资产统计' : '总收益' }}</text>
        <view class="chart-tabs">
          <view class="chart-tab" :class="{ active: chartMode === 'amount' }" @click="chartMode = 'amount'">总资产</view>
          <view class="chart-tab" :class="{ active: chartMode === 'profit' }" @click="chartMode = 'profit'">总收益</view>
        </view>
      </view>

      <view class="line-chart">
        <view class="chart-grid-line top"></view>
        <view class="chart-grid-line middle"></view>
        <view class="chart-grid-line bottom"></view>
        <view
          v-for="(point, index) in chartPoints"
          :key="point.key"
          class="chart-point"
          :class="chartMode === 'profit' && point.raw < 0 ? 'negative-dot' : 'positive-dot'"
          :style="{ left: point.left + '%', bottom: point.bottom + '%' }"
        >
          <view v-if="index > 0" class="chart-segment" :style="segmentStyle(index)"></view>
        </view>
      </view>
      <view class="chart-labels">
        <text v-for="point in chartPoints" :key="point.key">{{ point.label }}</text>
      </view>
    </AppCard>

    <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="tx-card">
      <view class="section-head">
        <view>
          <text class="section-title">交易记录</text>
          <text class="section-subtitle">买入金额 / 份额 / 每日净值 / 手续费</text>
        </view>
      </view>

      <view v-if="transactions.length" class="tx-list">
        <view v-for="tx in transactions" :key="tx.id" class="tx-item">
          <view class="tx-top">
            <view>
              <text class="tx-type" :class="tx.type === 'BUY' ? 'positive' : 'negative'">{{ tx.type === 'BUY' ? '买入' : '卖出' }}</text>
              <text class="tx-time">{{ fmtDateTime(tx.transactionTime) }}</text>
              <text class="tx-confirm-time">确认 {{ fmtDate(tx.confirmedDate) }}</text>
            </view>
            <text class="tx-status">{{ statusLabel(tx.status) }}</text>
          </view>
          <view class="tx-grid">
            <view class="tx-cell">
              <text class="tx-label">{{ tx.type === 'BUY' ? '买入金额' : '卖出金额' }}</text>
              <text class="tx-value">¥ {{ fmtAmount(tx.amount) }}</text>
            </view>
            <view class="tx-cell">
              <text class="tx-label">{{ tx.type === 'BUY' ? '买入份额' : '卖出份额' }}</text>
              <text class="tx-value">{{ fmtQuantity(resolveTxQuantity(tx)) }}</text>
            </view>
            <view class="tx-cell">
              <text class="tx-label">每日净值</text>
              <text class="tx-value">{{ fmtNav(resolveTxNav(tx)) }}</text>
            </view>
            <view class="tx-cell">
              <text class="tx-label">手续费</text>
              <text class="tx-value">¥ {{ fmtAmount(tx.fee) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else-if="!loading" class="empty-state">
        <AppIcon name="investmentActions.buy" size="64rpx" />
        <text>暂无交易记录</text>
      </view>

      <view v-else class="empty-state">
        <text>加载中...</text>
      </view>
    </AppCard>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onMounted } from 'vue'
import AppCard from '@/components/app/AppCard.vue'
import AppIcon from '@/components/app/AppIcon.vue'
import AppPage from '@/components/app/AppPage.vue'
import { useInvestmentStore } from '@/stores/investment'
import { useTheme } from '@/theme/useTheme'
import type { HoldingDetail, InvestmentTransactionItem } from '@/services/investmentApi'

const store = useInvestmentStore()
const { currentTheme } = useTheme()

const detail = ref<HoldingDetail | null>(null)
const routeName = ref('')
const routeId = ref('')
const loading = ref(false)
const chartMode = ref<'amount' | 'profit'>('amount')
const theme = computed(() => currentTheme.value)
const holding = computed(() => detail.value?.holding)
const summary = computed(() => detail.value?.summary)
const transactions = computed(() => detail.value?.transactions || [])
const backendChartRows = computed(() => (detail.value?.chartPoints || []).slice(-7))

const holdingName = computed(() => holding.value?.assetName || routeName.value || holding.value?.symbol || '--')
const holdingCode = computed(() => holding.value?.symbol ? `${holding.value.symbol} · ${holding.value.currency || 'CNY'}` : holding.value?.currency || '')
const assetTypeLabel = computed(() => {
  const type = String(holding.value?.assetType || '').toUpperCase()
  if (type.includes('FUND')) return '基金'
  if (type.includes('STOCK')) return '股票'
  if (type.includes('CRYPTO')) return '加密货币'
  return '投资'
})

const positionStats = computed(() => [
  { label: '待确认金额', value: `¥ ${fmtAmount(summary.value?.pendingConfirmAmount)}`, tone: '' },
  { label: '持仓成本价', value: `¥ ${fmtAmount(holding.value?.avgCost, priceScale.value)}`, tone: '' },
  { label: '持有份额', value: fmtQuantity(holding.value?.quantity), tone: '' },
  { label: '日涨幅', value: fmtPercent(holding.value?.todayChangeRate), tone: toneClass(holding.value?.todayChangeRate) },
  { label: '基金净值', value: fmtNav(holding.value?.latestPrice), tone: '' }
])

const priceScale = computed(() => holding.value?.priceScale ?? 4)

const chartPoints = computed(() => {
  const source = backendChartRows.value
  const values = source.map((item) => chartMode.value === 'amount'
    ? toNumber(item.totalAssetAmount)
    : toNumber(item.totalProfitAmount)
  )
  if (!values.length) {
    return []
  }
  const min = Math.min(...values, 0)
  const max = Math.max(...values, 1)
  const range = Math.max(max - min, 1)
  return source.map((item, index) => {
    const value = values[index]
    return {
      key: `${item.quoteTime || index}-${chartMode.value}`,
      label: formatChartLabel(item.quoteTime, index),
      raw: value,
      left: source.length <= 1 ? 50 : Math.round(index / (source.length - 1) * 100),
      bottom: Math.round(((value - min) / range) * 72 + 10)
    }
  })
})

onMounted(() => {
  const pages = getCurrentPages()
  const opts = (pages[pages.length - 1] as any).options || {}
  routeId.value = opts.id || ''
  routeName.value = decodeURIComponent(opts.name || '')
  reload()
})

async function reload() {
  if (!routeId.value || loading.value) return
  loading.value = true
  try {
    detail.value = await store.fetchDetail(routeId.value)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '持仓详情加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goBack() {
  uni.navigateBack()
}

function segmentStyle(index: number) {
  const prev = chartPoints.value[index - 1]
  const cur = chartPoints.value[index]
  const dx = cur.left - prev.left
  const dy = cur.bottom - prev.bottom
  const length = Math.sqrt(dx * dx + dy * dy)
  const angle = Math.atan2(dy, dx) * 180 / Math.PI
  return {
    width: `${length}%`,
    transform: `rotate(${angle}deg)`,
    transformOrigin: 'left center',
    left: `-${length}%`
  }
}

function resolveTxQuantity(tx: InvestmentTransactionItem) {
  return tx.confirmedQuantity ?? tx.tradeQuantity ?? tx.quantity
}

function resolveTxNav(tx: InvestmentTransactionItem) {
  return tx.confirmedNav ?? tx.tradePrice ?? tx.price
}

function fmtAmount(value: number | string | null | undefined, decimals = 2) {
  if (value == null) return '--'
  const num = toNumber(value)
  return num.toLocaleString('zh-CN', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

function fmtSignedMoney(value: number | string | null | undefined) {
  if (value == null) return '--'
  const num = toNumber(value)
  const sign = num >= 0 ? '+' : '-'
  return `${sign}¥ ${Math.abs(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function fmtPercent(value: number | string | null | undefined) {
  if (value == null) return '--'
  const num = toNumber(value)
  return `${num >= 0 ? '+' : ''}${num.toFixed(2)}%`
}

function fmtQuantity(value: number | string | null | undefined) {
  if (value == null) return '--'
  return toNumber(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })
}

function fmtNav(value: number | string | null | undefined) {
  if (value == null) return '--'
  return toNumber(value).toLocaleString('zh-CN', { minimumFractionDigits: priceScale.value, maximumFractionDigits: Math.max(priceScale.value, 4) })
}

function fmtDateTime(value: string) {
  if (!value) return '--'
  return value.slice(0, 16).replace('T', ' ')
}

function fmtDate(value: string | null | undefined) {
  if (!value) return '--'
  return value.slice(0, 10)
}

function formatChartLabel(value: string | undefined, index: number) {
  if (!value) return `${index + 1}`
  const normalized = value.replace('T', ' ')
  return normalized.slice(5, 10)
}

function statusLabel(status?: string | null) {
  const map: Record<string, string> = {
    NORMAL: '正常',
    CONFIRMED: '已确认',
    PENDING_CONFIRM: '待确认',
    REVOKED: '已撤销',
    CANCELLED: '已取消'
  }
  return status ? map[status] || status : '正常'
}

function toneClass(value: number | string | null | undefined) {
  if (value == null) return ''
  return toNumber(value) >= 0 ? 'positive' : 'negative'
}

function toNumber(value: number | string | null | undefined) {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num : 0
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.holding-page {
  min-height: 100vh;
}

.detail-nav,
.asset-head,
.section-head,
.tx-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-nav {
  min-height: 64rpx;
}

.nav-icon {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-title {
  font-size: $font-xl;
  font-weight: 800;
  color: var(--xo-text-primary);
}

.hero-card {
  overflow: hidden;
}

.asset-main {
  min-width: 0;
}

.asset-name {
  display: block;
  max-width: 460rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: $font-lg;
  font-weight: 800;
  color: var(--xo-white);
}

.asset-code {
  display: block;
  margin-top: 8rpx;
  font-size: $font-xs;
  color: var(--xo-white-75);
}

.asset-pill {
  padding: 10rpx 18rpx;
  border-radius: var(--xo-radius-round);
  background: var(--xo-white-25);
  font-size: $font-xs;
  color: var(--xo-white);
}

.hero-metrics {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr) minmax(0, 1fr);
  gap: 16rpx;
  margin-top: 34rpx;
}

.hero-metric {
  min-width: 0;
  padding: 18rpx 14rpx;
  border-radius: var(--xo-radius-lg);
  background: rgba(255, 255, 255, 0.14);
}

.hero-metric.primary {
  background: rgba(255, 255, 255, 0.22);
}

.metric-label {
  display: block;
  font-size: 22rpx;
  color: var(--xo-white-75);
}

.metric-value {
  display: block;
  margin-top: 10rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 30rpx;
  font-weight: 800;
  color: var(--xo-white);
}

.positive {
  color: var(--xo-positive);
}

.negative {
  color: var(--xo-negative);
}

.position-card,
.chart-card,
.tx-card {
  overflow: hidden;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx 20rpx;
}

.info-item {
  min-height: 88rpx;
  padding: 18rpx;
  border-radius: var(--xo-radius-lg);
  background: var(--xo-bg-accounts-summary-card);
  box-sizing: border-box;
}

.info-label {
  display: block;
  font-size: 22rpx;
  color: var(--xo-text-secondary);
}

.info-value {
  display: block;
  margin-top: 10rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 28rpx;
  font-weight: 800;
  color: var(--xo-text-primary);
}

.section-title {
  display: block;
  font-size: $font-lg;
  font-weight: 800;
  color: var(--xo-text-primary);
}

.section-subtitle {
  display: block;
  margin-top: 6rpx;
  font-size: $font-xs;
  color: var(--xo-text-secondary);
}

.chart-tabs {
  display: flex;
  padding: 6rpx;
  border-radius: var(--xo-radius-round);
  background: var(--xo-primary-soft);
}

.chart-tab {
  height: 52rpx;
  padding: 0 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--xo-radius-round);
  font-size: $font-xs;
  color: var(--xo-primary);
}

.chart-tab.active {
  color: var(--xo-white);
  background: var(--xo-gradient-button-primary);
  box-shadow: var(--xo-shadow-button);
}

.line-chart {
  position: relative;
  height: 260rpx;
  margin-top: 28rpx;
  border-radius: var(--xo-radius-xl);
  background: linear-gradient(180deg, rgba(47, 123, 255, 0.08), rgba(255, 255, 255, 0));
  overflow: hidden;
}

.chart-grid-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1rpx;
  background: var(--xo-border-color);
}

.chart-grid-line.top { top: 24%; }
.chart-grid-line.middle { top: 50%; }
.chart-grid-line.bottom { top: 76%; }

.chart-point {
  position: absolute;
  width: 14rpx;
  height: 14rpx;
  margin-left: -7rpx;
  margin-bottom: -7rpx;
  border: 4rpx solid var(--xo-card-bg);
  border-radius: 50%;
  z-index: 2;
}

.positive-dot {
  background: var(--xo-primary);
}

.negative-dot {
  background: var(--xo-negative);
}

.chart-segment {
  position: absolute;
  top: 3rpx;
  height: 4rpx;
  border-radius: var(--xo-radius-round);
  background: var(--xo-primary);
  z-index: 1;
}

.negative-dot .chart-segment {
  background: var(--xo-negative);
}

.chart-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 12rpx;
  font-size: 20rpx;
  color: var(--xo-text-placeholder);
}

.tx-list {
  margin-top: 24rpx;
}

.tx-item {
  padding: 22rpx 0;
  border-bottom: 1rpx solid var(--xo-border-color);
}

.tx-item:last-child {
  border-bottom: 0;
}

.tx-type {
  display: block;
  font-size: $font-md;
  font-weight: 800;
}

.tx-time {
  display: block;
  margin-top: 6rpx;
  font-size: $font-xs;
  color: var(--xo-text-secondary);
}

.tx-confirm-time {
  display: block;
  margin-top: 4rpx;
  font-size: $font-xs;
  color: var(--xo-text-muted);
}

.tx-status {
  padding: 8rpx 16rpx;
  border-radius: var(--xo-radius-round);
  background: var(--xo-primary-soft);
  font-size: 22rpx;
  color: var(--xo-primary);
}

.tx-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 18rpx;
}

.tx-cell {
  min-width: 0;
}

.tx-label {
  display: block;
  font-size: 22rpx;
  color: var(--xo-text-secondary);
}

.tx-value {
  display: block;
  margin-top: 6rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 26rpx;
  font-weight: 700;
  color: var(--xo-text-primary);
}

.empty-state {
  min-height: 200rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  color: var(--xo-text-secondary);
  font-size: $font-sm;
}
</style>
