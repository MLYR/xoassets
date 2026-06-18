# XOAssets App

XOAssets / 小〇财迹 React Native 新版移动端 App。

## 技术栈

- React Native
- TypeScript
- Expo
- Expo Router
- NativeWind
- 自研 XO Design System
- Zustand
- TanStack Query
- Axios
- React Hook Form
- Zod
- expo-secure-store
- @react-native-async-storage/async-storage
- react-native-svg
- react-native-reanimated

App 不直接连接 MySQL，只通过 Spring Boot HTTP API 访问后端。

## 文档入口

| 文件 | 说明 |
|---|---|
| `AGENTS.md` | 移动端 Codex 约束 |
| `MOBILE_APP_PHASES.md` | 移动端阶段计划 |
| `docs/MOBILE_PRODUCT_SPEC.md` | 页面与功能说明 |
| `docs/MOBILE_UI_DESIGN_SYSTEM.md` | UI 设计系统 |
| `docs/MOBILE_ICON_ASSETS_GUIDE.md` | 图标与启动图规范 |
| `docs/MOBILE_API_INTEGRATION.md` | 接口接入约束 |

## 运行方式

```bash
cd xoassets-app
npm install
npm run start
```

Android：

```bash
npm run android:emulator
npm run android
```

iOS：

```bash
npm run ios
```

## API 地址

默认：

```text
http://localhost:8080
```

Android 模拟器：

```text
http://10.0.2.2:8080
```

真机调试：

```text
http://<你的电脑IP>:8080
```

## 路由建议

```text
/(auth)/login
/(auth)/register
/(tabs)/home
/(tabs)/ledger
/(tabs)/investment
/(tabs)/profile
/transaction/edit
/investment/trade
/budget
/report
/settings
```
