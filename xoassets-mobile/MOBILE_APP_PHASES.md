# XOAssets Mobile V2 开发阶段约束

## 1. 总体规则

- 所有阶段必须按顺序推进。
- 每次只能执行用户指定阶段。
- 不允许越阶段实现。
- 不允许一次性实现所有业务。
- 不允许未确认就引入新依赖。
- 不允许修改与当前阶段无关的模块。
- UI 规范查看 `docs/MOBILE_UI_DESIGN_SYSTEM.md`。
- 图标规范查看 `docs/MOBILE_ICON_ASSETS_GUIDE.md`。
- 接口状态查看 `docs/MOBILE_API_INTEGRATION.md`。

## 2. 阶段总览

```text
阶段 0：基础约束与项目准备
阶段 1：App 壳子
阶段 2：登录与基础 API
阶段 3：首页资产驾驶舱
阶段 4：记账与流水录入
阶段 5：账户管理
阶段 6：投资管理
阶段 7：预算管理
阶段 8：AI 财务报告
阶段 9：设置与系统能力
阶段 10：附件与图片上传
阶段 11：图表与分析
阶段 12：性能优化与体验打磨
阶段 13：Android / iOS 上架准备
阶段 14：自动化与质量保障
```

## 阶段 0：基础约束与项目准备

### 目标

建立项目开发规则，防止 AI 乱改、乱删、越阶段实现。

### 任务

```text
扫描当前仓库结构
确认 Flutter 项目位置
确认是否已有 mobile 目录
确认是否存在后端 / Web / 文档目录
建立 MOBILE_APP_PHASES.md
建立 README.md
建立基础 .gitignore
确认 Flutter SDK 可用
```

### 禁止

```text
不要删除后端代码
不要删除 Web 代码
不要删除文档
不要移动旧项目目录
不要一次性生成完整业务
```

### 验收标准

```text
项目目录清晰
文档存在
AI 后续任务有约束依据
```

## 阶段 1：App 壳子

### 目标

搭建可运行的 Flutter App 基础壳子。

### 任务

```text
初始化 Flutter 项目
配置 Material 3
建立 XO Design System
建立基础路由
建立底部导航
建立中间悬浮 + 操作面板
建立 Splash 页面
建立 Login 页面骨架
建立 Home 页面骨架
建立 Ledger 页面骨架
建立 Investment 页面骨架
建立 Profile 页面骨架
建立 Settings 页面骨架
建立 TransactionEdit 页面骨架
建立 InvestmentTrade 页面骨架
建立 Budget 页面骨架
建立 Report 页面骨架
```

### 禁止

```text
不要接真实业务接口
不要引入 SQLite
不要引入 Drift
不要实现离线
不要直接连接 MySQL
不要引入大型 UI 库
不要实现复杂图表
```

### 验收标准

```text
App 可以启动
Splash 可以进入 Main
底部 Tab 可以切换
中间 + 可以弹出操作面板
基础页面可以跳转
全局主题生效
XO Design System 生效
Android 可以运行
iOS 在环境支持时可以运行
```

## 阶段 2：登录与基础 API

### 目标

建立真实登录能力和基础 API 通信能力。

### 任务

```text
完善 Login 页面
完善 Register 页面
接入登录接口
接入注册接口
保存 accessToken
保存 refreshToken，若后端支持
请求自动带 Token
401 自动处理
退出登录
登录态恢复
基础错误提示
基础 Loading 状态
```

### 推荐接口

```text
POST /api/auth/login
POST /api/auth/register
POST /api/auth/logout
POST /api/auth/refresh-token
GET  /api/auth/me
```

### 当前完成情况

```text
已接入 POST /api/auth/login
已接入 POST /api/auth/register
已接入 GET /api/auth/me
accessToken 已保存到 Secure Storage
请求已自动附加 Authorization: Bearer <token>
401 / 40100 已统一清理本地 token
退出登录当前按后端能力只清除本地 token
登录失败、空值校验和网络 / CORS 错误已使用弹窗提示
注册成功后复用登录接口建立会话
```

### 验收标准

```text
可以登录
可以注册
可以退出
重启 App 后可恢复登录态
Token 可自动添加到请求头
401 有统一处理
```

## 阶段 3：首页资产驾驶舱

### 目标

实现首页核心资产总览。

### 页面模块

```text
顶部问候
总资产卡
收支快览
账户资产摘要
投资摘要
AI 今日总结
最近流水
```

### 推荐接口

```text
GET /api/app/home/overview
GET /api/snapshots/latest
GET /api/dashboard/overview
GET /api/budgets/summary
GET /api/reports
GET /api/transactions
```

### UI 规则

```text
首页不要堆大量图表
总资产必须最突出
投资收益不要统一叫今日收益
金额必须支持隐藏
最近流水最多展示 5 条
```

### 验收标准

```text
首页接口可请求
首页数据可展示
加载态正常
错误态正常
金额隐藏正常
```

## 阶段 4：记账与流水录入

### 目标

实现日历记账和流水新增 / 编辑 / 删除。

### 页面范围

```text
记账日历
当天流水
流水录入
流水编辑
流水删除
```

### 禁止

