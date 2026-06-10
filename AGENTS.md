# AGENTS 协作规范

## 1. 项目定位
- XOAssets / 小〇财迹 是面向个人用户的资产管理与财务复盘工具。
- 核心目标：帮助用户每天知道钱花哪了、资产涨跌多少、净资产为什么变化。
- 第一版以 Web / H5 响应式页面为主，优先交付可运行、可验证的 MVP。

## 2. 当前权威文档
- 产品、设计、开发细节以 `小〇财迹_产品需求_设计_开发文档.md` 为准。
- 修改需求、接口、数据结构、页面范围前，先回读该文档对应章节。
- 若代码实现与文档冲突，先说明冲突点和风险，再做最小必要修改。

## 3. 技术栈约定

### 3.1 后端
- Java 17
- Spring Boot 3.x
- MyBatis-Plus
- MySQL 8
- JWT
- XXL-JOB（可视化定时任务调度中心）
- Lombok
- Knife4j / Swagger
- 日志使用 `logback-spring.xml` 按 `XOASSETS_PROFILE` 区分 dev / test / prod；dev 默认输出业务、MyBatis、JDBC 调试日志并保留每日滚动文件，生产不要开启 SQL DEBUG。
- Redis 已用于股票和虚拟货币投资行情最近 3 天原始快照；除该短期缓存场景外，非必要不新增 Redis 依赖范围。

### 3.2 前端
- Vue 3
- Vite
- TypeScript
- Element Plus（继续作为 Web 基础组件库）
- ECharts（继续作为 Web 图表库）
- Pinia
- Axios
- Vue Router
- Web 端必须建立并使用 `xo-design` tokens，统一覆盖 Element Plus 的颜色、圆角、阴影、按钮、输入框、表格、卡片、弹窗和分段控件。
- 前端真实接口统一放在 `xoassets-web/src/services/*Api.ts`，公共请求逻辑放在 `xoassets-web/src/services/http.ts`。

### 3.3 移动端
- uni-app 3.x（alpha）
- Vue 3
- Pinia
- TypeScript
- SCSS
- 自研 App 组件体系，不引入 Element Plus 或重型 UI 库。
- 移动端项目位于 `xoassets-app/`，独立于 Web 管理端，复用后端 API。
- 移动端 services 和 stores 结构与 Web 端一致，API 类型定义复用后端接口契约。
- 移动端 token 使用 `uni.storage` API，HTTP 请求通过 `uni.request` 封装。
- 移动端页面位于 `xoassets-app/src/pages/`，底部 Tab 含首页、记账、账户、投资、我的。
- 移动端页面必须走 `src/theme/`、`AppPage`、`AppCard`、`AppAmount`、`AppIcon`、`AppActionButton`、`AppSectionHeader`；弹窗、Picker、表单增强可按需评估轻量组件，但主视觉必须由 theme 控制。
- 移动端开发启动：`cd xoassets-app && npm install && npx uni --host 127.0.0.1 --port 5174`。
- 移动端构建：`cd xoassets-app && npx uni build`，产物在 `dist/build/h5/`。
- 登录 token 统一由 `xoassets-web/src/services/token.ts` 管理，请求通过 `Authorization: Bearer <token>` 传递。
- 未联调页面可以继续使用 `financeService` 和 `src/mock`，但已接后端页面不要回退到 mock。

## 4. 模块边界

后端模块建议按业务域拆分：

```text
com.xoassets
├── common
├── module
│   ├── auth
│   ├── account
│   ├── category
│   ├── transaction
│   ├── investment
│   ├── budget
│   ├── goal
│   ├── report
│   ├── dashboard
│   └── statistics
└── persistence
```

- 后端业务层使用 `service` 接口 + `service/impl` 实现类结构，Controller 和跨模块调用优先依赖接口。

第一版必须优先完成：
- 登录注册
- 账户管理
- 分类管理
- 收支记账
- 转账
- 首页仪表盘
- 图表统计

第一版建议做：
- 投资持仓手动维护
- 每日汇总
- AI 日报
- 预算管理
- 资产目标

