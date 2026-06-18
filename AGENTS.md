# XOAssets Codex 协作规范

## 1. 项目定位

XOAssets / 小〇财迹 是面向个人用户的资产管理与财务复盘工具。

核心目标：

- 帮助用户知道钱花哪了。
- 帮助用户知道资产涨跌多少。
- 帮助用户知道净资产为什么变化。
- 帮助用户进行轻量财务复盘。

项目目录约定：

```text
xoassets-server   Java 17 + Spring Boot 3 后端
xoassets-web      Vue3 + TypeScript + Vite Web 管理端
xoassets-app      React Native 新版移动端 App
```

如果实际目录与上述不一致，Codex 必须先说明当前目录结构，不允许擅自重命名、迁移或删除目录。

## 2. 文档优先级

Codex 执行任务前按以下顺序读取：

1. 根目录 `AGENTS.md`
2. 当前子项目目录下的 `AGENTS.md`
3. 当前任务相关 `docs/*.md`
4. 移动端任务读取 `xoassets-app/MOBILE_APP_PHASES.md`
5. Swagger / Knife4j / 后端接口文档
6. 当前代码实现

冲突处理：

- 先说明冲突点。
- 说明影响范围和风险。
- 采用最小必要修改。
- 高风险场景先询问用户。

## 3. 总体执行原则

1. 先读代码和相关文档，再改代码。
2. 优先最小改动，禁止无关重构。
3. 不修改与任务无关文件。
4. 不新增不必要依赖。
5. 不删除、不改写、不搬动原有注释，除非对应逻辑已修改且注释失真。
6. 注释只解释业务原因或非显然逻辑，避免空话。
7. 每次完成后必须说明改动内容、验证情况、剩余风险。
8. 不确定且高风险时先澄清。
9. 低风险场景可做合理假设并推进，但必须说明假设。

## 4. 技术栈边界

### 4.1 后端

- Java 17
- Spring Boot 3.x
- MyBatis-Plus
- MySQL 8
- JWT
- XXL-JOB
- Lombok
- Knife4j / Swagger
- Redis

后端规则：

- 事务边界放在 Service 层。
- Controller 优先依赖 Service 接口。
- 不要在 Controller 中堆复杂业务逻辑。
- 不要在 Mapper XML 中堆复杂业务判断。
- 金额、收益、资产口径以后端计算为准。

### 4.2 Web 前端

- Vue 3
- Vite
- TypeScript
- Element Plus
- ECharts
- Pinia
- Axios
- Vue Router

Web 规则：

- 继续使用 Element Plus + ECharts。
- Web 端必须使用 `xo-design` tokens。
- 禁止引入新的重型 UI 框架。
- 禁止为单个页面重复造独立视觉规则。

### 4.3 React Native 移动端

移动端目录：

```text
xoassets-app/
```

移动端技术栈：

- React Native
- TypeScript
- Expo
- Expo Router
- NativeWind
- 自研 XO Design System
- Zustand
- TanStack Query
- Axios
- React Hook Form
- Zod
- expo-secure-store
- @react-native-async-storage/async-storage
- react-native-svg
- react-native-reanimated

移动端规则：

- 新版移动端从零开始。
- App 只能通过后端 HTTP API 获取数据。
- App 不允许直接连接 MySQL。
- 第一版不做离线数据库。
- 第一版不引入 SQLite / WatermelonDB / Realm / TypeORM / Prisma Client。
- Token 使用 `expo-secure-store` 保存。
- 普通配置使用 `@react-native-async-storage/async-storage` 保存。
- 服务端状态使用 TanStack Query 管理。
- 本地 UI 状态使用 Zustand 管理。

## 5. 移动端 UI 风格约束

移动端视觉统一参考 shadcn/ui：

```text
简洁
克制
卡片化
弱边框
低饱和
清晰层级
强一致性
浅色优先，深色可扩展
```

参考地址：

```text
https://ui.shadcn.com/docs/installation
```

规则：

- shadcn/ui 只作为设计语言、组件结构、视觉密度和交互状态参考。
- React Native 端不直接运行 shadcn/ui Web 安装命令。
- React Native 端不直接使用 shadcn/ui Web 组件。
- 不使用 DOM、CSS Modules、浏览器专属 API。
- 不把 Web 页面缩小后搬到移动端。
- 所有组件必须通过 XO Design System 在 React Native 原生组件上实现。
- shadcn/ui 常见组件需要映射为 `XoButton`、`XoCard`、`XoTextField`、`XoBadge`、`XoTabs`、`XoBottomSheet`、`XoDialog`、`XoSkeleton`、`XoEmpty`。

详细 UI 规范查看：

```text
xoassets-app/docs/MOBILE_UI_DESIGN_SYSTEM.md
xoassets-app/docs/SHADCN_UI_STYLE_GUIDE.md
```

## 6. 业务口径入口

详细业务规则统一查看：

```text
docs/BUSINESS_RULES.md
```

Codex 不得在前端或移动端重新实现后端业务计算口径，尤其是：

- 净资产
- 总资产
- 投资收益
- 基金 / 股票 / 虚拟货币收益
- 快照重建
- 预算使用额
- 用户数据隔离

## 7. 接口入口

详细接口约定查看：

```text
docs/API_CONTRACTS.md
```

通用原则：

- 前端和移动端只调用 XOAssets 后端 API。
- 第三方行情只能由后端调用。
- 第三方汇率只能由后端调用。
- App 不直接访问对象存储密钥。
- 后端 Long ID 在前端和移动端按字符串处理。

## 8. 安全要求

- 使用 JWT 登录认证。
- 请求头格式：`Authorization: Bearer <token>`。
- 密码禁止明文存储，使用 BCrypt。
- 所有查询、修改、删除必须通过当前登录用户隔离数据。
- 日志中不要打印 Token、密码、账户信息、金额明细、AI 请求敏感内容。
- App 不允许写死生产密钥、MySQL 账号密码、对象存储密钥。
- 外部输入必须校验。
- 异常路径要显式处理。

## 9. 禁止事项

默认禁止：

- 未经确认删除文件。
- 未经确认大规模重构。
- 未经确认修改目录结构。
- 未经确认引入新框架。
- 未经确认替换技术栈。
- App 直接连接 MySQL。
- 前端或移动端直接调用第三方行情源。
- 前端或移动端直接调用第三方汇率源。
- 用 0 替代后端返回的 `null` 金额或收益字段。
- 用历史价或兜底价冒充今日价格。
- 给用户提供投资买卖建议。

## 10. Git 规则

未经用户明确要求，不执行：

```bash
git add
git commit
git push
git checkout -b
git branch
创建 PR
```

用户要求提交时，默认使用中文提交描述。

推荐 Commit 格式：

```text
type(scope): summary
```

## 11. 验证入口

详细验证清单查看：

```text
docs/VALIDATION_CHECKLIST.md
```

常用命令：

```bash
cd xoassets-server && ./mvnw test
cd xoassets-web && npm run build
cd xoassets-app && npm run typecheck
```

如果无法验证，必须说明原因和剩余风险。

## 12. Codex 工作流

详细工作流查看：

```text
docs/CODEX_WORKFLOW.md
```

Codex 每次执行前必须输出：

```text
当前目录结构分析
已读取的约束文档
准备新增的文件
准备修改的文件
本次执行计划
风险点
需要用户确认的问题
```

Codex 每次执行后必须输出：

```text
完成内容
新增文件
修改文件
删除文件，若有
新增依赖，若有
运行命令
验证结果
遗留问题
风险点
下一步建议
```
