<template>
  <AppPage class="invest-page" safe-bottom gap="24rpx">
    <AppNavBar title="投资" />

    <!-- 顶部资产卡片：优先走真实汇总字段，缺口字段仅做受控兜底。 -->
    <AppCard
      class="summary-card"
      :padding="theme.spacing.lg"
      :radius="theme.radius.xl"
      :shadow="false"
      :background="theme.backgrounds.investmentSummaryCard"
    >
      <view class="summary-card-line"></view>
      <view class="summary-main">
        <view class="summary-left">
          <text class="summary-title">投资总资产</text>
          <AppAmount class="summary-total-amount" :value="summaryMetrics.totalAsset" prefix="¥ " size="lg" :color="theme.colors.white" />
          <InvestmentSummaryCompare :metrics="summaryMetrics" />
        </view>

        <view class="summary-divider"></view>

        <view class="summary-right">
          <text class="summary-side-label">累计收益率</text>
          <view class="summary-rate-line">
            <AppAmount class="summary-rate-amount" :value="summaryMetrics.accumulatedRate" signed size="md" semantic="profit" />
            <text class="summary-rate-unit">%</text>
          </view>
          <view class="summary-side-profit">
            <text class="summary-side-profit-label">累计收益</text>
            <AppAmount class="summary-profit-amount" :value="summaryMetrics.accumulatedProfit" prefix="¥ " signed size="sm" semantic="profit" />
          </view>
        </view>
      </view>
    </AppCard>

    <!-- 资产分布：使用持仓实时聚合，现金类在当前接口缺失时显示 0。 -->
    <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="distribution-card">
      <AppSectionHeader title="资产分布" action-text="更多" @action="handleDistributionMore" />

      <view class="distribution-layout" @touchstart="onDistributionTouchStart" @touchend="onDistributionTouchEnd">
        <view class="distribution-donut" :style="distributionRingStyle">
          <view class="distribution-donut-center">
            <text class="distribution-center-label">{{ selectedDistributionItem?.label || '总资产' }}</text>
            <AppAmount class="distribution-center-amount" :value="selectedDistributionItem?.amount ?? distributionTotalAmount" prefix="¥ " size="sm" tone="neutral" />
          </view>
        </view>

        <view class="distribution-list">
          <view v-for="item in distributionItems" :key="item.key" class="distribution-item" :class="{ active: item.key === selectedDistributionKey }">
            <view class="distribution-item-main">
              <view class="distribution-item-name">
                <text class="distribution-dot" :style="{ background: item.color }"></text>
                <text class="distribution-name-text">{{ item.label }}</text>
              </view>
              <text class="distribution-amount">¥ {{ fmtAmount(item.amount) }}</text>
              <text class="distribution-percent">{{ fmtPercentNumber(item.percent) }}</text>
            </view>
          </view>
        </view>
      </view>
    </AppCard>

    <!-- 持仓卡片：保留现有持仓详情入口，只提升结构和视觉。 -->
    <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="holdings-card">
      <AppSectionHeader title="持仓" action-text="全部持仓" @action="handleAllHoldings" />

      <view class="holding-table">
        <view class="holding-header">
          <text class="holding-col holding-col-name">名称/代码</text>
          <text class="holding-col holding-col-amount">总/昨/今日收益</text>
          <text class="holding-col holding-col-profit">持有收益 / 率</text>
        </view>

        <view v-if="holdingRows.length" class="holding-body">
          <InvestmentHoldingRow
            v-for="row in holdingRows"
            :key="row.id"
            :row="row"
            @click="goDetail(row)"
          />
        </view>

        <view v-else-if="!loading" class="empty-state">
          <text>暂无持仓</text>
        </view>
      </view>

      <view class="holding-analysis-entry" @click="handleAllHoldings">
        <text class="holding-analysis-text">持仓分析</text>
        <AppIcon name="common.arrowRight" size="24rpx" />
      </view>
    </AppCard>

    <!-- 底部交易入口：提交到投资交易接口，转换由后端在同一事务中拆成卖出和买入。 -->
    <view class="action-row">
      <AppActionButton
        class="trade-action-button"
        text="买入"
        type="primary"
        icon="investmentActions.buy"
        :radius="theme.radius.round"
        :height="investmentTokens.actionCapsuleHeight"
        block
        @click="handleTradeAction('buy')"
      />
      <AppActionButton
        class="trade-action-button"
        text="转换"
        type="purple"
        icon="investmentActions.convert"
        :radius="theme.radius.round"
        :height="investmentTokens.actionCapsuleHeight"
        block
        @click="handleTradeAction('convert')"
      />
      <AppActionButton
        class="trade-action-button"
        text="卖出"
        type="sell"
        icon="investmentActions.sell"
        :radius="theme.radius.round"
        :height="investmentTokens.actionCapsuleHeight"
        block
        @click="handleTradeAction('sell')"
      />
    </view>

    <view v-if="tradeVisible" class="trade-mask" @click="closeTradeSheet">
      <view class="trade-sheet" @click.stop>
        <view class="trade-sheet-head">
          <text class="trade-sheet-title">{{ tradeTitle }}</text>
          <text class="trade-sheet-close" @click="closeTradeSheet">关闭</text>
        </view>

        <view class="trade-selector" @click="selectHolding('source')">
          <text class="trade-label">{{ tradeMode === 'convert' ? '转出持仓' : '持仓' }}</text>
          <text class="trade-value">{{ selectedSourceHolding?.assetName || selectedSourceHolding?.symbol || '请选择' }}</text>
        </view>

        <view v-if="tradeMode === 'convert'" class="trade-selector" @click="selectHolding('target')">
          <text class="trade-label">转入持仓</text>
          <text class="trade-value">{{ selectedTargetHolding?.assetName || selectedTargetHolding?.symbol || '请选择' }}</text>
        </view>

        <view class="trade-selector" @click="selectAccount">
          <text class="trade-label">资金账户</text>
          <text class="trade-value">{{ selectedTradeAccount?.name || '请选择' }}</text>
        </view>

        <view class="trade-grid">
          <view class="trade-field">
            <text class="trade-label">{{ tradeMode === 'convert' ? '转出份额' : '份额' }}</text>
            <input v-model="tradeForm.quantity" class="trade-input" type="digit" placeholder="0" placeholder-class="trade-placeholder" />
          </view>
          <view class="trade-field">
            <text class="trade-label">{{ tradeMode === 'convert' ? '转出价格' : '价格' }}</text>
            <input v-model="tradeForm.price" class="trade-input" type="digit" placeholder="0.0000" placeholder-class="trade-placeholder" />
          </view>
          <view v-if="tradeMode === 'convert'" class="trade-field">
            <text class="trade-label">转入份额</text>
            <input v-model="tradeForm.targetQuantity" class="trade-input" type="digit" placeholder="0" placeholder-class="trade-placeholder" />
          </view>
          <view v-if="tradeMode === 'convert'" class="trade-field">
            <text class="trade-label">转入价格</text>
            <input v-model="tradeForm.targetPrice" class="trade-input" type="digit" placeholder="0.0000" placeholder-class="trade-placeholder" />
          </view>
          <view class="trade-field">
            <text class="trade-label">手续费</text>
            <input v-model="tradeForm.fee" class="trade-input" type="digit" placeholder="0.00" placeholder-class="trade-placeholder" />
          </view>
        </view>

        <view class="trade-note-row">
          <text class="trade-label">备注</text>
          <input v-model="tradeForm.note" class="trade-note-input" placeholder="可选" placeholder-class="trade-placeholder" />
        </view>

        <view class="trade-submit" :class="{ disabled: tradeSaving }" @click="submitTrade">
          {{ tradeSaving ? '提交中' : '确认提交' }}
        </view>
      </view>
    </view>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppActionButton from '@/components/app/AppActionButton.vue'