第一版不建议做：
- 银行卡 / 支付宝 / 微信 / 券商自动同步
- 投资买卖建议
- 社交功能
- 复杂后台管理

## 5. 业务与数据口径
- 所有业务表必须带 `user_id`，并通过当前登录用户隔离数据。
- 总资产 = 现金类账户余额 + 投资资产市值。
- 总负债 = 信用卡负债 + 借款 + 贷款。
- 净资产 = 总资产 - 总负债。
- 今日净资产变化 = 今日净资产 - 昨日净资产。
- 投资浮动盈亏 = 当前市值 - 持仓成本。
- 投资收益率 = 浮动盈亏 / 持仓成本 * 100%。
- 转账不计入收入支出，只影响账户余额，不影响净资产。

## 6. 核心事务规则
- 新增支出：保存流水、扣减账户余额、刷新当日汇总必须在同一事务中完成。
- 新增收入：保存流水、增加账户余额、刷新当日汇总必须一致提交。
- 转账：保存流水、转出扣款、转入加款、刷新汇总必须一致提交。
- 投资买入：保存买入流水、扣减现金账户、增加或更新持仓、重算平均成本、刷新汇总。
- 投资卖出：校验持仓数量、保存卖出流水、减少持仓、增加现金账户、计算已实现盈亏、刷新汇总。
- 删除或修改流水时，必须处理账户余额反向恢复和每日汇总刷新。

