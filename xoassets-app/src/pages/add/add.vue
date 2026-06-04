<template>
  <AppPage class="add-page" safe-top safe-bottom gap="24rpx">
    <view class="page-header">
      <text class="page-title">记账</text>
      <view class="page-toolbar">
        <view class="month-switch">
          <view class="month-arrow" @click="changeMonth(-1)">
            <AppIcon name="common.back" size="28rpx" />
          </view>
          <text class="month-text">{{ currentMonthLabel }}</text>
          <view class="month-arrow" @click="changeMonth(1)">
            <AppIcon name="common.arrowRight" size="28rpx" />
          </view>
        </view>
        <view class="header-actions">
          <view class="icon-button" @click="goSearch">
            <AppIcon name="home.search" size="34rpx" />
          </view>
        </view>
      </view>
    </view>

    <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="calendar-card">
      <view class="week-row">
        <text v-for="week in weekLabels" :key="week" class="week-label">{{ week }}</text>
      </view>

      <view
        class="calendar-grid"
        @touchstart="onCalendarTouchStart"
        @touchend="onCalendarTouchEnd"
      >
        <view
          v-for="cell in calendarCells"
          :key="cell.dateKey"
          class="calendar-cell"
          :class="{
            'is-selected': cell.isSelected,
            'is-today': cell.isToday,
            'is-muted': !cell.isCurrentMonth
          }"
          @click="selectCalendarDate(cell.fullDate)"
        >
          <text class="calendar-day">{{ cell.day }}</text>
          <view class="calendar-amounts">
            <text v-if="cell.incomeAmount > 0" class="calendar-amount income">+{{ formatCalendarAmount(cell.incomeAmount) }}</text>
            <text v-if="cell.expenseAmount > 0" class="calendar-amount expense">-{{ formatCalendarAmount(cell.expenseAmount) }}</text>
          </view>
        </view>
      </view>
    </AppCard>

    <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="daily-summary-card">
      <view class="daily-summary-header">
        <text class="daily-summary-date">{{ selectedDateLabel }}</text>
        <text class="daily-summary-count">{{ selectedDateRecords.length }} 笔</text>
      </view>
      <view class="daily-summary-values">
        <view class="daily-summary-item">
          <text class="daily-summary-label">当天收入</text>
          <AppAmount :value="selectedDateIncome" prefix="¥ " size="sm" tone="positive" />
        </view>
        <view class="daily-summary-item">
          <text class="daily-summary-label">当天支出</text>
          <AppAmount :value="selectedDateExpense" prefix="¥ " size="sm" tone="negative" />
        </view>
      </view>
    </AppCard>

    <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="records-card">
      <view class="records-header">
        <text class="records-title">当天流水</text>
        <text class="records-filter">{{ activeFilterLabel }}</text>
      </view>

      <view v-if="selectedDateRecords.length" class="record-list">
        <view
          v-for="record in selectedDateRecords"
          :key="record.id"
          class="record-item"
          @click="goDetail(record.id)"
        >
          <view class="record-icon">
            <AppIcon :name="record.iconName" size="46rpx" />
          </view>
          <view class="record-main">
            <view class="record-title-row">
              <text class="record-title">{{ record.title }}</text>
              <text class="record-amount" :class="record.amountClass">{{ record.amountText }}</text>
            </view>
            <view class="record-meta-row">
              <text class="record-account">{{ record.accountText }}</text>
              <text class="record-time">{{ record.timeText }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else-if="!loading" class="empty-state">
        <text>当天暂无流水</text>
      </view>

      <view v-else class="empty-state">
        <text>加载中…</text>
      </view>
    </AppCard>

    <view class="floating-add-button" @click="openComposer">
      <AppIcon name="common.add" size="44rpx" />
    </view>

    <view v-if="showComposer" class="composer-overlay">
      <view class="composer-screen">
        <view class="composer-safe-top"></view>

        <view class="composer-topbar">
          <view class="topbar-back" @click="closeComposer">
            <AppIcon name="common.back" size="34rpx" />
          </view>
          <text class="topbar-title">流水录入</text>
          <text class="topbar-spacer"></text>
        </view>

        <view class="composer-type-tabs">
          <view class="composer-type-track">
            <view class="composer-type-tab" :class="{ active: tab === 'EXPENSE' }" @click="switchTab('EXPENSE')">支出</view>
            <view class="composer-type-tab" :class="{ active: tab === 'INCOME' }" @click="switchTab('INCOME')">收入</view>
            <view class="composer-type-tab" :class="{ active: tab === 'TRANSFER' }" @click="switchTab('TRANSFER')">转账</view>
          </view>
        </view>

        <scroll-view scroll-y class="composer-scroll">
          <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="composer-amount-card">
            <view class="composer-amount-head">
              <text class="composer-card-label">金额</text>
            </view>
            <view class="composer-amount-row">
              <text class="composer-amount-text">¥{{ amountDisplay }}</text>
              <view class="ocr-entry" @click="handleOcrEntry">
                <AppIcon name="common.camera" size="24rpx" />
                <text class="ocr-text">拍照识别</text>
              </view>
            </view>
          </AppCard>

          <AppCard
            v-if="tab !== 'TRANSFER'"
            :padding="theme.spacing.lg"
            :radius="theme.radius.xl"
            class="composer-category-card"
          >
            <text class="composer-card-title">分类</text>

            <view class="category-grid">
              <view
                v-for="item in visibleCategoryOptions"
                :key="item.key"
                class="category-grid-item"
                :class="{ active: selectedCategory?.id === item.category?.id }"
                @click="selectPresetCategory(item)"
              >
                <view class="category-grid-icon" :style="{ background: item.background, color: item.color }">
                  <AppIcon :name="`category.${item.iconKey}`" size="34rpx" :color="item.color" />
                </view>
                <text class="category-grid-name">{{ item.label }}</text>
              </view>
            </view>

            <view class="recent-header">
              <text class="recent-title">最近使用</text>
              <text class="recent-edit">编辑</text>
            </view>

            <view class="recent-chips">
              <view
                v-for="item in recentCategoryOptions"
                :key="item.key"
                class="recent-chip"
                @click="selectPresetCategory(item)"
              >
                <view class="recent-chip-icon" :style="{ color: item.color, background: item.softBackground }">
                  <AppIcon :name="`category.${item.iconKey}`" size="24rpx" :color="item.color" />
                </view>
                <text class="recent-chip-text">{{ item.label }}</text>
              </view>
            </view>
          </AppCard>

          <AppCard :padding="theme.spacing.lg" :radius="theme.radius.xl" class="composer-form-card">
            <view class="composer-form-row" @click="showAccountPicker = true">
              <view class="form-row-label">
                <view class="form-row-icon">
                  <AppIcon name="common.account" size="30rpx" />
                </view>
                <text class="form-row-name">账户</text>
              </view>
              <view class="form-row-value-group">
                <text class="form-row-value" :class="{ placeholder: !selectedAccount }">
                  {{ selectedAccount ? selectedAccount.name : '选择账户' }}
                </text>
                <AppIcon name="common.arrowRight" size="26rpx" />
              </view>
            </view>

            <view v-if="tab === 'TRANSFER'" class="composer-form-row" @click="showTargetPicker = true">
              <view class="form-row-label">
                <view class="form-row-icon">
                  <AppIcon name="quickActions.transfer" size="30rpx" />
                </view>
                <text class="form-row-name">目标账户</text>
              </view>
              <view class="form-row-value-group">
                <text class="form-row-value" :class="{ placeholder: !targetAccount }">
                  {{ targetAccount ? targetAccount.name : '选择目标账户' }}
                </text>
                <AppIcon name="common.arrowRight" size="26rpx" />
              </view>
            </view>

            <view class="composer-form-row">
              <view class="form-row-label">
                <view class="form-row-icon">
                  <AppIcon name="common.time" size="30rpx" />
                </view>
                <text class="form-row-name">日期时间</text>
              </view>
              <view class="form-row-value-group">
                <picker mode="date" :value="dateStr" @change="onDateChange">
                  <text class="form-row-value">{{ dateDisplay }}</text>
                </picker>
                <picker mode="time" :value="timeStr" @change="onTimeChange">
                  <text class="form-row-value form-row-time">{{ timeStr }}</text>
                </picker>
                <AppIcon name="common.arrowRight" size="26rpx" />
              </view>
            </view>

            <view class="composer-form-row">
              <view class="form-row-label">
                <view class="form-row-icon">
                  <AppIcon name="common.note" size="30rpx" />
                </view>
                <text class="form-row-name">备注</text>
              </view>
              <view class="form-row-note-group">
                <input
                  v-model="note"
                  maxlength="50"
                  class="form-row-note-input"
                  placeholder="请输入备注（可选）"
                  placeholder-style="color:#C0C7D2"
                />
                <text class="form-row-note-count">{{ note.length }}/50</text>
              </view>
            </view>

            <view class="composer-form-row image-row" @click="openImageActionSheet">
              <view class="form-row-label">
                <view class="form-row-icon">
                  <AppIcon name="common.album" size="30rpx" />
                </view>
                <text class="form-row-name">图片</text>
              </view>
              <view class="image-row-content">
                <image
                  v-if="localImageUrl"
                  :src="localImageUrl"
                  class="image-preview"
                  mode="aspectFill"
                />
                <view v-else class="image-placeholder">
                  <AppIcon name="common.album" size="38rpx" />
                </view>
                <view class="image-camera-icon" @click.stop="openImageActionSheet">
                  <AppIcon name="common.camera" size="40rpx" />
                </view>
              </view>
            </view>
          </AppCard>
        </scroll-view>

        <view class="keyboard-panel">
          <view
            v-for="keyItem in keyboardKeys"
            :key="keyItem.key"
            class="keyboard-key"
            :class="{
              'is-primary': keyItem.key === 'save',
              'is-blank': keyItem.key === 'blank'
            }"
            @click="handleKeyboardKey(keyItem.key)"
          >
            <text class="keyboard-key-text">{{ keyItem.label }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="showAccountPicker" class="picker-overlay" @click="showAccountPicker = false">
      <view class="picker-sheet" @click.stop>
        <view class="picker-title">选择账户</view>
        <scroll-view scroll-y class="picker-list">
          <view
            v-for="account in accountStore.accounts"
            :key="account.id"
            class="picker-item"
            @click="selectAccount(account)"
          >
            <text>{{ account.name }}</text>
            <text class="picker-balance">¥{{ fmtAmount(account.balance) }}</text>
          </view>
        </scroll-view>
      </view>
    </view>

    <view v-if="showTargetPicker" class="picker-overlay" @click="showTargetPicker = false">
      <view class="picker-sheet" @click.stop>
        <view class="picker-title">选择目标账户</view>
        <scroll-view scroll-y class="picker-list">
          <view
            v-for="account in targetAccountOptions"
            :key="account.id"
            class="picker-item"
            @click="selectTargetAccount(account)"
          >
            <text>{{ account.name }}</text>
            <text class="picker-balance">¥{{ fmtAmount(account.balance) }}</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppAmount from '@/components/app/AppAmount.vue'
import AppCard from '@/components/app/AppCard.vue'
import AppIcon from '@/components/app/AppIcon.vue'
import AppPage from '@/components/app/AppPage.vue'
import { categoryApi, type CategoryItem } from '@/services/categoryApi'
import { transactionApi, type TransactionItem, type TransactionType } from '@/services/transactionApi'
import { useAccountStore } from '@/stores/account'
import { useTransactionStore } from '@/stores/transaction'
import { useTheme } from '@/theme/useTheme'

type CalendarCell = {
  dateKey: string
  fullDate: string
  day: number
  isCurrentMonth: boolean
  isToday: boolean
  isSelected: boolean
  hasRecords: boolean
  incomeAmount: number
  expenseAmount: number
}

type CategoryPreset = {
  key: string
  label: string
  iconKey: string
  category: CategoryItem | null
  color: string
  background: string
  softBackground: string
}

type KeyboardKey = {
  key: string
  label: string
}

const weekLabels = ['一', '二', '三', '四', '五', '六', '日']
const keyboardKeys: KeyboardKey[] = [
  { key: '1', label: '1' },
  { key: '2', label: '2' },
  { key: '3', label: '3' },
  { key: 'delete', label: '⌫' },
  { key: '4', label: '4' },
  { key: '5', label: '5' },
  { key: '6', label: '6' },
  { key: '-', label: '-' },
  { key: '7', label: '7' },
  { key: '8', label: '8' },
  { key: '9', label: '9' },
  { key: '+', label: '+' },
  { key: '.', label: '.' },
  { key: '0', label: '0' },
  { key: 'blank', label: '' },
  { key: 'save', label: '保存' }
]

const expensePresets = [
  { key: 'dining', label: '餐饮', match: ['餐饮', '吃饭', '外卖'] },
  { key: 'transit', label: '交通', match: ['交通', '公交', '打车'] },
  { key: 'shopping', label: '购物', match: ['购物', '日用'] },
  { key: 'entertainment', label: '娱乐', match: ['娱乐', '游戏', '电影'] },
  { key: 'medical', label: '医疗', match: ['医疗', '药', '医院'] },
  { key: 'bills', label: '生活缴费', match: ['生活', '缴费', '水电'] },
  { key: 'education', label: '教育', match: ['教育', '学习', '培训'] },
  { key: 'other', label: '其他', match: ['其他'] }
]

const incomePresets = [
  { key: 'salary', label: '工资', match: ['工资', '薪资'] },
  { key: 'bonus', label: '奖金', match: ['奖金', '奖励'] },
  { key: 'refund', label: '退款', match: ['退款'] },
  { key: 'other', label: '其他', match: ['其他'] }
]

const now = new Date()
const accountStore = useAccountStore()
const txnStore = useTransactionStore()
const { currentTheme } = useTheme()

const theme = computed(() => currentTheme.value)
const displayMonth = ref(startOfMonth(now))
const selectedDate = ref(formatDate(now))
const activeFilter = ref<TransactionType | ''>('')
const records = ref<TransactionItem[]>([])
const loading = ref(false)
const showComposer = ref(false)

const tab = ref<TransactionType>('EXPENSE')
const amountInput = ref('')
const selectedAccount = ref<any>(null)
const targetAccount = ref<any>(null)
const selectedCategory = ref<CategoryItem | null>(null)
const categories = ref<CategoryItem[]>([])
const note = ref('')
const dateStr = ref(formatDate(now))
const timeStr = ref(formatTime(now))
const localImageUrl = ref('')
const calendarTouchStartX = ref(0)
const calendarTouchStartY = ref(0)

const showAccountPicker = ref(false)
const showTargetPicker = ref(false)

const currentMonthLabel = computed(() => `${displayMonth.value.getFullYear()}年${displayMonth.value.getMonth() + 1}月`)
const selectedDateLabel = computed(() => {
  const date = parseDate(selectedDate.value)
  return `${date.getMonth() + 1}月${date.getDate()}日`
})
const activeFilterLabel = computed(() => {
  if (activeFilter.value === 'INCOME') return '仅收入'
  if (activeFilter.value === 'EXPENSE') return '仅支出'
  if (activeFilter.value === 'TRANSFER') return '仅转账'
  return '全部类型'
})
const dateDisplay = computed(() => {
  const date = parseDate(dateStr.value)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
})
const amountDisplay = computed(() => (amountInput.value || '0').replace(/^\+/, ''))

const fallbackCategoryColor = computed(() => theme.value.colors.primary)

const canSubmit = computed(() => {
  const amount = getNormalizedAmount()
  if (!amount || amount <= 0) return false
  if (!selectedAccount.value) return false
  if (tab.value === 'TRANSFER' && !targetAccount.value) return false
  if (tab.value !== 'TRANSFER' && !selectedCategory.value) return false
  return true
})

const targetAccountOptions = computed(() => accountStore.accounts.filter((account) => account.id !== selectedAccount.value?.id))

const filteredRecords = computed(() => {
  if (!activeFilter.value) return records.value
  return records.value.filter((record) => record.type === activeFilter.value)
})

const monthRecords = computed(() => {
  const currentYear = displayMonth.value.getFullYear()
  const currentMonth = displayMonth.value.getMonth()
  return filteredRecords.value.filter((record) => {
    const date = normalizeTransactionDate(record.transactionTime)
    return date.getFullYear() === currentYear && date.getMonth() === currentMonth
  })
})

const selectedDateRecords = computed(() => {
  return monthRecords.value
    .filter((record) => extractDatePart(record.transactionTime) === selectedDate.value)
    .sort((a, b) => normalizeTransactionDate(b.transactionTime).getTime() - normalizeTransactionDate(a.transactionTime).getTime())
    .map((record) => {
      return {
        ...record,
        title: record.categoryName || typeLabel(record.type),
        iconName: resolveRecordIconName(record),
        accountText: record.targetAccountName ? `${record.accountName || '未知账户'} → ${record.targetAccountName}` : (record.accountName || '未知账户'),
        timeText: formatRecordTime(record.transactionTime),
        amountText: formatAmountWithSign(record),
        amountClass: amountClass(record.type)
      }
    })
})

const selectedDateIncome = computed(() => {
  return selectedDateRecords.value
    .filter((record) => record.type === 'INCOME' || record.type === 'REFUND')
    .reduce((sum, record) => sum + record.amount, 0)
})

const selectedDateExpense = computed(() => {
  return selectedDateRecords.value
    .filter((record) => record.type === 'EXPENSE')
    .reduce((sum, record) => sum + record.amount, 0)
})

const calendarDailyAmounts = computed(() => {
  const amountMap: Record<string, { income: number; expense: number }> = {}
  monthRecords.value.forEach((record) => {
    const dateKey = extractDatePart(record.transactionTime)
    if (!amountMap[dateKey]) amountMap[dateKey] = { income: 0, expense: 0 }
    if (record.type === 'INCOME' || record.type === 'REFUND') amountMap[dateKey].income += record.amount
    if (record.type === 'EXPENSE') amountMap[dateKey].expense += record.amount
  })
  return amountMap
})

const calendarCells = computed<CalendarCell[]>(() => {
  const firstDay = startOfMonth(displayMonth.value)
  const firstWeekday = getCalendarWeekday(firstDay)
  const daysInMonth = getDaysInMonth(displayMonth.value)
  const totalCells = Math.ceil((firstWeekday + daysInMonth) / 7) * 7
  const cells: CalendarCell[] = []

  for (let index = 0; index < totalCells; index += 1) {
    const offset = index - firstWeekday
    const cellDate = new Date(firstDay)
    cellDate.setDate(firstDay.getDate() + offset)
    const fullDate = formatDate(cellDate)
    const dayAmounts = calendarDailyAmounts.value[fullDate] || { income: 0, expense: 0 }
    cells.push({
      dateKey: `${fullDate}-${index}`,
      fullDate,
      day: cellDate.getDate(),
      isCurrentMonth: cellDate.getMonth() === displayMonth.value.getMonth(),
      isToday: fullDate === formatDate(now),
      isSelected: fullDate === selectedDate.value,
      hasRecords: dayAmounts.income > 0 || dayAmounts.expense > 0,
      incomeAmount: dayAmounts.income,
      expenseAmount: dayAmounts.expense
    })
  }

  return cells
})

const visibleCategoryOptions = computed(() => {
  const presets = tab.value === 'INCOME' ? incomePresets : expensePresets
  return presets.map((preset, index) => createCategoryPreset(preset, index))
})

const recentCategoryOptions = computed(() => {
  const recentKeys = loadRecentCategoryKeys(tab.value)
  const source = visibleCategoryOptions.value
  const recent = recentKeys
    .map((key: string) => source.find((item) => item.key === key))
    .filter(Boolean) as CategoryPreset[]
  return recent.length ? recent : source.slice(0, Math.min(4, source.length))
})

onShow(async () => {
  applyPendingComposerType()
  await Promise.all([accountStore.fetchAccounts(), loadCategories(), refreshTransactions()])
})

async function refreshTransactions() {
  loading.value = true
  try {
    records.value = await loadAllTransactions(activeFilter.value || undefined)
  } finally {
    loading.value = false
  }
}

async function loadAllTransactions(type?: TransactionType) {
  const pageSize = 100
  let pageNo = 1
  let total = 0
  const allRecords: TransactionItem[] = []

  do {
    const result = await transactionApi.page({ pageNo, pageSize, type })
    total = result.total
    allRecords.push(...result.records)
    pageNo += 1
  } while (allRecords.length < total && pageNo <= 20)

  return allRecords
}

function changeMonth(delta: number) {
  const next = new Date(displayMonth.value)
  next.setMonth(next.getMonth() + delta, 1)
  displayMonth.value = startOfMonth(next)
  const todayText = formatDate(now)
  selectedDate.value = isSameYearMonth(displayMonth.value, now) ? todayText : formatDate(displayMonth.value)
}

function onCalendarTouchStart(event: TouchEvent) {
  const touch = event.touches[0]
  calendarTouchStartX.value = touch?.clientX || 0
  calendarTouchStartY.value = touch?.clientY || 0
}

function onCalendarTouchEnd(event: TouchEvent) {
  const touch = event.changedTouches[0]
  const deltaX = (touch?.clientX || 0) - calendarTouchStartX.value
  const deltaY = (touch?.clientY || 0) - calendarTouchStartY.value
  if (Math.abs(deltaX) < 70 || Math.abs(deltaX) < Math.abs(deltaY) * 1.4) return
  changeMonth(deltaX < 0 ? 1 : -1)
}

function selectCalendarDate(dateText: string) {
  selectedDate.value = dateText
  const date = parseDate(dateText)
  if (!isSameYearMonth(displayMonth.value, date)) {
    displayMonth.value = startOfMonth(date)
  }
}

function goSearch() {
  uni.navigateTo({ url: '/pages/transactions/transactions' })
}

function openComposer() {
  showComposer.value = true
  dateStr.value = selectedDate.value
}

function closeComposer() {
  showComposer.value = false
}

function switchTab(type: TransactionType) {
  tab.value = type
  selectedCategory.value = null
  targetAccount.value = null
  localImageUrl.value = ''
  amountInput.value = ''
  loadCategories()
}

function applyPendingComposerType() {
  const pendingType = uni.getStorageSync('xoassets:add:pending-type') as TransactionType | ''
  if (!pendingType) return
  uni.removeStorageSync('xoassets:add:pending-type')
  // Tab 页无法携带 query 参数，首页快捷入口通过一次性 storage 指定默认记账类型。
  if (['EXPENSE', 'INCOME', 'TRANSFER'].includes(pendingType)) {
    tab.value = pendingType
    selectedCategory.value = null
    targetAccount.value = null
    localImageUrl.value = ''
    amountInput.value = ''
  }
}

async function loadCategories() {
  if (tab.value === 'TRANSFER') {
    categories.value = []
    return
  }
  try {
    categories.value = await categoryApi.list(tab.value === 'EXPENSE' ? 'EXPENSE' : 'INCOME')
  } catch {
    categories.value = []
  }
}

function createCategoryPreset(
  preset: { key: string; label: string; match: string[] },
  index: number
): CategoryPreset {
  const category = categories.value.find((item) => preset.match.some((keyword) => item.name.includes(keyword))) || null
  const palette = [
    { color: theme.value.colors.primary, background: 'rgba(47,123,255,0.10)', softBackground: 'rgba(47,123,255,0.08)' },
    { color: '#47D7A8', background: 'rgba(71,215,168,0.10)', softBackground: 'rgba(71,215,168,0.08)' },
    { color: '#8D68FF', background: 'rgba(141,104,255,0.10)', softBackground: 'rgba(141,104,255,0.08)' },
    { color: '#FF9A44', background: 'rgba(255,154,68,0.10)', softBackground: 'rgba(255,154,68,0.08)' },
    { color: '#FF6F7A', background: 'rgba(255,111,122,0.10)', softBackground: 'rgba(255,111,122,0.08)' },
    { color: '#57A7FF', background: 'rgba(87,167,255,0.10)', softBackground: 'rgba(87,167,255,0.08)' },
    { color: '#41D2A1', background: 'rgba(65,210,161,0.10)', softBackground: 'rgba(65,210,161,0.08)' },
    { color: '#A7AFBF', background: 'rgba(167,175,191,0.10)', softBackground: 'rgba(167,175,191,0.08)' }
  ]
  const tone = palette[index % palette.length]
  return {
    key: preset.key,
    label: category?.name || preset.label,
    iconKey: preset.key,
    category,
    color: tone.color,
    background: tone.background,
    softBackground: tone.softBackground
  }
}

function selectPresetCategory(item: CategoryPreset) {
  if (item.category) {
    selectedCategory.value = item.category
    saveRecentCategoryKey(tab.value, item.key)
    return
  }
  uni.showToast({ title: '当前分类未配置', icon: 'none' })
}

function selectAccount(account: any) {
  selectedAccount.value = account
  showAccountPicker.value = false
  if (tab.value === 'TRANSFER' && targetAccount.value?.id === account.id) {
    targetAccount.value = null
  }
}

function selectTargetAccount(account: any) {
  targetAccount.value = account
  showTargetPicker.value = false
}

function onDateChange(event: any) {
  dateStr.value = event.detail.value
}

function onTimeChange(event: any) {
  timeStr.value = event.detail.value
}

function handleKeyboardKey(key: string) {
  if (key === 'blank') return
  if (key === 'save') {
    handleSubmit()
    return
  }
  if (key === 'delete') {
    amountInput.value = amountInput.value.slice(0, -1)
    return
  }
  if (key === '+') {
    amountInput.value = amountInput.value.replace(/^-/, '')
    return
  }
  if (key === '-') {
    if (!amountInput.value) {
      amountInput.value = '-'
      return
    }
    amountInput.value = amountInput.value.startsWith('-') ? amountInput.value.slice(1) : `-${amountInput.value}`
    return
  }
  if (key === '.') {
    if (!amountInput.value) {
      amountInput.value = '0.'
      return
    }
    if (amountInput.value.includes('.')) return
    amountInput.value = `${amountInput.value}.`
    return
  }

  const unsignedValue = amountInput.value.replace(/^-/, '')
  if (unsignedValue === '0' && key !== '.') {
    amountInput.value = amountInput.value.startsWith('-') ? `-${key}` : key
    return
  }
  amountInput.value += key
}

function getNormalizedAmount() {
  const parsed = Number.parseFloat(amountInput.value)
  if (Number.isNaN(parsed)) return 0
  return Math.abs(parsed)
}

function handleOcrEntry() {
  uni.showToast({ title: '拍照识别待接入', icon: 'none' })
}

function openImageActionSheet() {
  uni.showActionSheet({
    itemList: ['从相册选择', '拍照'],
    success: (res) => {
      const sourceType = res.tapIndex === 1 ? ['camera'] : ['album']
      chooseImage(sourceType as Array<'album' | 'camera'>)
    }
  })
}

function chooseImage(sourceType: Array<'album' | 'camera'>) {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType,
    success: (res) => {
      localImageUrl.value = res.tempFilePaths[0] || ''
    }
  })
}

