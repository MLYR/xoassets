# 小〇财迹后端

第一期 MVP 后端服务，基于 Java 17、Spring Boot 3、MyBatis-Plus、MySQL 8、JWT 和 Knife4j。

## 当前范围

- 用户注册、登录、当前用户
- 账户管理
- 分类管理
- 收支流水
- 首页统计
- 基础图表统计

暂不包含投资行情自动同步、AI 报告真实调用、银行卡 / 支付宝 / 微信自动同步、股票基金自动交易或投资建议。

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
- `GET /api/accounts`
- `POST /api/accounts`
- `PUT /api/accounts/{id}`
- `DELETE /api/accounts/{id}`
- `GET /api/categories`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`
- `GET /api/transactions`
- `POST /api/transactions`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`
- `GET /api/dashboard/overview`
- `GET /api/dashboard/recent-transactions`
- `GET /api/statistics/asset-trend`
- `GET /api/statistics/expense-category`
- `GET /api/statistics/income-expense-trend`

## 业务约束

- 金额字段使用 `BigDecimal`，数据库使用 `DECIMAL(18,4)`。
- 业务表均包含 `user_id`，查询、修改、删除按当前登录用户隔离。
- 流水类型支持 `INCOME`、`EXPENSE`、`TRANSFER`、`REFUND`。
- 转账只影响账户余额，不计入收入支出统计。
- 删除或修改流水会在同一事务中反向修正账户余额。
- 业务层采用 `service` 接口 + `service/impl` 实现类结构，Controller 依赖接口。
