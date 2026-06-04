<template>
  <view class="detail-page safe-bottom" :style="pageStyle">
    <view class="nav-bar">
      <view class="nav-icon" @click="goBack">
        <AppIcon name="common.back" size="40rpx" />
      </view>
      <text class="nav-title">{{ account?.name || accName || '账户明细' }}</text>
      <view class="nav-icon" @click="openEditModal">
        <AppIcon name="common.edit" size="38rpx" />
      </view>
    </view>

    <view class="balance-card">
      <view class="balance-top">
        <view class="account-meta">
          <view class="account-icon">
            <AppIcon :name="accountIconName" size="56rpx" />
          </view>
          <view>
            <text class="account-name">{{ account?.name || accName || '--' }}</text>
            <text class="account-type">{{ accountTypeLabel(account?.type) }}</text>
          </view>
        </view>
        <view class="eye-button" @click="visibleAmount = !visibleAmount">
          <AppIcon :name="visibleAmount ? 'common.eye' : 'common.eyeOff'" size="34rpx" :color="currentTheme.colors.white" />
        </view>
      </view>

      <text class="balance-label">当前余额</text>
      <text class="balance-amount">{{ displayAmount(summary?.currentBalance) }}</text>

      <view class="balance-actions">
        <view class="balance-action" @click="openCorrectionModal">
          <AppIcon name="common.edit" size="28rpx" :color="currentTheme.colors.white" />
          <text>余额修正</text>
        </view>
        <view class="balance-action ghost" @click="openEditModal">
          <AppIcon name="common.settings" size="28rpx" :color="currentTheme.colors.white" />
          <text>修改账户</text>
        </view>
      </view>
    </view>

    <view class="month-card">
      <view class="month-switch">
        <view class="month-arrow" @click="changeMonth(-1)">
          <AppIcon name="common.back" size="28rpx" />
        </view>
        <view class="month-title-box">
          <text class="month-title">{{ monthTitle }}</text>
          <text class="month-subtitle">按月统计资金变化</text>
        </view>
        <view class="month-arrow next" @click="changeMonth(1)">
          <AppIcon name="common.back" size="28rpx" />
        </view>
      </view>

      <view class="flow-grid">
        <view class="flow-stat">
          <text class="flow-label">流入</text>
          <text class="flow-value positive">+{{ displayPlainAmount(monthInflow) }}</text>
        </view>
        <view class="flow-stat">
          <text class="flow-label">流出</text>
          <text class="flow-value negative">-{{ displayPlainAmount(monthOutflow) }}</text>
        </view>
        <view class="flow-stat">
          <text class="flow-label">净变化</text>
          <text class="flow-value" :class="monthNet >= 0 ? 'positive' : 'negative'">{{ displaySignedAmount(monthNet) }}</text>
        </view>
      </view>

      <view class="flow-bar">
        <view class="flow-bar-in" :style="{ width: inflowRatio + '%' }"></view>
        <view class="flow-bar-out" :style="{ width: outflowRatio + '%' }"></view>
      </view>

      <view class="trend-row">
        <view
          v-for="item in trendBars"
          :key="item.date"
          class="trend-item"
        >
          <view class="trend-track">
            <view
              class="trend-fill"
              :class="item.netFlow >= 0 ? 'positive-bg' : 'negative-bg'"
              :style="{ height: item.height + '%' }"
            ></view>
          </view>
          <text class="trend-day">{{ item.day }}</text>
        </view>
      </view>
    </view>

    <view class="ledger-section">
      <view class="section-header">
        <view>
          <text class="section-title">月度流水</text>
          <text class="section-subtitle">共 {{ totalCount }} 笔</text>
        </view>
        <view class="refresh-button" @click="reload">
          <AppIcon name="common.refresh" size="32rpx" />
        </view>
      </view>

      <view v-if="groupedRecords.length" class="ledger-groups">
        <view v-for="group in groupedRecords" :key="group.date" class="ledger-group">
          <view class="date-row">
            <text class="date-title">{{ group.dateLabel }}</text>
            <text class="date-summary">
              <text class="positive" v-if="group.inflow > 0">+{{ displayPlainAmount(group.inflow) }}</text>
              <text v-if="group.inflow > 0 && group.outflow > 0">  </text>
              <text class="negative" v-if="group.outflow > 0">-{{ displayPlainAmount(group.outflow) }}</text>
            </text>
          </view>

          <view
            v-for="item in group.records"
            :key="item.sourceType + '-' + item.id + '-' + item.bizType"
            class="ledger-item"
            @click="openLedgerDetail(item)"
          >
            <view class="ledger-icon">
              <AppIcon :name="ledgerIconName(item)" size="44rpx" />
            </view>
            <view class="ledger-main">
              <text class="ledger-title">{{ item.title }}</text>
              <text class="ledger-meta">{{ ledgerMeta(item) }}</text>
            </view>
            <view class="ledger-right">
              <text class="ledger-amount" :class="isInflow(item) ? 'positive' : 'negative'">
                {{ formatLedgerAmount(item) }}
              </text>
              <text class="ledger-time">{{ formatTime(item.transactionTime) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty-state">
        <AppIcon name="common.calendar" size="72rpx" />
        <text>本月暂无资金流水</text>
      </view>

      <view v-if="hasMore" class="load-more" @click="loadMore">
        <text>{{ loadingMore ? '加载中...' : '加载更多' }}</text>
      </view>
    </view>

    <view v-if="showEditModal" class="modal-mask" @click="closeModals">
      <view class="modal-panel" @click.stop>
        <view class="modal-header">
          <text class="modal-title">修改账户</text>
          <view class="modal-close" @click="closeModals">
            <AppIcon name="common.delete" size="30rpx" />
          </view>
        </view>
        <view class="form-row">
          <text class="form-label">账户名称</text>
          <input v-model="editForm.name" class="form-input" placeholder="请输入账户名称" />
        </view>
        <view class="form-row">
          <text class="form-label">账户类型</text>
          <picker :range="accountTypeOptions" range-key="label" @change="onTypeChange">
            <view class="form-picker">
              <text>{{ accountTypeLabel(editForm.type) }}</text>
              <AppIcon name="common.arrowRight" size="26rpx" />
            </view>
          </picker>
        </view>
        <view class="form-row">
          <text class="form-label">备注</text>
          <input v-model="editForm.remark" class="form-input" placeholder="选填" />
        </view>
        <view class="modal-actions">
          <button class="modal-btn secondary" @click="closeModals">取消</button>
          <button class="modal-btn primary" :loading="saving" @click="saveAccountEdit">保存</button>
        </view>
      </view>
    </view>

    <view v-if="showCorrectionModal" class="modal-mask" @click="closeModals">
      <view class="modal-panel" @click.stop>
        <view class="modal-header">
          <text class="modal-title">余额修正</text>
          <view class="modal-close" @click="closeModals">
            <AppIcon name="common.delete" size="30rpx" />
          </view>
        </view>
        <view class="correction-current">
          <text>当前余额</text>
          <text>{{ displayAmount(summary?.currentBalance) }}</text>
        </view>
        <view class="form-row amount-row">
          <text class="form-label">修正后余额</text>
          <input v-model="correctionBalance" class="form-input amount-input" type="digit" placeholder="0.00" />
        </view>
        <view class="form-row">
          <text class="form-label">修正备注</text>
          <input v-model="correctionRemark" class="form-input" placeholder="例如：补记利息、对账调整" />
        </view>
        <text class="modal-tip">余额修正会直接更新账户当前余额，不生成普通收支流水。</text>
        <view class="modal-actions">
          <button class="modal-btn secondary" @click="closeModals">取消</button>
          <button class="modal-btn primary" :loading="saving" @click="saveBalanceCorrection">确认修正</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { onReachBottom } from '@dcloudio/uni-app'
import AppIcon from '@/components/app/AppIcon.vue'
import { useAccountStore } from '@/stores/account'
import { useTheme } from '@/theme/useTheme'
import type {
  AccountDailyFlowItem,
  AccountFlowStatistics,
  AccountItem,
  AccountLedgerBizType,
  AccountLedgerItem,
  AccountLedgerSummary
} from '@/services/accountApi'

const store = useAccountStore()
const { currentTheme } = useTheme()

const accId = ref('')
const accName = ref('')
const account = ref<AccountItem | null>(null)
const records = ref<AccountLedgerItem[]>([])
const summary = ref<AccountLedgerSummary | null>(null)
const flowStatistics = ref<AccountFlowStatistics | null>(null)
const currentMonth = ref(new Date())
const pageNo = ref(1)
const totalCount = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const saving = ref(false)
const visibleAmount = ref(true)
const showEditModal = ref(false)
const showCorrectionModal = ref(false)
const correctionBalance = ref('')
const correctionRemark = ref('')

const editForm = reactive({
  name: '',
  type: 'BANK',
  remark: ''
})

const accountTypeOptions = [
  { label: '银行卡', value: 'BANK' },
  { label: '信用卡', value: 'CREDITCARD' },
  { label: '现金', value: 'CASH' },
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信', value: 'WECHAT' },
  { label: '其他', value: 'OTHER' }
]

const pageStyle = computed(() => ({
  background: currentTheme.value.backgrounds.accountsPage || currentTheme.value.colors.pageBg
}))

const monthKey = computed(() => {
  const year = currentMonth.value.getFullYear()
  const month = String(currentMonth.value.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
})

const monthTitle = computed(() => {
  return `${currentMonth.value.getFullYear()}年${currentMonth.value.getMonth() + 1}月`
})

const dateRange = computed(() => {
  const year = currentMonth.value.getFullYear()
  const month = currentMonth.value.getMonth()
  const start = new Date(year, month, 1)
  const end = new Date(year, month + 1, 0)
  return {
    startDate: formatDate(start),
    endDate: formatDate(end)
  }
})

const monthInflow = computed(() => {
  const stats = flowStatistics.value
  if (!stats) return summary.value?.totalInflow || 0
  return toNumber(stats.incomeAmount) + toNumber(stats.transferInAmount) + toNumber(stats.investmentSellAmount)
})

const monthOutflow = computed(() => {
  const stats = flowStatistics.value
  if (!stats) return summary.value?.totalOutflow || 0
  return toNumber(stats.expenseAmount) + toNumber(stats.transferOutAmount) + toNumber(stats.investmentBuyAmount)
})

const monthNet = computed(() => {
  if (flowStatistics.value) return toNumber(flowStatistics.value.netFlowAmount)
  return monthInflow.value - monthOutflow.value
})

const totalFlow = computed(() => monthInflow.value + monthOutflow.value)
const inflowRatio = computed(() => totalFlow.value <= 0 ? 0 : Math.round(monthInflow.value / totalFlow.value * 100))
const outflowRatio = computed(() => totalFlow.value <= 0 ? 0 : Math.round(monthOutflow.value / totalFlow.value * 100))

const trendBars = computed(() => {
  const trend = flowStatistics.value?.dailyFlowTrend || []
  const max = Math.max(...trend.map(item => Math.abs(toNumber(item.netFlow))), 1)
  return trend.slice(-14).map(item => ({
    ...item,
    day: item.date.slice(8, 10),
    height: Math.max(10, Math.round(Math.abs(toNumber(item.netFlow)) / max * 100))
  }))
})

const groupedRecords = computed(() => {
  const groups = new Map<string, { date: string; dateLabel: string; inflow: number; outflow: number; records: AccountLedgerItem[] }>()
  records.value.forEach(item => {
    const date = item.transactionTime.slice(0, 10)
    if (!groups.has(date)) {
      groups.set(date, {
        date,
        dateLabel: formatDateLabel(date),
        inflow: 0,
        outflow: 0,
        records: []
      })
    }
    const group = groups.get(date)!
    const amount = Math.abs(toNumber(item.amount))
    if (isInflow(item)) group.inflow += amount
    else group.outflow += amount
    group.records.push(item)
  })
  return Array.from(groups.values())
})

const hasMore = computed(() => records.value.length < totalCount.value)

const accountIconName = computed(() => {
  const type = (account.value?.type || '').toUpperCase()
  if (type.includes('CREDIT')) return 'accounts.creditCard'
  if (type.includes('CASH')) return 'accounts.cash'
  if (type.includes('ALIPAY')) return 'accounts.alipay'
  if (type.includes('WECHAT')) return 'accounts.wechat'
  if (type.includes('WALLET')) return 'accounts.wallet'
  return 'accounts.bankCard'
})

onMounted(() => {
  const pages = getCurrentPages()
  const opts = (pages[pages.length - 1] as any).options || {}
  accId.value = opts.id || ''
  accName.value = decodeURIComponent(opts.name || '')
  reload()
})

onReachBottom(() => {
  loadMore()
})

async function reload() {
  if (!accId.value || loading.value) return
  loading.value = true
  pageNo.value = 1
  try {
    const [ledger, stats] = await Promise.all([
      store.fetchLedger(accId.value, {
        pageNo: 1,
        pageSize: 30,
        startDate: dateRange.value.startDate,
        endDate: dateRange.value.endDate
      }),
      store.fetchFlowStatistics(accId.value, {
        month: monthKey.value,
        startDate: dateRange.value.startDate,
        endDate: dateRange.value.endDate
      })
    ])
    account.value = ledger.account
    records.value = ledger.page.records
    totalCount.value = ledger.page.total
    summary.value = ledger.summary
    flowStatistics.value = stats
    syncEditForm()
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '账户明细加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || loading.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const nextPage = pageNo.value + 1
    const res = await store.fetchLedger(accId.value, {
      pageNo: nextPage,
      pageSize: 30,
      startDate: dateRange.value.startDate,
      endDate: dateRange.value.endDate
    })
    records.value.push(...res.page.records)
    pageNo.value = nextPage
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加载失败', icon: 'none' })
  } finally {
    loadingMore.value = false
  }
}

function changeMonth(delta: number) {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + delta, 1)
  reload()
}

function goBack() {
  uni.navigateBack()
}

function openEditModal() {
  syncEditForm()
  showEditModal.value = true
}

function openCorrectionModal() {
  correctionBalance.value = account.value?.balance == null ? '' : String(account.value.balance)
  correctionRemark.value = ''
  showCorrectionModal.value = true
}

function closeModals() {
  if (saving.value) return
  showEditModal.value = false
  showCorrectionModal.value = false
}

function syncEditForm() {
  if (!account.value) return
  editForm.name = account.value.name
  editForm.type = account.value.type
  editForm.remark = account.value.remark || ''
}

function onTypeChange(event: any) {
  const index = Number(event.detail.value)
  editForm.type = accountTypeOptions[index]?.value || editForm.type
}

async function saveAccountEdit() {
  if (!account.value) return
  if (!editForm.name.trim()) {
    uni.showToast({ title: '账户名称不能为空', icon: 'none' })
    return
  }
  await saveAccount({
    name: editForm.name.trim(),
    type: editForm.type,
    balance: account.value.balance,
    remark: editForm.remark || null
  })
}

async function saveBalanceCorrection() {
  if (!account.value) return
  const balance = Number(correctionBalance.value)
  if (!Number.isFinite(balance)) {
    uni.showToast({ title: '请输入有效余额', icon: 'none' })
    return
  }
  await saveAccount({
    name: account.value.name,
    type: account.value.type,
    balance,
    remark: correctionRemark.value || account.value.remark || null
  })
}

async function saveAccount(payload: { name: string; type: string; balance: number; remark?: string | null }) {
  if (!account.value || saving.value) return
  saving.value = true
  try {
    await store.updateAccount(accId.value, {
      name: payload.name,
      type: payload.type,
      initialBalance: account.value.initialBalance,
      balance: payload.balance,
      currency: account.value.currency || 'CNY',
      status: account.value.status ?? 1,
      sortOrder: account.value.sortOrder ?? 0,
      remark: payload.remark ?? null
    })
    showEditModal.value = false
    showCorrectionModal.value = false
    uni.showToast({ title: '保存成功', icon: 'success' })
    await reload()
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

function openLedgerDetail(item: AccountLedgerItem) {
  if (item.sourceType !== 'TRANSACTION') return
  uni.navigateTo({ url: `/pages/transaction-detail/transaction-detail?id=${item.id}` })
}

function displayAmount(value: number | undefined | null) {
  if (!visibleAmount.value) return '¥ ****'
  if (value == null) return '¥ --'
  const num = toNumber(value)
  const sign = num < 0 ? '-' : ''
  return `${sign}¥ ${Math.abs(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function displayPlainAmount(value: number | undefined | null) {
  if (!visibleAmount.value) return '****'
  if (value == null) return '--'
  return Math.abs(toNumber(value)).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function displaySignedAmount(value: number | undefined | null) {
  if (!visibleAmount.value) return '****'
  if (value == null) return '--'
  const num = toNumber(value)
  const sign = num >= 0 ? '+' : '-'
  return `${sign}${Math.abs(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatLedgerAmount(item: AccountLedgerItem) {
  if (!visibleAmount.value) return '****'
  return displaySignedAmount(toNumber(item.amount))
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateLabel(date: string) {
  const d = new Date(`${date}T00:00:00`)
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return `${Number(date.slice(8, 10))}日 ${week}`
}

function formatTime(value: string) {
  if (!value) return ''
  return value.slice(11, 16)
}

function ledgerMeta(item: AccountLedgerItem) {
  const parts = [bizLabel(item.bizType)]
  if (item.relatedAccountName) parts.push(item.relatedAccountName)
  if (item.categoryName) parts.push(item.categoryName)
  if (item.assetName || item.symbol) parts.push(item.assetName || item.symbol || '')
  if (item.note) parts.push(item.note)
  return parts.filter(Boolean).join(' · ')
}

function ledgerIconName(item: AccountLedgerItem) {
  if (item.sourceType === 'INVESTMENT') return 'quickActions.invest'
  if (item.bizType === 'TRANSFER_IN' || item.bizType === 'TRANSFER_OUT') return 'quickActions.transfer'
  if (item.bizType === 'INCOME' || item.bizType === 'REFUND') return 'home.income'
  return categoryIconByName(item.categoryName) || 'home.expense'
}

function categoryIconByName(name?: string | null) {
  if (!name) return ''
  if (name.includes('餐') || name.includes('饮')) return 'category.dining'
  if (name.includes('交通') || name.includes('出行')) return 'category.transit'
  if (name.includes('购物')) return 'category.shopping'
  if (name.includes('娱乐')) return 'category.entertainment'
  if (name.includes('医疗')) return 'category.medical'
  if (name.includes('教育')) return 'category.education'
  if (name.includes('房') || name.includes('租')) return 'category.rent'
  return 'category.other'
}

function accountTypeLabel(type?: string | null) {
  const normalized = (type || '').toUpperCase()
  if (normalized.includes('CREDIT')) return '信用卡'
  if (normalized.includes('CASH')) return '现金'
  if (normalized.includes('ALIPAY')) return '支付宝'
  if (normalized.includes('WECHAT')) return '微信'
  if (normalized.includes('WALLET')) return '电子钱包'
  if (normalized.includes('BANK')) return '银行卡'
  return type || '账户'
}

function bizLabel(type: AccountLedgerBizType) {
  const labels: Record<AccountLedgerBizType, string> = {
    INCOME: '收入',
    EXPENSE: '支出',
    TRANSFER_OUT: '转出',
    TRANSFER_IN: '转入',
    REFUND: '退款',
    INVEST_BUY: '投资买入',
    INVEST_SELL: '投资卖出'
  }
  return labels[type] || type
}

function isInflow(item: AccountLedgerItem | AccountDailyFlowItem) {
  if ('amount' in item) return toNumber(item.amount) >= 0
  return toNumber(item.netFlow) >= 0
}

function toNumber(value: number | string | undefined | null) {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num : 0
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  padding: 24rpx 24rpx 40rpx;
  box-sizing: border-box;
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72rpx;
  margin-bottom: 18rpx;
}

.nav-icon {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-title {
  max-width: 480rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 34rpx;
  font-weight: 700;
  color: var(--xo-text-primary);
}

.balance-card {
  min-height: 300rpx;
  padding: 30rpx;
  border-radius: var(--xo-radius-xl);
  background: var(--xo-gradient-asset-card);
  box-shadow: var(--xo-shadow-floating);
  box-sizing: border-box;
  color: var(--xo-white);
}

.balance-top,
.account-meta,
.balance-actions,
.month-switch,
.section-header,
.date-row,
.ledger-item,
.modal-header,
.form-picker,
.correction-current {
  display: flex;
  align-items: center;
}

.balance-top,
.month-switch,
.section-header,
.date-row,
.ledger-item,
.modal-header,
.correction-current {
  justify-content: space-between;
}

.account-meta {
  min-width: 0;
  gap: 18rpx;
}

.account-icon {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--xo-radius-lg);
  background: var(--xo-white-25);
}

.account-name {
  display: block;
  max-width: 420rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--xo-white);
}

.account-type,
.balance-label {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: var(--xo-white-80);
}

.eye-button {
  width: 58rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--xo-radius-round);
  background: var(--xo-white-25);
}

.balance-label {
  margin-top: 34rpx;
}

.balance-amount {
  display: block;
  margin-top: 8rpx;
  font-size: var(--xo-amount-huge);
  line-height: 1.08;
  font-weight: 800;
  color: var(--xo-white);
}

.balance-actions {
  gap: 16rpx;
  margin-top: 28rpx;
}

.balance-action {
  height: 58rpx;
  padding: 0 22rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
  border-radius: var(--xo-radius-round);
  background: var(--xo-white-25);
  font-size: 24rpx;
  color: var(--xo-white);
}

.balance-action.ghost {
  background: rgba(255, 255, 255, 0.14);
}

.month-card,
.ledger-section,
.modal-panel {
  border-radius: var(--xo-radius-xl);
  background: var(--xo-card-bg);
  box-shadow: var(--xo-shadow-card);
}

.month-card {
  margin-top: 24rpx;
  padding: 26rpx;
}

.month-arrow {
  width: 58rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--xo-radius-round);
  background: var(--xo-primary-soft);
}

.month-arrow.next {
  transform: rotate(180deg);
}

.month-title-box {
  text-align: center;
}

.month-title {
  display: block;
  font-size: 32rpx;
  font-weight: 800;
  color: var(--xo-text-primary);
}

.month-subtitle {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--xo-text-secondary);
}

.flow-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
  margin-top: 28rpx;
}

.flow-stat {
  padding: 18rpx 12rpx;
  border-radius: var(--xo-radius-lg);
  background: var(--xo-bg-accounts-summary-card);
  text-align: center;
}

.flow-label {
  display: block;
  font-size: 22rpx;
  color: var(--xo-text-secondary);
}

.flow-value {
  display: block;
  margin-top: 8rpx;
  font-size: 28rpx;
  font-weight: 800;
  color: var(--xo-text-primary);
}

.positive {
  color: var(--xo-positive);
}

.negative {
  color: var(--xo-negative);
}

.flow-bar {
  height: 16rpx;
  display: flex;
  gap: 6rpx;
  margin-top: 24rpx;
  overflow: hidden;
  border-radius: var(--xo-radius-round);
  background: var(--xo-border-color);
}

.flow-bar-in,
.flow-bar-out {
  height: 100%;
  border-radius: var(--xo-radius-round);
}

.flow-bar-in {
  background: var(--xo-positive);
}

.flow-bar-out {
  background: var(--xo-negative);
}

.trend-row {
  height: 112rpx;
  display: flex;
  align-items: flex-end;
  gap: 12rpx;
  margin-top: 24rpx;
  overflow: hidden;
}

.trend-item {
  flex: 1;
  min-width: 0;
  text-align: center;
}

.trend-track {
  height: 78rpx;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.trend-fill {
  width: 12rpx;
  min-height: 10rpx;
  border-radius: var(--xo-radius-round);
}

.positive-bg {
  background: var(--xo-positive);
}

.negative-bg {
  background: var(--xo-negative);
}

.trend-day {
  display: block;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: var(--xo-text-placeholder);
}

.ledger-section {
  margin-top: 24rpx;
  padding: 24rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 800;
  color: var(--xo-text-primary);
}

.section-subtitle {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--xo-text-secondary);
}

.refresh-button {
  width: 58rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--xo-radius-round);
  background: var(--xo-primary-soft);
}

.ledger-groups {
  margin-top: 22rpx;
}

.ledger-group + .ledger-group {
  margin-top: 28rpx;
}

.date-row {
  height: 44rpx;
  margin-bottom: 8rpx;
}

.date-title {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--xo-text-regular);
}

.date-summary {
  font-size: 22rpx;
  font-weight: 700;
}

.ledger-item {
  min-height: 112rpx;
  gap: 16rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid var(--xo-border-color);
}

.ledger-group .ledger-item:last-child {
  border-bottom: 0;
}

.ledger-icon {
  width: 50rpx;
  height: 50rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.ledger-main {
  flex: 1;
  min-width: 0;
}

.ledger-title {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 28rpx;
  font-weight: 700;
  color: var(--xo-text-primary);
}

.ledger-meta {
  display: block;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 22rpx;
  color: var(--xo-text-secondary);
}

.ledger-right {
  min-width: 170rpx;
  text-align: right;
}

.ledger-amount {
  display: block;
  font-size: 30rpx;
  font-weight: 800;
}

.ledger-time {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--xo-text-placeholder);
}

.empty-state {
  height: 260rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  color: var(--xo-text-secondary);
  font-size: 26rpx;
}

.load-more {
  padding: 30rpx 0 8rpx;
  text-align: center;
  font-size: 26rpx;
  color: var(--xo-primary);
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 99;
  display: flex;
  align-items: flex-end;
  background: var(--xo-mask);
}

.modal-panel {
  width: 100%;
  padding: 30rpx 28rpx 46rpx;
  border-radius: 32rpx 32rpx 0 0;
  box-sizing: border-box;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--xo-text-primary);
}

