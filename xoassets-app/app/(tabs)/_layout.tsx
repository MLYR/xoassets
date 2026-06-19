import { Tabs } from 'expo-router';

import { MainTabBar } from '@/components/navigation/MainTabBar';

export default function TabsGroupLayout() {
  return (
    <Tabs tabBar={(props) => <MainTabBar {...props} />} screenOptions={{ headerShown: false }}>
      <Tabs.Screen name="home" />
      <Tabs.Screen name="ledger" />
      <Tabs.Screen name="investment" />
      <Tabs.Screen name="profile" />
    </Tabs>
  );
}