## 7. 接口约定
- 登录注册接口使用 `/api/auth/**`。
- 当前用户接口使用 `/api/auth/me`。
- 首页接口使用 `/api/dashboard/**`。
- 流水接口使用 `/api/transactions/**`。
- 账户接口使用 `/api/accounts/**`。
- 分类接口使用 `/api/categories/**`。
- 图表接口使用 `/api/statistics/**`。
- 公共投资资产接口使用 `/api/assets/**`。
- 持仓接口使用 `/api/holdings/**`。
- 投资交易接口使用 `/api/investment-transactions/**`。
- 手动行情接口使用 `/api/quotes/**`。
- 汇率展示接口使用 `/api/exchange-rates/**`，前端只读后端缓存汇率，不直连第三方汇率源。
- 资产快照接口使用 `/api/snapshots/**`，用于首页净资产变化、趋势图、AI 报告和资产目标分析；`POST /api/snapshots/rebuild` 用于批量补录或对账后按日期区间重建当前用户资产快照。
- 账户资金明细接口使用 `/api/accounts/{id}/ledger`，账户详情统计使用 `/api/accounts/{id}/flow-statistics`，余额修正和账户余额曲线使用 `/api/accounts/{id}/balance-adjustments`、`/api/accounts/{id}/balance-trend`；账户详情只展示当前余额、累计流入、累计流出、余额修正、余额曲线和支出分类，不再展示投资资金流向图。
- CSV 导出接口使用 `/api/export/**`，只允许导出当前登录用户自己的数据。
- 预算接口使用 `/api/budgets/**`。
- 资产目标接口使用 `/api/goals/**`。
- AI 报告接口使用 `/api/reports/**`。
- 接口新增或调整时，同步考虑 Swagger / Knife4j 文档。
- 后端返回给前端的 Long ID 必须按字符串处理，前端不得用 `number` 保存业务 ID，避免 JavaScript 精度丢失。
- 投资模块中 `xo_asset`、`xo_asset_price_current`、`xo_asset_price_daily` 为公共数据不带 `user_id`；`xo_holding`、`xo_investment_transaction`、`xo_investment_holding_daily_profit`、`xo_investment_daily_snapshot` 必须通过当前登录用户隔离。
- 前端投资模块只暴露“持仓”概念，资产代码、类型、币种和行情源在持仓表单内维护；后端 `xo_asset` 只作为内部行情基础数据。
- 新增持仓优先通过 `GET /api/assets/lookup` 自动识别资产信息；查询失败不能阻塞手动录入。保存持仓时若带 `latestPrice`，后端必须写入 `xo_asset_price_current`，带 `quoteTime` 时同步写入 `xo_asset_price_daily` 作为日级初始价；历史持仓补录必须通过投资买入 / 卖出交易表达，不用直接修改当前持仓数量倒推历史。
- 投资主页只展示聚合统计和图表，投资分布按具体投资产品统计，总投资资产曲线优先使用资产快照，收益贡献独占整行并支持总 / 当日 / 当月 / 当年切换且优先显示资产名称；基金、股票、虚拟货币模块分别承载持仓明细、买入卖出、编辑、价格刷新等操作；单个持仓详情使用 `/investments/holdings/:id`，详情页展示市值趋势、收益日历和交易记录；详情市值趋势必须按价格日期重建该持仓历史份额，不能用当前份额倒推历史。
- 投资买入 / 卖出必须选择当前用户资金账户；买入扣减账户余额，卖出增加账户余额，交易、账户和持仓更新必须在同一事务内完成，且不写入普通流水；投资交易补录、基金确认或撤销后必须从交易日起重建投资日快照和资产快照。
- 基金买入默认支持 `AMOUNT_NAV` 金额净值录入，基金可先创建 0 份额持仓；`transaction_time` 表示用户实际买入时间，后端通过 `xo_market_calendar` 计算有效申请日和确认日，15:00:00 及以前按当天申请，15:00 后或非交易日顺延，普通基金 T+1，名称包含 `QDII` 的基金 T+2；确认日净值缺失时交易保持 `PENDING_CONFIRM`，后续定时任务确认，股票等资产继续使用 `QUANTITY_PRICE` 数量价格录入。
- 投资交易撤销必须使用 `cost_amount` 反向恢复历史成本，不物理删除交易；已撤销交易保留展示但不参与账户资金明细汇总。
- 投资数量统一保留 10 位小数，手续费、成本、市值、盈亏和收益率统一按 4 位小数计算；当前价和日级价保留 8 位，并记录 previous_close、change_amount、change_percent、market_status，持仓接口返回 `priceScale`，CRYPTO 当前价至少展示 6 位，FUND / STOCK 展示 4 位。
- 公共资产 `xo_asset` 必须写入 `market`，股票为 SH / SZ / BJ / US，基金为 CN_FUND，虚拟货币为 CRYPTO；资产唯一性按 `type + market + symbol + deleted` 判断。
- 持仓收益分析字段由后端基于 `xo_asset_price_current` 当前价、`xo_asset_price_daily` 日级价格和 `xo_investment_holding_daily_profit` 持仓每日收益表计算，包括今日收益、昨日收益、浮动盈亏、收益率和回本涨幅；所有资产只有当前价格日期等于今天时才计算今日收益，其中基金和股票还必须当天为交易日，休市日返回 `todayPriceAvailable=false`、`priceStatus=MARKET_CLOSED` 并展示“休市”，交易日未更新返回 `priceStatus=TODAY_PRICE_NOT_AVAILABLE`，收益基准价格或基准数量缺失时对应字段返回 `null`，前端展示“暂无 / --”，不能用历史价、兜底价或其他值冒充今日价；今日收益同时返回当前 / 今日有效份额口径和上一交易日日终份额归因口径，Web 默认展示当前份额口径；每日收益日历、昨日收益、投资趋势每日收益和收益贡献必须从 `xo_investment_holding_daily_profit` 聚合，缺行时显示 `--`；持仓汇总、投资总览和模块卡通过 `todayProfitAvailable` / `primaryProfitAvailable` 标记今日收益是否可展示，不可用时金额返回 `null` 并显示 `--`，同时用 `todayProfitStatusLabel` / `primaryProfitStatusLabel` 说明“今日休市”或净值未更新原因。
- 持仓估值只能使用与资产币种一致的 `xo_asset_price_current` 当前价；前端展示市值、成本和盈亏必须使用后端返回字段，不得用格式化后的当前价反算。
- 账户余额校准必须生成 `xo_account_balance_adjustment` 修正事件；该事件进入账户账本和余额曲线，但不计入普通收入 / 支出统计。`PUT /api/accounts/{id}` 可兼容余额差异，但 Web/App 应优先调用专用余额修正接口。
- 账户详情页通过聚合普通流水、投资交易和余额修正展示资金变化，投资买入计入账户流出，投资卖出计入账户流入，但不进入普通收支统计。
- 行情刷新通过 `QuoteProvider` 扩展；CRYPTO 使用 CoinGecko，FUND 使用天天基金 F10 历史净值表和实时净值兜底，A 股使用新浪行情，美股使用 Yahoo Finance。第三方行情只能由后端调用，前端只调 XOAssets `/api/quotes/**`。
- 投资页 CNY / USD 切换使用下拉框，默认人民币；USD/CNY 汇率由后端日缓存提供，MVP 可用进程内缓存，后续可替换为 Redis。
- 第三方资产查询失败时，后端日志必须保留行情源、代码 / 市场、响应摘要和异常堆栈；前端错误提示保持简洁，不暴露第三方原文。
- 行情缓存 TTL：CRYPTO 15 分钟、STOCK 15 分钟、FUND 1 天、MANUAL 不过期；自动行情刷新写 `xo_asset_price_current`，手动行情写当前价并直接沉淀单点日级价；手动和自动行情刷新成功后都必须重建受影响资产的 `xo_investment_holding_daily_profit`，即使第三方返回的行情时间和价格与 current 完全相同，也要触发收益重建，避免收益日历停留在旧计算结果；股票和虚拟货币原始快照写入 Redis ZSET `price:snapshot:{assetId}:{yyyyMM}`，TTL 3 天；读取某天原始快照必须通过当天起止毫秒 score 范围查询，禁止全量拉取整月 ZSET 后内存筛选；`xo_asset_price_daily` 保存长期日级价格，`xo_investment_holding_daily_profit` 保存每个持仓展示日真实收益；`xo_investment_daily_snapshot` 通过投资交易流水重建历史持仓和当日投资本金净流入后保存用户投资日快照；`daily_profit` 是快照日资金流调整收益，不等同于收益日历展示日收益，`calendar_profit` 才是持仓每日收益表按展示日聚合后的收益；`buy_amount` / `sell_amount` / `fee_amount` 按资金实际发生日统计，基金确认日只影响份额生效；股票只在 09:30-15:30 之间拉取第三方行情；刷新失败保留最近价格，定时刷新失败不能影响应用启动。
- 投资日快照补跑按 `trade_date` 使用已回填的 `xo_asset_price_daily` 日级价格，不以价格行 `created_at` 限制历史修正，确保周末后和净值延迟时历史市值可被修正。
- 基金金额买入从实际申购日至确认日前按在途投资资产计入；交易后续确认后，补跑确认日前历史快照仍必须保留这段在途金额，避免已扣款但未确认份额导致净资产假跌。
- `xo_market_calendar` 是交易日判断的数据库权威来源；年度补齐任务只生成基础周末日历，交易所特殊休市日通过迁移脚本或人工修正写入数据库，禁止把年度休市日写死在 Java 或 yml。
- 预算使用额从 `xo_transaction` 汇总，转账不计入，退款抵扣支出；预算接口必须按当前 user_id 隔离。
- 资产快照写入 `xo_asset_snapshot`，同一用户同一天重复生成必须更新原记录；现金资产必须按账户初始余额 + 快照日前普通流水 / 投资交易 / 余额修正重建历史余额，正余额计入现金资产、负余额绝对值计入负债，不能用当前 `xo_account.balance` 回填历史；投资资产必须通过 `InvestmentPositionHistoryService.positionsAt(snapshotDate)` 重建快照日历史头寸，再使用同币种日级价 / 当前价估值，不能用当前 `xo_holding` 数量回填历史；普通流水补录、修改或删除后必须从受影响日期起触发资产快照重建，投资交易补录、基金确认或撤销后必须先重建持仓每日收益和投资日快照，再重建资产快照，31 天内同步重建，长跨度任务写入 `xo_snapshot_rebuild_task` 后由 `rebuildPendingAssetSnapshots` 批量处理。
- `/api/snapshots/latest` 的较昨日 / 较月初变化缺少基准快照时必须返回 `null`，前端展示 `--`，不能用 0 冒充缺失对比。
- 首页和统计总资产口径优先使用资产快照：总资产 = 现金资产 + 投资资产，净资产 = 总资产 - 负债；没有快照时页面可退回当前实时概览。
- 资产目标当前金额可手动填写，也可按当前净资产口径写入；目标接口必须按当前 user_id 隔离。
- AI 报告当前为模板化财务复盘，基于真实统计数据生成并保存到 `xo_ai_report`，不调用真实 AI，不提供投资买卖建议。
- 核心业务页需要保留空状态、删除二次确认、统一 loading、后端 message 展示和金额大于 0 的前端拦截。
- MVP 验收数据位于 `xoassets-server/src/main/resources/db/dev-data.sql`，全新库先执行 `schema.sql`，再执行 `migration-market-calendar.sql` 初始化交易所休市修正，最后执行 `dev-data.sql`，测试账号为 `demo / xoassets123`。
- 本地 Docker 一键启动使用仓库根目录 `docker compose up -d`，前端 Nginx 访问 `http://localhost:8088`，后端和 Knife4j 访问 `http://localhost:8080`。

