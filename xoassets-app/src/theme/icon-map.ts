/* 主题图标映射：只暴露语义 key，页面不直接引用图片路径。 */
import type { ThemeConfig, ThemeIcon, ThemeIconPair, ThemeName } from './types'

import chartPie from '@/assets/themes/classic-blue/icons/chart-pie.svg'
import chartTrend from '@/assets/themes/classic-blue/icons/chart-trend.svg'
import commonAdd from '@/assets/themes/classic-blue/icons/common-add.svg'
import commonArrowRight from '@/assets/themes/classic-blue/icons/common-arrow-right.svg'
import commonBack from '@/assets/themes/classic-blue/icons/common-back.svg'
import commonCalendar from '@/assets/themes/classic-blue/icons/common-calendar.svg'
import commonCamera from '@/assets/themes/classic-blue/icons/common-camera.svg'
import commonConvert from '@/assets/themes/classic-blue/icons/common-convert.svg'
import commonDelete from '@/assets/themes/classic-blue/icons/common-delete.svg'
import commonEdit from '@/assets/themes/classic-blue/icons/common-edit.svg'
import commonExport from '@/assets/themes/classic-blue/icons/common-export.svg'
import commonEye from '@/assets/themes/classic-blue/icons/common-eye.svg'
import commonFilter from '@/assets/themes/classic-blue/icons/common-filter.svg'
import commonMore from '@/assets/themes/classic-blue/icons/common-more.svg'
import commonRefresh from '@/assets/themes/classic-blue/icons/common-refresh.svg'
import commonSettings from '@/assets/themes/classic-blue/icons/common-settings.svg'
import commonShield from '@/assets/themes/classic-blue/icons/common-shield.svg'
import commonTag from '@/assets/themes/classic-blue/icons/common-tag.svg'
import commonTime from '@/assets/themes/classic-blue/icons/common-time.svg'
import categoryBills from '@/assets/themes/classic-blue/icons/category-bills.svg'
import categoryBonus from '@/assets/themes/classic-blue/icons/category-bonus.svg'
import categoryDining from '@/assets/themes/classic-blue/icons/category-dining.svg'
import categoryEducation from '@/assets/themes/classic-blue/icons/category-education.svg'
import categoryEntertainment from '@/assets/themes/classic-blue/icons/category-entertainment.svg'
import categoryMedical from '@/assets/themes/classic-blue/icons/category-medical.svg'
import categoryOther from '@/assets/themes/classic-blue/icons/category-other.svg'
import categoryRefund from '@/assets/themes/classic-blue/icons/category-refund.svg'
import categorySalary from '@/assets/themes/classic-blue/icons/category-salary.svg'
import categoryShopping from '@/assets/themes/classic-blue/icons/category-shopping.svg'
import categoryTransfer from '@/assets/themes/classic-blue/icons/category-transfer.svg'
import categoryTransit from '@/assets/themes/classic-blue/icons/category-transit.svg'
import homeBalance from '@/assets/themes/classic-blue/icons/home-balance.svg'
import homeBudget from '@/assets/themes/classic-blue/icons/home-budget.svg'
import homeExpense from '@/assets/themes/classic-blue/icons/home-expense.svg'
import homeGoal from '@/assets/themes/classic-blue/icons/home-goal.svg'
import homeIncome from '@/assets/themes/classic-blue/icons/home-income.svg'
import homeNotification from '@/assets/themes/classic-blue/icons/home-notification.svg'
import homeSearch from '@/assets/themes/classic-blue/icons/home-search.svg'
import quickBudget from '@/assets/themes/classic-blue/icons/quick-budget.svg'
import quickInvest from '@/assets/themes/classic-blue/icons/quick-invest.svg'
import quickRecord from '@/assets/themes/classic-blue/icons/quick-record.svg'
import quickTransfer from '@/assets/themes/classic-blue/icons/quick-transfer.svg'
import reportAi from '@/assets/themes/classic-blue/icons/report-ai.svg'
import reportDownload from '@/assets/themes/classic-blue/icons/report-download.svg'

type ThemeIconMap = ThemeConfig['icons']

