# 小〇财迹后端

第一期 MVP 后端服务，基于 Java 17、Spring Boot 3、MyBatis-Plus、MySQL 8、JWT 和 Knife4j。

## 当前范围

- 用户注册、登录、当前用户
- 账户管理
- 分类管理
- 收支流水
- 首页统计和数据分析
- 资产快照、净资产趋势和每日快照任务
- 投资资产识别、投资持仓、投资交易、手动价格维护、虚拟货币 / 基金 / 股票行情刷新
- 预算管理和预算汇总
- 资产目标管理和目标汇总
- AI 报告模板生成和报告列表

暂不包含 AI 报告真实调用、银行卡 / 支付宝 / 微信自动同步、自动交易或投资建议；前端不直接请求第三方行情接口。

## 启动前准备

1. 创建数据库表：

```bash
mysql -uroot -p < src/main/resources/db/schema.sql
```

2. 配置数据库和 JWT：

```bash
export XOASSETS_DATASOURCE_URL='jdbc:mysql://localhost:3306/xoassets?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export XOASSETS_DB_USERNAME='root'
export XOASSETS_DB_PASSWORD='your-password'
export XOASSETS_JWT_SECRET='replace-with-at-least-32-bytes-secret-key'
export XOASSETS_JWT_EXPIRE_MINUTES='10080'
```

3. 按环境启动服务：

```bash
export XOASSETS_PROFILE='dev'
mvn spring-boot:run
```

`XOASSETS_PROFILE` 支持 `dev`、`test`、`prod`，未设置时默认 `dev`。日志由 `src/main/resources/logback-spring.xml` 控制：dev 输出业务、MyBatis、JDBC 等调试日志，test/prod 收敛框架日志；每日滚动日志默认写入 `logs/xoassets-server.log` 和 `logs/xoassets-server-error.log`，可通过 `XOASSETS_LOG_PATH`、`XOASSETS_LOG_MAX_HISTORY`、`XOASSETS_LOG_TOTAL_SIZE_CAP` 覆盖。

## 验证命令

项目按 Java 17 作为目标版本。若本机同时安装多个 JDK，建议显式指定 JDK 17 后再编译：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -DskipTests compile
```

## 接口文档

服务启动后访问：

```text
http://localhost:8080/doc.html
```
## XXL-JOB 调度中心

定时任务已迁移到 XXL-JOB handler。后端默认不启用 executor；需要可视化调度时，先启动 XXL-JOB Admin，再设置 `XXL_JOB_EXECUTOR_ENABLED=true`。

### 本地开发：Docker Admin + 本机 MySQL

本地开发以本机 MySQL 为准，先执行：

```bash
mysql -uroot -p < src/main/resources/db/xxl-job-init.sql
```

然后在仓库根目录启动只连接本机 MySQL 的 XXL-JOB Admin：

```bash
docker compose -f docker-compose.local-xxl.yml up -d
```

本机 MySQL 密码不是 `root` 时：

```bash
LOCAL_MYSQL_USERNAME=root LOCAL_MYSQL_PASSWORD=你的密码 docker compose -f docker-compose.local-xxl.yml up -d
```

访问：

```text
http://localhost:8081/xxl-job-admin
admin / 123456
```

IDEA 启动后端时，如需注册执行器，增加环境变量：

```bash
XXL_JOB_EXECUTOR_ENABLED=true
XXL_JOB_ADMIN_ADDRESSES=http://localhost:8081/xxl-job-admin
XXL_JOB_ACCESS_TOKEN=xoassets-xxl-job-local-token
XXL_JOB_EXECUTOR_APPNAME=xoassets-server
XXL_JOB_EXECUTOR_ADDRESS=http://host.docker.internal:10099/
XXL_JOB_EXECUTOR_PORT=10099
XOASSETS_REDIS_HOST=localhost
XOASSETS_REDIS_PORT=6379
XOASSETS_REDIS_PASSWORD=root
XOASSETS_REDIS_DATABASE=0
```

本地 XXL-JOB Admin 跑在 Docker 中、后端 executor 跑在宿主机 IDEA 中时，`XXL_JOB_EXECUTOR_ADDRESS` 必须使用 `http://host.docker.internal:10099/`。不要依赖自动探测的 `192.168.x.x` 地址，否则换 Wi-Fi / 网络后 Admin 可能继续调用旧 IP。`10099` 是 XOAssets 本地 executor 端口，用于避开其他项目常用的 `9999`。

