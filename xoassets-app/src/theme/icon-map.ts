/* 主题图标映射：只暴露语义 key，页面不直接引用图片路径。 */
import type { ThemeConfig, ThemeIcon, ThemeIconPair, ThemeName } from './types'

import accountAlipay from '@/assets/themes/classic-blue/icons/account-alipay.svg'
import accountBankCard from '@/assets/themes/classic-blue/icons/account-bank-card.svg'
import accountCash from '@/assets/themes/classic-blue/icons/account-cash.svg'
import accountCreditCard from '@/assets/themes/classic-blue/icons/account-credit-card.svg'
import accountWechat from '@/assets/themes/classic-blue/icons/account-wechat.svg'
import accountWallet from '@/assets/themes/classic-blue/icons/account-wallet.svg'
import actionConvert from '@/assets/themes/classic-blue/icons/action-convert.svg'
import actionDeposit from '@/assets/themes/classic-blue/icons/action-deposit.svg'
import actionWithdraw from '@/assets/themes/classic-blue/icons/action-withdraw.svg'

import chartAnalysis from '@/assets/themes/classic-blue/icons/chart-analysis.svg'
import chartPie from '@/assets/themes/classic-blue/icons/chart-pie.svg'
import chartTrend from '@/assets/themes/classic-blue/icons/chart-trend.svg'
import commonAdd from '@/assets/themes/classic-blue/icons/common-add.svg'
import commonAddCircle from '@/assets/themes/classic-blue/icons/common-add-circle.svg'
import commonAlbum from '@/assets/themes/classic-blue/icons/common-album.svg'
import commonArrowRight from '@/assets/themes/classic-blue/icons/common-arrow-right.svg'
import commonBack from '@/assets/themes/classic-blue/icons/common-back.svg'
import commonCalendar from '@/assets/themes/classic-blue/icons/common-calendar.svg'
import commonCamera from '@/assets/themes/classic-blue/icons/common-camera.svg'
import commonConvert from '@/assets/themes/classic-blue/icons/common-convert.svg'
import commonDelete from '@/assets/themes/classic-blue/icons/common-delete.svg'
import commonEdit from '@/assets/themes/classic-blue/icons/common-edit.svg'
import commonExport from '@/assets/themes/classic-blue/icons/common-export.svg'
import commonEye from '@/assets/themes/classic-blue/icons/common-eye.svg'
import commonEyeOff from '@/assets/themes/classic-blue/icons/common-eye-off.svg'
import commonFilter from '@/assets/themes/classic-blue/icons/common-filter.svg'
import commonHelp from '@/assets/themes/classic-blue/icons/common-help.svg'
import commonImport from '@/assets/themes/classic-blue/icons/common-import.svg'
import commonLogout from '@/assets/themes/classic-blue/icons/common-logout.svg'
import commonMore from '@/assets/themes/classic-blue/icons/common-more.svg'
import commonRefresh from '@/assets/themes/classic-blue/icons/common-refresh.svg'
import commonSearch from '@/assets/themes/classic-blue/icons/common-search.svg'
import commonSettings from '@/assets/themes/classic-blue/icons/common-settings.svg'
import commonShield from '@/assets/themes/classic-blue/icons/common-shield.svg'
import commonTag from '@/assets/themes/classic-blue/icons/common-tag.svg'
import commonTime from '@/assets/themes/classic-blue/icons/common-time.svg'
import categoryBills from '@/assets/themes/classic-blue/icons/category-bills.svg'
import categoryBook from '@/assets/themes/classic-blue/icons/category-book.svg'
import categoryBonus from '@/assets/themes/classic-blue/icons/category-bonus.svg'
import categoryBus from '@/assets/themes/classic-blue/icons/category-bus.svg'
import categoryCar from '@/assets/themes/classic-blue/icons/category-car.svg'
import categoryCoffee from '@/assets/themes/classic-blue/icons/category-coffee.svg'
import categoryDining from '@/assets/themes/classic-blue/icons/category-dining.svg'
import categoryEducation from '@/assets/themes/classic-blue/icons/category-education.svg'
import categoryEntertainment from '@/assets/themes/classic-blue/icons/category-entertainment.svg'
import categoryGame from '@/assets/themes/classic-blue/icons/category-game.svg'
import categoryHotel from '@/assets/themes/classic-blue/icons/category-hotel.svg'
import categoryInternet from '@/assets/themes/classic-blue/icons/category-internet.svg'
import categoryMedical from '@/assets/themes/classic-blue/icons/category-medical.svg'
import categoryMovie from '@/assets/themes/classic-blue/icons/category-movie.svg'
import categoryOther from '@/assets/themes/classic-blue/icons/category-other.svg'
import categoryRefund from '@/assets/themes/classic-blue/icons/category-refund.svg'
import categorySalary from '@/assets/themes/classic-blue/icons/category-salary.svg'
import categoryShopping from '@/assets/themes/classic-blue/icons/category-shopping.svg'
import categorySubway from '@/assets/themes/classic-blue/icons/category-subway.svg'
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
import recordNotebook from '@/assets/themes/classic-blue/icons/record-notebook.svg'

