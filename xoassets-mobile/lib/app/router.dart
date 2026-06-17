import 'package:go_router/go_router.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/foundation.dart';

import '../features/auth/presentation/pages/login_page.dart';
import '../features/auth/presentation/pages/register_page.dart';
import '../features/auth/presentation/providers/auth_provider.dart';
import '../features/budget/presentation/pages/budget_page.dart';
import '../features/investment/presentation/pages/investment_trade_page.dart';
import '../features/main/presentation/pages/main_tab_page.dart';
import '../features/report/presentation/pages/report_page.dart';
import '../features/settings/presentation/pages/settings_page.dart';
import '../features/splash/presentation/pages/splash_page.dart';
import '../features/transaction/presentation/pages/transaction_edit_page.dart';
import 'routes.dart';

/// 路由表由 Riverpod 持有，方便按登录状态做统一重定向。
final appRouterProvider = Provider<GoRouter>((ref) {
  final refreshListenable = _AuthRouterRefresh(ref);
  ref.onDispose(refreshListenable.dispose);

  return GoRouter(
    initialLocation: AppRoutes.splash,
    refreshListenable: refreshListenable,
    redirect: (context, state) {
      final authState = ref.read(authProvider);
      final path = state.uri.path;
      final isSplash = path == AppRoutes.splash;
      final isLogin = path == AppRoutes.login;
      final isRegister = path == AppRoutes.register;

      if (authState.status == AuthStatus.unknown) {
        return isSplash ? null : AppRoutes.splash;
      }
      if (!authState.isAuthenticated) {
        // 注册页与登录页同属未登录白名单，避免注册流程被守卫打断。
        return (isLogin || isRegister) ? null : AppRoutes.login;
      }
      if (isSplash || isLogin || isRegister) {
        return AppRoutes.main;
      }
      return null;
    },
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
        path: AppRoutes.register,
        builder: (context, state) => const RegisterPage(),
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
});

/// 将登录态变化转成 GoRouter 刷新信号，避免重建整个 router 导致登录页输入框被清空。
class _AuthRouterRefresh extends ChangeNotifier {
  _AuthRouterRefresh(this._ref) {
    _subscription = _ref.listen<AuthState>(
      authProvider,
      (_, _) => notifyListeners(),
    );
  }

  final Ref _ref;
  late final ProviderSubscription<AuthState> _subscription;

  @override
  void dispose() {
    _subscription.close();
    super.dispose();
  }
}
