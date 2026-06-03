/* 主题图标映射：集中维护各主题可复用的图标语义键。 */
import type { ThemeConfig, ThemeIcon, ThemeIconPair, ThemeName } from './types'

type ThemeIconMap = ThemeConfig['icons']

const sharedCategoryFallback: ThemeIconMap['categoryFallback'] = {
  INCOME: { type: 'text', value: '收' },
  EXPENSE: { type: 'text', value: '支' },
  TRANSFER: { type: 'text', value: '转' }
}

const sharedCategoryIcons: ThemeIconMap['category'] = {
  dining: { type: 'text', value: '餐' },
  transit: { type: 'text', value: '交' },
  shopping: { type: 'text', value: '购' },
  entertainment: { type: 'text', value: '娱' },
  medical: { type: 'text', value: '医' },
  bills: { type: 'text', value: '缴' },
  education: { type: 'text', value: '教' },
  other: { type: 'text', value: '其' },
  salary: { type: 'text', value: '薪' },
  bonus: { type: 'text', value: '奖' },
  refund: { type: 'text', value: '退' },
  transfer: { type: 'text', value: '转' }
}

const sharedInvestmentActions: ThemeIconMap['investmentActions'] = {
  buy: { type: 'text', value: '买' },
  sell: { type: 'text', value: '卖' },
  convert: { type: 'text', value: '换' },
  refresh: { type: 'text', value: '刷' }
}

const sharedQuickActions: ThemeIconMap['quickActions'] = {
  record: { type: 'text', value: '记' },
  transfer: { type: 'text', value: '转' },
  invest: { type: 'text', value: '投' },
  budget: { type: 'text', value: '预' }
}

function createTabIcon(text: string): ThemeIconPair {
  return {
    normal: { type: 'text', value: text },
    active: { type: 'text', value: text }
  }
}

function createMenuIcon(text: string): ThemeIcon {
  return { type: 'text', value: text }
}

export const themeIconMap: Record<ThemeName, ThemeIconMap> = {
  'classic-blue': {
    tabBar: {
      home: createTabIcon('⌂'),
      add: createTabIcon('▤'),
      accounts: createTabIcon('▣'),
      investments: createTabIcon('↗'),
      mine: {
        normal: { type: 'text', value: '○' },
        active: { type: 'text', value: '●' }
      }
    },
    menu: {
      categories: createMenuIcon('▦'),
      budgets: createMenuIcon('◔'),
      goals: createMenuIcon('◎'),
      reports: createMenuIcon('▥'),
      logout: createMenuIcon('↪'),
      search: createMenuIcon('⌕'),
      filter: createMenuIcon('☰')
    },
    category: sharedCategoryIcons,
    categoryFallback: sharedCategoryFallback,
    investmentActions: sharedInvestmentActions,
    quickActions: sharedQuickActions
  },
  'tech-dark': {
    tabBar: {
      home: createTabIcon('⌂'),
      add: createTabIcon('▤'),
      accounts: createTabIcon('▣'),
      investments: createTabIcon('↗'),
      mine: {
        normal: { type: 'text', value: '○' },
        active: { type: 'text', value: '●' }
      }
    },
    menu: {
      categories: createMenuIcon('▦'),
      budgets: createMenuIcon('◔'),
      goals: createMenuIcon('◎'),
      reports: createMenuIcon('▥'),
      logout: createMenuIcon('↪'),
      search: createMenuIcon('⌕'),
      filter: createMenuIcon('☰')
    },
    category: sharedCategoryIcons,
    categoryFallback: sharedCategoryFallback,
    investmentActions: sharedInvestmentActions,
    quickActions: sharedQuickActions
  },
  'cartoon-soft': {
    tabBar: {
      home: createTabIcon('⌂'),
      add: createTabIcon('▤'),
      accounts: createTabIcon('▣'),
      investments: createTabIcon('↗'),
      mine: {
        normal: { type: 'text', value: '○' },
        active: { type: 'text', value: '●' }
      }
    },
    menu: {
      categories: createMenuIcon('▦'),
      budgets: createMenuIcon('◔'),
      goals: createMenuIcon('◎'),
      reports: createMenuIcon('▥'),
      logout: createMenuIcon('↪'),
      search: createMenuIcon('⌕'),
      filter: createMenuIcon('☰')
    },
    category: sharedCategoryIcons,
    categoryFallback: sharedCategoryFallback,
    investmentActions: sharedInvestmentActions,
    quickActions: sharedQuickActions
  }
}
