# XOAssets App Codex 协作规范

## 1. 项目定位

本目录是 XOAssets / 小〇财迹 React Native 新版移动端 App。

定位：

```text
个人资产驾驶舱 + 记账 + 账户管理 + 投资管理 + 预算管理 + AI 财务分析
```

目标平台：

- Android
- iOS

本项目从零开始设计，不复用旧移动端页面代码，不复用 Web 页面代码。

## 2. 文档优先级

移动端任务必须先读取：

1. `xoassets-app/AGENTS.md`
2. `xoassets-app/MOBILE_APP_PHASES.md`
3. `xoassets-app/docs/MOBILE_UI_DESIGN_SYSTEM.md`
4. `xoassets-app/docs/MOBILE_API_INTEGRATION.md`
5. 后端接口文档 / Swagger / Knife4j
6. 当前代码实现

## 3. 技术栈约束

必须使用：

| 类型 | 技术 |
|---|---|
| App 框架 | React Native |
| 语言 | TypeScript |
| 开发框架 | Expo |
| 路由 | Expo Router |
| 样式 | NativeWind + XO Design System |
| 服务端状态 | TanStack Query |
| 本地 UI 状态 | Zustand |
| 网络请求 | Axios |
| 表单 | React Hook Form |
| 校验 | Zod |
| Token 安全存储 | expo-secure-store |
| 普通配置存储 | AsyncStorage |
| 图标 / SVG | react-native-svg |
| 动效 | react-native-reanimated |
| 后端访问 | HTTP API |

第一版禁止引入：

| 禁止项 | 原因 |
|---|---|
| SQLite | 第一版不做离线数据库 |
| WatermelonDB | 第一版不做离线数据库 |
| Realm | 第一版不做离线数据库 |
| TypeORM | App 不直连数据库 |
| Prisma Client | App 不直连数据库 |
| WebView 套壳 | 本项目是原生体验 App |
| App 直连 MySQL | App 只能通过后端 API 访问数据 |
| 大型 UI 组件库 | UI 使用自研 XO Design System |
| Web 版 shadcn/ui | 只能参考视觉，不能直接使用 |
| 一次性实现所有业务 | 必须按阶段推进 |

## 4. 数据访问规则

正确架构：

```text
React Native App
  ↓ HTTP API
Spring Boot 后端
  ↓
MySQL
```

规则：

- App 不允许直接连接 MySQL。
- MySQL 只存在于后端服务中。
- App 只能通过 HTTP API 访问业务数据。
- 第一版不做离线数据库。
- 第一版不做本地业务数据长期缓存。
- 后端返回的数据是业务数据唯一可信来源。
- TanStack Query 只做接口缓存，不做离线权威数据源。

## 5. 本地存储规则

SecureStore 只能保存敏感信息：

```text
accessToken
refreshToken
```

AsyncStorage 只能保存普通配置：

```text
hideAmount
themeMode
lastSelectedAccountId
lastSelectedCategoryId
```

第一版不允许本地长期保存完整业务数据：

```text
流水明细
账户资产
投资持仓
预算明细
AI 报告
历史收益
资产快照
```

## 6. UI 规则入口

详细 UI 规范查看：

```text
xoassets-app/docs/MOBILE_UI_DESIGN_SYSTEM.md
xoassets-app/docs/SHADCN_UI_STYLE_GUIDE.md
xoassets-app/docs/MOBILE_ICON_ASSETS_GUIDE.md
```

强制规则：

- UI 使用 React Native 原生组件 + NativeWind + 自研 XO Design System。
- 视觉统一参考 shadcn/ui 的设计语言：简洁、卡片化、弱边框、低饱和、清晰层级。
- 参考 `https://ui.shadcn.com/docs/installation`，但不在 React Native 项目中直接运行 Web 安装命令。
- 不直接使用 shadcn/ui Web 组件。
- 不使用 DOM、CSS Modules、浏览器专属 API。
- 所有颜色、圆角、间距、字体、阴影、图标路径必须集中维护。
- 页面容器优先使用 `XoPage`。
- 卡片优先使用 `XoCard`。
- 金额展示必须使用 `XoMoneyText`。
- 图标必须使用 `XoIcon`。
- 主按钮必须使用 `XoButton`。
- 输入框必须使用 `XoTextField`。
- 不允许页面内到处写死颜色、圆角和字号。
- 不允许将 Web 页面缩小后搬到移动端。

## 7. 推荐目录结构

```text
app
├── _layout.tsx
├── index.tsx
├── (auth)
│   ├── login.tsx
│   └── register.tsx
├── (tabs)
│   ├── _layout.tsx
│   ├── home.tsx
│   ├── ledger.tsx
│   ├── investment.tsx
│   └── profile.tsx
├── transaction
│   └── edit.tsx
├── investment
│   └── trade.tsx
├── budget.tsx
├── report.tsx
└── settings.tsx

src
├── app
│   ├── providers.tsx
│   └── query-client.ts
├── core
│   ├── constants
│   ├── design
│   ├── errors
│   ├── network
│   ├── storage
│   ├── utils
│   └── components
├── features
│   ├── auth
│   ├── home
│   ├── ledger
│   ├── transaction
│   ├── account
│   ├── investment
│   ├── budget
│   ├── report
│   ├── profile
│   └── settings
└── shared
    ├── types
    └── enums
```

## 8. 状态管理规则

- 服务端状态使用 TanStack Query。
- 本地 UI 状态使用 Zustand。
- 表单状态使用 React Hook Form。
- 表单校验使用 Zod。
- 不要把所有状态塞进一个全局 store。
- 不要把接口缓存结果长期持久化为权威数据。

## 9. 网络层规则

必须包含：

```text
apiClient
authInterceptor
errorHandler
apiResponse
appException
```

规则：

- 统一配置 baseURL。
- 统一配置超时时间。
- 请求自动添加 Token。
- 统一处理 401。
- 统一处理业务异常。
- 页面不允许直接调用 Axios。
- 页面必须通过 feature API / repository 间接调用接口。

## 10. 构建与验证

```bash
npm install
npm run typecheck
npm run start
npm run android
npm run ios
```

如果无法验证，必须说明原因和剩余风险。
