# XOAssets App Codex 协作规范

## 1. 项目定位

本目录是 XOAssets / 小〇财迹 React Native 新版移动端 App。

定位：

```text
个人资产驾驶舱 + 记账 + 账户管理 + 投资管理 + 预算管理 + AI 财务分析
```

目标平台：Android、iOS。

本项目从零开始设计，不复用旧移动端页面代码，不复用 Web 页面代码。

## 2. 文档优先级

移动端任务必须先读取：

1. 根目录 `AGENTS.md`
2. `xoassets-app/AGENTS.md`
3. `xoassets-app/docs/REACT_NATIVE_REUSABLES_UI_CONSTRAINTS.md`
4. `xoassets-app/docs/MOBILE_PRODUCT_SPEC.md`
5. `xoassets-app/MOBILE_APP_PHASES.md`
6. `xoassets-app/docs/MOBILE_API_INTEGRATION.md`
7. `xoassets-app/docs/MOBILE_ICON_ASSETS_GUIDE.md`
8. `docs/BUSINESS_RULES.md`
9. `docs/API_CONTRACTS.md`
10. 后端接口文档 / Swagger / Knife4j
11. 当前代码实现

冲突处理：

- 移动端 UI 以 `REACT_NATIVE_REUSABLES_UI_CONSTRAINTS.md` 为准。
- 移动端接口接入以 `MOBILE_API_INTEGRATION.md` 和 `docs/API_CONTRACTS.md` 为准。
- 业务计算口径以 `docs/BUSINESS_RULES.md` 和后端实现为准。
- 旧文档或旧提示词中的 Web shadcn / Uiverse / 自研未落地 XO Design System 风格不再作为移动端主标准。

## 3. 技术栈约束

| 类型 | 技术 |
|---|---|
| App 框架 | React Native |
| 语言 | TypeScript |
| 开发框架 | Expo |
| 路由 | Expo Router |
| 样式 | NativeWind |
| UI 组件基准 | React Native Reusables |
| 项目 UI 出口 | `src/components/ui` |
| 服务端状态 | TanStack Query |
| 本地 UI 状态 | Zustand |
| 网络请求 | Axios |
| 表单 | React Hook Form |
| 校验 | Zod |
| 安全存储 | expo-secure-store |
| 普通配置存储 | AsyncStorage |
| 图标 / SVG | react-native-svg |
| 动效 | react-native-reanimated |
| 后端访问 | HTTP API |

第一版禁止引入：

| 禁止项 | 原因 |
|---|---|
| SQLite / WatermelonDB / Realm | 第一版不做离线数据库 |
| TypeORM / Prisma Client | App 不直连数据库 |
| WebView 套壳 | 本项目是原生体验 App |
| App 直连 MySQL | App 只能通过后端 API 访问数据 |
| React Native Paper / NativeBase / UI Kitten 等大型主题型 UI 库 | UI 以 React Native Reusables 为基准 |
| Web 版 shadcn/ui 组件 | 不能直接用于 React Native |
| 一次性实现所有业务 | 必须按阶段推进 |

## 4. 数据访问规则

```text
React Native App
  ↓ HTTP API
Spring Boot 后端
  ↓
MySQL
```

规则：

- App 只能通过 HTTP API 访问业务数据。
- App 不允许直接连接数据库。
- 第一版不做离线数据库。
- 第一版不做本地业务数据长期缓存。
- 后端返回的数据是业务数据唯一可信来源。
- TanStack Query 只做接口缓存，不做离线权威数据源。

## 5. 本地存储规则

- 安全存储只保存登录凭证类敏感信息。
- AsyncStorage 只保存普通配置，例如主题、金额隐藏、最近选择项。
- 第一版不允许本地长期保存完整业务数据，例如流水、账户、持仓、预算、报告、快照。

## 6. UI 规则入口

详细 UI 规范查看：

```text
xoassets-app/docs/REACT_NATIVE_REUSABLES_UI_CONSTRAINTS.md
xoassets-app/docs/MOBILE_ICON_ASSETS_GUIDE.md
```

强制规则：

- UI 使用 React Native Reusables + NativeWind + `src/components/ui`。
- React Native Reusables 是移动端组件和视觉基准。
- `src/components/ui` 是项目唯一 UI 组件出口。
- 页面必须优先从 `@/components/ui` 引入 Button、Card、Input、Text 等组件。
- 新增通用 UI 组件必须放入 `src/components/ui` 并从 `index.ts` 导出。
- 不直接使用 Web 版 shadcn/ui 组件。
- 不使用 DOM、CSS Modules、浏览器专属 API。
- 不允许页面内到处写死颜色、圆角和字号。
- 不允许将 Web 页面缩小后搬到移动端。

## 7. 推荐目录结构

```text
app
├── _layout.tsx
├── index.tsx
├── (auth)
├── (tabs)
├── transaction
├── investment
├── budget.tsx
├── report.tsx
└── settings.tsx

src
├── app
├── components/ui
├── core
├── features
└── shared
```

## 8. 状态管理规则

- 服务端状态使用 TanStack Query。
- 本地 UI 状态使用 Zustand。
- 表单状态使用 React Hook Form。
- 表单校验使用 Zod。
- 不要把所有状态塞进一个全局 store。
- 不要把接口缓存结果长期持久化为权威数据。

## 9. 网络层规则

详细规则查看：

```text
xoassets-app/docs/MOBILE_API_INTEGRATION.md
```

规则：

- 统一配置 baseURL。
- 统一配置超时时间。
- 请求自动添加认证信息。
- 统一处理未登录、业务异常和网络异常。
- 页面不允许直接调用 Axios。
- 页面必须通过 feature API / repository 间接调用接口。
- 不确定接口字段时查看后端 Controller / DTO / Swagger，不要凭空编字段。

## 10. 构建与验证

```bash
npm install
npm run typecheck
npm run start
npm run android
npm run ios
```

如果无法验证，必须说明原因和剩余风险。
