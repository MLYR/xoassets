<template>
  <AppPage class="home-page" safe-bottom gap="20rpx">
    <AppNavBar title="小〇财迹">
      <template #right>
        <view class="home-icon-button" @click="handleHeaderAction('search')">
          <AppIcon name="home.search" size="30rpx" :color="theme.colors.textPrimary" />
        </view>
        <view class="home-icon-button" @click="handleHeaderAction('notice')">
          <AppIcon name="home.notification" size="30rpx" :color="theme.colors.primary" />
        </view>
      </template>
    </AppNavBar>

    <view class="home-header">
      <view class="home-header-copy">
        <text class="home-greeting">你好，{{ displayName }} 👋</text>
        <text class="home-month">{{ currentMonthText }} ▼</text>
      </view>
    </view>

    <AppCard
      class="hero-card"
      :padding="homeTokens.heroPadding"
      :radius="homeTokens.heroRadius"
      :shadow="false"
      :background="theme.backgrounds.homeHeroCard"
    >
      <view class="hero-card-wave"></view>
      <view class="hero-card-shield"></view>
      <view class="hero-card-main">
        <view class="hero-card-top">
          <view class="hero-title-row">
            <text class="hero-label">净资产（元）</text>
            <AppIcon
              :name="isAmountVisible ? 'home.eye' : 'home.eyeOff'"
              size="26rpx"
              :color="theme.colors.white"
              @click.stop="toggleAmountVisible"
            />
          </view>
          <view class="hero-link" @click="handleAssetAnalysis">
            <AppIcon name="home.assetAnalysis" size="24rpx" :color="theme.colors.white" />
            <text class="hero-link-text">资产分析</text>
          </view>
        </view>
        <AppAmount
          v-if="isAmountVisible"
          :value="homeModel.netAssets"
          prefix="¥ "
          size="lg"
          :color="theme.colors.white"
        />
        <text v-else class="hero-amount-mask">¥ ****</text>
        <view class="hero-change-row">
          <text class="hero-change-label">今日变化</text>
          <text class="hero-change-value" :class="profitClass(homeModel.todayChangeAmount)">
            {{ visibleText(fmtSigned(homeModel.todayChangeAmount)) }}
          </text>
          <text class="hero-change-rate" :class="profitClass(homeModel.todayChangeAmount)">
            {{ visibleText(fmtPercent(homeModel.todayChangeRate)) }}
          </text>
        </view>
      </view>
    </AppCard>

    <view class="monthly-stat-grid">
      <AppCard
        v-for="item in homeModel.stats"
        :key="item.key"
        class="monthly-stat-card"
        :padding="theme.spacing.sm"
        :radius="theme.radius.lg"
      >
        <view class="stat-icon" :class="`is-${item.key}`">
          <AppIcon :name="item.icon" size="28rpx" :color="statIconColor(item.tone)" />
        </view>
        <text class="stat-label">{{ item.label }}</text>
        <AppAmount v-if="isAmountVisible" :value="item.amount" prefix="¥ " size="sm" :tone="item.tone" />
        <text v-else class="stat-amount-mask">¥ ****</text>
      </AppCard>
    </view>

    <view class="insight-grid">
      <AppCard class="trend-card" :padding="theme.spacing.md" :radius="theme.radius.xl">
        <AppSectionHeader title="资产趋势" action-text="更多" @action="handleTrendMore" />
        <text class="card-subtitle">近1个月净资产变化（元）</text>
        <view
          class="trend-chart"
          :style="{ height: homeTokens.chartHeight }"
          @touchstart="onTrendTouchStart"
          @touchend="onTrendTouchEnd"
        >
          <text class="trend-y-label is-top">{{ visibleText(trendYAxisLabels.top) }}</text>
          <text class="trend-y-label is-mid">{{ visibleText(trendYAxisLabels.mid) }}</text>
          <text class="trend-y-label is-bottom">{{ visibleText(trendYAxisLabels.bottom) }}</text>
          <view
            v-for="line in visibleTrendSegments"
            :key="line.key"
            class="trend-line"
            :style="line.style"
          ></view>
          <view
            v-for="point in visibleTrendPoints"
            :key="point.label"
            class="trend-point"
            :style="point.style"
          >
            <view class="trend-dot"></view>
          </view>
          <text v-if="!homeModel.trend.length" class="trend-empty">暂无趋势数据</text>
        </view>
        <view class="trend-axis">
          <text v-for="point in visibleTrendData" :key="point.label" class="trend-axis-label">{{ point.label }}</text>
        </view>
      </AppCard>

      <AppCard class="budget-card" :padding="theme.spacing.md" :radius="theme.radius.xl">
        <AppSectionHeader title="预算进度" action-text="本月" @action="goBudgets">
          <template #prefix>
            <AppIcon name="home.budget" size="34rpx" />
          </template>
        </AppSectionHeader>
        <view class="budget-content">
          <view class="budget-ring" :style="budgetRingStyle">
            <view class="budget-ring-center">
              <text class="budget-rate">{{ visibleText(`${Math.round(homeModel.budget.usageRate)}%`) }}</text>
              <text class="budget-ring-label">已使用</text>
            </view>
          </view>
          <view class="budget-summary">
            <text class="budget-label">已使用</text>
            <text class="budget-value">{{ visibleMoney(homeModel.budget.used) }}</text>
            <text class="budget-label">预算总额</text>
            <text class="budget-value">{{ visibleMoney(homeModel.budget.total) }}</text>
          </view>
        </view>
        <view class="progress-track" :style="{ height: homeTokens.progressHeight }">
          <view class="progress-fill" :style="budgetProgressStyle"></view>
        </view>
        <text class="budget-remaining">剩余 {{ visibleMoney(homeModel.budget.remaining) }}</text>
      </AppCard>
    </view>

    <AppCard
      class="goal-card"
      :padding="theme.spacing.sm"
      :radius="theme.radius.xl"
      :background="theme.backgrounds.homeGoalCard"
      @click="handleGoalDetail"
    >
      <AppSectionHeader title="目标进度" action-text="详情" margin-bottom="8rpx" @action="handleGoalDetail">
        <template #prefix>
          <AppIcon name="home.goal" size="34rpx" />
        </template>
        <template #actionIcon>
          <AppIcon name="common.arrowRight" size="28rpx" />
        </template>
      </AppSectionHeader>
      <view v-if="homeModel.goal" class="goal-main">
        <view class="goal-cover">
          <AppIcon name="home.goal" size="30rpx" :color="theme.colors.primary" />
        </view>
        <view class="goal-info">
          <text class="goal-name">{{ homeModel.goal.name }}</text>
          <text class="goal-target">目标 {{ visibleMoney(homeModel.goal.targetAmount) }}</text>
          <view class="progress-track goal-progress" :style="{ height: homeTokens.progressHeight }">
            <view class="progress-fill goal-progress-fill" :style="goalProgressStyle"></view>
          </view>
          <text class="goal-saved-line">已存 {{ visibleMoney(homeModel.goal.savedAmount) }}</text>
        </view>
        <view class="goal-saved">
          <text class="goal-rate">{{ visibleText(fmtPercentPlain(homeModel.goal.progressRate)) }}</text>
          <text class="goal-saved-label">{{ homeModel.goal.dueText }}</text>
        </view>
      </view>
      <view v-else class="goal-empty" @click.stop="handleGoalDetail">
        <AppIcon name="home.goal" size="34rpx" :color="theme.colors.primary" />
        <text class="goal-empty-text">暂无资产目标</text>
      </view>
    </AppCard>

    <AppCard :padding="theme.spacing.sm" :radius="theme.radius.xl">
      <AppSectionHeader title="快捷操作" margin-bottom="8rpx" />
      <view class="quick-action-grid">
        <view
          v-for="item in homeModel.quickActions"
          :key="item.key"
          class="quick-action"
          @click="handleQuickAction(item.key)"
        >
          <view class="quick-action-icon">
            <AppIcon :name="item.icon" size="34rpx" :color="theme.colors.primary" />
          </view>
          <text class="quick-action-label">{{ item.label }}</text>
        </view>
      </view>
    </AppCard>

    <AppCard :padding="theme.spacing.md" :radius="theme.radius.xl">
      <AppSectionHeader title="最近动态" action-text="全部" @action="goTransactions" />
      <view v-if="homeModel.activities.length" class="activity-list">
        <view
          v-for="item in homeModel.activities"
          :key="item.id"
          class="activity-item"
          @click="goTransactionDetail(item.id)"
        >
          <view class="activity-icon" :class="`is-${item.type}`">
            <AppIcon :name="`recentActivities.${item.iconType}`" size="28rpx" :color="activityIconColor(item.type)" />
          </view>
          <view class="activity-main">
            <text class="activity-title">{{ item.title }}</text>
            <text class="activity-meta">{{ item.account }} · {{ item.time }}</text>
          </view>
          <text class="activity-amount" :class="activityAmountClass(item.type)">
            {{ visibleText(item.displayAmount) }}
          </text>
        </view>
      </view>
      <view v-else class="empty-state">
        <text>暂无流水记录</text>
      </view>
    </AppCard>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import AppAmount from '@/components/app/AppAmount.vue'
