import { useColorScheme } from 'react-native';

import { useThemeStore } from '@/stores/themeStore';
import { tokens } from './tokens';

export const lightTheme = {
  background: '#fafafa',
  foreground: '#111111',
  card: '#ffffff',
  cardForeground: '#111111',
  popover: '#ffffff',
  popoverForeground: '#111111',
  primary: '#111111',
  primaryForeground: '#ffffff',
  secondary: '#f4f4f5',
  secondaryForeground: '#18181b',
  muted: '#f4f4f5',
  mutedForeground: '#71717a',
  accent: '#f4f4f5',
  accentForeground: '#18181b',
  destructive: '#dc2626',
  destructiveForeground: '#ffffff',
  success: '#16a34a',
  warning: '#d97706',
  info: '#2563eb',
  border: '#e4e4e7',
  input: '#e4e4e7',
  ring: '#a1a1aa',
  shadow: '#000000',
  tokens
} as const;

export const darkTheme = {
  background: '#09090b',
  foreground: '#fafafa',
  card: '#111113',
  cardForeground: '#fafafa',
  popover: '#111113',
  popoverForeground: '#fafafa',
  primary: '#fafafa',
  primaryForeground: '#09090b',
  secondary: '#1f1f23',
  secondaryForeground: '#fafafa',
  muted: '#1f1f23',
  mutedForeground: '#a1a1aa',
  accent: '#27272a',
  accentForeground: '#fafafa',
  destructive: '#ef4444',
  destructiveForeground: '#ffffff',
  success: '#22c55e',
  warning: '#f59e0b',
  info: '#60a5fa',
  border: '#27272a',
  input: '#3f3f46',
  ring: '#71717a',
  shadow: '#000000',
  tokens
} as const;

export type XoTheme = typeof lightTheme;

export function useTheme() {
  const systemScheme = useColorScheme();
  const { themeMode } = useThemeStore();
  const resolvedMode = themeMode === 'system' ? systemScheme : themeMode;

  return resolvedMode === 'dark' ? darkTheme : lightTheme;
}

export function getTheme(isDark: boolean) {
  return isDark ? darkTheme : lightTheme;
}
