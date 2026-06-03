# 小〇财迹 · 移动端 App

基于 uni-app 3.x + Vue 3 + Pinia + TypeScript 的移动端应用，复用 xoassets-server 后端接口。

## 技术栈

- **框架**: uni-app 3.x（alpha）
- **语言**: TypeScript
- **UI**: Vue 3 + SCSS
- **状态管理**: Pinia
- **构建**: Vite 5

## 项目结构

```
src/
├── components/         # 通用 UI 组件
├── pages/              # 17 个页面
│   ├── index/          # 首页仪表盘（Tab1）
│   ├── add/            # 快速记账（Tab2）
│   ├── accounts/       # 账户列表（Tab3）
│   ├── investments/    # 投资持仓（Tab4）
│   ├── mine/           # 我的（Tab5）
│   ├── login/          # 登录
│   ├── register/       # 注册
│   ├── transactions/   # 流水列表
│   ├── transaction-detail/ # 流水详情
│   ├── account-detail/ # 账户明细
│   ├── holding-detail/ # 持仓详情
│   ├── investment-distribution/ # 资产分布
│   ├── holding-analysis/ # 持仓分析
│   ├── categories/     # 分类管理
│   ├── budgets/        # 预算管理
│   ├── goals/          # 资产目标
│   └── reports/        # AI 报告
├── services/           # API 封装（复用后端接口契约）
├── stores/             # Pinia 状态管理
├── theme/              # 主题配置、图标映射和运行时应用
├── styles/             # 全局样式和设计变量
├── pages.json          # 路由 + Tab 配置
└── manifest.json       # uni-app 多端配置
```

## 底部 Tab

| Tab | 页面 | 说明 |
|-----|------|------|
| 首页 | `pages/index/index` | 净资产、收支概览、最近流水 |
| 记账 | `pages/add/add` | 日历流水、快速录入、图片与键盘输入 |
| 账户 | `pages/accounts/accounts` | 账户余额、账户明细 |
| 投资 | `pages/investments/investments` | 持仓汇总、持仓卡片 |
| 我的 | `pages/mine/mine` | 分类/预算/目标/报告入口、退出 |

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器（H5 模式）
npx uni --host 0.0.0.0 --port 5174 
# 手机浏览器访问 http://<你的IP>:5174

# 生产构建
npx uni build
# 产物在 dist/build/h5/

# 类型检查
npm run type-check
```

## 设计风格

延续 Web 端浅蓝金融 SaaS 风格：

- 主色 `#4A90D9`，浅灰蓝背景 `#F0F4F8`
- 卡片化布局，16-24rpx 大圆角
- 柔和阴影 `0 4rpx 20rpx rgba(0,0,0,0.06)`
- 金额数字加粗突出，按钮高度 88rpx 适配手指点击
- 收入绿色 / 支出红色 / 转账蓝色

当前投资页已按原型方向做了第一轮落地：
- 顶部蓝色渐变资产卡
- 资产分布 donut + 分类金额列表
- 持仓三列表格
- 买入 / 转换 / 卖出胶囊按钮
- 行情模块不在投资首页展示

投资子页面已补齐：
- `资产分布` 页：总资产卡、资产分布 donut、资产变化趋势、分布洞察
- `持仓分析` 页：持仓市值 / 成本 / 收益 / 收益率、收益贡献排行、持仓明细、风险分布

当前记账页已按原型方向做了第一轮落地：
- 顶部月份切换、搜索和筛选入口
- 日历驱动的当日流水展示
- 流水录入页改为全屏快速录入样式
- 分类网格、最近使用、图片入口和自定义数字键盘

## 主题系统

主题系统集中放在 `src/theme/`，用于统一管理颜色、图标、背景、圆角、阴影、字体和间距。

