<template>
  <view class="add-page safe-bottom">
    <!-- 账务类型切换 -->
    <view class="type-tabs">
      <view class="type-tab" :class="{ active: tab === 'EXPENSE' }" @click="switchTab('EXPENSE')">支出</view>
      <view class="type-tab" :class="{ active: tab === 'INCOME' }" @click="switchTab('INCOME')">收入</view>
      <view class="type-tab" :class="{ active: tab === 'TRANSFER' }" @click="switchTab('TRANSFER')">转账</view>
    </view>

    <!-- 金额输入区 -->
    <view class="amount-section">
      <text class="currency-symbol">¥</text>
      <input
        class="amount-input"
        v-model="amountStr"
        type="digit"
        placeholder="0.00"
        placeholder-style="color:#E0E0E0"
        :focus="true"
      />
    </view>

    <!-- 表单字段 -->
    <view class="form-section">
      <!-- 账户选择 -->
      <view class="form-row" @click="showAccountPicker = true">
        <text class="form-label">账户</text>
        <text class="form-value" :class="{ placeholder: !selectedAccount }">
          {{ selectedAccount ? selectedAccount.name : '选择账户' }}
        </text>
        <text class="form-arrow">›</text>
      </view>

      <!-- 目标账户（转账时显示） -->
      <view v-if="tab === 'TRANSFER'" class="form-row" @click="showTargetPicker = true">
        <text class="form-label">目标账户</text>
        <text class="form-value" :class="{ placeholder: !targetAccount }">
          {{ targetAccount ? targetAccount.name : '选择目标账户' }}
        </text>
        <text class="form-arrow">›</text>
      </view>

      <!-- 分类选择（非转账时显示） -->
      <view v-if="tab !== 'TRANSFER'" class="form-row" @click="showCategoryPicker = true">
        <text class="form-label">分类</text>
        <text class="form-value" :class="{ placeholder: !selectedCategory }">
          {{ selectedCategory ? selectedCategory.name : '选择分类' }}
        </text>
        <text class="form-arrow">›</text>
      </view>

      <!-- 时间 -->
      <view class="form-row">
        <text class="form-label">时间</text>
        <picker mode="date" :value="dateStr" @change="onDateChange">
          <text class="form-value">{{ dateStr }}</text>
        </picker>
        <picker mode="time" :value="timeStr" @change="onTimeChange">
          <text class="form-value time-value">{{ timeStr }}</text>
        </picker>
      </view>

      <!-- 备注 -->
      <view class="form-row form-row-note">
        <text class="form-label">备注</text>
        <input
          class="note-input"
          v-model="note"
          placeholder="写点什么（选填）"
          placeholder-style="color:#C0C4CC"
        />
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-area">
      <view class="btn-primary" :class="{ disabled: !canSubmit }" @click="handleSubmit">
        <text>{{ tab === 'EXPENSE' ? '记一笔支出' : tab === 'INCOME' ? '记一笔收入' : '确认转账' }}</text>
      </view>
    </view>

    <!-- 账户选择器 -->
    <view v-if="showAccountPicker" class="picker-overlay" @click="showAccountPicker = false">
      <view class="picker-sheet" @click.stop>
        <view class="picker-title">选择账户</view>
        <scroll-view scroll-y class="picker-list">
          <view
            v-for="a in accountStore.accounts"
            :key="a.id"
            class="picker-item"
            @click="selectAccount(a)"
          >
            <text>{{ a.name }}</text>
            <text class="picker-balance">¥{{ fmtAmount(a.balance) }}</text>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 目标账户选择器 -->
    <view v-if="showTargetPicker" class="picker-overlay" @click="showTargetPicker = false">
      <view class="picker-sheet" @click.stop>
        <view class="picker-title">选择目标账户</view>
        <scroll-view scroll-y class="picker-list">
          <view
            v-for="a in accountStore.accounts.filter(x => x.id !== selectedAccount?.id)"
            :key="a.id"
            class="picker-item"
            @click="selectTargetAccount(a)"
          >
            <text>{{ a.name }}</text>
            <text class="picker-balance">¥{{ fmtAmount(a.balance) }}</text>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 分类选择器 -->
    <view v-if="showCategoryPicker" class="picker-overlay" @click="showCategoryPicker = false">
      <view class="picker-sheet" @click.stop>
        <view class="picker-title">选择分类</view>
        <scroll-view scroll-y class="picker-list">
          <view
            v-for="c in categories"
            :key="c.id"
            class="picker-item"
            @click="selectCategory(c)"
          >
            <view class="cat-icon" :style="{ background: c.color || '#4A90D9' }">
              <text>{{ c.icon || c.name[0] }}</text>
            </view>
            <text>{{ c.name }}</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAccountStore } from '@/stores/account'
import { useTransactionStore } from '@/stores/transaction'
import { categoryApi, type CategoryItem } from '@/services/categoryApi'
import type { TransactionType } from '@/services/transactionApi'

const accountStore = useAccountStore()
const txnStore = useTransactionStore()

// tab 状态
const tab = ref<TransactionType>('EXPENSE')

// 表单数据
const amountStr = ref('')
const selectedAccount = ref<any>(null)
const targetAccount = ref<any>(null)
const selectedCategory = ref<CategoryItem | null>(null)
const categories = ref<CategoryItem[]>([])
const note = ref('')

// 日期时间
const now = new Date()
const dateStr = ref(formatDate(now))
const timeStr = ref(formatTime(now))

