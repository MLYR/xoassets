/* classic-blue：对齐当前原型图和 Web 端的浅蓝金融 SaaS 风格。 */
import type { ThemeConfig } from '../types'
import { themeIconMap } from '../icon-map'

export const classicBlueTheme: ThemeConfig = {
  name: 'classic-blue',
  label: '经典蓝白',
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
    }
  },
  icons: themeIconMap['classic-blue'],
  backgrounds: {
    page: '#F3F7FC',
    homeAssetCard: 'linear-gradient(135deg, #2F7BFF 0%, #1F74FF 54%, #3E8CFF 100%)',
    investmentSummaryCard: 'linear-gradient(135deg, #2F7BFF 0%, #2787FF 62%, #5CA9FF 100%)',
    mineProfileCard: 'linear-gradient(135deg, #F9FBFF 0%, #FFFFFF 100%)',
    loginPage: 'linear-gradient(180deg, #2F7BFF 0%, #6BA5E7 40%, #F3F7FC 40%)'
  }
}
