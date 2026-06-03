<template>
  <view class="summary-compare-list">
    <view class="summary-compare-row">
      <text class="summary-compare-label">较昨日</text>
      <text class="summary-compare-value">{{ fmtSigned(metrics.vsYesterdayAmount) }}</text>
      <text class="summary-compare-rate">{{ fmtPercent(metrics.vsYesterdayRate) }}</text>
    </view>
    <view class="summary-compare-row">
      <text class="summary-compare-label">较上月</text>
      <text class="summary-compare-value">{{ fmtSigned(metrics.vsLastMonthAmount) }}</text>
      <text class="summary-compare-rate">{{ fmtPercent(metrics.vsLastMonthRate) }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { fmtPercent, fmtSigned } from '../helpers'

type SummaryCompareMetrics = {
  vsYesterdayAmount: number
  vsYesterdayRate: number
  vsLastMonthAmount: number
  vsLastMonthRate: number
}

// 汇总对比组件只负责展示后端/ helper 已确定的业务口径，避免页面重复格式化逻辑。
defineProps<{
  metrics: SummaryCompareMetrics
}>()
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
  font-size: $font-sm;
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
}
</style>
