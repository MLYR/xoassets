import { Redirect, router } from 'expo-router';
import { ChevronRight, PieChart, Settings, Target, UserRound, WalletCards } from 'lucide-react-native';
import { useEffect, useMemo } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Card, CardContent, Separator, Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';
import { useAuthStore } from '@/stores/authStore';

const profileEntries = [
  { label: '账户管理', description: '账户列表、详情、流水和余额修正', route: '/account', icon: WalletCards },
  { label: '预算管理', description: '本月预算、分类预算和超支提醒', route: '/budget', icon: Target },
  { label: '投资管理', description: '投资总览、持仓、收益和交易', route: '/investment', icon: PieChart },
  { label: '设置', description: '登录状态和退出登录', route: '/settings', icon: Settings }
] as const;

export function ProfileScreen() {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const { userInfo, isHydrated, isLoggedIn, restoreToken } = useAuthStore();

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  if (!isHydrated) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator color={theme.primary} />
      </View>
    );
  }

  if (!isLoggedIn) {
    return <Redirect href="/login" />;
  }

  return (
    <SafeAreaView style={styles.page}>
      <GridBackdrop color={theme.border} />
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Text style={styles.title}>我的</Text>
          <Text variant="muted">账户、预算、投资和设置入口</Text>
        </View>

        <Card>
          <CardContent style={styles.profileCard}>
            <View style={styles.profileRow}>
              <View style={styles.avatar}>
                <UserRound color={theme.primaryForeground} size={24} strokeWidth={2.3} />
              </View>
              <View style={styles.profileCopy}>
                <Text style={styles.profileName}>{userInfo?.nickname || userInfo?.username || 'XOAssets 用户'}</Text>
                <Text variant="muted">{userInfo?.username || '已登录'}</Text>
              </View>
            </View>
          </CardContent>
        </Card>

        <Card>
          <CardContent style={styles.entryCard}>
            {profileEntries.map((entry, index) => {
              const Icon = entry.icon;
              return (
                <View key={entry.route}>
                  <Pressable style={styles.entryRow} onPress={() => router.push(entry.route)}>
                    <View style={styles.entryIcon}>
                      <Icon color={theme.foreground} size={20} strokeWidth={2.3} />
                    </View>
                    <View style={styles.entryCopy}>
                      <Text style={styles.entryTitle}>{entry.label}</Text>
                      <Text variant="caption">{entry.description}</Text>
                    </View>
                    <ChevronRight color={theme.mutedForeground} size={18} />
                  </Pressable>
                  {index < profileEntries.length - 1 ? <Separator /> : null}
                </View>
              );
            })}
          </CardContent>
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}

function GridBackdrop({ color }: { color: string }) {
  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill}>
      {Array.from({ length: 18 }).map((_, index) => (
        <View key={`v-${index}`} style={[stylesStatic.gridLineVertical, { left: index * 28, backgroundColor: color }]} />
      ))}
      {Array.from({ length: 36 }).map((_, index) => (
        <View key={`h-${index}`} style={[stylesStatic.gridLineHorizontal, { top: index * 28, backgroundColor: color }]} />
      ))}
    </View>
  );
}

const stylesStatic = StyleSheet.create({
  gridLineVertical: {
    opacity: 0.14,
    position: 'absolute',
    top: 0,
    bottom: 0,
    width: 1
  },
  gridLineHorizontal: {
    left: 0,
    opacity: 0.14,
    position: 'absolute',
    right: 0,
    height: 1
  }
});

const createStyles = (theme: ReturnType<typeof useTheme>) =>
  StyleSheet.create({
    page: {
      backgroundColor: theme.background,
      flex: 1
    },
    loading: {
      alignItems: 'center',
      backgroundColor: theme.background,
      flex: 1,
      justifyContent: 'center'
    },
    content: {
      gap: 14,
      padding: 18,
      paddingBottom: 112
    },
    header: {
      gap: 4,
      marginBottom: 4
    },
    title: {
      fontSize: 28,
      fontWeight: '900'
    },
    profileCard: {
      gap: 12
    },
    profileRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12
    },
    avatar: {
      alignItems: 'center',
      backgroundColor: theme.primary,
      borderRadius: 24,
      height: 48,
      justifyContent: 'center',
      width: 48
    },
    profileCopy: {
      flex: 1,
      gap: 3,
      minWidth: 0
    },
    profileName: {
      fontSize: 18,
      fontWeight: '900'
    },
    entryCard: {
      gap: 2
    },
    entryRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12,
      paddingVertical: 13
    },
    entryIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      height: 36,
      justifyContent: 'center',
      width: 36
    },
    entryCopy: {
      flex: 1,
      gap: 3,
      minWidth: 0
    },
    entryTitle: {
      fontSize: 16,
      fontWeight: '800'
    }
  });
