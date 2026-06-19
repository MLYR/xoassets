import { router } from 'expo-router';
import type { BottomTabBarProps } from '@react-navigation/bottom-tabs';
import { BarChart3, Home, NotebookText, ReceiptText, TrendingUp, UserRound, WalletCards, X } from 'lucide-react-native';
import { Modal, Pressable, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useState } from 'react';

import { Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';

const tabItems = [
  { name: 'home', label: '首页', icon: Home },
  { name: 'ledger', label: '记账', icon: NotebookText },
  { name: 'investment', label: '投资', icon: BarChart3 },
  { name: 'profile', label: '我的', icon: UserRound }
];

export function MainTabBar({ state, navigation }: BottomTabBarProps) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const styles = createStyles(theme, insets.bottom);
  const [quickOpen, setQuickOpen] = useState(false);

  function handleQuickNavigate(action: 'ledger' | 'investment' | 'profile') {
    setQuickOpen(false);
    if (action === 'ledger') {
      router.push(`/ledger?compose=${Date.now()}`);
      return;
    }
    navigation.navigate(action);
  }

  return (
    <>
      <View style={styles.wrap}>
        <View style={styles.sideTabs}>
          {tabItems.slice(0, 2).map((item) => (
            <TabButton key={item.name} item={item} active={state.routes[state.index]?.name === item.name} onPress={() => navigation.navigate(item.name)} />
          ))}
        </View>
        <View style={styles.addSlot}>
          <Pressable style={({ pressed }) => [styles.addHitArea, pressed ? styles.pressed : null]} onPress={() => setQuickOpen(true)}>
            <View style={styles.addButton}>
              <View style={styles.addIconHorizontal} />
              <View style={styles.addIconVertical} />
            </View>
          </Pressable>
        </View>
        <View style={styles.sideTabs}>
          {tabItems.slice(2).map((item) => (
            <TabButton key={item.name} item={item} active={state.routes[state.index]?.name === item.name} onPress={() => navigation.navigate(item.name)} />
          ))}
        </View>
      </View>
      <Modal visible={quickOpen} animationType="fade" transparent onRequestClose={() => setQuickOpen(false)}>
        <Pressable style={styles.modalBackdrop} onPress={() => setQuickOpen(false)} />
        <View style={styles.quickSheet}>
          <View style={styles.quickHeader}>
            <Text style={styles.quickTitle}>快捷创建</Text>
            <Pressable style={styles.closeButton} onPress={() => setQuickOpen(false)}>
              <X color={theme.foreground} size={22} />
            </Pressable>
          </View>
          <View style={styles.quickGrid}>
            <QuickAction icon={ReceiptText} label="记一笔" onPress={() => handleQuickNavigate('ledger')} />
            <QuickAction icon={TrendingUp} label="投资交易" onPress={() => handleQuickNavigate('investment')} />
            <QuickAction icon={WalletCards} label="账户" onPress={() => handleQuickNavigate('profile')} />
          </View>
        </View>
      </Modal>
    </>
  );
}

function QuickAction({ icon: Icon, label, onPress }: { icon: typeof ReceiptText; label: string; onPress: () => void }) {
  const theme = useTheme();
  const styles = createStyles(theme, 0);

  return (
    <Pressable style={({ pressed }) => [styles.quickAction, pressed ? styles.pressed : null]} onPress={onPress}>
      <View style={styles.quickIcon}>
        <Icon color={theme.foreground} size={23} strokeWidth={2.2} />
      </View>
      <Text style={styles.quickLabel}>{label}</Text>
    </Pressable>
  );
}

function TabButton({ item, active, onPress }: { item: (typeof tabItems)[number]; active: boolean; onPress: () => void }) {
  const theme = useTheme();
  const styles = createStyles(theme, 0);
  const Icon = item.icon;
  const color = active ? theme.foreground : theme.mutedForeground;

  return (
    <Pressable style={({ pressed }) => [styles.tabButton, pressed ? styles.pressed : null]} onPress={onPress}>
      <Icon color={color} size={23} strokeWidth={active ? 2.6 : 2} fill={active ? color : undefined} />
      <Text style={[styles.tabText, { color }]}>{item.label}</Text>
    </Pressable>
  );
}

const createStyles = (theme: ReturnType<typeof useTheme>, bottomInset: number) =>
  StyleSheet.create({
    wrap: {
      alignItems: 'center',
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderTopLeftRadius: 22,
      borderTopRightRadius: 22,
      borderWidth: 1,
      flexDirection: 'row',
      height: 70 + bottomInset,
      justifyContent: 'space-between',
      paddingBottom: Math.max(bottomInset, 8),
      paddingHorizontal: 10,
      shadowColor: theme.shadow,
      shadowOffset: { width: 0, height: -8 },
      shadowOpacity: 0.08,
      shadowRadius: 18
    },
    sideTabs: {
      alignItems: 'center',
      flexDirection: 'row',
      flex: 1,
      justifyContent: 'space-around'
    },
    tabButton: {
      alignItems: 'center',
      gap: 4,
      minWidth: 50
    },
    tabText: {
      fontSize: 12,
      fontWeight: '700'
    },
    addSlot: {
      alignItems: 'center',
      height: 58,
      justifyContent: 'center',
      position: 'relative',
      width: 72
    },
    addButton: {
      alignItems: 'center',
      backgroundColor: '#111111',
      borderColor: '#111111',
      borderRadius: 28,
      borderWidth: 1,
      elevation: 12,
      height: 56,
      justifyContent: 'center',
      overflow: 'visible',
      position: 'relative',
      shadowColor: theme.shadow,
      shadowOffset: { width: 0, height: 10 },
      shadowOpacity: 0.18,
      shadowRadius: 14,
      width: 56,
      zIndex: 10
    },
    addHitArea: {
      alignItems: 'center',
      height: 58,
      justifyContent: 'center',
      width: 72,
      zIndex: 11
    },
    addIconHorizontal: {
      backgroundColor: '#ffffff',
      borderRadius: 2,
      height: 4,
      position: 'absolute',
      width: 25
    },
    addIconVertical: {
      backgroundColor: '#ffffff',
      borderRadius: 2,
      height: 25,
      position: 'absolute',
      width: 4
    },
    modalBackdrop: {
      ...StyleSheet.absoluteFillObject,
      backgroundColor: 'rgba(0,0,0,0.18)'
    },
    quickSheet: {
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderRadius: 18,
      borderWidth: 1,
      bottom: 92 + bottomInset,
      left: 18,
      padding: 16,
      position: 'absolute',
      right: 18,
      shadowColor: theme.shadow,
      shadowOffset: { width: 0, height: 12 },
      shadowOpacity: 0.12,
      shadowRadius: 22
    },
    quickHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between',
      marginBottom: 14
    },
    quickTitle: {
      fontSize: 18,
      fontWeight: '800'
    },
    closeButton: {
      alignItems: 'center',
      height: 32,
      justifyContent: 'center',
      width: 32
    },
    quickGrid: {
      flexDirection: 'row',
      gap: 12
    },
    quickAction: {
      alignItems: 'center',
      flex: 1,
      gap: 8
    },
    quickIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      height: 48,
      justifyContent: 'center',
      width: 48
    },
    quickLabel: {
      fontSize: 13,
      fontWeight: '700'
    },
    pressed: {
      opacity: 0.76,
      transform: [{ scale: 0.98 }]
    }
  });
