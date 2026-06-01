# 小〇财迹

小〇财迹的 Vue3 前端项目，用于呈现资产、流水、账户、投资、分析、AI 报告、预算和目标等核心页面。

## 技术栈

- Vue3 + TypeScript + Vite
- Vue Router
- Pinia
- Element Plus
- ECharts
- Axios

## 接口联调状态

- 已接入真实接口：登录、注册、用户中心、账户管理、分类管理、记账流水、投资持仓、虚拟货币 / 基金 / 股票行情刷新、预算管理、首页仪表盘、数据分析、资产目标、AI 报告模板。
- 暂用 mock 数据：暂无。
- 请求封装：`src/services/http.ts` 统一处理 `/api` baseURL、JWT Header、业务响应和 401 跳转。
- API 模块：`src/services/authApi.ts`、`src/services/accountApi.ts`、`src/services/categoryApi.ts`、`src/services/transactionApi.ts`、`src/services/investmentApi.ts`、`src/services/budgetApi.ts`、`src/services/dashboardApi.ts`、`src/services/statisticsApi.ts`、`src/services/goalApi.ts`、`src/services/reportApi.ts`。
- 账户页：支持编辑账户当前余额，用于利息、漏记流水等现实余额校准；账户卡片可进入 `/accounts/:id` 查看资金明细、流向统计和导出账户明细。
- 记账页：支持分页、备注、图片上传和普通流水 CSV 导出，图片第一版以 Data URL 随流水保存。
- 投资页：前端只保留“持仓”概念，支持在持仓中维护资产名称、代码、类型、币种、行情源；主页只展示总投资 / 基金 / 股票 / 虚拟货币统计和图表，明细页支持按类型分页查看并完成新增、编辑、买入、卖出、单个刷新、批量刷新、手动价格、删除等操作，点击持仓行可进入 `/investments/holdings/:id` 查看单个持仓详情。
- 行情提示：CRYPTO 使用 CoinGecko id，例如 `bitcoin`；基金使用代码，例如 `000001`；A 股使用 `600519.SH`；美股使用 `AAPL`。前端只调用 XOAssets 后端接口，不直连第三方行情。
- 投资交易：买入弹窗必须选择扣款账户，卖出弹窗必须选择到账账户，交易记录展示资金账户和已实现盈亏。
- 投资撤销：投资交易记录支持撤销，撤销后刷新持仓、交易、账户和汇总数据，已撤销交易禁用撤销按钮。
- 投资收益：明细页展示投资总市值、今日收益、昨日收益、总收益、收益率、持仓数量，并在表格展示当前价、昨价、今日涨跌、今日收益、昨日收益、总收益和回本涨幅；持仓详情页展示当前市值、持仓数量、成本、今日收益、总收益、总收益率、已实现盈亏、回本涨幅、价格快照和当前持仓交易记录。
- 投资币种：虚拟货币新增时默认 USD，投资页默认以人民币展示，可切换 USD，并通过页面汇率输入做 USD/CNY 换算。
- 投资精度：投资数量、手续费、成本、市值、盈亏和收益率统一按 4 位小数展示；当前价按后端 `priceScale` 展示，CRYPTO 至少 6 位，FUND / STOCK 4 位，前端不使用格式化价格反算市值。
- 投资表格：明细页表格固定 560px 高度，持仓列固定左侧，操作列固定右侧，数量、价格、金额和收益率列右对齐且不换行。
- 导出：账户详情、记账流水和投资明细页支持按当前筛选条件导出 CSV。
- 预算页：支持月度总预算、分类预算、进度条、正常 / 预警 / 超支状态和删除确认。
- 首页和统计页：从真实接口读取账户、流水、投资、预算聚合指标和图表数据。
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