## 8. 前端设计约定
- 整体风格：简洁、专业、清晰、数据感、轻量、安全感。
- 当前 Web 视觉基线已统一为现代金融 SaaS 风格：浅灰蓝背景、蓝色主色、白色玻璃卡片、柔和阴影、大圆角和宽松留白；后续前端迭代优先延续这一套，不要回退到朴素后台风或深色侧栏方案。
- Web 第一版采用左侧菜单 + 顶部用户信息 + 主内容区。
- 移动端适配可采用底部 Tab + 卡片式内容。
- 页面优先级：
  - P0：登录、注册、首页仪表盘、记账流水、新增记账、账户管理、投资持仓、统计分析。
  - P1：AI 报告、预算管理、资产目标、分类管理。
  - P2：系统设置。
- Web 端继续使用 Element Plus + ECharts；图表优先使用 ECharts，表单和基础组件优先使用 Element Plus。
- Web 端必须建立 `xo-design` tokens，并通过 `xoassets-web/src/styles/variables.css`、`global.css`、`layout.css` 统一覆盖 Element Plus 的颜色、圆角、阴影、按钮、输入框、表格、卡片、弹窗和分段控件；禁止为单个页面重复造独立视觉规则。
- 登录页和业务页视觉参考统一以 `xoassets-web/原型图/` 下的设计图为准；做 UI 调整时优先复用现有布局骨架和自定义 CSS，不引入新的重型 UI 框架。
- 移动端保留 uni-app + Vue3 + SCSS + 自研 App 组件体系，禁止引入 Element Plus 或重型 UI 库。
- 移动端主题系统统一维护在 `xoassets-app/src/theme/`，主题配置必须保持 `colors`、`components`、`icons`、`assets`、`pageTokens` 等 schema 完整；所有页面必须优先使用 `AppPage`、`AppCard`、`AppAmount`、`AppIcon`、`AppActionButton`、`AppSectionHeader`，页面级布局 token 和素材入口不要硬编码散落在业务页面。
- 移动端弹窗、Picker、表单增强可按需评估轻量组件，但主视觉必须由 theme 控制；移动端投资页等原型复刻页面要优先使用主题 token、语义按钮变体和局部组件承载复杂区域，新增/调整后需核对原型关键视觉、字段口径和移动端首屏表现。

