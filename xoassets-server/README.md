# 小〇财迹后端

第一期 MVP 后端服务，基于 Java 17、Spring Boot 3、MyBatis-Plus、MySQL 8、JWT 和 Knife4j。

## 当前范围

- 用户注册、登录、当前用户
- 账户管理
- 分类管理
- 收支流水
- 首页统计
- 基础图表统计
- 投资资产、投资持仓、投资交易、手动价格维护和 CoinGecko 虚拟货币行情刷新

暂不包含 AI 报告真实调用、银行卡 / 支付宝 / 微信自动同步、股票基金自动交易或投资建议；股票 / 基金自动行情暂不接入。

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
- `GET /api/statistics/expense-category`
- `GET /api/statistics/income-expense-trend`
- `GET /api/assets/search`
- `POST /api/assets`
- `GET /api/holdings`
- `POST /api/holdings`
- `PUT /api/holdings/{id}`
- `DELETE /api/holdings/{id}`
- `POST /api/investment-transactions`
- `GET /api/investment-transactions`
- `POST /api/quotes/manual`
- `POST /api/quotes/refresh`

## 业务约束

- 金额字段使用 `BigDecimal`，数据库使用 `DECIMAL(18,4)`。
- 业务表均包含 `user_id`，查询、修改、删除按当前登录用户隔离。
- Long ID 统一序列化为字符串返回，避免前端 JavaScript number 精度丢失。
- 注册用户时会在同一事务内把默认收入 / 支出分类写入 `xo_category`，之后分类以表数据为准。
- 分类方向只由 `type` 表示，支持 `INCOME` 和 `EXPENSE`；分类名称、图标、颜色、排序和状态均为用户自定义数据。
- 分类被流水使用后不允许删除，只允许通过 `status = 0` 停用。
- 流水类型支持 `INCOME`、`EXPENSE`、`TRANSFER`、`REFUND`。
- 转账只影响账户余额，不计入收入支出统计。
- 删除或修改流水会在同一事务中反向修正账户余额。
- 公共资产表 `xo_asset` 和价格表 `xo_asset_price` 不带 `user_id`；用户持仓 `xo_holding` 和投资交易 `xo_investment_transaction` 必须按当前登录用户隔离。
- 投资买入会创建或更新持仓并按移动平均成本重算；投资卖出必须校验持仓数量，数量不足时拒绝。
- 持仓估值使用最新手动价格；没有价格时使用平均成本兜底，避免页面无法展示。
- 行情刷新通过 `QuoteProvider` 抽象扩展；阶段二支持 `ManualQuoteProvider` 和 `CoinGeckoQuoteProvider`。
- CoinGecko 第一版只支持 CRYPTO 资产的 BTC、ETH、SOL、BNB、DOGE，刷新失败只返回错误提示，不删除旧价格。
- 行情缓存按资产类型控制刷新频率：CRYPTO 5 分钟、STOCK 15 分钟、FUND 1 天；MANUAL 价格不过期。
- 后端启用 `QuoteRefreshScheduler` 定时刷新持仓涉及资产，任务或单个资产失败只记录日志，不影响主应用启动。
- 业务层采用 `service` 接口 + `service/impl` 实现类结构，Controller 依赖接口。
