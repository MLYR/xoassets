<template>
  <AppPage class="distribution-page" :padding="false" safe-bottom gap="24rpx">
    <view class="safe-top"></view>
    <view class="topbar">
      <text class="topbar-back" @click="goBack">‹</text>
      <text class="topbar-title">资产分布</text>
      <text class="topbar-spacer"></text>
    </view>

    <view class="page-body">
      <AppCard
        :padding="theme.spacing.lg"
        :radius="theme.radius.xl"
        :shadow="false"
        :background="theme.backgrounds.investmentSummaryCard"
        class="summary-card"
      >
        <view class="summary-line"></view>
        <text class="summary-title">总资产</text>
        <AppAmount :value="summaryMetrics.totalAsset" prefix="¥ " size="lg" :color="theme.colors.white" />
        <view class="summary-rows">
          <view class="summary-row">
            <text class="summary-label">较昨日</text>
            <text class="summary-value">{{ fmtSigned(summaryMetrics.vsYesterdayAmount) }}</text>
            <text class="summary-rate">{{ fmtPercent(summaryMetrics.vsYesterdayRate) }}</text>
          </view>
          <view class="summary-row">
            <text class="summary-label">较上月</text>
            <text class="summary-value">{{ fmtSigned(summaryMetrics.vsLastMonthAmount) }}</text>
            <text class="summary-rate">{{ fmtPercent(summaryMetrics.vsLastMonthRate) }}</text>
          </view>
        </view>
      </AppCard>

      <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl">
        <view class="distribution-layout">
          <view class="donut-wrap" :style="distributionRingStyle">
            <view class="donut-center">
              <text class="donut-center-label">总资产</text>
              <AppAmount :value="summaryMetrics.totalAsset" prefix="¥ " size="md" tone="neutral" />
            </view>
          </view>

          <view class="distribution-list">
            <view v-for="item in distributionItems" :key="item.key" class="distribution-item">
              <view class="distribution-item-name">
                <text class="distribution-dot" :style="{ background: item.color }"></text>
                <text class="distribution-item-label">{{ item.label }}</text>
              </view>
              <text class="distribution-item-amount">¥ {{ fmtAmount(item.amount) }}</text>
              <text class="distribution-item-percent">{{ fmtPercentNumber(item.percent) }}</text>
            </view>
          </view>
        </view>
      </AppCard>

      <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl">
        <view class="section-head">
          <text class="section-title">资产变化趋势</text>
          <text class="section-subtitle">总资产（元）</text>
        </view>

        <view class="trend-card">
          <svg class="trend-svg" viewBox="0 0 320 180" preserveAspectRatio="none">
            <defs>
              <linearGradient id="trendFill" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" stop-color="rgba(47,123,255,0.28)" />
                <stop offset="100%" stop-color="rgba(47,123,255,0.03)" />
              </linearGradient>
            </defs>
            <polyline
              class="trend-area"
              :points="trendAreaPoints"
              fill="url(#trendFill)"
              stroke="none"
            />
            <polyline
              class="trend-line"
              :points="trendLinePoints"
              fill="none"
              stroke="#2F7BFF"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <circle
              v-for="point in trendPlotPoints"
              :key="point.label"
              :cx="point.x"
              :cy="point.y"
              r="4"
              fill="#FFFFFF"
              stroke="#2F7BFF"
              stroke-width="2"
            />
          </svg>

          <view class="trend-axis">
            <text v-for="point in trendSeries" :key="point.label" class="trend-axis-label">{{ point.label }}</text>
          </view>
        </view>
      </AppCard>

      <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl">
        <text class="section-title">分布洞察</text>
        <view class="insight-list">
          <view v-for="(item, index) in insights" :key="item" class="insight-item">
            <view class="insight-badge">{{ index + 1 }}</view>
            <text class="insight-text">{{ item }}</text>
          </view>
        </view>
      </AppCard>
    </view>
  </AppPage>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppAmount from '@/components/app/AppAmount.vue'
import AppCard from '@/components/app/AppCard.vue'
import AppPage from '@/components/app/AppPage.vue'
import { useInvestmentStore } from '@/stores/investment'
import { useTheme } from '@/theme/useTheme'
import {
  buildDistributionInsights,
  buildDistributionItems,
  buildSummaryMetrics,
  buildTrendSeries,
  fmtAmount,
  fmtPercent,
  fmtPercentNumber,
  fmtSigned
} from '@/pages/investments/helpers'

const store = useInvestmentStore()
const { currentTheme } = useTheme()

const holdings = computed(() => store.holdings)
const summary = computed(() => store.summary)
const theme = computed(() => currentTheme.value)

onShow(() => {
  store.fetchHoldings()
})

const summaryMetrics = computed(() => buildSummaryMetrics(summary.value))