## 9. 安全要求
- 使用 JWT 登录认证，Token 放在 `Authorization` Header。
- 后端所有查询、修改、删除必须校验当前登录用户 ID 与数据所属 `user_id` 一致。
- 密码禁止明文存储，使用 BCrypt 加密。
- 日志中不要打印金额明细、账户信息、Token、密码、AI 请求敏感内容。
- 运行日志写入 `logs/` 并已加入 `.gitignore`；提交前不要把本地日志文件加入 Git。
- AI 请求前尽量脱敏个人财务数据。
- 外部输入必须校验，异常路径要显式处理。

## 10. 异常处理要求
- 金额为空：提示金额必填。
- 金额小于等于 0：提示金额必须大于 0。
- 账户不存在：提示账户不存在。
- 分类不存在：提示分类不存在。
- 转账账户相同：不允许同账户转账。
- 投资卖出数量超过持仓：提示持仓不足。
- 行情获取失败：使用最后一次可用价格，并标记价格过期。
- AI 报告生成失败：标记失败，并支持手动重试。

## 11. 定时任务
- 定时任务统一由 XXL-JOB Admin 触发，后端仅注册 executor handler；本地未启动 XXL-JOB Admin 或未开启 `XXL_JOB_EXECUTOR_ENABLED=true` 时不会自动执行定时任务。
- 股票 / 虚拟货币行情同步：每 15 分钟；股票仅交易日 09:30-15:30 拉取，虚拟货币全天拉取。
- 基金净值晚间刷新：每天 18:00 首轮，18:15-23:45 每 15 分钟强制跟进刷新。
- 资产日级价格聚合：每天 20:00-23:45 每 15 分钟处理晚间汇总和收尾汇总；股票 / 虚拟货币从 Redis 原始快照聚合，基金由净值刷新直接写日级价。
- 投资资产日快照：每天 20:00 首轮，20:15-23:45 每 15 分钟先重建持仓每日收益，再 upsert 最近 4 个自然日投资快照。
- 资产快照：每天 20:00-23:45 每 15 分钟生成所有启用用户当天快照，最后一轮为 23:45。
- 待重建资产快照处理：每天 10:00、15:00、21:00 处理 `xo_snapshot_rebuild_task`；投资触发的任务先补投资日快照再补资产快照。
- 基金待确认交易扫描：每 3 小时扫描一次。
- 每日汇总：每天 00:10 生成昨日财务汇总。
- AI 日报：每天 00:20 生成昨日 AI 报告。
- 预算检查：每天 09:00 检查预算超支。
- 价格过期检查：每小时检查行情是否过期。