```text
不要做本地离线记账
不要把流水存在本地数据库
不要引入 SQLite
不要引入 Drift
不要把分类统计放在主记账页
```

### 验收标准

```text
可以查看日历
可以切换日期
可以查看当天流水
可以新增流水
可以编辑流水
可以删除流水
基础表单校验正常
```

## 阶段 5：账户管理

### 目标

实现账户资产管理。

### 页面范围

```text
账户列表
账户详情
新增账户
编辑账户
账户余额调整
账户流水
```

### 推荐接口

```text
GET    /api/app/accounts
GET    /api/app/accounts/{id}
POST   /api/app/accounts
PUT    /api/app/accounts/{id}
DELETE /api/app/accounts/{id}
```

### UI 规则

```text
账户页只展示账户相关内容
账户页不要混入投资内容
信用卡负债要和资产区分
账户余额调整需要明确原因
```

### 验收标准

```text
可以查看账户列表
可以新增账户
可以编辑账户
可以查看账户详情
可以查看账户相关流水
```

## 阶段 6：投资管理

### 目标

实现投资资产管理。

### 页面范围

```text
投资总览
基金持仓
股票持仓
虚拟货币持仓
投资交易
持仓详情
收益明细
```

### 收益口径

```text
基金：昨日收益
股票：今日收益
虚拟货币：24h 收益
总投资：分项展示，不强行合并为今日收益
```

### 推荐接口

```text
GET  /api/app/investments/overview
GET  /api/app/investments/holdings?type=FUND
GET  /api/app/investments/holdings/{id}
POST /api/app/investments/trades
GET  /api/app/investments/trades
```

### 验收标准

```text
可以查看投资总览
可以按类型查看持仓
可以新增投资交易
可以查看持仓详情
收益口径展示正确
```

## 阶段 7：预算管理

### 目标

实现月度预算和分类预算。

### 页面范围

```text
本月预算总览
分类预算列表
新增预算
编辑预算
预算预警
预算详情
```

### 推荐接口

```text
GET  /api/app/budgets/current
POST /api/app/budgets
PUT  /api/app/budgets/{id}
GET  /api/app/budgets/{id}
```

### 验收标准

```text
可以查看本月预算
可以查看分类预算
可以新增预算
可以编辑预算
可以看到超支提醒
```

## 阶段 8：AI 财务报告

### 目标

实现 AI 财务分析报告。

### 页面范围

```text
今日报告
月度报告
异常消费
投资变化
预算风险
可执行建议
```

### 推荐接口

```text
GET  /api/app/reports/daily
GET  /api/app/reports/monthly
POST /api/app/reports/generate
```

### 禁止

```text
不要做成 AI 聊天首页
不要让 AI 功能影响记账主流程
不要生成过长报告
不要提供投资买卖建议
```

### 验收标准

```text
可以查看今日报告
可以查看月度报告
可以生成报告
报告加载态正常
报告错误态正常
```

## 阶段 9：设置与系统能力

### 目标

完善 App 设置、主题、权限、版本信息等基础能力。

### 页面范围

```text
设置页
主题设置
金额隐藏
深色模式
缓存清理
权限说明
版本信息
退出登录
```

### 验收标准

```text
金额隐藏生效
退出登录正常
设置项展示正常
版本信息展示正常
```

## 阶段 10：附件与图片上传

### 目标

支持流水附件图片能力。

### 功能

```text
拍照
相册选择
图片预览
图片删除
图片上传
上传失败提示
```

### 推荐接口

```text
POST /api/app/files/upload
DELETE /api/app/files/{id}
```

### 禁止

```text
不要把云存储密钥写进 App
不要在本地长期保存附件业务数据
不要做离线上传队列
```

## 阶段 11：图表与分析

### 目标

增加轻量数据分析能力。

### 页面范围

```text
收支趋势
分类支出
资产趋势
投资收益趋势
预算执行趋势
```

### 规则

```text
图表要克制
手机端不要堆太多图表
首页最多一个趋势小图
复杂分析放二级页面
```

## 阶段 12：性能优化与体验打磨

### 目标

提升移动端体验。

### 优化方向

```text
启动速度
页面切换
列表性能
图片缓存
接口错误处理
骨架屏
空状态
弱网体验
表单体验
```

## 阶段 13：Android / iOS 上架准备

### 目标

完成应用上架前准备。

Android：

```text
应用图标
启动页
应用名称
包名
权限说明
签名配置
AAB 打包
隐私政策
```

iOS：

```text
应用图标
启动页
Bundle ID
权限说明
证书配置
Provisioning Profile
IPA 打包
隐私政策
```

禁止：

```text
不要申请无关权限
不要在权限说明里写模糊文案
不要把测试环境地址打进生产包
```

## 阶段 14：自动化与质量保障

### 目标

建立基本工程质量保障。

### 内容

```text
flutter analyze
flutter test
代码格式化
构建检查
环境配置检查
CI 预留
```

### 推荐命令

```bash
flutter pub get
flutter analyze
flutter test
flutter build apk
```

### 验收标准

```text
flutter analyze 无严重错误
flutter test 可运行
Android 构建通过
README 更新完整
```
