# 小〇财迹

小〇财迹是面向个人用户的资产管理与财务复盘工具。当前仓库包含 Vue3 前端和 Spring Boot 后端 MVP。

## 项目结构

```text
.
├── xoassets-web      Vue3 + TypeScript + Vite 前端
├── xoassets-server   Java 17 + Spring Boot 3 后端
├── AGENTS.md         AI 协作规范
└── 小〇财迹_产品需求_设计_开发文档.md
```

## 当前进度

- 前端：已重建为 Vue3、Element Plus、ECharts、Pinia、Vue Router 项目；登录、注册、用户中心、账户管理、分类管理、记账流水、投资持仓、预算管理、首页、数据分析、资产目标和 AI 报告模板页已接入后端接口。
- 后端：已创建 Spring Boot MVP，覆盖登录注册、账户、分类、流水、首页统计、基础图表统计、投资持仓手动维护、行情刷新、预算管理、资产目标和 AI 报告模板生成。
- 体验稳定性：核心业务页已补齐空状态、删除二次确认、金额输入大于 0 校验、后端错误 message 展示和统一 loading 状态。
- 暂不做：自动同步银行卡 / 支付宝 / 微信、AI 报告真实调用、自动交易或投资建议；虚拟货币行情已支持 CoinGecko 手动刷新和定时刷新，股票 / 基金自动行情暂不接入。

## 前后端联调状态

- 认证：`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me` 已在前端封装为 `authApi`。
- 登录态：前端使用 Axios 请求封装，JWT 存入 `localStorage`，请求自动携带 `Authorization: Bearer <token>`。
- 路由守卫：没有 token 访问业务页会跳转 `/login`，401 响应会清理 token 并回到登录页。
- 账户管理：`GET /api/accounts`、`POST /api/accounts`、`PUT /api/accounts/{id}`、`DELETE /api/accounts/{id}` 已接入账户页，编辑账户时可手动校准当前余额；`GET /api/accounts/{id}/ledger` 和 `/flow-statistics` 已接入账户详情页。
- 分类管理：`GET /api/categories`、`POST /api/categories`、`PUT /api/categories/{id}`、`DELETE /api/categories/{id}`、`PUT /api/categories/{id}/status` 已接入分类页。
- 记账流水：`GET /api/transactions`、`POST /api/transactions`、`PUT /api/transactions/{id}`、`DELETE /api/transactions/{id}` 已接入记账页，支持分页和流水图片。
- 投资持仓：`GET /api/holdings`、`GET /api/holdings/summary`、`POST /api/holdings`、`PUT /api/holdings/{id}`、`DELETE /api/holdings/{id}`、`POST /api/investment-transactions`、`GET /api/investment-transactions`、`POST /api/quotes/manual`、`POST /api/quotes/refresh` 已接入投资页；前端只暴露持仓概念，`xo_asset` 作为后端内部行情基础表。
- 投资展示：投资主页只展示总投资 / 基金 / 股票 / 虚拟货币统计和图表，持仓表格、买入卖出、编辑删除、价格刷新、收益分析等操作集中在 `/investments/details`。
- 投资交易：买入必须选择扣款账户并扣减余额，卖出必须选择到账账户并增加余额；买入 / 卖出不写入普通流水，不计入生活收支统计。
- 投资撤销：`PUT /api/investment-transactions/{id}/revoke` 会反向恢复资金账户和持仓，撤销记录仍保留在投资交易中。
- 投资精度：投资数量、手续费、成本、市值、盈亏和收益率统一按 4 位小数计算；行情价格快照保留 8 位，CRYPTO 当前价至少展示 6 位，FUND / STOCK 当前价展示 4 位。持仓列表的 `marketValue` 始终由后端使用同一个 `latestPrice` 计算，前端不使用格式化价格反算市值。
- 行情缓存：持仓列表优先使用 `xo_asset_price` 最近快照；CRYPTO 5 分钟内、STOCK 15 分钟内、FUND 1 天内不重复刷新，MANUAL 价格不过期。
- 预算管理：`GET /api/budgets`、`POST /api/budgets`、`PUT /api/budgets/{id}`、`DELETE /api/budgets/{id}`、`GET /api/budgets/summary` 已接入预算页。
- 首页和统计：`GET /api/dashboard/overview` 返回账户、流水、投资和预算聚合指标；`/api/statistics/*` 返回净资产趋势、收支趋势、分类支出、资产分布、投资盈亏和预算进度。
- 资产目标：`GET /api/goals`、`POST /api/goals`、`PUT /api/goals/{id}`、`DELETE /api/goals/{id}`、`GET /api/goals/summary` 已接入目标页。
- AI 报告：`GET /api/reports`、`GET /api/reports/{id}`、`POST /api/reports/generate-preview` 已接入报告页，当前只生成模板化财务复盘，不调用真实 AI，不提供投资买卖建议。
- CSV 导出：`GET /api/export/account-ledger`、`/transactions`、`/investment-transactions` 已接入账户详情、流水页和投资明细页，导出文件带 UTF-8 BOM。
- ID 处理：后端 Long ID 以字符串返回，前端接口类型使用 `string` 保存和回传 ID，避免 JavaScript 数字精度丢失。
- 本地开发：前端 Vite 将 `/api` 代理到 `http://localhost:8080`。

