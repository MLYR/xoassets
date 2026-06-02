<template>
  <view class="reports-page safe-bottom">
    <view v-if="reports.length" class="report-list">
      <view v-for="r in reports" :key="r.id" class="report-card card" @click="viewReport(r)">
        <view class="rc-top">
          <text class="rc-title">{{ r.title }}</text>
          <text class="rc-status" :class="r.status === 'FAILED' ? 'failed' : ''">{{ r.statusLabel }}</text>
        </view>
        <text class="rc-date">{{ r.reportDate }}</text>
        <text class="rc-preview">{{ truncateContent(r.content) }}</text>
      </view>
    </view>

    <view v-else class="empty-state"><text>暂无报告</text></view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { reportApi, type AiReportItem } from '@/services/reportApi'

const reports = ref<AiReportItem[]>([])

onMounted(async () => {
  try {
    reports.value = await reportApi.list()
  } catch {}
})

function truncateContent(c: string) {
  return c ? c.slice(0, 120) + '…' : ''
}

function viewReport(r: AiReportItem) {
  uni.showToast({ title: r.title, icon: 'none' })
  // 第一版简化处理：展示标题
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.reports-page { min-height: 100vh; background: $bg-color; padding: $spacing-sm; }
.report-list { display: flex; flex-direction: column; gap: $spacing-sm; }
.rc-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8rpx; }
.rc-title { font-size: $font-md; font-weight: 600; color: $text-primary; }
.rc-status { font-size: $font-xs; padding: 2rpx 12rpx; border-radius: 8rpx; background: rgba($success-color,0.1); color: $success-color;
  &.failed { background: rgba($danger-color,0.1); color: $danger-color; }
}
.rc-date { font-size: $font-xs; color: $text-secondary; display: block; margin-bottom: 12rpx; }
.rc-preview { font-size: $font-sm; color: $text-regular; line-height: 1.6; }
</style>
