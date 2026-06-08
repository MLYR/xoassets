// Vue Router 配置：登录页独立展示，业务页挂载到 AppLayout。
import { createRouter, createWebHistory } from 'vue-router';
import { ROUTES } from '@/constants/routes';
import AppLayout from '@/layouts/AppLayout.vue';
import LoginView from '@/views/login/LoginView.vue';
import { hasToken } from '@/services/token';

// 业务页面使用懒加载，减少登录页首屏加载体积。
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: () => (hasToken() ? ROUTES.dashboard : ROUTES.login) },
    { path: ROUTES.login, component: LoginView, meta: { public: true } },
    { path: ROUTES.register, component: () => import('@/views/register/RegisterView.vue'), meta: { public: true } },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: ROUTES.dashboard.slice(1), component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '首页' } },
        { path: ROUTES.transactions.slice(1), component: () => import('@/views/transactions/TransactionsView.vue'), meta: { title: '记账流水' } },
        { path: ROUTES.accounts.slice(1), component: () => import('@/views/accounts/AccountsView.vue'), meta: { title: '账户管理' } },
        { path: ROUTES.accountDetail.slice(1), component: () => import('@/views/accounts/AccountDetailView.vue'), meta: { title: '账户详情' } },
        { path: ROUTES.categories.slice(1), component: () => import('@/views/categories/CategoriesView.vue'), meta: { title: '分类管理' } },
        { path: ROUTES.investments.slice(1), component: () => import('@/views/investments/InvestmentsView.vue'), meta: { title: '投资持仓' } },
        { path: ROUTES.holdingDetail.slice(1), component: () => import('@/views/investments/HoldingDetailView.vue'), meta: { title: '持仓详情' } },
        { path: ROUTES.analytics.slice(1), component: () => import('@/views/analytics/AnalyticsView.vue'), meta: { title: '数据分析' } },
        { path: ROUTES.reports.slice(1), component: () => import('@/views/reports/ReportsView.vue'), meta: { title: 'AI报告' } },
        { path: ROUTES.budgets.slice(1), component: () => import('@/views/budgets/BudgetsView.vue'), meta: { title: '预算管理' } },
        { path: ROUTES.goals.slice(1), component: () => import('@/views/goals/GoalsView.vue'), meta: { title: '资产目标' } }
      ]
    }
  ]
});

// 全局登录守卫：没有 token 时禁止直接进入业务页面。
router.beforeEach((to) => {
  const isPublicRoute = Boolean(to.meta.public);
  if (!isPublicRoute && !hasToken()) {
    return { path: ROUTES.login, query: { redirect: to.fullPath } };
  }
  if ((to.path === ROUTES.login || to.path === ROUTES.register) && hasToken()) {
    return ROUTES.dashboard;
  }
  return true;
});

export default router;
