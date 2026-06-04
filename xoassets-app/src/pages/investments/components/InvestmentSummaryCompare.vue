<template>
  <view class="summary-compare-list">
    <view class="summary-compare-row">
      <text class="summary-compare-label">昨日收益</text>
      <text class="summary-compare-value" :class="profitClass(metrics.vsYesterdayAmount)">{{ fmtOptionalSigned(metrics.vsYesterdayAmount) }}</text>
      <text class="summary-compare-rate" :class="profitClass(metrics.vsYesterdayRate)">{{ fmtOptionalPercent(metrics.vsYesterdayRate) }}</text>
    </view>
    <view class="summary-compare-row">
      <text class="summary-compare-label">今日收益</text>
      <text class="summary-compare-value" :class="profitClass(metrics.vsLastMonthAmount)">{{ fmtOptionalSigned(metrics.vsLastMonthAmount) }}</text>
      <text class="summary-compare-rate" :class="profitClass(metrics.vsLastMonthRate)">{{ fmtOptionalPercent(metrics.vsLastMonthRate) }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { fmtPercent, fmtSigned, profitClass } from '../helpers'

type SummaryCompareMetrics = {
  vsYesterdayAmount: number | null
  vsYesterdayRate: number | null
  vsLastMonthAmount: number | null
  vsLastMonthRate: number | null
}

// 汇总对比组件只负责展示后端/ helper 已确定的业务口径，避免页面重复格式化逻辑。
defineProps<{
  metrics: SummaryCompareMetrics
}>()

function fmtOptionalSigned(value: number | null) {
  return value == null ? '--' : fmtSigned(value)
}

function fmtOptionalPercent(value: number | null) {
  return value == null ? '--' : fmtPercent(value)
}
</script>

<style scoped lang="scss">
@import '@/styles/variables.scss';

.summary-compare-list {
  display: flex;
  flex-direction: column;
  row-gap: var(--xo-invest-summary-row-gap);
}

.summary-compare-row {
  display: flex;
  align-items: center;
  column-gap: var(--xo-invest-summary-gap);
  font-size: 24rpx;
  white-space: nowrap;
}

.summary-compare-label,
.summary-compare-value,
.summary-compare-rate {
  color: rgba(255, 255, 255, 0.94);
}

.summary-compare-label {
  width: var(--xo-invest-summary-label-width);
  color: rgba(255, 255, 255, 0.8);
}

.summary-compare-value,
.summary-compare-rate {
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.summary-compare-value.profit-positive,
.summary-compare-rate.profit-positive {
  color: var(--xo-profit-positive);
}

.summary-compare-value.profit-negative,
.summary-compare-rate.profit-negative {
  color: var(--xo-profit-negative);
}
</style>
