# 移动端接口接入说明

## 1. 基础规则

- App 只调用后端 API。
- App 不直接连接 MySQL。
- App 不直接调用第三方行情源。
- App 不直接调用第三方汇率源。
- App 不直接访问对象存储密钥。
- 第三方行情只能由后端调用。
- 第三方 AI 只能由后端调用。
- 后端 Long ID 在 App 中按字符串处理。
- 金额字段建议按字符串接收，避免精度问题。
- App 端展示金额时统一格式化。

## 2. 网络层

网络层位于：

```text
lib/core/network/
```

包含：

```text
ApiClient
dioProvider
AuthInterceptor
XoLogInterceptor
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

## 3. 默认 API Base URL

```text
http://localhost:8080/api
```

配置位置：

```text
lib/core/constants/api_constants.dart
```

Android 模拟器访问宿主机本地后端通常使用：

```text
http://10.0.2.2:8080/api
```

可通过以下参数覆盖：

```bash
--dart-define=XO_API_BASE_URL=<url>
```

## 4. 已接入接口

```text
POST /api/auth/login
POST /api/auth/register
GET  /api/auth/me
GET  /api/snapshots/latest
GET  /api/dashboard/overview
GET  /api/budgets/summary
GET  /api/reports
POST /api/reports/generate-preview
GET  /api/transactions
POST /api/transactions
GET  /api/accounts
GET  /api/categories
GET  /api/investments/overview
GET  /api/investments/holdings
POST /api/investment-transactions
POST /api/quotes/refresh-batch
```

## 5. 当前后端暂无接口

```text
/api/auth/logout
/api/auth/refresh-token
```

当前策略：

- 移动端退出登录仅清除本地 token。
- refresh token 字段先做兼容预留。

## 6. 认证状态

已完成：

- 登录页接入真实 `/api/auth/login`。
- 注册页接入真实 `/api/auth/register`。
- 注册成功后复用登录接口建立会话。
- 登录成功后将 accessToken 保存到 `flutter_secure_storage`。
- `AuthInterceptor` 自动为请求添加 `Authorization: Bearer <token>`。
- `GET /api/auth/me` 用于 App 启动后的登录态恢复。
- 401 / `40100` 会统一清理本地 token。
- 我的页展示当前登录用户，并提供退出登录入口。

## 7. 本次接口接入状态

已完成：

- 首页已接入资产快照、首页统计、预算汇总、AI 报告和最近流水接口。
- 记账页已接入流水列表。
- 流水录入页已接入账户、分类和新增流水接口。
- 投资页已接入投资总览、持仓列表和批量行情刷新接口。
- 投资交易页已接入新增投资交易接口。
- 设置页已支持 `system / light / dark` 主题模式，并通过 `shared_preferences` 持久化。

## 8. 下一阶段建议

- 账户管理页接入真实账户详情与新增账户。
- 分类管理页接入真实分类维护。
- 投资交易页补齐资产搜索 / 新建持仓流程，减少依赖既有持仓。
