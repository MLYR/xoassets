import type { PropsWithChildren } from 'react';

import { QueryClientProvider } from '@tanstack/react-query';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { queryClient } from '@/core/app/query-client';
import { useTheme } from '@/core/design/theme';

export function AppProviders({ children }: PropsWithChildren) {
  const theme = useTheme();

  return (
    <QueryClientProvider client={queryClient}>
      <SafeAreaProvider>
        {/* 状态栏样式跟随当前主题，避免路由整理后页面明暗反差失控。 */}
        <StatusBar style={theme.background === '#09090b' ? 'light' : 'dark'} />
        {children}
      </SafeAreaProvider>
    </QueryClientProvider>
  );
}
