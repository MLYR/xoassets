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

- 已接入真实接口：登录、注册、用户中心、账户管理、分类管理、记账流水、投资持仓。
- 暂用 mock 数据：首页仪表盘、数据分析、AI 报告、预算管理、资产目标。
- 请求封装：`src/services/http.ts` 统一处理 `/api` baseURL、JWT Header、业务响应和 401 跳转。
- API 模块：`src/services/authApi.ts`、`src/services/accountApi.ts`、`src/services/categoryApi.ts`、`src/services/transactionApi.ts`、`src/services/investmentApi.ts`。
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
