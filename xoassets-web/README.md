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

- 已接入真实接口：登录、注册、用户中心、账户管理、分类管理、记账流水、投资持仓、虚拟货币行情刷新、预算管理、首页仪表盘、数据分析、资产目标、AI 报告模板。
- 暂用 mock 数据：暂无。
- 请求封装：`src/services/http.ts` 统一处理 `/api` baseURL、JWT Header、业务响应和 401 跳转。
- API 模块：`src/services/authApi.ts`、`src/services/accountApi.ts`、`src/services/categoryApi.ts`、`src/services/transactionApi.ts`、`src/services/investmentApi.ts`、`src/services/budgetApi.ts`、`src/services/dashboardApi.ts`、`src/services/statisticsApi.ts`、`src/services/goalApi.ts`、`src/services/reportApi.ts`。
- 账户页：支持编辑账户当前余额，用于利息、漏记流水等现实余额校准。
- 记账页：支持分页、备注和图片上传，图片第一版以 Data URL 随流水保存。
- 投资页：前端只保留“持仓”概念，支持在持仓中维护资产名称、代码、类型、币种、行情源；主页只展示总投资 / 基金 / 股票 / 虚拟货币统计和图表，明细页支持按类型分页查看并完成新增、编辑、买入、卖出、刷新价格、删除等操作。
- 投资币种：虚拟货币新增时默认 USD，投资页默认以人民币展示，可切换 USD，并通过页面汇率输入做 USD/CNY 换算。
- 投资精度：投资数量、价格、手续费、成本、市值、盈亏和收益率统一按 4 位小数输入、计算和展示。
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

## 目录说明

- `src/layouts`：应用级布局、侧边栏和顶部栏。
- `src/views`：业务页面。
- `src/components/finance`：金额、指标卡、状态标签、趋势值等统一金融展示组件。
- `src/components/charts`：ECharts 图表封装。
- `src/mock`：页面使用的本地 mock 数据。
- `src/services`：HTTP 请求封装、认证 / 账户 / 分类 / 流水 API 和暂未联调页面的 mock 服务。
- `src/types`：业务类型定义。
- `src/styles`：全局样式和主题变量。
