<template>
  <view class="budget-page safe-bottom">
    <view class="summary-card card" v-if="summary">
      <view class="budget-progress">
        <text class="bp-label">本月预算使用</text>
        <text class="bp-amount">{{ fmtAmount(summary.totalUsed) }} / {{ fmtAmount(summary.totalBudget) }}</text>
      </view>
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: Math.min(summary.usageRate, 100) + '%' }"
          :class="usageClass(summary.usageRate)" />
      </view>
      <text class="bp-rate">已使用 {{ summary.usageRate.toFixed(1) }}%</text>
    </view>

    <!-- 预算明细 -->
    <view v-if="budgets.length" class="budget-list">
      <view v-for="b in budgets" :key="b.id" class="budget-item card">
        <view class="bi-top">
          <text class="bi-name">{{ b.categoryName || '总预算' }}</text>
          <text class="bi-used">{{ fmtAmount(b.usedAmount) }} / {{ fmtAmount(b.amount) }}</text>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: Math.min(b.usageRate, 100) + '%' }"
            :class="usageClass(b.usageRate)" />
        </view>
        <text class="bi-status">{{ b.usageStatusLabel }}</text>
      </view>
    </view>

    <view v-else class="empty-state"><text>本月暂无预算</text></view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { budgetApi, type BudgetItem } from '@/services/budgetApi'

const budgets = ref<BudgetItem[]>([])
const summary = ref<any>(null)
const month = ref(new Date().toISOString().slice(0, 7))

onMounted(async () => {
  try {
    const [items, sm] = await Promise.all([
      budgetApi.list(month.value),
      budgetApi.summary(month.value)
    ])
    budgets.value = items
    summary.value = sm
  } catch {}
})

function fmtAmount(v: number | undefined) {
  if (v == null) return '--'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function usageClass(rate: number) {
  if (rate >= 100) return 'over'
  if (rate >= 80) return 'warning'
  return 'normal'
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.budget-page { min-height: 100vh; background: $bg-color; padding: $spacing-sm; }
.summary-card { margin-bottom: $spacing-md; }
.budget-progress { display: flex; justify-content: space-between; margin-bottom: 12rpx; }
.bp-label { font-size: $font-md; font-weight: 600; color: $text-primary; }
.bp-amount { font-size: $font-md; color: $text-secondary; }
.progress-bar {
  height: 12rpx; background: $border-color; border-radius: 6rpx; overflow: hidden; margin-bottom: 8rpx;
}
.progress-fill { height: 100%; border-radius: 6rpx; transition: width 0.3s;
  &.normal { background: $primary-color; }
  &.warning { background: $warning-color; }
  &.over { background: $danger-color; }
}
.bp-rate { font-size: $font-xs; color: $text-secondary; }
.budget-list { display: flex; flex-direction: column; gap: $spacing-sm; }
.bi-top { display: flex; justify-content: space-between; margin-bottom: 12rpx; }
.bi-name { font-size: $font-md; font-weight: 600; color: $text-primary; }
.bi-used { font-size: $font-sm; color: $text-secondary; }
.bi-status { font-size: $font-xs; color: $text-secondary; margin-top: 8rpx; display: block; }
</style>
