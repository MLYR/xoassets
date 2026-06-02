<template>
  <view class="mine-page safe-bottom">
    <!-- 用户信息 -->
    <view class="user-section">
      <view class="avatar">
        <text class="avatar-text">{{ initial }}</text>
      </view>
      <view class="user-info">
        <text class="user-name">{{ authStore.user?.nickname || authStore.user?.username || '未登录' }}</text>
        <text class="user-id">@{{ authStore.user?.username }}</text>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="menu-section">
      <view class="menu-item" @click="goPage('/pages/categories/categories')">
        <text class="menu-icon">📂</text>
        <text class="menu-label">分类管理</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goPage('/pages/budgets/budgets')">
        <text class="menu-icon">💰</text>
        <text class="menu-label">预算管理</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goPage('/pages/goals/goals')">
        <text class="menu-icon">🎯</text>
        <text class="menu-label">资产目标</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goPage('/pages/reports/reports')">
        <text class="menu-icon">📊</text>
        <text class="menu-label">AI 报告</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 退出 -->
    <view class="logout-area">
      <view class="btn-outline danger" @click="handleLogout">
        <text>退出登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const initial = computed(() => {
  const name = authStore.user?.nickname || authStore.user?.username || '?'
  return name[0]?.toUpperCase() || '?'
})

function goPage(url: string) {
  uni.navigateTo({ url })
}

function handleLogout() {
  uni.showModal({
    title: '退出确认',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        authStore.logout()
      }
    }
  })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.mine-page { min-height: 100vh; background: $bg-color; }
.user-section {
  background: linear-gradient(135deg, #4A90D9, #6BA5E7);
  padding: 60rpx $spacing-lg 48rpx;
  display: flex;
  align-items: center;
  margin-bottom: $spacing-md;
}
.avatar {
  width: 96rpx; height: 96rpx;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
}
.avatar-text { font-size: 40rpx; font-weight: 700; color: #fff; }
.user-info { display: flex; flex-direction: column; }
.user-name { font-size: $font-xl; font-weight: 700; color: #fff; }
.user-id { font-size: $font-sm; color: rgba(255,255,255,0.75); margin-top: 4rpx; }

.menu-section {
  background: #fff;
  border-radius: $border-radius;
  margin: 0 $spacing-sm $spacing-lg;
  overflow: hidden;
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx $spacing-md;
  border-bottom: 1rpx solid $border-color;
  &:last-child { border-bottom: none; }
}
.menu-icon { font-size: 36rpx; margin-right: 20rpx; }
.menu-label { flex: 1; font-size: $font-md; color: $text-primary; font-weight: 500; }
.menu-arrow { font-size: 32rpx; color: $text-placeholder; }

.logout-area { padding: $spacing-xl $spacing-lg; }
.danger { border-color: $danger-color; color: $danger-color; }
</style>
