# 移动端接口接入 AI 约束

## 1. 适用范围

本文件约束 `xoassets-app` React Native App 的接口接入、鉴权、错误处理、数据缓存和类型定义。

移动端只能通过 XOAssets 后端 HTTP API 访问业务数据。

## 2. 核心原则

```text
React Native App
  ↓ HTTP API
Spring Boot Backend
  ↓
MySQL / Redis / 第三方服务
```

强制规则：

- App 不允许直接连接 MySQL。
- App 不允许直接访问 Redis。
- App 不允许直接调用第三方行情源。
- App 不允许直接调用第三方汇率源。
- App 不允许保存对象存储密钥。
- App 不允许把后端业务计算逻辑搬到前端。
- 后端返回的数据是业务展示唯一可信来源。

## 3. 网络层结构

推荐结构：

```text
src/core/network
├── apiClient.ts
├── apiResponse.ts
├── authInterceptor.ts
├── errorHandler.ts
└── appException.ts
```

业务接口结构：

```text
src/features/<feature>/api
├── <feature>Api.ts
└── <feature>Types.ts
```

页面调用规则：

```text
Screen
  ↓ hook / query
feature api
  ↓
core/network/apiClient
```

禁止页面中直接散写 `axios.get(...)`。

## 4. API Base URL

本地后端默认：

```text
http://localhost:8080
```

Android 模拟器访问宿主机：

```text
http://10.0.2.2:8080
```

真机调试：

```text
http://<电脑局域网IP>:8080
```

环境变量建议：

```text
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080
EXPO_PUBLIC_APP_ENV=dev
```

规则：

- 只允许 `EXPO_PUBLIC_*` 保存非敏感配置。
- 生产密钥、数据库账号、对象存储密钥不得写入 App。

## 5. 鉴权规则

请求头统一由网络层追加。

规则：

- 页面不直接拼接认证 Header。
- 登录凭证类敏感信息使用 `expo-secure-store`。
- 普通配置使用 `AsyncStorage`。
- 未登录或登录失效时统一清理本地登录态并跳转登录页。
- 不允许每个页面单独处理未登录逻辑。

## 6. 响应与错误处理

必须统一处理：

- HTTP 状态码错误。
- 后端业务错误。
- 网络超时。
- 无网络。
- 未登录。
- 无权限。
- 服务端异常。

页面层只拿到适合展示的信息，不能直接展示敏感异常。

错误提示规则：

| 场景 | 处理 |
|---|---|
| 表单校验失败 | 字段下方提示 |
| 登录失败 | 表单整体错误提示 |
| 列表加载失败 | 页面错误态 |
| 提交失败 | Toast / 文案提示 |
| 未登录 | 清理本地登录态并跳转登录 |

## 7. TanStack Query 使用规则

服务端状态统一使用 TanStack Query。

要求：

- 查询接口使用 `useQuery`。
- 新增、编辑、删除使用 `useMutation`。
- mutation 成功后 invalidate 相关 query。
- 不要把接口数据复制到 Zustand 作为权威数据。
- 不做离线持久化缓存。

禁止：

- 页面内手写多个 `useEffect + axios` 管理服务端状态。
- 把后端返回列表长期写入 AsyncStorage。
- 本地伪造业务统计结果替代后端数据。

## 8. Long ID 与金额精度

规则：

- 后端 Long ID 在移动端必须使用 `string`。
- 不允许使用 JavaScript `number` 保存业务 ID。
- 金额展示必须格式化。
- 金额、资产、收益计算以后端返回字段为准。
- 后端返回 `null` 时展示 `--` 或 `暂无`，不能用 `0` 冒充。

## 9. 当前移动端优先接口

认证：

```text
POST /api/auth/login
POST /api/auth/register
GET  /api/auth/me
```

首页：

```text
GET /api/snapshots/latest
GET /api/dashboard/overview
GET /api/budgets/summary
GET /api/reports
GET /api/transactions
```

基础业务：

```text
GET  /api/accounts
GET  /api/accounts/overview
GET  /api/accounts/{id}/ledger
GET  /api/accounts/{id}/flow-statistics
GET  /api/categories
GET  /api/transactions
POST /api/transactions
PUT  /api/transactions/{id}
DELETE /api/transactions/{id}
```

投资：

```text
GET  /api/investments/overview
GET  /api/investments/holdings
GET  /api/investments/trend
GET  /api/investments/daily-profit
GET  /api/investments/holdings/{id}/profit-calendar
GET  /api/investment-transactions
POST /api/investment-transactions
PUT  /api/investment-transactions/{id}/revoke
POST /api/quotes/refresh-batch
GET  /api/accounts
GET  /api/assets/lookup
GET  /api/assets/search
POST /api/assets
```

预算 / 报告：

```text
GET  /api/budgets
GET  /api/budgets/summary
GET  /api/reports
POST /api/reports/generate-preview
```

## 10. AI 执行规则

Codex / AI 接口相关任务必须：

1. 先读根 `AGENTS.md`。
2. 再读 `xoassets-app/AGENTS.md`。
3. 再读本文件。
4. 再读 `docs/API_CONTRACTS.md`。
5. 不确定接口字段时优先查看后端 Controller、DTO、Swagger / Knife4j。
6. 不允许凭空编接口路径。
7. 不允许凭空编字段名。
8. 不允许把 mock 数据当真实接口。
9. 不允许绕过统一网络层。
10. 修改后至少运行 `npm run typecheck`。
