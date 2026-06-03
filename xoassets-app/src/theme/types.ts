/* 主题配置类型：集中管理颜色、背景、图标、圆角、阴影、字体和间距。 */

export type ThemeName = 'classic-blue' | 'tech-dark' | 'cartoon-soft'

export type ThemeIcon =
  | { type: 'image'; src: string }
  | { type: 'text'; value: string }
  | { type: 'class'; className: string }

export interface ThemeIconPair {
  normal: ThemeIcon
  active: ThemeIcon
}

export interface ThemeConfig {
  name: ThemeName
  label: string
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
      variants: Record<'primary' | 'secondary' | 'success' | 'danger' | 'purple', {
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
    categoryFallback: Record<'INCOME' | 'EXPENSE' | 'TRANSFER', ThemeIcon>
    investmentActions: Record<'buy' | 'sell' | 'convert' | 'refresh', ThemeIcon>
    quickActions: Record<'record' | 'transfer' | 'invest' | 'budget', ThemeIcon>
  }
  backgrounds: {
    page: string
    homeAssetCard: string
    investmentSummaryCard: string
    mineProfileCard: string
    loginPage: string
  }
}