## 12. 实施原则
- 先读代码和相关文档，再改代码。
- 优先最小改动，禁止无关重构。
- 不修改与任务无关文件。
- 不新增不必要依赖，优先复用现有项目能力。
- 保持原有排版、缩进、空行、注释位置，不做顺手格式化。
- 禁止删除、改写、搬动原有注释，除非对应逻辑已实际修改且注释失真。
- 每次更改代码都要写明必要注释；注释应解释业务原因或非显然逻辑，避免空话。
- 项目代码必须保持基础注释覆盖：类 / 组件 / 接口 / 类型需要说明职责，公共方法和业务方法需要说明用途；复杂逻辑要补充关键业务口径、边界条件和为什么这么处理。
- 不确定且高风险时先澄清；低风险场景做合理假设并推进。

## 13. 验证要求
- 后端改动优先运行相关单元测试或最小范围测试。
- 前端改动优先运行相关 lint / typecheck。
- Java 程序不用自行编译测试，说明建议命令即可。
- 前端项目不用自行启动构建打包，说明启动 / 构建 / 打包命令即可。
- 若无法验证，要明确说明原因和剩余风险。

## 14. Git 约定
- 未经用户明确要求，不执行 `git add`、`git commit`、`git push`、创建分支或创建 PR。
- 写完代码后只汇报改动内容、验证结果、剩余风险，等待用户确认是否提交。
- 用户要求提交到 Git 时，默认使用中文提交描述。
- Commit 简单说明本次修改内容即可，推荐格式：`type(scope): summary`。
- 阶段性功能完成后，必须同步 agent 记忆、更新相关文档，并提交到 Git。

## 15. 沟通风格
- 简洁直接，先给结论，再解释原因。
- 能给步骤就给实际步骤，能给代码就给代码。
- 遇到风险点直接提醒。
- 不确定的地方明确说，不要编造。
- 自动寻找与当前需求匹配的 skills 执行，并简要说明。
