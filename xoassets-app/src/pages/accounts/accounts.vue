<!-- 账户页：按移动端原型展示账户总览、分类筛选、账户列表和账户小结。 -->
<template>
  <view class="accounts-page safe-bottom">
    <view class="accounts-nav">
      <view class="nav-spacer"></view>
      <text class="nav-title">账户</text>
      <view class="nav-actions">
        <view class="nav-icon" @click="handleSearch">
          <AppIcon name="accounts.search" size="52rpx" :color="theme.colors.textPrimary" />
        </view>
        <view class="nav-icon" @click="handleAdd">
          <AppIcon name="accounts.add" size="52rpx" :color="theme.colors.textPrimary" />
        </view>
      </view>
    </view>

    <view class="summary-card">
      <view class="summary-head">
        <view class="summary-left">
          <view class="label-row">
            <text class="summary-label">账户总资产</text>
            <view class="eye-button" @click="assetVisible = !assetVisible">
              <AppIcon name="accounts.eye" size="34rpx" :color="theme.colors.textRegular" />
            </view>
          </view>
          <text class="summary-amount">{{ displayMoney(overview?.totalAsset, true) }}</text>
          <view class="compare-pill" :class="compareTone">
            <text>较上月</text>
            <text v-if="overview?.compareAvailable">{{ fmtSignedRate(overview.lastMonthChangeRate) }}</text>
            <text v-else>暂无对比</text>
          </view>
        </view>

        <view class="summary-right">
          <text class="account-count">共 {{ overview?.accountCount || 0 }} 个账户</text>
          <view class="wallet-illustration">
            <view class="wallet-panel"></view>
            <view class="wallet-card">
              <AppIcon name="accounts.wallet" size="76rpx" :color="theme.colors.white" />
            </view>
            <view class="wallet-coin">¥</view>
          </view>
        </view>
      </view>

      <view class="summary-split"></view>

      <view class="asset-breakdown">
        <view v-for="item in orderedCategories" :key="item.group" class="breakdown-item">
          <text class="breakdown-label">{{ item.label }}</text>
          <text class="breakdown-amount">{{ displayMoney(item.amount, true) }}</text>
        </view>
      </view>
    </view>

    <view class="category-row">
      <view
        v-for="tab in categoryTabs"
        :key="tab.key"
        class="category-tab"
        :class="{ active: selectedGroup === tab.key }"
        @click="selectedGroup = tab.key"
      >
        <text>{{ tab.label }}</text>
      </view>
      <view class="sort-trigger" @click="openSortSheet">
        <text>{{ sortLabel }}</text>
        <text class="sort-caret">▾</text>
      </view>
    </view>

    <view v-if="loading" class="account-list">
      <view v-for="n in 4" :key="n" class="account-card skeleton-card">
        <view class="skeleton-icon"></view>
        <view class="skeleton-main">
          <view class="skeleton-line wide"></view>
          <view class="skeleton-line short"></view>
        </view>
      </view>
    </view>

    <view v-else-if="filteredAccounts.length" class="account-list">
      <view v-for="account in filteredAccounts" :key="account.id" class="account-card" @click="goDetail(account)">
        <view class="account-icon-wrap">
          <AppIcon :name="accountIconName(account)" size="62rpx" :color="theme.colors.primary" />
        </view>
        <view class="account-info">
          <text class="account-title">{{ account.name }}</text>
          <view class="account-meta">
            <text class="account-sub">{{ account.displayType }}</text>
          </view>
        </view>
        <view class="account-balance-box">
          <text class="account-balance" :class="{ negative: account.balance < 0 }">{{ displayBalance(account.balance) }}</text>
          <text v-if="account.availableCredit != null && assetVisible" class="available-credit">可用额度 ¥ {{ fmtAmount(account.availableCredit) }}</text>
        </view>
      </view>
    </view>

    <view v-else class="empty-state">
      <AppIcon name="accounts.summary" size="82rpx" :color="theme.colors.primary" />
      <text>暂无账户</text>
    </view>

    <view class="summary-section">
      <view class="section-head">
        <text class="section-title">账户小结</text>
        <view class="detail-link" @click="selectedGroup = 'ALL'">
          <text>查看详情</text>
          <AppIcon name="accounts.arrowRight" size="28rpx" :color="theme.colors.textSecondary" />
        </view>
      </view>

      <view class="mini-stats">
        <view class="mini-stat">
          <text class="mini-label">账户总数</text>
          <text class="mini-value">{{ overview?.accountCount || 0 }} 个</text>
        </view>
        <view class="mini-stat">
          <text class="mini-label">有余额账户</text>
          <text class="mini-value">{{ overview?.nonZeroAccountCount || 0 }} 个</text>
        </view>
        <view class="mini-stat wide">
          <text class="mini-label">非信用账户资产总额</text>
          <text class="mini-value">{{ displayMoney(overview?.nonCreditAssetTotal, true) }}</text>
        </view>
      </view>

      <view class="distribution-block">
        <text class="distribution-title">账户分布</text>
        <view class="distribution-bar">
          <view
            v-for="item in visibleDistribution"
            :key="item.group"
            class="distribution-segment"
            :style="distributionSegmentStyle(item)"
          ></view>
        </view>
        <view class="distribution-legend">
          <view v-for="item in orderedCategories" :key="item.group" class="legend-item">
            <view class="legend-dot" :style="{ background: distributionColor(item.colorKey) }"></view>
            <text>{{ item.label }} {{ assetVisible ? `¥ ${fmtAmount(item.amount)}` : '¥ ******' }}（{{ fmtRatio(item.ratio) }}）</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/app/AppIcon.vue'