import AppAmount from '@/components/app/AppAmount.vue'
import AppCard from '@/components/app/AppCard.vue'
import AppIcon from '@/components/app/AppIcon.vue'
import AppNavBar from '@/components/app/AppNavBar.vue'
import AppPage from '@/components/app/AppPage.vue'
import AppSectionHeader from '@/components/app/AppSectionHeader.vue'
import InvestmentHoldingRow from './components/InvestmentHoldingRow.vue'
import InvestmentSummaryCompare from './components/InvestmentSummaryCompare.vue'
import { useInvestmentStore } from '@/stores/investment'
import { accountApi, type AccountItem } from '@/services/accountApi'
import { useTheme } from '@/theme/useTheme'
import {
  buildDistributionItems,
  buildHoldingRows,
  buildSummaryMetrics,
  fmtAmount,
  fmtPercentNumber,
  type HoldingRow
} from './helpers'

const store = useInvestmentStore()
const { currentTheme, investmentTokens } = useTheme()

const holdings = computed(() => store.holdings)
const summary = computed(() => store.summary)
const loading = computed(() => store.loading)
const theme = computed(() => currentTheme.value)
const selectedDistributionIndex = ref(0)
const distributionTouchStartX = ref(0)
const distributionTouchStartY = ref(0)
const tradeVisible = ref(false)
const tradeSaving = ref(false)
const tradeMode = ref<'buy' | 'convert' | 'sell'>('buy')
const tradeAccounts = ref<AccountItem[]>([])
const tradeForm = ref({
  holdingId: '',
  targetHoldingId: '',
  accountId: '',
  quantity: '',
  price: '',
  targetQuantity: '',
  targetPrice: '',
  fee: '0',
  note: ''
})

