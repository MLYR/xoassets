<template>
  <view class="accounts-page safe-bottom">
    <!-- 总资产 -->
    <view class="total-section">
      <text class="total-label">账户总余额</text>
      <text class="total-value">¥{{ fmtAmount(totalBalance) }}</text>
    </view>

    <!-- 账户列表 -->
    <view v-if="accounts.length" class="account-list">
      <view v-for="a in accounts" :key="a.id" class="account-card" @click="goDetail(a)">
        <view class="ac-left">
          <text class="ac-name">{{ a.name }}</text>
          <text class="ac-type">{{ typeLabel(a.type) }}</text>
        </view>
        <text class="ac-balance">¥{{ fmtAmount(a.balance) }}</text>
      </view>
    </view>

    <view v-else-if="!loading" class="empty-state">
      <text>暂无账户</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAccountStore } from '@/stores/account'

const store = useAccountStore()
const accounts = computed(() => store.accounts)
const loading = computed(() => store.loading)

const totalBalance = computed(() =>
  accounts.value.reduce((sum, a) => sum + (a.balance || 0), 0)
)

onShow(() => {
  store.fetchAccounts()
})

function fmtAmount(v: number) {
  if (v == null) return '--'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function typeLabel(t: string) {
  const map: Record<string, string> = {
    CASH: '现金', BANK: '银行卡', CREDIT_CARD: '信用卡',
    LOAN: '贷款', INVEST: '投资账户', ALIPAY: '支付宝', WECHAT: '微信'
  }
  return map[t] || t
}

function goDetail(a: any) {
  uni.navigateTo({ url: `/pages/account-detail/account-detail?id=${a.id}&name=${encodeURIComponent(a.name)}` })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.accounts-page { min-height: 100vh; background: var(--xo-page-bg); }
.total-section {
  background: var(--xo-gradient-page-header);
  padding: 48rpx $spacing-lg;
  text-align: center;
  margin-bottom: $spacing-md;
}
.total-label { font-size: $font-sm; color: var(--xo-white-80); display: block; margin-bottom: 8rpx; }
.total-value { font-size: $amount-huge; font-weight: 800; color: var(--xo-white); }
.account-list { padding: 0 $spacing-sm; }
.account-card {
  background: var(--xo-component-card-bg);
  border-radius: var(--xo-component-card-radius);
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
  box-shadow: var(--xo-component-card-shadow);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ac-left { display: flex; flex-direction: column; }
.ac-name { font-size: $font-lg; font-weight: 600; color: var(--xo-text-primary); }
.ac-type { font-size: $font-xs; color: var(--xo-text-secondary); margin-top: 4rpx; }
.ac-balance { font-size: $amount-md; font-weight: 700; color: var(--xo-text-primary); }
</style>
