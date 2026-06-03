<template>
  <view class="invest-page safe-bottom">
    <!-- 持仓汇总 -->
    <view class="summary-section">
      <text class="summary-label">投资总市值</text>
      <text class="summary-value">¥{{ fmtAmount(summary?.totalMarketValue) }}</text>
      <view class="profit-row">
        <view class="profit-item">
          <text class="pi-label">今日收益</text>
          <text class="pi-val" :class="profitClass(summary?.todayProfit)">{{ fmtSigned(summary?.todayProfit) }}</text>
        </view>
        <view class="profit-item">
          <text class="pi-label">浮动盈亏</text>
          <text class="pi-val" :class="profitClass(summary?.floatingProfit)">{{ fmtSigned(summary?.floatingProfit) }}</text>
        </view>
        <view class="profit-item">
          <text class="pi-label">收益率</text>
          <text class="pi-val" :class="profitClass(summary?.floatingProfitRate)">
            {{ fmtPercent(summary?.floatingProfitRate) }}
          </text>
        </view>
      </view>
    </view>

    <!-- 持仓列表 -->
    <view class="section-title-row">
      <text class="st-title">持仓明细</text>
      <text class="st-count">{{ summary?.holdingCount || 0 }} 个</text>
    </view>

    <view v-if="holdings.length" class="holding-list">
      <view v-for="h in holdings" :key="h.id" class="holding-card" @click="goDetail(h)">
        <view class="hc-top">
          <view class="hc-name-row">
            <text class="hc-name">{{ h.assetName || h.symbol || '未知' }}</text>
            <text class="hc-type">{{ h.assetType }}</text>
          </view>
          <text class="hc-mv">¥{{ fmtAmount(h.marketValue) }}</text>
        </view>
        <view class="hc-bottom">
          <text class="hc-qty">{{ h.quantity }} 份 · 成本 ¥{{ fmtAmount(h.avgCost) }}</text>
          <text class="hc-profit" :class="profitClass(h.floatingProfit)">
            {{ fmtSigned(h.floatingProfit) }}
          </text>
        </view>
      </view>
    </view>

    <view v-else-if="!loading" class="empty-state">
      <text>暂无持仓</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useInvestmentStore } from '@/stores/investment'

const store = useInvestmentStore()
const holdings = computed(() => store.holdings)
const summary = computed(() => store.summary)
const loading = computed(() => store.loading)

onShow(() => {
  store.fetchHoldings()
})

function fmtAmount(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function fmtSigned(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  const prefix = v >= 0 ? '+' : ''
  return prefix + v.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function fmtPercent(v: number | null | undefined) {
  if (v == null || v === undefined) return '--'
  return (v >= 0 ? '+' : '') + v.toFixed(2) + '%'
}

function profitClass(v: number | null | undefined) {
  if (v == null || v === undefined) return ''
  return v >= 0 ? 'income' : 'expense'
}

function goDetail(h: any) {
  uni.navigateTo({ url: `/pages/holding-detail/holding-detail?id=${h.id}&name=${encodeURIComponent(h.assetName || h.symbol || '')}` })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.invest-page { min-height: 100vh; background: var(--xo-page-bg); }
.summary-section {
  background: var(--xo-bg-investment-summary-card);
  padding: 40rpx $spacing-lg 48rpx;
  margin-bottom: $spacing-md;
  text-align: center;
}
.summary-label { font-size: $font-sm; color: var(--xo-white-80); display: block; }
.summary-value { font-size: $amount-huge; font-weight: 800; color: var(--xo-white); display: block; margin: 8rpx 0 32rpx; }
.profit-row { display: flex; }
.profit-item { flex: 1; text-align: center; }
.pi-label { font-size: $font-xs; color: var(--xo-white-75); display: block; margin-bottom: 4rpx; }
.pi-val { font-size: $font-md; font-weight: 600; color: var(--xo-white);
  &.income { color: var(--xo-positive); }
  &.expense { color: var(--xo-negative); }
}
.section-title-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: $spacing-sm $spacing-md;
}
.st-title { font-size: $font-lg; font-weight: 700; color: var(--xo-text-primary); }
.st-count { font-size: $font-sm; color: var(--xo-text-secondary); }
.holding-list { padding: 0 $spacing-sm; }
.holding-card {
  background: var(--xo-component-card-bg); border-radius: var(--xo-component-card-radius); padding: $spacing-md;
  margin-bottom: $spacing-sm; box-shadow: var(--xo-component-card-shadow);
}
.hc-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12rpx; }
.hc-name-row { display: flex; align-items: center; gap: 12rpx; }
.hc-name { font-size: $font-lg; font-weight: 600; color: var(--xo-text-primary); }
.hc-type { font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 8rpx; background: var(--xo-primary-soft); color: var(--xo-primary); }
.hc-mv { font-size: $amount-md; font-weight: 700; color: var(--xo-text-primary); }
.hc-bottom { display: flex; justify-content: space-between; align-items: center; }
.hc-qty { font-size: $font-xs; color: var(--xo-text-secondary); }
.hc-profit { font-size: $font-sm; font-weight: 600;
  &.income { color: var(--xo-positive); }
  &.expense { color: var(--xo-negative); }
}
</style>
