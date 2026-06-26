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

### 打包 APK（可直接安装到手机）

当前项目已在 `eas.json` 里增加 `apk` 构建 profile，直接执行：

```bash
cd xoassets-app
npm install --global eas-cli
eas login
eas build --platform android --profile apk
```

说明：

- 如果终端提示 `zsh: command not found: eas`，先安装 `eas-cli`，然后重新打开终端再执行。
- 也可以临时使用 `npx eas-cli@latest ...`，不用全局安装。
- 这个命令会生成可安装的 `.apk` 文件，适合真机测试、发给同事安装。
- 如果只是上架 Google Play，一般用 `production` profile 生成 `.aab`，不是 APK。
- 如需本地构建，也可以用：

```bash
eas build --platform android --profile apk --local
```

- `EXPO_PUBLIC_API_BASE_URL` 这类 `EXPO_PUBLIC_*` 环境变量会在构建时注入到 App 里；你现在 `.env` 里的 `http://43.142.119.229:8080` 会被打进 APK。
- 如果要改后端地址，改完 `.env` 后需要重新打包，旧 APK 不会自动更新。
- `--local` 表示在本机执行同一套构建流程，依然会读取当前项目的 `.env` / 本机环境变量。

### 缩小 EAS 上传包体

项目根目录下已新增 `.easignore`。

规则：

- `.easignore` 会优先于 `.gitignore`，所以要先把 `.gitignore` 里的内容完整复制进去。
- 然后再额外排除 EAS 构建不需要的大目录，例如：

```text
/android
/ios
/docs
/coverage
```

- 这样可以明显缩小上传包，减少 `Compressed project files` 和 `Uploaded to EAS` 的时间。
- 如果后续发现某些本地原生修改需要随包上传，再把对应目录从 `.easignore` 里移除。

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
/account/new
/transaction/edit
/investment/trade
/budget
/report
/settings
```

## 当前移动端首页 / 记账 / 投资实现约定

- 首页投资概览接入真实 `GET /api/investments/overview`，按后端 `moduleAssets` 展示基金、股票、加密资产。
- 首页资产详情、投资概览、本月预算和最近记录入口已接到真实页面：账户、投资、预算和记账。
- 首页右上角设置入口已接到 `/settings`，设置页提供登录状态和退出登录。
- 我的 Tab 已接到真实页面骨架，聚合账户、预算、投资和设置入口。
- 投资概览每个模块固定展示“昨/今收益”：第一行使用后端 `yesterdayProfit`，该字段由收益日历聚合，昨日无收益时代表上一个收益日；第二行使用后端 `primaryProfitAmount`，不可用时显示 `--`。
- 首页最近记录第一行优先显示备注，备注为空时显示分类；第二行显示流水类型。
- 记账日历固定渲染 6 行；统计页和分类占比 / 排行均使用真实流水接口数据在 App 端做展示聚合，不伪造业务金额；分类排行和明细排行可点击查看当前周期内的单独分类明细和单条流水详情。
- `/transaction/edit` 现在直接进入独立“记一笔”页面，不再从底部弹层二次跳转；分类和账户使用下拉选择，日期时间使用中文滚动选择器，保存按钮固定放在备注下方。
- 账户 Tab 已接入阶段 5 真实接口，包含账户聚合总览、账户列表、账户详情资金统计、账户流水、新增账户、编辑账户和余额修正。
- 预算页已接入阶段 7 真实接口，包含本月预算汇总、总预算、分类预算、新增 / 编辑预算和超支提醒。
- 投资 Tab 已接入阶段 6 真实接口，包含总览、资产走势、收益走势、收益日历、持仓列表、持仓详情、交易录入、独立交易入口和交易详情。
- 投资收益日历使用后端 `/api/investments/daily-profit` 和 `/api/investments/holdings/{id}/profit-calendar` 返回结果；App 只展示后端 `marketClosed/statusLabel`，不在前端猜测休市。
- 投资页金额隐藏开关必须覆盖金额、收益率、数量、价格、日历收益和资产分布占比，不能只隐藏总资产卡片。