```text
src/theme/
├── index.ts                 # 主题注册和统一导出
├── types.ts                 # ThemeConfig 类型定义
├── icon-map.ts              # 主题图标语义映射
├── useTheme.ts              # 读取当前主题的 composable
├── applyTheme.ts            # 同步 CSS 变量、导航栏和 TabBar 样式
├── helpers.ts               # 读取主题颜色/图标的辅助方法
└── themes/
    ├── classic-blue.ts      # 当前蓝白金融风
    ├── tech-dark.ts         # 预留科技深色风
    └── cartoon-soft.ts      # 预留卡通柔和风
```

主题状态由 `src/stores/theme.ts` 管理，默认主题为 `classic-blue`，会持久化到 `uni.storage`。

## 组件系统

为后续复刻原型，当前已建立 `src/components/app/` 通用组件层，组件样式优先依赖 theme，不在页面里重复散落卡片、金额、按钮和图标样式。

```text
src/components/app/
├── AppPage.vue             # 页面容器，统一背景、安全区和 padding
├── AppCard.vue             # 统一卡片背景、圆角、阴影、间距
├── AppAmount.vue           # 金额显示，支持尺寸和正负色
├── AppIcon.vue             # 从 theme.icons 读取图标
├── AppActionButton.vue     # 通用操作按钮，支持主题变体
├── AppSectionHeader.vue    # 标题 + 右侧操作
└── AppBottomTabs.vue       # 底部 Tab UI 能力层
```

按钮变体目前支持：
- `primary`
- `secondary`
- `success`
- `danger`
- `purple`

页面后续接入建议：
1. 页面最外层优先使用 `AppPage`
2. 业务区块优先使用 `AppCard`
3. 金额统一使用 `AppAmount`
4. 图标统一使用 `AppIcon`
5. 操作按钮统一使用 `AppActionButton`
6. 标题栏统一使用 `AppSectionHeader`

新增主题时：
1. 在 `src/theme/themes/` 新建主题配置文件，实现完整 `ThemeConfig`。
2. 在 `src/theme/index.ts` 注册主题。
3. 页面优先使用 CSS 变量或 `src/theme/helpers.ts` 读取语义化配置，不要直接写死颜色或图标路径。

已接入主题变量的范围：
- 全局 `page`、`.card`、`.btn-primary`、`.btn-outline`、`.tag-*`、`.amount.*`
- 启动时通过 `App.vue` 应用当前主题
- 首页、账户、投资、登录、注册、记账、分类、我的页面的核心背景/卡片/图标 fallback
- 原生导航栏和 TabBar 色彩通过 `applyTheme.ts` 运行时同步
- 通用组件 `AppPage`、`AppCard`、`AppAmount`、`AppIcon`、`AppActionButton`、`AppSectionHeader`、`AppBottomTabs` 已直接依赖主题配置
- 投资页已通过 `theme.charts.investmentDistribution`、`theme.backgrounds.investmentSummaryCard`、`theme.components.button.variants` 统一控制视觉

## 接口

- 后端地址：开发时 Vite 代理 `/api` → `http://localhost:8080`
- Token 管理：`uni.storage`，请求自动携带 `Authorization: Bearer <token>`
- 401 处理：自动清除 token 并跳转登录页
- 错误展示：后端 `message` 字段直接 toast 提示

## 第一版范围

✅ 已实现：
- 登录 / 注册
- 首页资产概览
- 快速记账（支出 / 收入 / 转账）
- 流水列表与详情（筛选、删除二次确认）
- 账户列表与明细（含投资买卖记录）
- 投资持仓与详情
- 分类管理、预算管理、资产目标、AI 报告

❌ 第一版不做：
- 复杂图表
- CSV 导出
- 股票/基金复杂筛选
- 推送通知
- 生物识别登录
- 离线模式

## 与 xoassets-web 的关系

- 完全独立的前端项目，不共享代码
- 复用同一套后端 API 接口
- 设计风格一致（浅蓝金融 SaaS）
- 可并行开发、独立部署

## 原生 App 打包

当前 H5 模式可直接在手机浏览器使用。如需打包 iOS / Android 原生 App，需使用 HBuilderX 导入本项目。