如果本地 Redis 也是 Docker 起的，就把 `XOASSETS_REDIS_HOST` 改成容器可达地址，密码和数据库号保持和容器配置一致。

### 服务器部署：Docker Compose 全套启动

服务器部署使用仓库根目录 `docker-compose.yml`，一次启动 MySQL、XXL-JOB Admin、后端和前端：

```bash
docker compose up -d
```

`docker-compose.yml` 会在 MySQL 首次初始化时导入 `xxl-job-init.sql`，并预置 XXL-JOB 表结构、执行器和任务清单。任务默认停止，避免首次初始化立刻触发时点敏感任务，确认后在控制台启动。

已有 Docker 数据卷不会自动重放初始化 SQL，升级时先执行：

```bash
docker exec -i xoassets-mysql mysql -uroot -proot < src/main/resources/db/xxl-job-init.sql
```

| 任务 | Handler | 默认调度 |
|---|---|---|
| 虚拟货币行情刷新 | `refreshCryptoQuotes` | `0 0/15 * * * ? *` |
| 股票行情开盘刷新 | `refreshStockQuotes` | `0 30,45 9 ? * MON-FRI *` |
| 股票行情盘中刷新 | `refreshStockQuotes` | `0 0/15 10-14 ? * MON-FRI *` |
| 股票行情收盘刷新 | `refreshStockQuotes` | `0 0 15 ? * MON-FRI *` |
| 基金净值晚间首轮刷新 | `refreshFundQuotes` | `0 0 18 * * ? *` |
| 基金净值晚间跟进刷新 | `refreshFundQuotesFollowup` | `0 15/15 18-23 * * ? *` |
| 资产日级价格晚间汇总 | `aggregateRecentAssetPrices` | `0 0/15 20-23 * * ? *` |
| 资产日级价格收尾汇总 | `aggregateLateRecentAssetPrices` | `0 0/15 20-23 * * ? *` |
| 用户投资日快照首轮生成 | `snapshotRecentInvestmentDays` | `0 0 20 * * ? *` |
| 用户投资日快照跟进生成 | `snapshotRecentInvestmentDaysFollowup` | `0 15/15 20-23 * * ? *` |
| 基金待确认交易扫描 | `confirmPendingFundTransactions` | `0 0 0/3 * * ? *` |
| 每日用户资产快照生成 | `generateDailySnapshots` | `0 0/15 20-23 * * ? *` |
| 市场交易日历年度补齐 | `refreshYearlyMarketCalendar` | `0 5 0 1 1 ? *` |
| USD/CNY 汇率日缓存刷新 | `refreshDailyUsdCnyExchangeRate` | `0 20 6 * * ? *` |
| 待重建资产快照处理 | `rebuildPendingAssetSnapshots` | `0 0 10,15,21 * * ? *` |

```
XXL_JOB_ADMIN_ADDRESSES=http://localhost:8081/xxl-job-admin
XXL_JOB_ACCESS_TOKEN=xoassets-xxl-job-local-token
XXL_JOB_EXECUTOR_ENABLED=true
XXL_JOB_EXECUTOR_ADDRESS=http://host.docker.internal:10099/
XXL_JOB_EXECUTOR_PORT=10099
XOASSETS_REDIS_HOST=localhost
XOASSETS_REDIS_PORT=6379
XOASSETS_REDIS_PASSWORD=root
XOASSETS_REDIS_DATABASE=0
```


