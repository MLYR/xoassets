/* classic-blue：对齐当前原型图和 Web 端的浅蓝金融 SaaS 风格。 */
import type { ThemeConfig } from '../types'
import { themeIconMap } from '../icon-map'

export const classicBlueTheme: ThemeConfig = {
  name: 'classic-blue',
  label: '经典蓝白',
  assets: {
    icons: {
      root: 'src/assets/themes/classic-blue/icons'
    },
    backgrounds: {}
  },
  pageTokens: {
    home: {
      heroPadding: '20rpx',
      heroRadius: '32rpx',
      statGridGap: '24rpx',
      chartHeight: '154rpx',
      progressHeight: '8rpx',
      quickActionGap: '16rpx',
      activityIconSize: '64rpx'
    },
    investments: {
      summaryCompareLabelWidth: '104rpx',
      summaryCompareGap: '16rpx',
      summaryCompareRowGap: '10rpx',
      actionCapsuleHeight: '88rpx',
      actionCapsuleGap: '18rpx',
      holdingGridTemplate: 'minmax(0, 1.1fr) minmax(0, 1fr) minmax(0, 0.82fr)'
    },
    accounts: {
      summaryCardMinHeight: '270rpx',
      heroIllustrationSize: '220rpx',
      categoryTabHeight: '72rpx',
      accountRowMinHeight: '128rpx',
      distributionBarHeight: '18rpx',
      bottomActionHeight: '96rpx'
    }
  },
  colors: {
    primary: '#2F7BFF',
    primaryLight: '#6BA5E7',
    primaryDark: '#1F64E8',
    secondary: '#16C8A7',
    pageBg: '#F3F7FC',
    cardBg: '#FFFFFF',
    cardBgElevated: '#FFFFFF',
    textPrimary: '#161A22',
    textRegular: '#5F697A',
    textSecondary: '#8E99A8',
    textPlaceholder: '#C0C7D2',
    border: '#E8EEF6',
    positive: '#12C46B',
    negative: '#FF3B3F',
    profitPositive: '#FF3B3F',
    profitNegative: '#12C46B',
    transfer: '#2F7BFF',
    warning: '#FA9D2A',
    info: '#7C8798',
    white: '#FFFFFF',
    mask: 'rgba(0, 0, 0, 0.42)',
    disabled: '#C7CFDB'
  },
  gradients: {
    pageHeader: 'linear-gradient(135deg, #2F7BFF 0%, #5FA2FF 100%)',
    assetCard: 'linear-gradient(135deg, #2F7BFF 0%, #1F74FF 54%, #3E8CFF 100%)',
    investmentCard: 'linear-gradient(135deg, #2F7BFF 0%, #2787FF 62%, #5CA9FF 100%)',
    incomeCard: 'linear-gradient(135deg, #12C46B 0%, #42D889 100%)',
    expenseCard: 'linear-gradient(135deg, #FF6B6B 0%, #FF3B3F 100%)',
    transferCard: 'linear-gradient(135deg, #2F7BFF 0%, #5FA2FF 100%)',
    buttonPrimary: 'linear-gradient(135deg, #2F7BFF 0%, #1F64E8 100%)',
    buttonDanger: 'linear-gradient(135deg, #FF6B6B 0%, #FF3B3F 100%)'
  },
  radius: {
    sm: '12rpx',
    md: '16rpx',
    lg: '24rpx',
    xl: '32rpx',
    round: '999rpx'
  },
  shadow: {
    card: '0 8rpx 28rpx rgba(32, 88, 166, 0.08)',
    cardHover: '0 12rpx 36rpx rgba(32, 88, 166, 0.12)',
    floating: '0 16rpx 44rpx rgba(47, 123, 255, 0.28)',
    button: '0 12rpx 28rpx rgba(47, 123, 255, 0.24)'
  },
  spacing: {
    xs: '8rpx',
    sm: '16rpx',
    md: '24rpx',
    lg: '32rpx',
    xl: '48rpx'
  },
  typography: {
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
    fontSizeXs: '22rpx',
    fontSizeSm: '26rpx',
    fontSizeMd: '28rpx',
    fontSizeLg: '32rpx',
    fontSizeXl: '36rpx',
    fontSizeXxl: '44rpx',
    amountSm: '32rpx',
    amountMd: '40rpx',
    amountLg: '52rpx',
    amountHuge: '64rpx'
  },
  components: {
    page: {
      background: '#F3F7FC',
      paddingX: '24rpx',
      paddingY: '24rpx',
      sectionGap: '24rpx'
    },
    button: {
      height: '88rpx',
      radius: '16rpx',
      primaryBg: 'linear-gradient(135deg, #2F7BFF 0%, #1F64E8 100%)',
      primaryText: '#FFFFFF',
      outlineBorder: '#2F7BFF',
      outlineText: '#2F7BFF',
      disabledBg: '#C7CFDB',
      variants: {
        primary: {
          background: 'linear-gradient(135deg, #2F7BFF 0%, #1F64E8 100%)',
          text: '#FFFFFF',
          shadow: '0 12rpx 28rpx rgba(47, 123, 255, 0.24)'
        },
        secondary: {
          background: '#EFF5FF',
          text: '#2F7BFF',
          border: '#CFE0FF'
        },
        success: {
          background: 'linear-gradient(135deg, #12C46B 0%, #42D889 100%)',
          text: '#FFFFFF',
          shadow: '0 12rpx 24rpx rgba(18, 196, 107, 0.22)'
        },
        danger: {
          background: 'linear-gradient(135deg, #FF6B6B 0%, #FF3B3F 100%)',
          text: '#FFFFFF',
          shadow: '0 12rpx 24rpx rgba(255, 59, 63, 0.22)'
        },
        purple: {
          background: 'linear-gradient(135deg, #8E6BFF 0%, #6F42FF 100%)',
          text: '#FFFFFF',
          shadow: '0 12rpx 24rpx rgba(111, 66, 255, 0.22)'
        },
        sell: {
          background: 'linear-gradient(135deg, #FF9D47 0%, #FF6A3D 100%)',
          text: '#FFFFFF',
          shadow: '0 12rpx 28rpx rgba(255, 122, 69, 0.28)'
        }
      }
    },
    card: {
      bg: '#FFFFFF',
      elevatedBg: '#FFFFFF',
      radius: '24rpx',
      shadow: '0 8rpx 28rpx rgba(32, 88, 166, 0.08)'
    },
    tabBar: {
      color: '#6F7B8B',
      selectedColor: '#2F7BFF',
      backgroundColor: '#FFFFFF',
      borderStyle: 'black'
    },
    navBar: {
      height: '96rpx',
      background: 'rgba(243, 247, 252, 0.96)',
      textColor: '#161A22',
      iconColor: '#161A22',
      shadow: '0 8rpx 24rpx rgba(32, 88, 166, 0.06)',
      blur: '18rpx',
      zIndex: 20
    }
  },
  icons: themeIconMap['classic-blue'],
  charts: {
    investmentDistribution: {
      fund: '#2F7BFF',
      stock: '#19C2C8',
      crypto: '#FF8A34',
      cash: '#34C96F',
      other: '#8D4DFF'
    },
    assetTrend: {
      line: '#2F7BFF',
      fill: 'rgba(47, 123, 255, 0.14)',
      point: '#FFFFFF',
      grid: '#E8EEF6'
    },
    budgetProgress: {
      used: '#2F7BFF',
      remaining: '#12C46B',
      track: '#E8EEF6'
    },
    accountDistribution: {
      bankCard: '#2F7BFF',
      cash: '#FF991F',
      thirdParty: '#18C786'
    }
  },
  backgrounds: {
    page: '#F3F7FC',
    homeAssetCard: 'linear-gradient(135deg, #2F7BFF 0%, #1F74FF 54%, #3E8CFF 100%)',
    homeHeroCard: 'linear-gradient(135deg, #2F7BFF 0%, #297DFF 48%, #64B1FF 100%)',
    homeGoalCard: 'linear-gradient(135deg, #F8FBFF 0%, #EEF5FF 100%)',
    homeGoalCover: 'radial-gradient(circle at 28% 22%, rgba(255,255,255,0.8) 0 16%, transparent 17%), linear-gradient(150deg, #6FCBFF 0%, #B7E9FF 42%, #6ED5BE 43%, #DFF6EA 64%, #FFE1A8 65%, #F8C57C 100%)',
    investmentSummaryCard: 'linear-gradient(135deg, #2F7BFF 0%, #2787FF 62%, #5CA9FF 100%)',
    mineProfileCard: 'linear-gradient(135deg, #F9FBFF 0%, #FFFFFF 100%)',
    loginPage: 'linear-gradient(180deg, #2F7BFF 0%, #6BA5E7 40%, #F3F7FC 40%)',
    accountsPage: '#F7FAFE',
    accountsSummaryCard: 'linear-gradient(135deg, #FFFFFF 0%, #F6FAFF 45%, #EAF3FF 100%)'
  }
}
