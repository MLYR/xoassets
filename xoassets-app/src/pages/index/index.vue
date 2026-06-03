<template>
  <view class="home-page safe-bottom">
    <!-- 顶部欢迎区 -->
    <view class="header-section">
      <view class="header-top">
        <text class="greeting">{{ greetingText }}</text>
        <text class="username">{{ authStore.user?.nickname || authStore.user?.username }}</text>
      </view>
      <text class="net-assets-label">净资产</text>
      <text class="net-assets-value">{{ fmtAmount(overview?.netAssets) }}</text>
    </view>

    <!-- 资产概览卡片 -->
    <view class="overview-cards">
      <view class="overview-card">
        <text class="oc-label">总资产</text>
        <text class="oc-value">{{ fmtAmount(overview?.totalAssets) }}</text>
      </view>
      <view class="overview-card">
        <text class="oc-label">本月收入</text>
        <text class="oc-value income">{{ fmtAmount(overview?.monthlyIncome) }}</text>
      </view>
      <view class="overview-card">
        <text class="oc-label">本月支出</text>
        <text class="oc-value expense">{{ fmtAmount(overview?.monthlyExpense) }}</text>
      </view>
      <view class="overview-card">
        <text class="oc-label">今日支出</text>
        <text class="oc-value expense">{{ fmtAmount(overview?.todayExpense) }}</text>
      </view>
      <view class="overview-card">
        <text class="oc-label">投资市值</text>
        <text class="oc-value">{{ fmtAmount(overview?.investmentMarketValue) }}</text>
      </view>
      <view class="overview-card">
        <text class="oc-label">浮动盈亏</text>
        <text class="oc-value" :class="getProfitClass(overview?.investmentFloatingProfit)">
          {{ fmtAmount(overview?.investmentFloatingProfit) }}
        </text>
      </view>
    </view>

    <!-- 最近流水 -->
    <view class="section-card">
      <view class="section-header">
        <text class="section-title">最近流水</text>
        <text class="section-link" @click="goTransactions">查看全部</text>
      </view>
      <view v-if="recentTransactions.length" class="txn-list">
        <view v-for="t in recentTransactions" :key="t.id" class="txn-item" @click="goTransactionDetail(t.id)">
          <view class="txn-left">
            <text class="txn-category">{{ t.categoryName || '未分类' }}</text>
            <text class="txn-time">{{ fmtTime(t.transactionTime) }}</text>
          </view>
          <text class="txn-amount" :class="getAmountClass(t.type)">{{ fmtSignedAmount(t) }}</text>
        </view>
      </view>
      <view v-else class="empty-state">
        <text>暂无流水记录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import { useDashboardStore } from '@/stores/dashboard'
import type { TransactionItem } from '@/services/dashboardApi'

const authStore = useAuthStore()
const store = useDashboardStore()

const overview = computed(() => store.overview)
const recentTransactions = computed(() => store.overview?.recentTransactions || [])

// 根据当前时间生成问候语
const now = new Date()
const hour = now.getHours()
const greetingText = computed(() => {
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

onShow(() => {
  store.fetchOverview()
})

// 金额格式化
function fmtAmount(v: number | undefined | null): string {
  if (v == null || v === undefined) return '--'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtSignedAmount(t: TransactionItem): string {
  const prefix = t.type === 'INCOME' ? '+' : t.type === 'EXPENSE' ? '-' : ''
  return prefix + fmtAmount(t.amount)
}

function fmtTime(s: string): string {
  if (!s) return ''
  return s.slice(5, 16).replace('T', ' ')
}

function getAmountClass(type: string): string {
  if (type === 'INCOME') return 'income'
  if (type === 'EXPENSE') return 'expense'
  return 'transfer'
}

function getProfitClass(v: number | undefined | null): string {
  if (v == null || v === undefined) return ''
  return v >= 0 ? 'income' : 'expense'
}

function goTransactions() {
  uni.navigateTo({ url: '/pages/transactions/transactions' })
}

function goTransactionDetail(id: string) {
  uni.navigateTo({ url: `/pages/transaction-detail/transaction-detail?id=${id}` })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.home-page {
  padding-bottom: 40rpx;
  background: var(--xo-page-bg);
}

/* 顶部 */
.header-section {
  background: var(--xo-bg-home-asset-card);
  padding: 40rpx $spacing-lg 60rpx;
  border-radius: 0 0 40rpx 40rpx;
}
.header-top {
  margin-bottom: 32rpx;
}
.greeting {
  font-size: $font-md;
  color: var(--xo-white-85);
}
.username {
  font-size: $font-xl;
  color: var(--xo-white);
  font-weight: 700;
  margin-left: 12rpx;
}
.net-assets-label {
  font-size: $font-sm;
  color: var(--xo-white-75);
  display: block;
  margin-bottom: 8rpx;
}
.net-assets-value {
  font-size: $amount-huge;
  font-weight: 800;
  color: var(--xo-white);
  font-variant-numeric: tabular-nums;
}

/* 概览卡片 */
.overview-cards {
  display: flex;
  flex-wrap: wrap;
  margin: -30rpx $spacing-sm $spacing-sm;
}
.overview-card {
  width: calc(33.33% - 24rpx);
  background: var(--xo-component-card-bg);
  border-radius: var(--xo-component-card-radius);
  box-shadow: var(--xo-component-card-shadow);
  padding: $spacing-md $spacing-sm;
  margin: 0 12rpx 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.oc-label {
  font-size: $font-xs;
  color: var(--xo-text-secondary);
  margin-bottom: 8rpx;
}
.oc-value {
  font-size: $amount-sm;
  font-weight: 700;
  color: var(--xo-text-primary);
  font-variant-numeric: tabular-nums;
  &.income { color: var(--xo-positive); }
  &.expense { color: var(--xo-negative); }
}

/* 流水列表 */
.section-card {
  background: var(--xo-component-card-bg);
  border-radius: var(--xo-component-card-radius);
  box-shadow: var(--xo-component-card-shadow);
  margin: 0 $spacing-sm;
  padding: $spacing-md;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-sm;
}
.section-title {
  font-size: $font-lg;
  font-weight: 700;
  color: var(--xo-text-primary);
}
.section-link {
  font-size: $font-sm;
  color: var(--xo-primary);
}
.txn-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid var(--xo-border-color);
  &:last-child { border-bottom: none; }
}
.txn-left {
  display: flex;
  flex-direction: column;
}
.txn-category {
  font-size: $font-md;
  color: var(--xo-text-primary);
  margin-bottom: 4rpx;
}
.txn-time {
  font-size: $font-xs;
  color: var(--xo-text-secondary);
}
.txn-amount {
  font-size: $amount-sm;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  &.income { color: var(--xo-positive); }
  &.expense { color: var(--xo-negative); }
  &.transfer { color: var(--xo-transfer); }
}
</style>
