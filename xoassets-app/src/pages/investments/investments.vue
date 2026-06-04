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
          <InvestmentSummaryCompare :metrics="summaryMetrics" />
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
      <AppSectionHeader title="资产分布" action-text="更多" @action="handleDistributionMore" />

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
      <AppSectionHeader title="持仓" action-text="全部持仓" @action="handleAllHoldings" />

      <view class="holding-table">
        <view class="holding-header">
          <text class="holding-col holding-col-name">名称/代码</text>
          <text class="holding-col holding-col-amount">总金额/昨日/今日收益</text>
          <text class="holding-col holding-col-profit">持有收益 / 率</text>
        </view>

        <view v-if="holdingRows.length" class="holding-body">
          <InvestmentHoldingRow
            v-for="row in holdingRows"
            :key="row.id"
            :row="row"
            @click="goDetail(row)"
          />
        </view>

        <view v-else-if="!loading" class="empty-state">
          <text>暂无持仓</text>
        </view>
      </view>

      <view class="holding-analysis-entry" @click="handleAllHoldings">
        <text class="holding-analysis-text">持仓分析</text>
        <AppIcon name="common.arrowRight" size="24rpx" />
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
        :height="investmentTokens.actionCapsuleHeight"
        block
        @click="handleTradeAction('buy')"
      />
      <AppActionButton
        class="trade-action-button"
        text="转换"
        type="purple"
        icon="investmentActions.convert"
        :radius="theme.radius.round"
        :height="investmentTokens.actionCapsuleHeight"
        block
        @click="handleTradeAction('convert')"
      />
      <AppActionButton
        class="trade-action-button"
        text="卖出"
        type="sell"
        icon="investmentActions.sell"
        :radius="theme.radius.round"
        :height="investmentTokens.actionCapsuleHeight"
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
import AppIcon from '@/components/app/AppIcon.vue'
import AppPage from '@/components/app/AppPage.vue'
import AppSectionHeader from '@/components/app/AppSectionHeader.vue'
import InvestmentHoldingRow from './components/InvestmentHoldingRow.vue'
import InvestmentSummaryCompare from './components/InvestmentSummaryCompare.vue'
import { useInvestmentStore } from '@/stores/investment'
import { useTheme } from '@/theme/useTheme'
import {
  buildDistributionItems,
  buildHoldingRows,
  buildSummaryMetrics,
  fmtAmount,
  fmtPercentNumber,
  type HoldingRow
} from './helpers'

const store = useInvestmentStore()
const { currentTheme, investmentTokens } = useTheme()

const holdings = computed(() => store.holdings)
const summary = computed(() => store.summary)
const loading = computed(() => store.loading)
const theme = computed(() => currentTheme.value)

onShow(() => {
  store.fetchHoldings()
})

const summaryMetrics = computed(() => buildSummaryMetrics(summary.value))

const distributionItems = computed(() => {
  return buildDistributionItems(holdings.value, theme.value.charts.investmentDistribution, summary.value?.totalMarketValue)
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

const holdingRows = computed(() => buildHoldingRows(holdings.value))

function goDetail(row: HoldingRow) {
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
.summary-side-profit-label {
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

.holding-header {
  display: grid;
  grid-template-columns: var(--xo-invest-holding-grid);
  column-gap: 16rpx;
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

.action-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: var(--xo-invest-action-gap);
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