## 前端命令

```bash
cd xoassets-web
npm install
npm run dev
npm run build
```

## 后端命令

```bash
cd xoassets-server
mysql -uroot -p < src/main/resources/db/schema.sql
mvn spring-boot:run
```

后端按 Java 17 编译。若本机有多个 JDK，建议显式指定：

```bash
cd xoassets-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -DskipTests compile
```

后端接口文档：

```text
http://localhost:8080/doc.html
```

## 本地开发启动

1. 初始化数据库：

```bash
cd xoassets-server
mysql -uroot -p < src/main/resources/db/schema.sql
mysql -uroot -p xoassets < src/main/resources/db/dev-data.sql
```

2. 启动后端：

```bash
cd xoassets-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn spring-boot:run
```

3. 启动前端：

```bash
cd xoassets-web
npm install
npm run dev
```

访问地址：

- 前端开发地址：`http://localhost:5173`
- 后端接口文档：`http://localhost:8080/doc.html`
- 开发测试账号：`demo / xoassets123`

## Docker 启动

Docker Compose 会启动 MySQL 8、Spring Boot 后端和 Nginx 前端，并在 MySQL 首次初始化时自动执行 `schema.sql` 和 `dev-data.sql`。

```bash
docker compose up -d
```

访问地址：

- 前端：`http://localhost:8088`
- 后端：`http://localhost:8080`
- Knife4j：`http://localhost:8080/doc.html`
- MySQL：`localhost:3306`，账号密码 `root / root`
- 开发测试账号：`demo / xoassets123`

如果需要重新导入初始化数据，先删除数据卷再启动：

```bash
docker compose down -v
docker compose up -d
```

## 开发测试数据

`xoassets-server/src/main/resources/db/dev-data.sql` 包含：

- 测试用户、默认账户、收入 / 支出分类。
- 收入、支出、转账、退款流水，账户余额与流水影响自洽。
- DOGE 和基金 A 投资资产、持仓、买入 / 卖出记录、行情价格快照。
- 月度总预算、餐饮分类预算、资产目标、AI 模板报告。

关键验收口径：

- DOGE：`quantity = 881.3220`，`latestPrice = 0.72432000`，`marketValue = 638.3592`。
- 投资收益分析：持仓接口返回最新价、昨价、前日价、今日收益、昨日收益、总收益、收益率和回本涨幅；缺少历史价格时页面展示“暂无”。
- 预算：5 月餐饮支出 `86.5000 - 20.0000 = 66.5000`，转账不进入预算。
- 账户：银行卡 `21500.0000`，支付宝 `1933.5000`，与初始化余额和流水变更一致。

## MVP 验收清单

- 登录 `demo / xoassets123` 后能进入首页。
- 首页总资产 = 账户余额 + 投资市值，净资产当前等于总资产。
- 记账新增收入后账户余额增加，新增支出后账户余额减少，转账只改变账户分布。
- 账户详情能展示普通收支、转账、退款、投资买入和投资卖出的资金明细，并按当前账户方向计算累计流入、流出和净流入。
- 删除流水后账户余额按原流水影响反向恢复。
- 预算统计只计算支出和退款，转账不计入预算。
- 投资持仓市值使用后端返回的 `latestPrice` 计算，DOGE 当前价至少显示 6 位小数。
- 投资买入扣减资金账户余额，卖出增加资金账户余额，已实现盈亏只进入投资交易记录。
- 投资交易撤销后账户余额和持仓数量 / 成本反向恢复，已撤销交易不参与账户资金明细汇总。
- 账户详情、普通流水和投资交易可导出 CSV，Excel 打开中文不乱码。
- 数据分析页收支趋势排除转账，投资盈亏使用最新价格快照。
- 用户 A 不能查看或修改用户 B 的账户、分类、流水、持仓、预算、目标。

## 当前暂不支持

- 银行卡 / 支付宝 / 微信自动同步。
- 股票 / 基金自动行情。
- 真实 AI 调用。
- 投资建议。
- 自动交易。

## 测试命令

```bash
cd xoassets-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test

cd ../xoassets-web
npm run build
```

## 后端结构约定

- 业务层使用 `service` 接口 + `service/impl` 实现类结构。
- Controller 和跨模块调用优先依赖 service 接口。
- MyBatis-Plus 3.5.9 的分页拦截器需要保留 `mybatis-plus-jsqlparser` 依赖。
