// 路由路径常量：集中管理，避免页面和菜单硬编码分散。
export const ROUTES = {
  login: '/login',
  register: '/register',
  dashboard: '/dashboard',
  transactions: '/transactions',
  accounts: '/accounts',
  categories: '/categories',
  investments: '/investments',
  investmentDetails: '/investments/details',
  analytics: '/analytics',
  reports: '/reports',
  budgets: '/budgets',
  goals: '/goals'
} as const;
