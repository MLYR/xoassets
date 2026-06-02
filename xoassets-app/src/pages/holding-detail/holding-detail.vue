<template>
  <view class="detail-page safe-bottom">
    <!-- 持仓概览 -->
    <view class="header-section">
      <text class="h-name">{{ hName }}</text>
      <text class="h-mv">¥{{ fmtAmount(detail?.holding.marketValue) }}</text>
      <view class="h-stats">
        <view class="h-stat">
          <text class="hs-label">成本</text>
          <text class="hs-val">¥{{ fmtAmount(detail?.holding.totalCost) }}</text>
        </view>
        <view class="h-stat">
          <text class="hs-label">浮动盈亏</text>
          <text class="hs-val" :class="profitClass(detail?.holding.floatingProfit)">{{ fmtSigned(detail?.holding.floatingProfit) }}</text>
        </view>
        <view class="h-stat">
          <text class="hs-label">收益率</text>
          <text class="hs-val" :class="profitClass(detail?.holding.floatingProfitRate)">
            {{ fmtPercent(detail?.holding.floatingProfitRate) }}
          </text>
        </view>
      </view>
    </view>

    <!-- 交易记录 -->
    <view class="section-title">交易记录</view>
    <view v-if="transactions.length" class="tx-list">
      <view v-for="tx in transactions" :key="tx.id" class="tx-item">
        <view class="tx-left">
          <view class="tag tag-sm" :class="tx.type === 'BUY' ? 'tag-income' : 'tag-expense'">
            {{ tx.type === 'BUY' ? '买入' : '卖出' }}
          </view>
          <text class="tx-qty">{{ tx.quantity }} 份 @ ¥{{ fmtAmount(tx.price) }}</text>
        </view>
        <view class="tx-right">
          <text class="tx-amount">¥{{ fmtAmount(tx.amount) }}</text>
          <text class="tx-time">{{ fmtTime(tx.transactionTime) }}</text>
        </view>
      </view>
    </view>
    <view v-else class="empty-state">
      <text>暂无交易记录</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useInvestmentStore } from '@/stores/investment'
import type { HoldingDetail, InvestmentTransactionItem } from '@/services/investmentApi'

const store = useInvestmentStore()
const detail = ref<HoldingDetail | null>(null)
const hName = ref('')

const transactions = computed(() => detail.value?.transactions || [])

onMounted(async () => {
  const pages = getCurrentPages()
  const opts = (pages[pages.length - 1] as any).options || {}
  hName.value = decodeURIComponent(opts.name || '')
  const id = opts.id
  if (id) {
    try {
      detail.value = await store.fetchDetail(id)
    } catch {}
  }
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
function fmtTime(s: string) {
  if (!s) return ''
  return s.slice(5, 16).replace('T', ' ')
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.detail-page { min-height: 100vh; background: $bg-color; }
.header-section {
  background: linear-gradient(135deg, #4A90D9, #6BA5E7);
  padding: 40rpx $spacing-lg 48rpx;
  margin-bottom: $spacing-sm;
  text-align: center;
}
.h-name { font-size: $font-lg; color: rgba(255,255,255,0.85); }
.h-mv { font-size: $amount-huge; font-weight: 800; color: #fff; display: block; margin: 12rpx 0 32rpx; }
.h-stats { display: flex; }
.h-stat { flex: 1; }
.hs-label { font-size: $font-xs; color: rgba(255,255,255,0.75); display: block; margin-bottom: 4rpx; }
.hs-val { font-size: $font-md; font-weight: 600; color: #fff;
  &.income { color: #A8E6A0; }
  &.expense { color: #FFB3B3; }
}
.section-title {
  font-size: $font-lg; font-weight: 700; color: $text-primary;
  padding: $spacing-sm $spacing-md;
}
.tx-list { padding: 0 $spacing-sm; }
.tx-item {
  background: #fff; border-radius: $border-radius; padding: $spacing-md;
  margin-bottom: $spacing-sm; box-shadow: $card-shadow;
  display: flex; justify-content: space-between; align-items: center;
}
.tx-left { display: flex; align-items: center; gap: 12rpx; }
.tag-sm { padding: 2rpx 12rpx; font-size: 20rpx; border-radius: 6rpx; }
.tx-qty { font-size: $font-xs; color: $text-secondary; }
.tx-right { text-align: right; }
.tx-amount { font-size: $font-md; font-weight: 600; color: $text-primary; display: block; }
.tx-time { font-size: $font-xs; color: $text-secondary; }
</style>