async function handleSubmit() {
  if (!canSubmit.value) return
  const amount = getNormalizedAmount()
  try {
    await txnStore.create({
      type: tab.value,
      amount,
      accountId: selectedAccount.value.id,
      targetAccountId: tab.value === 'TRANSFER' ? targetAccount.value?.id : null,
      categoryId: tab.value !== 'TRANSFER' ? selectedCategory.value?.id : null,
      transactionTime: formatTransactionDateTime(dateStr.value, timeStr.value),
      // TODO: 后端交易接口当前未接图片字段，先只在本地录入层保留预览状态。
      note: note.value || undefined
    })

    if (tab.value !== 'TRANSFER' && selectedCategory.value) {
      const matched = visibleCategoryOptions.value.find((item) => item.category?.id === selectedCategory.value?.id)
      if (matched) saveRecentCategoryKey(tab.value, matched.key)
    }

    uni.showToast({ title: submitSuccessText(tab.value), icon: 'success' })
    resetComposer()
    selectedDate.value = dateStr.value
    displayMonth.value = startOfMonth(parseDate(dateStr.value))
    await Promise.all([accountStore.fetchAccounts(), refreshTransactions()])
  } catch (error: any) {
    uni.showToast({ title: error.message || '记账失败', icon: 'none' })
  }
}