const distributionItems = computed(() => {
  return buildDistributionItems(
    holdings.value,
    theme.value.charts.investmentDistribution,
    summary.value?.totalMarketValue
  )
})

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

const trendSeries = computed(() => buildTrendSeries(summary.value))

const trendPlotPoints = computed(() => {
  const values = trendSeries.value.map((point) => point.value)
  const max = Math.max(...values, 1)
  const min = Math.min(...values, 0)
  const width = 296
  const height = 132
  const left = 12
  const top = 16
  const range = Math.max(max - min, 1)

  return trendSeries.value.map((point, index) => ({
    ...point,
    x: left + (width / Math.max(trendSeries.value.length - 1, 1)) * index,
    y: top + height - ((point.value - min) / range) * height
  }))
})

const trendLinePoints = computed(() => trendPlotPoints.value.map((point) => `${point.x},${point.y}`).join(' '))

const trendAreaPoints = computed(() => {
  const points = trendPlotPoints.value
  if (!points.length) return ''
  const first = points[0]
  const last = points[points.length - 1]
  return `${first.x},148 ${trendLinePoints.value} ${last.x},148`
})

const insights = computed(() => buildDistributionInsights(distributionItems.value))

function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.distribution-page {
  min-height: 100vh;
}

.safe-top {
  height: env(safe-area-inset-top, 0px);
}

.topbar {
  display: grid;
  grid-template-columns: 56rpx 1fr 56rpx;
  align-items: center;
  padding: 20rpx 24rpx 8rpx;
}

.topbar-back,
.topbar-spacer {
  font-size: 48rpx;
  color: var(--xo-text-primary);
}

.topbar-title {
  text-align: center;
  font-size: $font-xl;
  font-weight: 700;
  color: var(--xo-text-primary);
}

.page-body {
  padding: 0 24rpx 24rpx;
  display: flex;
  flex-direction: column;
  row-gap: 24rpx;
}

.summary-card {
  position: relative;
  overflow: hidden;
}

.summary-line {
  position: absolute;
  right: -16rpx;
  bottom: -4rpx;
  width: 240rpx;
  height: 140rpx;
  opacity: 0.16;
  border-right: 4rpx solid rgba(255, 255, 255, 0.72);
  border-top: 4rpx solid rgba(255, 255, 255, 0.4);
  border-radius: 120rpx 0 0 0;
  transform: rotate(-16deg);
}

.summary-title,
.summary-label,
.summary-value,
.summary-rate {
  position: relative;
  z-index: 1;
  color: rgba(255, 255, 255, 0.94);
}

.summary-title {
  font-size: $font-sm;
  margin-bottom: 18rpx;
}

.summary-rows {
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  row-gap: 12rpx;
}

.summary-row {
  display: flex;
  align-items: center;
  column-gap: 18rpx;
  font-size: $font-sm;
}

.summary-label {
  width: 86rpx;
  color: rgba(255, 255, 255, 0.8);
}

.distribution-layout {
  display: flex;
  align-items: center;
  column-gap: 24rpx;
}

.donut-wrap {
  width: 248rpx;
  height: 248rpx;
  border-radius: 50%;
  padding: 20rpx;
  box-sizing: border-box;
  flex-shrink: 0;
}

.donut-center {
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

.donut-center-label {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.distribution-list {
  flex: 1;
  min-width: 0;
}

.distribution-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  column-gap: 12rpx;
}

.distribution-item + .distribution-item {
  margin-top: 18rpx;
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

.distribution-item-label,
.distribution-item-amount,
.distribution-item-percent {
  font-size: $font-sm;
  color: var(--xo-text-primary);
  font-variant-numeric: tabular-nums;
}

.distribution-item-percent {
  color: var(--xo-text-regular);
}

.section-head {
  display: flex;
  flex-direction: column;
  row-gap: 8rpx;
  margin-bottom: 18rpx;
}

.section-title {
  font-size: $font-lg;
  font-weight: 700;
  color: var(--xo-text-primary);
}

.section-subtitle {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.trend-card {
  background: linear-gradient(180deg, rgba(47,123,255,0.06) 0%, rgba(47,123,255,0.01) 100%);
  border-radius: 24rpx;
  padding: 18rpx 18rpx 12rpx;
}

.trend-svg {
  width: 100%;
  height: 360rpx;
}

.trend-axis {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  margin-top: 10rpx;
}

.trend-axis-label {
  text-align: center;
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.insight-list {
  display: flex;
  flex-direction: column;
  row-gap: 18rpx;
  margin-top: 18rpx;
}

.insight-item {
  display: flex;
  align-items: flex-start;
  column-gap: 16rpx;
}

.insight-badge {
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  background: rgba(47, 123, 255, 0.12);
  color: var(--xo-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-sm;
  font-weight: 700;
  flex-shrink: 0;
}

.insight-text {
  font-size: $font-md;
  line-height: 1.6;
  color: var(--xo-text-primary);
}
</style>
