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

- 前端：已重建为 Vue3、Element Plus、ECharts、Pinia、Vue Router 项目；登录、注册、用户中心、账户管理、分类管理、记账流水、投资持仓已接入后端接口，其余页面暂用 mock 数据。
- 后端：已创建 Spring Boot MVP，覆盖登录注册、账户、分类、流水、首页统计、基础图表统计和投资持仓手动维护。
- 暂不做：自动同步银行卡 / 支付宝 / 微信、AI 报告真实调用、自动交易或投资建议；虚拟货币行情已支持 CoinGecko 手动刷新，股票 / 基金自动行情暂不接入。

## 前后端联调状态

- 认证：`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me` 已在前端封装为 `authApi`。
- 登录态：前端使用 Axios 请求封装，JWT 存入 `localStorage`，请求自动携带 `Authorization: Bearer <token>`。
- 路由守卫：没有 token 访问业务页会跳转 `/login`，401 响应会清理 token 并回到登录页。
- 账户管理：`GET /api/accounts`、`POST /api/accounts`、`PUT /api/accounts/{id}`、`DELETE /api/accounts/{id}` 已接入账户页。
- 分类管理：`GET /api/categories`、`POST /api/categories`、`PUT /api/categories/{id}`、`DELETE /api/categories/{id}`、`PUT /api/categories/{id}/status` 已接入分类页。
- 记账流水：`GET /api/transactions`、`POST /api/transactions`、`PUT /api/transactions/{id}`、`DELETE /api/transactions/{id}` 已接入记账页。
- 投资持仓：`GET /api/assets/search`、`POST /api/assets`、`GET /api/holdings`、`POST /api/holdings`、`PUT /api/holdings/{id}`、`DELETE /api/holdings/{id}`、`POST /api/investment-transactions`、`GET /api/investment-transactions`、`POST /api/quotes/manual`、`POST /api/quotes/refresh` 已接入投资页。
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

## 后端结构约定

- 业务层使用 `service` 接口 + `service/impl` 实现类结构。
- Controller 和跨模块调用优先依赖 service 接口。
- MyBatis-Plus 3.5.9 的分页拦截器需要保留 `mybatis-plus-jsqlparser` 依赖。
