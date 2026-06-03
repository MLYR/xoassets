/* cartoon-soft：预留柔和卡通主题，适合后续更轻松的个人财务风格。 */
import type { ThemeConfig } from '../types'
import { themeIconMap } from '../icon-map'

export const cartoonSoftTheme: ThemeConfig = {
  name: 'cartoon-soft',
  label: '柔和卡通',
  assets: {
    icons: {},
    backgrounds: {}
  },
  pageTokens: {
    investments: {
      summaryCompareLabelWidth: '88rpx',
      summaryCompareGap: '16rpx',
      summaryCompareRowGap: '12rpx',
      actionCapsuleHeight: '90rpx',
      actionCapsuleGap: '18rpx',
      holdingGridTemplate: 'minmax(0, 1.1fr) minmax(0, 1fr) minmax(0, 0.82fr)'
    }
  },
  colors: {
    primary: '#6C8CFF',
    primaryLight: '#AFC0FF',
    primaryDark: '#496CE8',
    secondary: '#FFB15F',
    pageBg: '#FFF8F0',
    cardBg: '#FFFFFF',
    cardBgElevated: '#FFFDF9',
    textPrimary: '#2D2A32',
    textRegular: '#6D6575',
    textSecondary: '#9B91A8',
    textPlaceholder: '#C9BFCE',
    border: '#F1E7DD',
    positive: '#22C983',
    negative: '#FF6B7C',
    transfer: '#6C8CFF',
    warning: '#FFB15F',
    info: '#9B91A8',
    white: '#FFFFFF',
    mask: 'rgba(45, 42, 50, 0.36)',
    disabled: '#D6CEDD'
  },
  gradients: {
    pageHeader: 'linear-gradient(135deg, #6C8CFF 0%, #AFC0FF 100%)',
    assetCard: 'linear-gradient(135deg, #6C8CFF 0%, #8BA3FF 58%, #B7C6FF 100%)',
    investmentCard: 'linear-gradient(135deg, #7C8DFF 0%, #9B7CFF 100%)',
    incomeCard: 'linear-gradient(135deg, #22C983 0%, #8BE8BC 100%)',
    expenseCard: 'linear-gradient(135deg, #FF9AA7 0%, #FF6B7C 100%)',
    transferCard: 'linear-gradient(135deg, #6C8CFF 0%, #AFC0FF 100%)',
    buttonPrimary: 'linear-gradient(135deg, #6C8CFF 0%, #496CE8 100%)',
    buttonDanger: 'linear-gradient(135deg, #FF9AA7 0%, #FF6B7C 100%)'
  },
  radius: { sm: '16rpx', md: '22rpx', lg: '30rpx', xl: '40rpx', round: '999rpx' },
  shadow: {
    card: '0 10rpx 28rpx rgba(108, 140, 255, 0.10)',
    cardHover: '0 14rpx 38rpx rgba(108, 140, 255, 0.16)',
    floating: '0 16rpx 42rpx rgba(108, 140, 255, 0.24)',
    button: '0 12rpx 28rpx rgba(108, 140, 255, 0.22)'
  },
  spacing: { xs: '8rpx', sm: '18rpx', md: '26rpx', lg: '34rpx', xl: '50rpx' },
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
      background: '#FFF8F0',
      paddingX: '24rpx',
      paddingY: '24rpx',
      sectionGap: '26rpx'
    },
    button: {
      height: '90rpx',
      radius: '24rpx',
      primaryBg: 'linear-gradient(135deg, #6C8CFF 0%, #496CE8 100%)',
      primaryText: '#FFFFFF',
      outlineBorder: '#6C8CFF',
      outlineText: '#496CE8',
      disabledBg: '#D6CEDD',
      variants: {
        primary: {
          background: 'linear-gradient(135deg, #6C8CFF 0%, #496CE8 100%)',
          text: '#FFFFFF',
          shadow: '0 12rpx 28rpx rgba(108, 140, 255, 0.22)'
        },
        secondary: {
          background: '#FFF1E1',
          text: '#6C8CFF',
          border: '#F1E7DD'
        },
        success: {
          background: 'linear-gradient(135deg, #22C983 0%, #8BE8BC 100%)',
          text: '#FFFFFF',
          shadow: '0 12rpx 24rpx rgba(34, 201, 131, 0.22)'
        },
        danger: {
          background: 'linear-gradient(135deg, #FF9AA7 0%, #FF6B7C 100%)',
          text: '#FFFFFF',
          shadow: '0 12rpx 24rpx rgba(255, 107, 124, 0.22)'
        },
        purple: {
          background: 'linear-gradient(135deg, #9B7CFF 0%, #7C8DFF 100%)',
          text: '#FFFFFF',
          shadow: '0 12rpx 24rpx rgba(155, 124, 255, 0.22)'
        },
        sell: {
          background: 'linear-gradient(135deg, #FFC06F 0%, #FF8C42 100%)',
          text: '#FFFFFF',
          shadow: '0 12rpx 26rpx rgba(255, 140, 66, 0.24)'
        }
      }
    },
    card: {
      bg: '#FFFFFF',
      elevatedBg: '#FFFDF9',
      radius: '30rpx',
      shadow: '0 10rpx 28rpx rgba(108, 140, 255, 0.10)'
    },
    tabBar: { color: '#8F849C', selectedColor: '#6C8CFF', backgroundColor: '#FFFFFF', borderStyle: 'black' }
  },
  icons: themeIconMap['cartoon-soft'],
  charts: {
    investmentDistribution: {
      fund: '#6C8CFF',
      stock: '#32C7D2',
      crypto: '#FFA14E',
      cash: '#4FD47C',
      other: '#B47CFF'
    }
  },
  backgrounds: {
    page: '#FFF8F0',
    homeAssetCard: 'linear-gradient(135deg, #6C8CFF 0%, #8BA3FF 58%, #B7C6FF 100%)',
    investmentSummaryCard: 'linear-gradient(135deg, #7C8DFF 0%, #9B7CFF 100%)',
    mineProfileCard: 'linear-gradient(135deg, #FFFFFF 0%, #FFF1E1 100%)',
    loginPage: 'linear-gradient(180deg, #6C8CFF 0%, #AFC0FF 40%, #FFF8F0 40%)'
  }
}