onShow(() => {
  store.fetchHoldings()
})

const summaryMetrics = computed(() => buildSummaryMetrics(summary.value))

const distributionItems = computed(() => {
  return buildDistributionItems(holdings.value, theme.value.charts.investmentDistribution, summary.value?.totalMarketValue)
})

const distributionTotalAmount = computed(() => summary.value?.totalMarketValue ?? 0)
const selectedDistributionItem = computed(() => distributionItems.value[selectedDistributionIndex.value] || distributionItems.value[0] || null)
const selectedDistributionKey = computed(() => selectedDistributionItem.value?.key || '')

const distributionRingStyle = computed(() => {
  const hasValue = distributionItems.value.some((item) => item.percent > 0)
  if (!hasValue) {
    return {
      background: `conic-gradient(${theme.value.colors.border} 0% 100%)`
    }
  }

  const segments = distributionItems.value
    .map((item, index, list) => {
      const start = list.slice(0, index).reduce((acc, cur) => acc + cur.percent, 0)
      const end = start + item.percent
      return `${item.color} ${start}% ${end}%`
    })
    .join(', ')

  return {
    background: `conic-gradient(${segments})`
  }
})

const holdingRows = computed(() => buildHoldingRows(holdings.value))
const selectedSourceHolding = computed(() => holdings.value.find((item) => item.id === tradeForm.value.holdingId) || null)
const selectedTargetHolding = computed(() => holdings.value.find((item) => item.id === tradeForm.value.targetHoldingId) || null)
const selectedTradeAccount = computed(() => tradeAccounts.value.find((item) => item.id === tradeForm.value.accountId) || null)
const tradeTitle = computed(() => {
  if (tradeMode.value === 'buy') return '买入'
  if (tradeMode.value === 'sell') return '卖出'
  return '转换'
})

function goDetail(row: HoldingRow) {
  const h = row.raw
  uni.navigateTo({ url: `/pages/holding-detail/holding-detail?id=${h.id}&name=${encodeURIComponent(h.assetName || h.symbol || '')}` })
}

function handleDistributionMore() {
  uni.navigateTo({ url: '/pages/investment-distribution/investment-distribution' })
}

function handleAllHoldings() {
  uni.navigateTo({ url: '/pages/holding-analysis/holding-analysis' })
}

function onDistributionTouchStart(event: TouchEvent) {
  const touch = event.touches[0]
  distributionTouchStartX.value = touch?.clientX || 0
  distributionTouchStartY.value = touch?.clientY || 0
}

function onDistributionTouchEnd(event: TouchEvent) {
  const touch = event.changedTouches[0]
  const deltaX = (touch?.clientX || 0) - distributionTouchStartX.value
  const deltaY = (touch?.clientY || 0) - distributionTouchStartY.value
  if (Math.abs(deltaX) < 40 || Math.abs(deltaX) < Math.abs(deltaY)) return
  const total = distributionItems.value.length
  if (!total) return
  const next = selectedDistributionIndex.value + (deltaX < 0 ? 1 : -1)
  selectedDistributionIndex.value = (next + total) % total
}

