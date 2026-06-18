import { Stack } from 'expo-router';

export default function TabsGroupLayout() {
  // 先用路由分组收敛首页主流程，后续再在这里接正式底部 Tab。
  return <Stack screenOptions={{ headerShown: false }} />;
}
