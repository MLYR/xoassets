<template>
  <view class="txns-page">
    <AppNavBar title="流水" />

    <view class="search-panel">
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜索备注关键词"
        placeholder-class="search-placeholder"
        confirm-type="search"
        @confirm="fetchData"
      />
      <view class="search-action" @click="fetchData">搜索</view>
    </view>

    <!-- 类型筛选 -->
    <view class="filter-bar">
      <view class="filter-tab" :class="{ active: filter === '' }" @click="setFilter('')">全部</view>
      <view class="filter-tab" :class="{ active: filter === 'EXPENSE' }" @click="setFilter('EXPENSE')">支出</view>
      <view class="filter-tab" :class="{ active: filter === 'INCOME' }" @click="setFilter('INCOME')">收入</view>
      <view class="filter-tab" :class="{ active: filter === 'TRANSFER' }" @click="setFilter('TRANSFER')">转账</view>
    </view>

    <!-- 流水列表 -->
    <view v-if="records.length" class="txn-list">
      <view v-for="t in records" :key="t.id" class="txn-card" @click="goDetail(t.id)">
        <view class="txn-row">
          <view class="txn-main">
            <text class="txn-title">{{ t.categoryName || (t.type === 'TRANSFER' ? '转账' : '未分类') }}</text>
            <text v-if="t.note" class="txn-note">{{ t.note }}</text>
          </view>
          <text class="txn-amount" :class="amountClass(t.type)">{{ fmtSigned(t) }}</text>
        </view>
        <view class="txn-meta">
          <view class="tag" :class="'tag-' + t.type.toLowerCase()">{{ typeLabel(t.type) }}</view>
          <text class="txn-account">{{ t.accountName }}</text>
          <text v-if="t.targetAccountName" class="txn-target">→ {{ t.targetAccountName }}</text>
          <text class="txn-time">{{ fmtTime(t.transactionTime) }}</text>
        </view>
      </view>

      <!-- 加载更多 -->
      <view v-if="hasMore" class="load-more" @click="loadMore">
        <text>{{ loadingMore ? '加载中…' : '加载更多' }}</text>
      </view>
    </view>

    <view v-else-if="!store.loading" class="empty-state">
      <text>暂无流水记录</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppNavBar from '@/components/app/AppNavBar.vue'
import { useTransactionStore } from '@/stores/transaction'
import type { TransactionType, TransactionItem } from '@/services/transactionApi'

const store = useTransactionStore()
const filter = ref<TransactionType | ''>('')
const keyword = ref('')
const loadingMore = ref(false)

const records = computed(() => store.page.records)
const hasMore = computed(() => store.page.records.length < store.page.total)

onLoad((options) => {
  keyword.value = typeof options?.keyword === 'string' ? decodeURIComponent(options.keyword) : ''
})

onMounted(() => {
  fetchData()
})

async function fetchData() {
  await store.fetchPage({
    pageNo: 1,
    pageSize: 20,
    type: filter.value || undefined,
    keyword: keyword.value.trim() || undefined
  })
}

function setFilter(f: TransactionType | '') {
  filter.value = f
  fetchData()
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    await store.fetchPage({
      pageNo: store.page.pageNo + 1,
      pageSize: 20,
      type: filter.value || undefined,
      keyword: keyword.value.trim() || undefined
    })
  } finally {
    loadingMore.value = false
  }
}

function goDetail(id: string) {
  uni.navigateTo({ url: `/pages/transaction-detail/transaction-detail?id=${id}` })
}

function fmtSigned(t: TransactionItem) {
  const v = t.amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
  if (t.type === 'INCOME') return '+' + v
  if (t.type === 'EXPENSE') return '-' + v
  return v
}

function fmtTime(s: string) {
  if (!s) return ''
  return s.slice(5, 16).replace('T', ' ')
}

function amountClass(type: string) {
  if (type === 'INCOME') return 'income'
  if (type === 'EXPENSE') return 'expense'
  return ''
}

function typeLabel(type: string) {
  const map: Record<string, string> = { INCOME: '收入', EXPENSE: '支出', TRANSFER: '转账', REFUND: '退款' }
  return map[type] || type
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.txns-page {
  min-height: 100vh;
  padding-bottom: 60rpx;
  background: var(--xo-page-bg);
}

.search-panel {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin: 0 $spacing-sm $spacing-sm;
  padding: 0 22rpx;
  min-height: 78rpx;
  border: 1rpx solid var(--xo-border-color);
  border-radius: var(--xo-radius-round);
  background: var(--xo-component-card-bg);
  box-shadow: var(--xo-component-card-shadow);
}

.search-input {
  flex: 1;
  min-width: 0;
  color: var(--xo-text-primary);
  font-size: $font-sm;
}

.search-placeholder {
  color: var(--xo-text-placeholder);
}

.search-action {
  flex-shrink: 0;
  color: var(--xo-primary);
  font-size: $font-sm;
  font-weight: 700;
}

.filter-bar {
  display: flex;
  background: var(--xo-component-card-bg);
  padding: $spacing-sm $spacing-md;
  margin-bottom: $spacing-sm;
  position: sticky;
  top: calc(var(--xo-nav-height) + env(safe-area-inset-top, 0px));
  z-index: 10;
}
.filter-tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: $border-radius-sm;
  font-size: $font-sm;
  color: $text-secondary;
  &.active {
    background: rgba($primary-color, 0.1);
    color: $primary-color;
    font-weight: 600;
  }
}

.txn-list {
  padding: 0 $spacing-sm;
}
.txn-card {
  background: var(--xo-component-card-bg);
  border-radius: $border-radius;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
  box-shadow: $card-shadow;
}
.txn-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16rpx;
}
.txn-main {
  flex: 1;
  overflow: hidden;
}
.txn-title {
  font-size: $font-lg;
  font-weight: 600;
  color: $text-primary;
  display: block;
}
.txn-note {
  font-size: $font-xs;
  color: $text-secondary;
  margin-top: 4rpx;
  display: block;
}
.txn-amount {
  font-size: $amount-md;
  font-weight: 700;
  flex-shrink: 0;
  margin-left: $spacing-sm;
  white-space: nowrap;
  &.income { color: var(--xo-profit-positive); }
  &.expense { color: var(--xo-profit-negative); }
}
.txn-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  font-size: $font-xs;
  color: $text-secondary;
}
.txn-account { color: $text-regular; }
.txn-target { color: $primary-color; }
.txn-time { margin-left: auto; }
.load-more {
  text-align: center;
  padding: 32rpx;
  color: $primary-color;
  font-size: $font-sm;
}
</style>
