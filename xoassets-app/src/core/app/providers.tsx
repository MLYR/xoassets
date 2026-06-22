import type { PropsWithChildren } from 'react';
import { useEffect, useRef } from 'react';

import { QueryClientProvider } from '@tanstack/react-query';
import { usePathname } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { AppState } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { queryClient } from '@/core/app/query-client';
import { authApi } from '@/features/auth/api/authApi';
import { useTheme } from '@/core/design/theme';
import { useAuthStore } from '@/stores/authStore';

export function AppProviders({ children }: PropsWithChildren) {
  const theme = useTheme();

  return (
    <QueryClientProvider client={queryClient}>
      <SafeAreaProvider>
        {/* 状态栏样式跟随当前主题，避免路由整理后页面明暗反差失控。 */}
        <StatusBar style={theme.background === '#09090b' ? 'light' : 'dark'} />
        {/* 全局校验登录态：切页或回到前台时主动请求一次 /api/auth/me，避免只靠缓存看起来还在线。 */}
        <AuthSessionWatcher />
        {children}
      </SafeAreaProvider>
    </QueryClientProvider>
  );
}

function AuthSessionWatcher() {
  const pathname = usePathname();
  const { isHydrated, isLoggedIn } = useAuthStore();
  const lastValidatedRef = useRef('');

  useEffect(() => {
    if (!isHydrated || !isLoggedIn || isAuthRoute(pathname)) {
      return;
    }

    const validationKey = `route:${pathname}`;
    if (lastValidatedRef.current === validationKey) {
      return;
    }
    lastValidatedRef.current = validationKey;
    authApi.me().catch(() => {});
  }, [isHydrated, isLoggedIn, pathname]);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', (state) => {
      if (state !== 'active' || !isHydrated || !isLoggedIn || isAuthRoute(pathname)) {
        return;
      }
      const validationKey = `app:${pathname}`;
      if (lastValidatedRef.current === validationKey) {
        return;
      }
      lastValidatedRef.current = validationKey;
      authApi.me().catch(() => {});
    });

    return () => {
      subscription.remove();
    };
  }, [isHydrated, isLoggedIn, pathname]);

  return null;
}

function isAuthRoute(pathname: string) {
  return pathname === '/login' || pathname === '/register';
}
