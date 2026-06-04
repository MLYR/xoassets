/* tech-dark：预留科技深色主题，先保证 token 完整。 */
import type { ThemeConfig } from '../types'
import { themeIconMap } from '../icon-map'

export const techDarkTheme: ThemeConfig = {
  name: 'tech-dark',
  label: '科技深色',
  assets: {
    icons: {
      root: 'src/assets/themes/classic-blue/icons'
    },
    backgrounds: {}
  },
  pageTokens: {
    home: {
      heroPadding: '20rpx',
      heroRadius: '34rpx',
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
    primary: '#44D7FF',
    primaryLight: '#7AE6FF',
    primaryDark: '#1599C2',
    secondary: '#8A7CFF',
    pageBg: '#0B1220',
    cardBg: '#121B2D',
    cardBgElevated: '#18243A',
    textPrimary: '#F5F8FF',
    textRegular: '#C7D0E2',
    textSecondary: '#7F8DA5',
    textPlaceholder: '#566176',
    border: '#22304A',
    positive: '#2BFFA1',
    negative: '#FF5F7E',
    transfer: '#44D7FF',
    warning: '#FFC857',
    info: '#7F8DA5',
    white: '#FFFFFF',
    mask: 'rgba(0, 0, 0, 0.62)',
    disabled: '#3A465C'
  },
  gradients: {
    pageHeader: 'linear-gradient(135deg, #111C31 0%, #0B1220 100%)',
    assetCard: 'linear-gradient(135deg, #122443 0%, #164C74 100%)',
    investmentCard: 'linear-gradient(135deg, #141E35 0%, #1D4E74 60%, #223A81 100%)',
    incomeCard: 'linear-gradient(135deg, #0B6B52 0%, #18C987 100%)',
    expenseCard: 'linear-gradient(135deg, #84213B 0%, #FF5F7E 100%)',
    transferCard: 'linear-gradient(135deg, #114B6A 0%, #44D7FF 100%)',
    buttonPrimary: 'linear-gradient(135deg, #1599C2 0%, #44D7FF 100%)',
    buttonDanger: 'linear-gradient(135deg, #84213B 0%, #FF5F7E 100%)'
  },
  radius: { sm: '12rpx', md: '18rpx', lg: '26rpx', xl: '34rpx', round: '999rpx' },
  shadow: {
    card: '0 10rpx 32rpx rgba(0, 0, 0, 0.26)',
    cardHover: '0 14rpx 42rpx rgba(0, 0, 0, 0.34)',
    floating: '0 18rpx 48rpx rgba(68, 215, 255, 0.24)',
    button: '0 12rpx 32rpx rgba(68, 215, 255, 0.22)'
  },
  spacing: { xs: '8rpx', sm: '16rpx', md: '24rpx', lg: '32rpx', xl: '48rpx' },
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
      background: '#0B1220',
      paddingX: '24rpx',
      paddingY: '24rpx',
      sectionGap: '24rpx'
    },
    button: {
      height: '88rpx',
      radius: '18rpx',
      primaryBg: 'linear-gradient(135deg, #1599C2 0%, #44D7FF 100%)',
      primaryText: '#07101D',
      outlineBorder: '#44D7FF',
      outlineText: '#44D7FF',
      disabledBg: '#3A465C',
      variants: {
        primary: {
          background: 'linear-gradient(135deg, #1599C2 0%, #44D7FF 100%)',
          text: '#07101D',
          shadow: '0 12rpx 32rpx rgba(68, 215, 255, 0.22)'
        },
        secondary: {
          background: '#172237',
          text: '#44D7FF',
          border: '#22304A'
        },
        success: {
          background: 'linear-gradient(135deg, #0B6B52 0%, #18C987 100%)',
          text: '#F5F8FF',
          shadow: '0 12rpx 26rpx rgba(24, 201, 135, 0.22)'
        },
        danger: {
          background: 'linear-gradient(135deg, #84213B 0%, #FF5F7E 100%)',
          text: '#F5F8FF',
          shadow: '0 12rpx 26rpx rgba(255, 95, 126, 0.22)'
        },
        purple: {
          background: 'linear-gradient(135deg, #5A54F9 0%, #8A7CFF 100%)',
          text: '#F5F8FF',
          shadow: '0 12rpx 26rpx rgba(138, 124, 255, 0.22)'
        },
        sell: {
          background: 'linear-gradient(135deg, #C86C1B 0%, #FF9F43 100%)',
          text: '#07101D',
          shadow: '0 12rpx 28rpx rgba(255, 159, 67, 0.24)'
        }
      }
    },
    card: {
      bg: '#121B2D',
      elevatedBg: '#18243A',
      radius: '26rpx',
      shadow: '0 10rpx 32rpx rgba(0, 0, 0, 0.26)'
    },
    tabBar: { color: '#7F8DA5', selectedColor: '#44D7FF', backgroundColor: '#0F1728', borderStyle: 'white' }
  },
  icons: themeIconMap['tech-dark'],
  charts: {
    investmentDistribution: {
      fund: '#4E84FF',
      stock: '#19C2C8',
      crypto: '#FF9F43',
      cash: '#32D48E',
      other: '#9B7CFF'
    },
    assetTrend: {
      line: '#44D7FF',
      fill: 'rgba(68, 215, 255, 0.16)',
      point: '#0B1220',
      grid: '#22304A'
    },
    budgetProgress: {
      used: '#44D7FF',
      remaining: '#2BFFA1',
      track: '#22304A'
    },
    accountDistribution: {
      bankCard: '#44D7FF',
      cash: '#FFC857',
      thirdParty: '#2BFFA1'
    }
  },
  backgrounds: {
    page: '#0B1220',
    homeAssetCard: 'linear-gradient(135deg, #122443 0%, #164C74 100%)',
    homeHeroCard: 'linear-gradient(135deg, #122443 0%, #164C74 58%, #223A81 100%)',
    homeGoalCard: 'linear-gradient(135deg, #121B2D 0%, #172A46 100%)',
    homeGoalCover: 'radial-gradient(circle at 28% 22%, rgba(255,255,255,0.34) 0 16%, transparent 17%), linear-gradient(150deg, #1E5A8A 0%, #253D68 42%, #1D7A73 43%, #183A43 64%, #8F6B3E 65%, #4F3420 100%)',
    investmentSummaryCard: 'linear-gradient(135deg, #141E35 0%, #1D4E74 60%, #223A81 100%)',
    mineProfileCard: 'linear-gradient(135deg, #121B2D 0%, #18243A 100%)',
    loginPage: 'linear-gradient(180deg, #111C31 0%, #0B1220 100%)',
    accountsPage: '#0B1220',
    accountsSummaryCard: 'linear-gradient(135deg, #121B2D 0%, #18243A 48%, #173B65 100%)'
  }
}