import { useAccountStore } from '@/stores/account'
import type { AccountDisplayItem, AccountGroup, AccountCategorySummary } from '@/services/accountApi'
import { useTheme } from '@/theme/useTheme'

type AccountTabKey = 'ALL' | AccountGroup
type AccountSortKey = 'DEFAULT' | 'NAME' | 'BALANCE'

const store = useAccountStore()
const { currentTheme } = useTheme()
const selectedGroup = ref<AccountTabKey>('ALL')
const sortKey = ref<AccountSortKey>('DEFAULT')
const assetVisible = ref(true)

const theme = computed(() => currentTheme.value)
const overview = computed(() => store.overview)
const loading = computed(() => store.loading)
const categories = computed(() => overview.value?.categories || [])
const accounts = computed(() => overview.value?.accounts || [])
const categoryOrder: AccountGroup[] = ['BANK_CARD', 'THIRD_PARTY', 'CASH']
const categoryLabels: Record<AccountGroup, string> = {
  BANK_CARD: '银行卡',
  THIRD_PARTY: '电子钱包',
  CASH: '现金'
}

const orderedCategories = computed(() => {
  const map = new Map(categories.value.map((item) => [item.group, item]))
  return categoryOrder.map((group) => {
    const item = map.get(group)
    return {
      group,
      label: categoryLabels[group],
      amount: item?.amount || 0,
      ratio: item?.ratio || 0,
      count: item?.count || 0,
      colorKey: item?.colorKey || (group === 'BANK_CARD' ? 'bankCard' : group === 'THIRD_PARTY' ? 'thirdParty' : 'cash')
    }
  })
})

const categoryTabs = computed<Array<{ key: AccountTabKey; label: string }>>(() => [
  { key: 'ALL', label: '全部' },
  ...orderedCategories.value.map((item) => ({ key: item.group, label: item.label }))
])

const filteredAccounts = computed(() => {
  const list = selectedGroup.value === 'ALL'
    ? [...accounts.value]
    : accounts.value.filter((account) => account.group === selectedGroup.value)
  if (sortKey.value === 'NAME') {
    return list.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
  }
  if (sortKey.value === 'BALANCE') {
    return list.sort((a, b) => Number(b.balance || 0) - Number(a.balance || 0))
  }
  return list
})