## 核心接口

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `PUT /api/auth/profile`
- `PUT /api/auth/password`
- `GET /api/accounts`
- `POST /api/accounts`
- `PUT /api/accounts/{id}`
- `DELETE /api/accounts/{id}`
- `GET /api/accounts/{id}/ledger`
- `GET /api/accounts/{id}/flow-statistics`
- `POST /api/accounts/{id}/balance-adjustments`
- `GET /api/accounts/{id}/balance-trend`
- `GET /api/categories`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`
- `PUT /api/categories/{id}/status`
- `GET /api/transactions`
- `POST /api/transactions`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`
- `GET /api/dashboard/overview`
- `GET /api/dashboard/recent-transactions`
- `GET /api/statistics/asset-trend`
- `GET /api/statistics/net-assets-trend`
- `GET /api/statistics/expense-category`
- `GET /api/statistics/income-expense-trend`
- `GET /api/statistics/asset-distribution`
- `GET /api/statistics/investment-profit-trend`
- `GET /api/statistics/budget-progress`
- `GET /api/snapshots/latest`
- `GET /api/snapshots/trend`
- `POST /api/snapshots/generate-today`
- `GET /api/assets/search`
- `POST /api/assets`
- `GET /api/holdings`
- `GET /api/holdings/summary`
- `GET /api/holdings/{id}/detail`
- `POST /api/holdings`
- `PUT /api/holdings/{id}`
- `DELETE /api/holdings/{id}`
- `POST /api/investment-transactions`
- `GET /api/investment-transactions`
- `GET /api/investment-transactions/fund-confirm-preview`
- `PUT /api/investment-transactions/{id}/revoke`
- `POST /api/quotes/manual`
- `POST /api/quotes/refresh`
- `GET /api/export/account-ledger`
- `GET /api/export/transactions`
- `GET /api/export/investment-transactions`
- `GET /api/budgets`
- `POST /api/budgets`
- `PUT /api/budgets/{id}`
- `DELETE /api/budgets/{id}`
- `GET /api/budgets/summary`
- `GET /api/goals`
- `POST /api/goals`
- `PUT /api/goals/{id}`
- `DELETE /api/goals/{id}`
- `GET /api/goals/summary`
- `GET /api/reports`
- `GET /api/reports/{id}`
- `POST /api/reports/generate-preview`

## 业务约束

