# XOAssets App

XOAssets / 小〇财迹 React Native 新版移动端 App。

## 技术栈

- React Native
- TypeScript
- Expo
- Expo Router
- NativeWind
- React Native Reusables
- `src/components/ui` 项目 UI 组件出口
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
| `docs/REACT_NATIVE_REUSABLES_UI_CONSTRAINTS.md` | React Native Reusables UI 约束 |
| `docs/MOBILE_PRODUCT_SPEC.md` | 移动端产品范围 |
| `docs/MOBILE_API_INTEGRATION.md` | 移动端接口接入约束 |
| `docs/MOBILE_ICON_ASSETS_GUIDE.md` | 图标与启动图规范 |

## UI 标准

```text
React Native Reusables + NativeWind + src/components/ui
```

规则：

- React Native Reusables 是移动端组件和视觉基准。
- NativeWind 是样式基础。
- `src/components/ui` 是项目唯一 UI 组件出口。
- 不使用 Web 版 shadcn/ui 组件。
- 不继续使用旧 Uiverse / Web shadcn 风格作为移动端主标准。

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
