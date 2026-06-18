import '@/styles/global.css';

import { Stack } from 'expo-router';

import { AppProviders } from '@/core/app/providers';
import { useTheme } from '@/core/design/theme';

export default function RootLayout() {
  const theme = useTheme();
  return (
    <AppProviders>
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: theme.background }
        }}
      />
    </AppProviders>
  );
}