- 金额字段使用 `BigDecimal`，数据库使用 `DECIMAL(18,4)`。
- 业务表均包含 `user_id`，查询、修改、删除按当前登录用户隔离。
- 所有金额、价格、数量等关键输入由 Bean Validation 和业务校验兜底，金额和价格必须大于 0。
- Long ID 统一序列化为字符串返回，避免前端 JavaScript number 精度丢失。
- 注册用户时会在同一事务内把默认收入 / 支出分类写入 `xo_category`，之后分类以表数据为准。
- 分类方向只由 `type` 表示，支持 `INCOME` 和 `EXPENSE`；分类名称、图标、颜色、排序和状态均为用户自定义数据。
- 分类被流水使用后不允许删除，只允许通过 `status = 0` 停用。
- 流水类型支持 `INCOME`、`EXPENSE`、`TRANSFER`、`REFUND`。
- 转账只影响账户余额，不计入收入支出统计。
- 删除或修改流水会在同一事务中反向修正账户余额。
- 账户余额校准通过 `xo_account_balance_adjustment` 生成专用修正事件；`PUT /api/accounts/{id}` 仍兼容余额差异，但 Web/App 应优先调用 `POST /api/accounts/{id}/balance-adjustments`。
- 余额修正不计入普通收入 / 支出统计，但会进入账户资金明细；账户日终余额曲线从普通流水、投资交易和余额修正事件实时重建。
- 账户资金明细接口聚合 `xo_transaction`、`xo_investment_transaction` 和 `xo_account_balance_adjustment`，按当前账户方向展示收入、支出、转账转入/转出、退款、投资买入、投资卖出和余额修正。
- 账户详情统计保留普通收支、转账、投资买卖汇总、余额修正、支出分类和余额曲线；页面不再返回投资资金流向图和日流入 / 流出趋势。
- 流水可保存 `image_url`，第一版允许前端传图片 Data URL，数据库使用 `MEDIUMTEXT`，后续可替换为对象存储 URL。
- 公共资产表 `xo_asset`、当前价表 `xo_asset_price_current` 和日级价格表 `xo_asset_price_daily` 不带 `user_id`；前端不再暴露资产管理入口，`xo_asset` 仅作为持仓行情和价格的内部基础数据。
- 旧价格快照表已退役；本地历史库删除旧表前先执行 `src/main/resources/db/migration-retire-asset-price.sql`，把历史价迁入 current/daily 后再 drop。
- 投资买入会创建或更新持仓并按移动平均成本重算；投资卖出必须校验持仓数量，数量不足时拒绝。
- 投资买入必须选择扣款账户并扣减账户余额；投资卖出必须选择到账账户并增加账户余额，已实现盈亏写入投资交易记录。
- 基金可先创建 0 份额持仓，再使用金额确认模式买入：`input_mode = AMOUNT_NAV` 时先按买入总金额扣减资金账户，`transaction_time` 表示用户实际买入时间；后端按 `xo_market_calendar` 计算有效申请日和确认日，15:00:00 及以前按当天申请，15:00 后或非交易日顺延，普通基金 T+1，名称包含 `QDII` 的基金 T+2。确认净值优先读取 `xo_asset_price_daily.close_price`，`xo_asset_price_current.quote_time` 等于确认日时可兜底；没有净值则保存为 `PENDING_CONFIRM`，由基金确认定时任务后续更新为 `CONFIRMED`，确认成功后同步 upsert 实际买入日至确认日区间以及今日的投资日快照、资产快照。
- 投资交易、资金账户和持仓联动在同一事务中完成，避免交易记录与账户余额、持仓数量、成本不一致。
- 投资交易支持撤销，不物理删除；撤销状态写入 `status = REVOKED`，账户余额和持仓通过原交易 `cost_amount` 反向恢复。
- 投资数量和基金确认份额统一保留 10 位小数，手续费、持仓成本、市值、盈亏和收益率统一按 4 位小数归一化后计算，避免不同调用入口产生精度口径差异。
- 投资总览会单独返回 `pendingConfirmAmount`，表示基金金额买入待确认的在途金额；`totalInvestmentAsset` 口径为已确认持仓市值 + 在途基金金额。
- 持仓接口返回最新价、昨价、前日价、今日收益、昨日收益、浮动盈亏、收益率、回本涨幅和报价时间；所有资产只有当前价格日期等于今天时才计算今日收益，其中基金和股票还必须当天为交易日，非交易日返回 `todayPriceAvailable=false`、`priceStatus=MARKET_CLOSED`，交易日未更新或虚拟货币当前价过期返回 `priceStatus=TODAY_PRICE_NOT_AVAILABLE`；收益基准价格或基准持仓数量缺失时返回 `null`，前端展示 `--`；投资总览和模块卡通过 `todayProfitStatusLabel` / `primaryProfitStatusLabel` 暴露“今日休市”等不可用原因。
- 今日收益同时返回当前 / 今日有效份额口径和上一交易日日终份额归因口径，Web 默认展示当前份额口径；每日收益日历、昨日收益、趋势图每日收益和收益贡献统一从 `xo_investment_holding_daily_profit` 聚合，缺少持仓每日收益行时返回 `null` 并显示 `--`；投资总今日 / 本月收益按 `当前市值 - 基准市值 - 净入金` 计算，避免期间买卖带来的资金流入 / 流出影响收益判断。
- 行情当前价和日级价格使用 `DECIMAL(28,8)`，第三方行情和手动报价入库前统一保留 8 位；`xo_asset_price_current` / `xo_asset_price_daily` 记录 `previous_close`、`change_amount`、`change_percent` 等行情字段，持仓返回 `priceScale`，CRYPTO 当前价至少展示 6 位，FUND / STOCK 展示 4 位。
- 公共资产表 `xo_asset` 使用 `market` 区分交易市场，股票为 `SH` / `SZ` / `BJ` / `US`，基金为 `CN_FUND`，虚拟货币为 `CRYPTO`；唯一性按 `type + market + symbol + deleted` 控制。
- 持仓估值使用与资产币种一致的最近价格；没有价格或价格币种不一致时使用平均成本兜底，避免当前价和市值口径不一致。
- 行情刷新通过 `QuoteProvider` 抽象扩展；当前支持 `ManualQuoteProvider`、`CoinGeckoQuoteProvider`、`EastMoneyFundQuoteProvider`、`StockQuoteProvider`。基金优先读取天天基金 F10 历史净值表，最新价使用最新单位净值，昨价使用上一交易日单位净值，失败时再回退实时净值接口；手动和自动刷新成功后都会重建受影响资产的持仓每日收益，即使第三方返回的行情时间和价格与 current 完全相同，也要重建，避免收益日历停留在旧计算结果。
- `GET /api/assets/lookup` 支持新增持仓时按类型查询基金、股票、虚拟货币基础信息和当前价格；前端选择结果后保存持仓，后端创建或复用 `xo_asset` 并写入 `xo_asset_price_current`，带报价日期的初始价同步写入 `xo_asset_price_daily`。
- 资产查询失败会在后端 WARN 日志中输出行情源、代码 / 市场、响应摘要和原始异常堆栈，便于区分网络失败、第三方格式变更和代码无效。
- CoinGecko 支持 CRYPTO 资产 BTC、ETH、SOL、BNB、DOGE；天天基金支持基金单位净值；新浪支持 A 股；Yahoo Finance 支持美股。
- `POST /api/quotes/refresh-batch` 支持按当前持仓资产批量刷新；刷新失败保留旧价格，不删除历史快照。
- 行情缓存按资产类型控制刷新频率：CRYPTO 15 分钟、STOCK 15 分钟、FUND 1 天；MANUAL 价格不过期；股票只在开盘日 09:30-15:00 之间拉取第三方行情，虚拟货币全天每 15 分钟刷新，基金净值每天 18:00 首轮、18:15-23:45 每 15 分钟强制跟进刷新。股票和虚拟货币原始行情写入 Redis ZSET `price:snapshot:{assetId}:{yyyyMM}`，TTL 3 天，日级汇总读取某天数据时按当天起止毫秒 score 范围查询，不全量拉取整月数据。`GET /api/exchange-rates/usd-cny` 返回 USD/CNY 日缓存汇率，MVP 使用进程内缓存，后续可替换为 Redis。
- 后端行情刷新由 XXL-JOB 可视化调度中心触发，`refreshStockQuotes` / `refreshCryptoQuotes` / `refreshFundQuotes` 等 handler 按资产类型分开处理；任务或单个资产失败只记录日志，不影响主应用启动。
- 市场交易日历使用 `xo_market_calendar` 存储，`MarketCalendarRefreshScheduler` 在应用启动和每年 1 月 1 日补齐当前年、下一年基础周末日历；春节、国庆等交易所特殊休市以数据库修正记录为准，不写死在 Java 或配置文件里。
- 预算表 `xo_budget` 按当前用户隔离；每个用户每月只能有一个总预算，每个支出分类每月只能有一个分类预算。
- 预算使用额从 `xo_transaction` 汇总，转账不计入预算，退款抵扣支出。
- 资产快照表 `xo_asset_snapshot` 每天记录现金资产、投资资产、负债、净资产、截至快照日的月度收支和预算使用率；同一用户同一天只保留一条，重复生成会更新。
- `/api/snapshots/latest` 的较昨日 / 较月初变化缺少基准快照时返回 `null`，前端展示 `--`；本月只有最新一条且最新日期不是 1 号时，较月初也返回 `null`，不能把缺失对比冒充为 0。
- 持仓每日收益表 `xo_investment_holding_daily_profit` 按展示日保存每个持仓的真实日收益，是收益日历、昨日收益、投资趋势每日收益和收益贡献排行的权威来源；清仓日收益按日内分段卖出价计算，更新时必须显式覆盖可空字段，避免旧 `profit_rate` / `market_value` 残留；基金 / QDII 净值收益按净值日后的下一交易日展示，确认日当天新生效份额必须参与该展示日收益计算，股票按价格日展示。
- 投资日快照表 `xo_investment_daily_snapshot` 按 `xo_investment_transaction` 截至快照日重建历史持仓、成本、市值、已实现收益和当日投资本金净流入；手工初始化持仓存在后续买卖时，`InvestmentPositionHistoryService` 先反推出手工底仓再按交易流水重建，卖出交易净变化不能提前截断为 0；净值型基金收益重建使用收益展示日份额，保证金额申购确认日新份额不漏算；`daily_profit` 表示快照日资金流调整收益，公式为本日投资市值 - 上一快照日投资市值 - 当日投资本金净流入，不等同于收益日历展示日收益；`calendar_profit` 从 `xo_investment_holding_daily_profit` 按展示日聚合，`buy_amount` / `sell_amount` / `fee_amount` 按资金实际发生日统计，基金确认日只影响份额生效；补跑用户覆盖近期有交易、已有快照或当前有持仓的用户。
- 基金金额买入从实际申购日至确认日前按在途投资资产计入；交易后续确认后，补跑确认日前历史快照仍必须保留这段在途金额，避免已扣款但未确认份额导致净资产假跌。
- 投资日快照补跑按 `trade_date` 使用已回填的 `xo_asset_price_daily` 日级价格，不用价格行 `created_at` 判断是否晚于快照日，确保周末后和净值延迟时历史市值可被修正。
- 资产快照中现金资产按账户初始余额 + 普通流水 / 投资交易 / 余额修正重建快照日历史余额，正余额计入现金资产、负余额按绝对值计入负债；投资资产按快照日通过交易流水重建历史头寸，再使用同币种日级价 / 当前价估值，补跑历史快照不能用当前账户余额或当前持仓数量倒推。
- 本地对账可手动重建当前用户指定日期：`POST /api/investments/snapshots/generate?snapshotDate=yyyy-MM-dd` 会先重建持仓每日收益，再 upsert 投资日快照；`POST /api/quotes/manual`、`POST /api/quotes/refresh`、`POST /api/quotes/refresh-batch` 会在行情成功后即时重建受影响资产的持仓每日收益；`POST /api/snapshots/generate?snapshotDate=yyyy-MM-dd` 重建用户资产快照；不允许生成未来日期快照。
- 首页总资产优先读取 `/api/snapshots/latest`；快照口径下总资产 = 现金资产 + 投资资产，净资产 = 总资产 - 负债。
- 用户资产快照由 XXL-JOB handler `generateDailySnapshots` 默认每天 20:00-23:45 每 15 分钟触发，为所有启用用户生成资产快照，单个用户失败只记录日志。
- 统计接口全部按当前 `user_id` 隔离，支出统计排除转账，退款抵扣支出。
- 资产目标表 `xo_goal` 按当前用户隔离；当前金额可手动填写，也可按当前净资产口径写入。
- AI 报告表 `xo_ai_report` 按当前用户隔离；阶段七只基于首页、预算、投资等真实数据生成模板化复盘，不调用真实 AI，不提供投资买卖建议。
- 业务层采用 `service` 接口 + `service/impl` 实现类结构，Controller 依赖接口。