.modal-close {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-row {
  margin-top: 24rpx;
}

.form-label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: var(--xo-text-secondary);
}

.form-input,
.form-picker {
  height: 88rpx;
  padding: 0 22rpx;
  border-radius: var(--xo-radius-lg);
  background: var(--xo-page-bg);
  box-sizing: border-box;
  font-size: 28rpx;
  color: var(--xo-text-primary);
}

.form-picker {
  justify-content: space-between;
}

.amount-input {
  font-size: 38rpx;
  font-weight: 800;
}

.correction-current {
  margin-top: 22rpx;
  padding: 22rpx;
  border-radius: var(--xo-radius-lg);
  background: var(--xo-primary-soft);
  font-size: 26rpx;
  color: var(--xo-text-regular);
}

.correction-current text:last-child {
  font-weight: 800;
  color: var(--xo-primary);
}

.modal-tip {
  display: block;
  margin-top: 18rpx;
  font-size: 22rpx;
  color: var(--xo-text-secondary);
}

.modal-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 30rpx;
}

.modal-btn {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--xo-radius-round);
  font-size: 28rpx;
  font-weight: 700;
  line-height: 88rpx;
}

.modal-btn::after {
  border: 0;
}

.modal-btn.primary {
  color: var(--xo-button-primary-text);
  background: var(--xo-button-primary-bg);
  box-shadow: var(--xo-shadow-button);
}

.modal-btn.secondary {
  color: var(--xo-primary);
  background: var(--xo-primary-soft);
}
</style>
