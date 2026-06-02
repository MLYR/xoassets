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
├── pages/              # 15 个页面
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
│   ├── categories/     # 分类管理
│   ├── budgets/        # 预算管理
│   ├── goals/          # 资产目标
│   └── reports/        # AI 报告
├── services/           # API 封装（复用后端接口契约）
├── stores/             # Pinia 状态管理
├── styles/             # 全局样式和设计变量
├── pages.json          # 路由 + Tab 配置
└── manifest.json       # uni-app 多端配置
```

## 底部 Tab

| Tab | 页面 | 说明 |
|-----|------|------|
| 首页 | `pages/index/index` | 净资产、收支概览、最近流水 |
| 记账 | `pages/add/add` | 支出/收入/转账快速录入 |
| 账户 | `pages/accounts/accounts` | 账户余额、账户明细 |
| 投资 | `pages/investments/investments` | 持仓汇总、持仓卡片 |
| 我的 | `pages/mine/mine` | 分类/预算/目标/报告入口、退出 |

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器（H5 模式）
npx uni --host 127.0.0.1 --port 5174
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