function resetComposer() {
  amountInput.value = ''
  note.value = ''
  selectedCategory.value = null
  targetAccount.value = null
  localImageUrl.value = ''
  showComposer.value = false
}

function resolveRecordIconName(record: TransactionItem) {
  if (record.type === 'TRANSFER') return 'quickActions.transfer'
  if (record.type === 'INCOME' || record.type === 'REFUND') return 'home.income'
  const preset = expensePresets.find((item) => item.match.some((keyword) => record.categoryName?.includes(keyword)))
  return preset ? `category.${preset.key}` : 'home.expense'
}

function formatAmountWithSign(record: TransactionItem) {
  const amount = fmtAmount(record.amount)
  if (record.type === 'INCOME' || record.type === 'REFUND') return `+${amount}`
  if (record.type === 'EXPENSE') return `-${amount}`
  return amount
}

function amountClass(type: TransactionType) {
  if (type === 'INCOME' || type === 'REFUND') return 'income'
  if (type === 'EXPENSE') return 'expense'
  return 'transfer'
}

function typeLabel(type: TransactionType) {
  const map: Record<TransactionType, string> = {
    INCOME: '收入',
    EXPENSE: '支出',
    TRANSFER: '转账',
    REFUND: '退款'
  }
  return map[type]
}

function formatRecordTime(time: string) {
  const date = normalizeTransactionDate(time)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function submitSuccessText(type: TransactionType) {
  if (type === 'EXPENSE') return '支出已记录'
  if (type === 'INCOME') return '收入已记录'
  return '转账已记录'
}

function goDetail(id: string) {
  uni.navigateTo({ url: `/pages/transaction-detail/transaction-detail?id=${id}` })
}

function loadRecentCategoryKeys(type: TransactionType) {
  const key = getRecentCategoryStorageKey(type)
  return (uni.getStorageSync(key) || []) as string[]
}

function saveRecentCategoryKey(type: TransactionType, key: string) {
  const storageKey = getRecentCategoryStorageKey(type)
  const next = [key, ...loadRecentCategoryKeys(type).filter((item: string) => item !== key)].slice(0, 4)
  uni.setStorageSync(storageKey, next)
}

function getRecentCategoryStorageKey(type: TransactionType) {
  return `xoassets:add:recent-category:${type}`
}

function fmtAmount(value: number | null | undefined) {
  if (value == null) return '--'
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatCalendarAmount(value: number) {
  if (value >= 10000) return `${Math.round(value / 1000) / 10}w`
  if (value >= 1000) return `${Math.round(value)}`
  return `${Math.round(value * 10) / 10}`.replace(/\.0$/, '')
}

function formatDate(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function formatTime(date: Date) {
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatTransactionDateTime(date: string, time: string) {
  // uni-app time picker 只提供 HH:mm，提交接口时补秒，避免后端 LocalDateTime 解析失败。
  return `${date} ${time}:00`
}

function parseDate(text: string) {
  const [year, month, day] = text.split('-').map((value) => Number(value))
  return new Date(year, month - 1, day)
}

function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

function getDaysInMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate()
}

function getCalendarWeekday(date: Date) {
  const day = date.getDay()
  return day === 0 ? 6 : day - 1
}

function extractDatePart(text: string) {
  return text.slice(0, 10)
}

function normalizeTransactionDate(text: string) {
  return new Date(text.replace(' ', 'T'))
}

function isSameYearMonth(left: Date, right: Date) {
  return left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth()
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.add-page {
  min-height: 100vh;
}

.page-header {
  display: flex;
  flex-direction: column;
  row-gap: 18rpx;
}

.page-title {
  font-size: $font-xl;
  font-weight: 700;
  color: var(--xo-text-primary);
  text-align: center;
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  column-gap: 16rpx;
}

.month-switch {
  display: inline-flex;
  align-items: center;
  column-gap: 18rpx;
  padding: 14rpx 24rpx;
  background: var(--xo-component-card-bg);
  border-radius: 999rpx;
  box-shadow: var(--xo-component-card-shadow);
}

.month-arrow,
.month-text {
  color: var(--xo-text-primary);
  font-size: $font-md;
}

.month-arrow {
  font-size: 34rpx;
  font-weight: 600;
}

.month-text {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.header-actions {
  display: flex;
  align-items: center;
  column-gap: 14rpx;
}

.icon-button {
  width: 72rpx;
  height: 72rpx;
  border-radius: 999rpx;
  background: var(--xo-component-card-bg);
  box-shadow: var(--xo-component-card-shadow);
  display: flex;
  align-items: center;
  justify-content: center;
}

.calendar-card {
  overflow: hidden;
}

.week-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  margin-bottom: 18rpx;
}

.week-label {
  text-align: center;
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 14rpx 0;
  touch-action: pan-y;
}

.calendar-cell {
  min-height: 104rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  row-gap: 4rpx;
}

.calendar-day {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: $font-md;
  color: var(--xo-text-primary);
}

.calendar-amounts {
  min-height: 34rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  row-gap: 0;
  transform: scale(0.86);
  transform-origin: top center;
}

.calendar-amount {
  max-width: 80rpx;
  font-size: 18rpx;
  line-height: 18rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.calendar-amount.income {
  color: var(--xo-positive);
}

.calendar-amount.expense {
  color: var(--xo-negative);
}

.calendar-cell.is-selected .calendar-day {
  background: var(--xo-gradient-asset-card);
  color: var(--xo-white);
  box-shadow: 0 8rpx 18rpx rgba(47, 123, 255, 0.24);
}

.calendar-cell.is-muted .calendar-day {
  color: var(--xo-text-placeholder);
}

.calendar-cell.is-today:not(.is-selected) .calendar-day {
  background: rgba(47, 123, 255, 0.10);
  color: var(--xo-primary);
}

.daily-summary-header,
.daily-summary-values,
.records-header,
.record-title-row,
.record-meta-row,
.record-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.daily-summary-header {
  margin-bottom: 18rpx;
}

.daily-summary-date,
.records-title {
  font-size: $font-lg;
  font-weight: 700;
  color: var(--xo-text-primary);
}

.daily-summary-count,
.records-filter {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.daily-summary-values {
  column-gap: 20rpx;
}

.daily-summary-item {
  flex: 1;
  background: var(--xo-page-bg);
  border-radius: 20rpx;
  padding: 22rpx 24rpx;
  display: flex;
  flex-direction: column;
  row-gap: 10rpx;
}

.daily-summary-label {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.record-list {
  display: flex;
  flex-direction: column;
}

.record-item {
  column-gap: 18rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid var(--xo-border-color);
}

.record-item:last-child {
  border-bottom: none;
}

.record-icon {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.record-main {
  min-width: 0;
  flex: 1;
}

.record-title-row {
  margin-bottom: 8rpx;
  column-gap: 16rpx;
}

.record-title {
  flex: 1;
  min-width: 0;
  font-size: $font-md;
  font-weight: 600;
  color: var(--xo-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-amount {
  flex-shrink: 0;
  font-size: $amount-sm;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.record-amount.income {
  color: var(--xo-positive);
}

.record-amount.expense {
  color: var(--xo-negative);
}

.record-amount.transfer {
  color: var(--xo-transfer);
}

.record-meta-row {
  column-gap: 16rpx;
}

.record-account,
.record-time {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.record-account {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.floating-add-button {
  position: fixed;
  right: 28rpx;
  bottom: calc(env(safe-area-inset-bottom, 0px) + 128rpx);
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  background: var(--xo-gradient-home-asset-card);
  box-shadow: var(--xo-shadow-floating);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
}

.composer-overlay,
.picker-overlay {
  position: fixed;
  inset: 0;
  background: var(--xo-mask);
  z-index: 999;
}

.composer-screen {
  width: 100%;
  height: 100%;
  background: var(--xo-page-bg);
  display: flex;
  flex-direction: column;
}

.composer-safe-top {
  height: env(safe-area-inset-top, 0px);
}

.composer-topbar {
  display: grid;
  grid-template-columns: 56rpx 1fr 56rpx;
  align-items: center;
  padding: 20rpx 28rpx 8rpx;
}

.topbar-back,
.topbar-spacer {
  font-size: 48rpx;
  color: var(--xo-text-primary);
}

.topbar-title {
  text-align: center;
  font-size: $font-xl;
  font-weight: 700;
  color: var(--xo-text-primary);
}

.composer-type-tabs {
  padding: 20rpx 24rpx 0;
}

.composer-type-track {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding: 8rpx;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 999rpx;
  box-shadow: var(--xo-component-card-shadow);
}

.composer-type-tab {
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  font-size: $font-lg;
  color: var(--xo-text-secondary);
}

.composer-type-tab.active {
  color: var(--xo-primary);
  background: var(--xo-card-bg);
  box-shadow: 0 8rpx 18rpx rgba(32, 88, 166, 0.10);
  font-weight: 600;
}

.composer-scroll {
  flex: 1;
  min-height: 0;
  padding: 20rpx 24rpx 0;
  box-sizing: border-box;
}

.composer-amount-card,
.composer-category-card,
.composer-form-card {
  margin-bottom: 20rpx;
}

.composer-card-label,
.composer-card-title {
  font-size: $font-lg;
  font-weight: 600;
  color: var(--xo-text-primary);
}

.composer-amount-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  column-gap: 20rpx;
  margin-top: 20rpx;
}

.composer-amount-text {
  flex: 1;
  min-width: 0;
  font-size: 84rpx;
  line-height: 1.05;
  color: #13254B;
  font-weight: 800;
  letter-spacing: 0;
}

.ocr-entry {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  column-gap: 8rpx;
  padding: 14rpx 18rpx;
  background: rgba(47, 123, 255, 0.06);
  border-radius: 999rpx;
  color: var(--xo-primary);
}

.ocr-text {
  font-size: $font-sm;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18rpx 12rpx;
  margin-top: 24rpx;
}

.category-grid-item {
  padding: 18rpx 10rpx 14rpx;
  border-radius: 26rpx;
  border: 2rpx solid transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  row-gap: 14rpx;
}

.category-grid-item.active {
  border-color: var(--xo-primary);
  box-shadow: 0 10rpx 24rpx rgba(47, 123, 255, 0.10);
}

.category-grid-icon {
  width: 92rpx;
  height: 92rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.category-grid-name {
  text-align: center;
  font-size: $font-md;
  color: var(--xo-text-primary);
}

.recent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid var(--xo-border-color);
}

.recent-title {
  font-size: $font-md;
  color: var(--xo-text-secondary);
}

.recent-edit {
  font-size: $font-md;
  color: var(--xo-primary);
}

.recent-chips {
  display: flex;
  gap: 14rpx;
  margin-top: 18rpx;
  flex-wrap: wrap;
}

.recent-chip {
  display: inline-flex;
  align-items: center;
  column-gap: 10rpx;
  padding: 14rpx 18rpx;
  border-radius: 999rpx;
  border: 1rpx solid var(--xo-border-color);
  background: var(--xo-card-bg);
}

.recent-chip-icon {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.recent-chip-text {
  font-size: $font-md;
  color: var(--xo-text-primary);
}

.composer-form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  column-gap: 16rpx;
  padding: 28rpx 0;
  border-bottom: 1rpx solid var(--xo-border-color);
}

.composer-form-row:last-child {
  border-bottom: none;
}

.form-row-label {
  display: inline-flex;
  align-items: center;
  column-gap: 18rpx;
  flex-shrink: 0;
}

.form-row-icon {
  width: 42rpx;
  text-align: center;
  color: var(--xo-primary);
  font-size: $font-md;
  font-weight: 700;
}

.form-row-name {
  font-size: $font-lg;
  color: var(--xo-text-primary);
}

.form-row-value-group {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  column-gap: 12rpx;
}

.form-row-value {
  font-size: $font-lg;
  color: #7A84A1;
  text-align: right;
}

.form-row-value.placeholder {
  color: var(--xo-text-placeholder);
}

.form-row-time {
  margin-left: 4rpx;
}

.form-row-arrow {
  color: var(--xo-text-secondary);
  font-size: 34rpx;
}

.form-row-note-group {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  column-gap: 14rpx;
}

.form-row-note-input {
  min-width: 0;
  flex: 1;
  text-align: right;
  font-size: $font-lg;
  color: #7A84A1;
}

.form-row-note-count {
  flex-shrink: 0;
  font-size: $font-sm;
  color: var(--xo-text-placeholder);
}

.image-row-content {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  column-gap: 14rpx;
}

.image-preview,
.image-placeholder {
  width: 96rpx;
  height: 96rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx dashed var(--xo-border-color);
  background: rgba(247, 250, 255, 0.9);
}

.image-placeholder-icon {
  color: var(--xo-text-placeholder);
  font-size: 32rpx;
}

.image-camera-icon {
  color: #707070;
  font-size: 40rpx;
}

.keyboard-panel {
  padding: 16rpx 24rpx calc(env(safe-area-inset-bottom, 0px) + 18rpx);
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12rpx;
  background: rgba(243, 247, 252, 0.96);
}

.keyboard-key {
  height: 88rpx;
  border-radius: 22rpx;
  background: var(--xo-card-bg);
  box-shadow: var(--xo-component-card-shadow);
  display: flex;
  align-items: center;
  justify-content: center;
}

.keyboard-key.is-primary {
  background: var(--xo-gradient-button-primary);
}

.keyboard-key.is-blank {
  background: rgba(255, 255, 255, 0.42);
  box-shadow: none;
}

.keyboard-key-text {
  font-size: 42rpx;
  color: var(--xo-text-primary);
  font-weight: 500;
}

.keyboard-key.is-primary .keyboard-key-text {
  font-size: $font-xl;
  color: var(--xo-white);
}

.keyboard-key.is-blank .keyboard-key-text {
  color: transparent;
}

.picker-overlay {
  display: flex;
  align-items: flex-end;
}

.picker-sheet {
  width: 100%;
  max-height: 60vh;
  background: var(--xo-component-card-bg);
  border-radius: var(--xo-radius-xl) var(--xo-radius-xl) 0 0;
}

.picker-title {
  text-align: center;
  padding: 28rpx;
  border-bottom: 1rpx solid var(--xo-border-color);
  font-size: $font-lg;
  font-weight: 600;
  color: var(--xo-text-primary);
}

.picker-list {
  max-height: 50vh;
}

.picker-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid var(--xo-border-color);
  font-size: $font-md;
  color: var(--xo-text-primary);
}

.picker-balance {
  font-size: $font-sm;
  color: var(--xo-text-secondary);
}

.empty-state {
  padding: 40rpx 0 12rpx;
  text-align: center;
  color: var(--xo-text-secondary);
  font-size: $font-sm;
}
</style>
