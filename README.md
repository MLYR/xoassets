# 小〇财迹 / XOAssets

小〇财迹是面向个人用户的资产管理与财务复盘工具。当前仓库包含：

```text
xoassets-server   Java 17 + Spring Boot 3 后端
xoassets-web      Vue3 + TypeScript + Vite Web 管理端
xoassets-mobile   Flutter 新版移动端 App
xoassets-app      uni-app 旧移动端 App
AGENTS.md         AI 协作规范
```

## 当前重点

- 后端是业务和数据口径权威。
- Web 是已有管理端 / Web 前端。
- Flutter 是新版移动端方向。
- 旧 uni-app 目录保留，但默认不作为新版移动端依据。

## 当前暂不支持

- 银行卡自动同步。
- 支付宝自动同步。
- 微信自动同步。
- 银行、券商、基金平台自动持仓同步。
- 真实 AI 调用。
- 投资建议。
- 自动交易。

## 文档入口

| 文件 | 说明 |
|---|---|
| `AGENTS.md` | AI 协作主约束 |
| `docs/PROJECT_OVERVIEW.md` | 项目概览和当前进度 |
| `docs/BUSINESS_RULES.md` | 核心业务口径 |
| `docs/API_CONTRACTS.md` | 接口约定和联调状态 |
| `docs/LOCAL_DEVELOPMENT.md` | 本地启动、Docker、XXL-JOB |
| `docs/VALIDATION_CHECKLIST.md` | MVP 验收和测试命令 |
| `docs/AI_AGENT_WORKFLOW.md` | AI 执行前后流程 |
| `xoassets-mobile/AGENTS.md` | Flutter 移动端 AI 约束 |
| `xoassets-mobile/MOBILE_APP_PHASES.md` | 移动端阶段计划 |
| `xoassets-mobile/docs/MOBILE_UI_DESIGN_SYSTEM.md` | 移动端 UI 规范 |

## 快速启动

后端：

```bash
cd xoassets-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn spring-boot:run
```

Web：

```bash
cd xoassets-web
npm install
npm run dev
```

Flutter：

```bash
cd xoassets-mobile
flutter pub get
flutter run
```

接口文档：

```text
http://localhost:8080/doc.html
```

测试账号：

```text
demo / xoassets123
```
