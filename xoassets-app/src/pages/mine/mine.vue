<template>
  <view class="mine-page safe-bottom">
    <AppNavBar title="我的" />

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
        <view class="menu-icon">
          <AppIcon name="menu.categories" size="34rpx" />
        </view>
        <text class="menu-label">分类管理</text>
        <AppIcon name="common.arrowRight" size="26rpx" />
      </view>
      <view class="menu-item" @click="goPage('/pages/budgets/budgets')">
        <view class="menu-icon">
          <AppIcon name="menu.budgets" size="34rpx" />
        </view>
        <text class="menu-label">预算管理</text>
        <AppIcon name="common.arrowRight" size="26rpx" />
      </view>
      <view class="menu-item" @click="goPage('/pages/goals/goals')">
        <view class="menu-icon">
          <AppIcon name="menu.goals" size="34rpx" />
        </view>
        <text class="menu-label">资产目标</text>
        <AppIcon name="common.arrowRight" size="26rpx" />
      </view>
      <view class="menu-item" @click="goPage('/pages/reports/reports')">
        <view class="menu-icon">
          <AppIcon name="menu.reports" size="34rpx" />
        </view>
        <text class="menu-label">AI 报告</text>
        <AppIcon name="common.arrowRight" size="26rpx" />
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
import AppIcon from '@/components/app/AppIcon.vue'
import AppNavBar from '@/components/app/AppNavBar.vue'
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
.mine-page { min-height: 100vh; background: var(--xo-page-bg); }
.user-section {
  background: var(--xo-gradient-page-header);
  padding: 60rpx $spacing-lg 48rpx;
  display: flex;
  align-items: center;
  margin-bottom: $spacing-md;
}
.avatar {
  width: 96rpx; height: 96rpx;
  border-radius: 50%;
  background: var(--xo-white-25);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
}
.avatar-text { font-size: 40rpx; font-weight: 700; color: var(--xo-white); }
.user-info { display: flex; flex-direction: column; }
.user-name { font-size: $font-xl; font-weight: 700; color: var(--xo-white); }
.user-id { font-size: $font-sm; color: var(--xo-white-75); margin-top: 4rpx; }

.menu-section {
  background: var(--xo-component-card-bg);
  border-radius: var(--xo-component-card-radius);
  margin: 0 $spacing-sm $spacing-lg;
  overflow: hidden;
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx $spacing-md;
  border-bottom: 1rpx solid var(--xo-border-color);
  &:last-child { border-bottom: none; }
}
.menu-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: var(--xo-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  color: var(--xo-primary);
  background: var(--xo-primary-soft);
  font-size: 24rpx;
  font-weight: 700;
}
.menu-label { flex: 1; font-size: $font-md; color: var(--xo-text-primary); font-weight: 500; }
.menu-arrow { font-size: 32rpx; color: var(--xo-text-placeholder); }

.logout-area { padding: $spacing-xl $spacing-lg; }
.danger { border-color: var(--xo-negative); color: var(--xo-negative); }
</style>
