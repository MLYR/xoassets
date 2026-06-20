# 接口约定与联调状态

## 1. 通用接口路径

| 模块 | 路径 |
|---|---|
| 登录注册 | `/api/auth/**` |
| 当前用户 | `/api/auth/me` |
| 首页 | `/api/dashboard/**` |
| 流水 | `/api/transactions/**` |
| 账户 | `/api/accounts/**` |
| 分类 | `/api/categories/**` |
| 图表统计 | `/api/statistics/**` |
| CSV 导出 | `/api/export/**` |
| 预算 | `/api/budgets/**` |
| 资产目标 | `/api/goals/**` |
| AI 报告 | `/api/reports/**` |
| 资产快照 | `/api/snapshots/**` |

## 2. 投资接口

| 模块 | 路径 |
|---|---|
| 公共投资资产 | `/api/assets/**` |
| 投资聚合 | `/api/investments/**` |
| 持仓 | `/api/holdings/**` |
| 投资交易 | `/api/investment-transactions/**` |
| 手动行情 | `/api/quotes/**` |
| 汇率展示 | `/api/exchange-rates/**` |

规则：

- 第三方行情只能由后端调用。
- 前端和移动端只调用 XOAssets `/api/quotes/**`。
- 前端和移动端只读后端缓存汇率，不直连第三方汇率源。

## 3. 账户详情接口

| 功能 | 接口 |
|---|---|
| 账户资金明细 | `/api/accounts/{id}/ledger` |
| 账户详情统计 | `/api/accounts/{id}/flow-statistics` |
| 余额修正 | `/api/accounts/{id}/balance-adjustments` |
| 账户余额曲线 | `/api/accounts/{id}/balance-trend` |

账户详情只展示：

- 当前余额
- 累计流入
- 累计流出
- 余额修正
- 余额曲线
- 支出分类

账户详情不再展示投资资金流向图。

## 4. 接口变更规则

- 接口新增或调整时，同步考虑 Swagger / Knife4j 文档。
- 前端和移动端不得绕过后端接口直接访问数据库。
- 后端返回金额字段时应保持精度。
- 前端和移动端展示金额时使用统一格式化。
- 后端错误信息可展示给前端，但不能暴露敏感信息或第三方原始异常。
- 后端 Long ID 以字符串返回，前端和移动端接口类型使用 `string` 保存和回传 ID。

## 5. Web 已接入接口

### 认证

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`

### 账户管理

- `GET /api/accounts`
- `POST /api/accounts`
- `PUT /api/accounts/{id}`
- `DELETE /api/accounts/{id}`
- `POST /api/accounts/{id}/balance-adjustments`
- `GET /api/accounts/{id}/ledger`
- `GET /api/accounts/{id}/flow-statistics`

### 分类管理

- `GET /api/categories`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`
- `PUT /api/categories/{id}/status`

### 记账流水

- `GET /api/transactions`
- `POST /api/transactions`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`

### 投资持仓与交易

- `GET /api/assets/lookup`
- `GET /api/holdings`
- `GET /api/holdings/summary`
- `GET /api/holdings/{id}/detail`
- `POST /api/holdings`
- `PUT /api/holdings/{id}`
- `POST /api/investment-transactions`
- `GET /api/investment-transactions`
- `GET /api/investment-transactions/fund-confirm-preview`
- `PUT /api/investment-transactions/{id}/revoke`
- `POST /api/quotes/manual`
- `POST /api/quotes/refresh`
- `POST /api/quotes/refresh-batch`

### 预算、快照、目标、报告

- `GET /api/budgets`
- `POST /api/budgets`
- `PUT /api/budgets/{id}`
- `DELETE /api/budgets/{id}`
- `GET /api/budgets/summary`
- `GET /api/snapshots/latest`
- `GET /api/snapshots/trend`
- `POST /api/snapshots/generate-today`
- `POST /api/snapshots/generate?snapshotDate=yyyy-MM-dd`
- `POST /api/snapshots/rebuild?startDate=yyyy-MM-dd&endDate=yyyy-MM-dd`
- `GET /api/goals`
- `POST /api/goals`
- `PUT /api/goals/{id}`
- `DELETE /api/goals/{id}`
- `GET /api/goals/summary`
- `GET /api/reports`
- `GET /api/reports/{id}`
- `POST /api/reports/generate-preview`

## 6. 移动端已接入接口

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/me`
- `GET /api/snapshots/latest`
- `GET /api/dashboard/overview`
- `GET /api/budgets/summary`
- `GET /api/reports`
- `POST /api/reports/generate-preview`
- `GET /api/transactions`
- `POST /api/transactions`
- `GET /api/accounts`
- `GET /api/accounts/overview`
- `POST /api/accounts`
- `PUT /api/accounts/{id}`
- `GET /api/accounts/{id}/ledger`
- `GET /api/accounts/{id}/flow-statistics`
- `POST /api/accounts/{id}/balance-adjustments`
- `GET /api/categories`
- `GET /api/budgets`
- `POST /api/budgets`
- `PUT /api/budgets/{id}`
- `GET /api/investments/overview`
- `GET /api/investments/holdings`
- `GET /api/investments/trend`
- `GET /api/investments/daily-profit`
- `GET /api/investments/holdings/{id}/profit-calendar`
- `GET /api/investment-transactions`
- `POST /api/investment-transactions`
- `POST /api/quotes/refresh-batch`
- `GET /api/assets/lookup`
- `GET /api/assets/search`
- `POST /api/assets`

当前后端暂无：

- `/api/auth/logout`
- `/api/auth/refresh-token`

移动端退出登录当前仅清除本地 token，refresh token 字段先做兼容预留。
