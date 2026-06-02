<template>
  <view class="detail-page safe-bottom">
    <!-- 账户概览 -->
    <view class="header-section">
      <text class="acc-name">{{ accName }}</text>
      <text class="acc-balance">¥{{ fmtAmount(summary?.currentBalance) }}</text>
      <view class="flow-stats">
        <view class="flow-item">
          <text class="flow-label">流入</text>
          <text class="flow-val income">+{{ fmtAmount(summary?.totalInflow) }}</text>
        </view>
        <view class="flow-item">
          <text class="flow-label">流出</text>
          <text class="flow-val expense">-{{ fmtAmount(summary?.totalOutflow) }}</text>
        </view>
        <view class="flow-item">
          <text class="flow-label">净流入</text>
          <text class="flow-val" :class="netClass">{{ fmtSigned(summary?.netInflow) }}</text>
        </view>
      </view>
    </view>

    <!-- 资金明细 -->
    <view class="ledger-list">
      <view v-for="item in records" :key="item.id" class="ledger-item">
        <view class="ledger-left">
          <text class="ledger-title">{{ item.title }}</text>
          <view class="ledger-meta">
            <view class="tag tag-sm" :class="'tag-' + bizTagClass(item.bizType)">{{ bizLabel(item.bizType) }}</view>
            <text class="ledger-time">{{ fmtTime(item.transactionTime) }}</text>
          </view>
        </view>
        <text class="ledger-amount" :class="amountBizClass(item.bizType)">
          {{ fmtBizAmount(item.bizType, item.amount) }}
        </text>
      </view>

      <view v-if="hasMore" class="load-more" @click="loadMore">
        <text>{{ loadingMore ? '加载中…' : '加载更多' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAccountStore } from '@/stores/account'
import type { AccountLedgerItem, AccountLedgerSummary, AccountLedgerBizType } from '@/services/accountApi'

const store = useAccountStore()

const accId = ref('')
const accName = ref('')
const records = ref<AccountLedgerItem[]>([])
const summary = ref<AccountLedgerSummary | null>(null)
const pageNo = ref(1)
const totalCount = ref(0)
const loadingMore = ref(false)
const hasMore = computed(() => records.value.length < totalCount.value)

onMounted(() => {
  const pages = getCurrentPages()
  const opts = (pages[pages.length - 1] as any).options || {}
  accId.value = opts.id || ''
  accName.value = decodeURIComponent(opts.name || '')
  fetchData()
})

async function fetchData() {
  try {
    const res = await store.fetchLedger(accId.value, { pageNo: 1, pageSize: 20 })
    records.value = res.page.records
    totalCount.value = res.page.total
    summary.value = res.summary
  } catch {}
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const res = await store.fetchLedger(accId.value, { pageNo: pageNo.value + 1, pageSize: 20 })
    records.value.push(...res.page.records)
    pageNo.value++
  } finally {
    loadingMore.value = false
  }
}

function fmtAmount(v: number | undefined | null) {
  if (v == null || v === undefined) return '--'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function fmtSigned(v: number | undefined | null) {
  if (v == null || v === undefined) return '--'
  const prefix = v >= 0 ? '+' : ''
  return prefix + v.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function fmtTime(s: string) {
  if (!s) return ''
  return s.slice(5, 16).replace('T', ' ')
}

const netClass = computed(() => {
  const v = summary.value?.netInflow
  if (v == null) return ''
  return v >= 0 ? 'income' : 'expense'
})

function bizLabel(b: AccountLedgerBizType) {
  const m: Record<string, string> = {
    INCOME: '收入', EXPENSE: '支出', TRANSFER_IN: '转入', TRANSFER_OUT: '转出',
    REFUND: '退款', INVEST_BUY: '买入', INVEST_SELL: '卖出'
  }
  return m[b] || b
}

function bizTagClass(b: AccountLedgerBizType) {
  if (['INCOME', 'TRANSFER_IN', 'INVEST_SELL', 'REFUND'].includes(b)) return 'income'
  return 'expense'
}

function amountBizClass(b: AccountLedgerBizType) {
  if (['INCOME', 'TRANSFER_IN', 'INVEST_SELL', 'REFUND'].includes(b)) return 'income'
  return 'expense'
}

function fmtBizAmount(b: AccountLedgerBizType, v: number) {
  const s = v.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
  if (['INCOME', 'TRANSFER_IN', 'INVEST_SELL', 'REFUND'].includes(b)) return '+' + s
  return '-' + s
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.detail-page { min-height: 100vh; background: $bg-color; }
.header-section {
  background: linear-gradient(135deg, #4A90D9, #6BA5E7);
  padding: 40rpx $spacing-lg;
  margin-bottom: $spacing-sm;
}
.acc-name { font-size: $font-lg; color: rgba(255,255,255,0.85); display: block; }
.acc-balance { font-size: $amount-huge; font-weight: 800; color: #fff; display: block; margin: 12rpx 0 24rpx; }
.flow-stats { display: flex; }
.flow-item { flex: 1; text-align: center; }
.flow-label { font-size: $font-xs; color: rgba(255,255,255,0.75); display: block; margin-bottom: 4rpx; }
.flow-val { font-size: $font-md; font-weight: 600; color: #fff;
  &.income { color: #A8E6A0; }
  &.expense { color: #FFB3B3; }
}
.ledger-list { padding: 0 $spacing-sm; }
.ledger-item {
  background: #fff; border-radius: $border-radius; padding: $spacing-md;
  margin-bottom: $spacing-sm; box-shadow: $card-shadow;
  display: flex; justify-content: space-between; align-items: flex-start;
}
.ledger-left { flex: 1; overflow: hidden; }
.ledger-title { font-size: $font-md; font-weight: 600; color: $text-primary; display: block; margin-bottom: 8rpx; }
.ledger-meta { display: flex; align-items: center; gap: 12rpx; }
.tag-sm { padding: 2rpx 12rpx; font-size: 20rpx; border-radius: 6rpx; }
.ledger-time { font-size: $font-xs; color: $text-secondary; }
.ledger-amount { font-size: $amount-sm; font-weight: 700;
  &.income { color: $income-color; }
  &.expense { color: $expense-color; }
}
.load-more { text-align: center; padding: 32rpx; color: $primary-color; font-size: $font-sm; }
</style>