// picker 显示状态
const showAccountPicker = ref(false)
const showTargetPicker = ref(false)
const showCategoryPicker = ref(false)

const canSubmit = computed(() => {
  const amount = parseFloat(amountStr.value)
  if (isNaN(amount) || amount <= 0) return false
  if (!selectedAccount.value) return false
  if (tab.value === 'TRANSFER' && !targetAccount.value) return false
  if (tab.value !== 'TRANSFER' && !selectedCategory.value) return false
  return true
})

onMounted(() => {
  accountStore.fetchAccounts()
  loadCategories()
})

function switchTab(t: TransactionType) {
  tab.value = t
  selectedCategory.value = null
  targetAccount.value = null
  loadCategories()
}

async function loadCategories() {
  if (tab.value === 'TRANSFER') return
  try {
    categories.value = await categoryApi.list(
      tab.value === 'EXPENSE' ? 'EXPENSE' : 'INCOME'
    )
  } catch { /* 忽略 */ }
}

function selectAccount(a: any) {
  selectedAccount.value = a
  showAccountPicker.value = false
  if (tab.value === 'TRANSFER' && targetAccount.value?.id === a.id) {
    targetAccount.value = null
  }
}

function selectTargetAccount(a: any) {
  targetAccount.value = a
  showTargetPicker.value = false
}

function selectCategory(c: CategoryItem) {
  selectedCategory.value = c
  showCategoryPicker.value = false
}

function onDateChange(e: any) {
  dateStr.value = e.detail.value
}

function onTimeChange(e: any) {
  timeStr.value = e.detail.value
}

async function handleSubmit() {
  if (!canSubmit.value) return
  const amount = parseFloat(amountStr.value)
  try {
    await txnStore.create({
      type: tab.value,
      amount,
      accountId: selectedAccount.value.id,
      targetAccountId: tab.value === 'TRANSFER' ? targetAccount.value?.id : null,
      categoryId: tab.value !== 'TRANSFER' ? selectedCategory.value?.id : null,
      transactionTime: `${dateStr.value} ${timeStr.value}`,
      note: note.value || undefined
    })
    // 记账成功，清空表单
    amountStr.value = ''
    note.value = ''
    selectedCategory.value = null
    uni.showToast({ title: tab.value === 'EXPENSE' ? '支出已记录' : tab.value === 'INCOME' ? '收入已记录' : '转账已记录', icon: 'success' })
    // 刷新账户余额
    accountStore.fetchAccounts()
  } catch (e: any) {
    uni.showToast({ title: e.message || '记账失败', icon: 'none' })
  }
}

function fmtAmount(v: number) {
  if (v == null) return '--'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function formatDate(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

function formatTime(d: Date) {
  return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.add-page {
  min-height: 100vh;
  background: $bg-color;
}

/* 类型切换 */
.type-tabs {
  display: flex;
  background: #fff;
  padding: $spacing-sm;
  margin-bottom: $spacing-sm;
}
.type-tab {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  border-radius: $border-radius-sm;
  font-size: $font-md;
  color: $text-secondary;
  background: $bg-color;
  margin: 0 8rpx;
  transition: all 0.2s;
  &.active {
    background: $primary-color;
    color: #fff;
    font-weight: 600;
  }
}

/* 金额 */
.amount-section {
  background: #fff;
  padding: 48rpx $spacing-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $spacing-sm;
}
.currency-symbol {
  font-size: 56rpx;
  font-weight: 700;
  color: $text-primary;
  margin-right: 16rpx;
}
.amount-input {
  font-size: 72rpx;
  font-weight: 800;
  color: $text-primary;
  min-width: 200rpx;
  text-align: left;
}

/* 表单 */
.form-section {
  background: #fff;
  border-radius: $border-radius;
  margin: 0 $spacing-sm $spacing-lg;
}
.form-row {
  display: flex;
  align-items: center;
  padding: 28rpx $spacing-md;
  border-bottom: 1rpx solid $border-color;
  &:last-child { border-bottom: none; }
}
.form-row-note {
  border-bottom: none;
}
.form-label {
  width: 140rpx;
  font-size: $font-md;
  color: $text-primary;
  font-weight: 500;
  flex-shrink: 0;
}
.form-value {
  flex: 1;
  font-size: $font-md;
  color: $text-primary;
  &.placeholder { color: $text-placeholder; }
}
.form-arrow {
  font-size: 36rpx;
  color: $text-placeholder;
}
.time-value {
  margin-left: 16rpx;
}
.note-input {
  flex: 1;
  font-size: $font-md;
  color: $text-primary;
}

/* 提交 */
.submit-area {
  padding: 0 $spacing-lg;
  margin-top: 40rpx;
  .disabled { background: #C0C4CC; }
}

/* Picker */
.picker-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}
.picker-sheet {
  width: 100%;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  max-height: 60vh;
}
.picker-title {
  text-align: center;
  font-size: $font-lg;
  font-weight: 600;
  padding: 28rpx;
  border-bottom: 1rpx solid $border-color;
}
.picker-list { max-height: 50vh; }
.picker-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx $spacing-md;
  border-bottom: 1rpx solid $border-color;
  font-size: $font-md;
}
.picker-balance { color: $text-secondary; font-size: $font-sm; }
.cat-icon {
  width: 48rpx; height: 48rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  color: #fff;
  font-size: $font-xs;
}
</style>