## 开发验收

- 初始化表结构：`mysql -uroot -p < src/main/resources/db/schema.sql`。
- 初始化市场日历：`mysql -uroot -p xoassets < src/main/resources/db/migration-market-calendar.sql`。
- 导入开发数据：`mysql -uroot -p xoassets < src/main/resources/db/dev-data.sql`。
- 测试账号：`demo / xoassets123`。
- Docker 一键启动从仓库根目录执行 `docker compose up -d`，MySQL 首次启动会自动执行 `schema.sql`、`migration-market-calendar.sql` 和 `dev-data.sql`。
- 核心测试在 `src/test/java/com/xoassets/module/MvpCoreServiceTest.java`，覆盖流水余额联动、余额修正、账户账本、账户余额曲线、预算退款抵扣、投资账户联动、移动平均成本、历史投资头寸、收益分析、首页总资产和数据隔离基础路径。
- `mvn test` 会生成 JaCoCo 覆盖率报告，不设置阻断阈值。
- CSV 导出接口只导出当前用户数据，包含账户资金明细、普通流水和投资交易；输出 UTF-8 BOM，金额保留 4 位小数。

已有库升级到投资账户联动版本时，先为历史投资交易补齐当前用户的资金账户，再把字段改为非空：

```sql
ALTER TABLE xo_investment_transaction
  ADD COLUMN account_id BIGINT NULL COMMENT '资金账户ID' AFTER asset_id,
  ADD COLUMN cost_amount DECIMAL(18,4) DEFAULT NULL COMMENT '本次交易对应成本金额' AFTER fee,
  ADD COLUMN realized_profit DECIMAL(18,4) DEFAULT NULL COMMENT '已实现盈亏' AFTER fee,
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL正常 REVOKED已撤销' AFTER realized_profit,
  ADD COLUMN revoke_time DATETIME DEFAULT NULL COMMENT '撤销时间' AFTER status,
  ADD COLUMN revoke_reason VARCHAR(255) DEFAULT NULL COMMENT '撤销原因' AFTER revoke_time,
  ADD KEY idx_user_account_time (user_id, account_id, transaction_time);

UPDATE xo_investment_transaction t
JOIN xo_account a ON a.user_id = t.user_id AND a.deleted = 0
SET t.account_id = a.id
WHERE t.account_id IS NULL;

UPDATE xo_investment_transaction
SET cost_amount = CASE
  WHEN type = 'BUY' THEN amount + fee
  ELSE amount
END
WHERE cost_amount IS NULL;

ALTER TABLE xo_investment_transaction
  MODIFY account_id BIGINT NOT NULL COMMENT '资金账户ID';
```

已有库升级到基金金额买入版本时，执行：

```bash
mysql -u root -p xoassets < src/main/resources/db/migration-fund-amount-nav.sql
```

已有库升级到市场交易日历版本时，执行：

```bash
mysql -u root -p xoassets < src/main/resources/db/migration-market-calendar.sql
```

已有库升级到账户余额修正和余额曲线版本时，执行：

```bash
mysql -u root -p xoassets < src/main/resources/db/migration-account-adjustment-balance-trend.sql
```

已有库升级到 Redis 行情分层版本时，执行：

```bash
mysql -u root -p xoassets < src/main/resources/db/migration-redis-quote-layer.sql
```

已有库退役旧价格快照表时，先确认已备份旧表，再执行：

```bash
mysql -u root -p xoassets < src/main/resources/db/migration-retire-asset-price.sql
```

已有库升级到持仓每日收益持久化版本时，执行：

```bash
mysql -u root -p xoassets < src/main/resources/db/migration-investment-holding-daily-profit.sql
```

已有库退役账户日余额快照表时，执行：

```bash
mysql -u root -p xoassets < src/main/resources/db/migration-retire-account-daily-balance-snapshot.sql
```
