# 小〇财迹

小〇财迹是面向个人用户的资产管理与财务复盘工具。当前仓库包含 Vue3 前端原型和 Spring Boot 后端 MVP。

## 项目结构

```text
.
├── xoassets-web      Vue3 + TypeScript + Vite 前端
├── xoassets-server   Java 17 + Spring Boot 3 后端
├── AGENTS.md         AI 协作规范
└── 小〇财迹_产品需求_设计_开发文档.md
```

## 当前进度

- 前端：已重建为 Vue3、Element Plus、ECharts、Pinia、Vue Router 项目，页面暂用 mock 数据。
- 后端：已创建 Spring Boot MVP，覆盖登录注册、账户、分类、流水、首页统计和基础图表统计。
- 暂不做：自动同步银行卡 / 支付宝 / 微信、投资行情自动同步、AI 报告真实调用、自动交易或投资建议。

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
