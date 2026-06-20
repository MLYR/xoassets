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

安装依赖：

```bash
cd xoassets-app
npm install
```

### Expo Go 真机调试

默认固定 Metro 端口为 `8088`，并跳过 Expo 联网依赖校验，避免本地网络不稳定时启动失败。

```bash
npm run start
```

手机和电脑需要在同一个局域网。启动后使用手机 Expo Go 扫终端二维码。

如果手机仍显示旧页面，先停掉当前进程，再重新启动：

```bash
npm run start
```

### Expo Web 预览

```bash
npm run web
```

浏览器访问：

```text
http://localhost:8088
```

### Android 模拟器

```bash
npm run android:emulator
npm run android
```

说明：

- `npm run android:emulator` 只启动本机 AVD，默认使用 `xoassets_api36`，并等待系统启动完成。
- `npm run android` 固定使用本机 OpenJDK 17、Android SDK 和 Metro `8088` 端口。
- Android 构建前会执行 `scripts/patch-react-native-gradle-repos.js`，用于给 React Native / Expo 的 Gradle 插件和 RN 模块临时补国内 Maven 镜像，避免 Maven Central 403。

### iOS 模拟器

```bash
npm run ios
```

### 常用排查

端口占用：

```bash
lsof -nP -iTCP:8088 -sTCP:LISTEN
```

手动清缓存启动：

```bash
EXPO_NO_DEPENDENCY_VALIDATION=1 npx expo start --port 8088 --clear
```

Android 模拟器访问本机 Expo 服务：

```bash
adb reverse tcp:8088 tcp:8088
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
/account
/transaction/edit
/investment/trade
/budget
/report
/settings
```

## 当前移动端首页 / 记账 / 投资实现约定

- 首页投资概览接入真实 `GET /api/investments/overview`，按后端 `moduleAssets` 展示基金、股票、加密资产。
- 投资概览每个模块固定展示“昨/今收益”：第一行使用后端 `yesterdayProfit`，该字段由收益日历聚合，昨日无收益时代表上一个收益日；第二行使用后端 `primaryProfitAmount`，不可用时显示 `--`。
- 首页最近记录第一行优先显示备注，备注为空时显示分类；第二行显示流水类型。
- 记账日历固定渲染 6 行；统计页和分类占比 / 排行均使用真实流水接口数据在 App 端做展示聚合，不伪造业务金额。
- 账户 Tab 已接入阶段 5 真实接口，包含账户聚合总览、账户列表、账户详情资金统计、账户流水、新增账户、编辑账户和余额修正。
- 预算页已接入阶段 7 真实接口，包含本月预算汇总、总预算、分类预算、新增 / 编辑预算和超支提醒。
- 投资 Tab 已接入阶段 6 真实接口，包含总览、资产走势、收益走势、收益日历、持仓列表、持仓详情、交易录入和交易详情。
- 投资收益日历使用后端 `/api/investments/daily-profit` 和 `/api/investments/holdings/{id}/profit-calendar` 返回结果；App 只展示后端 `marketClosed/statusLabel`，不在前端猜测休市。
- 投资页金额隐藏开关必须覆盖金额、收益率、数量、价格、日历收益和资产分布占比，不能只隐藏总资产卡片。