import AppCard from '@/components/app/AppCard.vue'
import AppIcon from '@/components/app/AppIcon.vue'
import AppNavBar from '@/components/app/AppNavBar.vue'
import AppPage from '@/components/app/AppPage.vue'
import AppSectionHeader from '@/components/app/AppSectionHeader.vue'
import { budgetApi, type BudgetSummary } from '@/services/budgetApi'
import { dashboardApi, type AssetSnapshotLatest, type AssetTrendPoint } from '@/services/dashboardApi'
import { goalApi, type GoalItem } from '@/services/goalApi'
import { useAuthStore } from '@/stores/auth'
import { useDashboardStore } from '@/stores/dashboard'
import { useTheme } from '@/theme/useTheme'
import {
  buildHomeDashboardModel,
  type HomeActivityType,
  type HomeQuickActionKey,
  type HomeStatCard
} from './adapter'

type TrendPosition = {
  label: string
  x: number
  y: number
}

const PRIVACY_STORAGE_KEY = 'xoassets:home:amount-visible'
const authStore = useAuthStore()
const store = useDashboardStore()
const { currentTheme, homeTokens } = useTheme()
const budgetSummary = ref<BudgetSummary | null>(null)
const goals = ref<GoalItem[] | null>(null)
const latestSnapshot = ref<AssetSnapshotLatest | null>(null)
const netAssetsTrend = ref<AssetTrendPoint[] | null>(null)
const isAmountVisible = ref(uni.getStorageSync(PRIVACY_STORAGE_KEY) !== 'hidden')
const trendWindowStart = ref(Number.MAX_SAFE_INTEGER)
const trendTouchStartX = ref(0)
const trendTouchStartY = ref(0)
const trendWindowSize = 6