async function handleTradeAction(action: 'buy' | 'convert' | 'sell') {
  if (!holdings.value.length) {
    uni.showToast({ title: '请先新增持仓', icon: 'none' })
    return
  }
  if (action === 'convert' && holdings.value.length < 2) {
    uni.showToast({ title: '转换至少需要两个持仓', icon: 'none' })
    return
  }
  tradeMode.value = action
  try {
    await ensureTradeAccounts()
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '账户加载失败', icon: 'none' })
    return
  }
  if (!tradeAccounts.value.length) {
    uni.showToast({ title: '请先新增资金账户', icon: 'none' })
    return
  }
  resetTradeForm(action)
  tradeVisible.value = true
}

async function ensureTradeAccounts() {
  if (tradeAccounts.value.length) return
  tradeAccounts.value = await accountApi.list()
}

function resetTradeForm(action: 'buy' | 'convert' | 'sell') {
  const source = holdings.value[0]
  const target = action === 'convert' ? holdings.value.find((item) => item.id !== source?.id) : null
  tradeForm.value = {
    holdingId: source?.id || '',
    targetHoldingId: target?.id || '',
    accountId: tradeAccounts.value[0]?.id || '',
    quantity: action === 'sell' ? String(source?.quantity || '') : '',
    price: source?.latestPrice ? String(source.latestPrice) : '',
    targetQuantity: '',
    targetPrice: target?.latestPrice ? String(target.latestPrice) : '',
    fee: '0',
    note: ''
  }
}

function closeTradeSheet() {
  if (tradeSaving.value) return
  tradeVisible.value = false
}

function selectHolding(role: 'source' | 'target') {
  const options = holdings.value.filter((item) => role === 'source' || item.id !== tradeForm.value.holdingId)
  if (!options.length) return
  uni.showActionSheet({
    itemList: options.map((item) => item.assetName || item.symbol || '未命名持仓'),
    success: (res) => {
      const selected = options[res.tapIndex]
      if (!selected) return
      if (role === 'source') {
        tradeForm.value.holdingId = selected.id
        tradeForm.value.price = selected.latestPrice ? String(selected.latestPrice) : tradeForm.value.price
        if (tradeMode.value === 'sell') tradeForm.value.quantity = String(selected.quantity || '')
        if (tradeMode.value === 'convert' && tradeForm.value.targetHoldingId === selected.id) {
          const nextTarget = holdings.value.find((item) => item.id !== selected.id)
          tradeForm.value.targetHoldingId = nextTarget?.id || ''
          tradeForm.value.targetPrice = nextTarget?.latestPrice ? String(nextTarget.latestPrice) : ''
        }
      } else {
        tradeForm.value.targetHoldingId = selected.id
        tradeForm.value.targetPrice = selected.latestPrice ? String(selected.latestPrice) : tradeForm.value.targetPrice
      }
    }
  })
}

function selectAccount() {
  if (!tradeAccounts.value.length) return
  uni.showActionSheet({
    itemList: tradeAccounts.value.map((item) => item.name),
    success: (res) => {
      tradeForm.value.accountId = tradeAccounts.value[res.tapIndex]?.id || tradeForm.value.accountId
    }
  })
}

