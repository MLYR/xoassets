import 'package:go_router/go_router.dart';

import '../features/auth/presentation/pages/login_page.dart';
import '../features/budget/presentation/pages/budget_page.dart';
import '../features/investment/presentation/pages/investment_trade_page.dart';
import '../features/main/presentation/pages/main_tab_page.dart';
import '../features/report/presentation/pages/report_page.dart';
import '../features/settings/presentation/pages/settings_page.dart';
import '../features/splash/presentation/pages/splash_page.dart';
import '../features/transaction/presentation/pages/transaction_edit_page.dart';
import 'routes.dart';

/// 第一阶段路由表：业务详情页先保留壳子，不做真实接口接入。
final appRouter = GoRouter(
  initialLocation: AppRoutes.splash,
  routes: [
    GoRoute(
      path: AppRoutes.splash,
      builder: (context, state) => const SplashPage(),
    ),
    GoRoute(
      path: AppRoutes.login,
      builder: (context, state) => const LoginPage(),
    ),
    GoRoute(
      path: AppRoutes.main,
      builder: (context, state) => const MainTabPage(),
    ),
    GoRoute(
      path: AppRoutes.transactionEdit,
      builder: (context, state) => TransactionEditPage(
        initialType: state.uri.queryParameters['type'] ?? 'expense',
      ),
    ),
    GoRoute(
      path: AppRoutes.investmentTrade,
      builder: (context, state) => const InvestmentTradePage(),
    ),
    GoRoute(
      path: AppRoutes.budget,
      builder: (context, state) => const BudgetPage(),
    ),
    GoRoute(
      path: AppRoutes.report,
      builder: (context, state) => const ReportPage(),
    ),
    GoRoute(
      path: AppRoutes.settings,
      builder: (context, state) => const SettingsPage(),
    ),
  ],
);