const sortLabel = computed(() => {
  const map: Record<AccountSortKey, string> = {
    DEFAULT: '默认排序',
    NAME: '按名称',
    BALANCE: '按金额'
  }
  return map[sortKey.value]
})

const visibleDistribution = computed(() => orderedCategories.value.filter((item) => Number(item.ratio) > 0))

const compareTone = computed(() => {
  const value = Number(overview.value?.lastMonthChangeRate || 0)
  if (!overview.value?.compareAvailable) return ''
  return value >= 0 ? 'positive' : 'negative'
})

onShow(() => {
  loadOverview()
})

async function loadOverview() {
  try {
    await store.fetchOverview()
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '账户总览加载失败', icon: 'none' })
  }
}

function fmtAmount(value: number | undefined | null) {
  if (value == null) return '0.00'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtBalance(value: number) {
  const abs = Math.abs(Number(value || 0))
  return `${value < 0 ? '-¥' : '¥'} ${fmtAmount(abs)}`
}

function displayMoney(value: number | undefined | null, withSymbol = false) {
  if (!assetVisible.value) return withSymbol ? '¥ ******' : '******'
  return withSymbol ? `¥ ${fmtAmount(value)}` : fmtAmount(value)
}

function displayBalance(value: number) {
  return assetVisible.value ? fmtBalance(value) : '¥ ******'
}

function fmtSignedRate(value: number) {
  const number = Number(value || 0)
  return `${number >= 0 ? '+' : ''}${number.toFixed(2)}%`
}

function fmtRatio(value: number) {
  return `${Number(value || 0).toFixed(1)}%`
}

function accountIconName(account: AccountDisplayItem) {
  const name = account.name || ''
  if (name.includes('微信')) return 'accounts.wechat'
  if (name.includes('支付宝')) return 'accounts.alipay'
  if (account.displayType === '信用卡') return 'accounts.creditCard'
  if (account.group === 'CASH') return 'accounts.cash'
  if (account.group === 'THIRD_PARTY') return 'accounts.wallet'
  return 'accounts.bankCard'
}

function distributionColor(key: string) {
  const palette = theme.value.charts.accountDistribution
  if (key === 'cash') return palette.cash
  if (key === 'thirdParty') return palette.thirdParty
  return palette.bankCard
}

function distributionSegmentStyle(item: AccountCategorySummary) {
  return {
    width: `${Math.max(0, Number(item.ratio || 0))}%`,
    background: distributionColor(item.colorKey)
  }
}

function openSortSheet() {
  uni.showActionSheet({
    itemList: ['默认排序', '按名称', '按金额'],
    success: (res) => {
      const keys: AccountSortKey[] = ['DEFAULT', 'NAME', 'BALANCE']
      sortKey.value = keys[res.tapIndex] || 'DEFAULT'
    }
  })
}

function goDetail(account: AccountDisplayItem) {
  uni.navigateTo({ url: `/pages/account-detail/account-detail?id=${account.id}&name=${encodeURIComponent(account.name)}` })
}

function handleSearch() {
  uni.showToast({ title: '账户搜索稍后开放', icon: 'none' })
}

function handleAdd() {
  // 移动端账户表单尚未建立，本入口先保留为后续新增账户页面的稳定入口。
  uni.showToast({ title: '新增账户稍后开放', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.accounts-page {
  min-height: 100vh;
  padding: 0 28rpx 80rpx;
  background: var(--xo-bg-accounts-page);
  box-sizing: border-box;
}

.accounts-nav {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 132rpx;
  padding-top: 30rpx;
}

.nav-spacer,
.nav-actions {
  width: 180rpx;
}

.nav-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  color: var(--xo-text-primary);
  font-size: 40rpx;
  font-weight: 700;
}

.nav-actions {
  display: flex;
  justify-content: flex-end;
  gap: 26rpx;
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58rpx;
  height: 58rpx;
}

.summary-card {
  overflow: hidden;
  min-height: var(--xo-accounts-summary-card-min-height);
  border: 1rpx solid rgba(223, 232, 245, 0.9);
  border-radius: 26rpx;
  background: var(--xo-bg-accounts-summary-card);
  box-shadow: var(--xo-shadow-card-hover);
}

.summary-head {
  position: relative;
  display: flex;
  justify-content: space-between;
  min-height: 250rpx;
  padding: 30rpx 32rpx 24rpx;
  box-sizing: border-box;
}

.summary-left {
  position: relative;
  z-index: 2;
  flex: 1;
}

.label-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.summary-label {
  color: var(--xo-text-regular);
  font-size: 28rpx;
}

.eye-button {
  display: flex;
  align-items: center;
}

.summary-amount {
  display: block;
  margin-top: 22rpx;
  color: var(--xo-text-primary);
  font-size: 58rpx;
  font-weight: 800;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.compare-pill {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 26rpx;
  padding: 8rpx 16rpx;
  border: 1rpx solid var(--xo-border-color);
  border-radius: var(--xo-radius-round);
  background: rgba(255, 255, 255, 0.88);
  color: var(--xo-text-secondary);
  font-size: 26rpx;
}

.compare-pill.positive {
  color: var(--xo-negative);
}

.compare-pill.negative {
  color: var(--xo-positive);
}

.summary-right {
  position: relative;
  width: 260rpx;
}

.account-count {
  position: absolute;
  right: 0;
  top: 0;
  z-index: 3;
  padding: 8rpx 22rpx;
  border: 1rpx solid rgba(47, 123, 255, 0.34);
  border-radius: var(--xo-radius-round);
  background: rgba(255, 255, 255, 0.72);
  color: var(--xo-primary);
  font-size: 26rpx;
}

.wallet-illustration {
  position: absolute;
  right: -16rpx;
  bottom: 2rpx;
  width: var(--xo-accounts-hero-illustration-size);
  height: var(--xo-accounts-hero-illustration-size);
}

.wallet-panel {
  position: absolute;
  right: 20rpx;
  top: 12rpx;
  width: 140rpx;
  height: 112rpx;
  border: 4rpx solid rgba(47, 123, 255, 0.28);
  border-radius: 24rpx;
  transform: rotate(-22deg);
}

.wallet-card {
  position: absolute;
  right: 16rpx;
  bottom: 38rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 140rpx;
  height: 110rpx;
  border-radius: 24rpx;
  background: var(--xo-gradient-asset-card);
  box-shadow: 0 18rpx 38rpx rgba(47, 123, 255, 0.24);
}

.wallet-coin {
  position: absolute;
  left: 18rpx;
  bottom: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: var(--xo-primary);
  color: var(--xo-white);
  font-size: 28rpx;
  font-weight: 800;
  box-shadow: var(--xo-shadow-button);
}

.summary-split {
  height: 1rpx;
  background: var(--xo-border-color);
}

.asset-breakdown {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding: 22rpx 0;
}

.breakdown-item {
  min-width: 0;
  padding: 0 30rpx;
  border-right: 1rpx solid var(--xo-border-color);
}

.breakdown-item:last-child {
  border-right: 0;
}

.breakdown-label,
.breakdown-amount {
  display: block;
}

.breakdown-label {
  color: var(--xo-text-regular);
  font-size: 28rpx;
}

.breakdown-amount {
  margin-top: 12rpx;
  color: var(--xo-text-primary);
  font-size: 28rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.category-row {
  display: flex;
  align-items: center;
  min-height: var(--xo-accounts-category-tab-height);
  margin: 22rpx 0 10rpx;
  gap: 26rpx;
}

.category-tab {
  position: relative;
  flex-shrink: 0;
  color: var(--xo-text-regular);
  font-size: 29rpx;
  font-weight: 500;
}

.category-tab.active {
  color: var(--xo-primary);
  font-weight: 700;
}

.category-tab.active::after {
  position: absolute;
  left: 8rpx;
  right: 8rpx;
  bottom: -14rpx;
  height: 6rpx;
  border-radius: var(--xo-radius-round);
  background: var(--xo-primary);
  content: '';
}

.sort-trigger {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-left: auto;
  color: var(--xo-text-regular);
  font-size: 26rpx;
}

.sort-caret {
  color: var(--xo-text-secondary);
  font-size: 24rpx;
}

.account-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.account-card {
  display: flex;
  align-items: center;
  min-height: var(--xo-accounts-row-min-height);
  padding: 20rpx 22rpx;
  border-radius: 22rpx;
  background: var(--xo-component-card-bg);
  box-shadow: var(--xo-component-card-shadow);
  box-sizing: border-box;
}

.account-icon-wrap {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 76rpx;
  height: 76rpx;
}

.account-info {
  min-width: 0;
  flex: 1;
  margin-left: 22rpx;
}

.account-title {
  display: block;
  overflow: hidden;
  color: var(--xo-text-primary);
  font-size: 30rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-meta {
  display: flex;
  align-items: center;
  min-height: 36rpx;
  margin-top: 10rpx;
}

.account-sub {
  color: var(--xo-text-secondary);
  font-size: 24rpx;
}

.account-balance-box {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  align-items: flex-end;
  min-width: 176rpx;
}

.account-balance {
  color: var(--xo-text-primary);
  font-size: 30rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.account-balance.negative {
  color: var(--xo-text-primary);
}

.available-credit {
  margin-top: 8rpx;
  color: var(--xo-text-secondary);
  font-size: 22rpx;
}

.summary-section {
  margin-top: 18rpx;
  padding: 26rpx 30rpx 28rpx;
  border-radius: 24rpx;
  background: var(--xo-component-card-bg);
  box-shadow: var(--xo-component-card-shadow);
}

.section-head,
.detail-link {
  display: flex;
  align-items: center;
}

.section-head {
  justify-content: space-between;
}

.section-title {
  color: var(--xo-text-primary);
  font-size: 32rpx;
  font-weight: 800;
}

.detail-link {
  gap: 6rpx;
  color: var(--xo-text-regular);
  font-size: 24rpx;
}

.mini-stats {
  display: grid;
  grid-template-columns: 0.8fr 0.9fr 1.45fr;
  margin-top: 26rpx;
}

.mini-stat {
  min-width: 0;
  padding-right: 22rpx;
  border-right: 1rpx solid var(--xo-border-color);
}

.mini-stat:last-child {
  border-right: 0;
  padding-right: 0;
  padding-left: 22rpx;
}

.mini-label,
.mini-value {
  display: block;
}

.mini-label {
  color: var(--xo-text-regular);
  font-size: 24rpx;
}

.mini-value {
  margin-top: 12rpx;
  color: var(--xo-text-primary);
  font-size: 32rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.distribution-block {
  margin-top: 26rpx;
}

.distribution-title {
  color: var(--xo-text-regular);
  font-size: 24rpx;
}

.distribution-bar {
  display: flex;
  overflow: hidden;
  height: var(--xo-accounts-distribution-bar-height);
  margin-top: 14rpx;
  border-radius: var(--xo-radius-round);
  background: var(--xo-border-color);
}

.distribution-segment {
  min-width: 0;
  height: 100%;
}

.distribution-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx 24rpx;
  margin-top: 18rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  max-width: 100%;
  gap: 8rpx;
  color: var(--xo-text-regular);
  font-size: 22rpx;
}

.legend-dot {
  flex-shrink: 0;
  width: 14rpx;
  height: 14rpx;
  border-radius: 4rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18rpx;
  padding: 80rpx 0;
  color: var(--xo-text-secondary);
  font-size: 28rpx;
}

.skeleton-card {
  gap: 20rpx;
}

.skeleton-icon,
.skeleton-line {
  background: linear-gradient(90deg, var(--xo-border-color), var(--xo-card-bg), var(--xo-border-color));
}

.skeleton-icon {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
}

.skeleton-main {
  flex: 1;
}

.skeleton-line {
  height: 24rpx;
  border-radius: var(--xo-radius-round);
}

.skeleton-line.wide {
  width: 70%;
}

.skeleton-line.short {
  width: 42%;
  margin-top: 18rpx;
}
</style>
