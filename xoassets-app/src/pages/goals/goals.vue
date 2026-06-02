<template>
  <view class="goals-page safe-bottom">
    <view v-if="goals.length" class="goal-list">
      <view v-for="g in goals" :key="g.id" class="goal-card card">
        <view class="gc-top">
          <text class="gc-name">{{ g.name }}</text>
          <text class="gc-status" :class="g.status === 'DONE' ? 'done' : 'active'">{{ g.statusLabel }}</text>
        </view>
        <view class="gc-progress">
          <view class="progress-bar">
            <view class="progress-fill" :style="{ width: Math.min(g.completionRate, 100) + '%' }" />
          </view>
          <text class="gc-rate">{{ g.completionRate.toFixed(1) }}%</text>
        </view>
        <view class="gc-info">
          <text class="gc-amount">{{ fmtAmount(g.currentAmount) }} / {{ fmtAmount(g.targetAmount) }}</text>
          <text class="gc-left">还差 {{ fmtAmount(g.remainingAmount) }}</text>
        </view>
        <view v-if="g.daysLeft > 0" class="gc-meta">
          <text>剩余 {{ g.daysLeft }} 天 · 每月需 {{ fmtAmount(g.monthlyRequiredAmount) }}</text>
        </view>
      </view>
    </view>

    <view v-else class="empty-state"><text>暂无资产目标</text></view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { goalApi, type GoalItem } from '@/services/goalApi'

const goals = ref<GoalItem[]>([])

onMounted(async () => {
  try {
    goals.value = await goalApi.list()
  } catch {}
})

function fmtAmount(v: number | undefined) {
  if (v == null) return '--'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.goals-page { min-height: 100vh; background: $bg-color; padding: $spacing-sm; }
.goal-list { display: flex; flex-direction: column; gap: $spacing-sm; }
.gc-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.gc-name { font-size: $font-lg; font-weight: 600; color: $text-primary; }
.gc-status { font-size: $font-xs; padding: 4rpx 16rpx; border-radius: 8rpx;
  &.active { background: rgba($primary-color,0.1); color: $primary-color; }
  &.done { background: rgba($success-color,0.1); color: $success-color; }
}
.gc-progress { display: flex; align-items: center; gap: 12rpx; margin-bottom: 12rpx; }
.progress-bar { flex: 1; height: 12rpx; background: $border-color; border-radius: 6rpx; overflow: hidden; }
.progress-fill { height: 100%; background: $primary-color; border-radius: 6rpx; }
.gc-rate { font-size: $font-sm; color: $primary-color; font-weight: 600; }
.gc-info { display: flex; justify-content: space-between; }
.gc-amount { font-size: $font-sm; color: $text-regular; }
.gc-left { font-size: $font-sm; color: $text-secondary; }
.gc-meta { margin-top: 8rpx;
  text { font-size: $font-xs; color: $text-secondary; }
}
</style>
