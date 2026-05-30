// Vue Router 配置：登录页独立展示，业务页挂载到 AppLayout。
import { createRouter, createWebHistory } from 'vue-router';
import { ROUTES } from '@/constants/routes';
import AppLayout from '@/layouts/AppLayout.vue';
import LoginView from '@/views/login/LoginView.vue';

// 业务页面使用懒加载，减少登录页首屏加载体积。
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: ROUTES.login },
    { path: ROUTES.login, component: LoginView },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: ROUTES.dashboard.slice(1), component: () => import('@/views/dashboard/DashboardView.vue') },
        { path: ROUTES.transactions.slice(1), component: () => import('@/views/transactions/TransactionsView.vue') },
        { path: ROUTES.accounts.slice(1), component: () => import('@/views/accounts/AccountsView.vue') },
        { path: ROUTES.investments.slice(1), component: () => import('@/views/investments/InvestmentsView.vue') },
        { path: ROUTES.analytics.slice(1), component: () => import('@/views/analytics/AnalyticsView.vue') },
        { path: ROUTES.reports.slice(1), component: () => import('@/views/reports/ReportsView.vue') },
        { path: ROUTES.budgets.slice(1), component: () => import('@/views/budgets/BudgetsView.vue') },
        { path: ROUTES.goals.slice(1), component: () => import('@/views/goals/GoalsView.vue') }
      ]
    }
  ]
});

export default router;
