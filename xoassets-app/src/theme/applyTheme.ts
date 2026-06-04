/* 主题应用器：把 TS 主题配置同步到 CSS 变量和 uni 原生导航/TabBar。 */
import type { ThemeConfig } from './types'

const TAB_ITEMS: Array<keyof ThemeConfig['icons']['tabBar']> = [
  'home',
  'record',
  'accounts',
  'investments',
  'mine'
]

function setCssVar(name: string, value: string): void {
  // H5 运行时通过 CSS 变量完成主题切换；App/小程序保留 pages.json 默认值作为兜底。
  if (typeof document === 'undefined') return
  document.documentElement.style.setProperty(name, value)
}

function hexToRgba(hex: string, alpha: number): string {
  const normalized = hex.replace('#', '')
  if (normalized.length !== 6) return hex
  const r = Number.parseInt(normalized.slice(0, 2), 16)
  const g = Number.parseInt(normalized.slice(2, 4), 16)
  const b = Number.parseInt(normalized.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

export function applyTheme(theme: ThemeConfig): void {
  setCssVariables(theme)
  applyUniChrome(theme)
}

export function setCssVariables(theme: ThemeConfig): void {
  const { colors, gradients, radius, shadow, spacing, typography, components, backgrounds, pageTokens } = theme

  setCssVar('--xo-primary', colors.primary)
  setCssVar('--xo-primary-light', colors.primaryLight)
  setCssVar('--xo-primary-dark', colors.primaryDark)
  setCssVar('--xo-secondary', colors.secondary)
  setCssVar('--xo-page-bg', colors.pageBg)
  setCssVar('--xo-card-bg', colors.cardBg)
  setCssVar('--xo-card-bg-elevated', colors.cardBgElevated)
  setCssVar('--xo-text-primary', colors.textPrimary)
  setCssVar('--xo-text-regular', colors.textRegular)
  setCssVar('--xo-text-secondary', colors.textSecondary)
  setCssVar('--xo-text-placeholder', colors.textPlaceholder)
  setCssVar('--xo-border-color', colors.border)
  setCssVar('--xo-positive', colors.positive)
  setCssVar('--xo-negative', colors.negative)
  setCssVar('--xo-transfer', colors.transfer)
  setCssVar('--xo-warning', colors.warning)
  setCssVar('--xo-info', colors.info)
  setCssVar('--xo-white', colors.white)
  setCssVar('--xo-mask', colors.mask)
  setCssVar('--xo-disabled', colors.disabled)
  setCssVar('--xo-primary-soft', hexToRgba(colors.primary, 0.1))
  setCssVar('--xo-positive-soft', hexToRgba(colors.positive, 0.1))
  setCssVar('--xo-negative-soft', hexToRgba(colors.negative, 0.1))
  setCssVar('--xo-transfer-soft', hexToRgba(colors.transfer, 0.1))
  setCssVar('--xo-white-25', hexToRgba(colors.white, 0.25))
  setCssVar('--xo-white-75', hexToRgba(colors.white, 0.75))
  setCssVar('--xo-white-80', hexToRgba(colors.white, 0.8))
  setCssVar('--xo-white-85', hexToRgba(colors.white, 0.85))

  setCssVar('--xo-gradient-page-header', gradients.pageHeader)
  setCssVar('--xo-gradient-asset-card', gradients.assetCard)
  setCssVar('--xo-gradient-investment-card', gradients.investmentCard)
  setCssVar('--xo-gradient-income-card', gradients.incomeCard)
  setCssVar('--xo-gradient-expense-card', gradients.expenseCard)
  setCssVar('--xo-gradient-transfer-card', gradients.transferCard)
  setCssVar('--xo-gradient-button-primary', gradients.buttonPrimary)
  setCssVar('--xo-gradient-button-danger', gradients.buttonDanger)

  setCssVar('--xo-radius-sm', radius.sm)
  setCssVar('--xo-radius-md', radius.md)
  setCssVar('--xo-radius-lg', radius.lg)
  setCssVar('--xo-radius-xl', radius.xl)
  setCssVar('--xo-radius-round', radius.round)
  setCssVar('--xo-shadow-card', shadow.card)
  setCssVar('--xo-shadow-card-hover', shadow.cardHover)
  setCssVar('--xo-shadow-floating', shadow.floating)
  setCssVar('--xo-shadow-button', shadow.button)

  setCssVar('--xo-spacing-xs', spacing.xs)
  setCssVar('--xo-spacing-sm', spacing.sm)
  setCssVar('--xo-spacing-md', spacing.md)
  setCssVar('--xo-spacing-lg', spacing.lg)
  setCssVar('--xo-spacing-xl', spacing.xl)

  setCssVar('--xo-font-family', typography.fontFamily)
  setCssVar('--xo-font-xs', typography.fontSizeXs)
  setCssVar('--xo-font-sm', typography.fontSizeSm)
  setCssVar('--xo-font-md', typography.fontSizeMd)
  setCssVar('--xo-font-lg', typography.fontSizeLg)
  setCssVar('--xo-font-xl', typography.fontSizeXl)
  setCssVar('--xo-font-xxl', typography.fontSizeXxl)
  setCssVar('--xo-amount-sm', typography.amountSm)
  setCssVar('--xo-amount-md', typography.amountMd)
  setCssVar('--xo-amount-lg', typography.amountLg)
  setCssVar('--xo-amount-huge', typography.amountHuge)

  setCssVar('--xo-button-height', components.button.height)
  setCssVar('--xo-button-radius', components.button.radius)
  setCssVar('--xo-button-primary-bg', components.button.primaryBg)
  setCssVar('--xo-button-primary-text', components.button.primaryText)
  setCssVar('--xo-button-outline-border', components.button.outlineBorder)
  setCssVar('--xo-button-outline-text', components.button.outlineText)
  setCssVar('--xo-button-disabled-bg', components.button.disabledBg)
  setCssVar('--xo-component-card-bg', components.card.bg)
  setCssVar('--xo-component-card-elevated-bg', components.card.elevatedBg)
  setCssVar('--xo-component-card-radius', components.card.radius)
  setCssVar('--xo-component-card-shadow', components.card.shadow)

  setCssVar('--xo-bg-page', backgrounds.page)
  setCssVar('--xo-bg-home-asset-card', backgrounds.homeAssetCard)
  setCssVar('--xo-bg-home-hero-card', backgrounds.homeHeroCard)
  setCssVar('--xo-bg-home-goal-card', backgrounds.homeGoalCard)
  setCssVar('--xo-bg-home-goal-cover', backgrounds.homeGoalCover)
  setCssVar('--xo-bg-investment-summary-card', backgrounds.investmentSummaryCard)
  setCssVar('--xo-bg-mine-profile-card', backgrounds.mineProfileCard)
  setCssVar('--xo-bg-login-page', backgrounds.loginPage)
  setCssVar('--xo-bg-accounts-page', backgrounds.accountsPage)
  setCssVar('--xo-bg-accounts-summary-card', backgrounds.accountsSummaryCard)

  // 首页 Dashboard 场景 token，承接原型卡片间距、图表高度和进度条尺寸。
  setCssVar('--xo-home-hero-padding', pageTokens.home.heroPadding)
  setCssVar('--xo-home-hero-radius', pageTokens.home.heroRadius)
  setCssVar('--xo-home-stat-grid-gap', pageTokens.home.statGridGap)
  setCssVar('--xo-home-chart-height', pageTokens.home.chartHeight)
  setCssVar('--xo-home-progress-height', pageTokens.home.progressHeight)
  setCssVar('--xo-home-quick-action-gap', pageTokens.home.quickActionGap)
  setCssVar('--xo-home-activity-icon-size', pageTokens.home.activityIconSize)

  // 投资页复刻原型所需的场景级尺寸，跟随主题切换而不是写死在页面中。
  setCssVar('--xo-invest-summary-label-width', pageTokens.investments.summaryCompareLabelWidth)
  setCssVar('--xo-invest-summary-gap', pageTokens.investments.summaryCompareGap)
  setCssVar('--xo-invest-summary-row-gap', pageTokens.investments.summaryCompareRowGap)
  setCssVar('--xo-invest-action-height', pageTokens.investments.actionCapsuleHeight)
  setCssVar('--xo-invest-action-gap', pageTokens.investments.actionCapsuleGap)
  setCssVar('--xo-invest-holding-grid', pageTokens.investments.holdingGridTemplate)

  // 账户页原型复刻场景 token，集中控制资产卡、分类栏和底部 CTA 尺寸。
  setCssVar('--xo-accounts-summary-card-min-height', pageTokens.accounts.summaryCardMinHeight)
  setCssVar('--xo-accounts-hero-illustration-size', pageTokens.accounts.heroIllustrationSize)
  setCssVar('--xo-accounts-category-tab-height', pageTokens.accounts.categoryTabHeight)
  setCssVar('--xo-accounts-row-min-height', pageTokens.accounts.accountRowMinHeight)
  setCssVar('--xo-accounts-distribution-bar-height', pageTokens.accounts.distributionBarHeight)
  setCssVar('--xo-accounts-bottom-action-height', pageTokens.accounts.bottomActionHeight)
}

export function applyUniChrome(theme: ThemeConfig): void {
  const { colors, components } = theme

  uni.setNavigationBarColor({
    frontColor: colors.white === '#FFFFFF' ? '#ffffff' : '#000000',
    backgroundColor: colors.primary
  })

  uni.setTabBarStyle({
    color: components.tabBar.color,
    selectedColor: components.tabBar.selectedColor,
    backgroundColor: components.tabBar.backgroundColor,
    borderStyle: components.tabBar.borderStyle
  })

  // 只有 image 类型图标才同步到原生 TabBar；text/class 类型作为 H5 和页面内 fallback。
  TAB_ITEMS.forEach((key, index) => {
    const icon = theme.icons.tabBar[key]
    if (icon.normal.type !== 'image' || icon.active.type !== 'image') return
    uni.setTabBarItem({
      index,
      iconPath: normalizeTabBarPath(icon.normal.src),
      selectedIconPath: normalizeTabBarPath(icon.active.src)
    })
  })
}

function normalizeTabBarPath(path: string): string {
  return path.startsWith('/') ? path.slice(1) : path
}
