# 小〇财迹

小〇财迹的 Vue3 前端项目，用于呈现资产、流水、账户、投资、分析、AI 报告、预算和目标等核心页面。

## 技术栈

- Vue3 + TypeScript + Vite
- Vue Router
- Pinia
- Element Plus
- ECharts
- Axios

## 当前视觉基线

- 设计参考：`原型图/` 目录下的页面截图。
- 整体风格：现代金融 SaaS，浅灰蓝背景、蓝色主色、白色玻璃卡片、柔和阴影、大圆角、宽松留白。
- Web 端继续使用 Element Plus + ECharts；Element Plus 作为基础组件库，ECharts 作为图表库。
- 建立并维护 `xo-design` tokens，统一覆盖 Element Plus 的颜色、圆角、阴影、按钮、输入框、表格、卡片、弹窗和分段控件。
- 主题样式入口：
  - `src/styles/variables.css`：`xo-design` token 入口，集中定义颜色、圆角、阴影、图表色板等设计变量。
  - `src/styles/global.css`：Element Plus 按钮、输入框、表格、卡片、弹窗、分段控件等全局覆盖。
  - `src/styles/layout.css`：页面容器、栅格、面板和通用排版辅助类。
- 统一组件：
  - `src/components/finance/MetricCard.vue`
  - `src/components/finance/AmountText.vue`
  - `src/components/finance/StatusBadge.vue`
  - `src/components/finance/TrendValue.vue`
- 当前重点优化页面：登录页、首页仪表盘、账户管理、账户详情、记账流水、投资模块、持仓详情、预算管理、统计分析、AI 报告。
- 后续做 UI 调整时，优先改 `xo-design` tokens、全局变量和公共样式，再做页面局部补充；禁止为单个页面重复造独立视觉规则。

## 接口联调状态