const theme = computed(() => currentTheme.value)
const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || 'Mo')
const currentMonthText = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月`
})
const homeModel = computed(() => buildHomeDashboardModel(store.overview, {
  budgetSummary: budgetSummary.value,
  goals: goals.value,
  latestSnapshot: latestSnapshot.value,
  netAssetsTrend: netAssetsTrend.value
}))

const visibleTrendData = computed(() => {
  const list = homeModel.value.trend
  if (list.length <= trendWindowSize) return list
  const maxStart = Math.max(list.length - trendWindowSize, 0)
  const start = Math.min(trendWindowStart.value, maxStart)
  return list.slice(start, start + trendWindowSize)
})

const trendPositions = computed<TrendPosition[]>(() => {
  const values = visibleTrendData.value.map((item) => item.value)
  const max = Math.max(...values, 1)
  const min = Math.min(...values, 0)
  const range = Math.max(max - min, 1)
  const lastIndex = Math.max(visibleTrendData.value.length - 1, 1)

  return visibleTrendData.value.map((item, index) => ({
    label: item.label,
    x: (index / lastIndex) * 100,
    y: 18 + ((max - item.value) / range) * 64
  }))
})

const trendPoints = computed(() => trendPositions.value.map((item) => ({
  label: item.label,
  style: {
    left: `${item.x}%`,
    top: `${item.y}%`,
    borderColor: theme.value.charts.assetTrend.line,
    background: theme.value.charts.assetTrend.point
  }
})))

const visibleTrendPoints = computed(() => (isAmountVisible.value ? trendPoints.value : []))

const trendSegments = computed(() => trendPositions.value.slice(0, -1).map((item, index) => {
  const next = trendPositions.value[index + 1]
  const dx = next.x - item.x
  const dy = next.y - item.y
  const length = Math.sqrt(dx * dx + dy * dy)
  const angle = Math.atan2(dy, dx) * 180 / Math.PI

  return {
    key: `${item.label}-${next.label}`,
    style: {
      left: `${item.x}%`,
      top: `${item.y}%`,
      width: `${length}%`,
      background: theme.value.charts.assetTrend.line,
      transform: `rotate(${angle}deg)`
    }
  }
}))

const trendYAxisLabels = computed(() => {
  const values = homeModel.value.trend.map((item) => item.value)
  if (!values.length) return { top: '--', mid: '--', bottom: '--' }
  const top = roundTrendTop(Math.max(...values, 0))
  return {
    top: formatShortAmount(top),
    mid: formatShortAmount(top / 2),
    bottom: '0'
  }
})

const visibleTrendSegments = computed(() => (isAmountVisible.value ? trendSegments.value : []))

const budgetProgressStyle = computed(() => ({
  width: `${isAmountVisible.value ? homeModel.value.budget.usageRate : 0}%`,
  background: theme.value.charts.budgetProgress.used
}))

const budgetRingStyle = computed(() => ({
  background: `conic-gradient(${theme.value.charts.budgetProgress.used} 0% ${isAmountVisible.value ? homeModel.value.budget.usageRate : 0}%, ${theme.value.charts.budgetProgress.track} ${isAmountVisible.value ? homeModel.value.budget.usageRate : 0}% 100%)`
}))

const goalProgressStyle = computed(() => ({
  width: `${isAmountVisible.value ? (homeModel.value.goal?.progressRate ?? 0) : 0}%`,
  background: theme.value.charts.budgetProgress.remaining
}))

onShow(() => {
  fetchHomeData()
})

onPullDownRefresh(async () => {
  try {
    await fetchHomeData()
  } finally {
    uni.stopPullDownRefresh()
  }
})

async function fetchHomeData() {
  await Promise.allSettled([
    store.fetchOverview(),
    fetchBudgetSummary(),
    fetchGoals(),
    fetchLatestSnapshot(),
    fetchNetAssetsTrend()
  ])
}

async function fetchBudgetSummary() {
  try {
    budgetSummary.value = await budgetApi.summary(currentMonthValue())
  } catch {
    // TODO: 预算汇总失败时保留 adapter fallback，避免首页主数据被预算接口拖垮。
    budgetSummary.value = null
  }
}

async function fetchGoals() {
  try {
    goals.value = await goalApi.list()
  } catch {
    // TODO: 目标列表失败时显示空态，不再使用假目标数据。
    goals.value = null
  }
}

async function fetchLatestSnapshot() {
  try {
    latestSnapshot.value = await dashboardApi.latestSnapshot()
  } catch {
    // TODO: 快照接口失败时今日变化展示 0，避免继续使用假变化值。
    latestSnapshot.value = null
  }
}

async function fetchNetAssetsTrend() {
  try {
    const range = trendDateRange()
    netAssetsTrend.value = await dashboardApi.netAssetsTrend(range.startDate, range.endDate)
  } catch {
    // TODO: 净资产趋势接口失败时展示空态，不再使用原型假曲线。
    netAssetsTrend.value = null
  }
}

function fmtAmount(v: number | undefined | null): string {
  if (v == null || v === undefined) return '--'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtSigned(v: number | undefined | null): string {
  if (v == null || v === undefined) return '--'
  return `${v >= 0 ? '+' : ''}${fmtAmount(v)}`
}

function fmtPercent(v: number | undefined | null): string {
  if (v == null || v === undefined) return '--'
  return `${v >= 0 ? '+' : ''}${v.toFixed(2)}%`
}

function fmtPercentPlain(v: number | undefined | null): string {
  if (v == null || v === undefined) return '--'
  return `${v.toFixed(1)}%`
}

function visibleText(text: string): string {
  return isAmountVisible.value ? text : '****'
}

function visibleMoney(value: number | undefined | null): string {
  return isAmountVisible.value ? `¥ ${fmtAmount(value)}` : '¥ ****'
}

function toggleAmountVisible() {
  isAmountVisible.value = !isAmountVisible.value
  // 首页隐私开关持久化，避免切页回来后短暂露出真实金额。
  uni.setStorageSync(PRIVACY_STORAGE_KEY, isAmountVisible.value ? 'visible' : 'hidden')
}

function onTrendTouchStart(event: TouchEvent) {
  const touch = event.touches[0]
  trendTouchStartX.value = touch?.clientX || 0
  trendTouchStartY.value = touch?.clientY || 0
}

function onTrendTouchEnd(event: TouchEvent) {
  const touch = event.changedTouches[0]
  const deltaX = (touch?.clientX || 0) - trendTouchStartX.value
  const deltaY = (touch?.clientY || 0) - trendTouchStartY.value
  if (Math.abs(deltaX) < 40 || Math.abs(deltaX) < Math.abs(deltaY)) return
  shiftTrendWindow(deltaX < 0 ? 1 : -1)
}

function shiftTrendWindow(delta: number) {
  const list = homeModel.value.trend
  const maxStart = Math.max(list.length - trendWindowSize, 0)
  const current = Math.min(trendWindowStart.value, maxStart)
  trendWindowStart.value = Math.max(0, Math.min(current + delta, maxStart))
}

function currentMonthValue(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function trendDateRange(): { startDate: string; endDate: string } {
  const end = new Date()
  const start = new Date(end.getFullYear(), end.getMonth(), end.getDate() - 29)
  return {
    startDate: formatDateForApi(start),
    endDate: formatDateForApi(end)
  }
}

function formatDateForApi(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function roundTrendTop(value: number): number {
  if (value <= 0) return 0
  const unit = value >= 100000 ? 50000 : value >= 10000 ? 10000 : 1000
  return Math.ceil(value / unit) * unit
}

function formatShortAmount(value: number): string {
  if (value >= 1000) return `${Math.round(value / 1000)}k`
  return `${Math.round(value)}`
}

function profitClass(v: number | undefined | null): string {
  if (v == null || v === undefined) return ''
  return v >= 0 ? 'profit-positive' : 'profit-negative'
}

function statIconColor(tone: HomeStatCard['tone']): string {
  if (tone === 'positive') return theme.value.colors.positive
  if (tone === 'negative') return theme.value.colors.negative
  return theme.value.colors.primary
}

function activityIconColor(type: HomeActivityType): string {
  if (type === 'income' || type === 'refund') return theme.value.colors.positive
  if (type === 'expense') return theme.value.colors.negative
  return theme.value.colors.transfer
}

function activityAmountClass(type: HomeActivityType): string {
  if (type === 'income' || type === 'refund') return 'income'
  if (type === 'expense') return 'expense'
  return 'transfer'
}

function handleHeaderAction(type: 'search' | 'notice') {
  if (type === 'search') {
    uni.navigateTo({ url: '/pages/transactions/transactions' })
    return
  }
  uni.navigateTo({ url: '/pages/reports/reports' })
}

function handleAssetAnalysis() {
  uni.switchTab({ url: '/pages/accounts/accounts' })
}

function handleTrendMore() {
  uni.switchTab({ url: '/pages/accounts/accounts' })
}

function handleGoalDetail() {
  uni.navigateTo({ url: '/pages/goals/goals' })
}

function goBudgets() {
  uni.navigateTo({ url: '/pages/budgets/budgets' })
}

function handleQuickAction(key: HomeQuickActionKey) {
  if (key === 'record') {
    uni.switchTab({ url: '/pages/add/add' })
    return
  }
  if (key === 'transfer') {
    uni.setStorageSync('xoassets:add:pending-type', 'TRANSFER')
    uni.switchTab({ url: '/pages/add/add' })
    return
  }
  if (key === 'invest') {
    uni.switchTab({ url: '/pages/investments/investments' })
    return
  }
  uni.navigateTo({ url: '/pages/budgets/budgets' })
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
  background: var(--xo-bg-page);
  width: 100vw !important;
  max-width: 100vw;
  overflow-x: hidden;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  column-gap: 24rpx;
}

.home-header-copy {
  display: flex;
  flex-direction: column;
  row-gap: 8rpx;
  min-width: 0;
}

.home-greeting {
  font-size: 44rpx;
  font-weight: 800;
  color: var(--xo-text-primary);
}

.home-month {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.home-header-actions {
  display: flex;
  align-items: center;
  column-gap: 22rpx;
}

.home-icon-button {
  width: 42rpx;
  height: 42rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-card {
  position: relative;
  overflow: hidden;
}

.hero-card-wave {
  position: absolute;
  right: 22rpx;
  bottom: 28rpx;
  width: 280rpx;
  height: 92rpx;
  border-radius: 50%;
  border: 2rpx solid rgba(255, 255, 255, 0.18);
  border-left-color: transparent;
  border-right-color: transparent;
  opacity: 0.8;
  transform: rotate(-7deg);
}

.hero-card-wave::before,
.hero-card-wave::after {
  content: '';
  position: absolute;
  inset: 16rpx 18rpx 16rpx 18rpx;
  border-radius: inherit;
  border: 2rpx solid rgba(255, 255, 255, 0.14);
  border-left-color: transparent;
  border-right-color: transparent;
}

.hero-card-wave::after {
  inset: 30rpx 38rpx 6rpx 38rpx;
  opacity: 0.72;
}

.hero-card-shield {
  position: absolute;
  right: 26rpx;
  bottom: 22rpx;
  width: 140rpx;
  height: 150rpx;
  border: 8rpx solid rgba(255, 255, 255, 0.12);
  border-top-left-radius: 54rpx;
  border-top-right-radius: 54rpx;
  border-bottom-left-radius: 38rpx;
  border-bottom-right-radius: 38rpx;
  clip-path: polygon(50% 0%, 90% 14%, 90% 58%, 50% 100%, 10% 58%, 10% 14%);
  opacity: 0.9;
}

.hero-card-shield::before {
  content: '';
  position: absolute;
  inset: 34rpx 30rpx 46rpx;
  border-left: 10rpx solid transparent;
  border-right: 10rpx solid transparent;
  border-bottom: 18rpx solid rgba(255, 255, 255, 0.16);
  transform: rotate(45deg);
  border-radius: 4rpx;
}

.hero-card-main {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  row-gap: 10rpx;
}

.hero-card :deep(.app-amount) {
  font-size: 60rpx !important;
}

.hero-amount-mask {
  font-size: 60rpx;
  line-height: 1.2;
  color: var(--xo-white);
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.hero-title-row {
  display: flex;
  align-items: center;
  column-gap: 14rpx;
}

.hero-card-top,
.hero-link,
.hero-change-row,
.goal-main,
.activity-item {
  display: flex;
  align-items: center;
}

.hero-card-top {
  justify-content: space-between;
}

.hero-label,
.hero-link-text,
.hero-change-label,
.hero-change-value,
.hero-change-rate {
  color: var(--xo-white-85);
}

.hero-label {
  font-size: $font-sm;
}

.hero-link {
  column-gap: 8rpx;
  padding: 10rpx 16rpx;
  border-radius: var(--xo-radius-round);
  background: var(--xo-white-25);
}

.hero-link-text {
  font-size: $font-sm;
  font-weight: 600;
}

.hero-change-row {
  column-gap: 14rpx;
  font-size: $font-sm;
}

.hero-change-value,
.hero-change-rate {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.hero-change-value.profit-positive,
.hero-change-rate.profit-positive {
  color: var(--xo-profit-positive);
}

.hero-change-value.profit-negative,
.hero-change-rate.profit-negative {
  color: var(--xo-profit-negative);
}

.monthly-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: var(--xo-home-stat-grid-gap);
  width: 100%;
  min-width: 0;
}

.monthly-stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  row-gap: 4rpx;
  min-height: 124rpx;
  min-width: 0;
}

.monthly-stat-card :deep(.app-amount) {
  font-size: 28rpx !important;
  white-space: nowrap;
}

.stat-amount-mask {
  font-size: 28rpx;
  color: var(--xo-text-primary);
  font-weight: 700;
  white-space: nowrap;
}

.stat-icon,
.quick-action-icon,
.activity-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--xo-radius-lg);
  background: var(--xo-primary-soft);
}

.stat-icon {
  width: 46rpx;
  height: 46rpx;
}

.stat-icon.is-income,
.activity-icon.is-income,
.activity-icon.is-refund {
  background: var(--xo-positive-soft);
}

.stat-icon.is-expense,
.activity-icon.is-expense {
  background: var(--xo-negative-soft);
}

.stat-icon.is-balance {
  background: var(--xo-transfer-soft);
}

.stat-label,
.budget-label,
.goal-target,
.goal-saved-label,
.goal-saved-line,
.activity-meta {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.insight-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.9fr);
  column-gap: 18rpx;
  width: 100%;
  min-width: 0;
}

.trend-card,
.budget-card {
  min-height: 252rpx;
  min-width: 0;
}

.card-subtitle {
  display: block;
  margin-bottom: 4rpx;
  font-size: $font-xs;
  color: var(--xo-text-secondary);
}

.trend-chart {
  position: relative;
  margin-top: 8rpx;
  border-radius: var(--xo-radius-lg);
  background: linear-gradient(180deg, var(--xo-primary-soft), transparent);
  overflow: hidden;
  touch-action: pan-y;
}

.trend-chart::before,
.trend-chart::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  border-top: 1rpx dashed var(--xo-border-color);
}

.trend-chart::before {
  top: 38%;
}

.trend-chart::after {
  top: 70%;
}

.trend-y-label {
  position: absolute;
  left: 0;
  font-size: 20rpx;
  color: var(--xo-text-secondary);
}

.trend-y-label.is-top {
  top: 10%;
}

.trend-y-label.is-mid {
  top: 43%;
}

.trend-y-label.is-bottom {
  bottom: 6%;
}

.trend-line {
  position: absolute;
  height: 5rpx;
  border-radius: var(--xo-radius-round);
  transform-origin: left center;
}

.trend-point {
  position: absolute;
  width: 18rpx;
  height: 18rpx;
  margin-left: -9rpx;
  margin-top: -9rpx;
  border: 4rpx solid;
  border-radius: 50%;
  box-sizing: border-box;
}

.trend-dot {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.trend-empty {
  position: absolute;
  left: 50%;
  top: 50%;
  color: var(--xo-text-secondary);
  font-size: $font-sm;
  transform: translate(-50%, -50%);
}

.trend-axis {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  margin-top: 12rpx;
}

.trend-axis-label {
  text-align: center;
  font-size: $font-xs;
  color: var(--xo-text-secondary);
}

.budget-content {
  display: grid;
  grid-template-columns: 112rpx minmax(0, 1fr);
  align-items: center;
  column-gap: 16rpx;
}

.budget-ring {
  width: 108rpx;
  height: 108rpx;
  padding: 8rpx;
  border-radius: 50%;
  box-sizing: border-box;
}

.budget-ring-center {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--xo-component-card-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  row-gap: 4rpx;
}

.budget-rate {
  font-size: 28rpx;
  color: var(--xo-text-primary);
  font-weight: 700;
}

.budget-ring-label,
.budget-remaining {
  font-size: 20rpx;
  color: var(--xo-text-secondary);
}

.budget-summary,
.goal-saved {
  display: flex;
  flex-direction: column;
  row-gap: 6rpx;
}

.budget-summary {
  align-items: flex-start;
}

.budget-value {
  font-size: 20rpx;
  color: var(--xo-text-primary);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.progress-track {
  margin-top: 14rpx;
  border-radius: var(--xo-radius-round);
  background: var(--xo-border-color);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
}

.budget-remaining {
  display: block;
  margin-top: 8rpx;
  color: var(--xo-primary);
}

.goal-card {
  overflow: hidden;
}

.goal-main {
  column-gap: 18rpx;
}

.goal-cover {
  position: relative;
  width: 86rpx;
  height: 86rpx;
  flex-shrink: 0;
  border-radius: var(--xo-radius-lg);
  overflow: hidden;
  background: var(--xo-bg-home-goal-cover);
  display: flex;
  align-items: center;
  justify-content: center;
}

.goal-cover::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 22rpx;
  height: 20rpx;
  background: var(--xo-white-75);
  opacity: 0.72;
  transform: skewY(-8deg);
}

.goal-saved {
  align-items: flex-end;
  flex-shrink: 0;
}

.goal-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  row-gap: 6rpx;
}

.goal-name {
  font-size: 30rpx;
  color: var(--xo-text-primary);
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.goal-progress {
  margin-top: 0;
}

.goal-rate {
  font-size: $font-lg;
  color: var(--xo-primary);
  font-weight: 700;
}

.goal-saved-label {
  font-size: 22rpx;
  white-space: nowrap;
}

.goal-empty {
  min-height: 86rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  column-gap: 12rpx;
  color: var(--xo-text-secondary);
}

.goal-empty-text {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.quick-action-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  column-gap: var(--xo-home-quick-action-gap);
}

.quick-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  row-gap: 8rpx;
}

.quick-action-icon {
  width: 58rpx;
  height: 58rpx;
}

.quick-action-label {
  font-size: $font-sm;
  color: var(--xo-text-primary);
  font-weight: 600;
}

.activity-list {
  display: flex;
  flex-direction: column;
}

.activity-item {
  column-gap: 16rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid var(--xo-border-color);
}

.activity-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.activity-icon {
  width: var(--xo-home-activity-icon-size);
  height: var(--xo-home-activity-icon-size);
  flex-shrink: 0;
}

.activity-icon.is-transfer {
  background: var(--xo-transfer-soft);
}

.activity-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  row-gap: 8rpx;
}

.activity-title {
  font-size: $font-md;
  color: var(--xo-text-primary);
  font-weight: 700;
}

.activity-meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-amount {
  font-size: $amount-sm;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.activity-amount.income {
  color: var(--xo-positive);
}

.activity-amount.expense {
  color: var(--xo-negative);
}

.activity-amount.transfer {
  color: var(--xo-transfer);
}

.empty-state {
  padding: 42rpx 0 12rpx;
  text-align: center;
  color: var(--xo-text-secondary);
  font-size: $font-sm;
}
</style>
