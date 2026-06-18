import { Redirect } from 'expo-router';
import { useEffect } from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';

import { useAuthStore } from '@/stores/authStore';
import { useTheme } from '@/core/design/theme';

export default function IndexPage() {
  const theme = useTheme();
  const { isHydrated, isLoggedIn, restoreToken } = useAuthStore();

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  if (!isHydrated) {
    return (
      <View style={[styles.loading, { backgroundColor: theme.background }]}>
        <ActivityIndicator color={theme.primary} />
      </View>
    );
  }

  return <Redirect href={isLoggedIn ? '/home' : '/login'} />;
}

const styles = StyleSheet.create({
  loading: {
    alignItems: 'center',
    flex: 1,
    justifyContent: 'center'
  }
});
