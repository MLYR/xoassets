<template>
  <view class="detail-page">
    <AppNavBar title="流水详情" detail />

    <!-- 金额区 -->
    <view v-if="txn" class="amount-section" :class="'bg-' + txn.type.toLowerCase()">
      <text class="detail-label">{{ typeLabel }}</text>
      <text class="detail-amount">{{ fmtSigned }}</text>
    </view>

    <!-- 信息区 -->
    <view v-if="txn" class="info-section card">
      <view class="info-row">
        <text class="info-label">类型</text>
        <text class="info-value">{{ typeLabel }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">账户</text>
        <text class="info-value">{{ txn.accountName }}</text>
      </view>
      <view v-if="txn.targetAccountName" class="info-row">
        <text class="info-label">目标账户</text>
        <text class="info-value">{{ txn.targetAccountName }}</text>
      </view>
      <view v-if="txn.categoryName" class="info-row">
        <text class="info-label">分类</text>
        <text class="info-value">{{ txn.categoryName }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">时间</text>
        <text class="info-value">{{ txn.transactionTime }}</text>
      </view>
      <view v-if="txn.note" class="info-row">
        <text class="info-label">备注</text>
        <text class="info-value">{{ txn.note }}</text>
      </view>
    </view>

    <!-- 删除 -->
    <view v-if="txn" class="action-area">
      <view class="btn-outline danger" @click="handleDelete">
        <text>删除此流水</text>
      </view>
    </view>

    <view v-else class="empty-state">
      <text>{{ loading ? '加载中…' : '未找到流水详情' }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppNavBar from '@/components/app/AppNavBar.vue'
import { transactionApi, type TransactionItem } from '@/services/transactionApi'

const txn = ref<TransactionItem | null>(null)
const loading = ref(false)

const typeLabel = computed(() => {
  if (!txn.value) return ''
  const map: Record<string, string> = { INCOME: '收入', EXPENSE: '支出', TRANSFER: '转账', REFUND: '退款' }
  return map[txn.value.type] || txn.value.type
})

const fmtSigned = computed(() => {
  if (!txn.value) return '--'
  const t = txn.value
  const v = t.amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
  if (t.type === 'INCOME') return '+' + v
  if (t.type === 'EXPENSE') return '-' + v
  return '¥' + v
})

onMounted(() => {
  const pages = getCurrentPages()
  const id = (pages[pages.length - 1] as any).options?.id
  if (id) fetchDetail(id)
})

async function fetchDetail(id: string) {
  loading.value = true
  try {
    txn.value = await transactionApi.detail(id)
  } catch (e: any) {
    uni.showToast({ title: e.message || '流水详情加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleDelete() {
  if (!txn.value) return
  const res = await new Promise<boolean>(resolve => {
    uni.showModal({
      title: '确认删除',
      content: '删除后不可恢复，确定要删除此流水吗？',
      success: r => resolve(r.confirm)
    })
  })
  if (!res) return
  try {
    await transactionApi.remove(txn.value.id)
    uni.showToast({ title: '已删除', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e: any) {
    uni.showToast({ title: e.message || '删除失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.detail-page { min-height: 100vh; background: var(--xo-page-bg); }
.amount-section {
  padding: 64rpx $spacing-lg;
  text-align: center;
  margin-bottom: $spacing-md;
  &.bg-expense { background: var(--xo-gradient-expense-card); }
  &.bg-income { background: var(--xo-gradient-income-card); }
  &.bg-transfer { background: var(--xo-gradient-transfer-card); }
  &.bg-refund { background: linear-gradient(135deg, var(--xo-warning), var(--xo-warning)); }
}
.detail-label { font-size: $font-md; color: var(--xo-white-85); display: block; margin-bottom: 12rpx; }
.detail-amount { font-size: 64rpx; font-weight: 800; color: var(--xo-white); white-space: nowrap; }
.info-section { margin: 0 $spacing-sm; }
.info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 0; border-bottom: 1rpx solid $border-color;
  &:last-child { border-bottom: none; }
}
.info-label { font-size: $font-md; color: $text-secondary; }
.info-value { font-size: $font-md; color: $text-primary; font-weight: 500; }
.action-area { padding: 64rpx $spacing-lg; }
.danger { border-color: $danger-color; color: $danger-color; }
</style>
