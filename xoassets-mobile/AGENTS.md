# XOAssets Mobile V2 AI 协作规范

## 1. 项目定位

本目录是 XOAssets / 小〇财迹 新版移动端 Flutter App。

定位：

```text
个人资产驾驶舱 + 记账 + 账户管理 + 投资管理 + 预算管理 + AI 财务分析
```

目标平台：

- Android
- iOS

本项目从零开始设计，不复用旧 uni-app 移动端代码，不复用 Web 页面代码。

## 2. 文档优先级

移动端任务必须先读取：

1. `xoassets-mobile/AGENTS.md`
2. `xoassets-mobile/MOBILE_APP_PHASES.md`
3. `xoassets-mobile/docs/MOBILE_UI_DESIGN_SYSTEM.md`
4. `xoassets-mobile/docs/MOBILE_API_INTEGRATION.md`
5. 后端接口文档 / Swagger / Knife4j
6. 当前代码实现

如果文档冲突：

- 先说明冲突点。
- 不要擅自修改大范围代码。
- 采用最小必要修改。
- 高风险场景先询问用户。

## 3. 技术栈约束

必须使用：

| 类型 | 技术 |
|---|---|
| App 框架 | Flutter |
| 语言 | Dart |
| UI 基础 | Flutter Material 3 |
| UI 体系 | 自研 XO Design System |
| 状态管理 | Riverpod |
| 路由 | go_router |
| 网络请求 | Dio |
| Token 安全存储 | flutter_secure_storage |
| 普通配置存储 | shared_preferences |
| 后端访问 | HTTP API |

第一版禁止：

| 禁止项 | 原因 |
|---|---|
| SQLite | 第一版不做离线 |
| Drift | 第一版不做离线 |
| Hive | 第一版不做本地数据库 |
| Isar | 第一版不做本地数据库 |
| 大型 UI 组件库 | UI 使用 Material 3 + 自研 XO Design System |
| WebView 套壳 | 本项目是 Flutter App |
| App 直连 MySQL | App 只能通过后端 API 访问数据 |
| 一次性实现所有业务 | 必须按阶段推进 |

## 4. 数据访问规则

正确架构：

```text
Flutter App
  ↓ HTTP API
Spring Boot 后端
  ↓
MySQL
```

规则：

- Flutter App 不允许直接连接 MySQL。
- MySQL 只存在于后端服务中。
- App 只能通过 HTTP API 访问业务数据。
- 第一版不做离线能力。
- 第一版不引入本地数据库。
- 第一版不做本地业务数据长期缓存。
- 后端返回的数据是业务数据唯一可信来源。

## 5. 本地存储规则

Secure Storage 只能保存：

```text
accessToken
refreshToken
```

SharedPreferences 只能保存：

```text
hideAmount
darkMode
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
xoassets-mobile/docs/MOBILE_UI_DESIGN_SYSTEM.md
xoassets-mobile/docs/MOBILE_ICON_ASSETS_GUIDE.md
```

强制规则：

- UI 使用 Flutter Material 3 + 自研 XO Design System。
- 所有颜色、圆角、间距、字体、阴影、图标路径必须集中维护。
- 页面容器优先使用 `XoPage`。
- 卡片优先使用 `XoCard`。
- 金额展示必须使用 `XoMoneyText`。
- 图标必须使用 `XoIcon`。
- 主按钮必须使用 `XoButton`。
- 输入框必须使用 `XoTextField`。
- 不允许页面内到处写死颜色、圆角和字号。
- 不允许直接套大型 UI 库。
- 不允许将 Web 页面缩小后搬到移动端。

## 7. 推荐目录结构

```text
lib
├── main.dart
├── app
│   ├── app.dart
│   ├── router.dart
│   ├── routes.dart
│   └── theme.dart
├── core
│   ├── constants
│   ├── design
│   ├── errors
│   ├── network
│   ├── storage
│   ├── utils
│   └── widgets
├── features
│   ├── splash
│   ├── auth
│   ├── main
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
    ├── models
    └── enums
```

## 8. 路由规则

必须使用 `go_router`。

基础路由：

```text
/splash
/login
/register
/main
/transaction/edit
/investment/trade
/budget
/report
/settings
```

## 9. 底部导航规则

底部导航固定为：

```text
首页
记账
中间悬浮 +
投资
我的
```

中间 `+` 点击后展示底部操作面板：

```text
记一笔
转账
投资交易
新增账户
新增预算
```

## 10. 状态管理规则

必须使用 Riverpod。

至少包含：

```text
authProvider
appSettingsProvider
mainTabProvider
```

## 11. 网络层规则

必须使用 Dio。

需要包含：

```text
ApiClient
DioProvider
AuthInterceptor
LogInterceptor
ApiResponse
AppException
ErrorHandler
```

规则：

- 统一配置 `baseUrl`。
- 统一配置超时时间。
- 请求自动添加 Token。
- 统一处理 401。
- 统一处理业务异常。
- 页面不允许直接调用 Dio。
- 页面必须通过 Repository / ApiClient 间接调用接口。
- 注册成功后如后端未直接返回 Token，必须复用登录接口建立会话，不允许前端伪造 Token。

## 12. 页面规则

详细页面范围查看：

```text
xoassets-mobile/docs/MOBILE_PRODUCT_SPEC.md
```

长期原则：

- 首页是资产驾驶舱，总资产必须最突出。
- 记账页使用日历视图。
- 投资页独立，不要做成交易所。
- AI 报告页不是聊天页。
- 设置页不要堆业务功能。

## 13. 阶段开发规则

所有阶段以 `MOBILE_APP_PHASES.md` 为准。

禁止：

```text
越阶段实现
一次性实现所有业务
引入未批准依赖
重写无关模块
删除无关文件
修改后端代码
修改 Web 端代码
```

## 14. 构建与验证

```bash
flutter pub get
flutter analyze
flutter test
flutter run
flutter build apk
```

每次完成代码修改后必须说明：

```text
flutter pub get 是否成功
flutter analyze 是否通过
flutter test 是否运行
Android 是否可运行
iOS 是否可运行，若当前环境支持
```

如果无法验证，必须说明原因和剩余风险。
