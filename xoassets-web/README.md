# 小〇财迹

 小〇财迹的 Vue3 前端原型项目，使用 mock 数据呈现资产、流水、账户、投资、分析、AI 报告、预算和目标等核心页面。

## 技术栈

- Vue3 + TypeScript + Vite
- Vue Router
- Pinia
- Element Plus
- ECharts

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
- `src/services`：预留 API 服务封装，目前读取 mock 数据。
- `src/types`：业务类型定义。
- `src/styles`：全局样式和主题变量。