- 已接入真实接口：登录、注册、用户中心、账户管理、分类管理、记账流水、投资持仓、虚拟货币 / 基金 / 股票行情刷新、预算管理、首页仪表盘、数据分析、资产目标、AI 报告模板。
- 暂用 mock 数据：暂无。
- 请求封装：`src/services/http.ts` 统一处理 `/api` baseURL、JWT Header、业务响应和 401 跳转。
- API 模块：`src/services/authApi.ts`、`src/services/accountApi.ts`、`src/services/categoryApi.ts`、`src/services/transactionApi.ts`、`src/services/investmentApi.ts`、`src/services/budgetApi.ts`、`src/services/dashboardApi.ts`、`src/services/statisticsApi.ts`、`src/services/snapshotApi.ts`、`src/services/goalApi.ts`、`src/services/reportApi.ts`。
- 账户页：余额校准调用 `POST /api/accounts/{id}/balance-adjustments` 生成专用修正记录；账户卡片可进入 `/accounts/:id` 查看资金明细、余额修正、账户余额曲线、支出分类和导出账户明细。
- 记账页：支持分页、备注、单张 10MB 内图片上传和普通流水 CSV 导出，图片第一版以 Data URL 随流水保存。
- 投资页：前端只保留“持仓”概念，支持通过资产类型 + 代码 / 名称搜索自动带出资产名称、代码、市场、币种、行情源、行情键和当前价格，也保留手动录入；投资模块按总览 / 基金 / 股票 / 虚拟货币拆分展示，总览资产趋势同图展示总览、股票、基金、虚拟货币四条线并支持周 / 月 / 年切换，金额轴单位为 k；各类型模块内完成新增、编辑、买入、卖出、单个刷新、批量刷新、手动价格和清仓后删除等操作，点击持仓行可进入 `/investments/holdings/:id` 查看单个持仓详情。
- 行情提示：CRYPTO 使用 CoinGecko id，例如 `bitcoin`；基金使用代码，例如 `000001`；A 股使用 `600519.SH`；美股使用 `AAPL`。前端只调用 XOAssets 后端接口，不直连第三方行情。
- 投资交易：买入弹窗必须选择扣款账户，卖出弹窗必须选择到账账户；总览每日收益区域可切换到全持仓交易记录，全持仓交易记录与持仓详情交易记录统一类型 / 状态彩色标签，不展示已实现盈亏列，全持仓交易记录点击整行进入持仓详情，操作列保留撤销；基金金额买入使用“实际买入时间”日期时间选择器，并通过确认日预估接口展示 15:00 后、非交易日顺延和 QDII T+2 提示。
- 投资撤销：投资交易记录支持撤销，撤销后刷新持仓、交易、账户和汇总数据，已撤销交易禁用撤销按钮。
- 投资收益：模块持仓表展示持有市值、持有收益、今日收益 / 收益率、昨日收益、最新净值 / 当前价等字段，并支持这些表头排序，默认按持有市值倒序；收益基准缺失时展示 `--`，不使用其他字段伪造 0；持仓详情页展示当前市值、持仓数量、成本、今日收益、总收益、回本涨幅、价格快照和当前持仓交易记录；总市值走势读取详情接口 `chartPoints`，价格走势读取 `priceSnapshots`，清仓后不能用当前 0 份额乘历史价格画图；每日收益日历、昨日收益和趋势图每日收益读取后端 `xo_investment_holding_daily_profit` 口径，不从累计持有收益反算。
- 投资币种：虚拟货币新增时默认 USD，投资页默认以人民币展示，可切换 USD，并通过页面汇率输入做 USD/CNY 换算。
- 投资精度：CRYPTO 数量最多支持 10 位小数，FUND / STOCK 数量默认展示 4 位；手续费、成本、市值、盈亏和收益率统一按 4 位小数展示。当前价按后端 `priceScale` 展示，CRYPTO 至少 6 位，FUND / STOCK 4 位，前端不使用格式化价格反算市值。
- 投资表格：各类型模块表格固定 560px 高度，持仓列固定左侧，操作列固定右侧，持有市值、持有收益、今日收益 / 收益率、昨日收益、最新净值 / 当前价、数量、价格、金额和收益率列右对齐且不换行；持仓列表排序在分页前执行。
- 导出：账户详情和记账流水页支持按当前筛选条件导出 CSV。
- 预算页：支持月度总预算、分类预算、进度条、正常 / 预警 / 超支状态和删除确认。
- 首页和统计页：从真实接口读取账户、流水、投资、预算聚合指标和图表数据；首页主净资产使用 `/api/dashboard/overview` 的当前估算净资产，较昨日变化和快照趋势使用资产快照接口，数据分析页展示净资产、总资产、现金 / 投资资产快照趋势。
- 资产目标页：支持新增、编辑、删除、完成状态展示，以及使用当前净资产作为当前金额。
- AI 报告页：支持报告列表、报告详情和模板化报告生成，当前不调用真实 AI，不展示投资买卖建议。
- 稳定性：业务列表和图表补充空状态，删除操作均有二次确认，金额 / 价格 / 数量输入先在前端拦截无效值，接口错误统一展示后端 message。
- ID 约定：后端 Long ID 以字符串返回，前端接口类型统一用 `string` 接收和回传业务 ID。
- 本地代理：`vite.config.ts` 将 `/api` 转发到 `http://localhost:8080`。

## 常用命令

```bash
npm install
npm run dev
npm run build
npm run preview
```

## Docker 访问

从仓库根目录执行 `docker compose up -d` 后，前端由 Nginx 提供静态资源，访问 `http://localhost:8088`；`/api` 会代理到后端 `xoassets-server:8080`。

## 目录说明

- `src/layouts`：应用级布局、侧边栏和顶部栏。
- `src/views`：业务页面。
- `src/components/finance`：金额、指标卡、状态标签、趋势值等统一金融展示组件。
- `src/components/charts`：ECharts 图表封装。
- `src/mock`：页面使用的本地 mock 数据。
- `src/services`：HTTP 请求封装、认证 / 账户 / 分类 / 流水 API 和暂未联调页面的 mock 服务。
- `src/types`：业务类型定义。
- `src/styles`：全局样式和主题变量。
