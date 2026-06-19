# XOAssets / 小〇财迹

XOAssets 是个人资产管理与财务复盘项目，当前仓库包含：

```text
xoassets-server   Java 17 + Spring Boot 3 后端
xoassets-web      Vue3 + TypeScript + Vite Web 管理端
xoassets-app      React Native 新版移动端 App
AGENTS.md         Codex 协作规范
```

## 当前重点

- 后端是业务和数据口径权威。
- Web 是现有管理端 / Web 前端。
- 新版移动端以 `xoassets-app` 为准，使用 React Native + Expo。
- 移动端 UI 以 React Native Reusables 为组件和视觉基准，样式基础为 NativeWind，项目组件出口为 `src/components/ui`。

## 文档入口

| 文件 | 说明 |
|---|---|
| `AGENTS.md` | 根目录 Codex 协作约束 |
| `docs/PROJECT_OVERVIEW.md` | 项目概览 |
| `docs/BUSINESS_RULES.md` | 业务口径 |
| `docs/API_CONTRACTS.md` | 接口约定 |
| `docs/LOCAL_DEVELOPMENT.md` | 本地开发与联调 |
| `docs/VALIDATION_CHECKLIST.md` | 验证清单 |
| `docs/CODEX_WORKFLOW.md` | Codex 工作流 |
| `xoassets-app/AGENTS.md` | 移动端子项目约束 |
| `xoassets-app/MOBILE_APP_PHASES.md` | 移动端阶段计划 |
| `xoassets-app/docs/REACT_NATIVE_REUSABLES_UI_CONSTRAINTS.md` | 移动端 UI 组件库约束 |
| `xoassets-app/docs/MOBILE_API_INTEGRATION.md` | 移动端接口接入约束 |
| `xoassets-app/README.md` | 移动端说明 |

## 移动端技术栈

```text
React Native
TypeScript
Expo
Expo Router
NativeWind
React Native Reusables
src/components/ui
Zustand
TanStack Query
Axios
React Hook Form
Zod
expo-secure-store
@react-native-async-storage/async-storage
react-native-svg
react-native-reanimated
```

## UI 风格

- 移动端 UI 以 React Native Reusables 为准。
- NativeWind 是样式基础。
- `src/components/ui` 是项目唯一 UI 组件出口。
- 不直接使用 Web 版 shadcn/ui 组件。
- 不继续使用旧 Uiverse / Web shadcn 风格作为移动端主标准。
- 不引入 React Native Paper / NativeBase / UI Kitten 等与当前风格冲突的大型主题型 UI 库。

## 快速启动

后端：

```bash
cd xoassets-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn spring-boot:run
```

Web：

```bash
cd xoassets-web
npm install
npm run dev
```

移动端：

```bash
cd xoassets-app
npm install
npm run start
```

接口文档：

```text
http://localhost:8080/doc.html
```

测试账号：

```text
demo / xoassets123
```
