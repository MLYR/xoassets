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
export XOASSETS_DB_URL='jdbc:mysql://localhost:3306/xoassets?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export XOASSETS_DB_USERNAME='root'
export XOASSETS_DB_PASSWORD='your-password'
export XOASSETS_JWT_SECRET='replace-with-at-least-32-bytes-secret-key'
export XOASSETS_JWT_EXPIRE_MINUTES='10080'
```

3. 启动服务：

```bash
mvn spring-boot:run
```

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
- 账户余额允许通过账户编辑手动校准，用于处理利息、漏记流水等现实差异；流水增删改仍会按事务联动余额。
- 账户资金明细接口聚合 `xo_transaction` 和 `xo_investment_transaction`，按当前账户方向展示收入、支出、转账转入/转出、退款、投资买入和投资卖出。
- 账户资金流向统计单独区分普通收支、转账和投资资金流，投资买入不计入普通支出分类统计。
- 流水可保存 `image_url`，第一版允许前端传图片 Data URL，数据库使用 `MEDIUMTEXT`，后续可替换为对象存储 URL。
- 公共资产表 `xo_asset` 和价格表 `xo_asset_price` 不带 `user_id`；前端不再暴露资产管理入口，`xo_asset` 仅作为持仓行情和价格快照的内部基础数据。
- 已有本地库升级行情字段时，执行 `src/main/resources/db/migration-quote-fields.sql`。
- 投资买入会创建或更新持仓并按移动平均成本重算；投资卖出必须校验持仓数量，数量不足时拒绝。
- 投资买入必须选择扣款账户并扣减账户余额；投资卖出必须选择到账账户并增加账户余额，已实现盈亏写入投资交易记录。
- 投资交易、资金账户和持仓联动在同一事务中完成，避免交易记录与账户余额、持仓数量、成本不一致。
- 投资交易支持撤销，不物理删除；撤销状态写入 `status = REVOKED`，账户余额和持仓通过原交易 `cost_amount` 反向恢复。
- 投资数量统一保留 10 位小数，手续费、持仓成本、市值、盈亏和收益率统一按 4 位小数归一化后计算，避免不同调用入口产生精度口径差异。
- 持仓接口返回最新价、昨价、前日价、今日收益、昨日收益、浮动盈亏、收益率、回本涨幅和报价时间；缺少历史价格时收益分析字段允许为空。
- 行情价格快照使用 `DECIMAL(28,8)`，第三方行情和手动报价入库前统一保留 8 位；`xo_asset_price` 记录 `previous_close`、`change_amount`、`change_percent`、`market_status`，持仓返回 `priceScale`，CRYPTO 当前价至少展示 6 位，FUND / STOCK 展示 4 位。
- 公共资产表 `xo_asset` 使用 `market` 区分交易市场，股票为 `SH` / `SZ` / `BJ` / `US`，基金为 `CN_FUND`，虚拟货币为 `CRYPTO`；唯一性按 `type + market + symbol + deleted` 控制。
- 持仓估值使用与资产币种一致的最近价格；没有价格或价格币种不一致时使用平均成本兜底，避免当前价和市值口径不一致。
- 行情刷新通过 `QuoteProvider` 抽象扩展；当前支持 `ManualQuoteProvider`、`CoinGeckoQuoteProvider`、`EastMoneyFundQuoteProvider`、`StockQuoteProvider`。基金优先读取天天基金 F10 历史净值表，最新价使用最新单位净值，昨价使用上一交易日单位净值，失败时再回退实时净值接口。
- `GET /api/assets/lookup` 支持新增持仓时按类型查询基金、股票、虚拟货币基础信息和当前价格；前端选择结果后保存持仓，后端创建或复用 `xo_asset` 并写入 `xo_asset_price`。
- 资产查询失败会在后端 WARN 日志中输出行情源、代码 / 市场、响应摘要和原始异常堆栈，便于区分网络失败、第三方格式变更和代码无效。
- CoinGecko 支持 CRYPTO 资产 BTC、ETH、SOL、BNB、DOGE；天天基金支持基金单位净值；新浪支持 A 股；Yahoo Finance 支持美股。
- `POST /api/quotes/refresh-batch` 支持按当前持仓资产批量刷新；刷新失败保留旧价格，不删除历史快照。
- 行情缓存按资产类型控制刷新频率：CRYPTO 1 小时、STOCK 15 分钟、FUND 1 天；MANUAL 价格不过期；股票只在 09:30-15:00 之间拉取第三方行情。`GET /api/exchange-rates/usd-cny` 返回 USD/CNY 日缓存汇率，MVP 使用进程内缓存，后续可替换为 Redis。
- 后端启用 `QuoteRefreshScheduler` 定时刷新持仓涉及资产，任务或单个资产失败只记录日志，不影响主应用启动。
- 预算表 `xo_budget` 按当前用户隔离；每个用户每月只能有一个总预算，每个支出分类每月只能有一个分类预算。
- 预算使用额从 `xo_transaction` 汇总，转账不计入预算，退款抵扣支出。
- 资产快照表 `xo_asset_snapshot` 每天记录现金资产、投资资产、负债、净资产、月度收支和预算使用率；同一用户同一天只保留一条，重复生成会更新。
- 资产快照中现金资产只统计正余额账户，负余额账户按绝对值计入负债；投资资产使用持仓数量和同币种最新价格快照计算。
- 首页总资产 = 快照现金资产 + 投资持仓市值；净资产 = 总资产 - 负债。
- 后端启用 `AssetSnapshotScheduler`，默认每天 23:55 为所有启用用户生成资产快照，单个用户失败只记录日志。
- 统计接口全部按当前 `user_id` 隔离，支出统计排除转账，退款抵扣支出。
- 资产目标表 `xo_goal` 按当前用户隔离；当前金额可手动填写，也可按当前净资产口径写入。
- AI 报告表 `xo_ai_report` 按当前用户隔离；阶段七只基于首页、预算、投资等真实数据生成模板化复盘，不调用真实 AI，不提供投资买卖建议。
- 业务层采用 `service` 接口 + `service/impl` 实现类结构，Controller 依赖接口。

## 开发验收

- 初始化表结构：`mysql -uroot -p < src/main/resources/db/schema.sql`。
- 导入开发数据：`mysql -uroot -p xoassets < src/main/resources/db/dev-data.sql`。
- 测试账号：`demo / xoassets123`。
- Docker 一键启动从仓库根目录执行 `docker compose up -d`，MySQL 首次启动会自动执行 `schema.sql` 和 `dev-data.sql`。
- 核心测试在 `src/test/java/com/xoassets/module/MvpCoreServiceTest.java`，覆盖流水余额联动、预算退款抵扣、投资账户联动、移动平均成本、收益分析、首页总资产和数据隔离基础路径。
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