const image = (src: string): ThemeIcon => ({ type: 'image', src })

function imagePair(src: string): ThemeIconPair {
  return {
    normal: image(src),
    active: image(src)
  }
}

const tabBar: ThemeIconMap['tabBar'] = {
  home: imagePair('/static/tabbar/tab-home.png'),
  record: imagePair('/static/tabbar/tab-record.png'),
  accounts: imagePair('/static/tabbar/tab-account.png'),
  investments: imagePair('/static/tabbar/tab-invest.png'),
  budget: imagePair(homeBudget),
  mine: imagePair('/static/tabbar/tab-mine.png'),
  add: imagePair('/static/tabbar/tab-record.png')
}

const home: ThemeIconMap['home'] = {
  search: image(homeSearch),
  notification: image(homeNotification),
  more: image(commonMore),
  assetAnalysis: image(commonArrowRight),
  notice: image(homeNotification),
  analysis: image(commonArrowRight),
  eye: image(commonEye),
  eyeOff: image(commonEye),
  income: image(homeIncome),
  expense: image(homeExpense),
  balance: image(homeBalance),
  trend: image(chartTrend),
  budget: image(homeBudget),
  goal: image(homeGoal)
}

const quickActions: ThemeIconMap['quickActions'] = {
  record: image(quickRecord),
  transfer: image(quickTransfer),
  invest: image(quickInvest),
  budget: image(quickBudget)
}

const common: ThemeIconMap['common'] = {
  back: image(commonBack),
  more: image(commonMore),
  refresh: image(commonRefresh),
  filter: image(commonFilter),
  settings: image(commonSettings),
  calendar: image(commonCalendar),
  time: image(commonTime),
  camera: image(commonCamera),
  eye: image(commonEye),
  add: image(commonAdd),
  delete: image(commonDelete),
  edit: image(commonEdit),
  arrowRight: image(commonArrowRight),
  convert: image(commonConvert)
}

const homeStats: ThemeIconMap['homeStats'] = {
  income: home.income,
  expense: home.expense,
  balance: home.balance
}

const recentActivities: ThemeIconMap['recentActivities'] = {
  income: home.income,
  expense: home.expense,
  transfer: quickActions.transfer,
  investment: quickActions.invest,
  refund: common.refresh
}

const categoryFallback: ThemeIconMap['categoryFallback'] = {
  INCOME: home.income,
  EXPENSE: home.expense,
  TRANSFER: quickActions.transfer
}

const category: ThemeIconMap['category'] = {
  dining: image(categoryDining),
  transit: image(categoryTransit),
  shopping: image(categoryShopping),
  entertainment: image(categoryEntertainment),
  medical: image(categoryMedical),
  bills: image(categoryBills),
  education: image(categoryEducation),
  other: image(categoryOther),
  salary: image(categorySalary),
  bonus: image(categoryBonus),
  refund: image(categoryRefund),
  transfer: image(categoryTransfer)
}

const investmentActions: ThemeIconMap['investmentActions'] = {
  buy: common.add,
  sell: image(commonExport),
  convert: quickActions.transfer,
  refresh: common.refresh
}

const menu: ThemeIconMap['menu'] = {
  categories: image(commonTag),
  budgets: quickActions.budget,
  goals: home.goal,
  reports: image(reportAi),
  logout: common.back,
  search: home.search,
  filter: common.filter
}

const chartIcons: ThemeIconMap['chartIcons'] = {
  trend: image(chartTrend),
  pie: image(chartPie)
}

const reports: ThemeIconMap['reports'] = {
  download: image(reportDownload),
  ai: image(reportAi)
}

function createIconMap(): ThemeIconMap {
  return {
    tabBar,
    menu,
    home,
    homeStats,
    recentActivities,
    category,
    categoryFallback,
    investmentActions,
    quickActions,
    common,
    chartIcons,
    reports
  }
}

export const themeIconMap: Record<ThemeName, ThemeIconMap> = {
  'classic-blue': createIconMap(),
  'tech-dark': createIconMap(),
  'cartoon-soft': createIconMap()
}
