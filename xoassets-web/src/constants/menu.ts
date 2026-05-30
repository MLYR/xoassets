// 菜单配置：侧边栏直接渲染该数组，保持路由和标题一致。
import {
  Aim,
  DataAnalysis,
  Document,
  HomeFilled,
  List,
  Money,
  PieChart,
  TrendCharts
} from '@element-plus/icons-vue';
import { ROUTES } from './routes';

export const menuItems = [
  { path: ROUTES.dashboard, icon: HomeFilled, label: '首页' },
  { path: ROUTES.transactions, icon: List, label: '记账' },
  { path: ROUTES.accounts, icon: Money, label: '账户' },
  { path: ROUTES.investments, icon: TrendCharts, label: '投资' },
  { path: ROUTES.analytics, icon: DataAnalysis, label: '统计' },
  { path: ROUTES.reports, icon: Document, label: 'AI报告' },
  { path: ROUTES.budgets, icon: PieChart, label: '预算' },
  { path: ROUTES.goals, icon: Aim, label: '目标' }
];
