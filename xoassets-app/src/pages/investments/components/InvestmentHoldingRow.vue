<template>
  <view class="holding-row" @click="emit('click', row)">
    <view class="holding-cell holding-name-cell">
      <text class="holding-name">{{ row.name }}</text>
      <text class="holding-code">{{ row.code }}</text>
    </view>

    <view class="holding-cell holding-amount-cell">
      <text class="holding-market-value">¥ {{ fmtAmount(row.marketValue) }}</text>
      <view class="holding-daily-line">
        <text class="holding-daily-label">{{ row.primaryProfitLabel }}</text>
        <text class="holding-daily-value" :class="profitClass(row.primaryProfitAmount)">
          {{ fmtSignedOrFallback(row.primaryProfitAmount) }}
        </text>
      </view>
    </view>

    <view class="holding-cell holding-profit-cell">
      <AppAmount :value="row.floatingProfit" signed size="sm" semantic="profit" />
      <text class="holding-profit-rate" :class="profitClass(row.floatingProfitRate)">
        {{ fmtPercent(row.floatingProfitRate) }}
      </text>
    </view>
  </view>
</template>

<script setup lang="ts">
import AppAmount from '@/components/app/AppAmount.vue'
import { fmtAmount, fmtPercent, fmtSignedOrFallback, profitClass, type HoldingRow } from '../helpers'

// 持仓行统一展示后端估值字段，禁止从格式化后的价格在前端反算市值/盈亏。
defineProps<{
  row: HoldingRow
}>()

const emit = defineEmits<{
  click: [row: HoldingRow]
}>()
</script>

<style scoped lang="scss">
@import '@/styles/variables.scss';

.holding-row {
  display: grid;
  grid-template-columns: var(--xo-invest-holding-grid);
  column-gap: 16rpx;
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

.holding-daily-label,
.holding-daily-value,
.holding-profit-rate {
  font-size: $font-sm;
  color: var(--xo-text-regular);
  font-variant-numeric: tabular-nums;
}

.holding-daily-label {
  color: var(--xo-text-secondary);
}

.holding-profit-cell {
  align-items: flex-end;
}

.holding-daily-value.profit-positive,
.holding-profit-rate.profit-positive {
  color: var(--xo-profit-positive);
}

.holding-daily-value.profit-negative,
.holding-profit-rate.profit-negative {
  color: var(--xo-profit-negative);
}
</style>
