<template>
  <AppPage class="analysis-page" :padding="false" safe-bottom gap="24rpx">
    <AppNavBar title="持仓分析" detail />

    <view class="page-body">
      <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl">
        <view class="summary-grid">
          <view class="summary-item">
            <text class="summary-label">持仓市值</text>
            <AppAmount :value="summaryMetrics.totalAsset" prefix="¥ " size="md" tone="neutral" />
          </view>
          <view class="summary-item">
            <text class="summary-label">持仓成本</text>
            <AppAmount :value="costMetrics.totalCost" prefix="¥ " size="md" tone="neutral" />
          </view>
          <view class="summary-item">
            <text class="summary-label">持有收益</text>
            <AppAmount :value="summaryMetrics.accumulatedProfit" prefix="¥ " signed size="md" semantic="profit" />
          </view>
          <view class="summary-item">
            <text class="summary-label">收益率</text>
            <AppAmount :value="summaryMetrics.accumulatedRate" signed size="md" semantic="profit" />
          </view>
        </view>
      </AppCard>

      <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl">
        <text class="section-title">上一收益日贡献排行</text>
        <view class="rank-list">
          <view v-for="item in profitRanking" :key="item.id" class="rank-item">
            <view class="rank-head">
              <text class="rank-name">{{ item.name }}</text>
              <text class="rank-profit" :class="profitClass(item.contributionProfit)">{{ fmtSigned(item.contributionProfit) }}</text>
            </view>
            <view class="rank-sub">
              <text class="rank-code">{{ item.code }}</text>
              <text class="rank-rate" :class="profitClass(item.contributionProfit)">{{ item.contributionLabel }}</text>
            </view>
            <view class="rank-bar-track">
              <view class="rank-bar-fill" :style="{ width: `${item.sharePercent}%` }"></view>
            </view>
          </view>
        </view>
      </AppCard>

      <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl">
        <text class="section-title">持仓明细</text>
        <view class="detail-table">
          <view class="detail-head">
            <text class="detail-col detail-col-name">名称/代码</text>
            <text class="detail-col detail-col-mid">市值/成本</text>
            <text class="detail-col detail-col-right">收益/率</text>
          </view>
          <view v-for="row in holdingRows" :key="row.id" class="detail-row" @click="goHoldingDetail(row.raw)">
            <view class="detail-cell">
              <text class="detail-name">{{ row.name }}</text>
              <text class="detail-code">{{ row.code }}</text>
            </view>
            <view class="detail-cell detail-cell-mid">
              <text class="detail-main-value">¥ {{ fmtAmount(row.marketValue) }}</text>
              <text class="detail-sub-value">¥ {{ fmtAmount(row.raw.totalCost) }}</text>
            </view>
            <view class="detail-cell detail-cell-right">
              <text class="detail-main-value" :class="profitClass(row.floatingProfit)">{{ fmtSigned(row.floatingProfit) }}</text>
              <text class="detail-sub-value" :class="profitClass(row.floatingProfitRate)">{{ fmtPercent(row.floatingProfitRate) }}</text>
            </view>
          </view>
        </view>
      </AppCard>

      <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl">
        <text class="section-title">风险分布</text>
        <view class="risk-list">
          <view v-for="item in riskDistribution" :key="item.label" class="risk-item">
            <view class="risk-head">
              <view class="risk-label-wrap">
                <text class="risk-dot" :style="{ background: item.color }"></text>
                <text class="risk-label">{{ item.label }}</text>
              </view>
              <text class="risk-percent">{{ fmtPercentNumber(item.percent) }}</text>
            </view>
            <view class="risk-track">
              <view class="risk-fill" :style="{ width: `${item.percent}%`, background: item.color }"></view>
            </view>
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
import AppNavBar from '@/components/app/AppNavBar.vue'
import AppPage from '@/components/app/AppPage.vue'
import type { HoldingItem } from '@/services/investmentApi'
import { useInvestmentStore } from '@/stores/investment'
import { useTheme } from '@/theme/useTheme'
import {
  buildDistributionItems,
  buildHoldingRows,
  buildRiskDistribution,
  buildSummaryMetrics,
  fmtAmount,
  fmtPercent,
  fmtPercentNumber,
  fmtSigned,
  profitClass
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
const holdingRows = computed(() => buildHoldingRows(holdings.value))

const costMetrics = computed(() => {
  const totalCost = holdings.value.reduce((sum, item) => sum + Number(item.totalCost || 0), 0)
  return { totalCost }
})

const profitRanking = computed(() => {
  const rows = holdingRows.value.map((item) => ({
    ...item,
    // 收益贡献使用后端持仓每日收益表产出的上一收益日收益，不再用累计持有收益冒充日收益贡献。
    contributionProfit: item.yesterdayProfit,
    contributionLabel: item.yesterdayProfit == null ? '暂无上一收益日' : '上一收益日'
  }))
  const maxProfit = Math.max(...rows.map((item) => Math.abs(item.contributionProfit ?? 0)), 0)
  return rows
    .sort((a, b) => (b.contributionProfit ?? Number.NEGATIVE_INFINITY) - (a.contributionProfit ?? Number.NEGATIVE_INFINITY))
    .map((item) => ({
      ...item,
      sharePercent: maxProfit > 0 && item.contributionProfit != null ? Math.max((Math.abs(item.contributionProfit) / maxProfit) * 100, 8) : 0
    }))
})

const distributionItems = computed(() => {
  return buildDistributionItems(
    holdings.value,
    theme.value.charts.investmentDistribution,
    summary.value?.totalMarketValue
  )
})

const riskDistribution = computed(() => buildRiskDistribution(distributionItems.value))

function goHoldingDetail(item: HoldingItem) {
  uni.navigateTo({ url: `/pages/holding-detail/holding-detail?id=${item.id}&name=${encodeURIComponent(item.assetName || item.symbol || '')}` })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.analysis-page {
  min-height: 100vh;
}

.page-body {
  padding: 0 24rpx 24rpx;
  display: flex;
  flex-direction: column;
  row-gap: 24rpx;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.summary-item {
  background: rgba(47, 123, 255, 0.06);
  border-radius: 22rpx;
  padding: 22rpx 24rpx;
  display: flex;
  flex-direction: column;
  row-gap: 10rpx;
}

.summary-label {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.section-title {
  font-size: $font-lg;
  font-weight: 700;
  color: var(--xo-text-primary);
  margin-bottom: 18rpx;
}

.rank-list,
.risk-list {
  display: flex;
  flex-direction: column;
  row-gap: 20rpx;
}

.rank-item {
  display: flex;
  flex-direction: column;
  row-gap: 10rpx;
}

.rank-head,
.rank-sub,
.risk-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rank-name,
.risk-label {
  font-size: $font-md;
  color: var(--xo-text-primary);
  font-weight: 600;
}

.rank-profit,
.rank-rate {
  font-size: $font-sm;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.rank-profit.profit-positive,
.rank-rate.profit-positive,
.detail-main-value.profit-positive,
.detail-sub-value.profit-positive {
  color: var(--xo-profit-positive);
}

.rank-profit.profit-negative,
.rank-rate.profit-negative,
.detail-main-value.profit-negative,
.detail-sub-value.profit-negative {
  color: var(--xo-profit-negative);
}

.rank-code {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.rank-bar-track,
.risk-track {
  width: 100%;
  height: 12rpx;
  border-radius: 999rpx;
  background: rgba(47, 123, 255, 0.08);
  overflow: hidden;
}

.rank-bar-fill,
.risk-fill {
  height: 100%;
  border-radius: 999rpx;
  background: var(--xo-gradient-button-primary);
}

.detail-table {
  display: flex;
  flex-direction: column;
}

.detail-head,
.detail-row {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr) minmax(0, 0.8fr);
  column-gap: 16rpx;
}

.detail-head {
  padding-bottom: 18rpx;
  border-bottom: 1rpx solid var(--xo-border-color);
}

.detail-col {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.detail-col-mid,
.detail-col-right {
  text-align: right;
}

.detail-row {
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid var(--xo-border-color);
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  row-gap: 8rpx;
}

.detail-cell-mid,
.detail-cell-right {
  align-items: flex-end;
}

.detail-name {
  font-size: $font-md;
  color: var(--xo-text-primary);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-code,
.detail-sub-value {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
  font-variant-numeric: tabular-nums;
}

.detail-main-value {
  font-size: $font-md;
  color: var(--xo-text-primary);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.risk-label-wrap {
  display: flex;
  align-items: center;
}

.risk-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 12rpx;
}

.risk-percent {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
  font-variant-numeric: tabular-nums;
}
</style>
