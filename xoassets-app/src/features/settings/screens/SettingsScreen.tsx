import { Redirect, router } from 'expo-router';
import Constants from 'expo-constants';
import { ChevronLeft, Info, LogOut, Palette, ShieldCheck } from 'lucide-react-native';
import { useEffect, useMemo } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button, Card, CardContent, Separator, Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';
import { useAuthStore } from '@/stores/authStore';
import { useThemeStore, type ThemeMode } from '@/stores/themeStore';

export function SettingsScreen() {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const { isHydrated, isLoggedIn, restoreToken, logout } = useAuthStore();
  const { themeMode, setThemeMode } = useThemeStore();

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

  async function handleLogout() {
    // 后端当前没有 logout endpoint，移动端退出登录就是清理本地 token。
    await logout();
    router.replace('/login');
  }

  return (
    <SafeAreaView style={styles.page}>
      <GridBackdrop color={theme.border} />
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Pressable style={styles.iconButton} onPress={() => router.back()}>
            <ChevronLeft color={theme.foreground} size={21} />
          </Pressable>
          <View style={styles.headerCopy}>
            <Text style={styles.title}>设置</Text>
            <Text variant="muted">偏好与本机安全</Text>
          </View>
        </View>

        <Card>
          <CardContent style={styles.cardContent}>
            <View style={styles.securityTitle}>
              <View style={styles.securityIcon}>
                <Palette color={theme.foreground} size={20} strokeWidth={2.3} />
              </View>
              <View>
                <Text style={styles.sectionTitle}>显示偏好</Text>
                <Text variant="muted">主题会保存在本机</Text>
              </View>
            </View>
            <View style={styles.themeSegmented}>
              {themeOptions.map((item) => (
                <Pressable key={item.value} style={[styles.themeButton, themeMode === item.value ? styles.themeButtonActive : null]} onPress={() => setThemeMode(item.value)}>
                  <Text style={[styles.themeButtonText, themeMode === item.value ? styles.themeButtonTextActive : null]}>{item.label}</Text>
                </Pressable>
              ))}
            </View>
          </CardContent>
        </Card>

        <Card>
          <CardContent style={styles.cardContent}>
            <View style={styles.securityTitle}>
              <View style={styles.securityIcon}>
                <ShieldCheck color={theme.foreground} size={20} strokeWidth={2.3} />
              </View>
              <View>
                <Text style={styles.sectionTitle}>本机安全</Text>
                <Text variant="muted">退出后会清理本地登录凭证</Text>
              </View>
            </View>
            <InfoRow label="认证状态" value="已登录" />
            <InfoRow label="凭证存储" value="SecureStore" />
            <InfoRow label="后端退出接口" value="暂无" />
            <Button variant="destructive" onPress={handleLogout}>
              <LogOut color={theme.destructiveForeground} size={18} strokeWidth={2.3} />
              退出登录
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardContent style={styles.cardContent}>
            <View style={styles.securityTitle}>
              <View style={styles.securityIcon}>
                <Info color={theme.foreground} size={20} strokeWidth={2.3} />
              </View>
              <View>
                <Text style={styles.sectionTitle}>关于</Text>
                <Text variant="muted">当前 App 运行信息</Text>
              </View>
            </View>
            <Separator />
            <InfoRow label="应用" value="小〇财迹" />
            <InfoRow label="版本" value={Constants.expoConfig?.version || '1.0.0'} />
          </CardContent>
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}

const themeOptions: Array<{ label: string; value: ThemeMode }> = [
  { label: '跟随系统', value: 'system' },
  { label: '浅色', value: 'light' },
  { label: '深色', value: 'dark' }
];

function InfoRow({ label, value }: { label: string; value: string }) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <View style={styles.infoRow}>
      <Text variant="muted">{label}</Text>
      <Text style={styles.infoValue}>{value}</Text>
    </View>
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
      paddingBottom: 40
    },
    header: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12
    },
    iconButton: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      height: 36,
      justifyContent: 'center',
      width: 36
    },
    headerCopy: {
      flex: 1,
      gap: 2
    },
    title: {
      fontSize: 28,
      fontWeight: '900'
    },
    cardContent: {
      gap: 14
    },
    profileRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12
    },
    avatar: {
      alignItems: 'center',
      backgroundColor: theme.primary,
      borderRadius: 22,
      height: 44,
      justifyContent: 'center',
      width: 44
    },
    avatarText: {
      color: theme.primaryForeground,
      fontSize: 18,
      fontWeight: '900'
    },
    profileCopy: {
      flex: 1,
      gap: 3,
      minWidth: 0
    },
    profileName: {
      fontSize: 17,
      fontWeight: '800'
    },
    infoRow: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    infoValue: {
      fontSize: 15,
      fontWeight: '800'
    },
    securityTitle: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12
    },
    securityIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      height: 36,
      justifyContent: 'center',
      width: 36
    },
    sectionTitle: {
      fontSize: 17,
      fontWeight: '800'
    },
    themeSegmented: {
      backgroundColor: theme.secondary,
      borderRadius: 18,
      flexDirection: 'row',
      padding: 4
    },
    themeButton: {
      alignItems: 'center',
      borderRadius: 14,
      flex: 1,
      minHeight: 36,
      justifyContent: 'center'
    },
    themeButtonActive: {
      backgroundColor: theme.foreground
    },
    themeButtonText: {
      color: theme.foreground,
      fontSize: 13,
      fontWeight: '800'
    },
    themeButtonTextActive: {
      color: theme.background
    }
  });