type ThemeIconMap = ThemeConfig['icons']

const image = (src: string): ThemeIcon => ({ type: 'image', src })
const text = (value: string): ThemeIcon => ({ type: 'text', value })

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
  assetAnalysis: image(chartAnalysis),
  notice: image(homeNotification),
  analysis: image(chartAnalysis),
  eye: image(commonEye),
  eyeOff: image(commonEyeOff),
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
  search: image(homeSearch),
  settings: image(commonSettings),
  calendar: image(commonCalendar),
  time: image(commonTime),
  camera: image(commonCamera),
  album: image(commonAlbum),
  eye: image(commonEye),
  eyeOff: image(commonEyeOff),
  add: image(commonAdd),
  delete: image(commonDelete),
  edit: image(commonEdit),
  account: image(accountWallet),
  note: image(recordNotebook),
  arrowRight: image(commonArrowRight),
  convert: image(commonConvert),
  import: image(commonImport),
  export: image(commonExport),
  logout: image(commonLogout),
  help: image(commonHelp),
  shield: image(commonShield)
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
  refund: image(categoryRefund)
}

const categoryFallback: ThemeIconMap['categoryFallback'] = {
  INCOME: home.income,
  EXPENSE: home.expense,
  TRANSFER: quickActions.transfer
}

const accounts: ThemeIconMap['accounts'] = {
  search: image(commonSearch),
  add: image(commonAddCircle),
  eye: image(commonEye),
  arrowRight: image(commonArrowRight),
  bankCard: image(accountBankCard),
  creditCard: image(accountCreditCard),
  cash: image(accountCash),
  wallet: image(accountWallet),
  alipay: image(accountAlipay),
  wechat: image(accountWechat),
  cmb: text('招'),
  icbc: text('工'),
  ccb: text('建'),
  summary: image(accountWallet),
  distribution: image(chartPie)
}

const category: ThemeIconMap['category'] = {
  dining: image(categoryDining),
  transit: image(categoryTransit),
  bus: image(categoryBus),
  car: image(categoryCar),
  subway: image(categorySubway),
  shopping: image(categoryShopping),
  entertainment: image(categoryEntertainment),
  movie: image(categoryMovie),
  game: image(categoryGame),
  medical: image(categoryMedical),
  bills: image(categoryBills),
  education: image(categoryEducation),
  book: image(categoryBook),
  coffee: image(categoryCoffee),
  hotel: image(categoryHotel),
  internet: image(categoryInternet),
  other: image(categoryOther),
  salary: image(categorySalary),
  bonus: image(categoryBonus),
  refund: image(categoryRefund),
  transfer: image(categoryTransfer)
}

const investmentActions: ThemeIconMap['investmentActions'] = {
  buy: image(actionDeposit),
  sell: image(actionWithdraw),
  convert: image(actionConvert),
  refresh: common.refresh
}

const menu: ThemeIconMap['menu'] = {
  categories: image(commonTag),
  budgets: quickActions.budget,
  goals: home.goal,
  reports: image(reportAi),
  logout: image(commonLogout),
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
    accounts,
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