async function submitTrade() {
  if (tradeSaving.value) return
  const source = selectedSourceHolding.value
  const accountId = tradeForm.value.accountId
  const quantity = Number(tradeForm.value.quantity)
  const price = Number(tradeForm.value.price)
  const fee = Number(tradeForm.value.fee || 0)
  if (!source || !accountId || quantity <= 0 || price <= 0 || fee < 0) {
    uni.showToast({ title: '请完整填写交易信息', icon: 'none' })
    return
  }
  tradeSaving.value = true
  try {
    if (tradeMode.value === 'convert') {
      const target = selectedTargetHolding.value
      const targetQuantity = Number(tradeForm.value.targetQuantity)
      const targetPrice = Number(tradeForm.value.targetPrice)
      if (!target || targetQuantity <= 0 || targetPrice <= 0) {
        uni.showToast({ title: '请完整填写转换信息', icon: 'none' })
        return
      }
      await store.convertTransaction({
        sourceHoldingId: source.id,
        targetHoldingId: target.id,
        accountId,
        sourceQuantity: quantity,
        sourcePrice: price,
        targetQuantity,
        targetPrice,
        fee,
        transactionTime: formatLocalDateTime(new Date()),
        note: tradeForm.value.note.trim() || undefined
      })
    } else {
      await store.createTransaction({
        holdingId: source.id,
        assetId: source.assetId,
        accountId,
        type: tradeMode.value === 'buy' ? 'BUY' : 'SELL',
        quantity,
        price,
        fee,
        transactionTime: formatLocalDateTime(new Date()),
        note: tradeForm.value.note.trim() || undefined
      })
    }
    await store.fetchHoldings()
    tradeVisible.value = false
    uni.showToast({ title: '交易已提交', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '提交失败', icon: 'none' })
  } finally {
    tradeSaving.value = false
  }
}

function formatLocalDateTime(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.invest-page {
  min-height: 100vh;
}

.summary-card {
  position: relative;
  overflow: hidden;
}

.summary-card-line {
  position: absolute;
  right: -30rpx;
  bottom: -10rpx;
  width: 320rpx;
  height: 180rpx;
  opacity: 0.16;
  border-right: 4rpx solid rgba(255, 255, 255, 0.72);
  border-top: 4rpx solid rgba(255, 255, 255, 0.4);
  border-radius: 120rpx 0 0 0;
  transform: rotate(-18deg);
}

.summary-main {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: stretch;
  column-gap: 16rpx;
}

.summary-left,
.summary-right {
  display: flex;
  flex-direction: column;
}

.summary-left {
  flex: 1.6;
  min-width: 0;
  row-gap: 16rpx;
}

.summary-right {
  flex: 0.72;
  min-width: 168rpx;
  justify-content: center;
  row-gap: 16rpx;
}

.summary-title,
.summary-side-label,
.summary-side-profit-label {
  color: rgba(255, 255, 255, 0.94);
}

.summary-title,
.summary-side-label {
  font-size: $font-sm;
}

.summary-divider {
  width: 2rpx;
  background: rgba(255, 255, 255, 0.24);
}

.summary-side-profit {
  display: flex;
  flex-direction: column;
  row-gap: 8rpx;
}

.summary-rate-line {
  display: flex;
  align-items: flex-end;
  column-gap: 4rpx;
  white-space: nowrap;
}

.summary-rate-unit {
  padding-bottom: 4rpx;
  font-size: $font-sm;
  color: rgba(255, 255, 255, 0.94);
  font-weight: 600;
}

.summary-total-amount,
.summary-rate-amount,
.summary-profit-amount {
  white-space: nowrap;
}

.summary-right :deep(.summary-rate-amount.app-amount) {
  font-size: 38rpx !important;
  line-height: 1.1;
}

.summary-right :deep(.summary-profit-amount.app-amount) {
  font-size: 24rpx !important;
  line-height: 1.2;
}

.distribution-layout {
  display: flex;
  align-items: center;
  column-gap: 28rpx;
  touch-action: pan-y;
}

.section-action-text {
  font-size: $font-sm;
  color: var(--xo-text-regular);
}

.distribution-donut {
  width: 232rpx;
  height: 232rpx;
  border-radius: 50%;
  padding: 20rpx;
  box-sizing: border-box;
  flex-shrink: 0;
}

.distribution-donut-center {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--xo-component-card-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  row-gap: 8rpx;
  overflow: hidden;
}

.distribution-center-label {
  font-size: $font-xs;
  color: var(--xo-text-secondary);
}

.distribution-donut-center :deep(.distribution-center-amount.app-amount) {
  max-width: 138rpx;
  display: block;
  overflow: hidden;
  text-align: center;
  white-space: nowrap;
  font-size: 24rpx !important;
  line-height: 1.15;
}

.distribution-list {
  flex: 1;
  min-width: 0;
}

.distribution-item + .distribution-item {
  margin-top: 18rpx;
}

.distribution-item.active .distribution-name-text,
.distribution-item.active .distribution-amount,
.distribution-item.active .distribution-percent {
  color: var(--xo-primary);
}

.distribution-item-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  column-gap: 12rpx;
}

