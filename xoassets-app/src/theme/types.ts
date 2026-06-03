/* 主题配置类型：集中管理颜色、背景、图标、圆角、阴影、字体和间距。 */

export type ThemeName = 'classic-blue' | 'tech-dark' | 'cartoon-soft'

export type ButtonVariant = 'primary' | 'secondary' | 'success' | 'danger' | 'purple' | 'sell'

export type ThemeIcon =
  | { type: 'image'; src: string }
  | { type: 'text'; value: string }
  | { type: 'class'; className: string }

export interface ThemeIconPair {
  normal: ThemeIcon
  active: ThemeIcon
}

export interface ThemeAssets {
  icons: Record<string, string>
  backgrounds: Record<string, string>
}

export interface ThemePageTokens {
  home: {
    heroPadding: string
    heroRadius: string
    statGridGap: string
    chartHeight: string
    progressHeight: string
    quickActionGap: string
    activityIconSize: string
  }
  investments: {
    summaryCompareLabelWidth: string
    summaryCompareGap: string
    summaryCompareRowGap: string
    actionCapsuleHeight: string
    actionCapsuleGap: string
    holdingGridTemplate: string
  }
}

export interface ThemeConfig {
  name: ThemeName
  label: string
  // assets/pageTokens 作为主题扩展层，避免页面级素材和布局口径散落在单页样式中。
  assets: ThemeAssets
  pageTokens: ThemePageTokens
  colors: {
    primary: string
    primaryLight: string
    primaryDark: string
    secondary: string
    pageBg: string
    cardBg: string
    cardBgElevated: string
    textPrimary: string
    textRegular: string
    textSecondary: string
    textPlaceholder: string
    border: string
    positive: string
    negative: string
    transfer: string
    warning: string
    info: string
    white: string
    mask: string
    disabled: string
  }
  gradients: {
    pageHeader: string
    assetCard: string
    investmentCard: string
    incomeCard: string
    expenseCard: string
    transferCard: string
    buttonPrimary: string
    buttonDanger: string
  }
  radius: {
    sm: string
    md: string
    lg: string
    xl: string
    round: string
  }
  shadow: {
    card: string
    cardHover: string
    floating: string
    button: string
  }
  spacing: {
    xs: string
    sm: string
    md: string
    lg: string
    xl: string
  }
  typography: {
    fontFamily: string
    fontSizeXs: string
    fontSizeSm: string
    fontSizeMd: string
    fontSizeLg: string
    fontSizeXl: string
    fontSizeXxl: string
    amountSm: string
    amountMd: string
    amountLg: string
    amountHuge: string
  }
  components: {
    page: {
      background: string
      paddingX: string
      paddingY: string
      sectionGap: string
    }
    button: {
      height: string
      radius: string
      primaryBg: string
      primaryText: string
      outlineBorder: string
      outlineText: string
      disabledBg: string
      variants: Record<ButtonVariant, {
        background: string
        text: string
        shadow?: string
        border?: string
      }>
    }
    card: {
      bg: string
      elevatedBg: string
      radius: string
      shadow: string
    }
    tabBar: {
      color: string
      selectedColor: string
      backgroundColor: string
      borderStyle: 'black' | 'white'
    }
  }
  icons: {
    tabBar: Record<'home' | 'add' | 'accounts' | 'investments' | 'mine', ThemeIconPair>
    menu: Record<string, ThemeIcon>
    home: Record<'search' | 'notice' | 'analysis' | 'eye' | 'eyeOff' | 'trend' | 'budget' | 'goal', ThemeIcon>
    homeStats: Record<'income' | 'expense' | 'balance', ThemeIcon>
    recentActivities: Record<'income' | 'expense' | 'transfer' | 'refund', ThemeIcon>
    category: Record<string, ThemeIcon>
    categoryFallback: Record<'INCOME' | 'EXPENSE' | 'TRANSFER', ThemeIcon>
    investmentActions: Record<'buy' | 'sell' | 'convert' | 'refresh', ThemeIcon>
    quickActions: Record<'record' | 'transfer' | 'invest' | 'budget', ThemeIcon>
  }
  charts: {
    investmentDistribution: Record<'fund' | 'stock' | 'crypto' | 'cash' | 'other', string>
    assetTrend: {
      line: string
      fill: string
      point: string
      grid: string
    }
    budgetProgress: {
      used: string
      remaining: string
      track: string
    }
  }
  backgrounds: {
    page: string
    homeAssetCard: string
    homeHeroCard: string
    homeGoalCard: string
    homeGoalCover: string
    investmentSummaryCard: string
    mineProfileCard: string
    loginPage: string
  }
}