.distribution-item-name {
  display: flex;
  align-items: center;
  min-width: 0;
}

.distribution-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 12rpx;
  flex-shrink: 0;
}

.distribution-name-text,
.distribution-amount,
.distribution-percent {
  font-size: $font-sm;
  color: var(--xo-text-primary);
  font-variant-numeric: tabular-nums;
}

.distribution-percent {
  color: var(--xo-text-regular);
}

.holdings-card {
  overflow: hidden;
}

.holding-analysis-entry {
  display: flex;
  justify-content: flex-end;
  padding-top: 18rpx;
}

.holding-analysis-text {
  font-size: $font-sm;
  color: var(--xo-primary);
}

.holding-header {
  display: grid;
  grid-template-columns: var(--xo-invest-holding-grid);
  column-gap: 16rpx;
  padding: 8rpx 0 18rpx;
  border-bottom: 1rpx solid var(--xo-border-color);
}

.holding-col {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.holding-col-amount,
.holding-col-profit {
  text-align: right;
}

.holding-body {
  display: flex;
  flex-direction: column;
}

.action-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: var(--xo-invest-action-gap);
  padding-bottom: calc(env(safe-area-inset-bottom, 0px) + 8rpx);
}

.trade-action-button {
  border-radius: 999rpx;
}

.trade-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 80;
  display: flex;
  align-items: flex-end;
  background: var(--xo-mask);
}

.trade-sheet {
  width: 100%;
  padding: 30rpx 30rpx calc(36rpx + env(safe-area-inset-bottom, 0px));
  border-radius: 34rpx 34rpx 0 0;
  background: var(--xo-component-card-bg);
  box-shadow: var(--xo-shadow-floating);
  box-sizing: border-box;
}

.trade-sheet-head,
.trade-selector,
.trade-note-row,
.trade-submit {
  display: flex;
  align-items: center;
}

.trade-sheet-head {
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.trade-sheet-title {
  color: var(--xo-text-primary);
  font-size: 34rpx;
  font-weight: 800;
}

.trade-sheet-close {
  color: var(--xo-text-secondary);
  font-size: 26rpx;
}

.trade-selector,
.trade-note-row,
.trade-field {
  border: 1rpx solid var(--xo-border-color);
  border-radius: var(--xo-radius-lg);
  background: var(--xo-card-bg);
}

.trade-selector {
  justify-content: space-between;
  min-height: 86rpx;
  margin-top: 14rpx;
  padding: 0 22rpx;
}

.trade-label {
  flex-shrink: 0;
  color: var(--xo-text-regular);
  font-size: 25rpx;
}

.trade-value {
  overflow: hidden;
  margin-left: 24rpx;
  color: var(--xo-text-primary);
  font-size: 28rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trade-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
  margin-top: 14rpx;
}

.trade-field {
  min-width: 0;
  padding: 18rpx 20rpx;
}

.trade-input {
  width: 100%;
  margin-top: 10rpx;
  color: var(--xo-text-primary);
  font-size: 30rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.trade-note-row {
  min-height: 86rpx;
  gap: 20rpx;
  margin-top: 14rpx;
  padding: 0 22rpx;
}

.trade-note-input {
  flex: 1;
  min-width: 0;
  color: var(--xo-text-primary);
  font-size: 28rpx;
}

.trade-placeholder {
  color: var(--xo-text-placeholder);
}

.trade-submit {
  justify-content: center;
  height: 88rpx;
  margin-top: 26rpx;
  border-radius: var(--xo-radius-round);
  background: var(--xo-gradient-button-primary);
  color: var(--xo-white);
  font-size: 30rpx;
  font-weight: 800;
  box-shadow: var(--xo-shadow-button);
}

.trade-submit.disabled {
  opacity: 0.68;
}

.empty-state {
  padding: 48rpx 0 16rpx;
  text-align: center;
  color: var(--xo-text-secondary);
}
</style>
