# AGENTS.md

# XOAssets Mobile V2 协作规范

## 1. 项目定位

本目录是 XOAssets / 小〇财迹 新版移动端 App 项目。

新版移动端项目定位为：

> 个人资产驾驶舱 + 记账 + 账户管理 + 投资管理 + 预算管理 + AI 财务分析

目标平台：

* Android
* iOS

本项目从零开始设计，不复用旧 uni-app 移动端代码，不复用 Web 页面代码。

---

## 2. 当前权威文档

本目录下的权威文档优先级如下：

1. `AGENTS.md`
2. `MOBILE_APP_PHASES.md`
3. 后端接口文档 / Swagger / Knife4j
4. 产品需求文档
5. 代码实现

当文档冲突时，优先级高的文档生效。

如果 `AGENTS.md` 与 `MOBILE_APP_PHASES.md` 冲突：

* 先说明冲突点
* 不要擅自修改大范围代码
* 采用最小必要修改
* 高风险场景先询问用户

---

## 3. 技术栈约束

### 3.1 必须使用

| 类型         | 技术                     |
| ---------- | ---------------------- |
| App 框架     | Flutter                |
| 语言         | Dart                   |
| UI 基础      | Flutter Material 3     |
| UI 体系      | 自研 XO Design System    |
| 状态管理       | Riverpod               |
| 路由         | go_router              |
| 网络请求       | Dio                    |
| Token 安全存储 | flutter_secure_storage |
| 普通配置存储     | shared_preferences     |
| 后端访问       | HTTP API               |
| 后端数据库      | MySQL                  |

---

### 3.2 禁止使用

第一版禁止引入以下技术：

| 禁止项          | 原因                                     |
| ------------ | -------------------------------------- |
| SQLite       | 第一版不做离线                                |
| Drift        | 第一版不做离线                                |
| Hive         | 第一版不做本地数据库                             |
| Isar         | 第一版不做本地数据库                             |
| 大型 UI 组件库    | UI 使用 Material 3 + 自研 XO Design System |
| WebView 套壳   | 本项目是 Flutter App                       |
| App 直连 MySQL | App 只能通过后端 API 访问数据                    |
| 未批准的重型依赖     | 避免项目膨胀                                 |
| 一次性实现所有业务    | 必须按阶段推进                                |

---

## 4. 数据访问规则

### 4.1 正确架构

```text
Flutter App
  ↓ HTTP API
Spring Boot 后端
  ↓
MySQL
```

### 4.2 重要约束

1. Flutter App 不允许直接连接 MySQL。
2. MySQL 只存在于后端服务中。
3. App 只能通过 HTTP API 访问业务数据。
4. 第一版不做离线能力。
5. 第一版不引入本地数据库。
6. 第一版不做本地业务数据长期缓存。
7. 后端返回的数据是业务数据唯一可信来源。

---

## 5. 本地存储规则

### 5.1 Secure Storage

只能用于保存敏感信息：

```text
accessToken
refreshToken
```

### 5.2 SharedPreferences

只能用于保存普通配置：

```text
hideAmount
darkMode
lastSelectedAccountId
lastSelectedCategoryId
```

### 5.3 禁止本地长期保存

第一版不允许在本地长期保存完整业务数据：

```text
流水明细
账户资产
投资持仓
预算明细
AI 报告
历史收益
资产快照
```

这些数据必须从后端接口获取。

---

## 6. UI 设计规则

### 6.1 设计方向

整体风格：

```text
温和金融
卡片化
清晰
干净
克制
移动端优先
安全感
```

### 6.2 禁止风格

```text
交易所风格
博彩风格
过度科技感
过重玻璃拟态
颜色过多
图表堆叠
Web 页面缩小版
后台管理系统风格
```

### 6.3 UI 实现方式

UI 必须使用：

```text
Flutter Material 3
+ 自研 XO Design System
```

禁止直接套用大型 UI 组件库。

---

## 7. XO Design System

所有页面必须使用 XO Design System，不允许页面内到处写死颜色、圆角、字号、阴影。

### 7.1 颜色

推荐定义在：

```text
lib/core/design/xo_colors.dart
```

```dart
class XoColors {
  static const primary = Color(0xFF1F7A5B);
  static const primaryLight = Color(0xFFE8F5EF);

  static const income = Color(0xFF1F7A5B);
  static const expense = Color(0xFFD9534F);
  static const warning = Color(0xFFF2A93B);
  static const info = Color(0xFF3B82F6);

  static const pageBg = Color(0xFFF6F7F4);
  static const cardBg = Color(0xFFFFFFFF);

  static const textMain = Color(0xFF1F2933);
  static const textSecondary = Color(0xFF6B7280);
  static const textPlaceholder = Color(0xFFA0A7B1);

  static const border = Color(0xFFE5E7EB);
}
```

### 7.2 圆角

推荐定义在：

```text
lib/core/design/xo_radius.dart
```

```dart
class XoRadius {
  static const sm = 8.0;
  static const md = 12.0;
  static const lg = 18.0;
  static const xl = 24.0;
  static const card = 24.0;
}
```

### 7.3 间距

推荐定义在：

```text
lib/core/design/xo_spacing.dart
```

```dart
class XoSpacing {
  static const xs = 4.0;
  static const sm = 8.0;
  static const md = 16.0;
  static const lg = 24.0;
  static const xl = 32.0;
}
```

### 7.4 字体

推荐定义在：

```text
lib/core/design/xo_text_styles.dart
```

```dart
class XoTextStyles {
  static const titleLarge = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.w700,
  );

  static const titleMedium = TextStyle(
    fontSize: 18,
    fontWeight: FontWeight.w600,
  );

  static const body = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w400,
  );

  static const caption = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w400,
  );

  static const moneyLarge = TextStyle(
    fontSize: 32,
    fontWeight: FontWeight.w700,
  );

  static const moneyMedium = TextStyle(
    fontSize: 20,
    fontWeight: FontWeight.w600,
  );
}
```

---

## 8. 组件规则

### 8.1 基础组件

必须优先封装并复用：

```text
XoPage
XoCard
XoMoneyText
XoEmpty
XoLoading
XoErrorView
XoSectionHeader
XoBottomSheet
```

### 8.2 业务组件

推荐封装：

```text
XoAssetOverviewCard
XoQuickStatCard
XoTransactionItem
XoInvestmentItem
XoBudgetProgressCard
XoAiSummaryCard
XoAccountCard
```

### 8.3 组件使用要求

1. 页面容器必须优先使用 `XoPage`。
2. 卡片必须优先使用 `XoCard`。
3. 金额必须使用 `XoMoneyText`。
4. 空状态必须使用 `XoEmpty`。
5. 加载状态必须使用 `XoLoading`。
6. 错误状态必须使用 `XoErrorView`。
7. 不允许每个页面单独写一套卡片样式。
8. 不允许金额格式化逻辑散落在页面中。
9. 不允许颜色、圆角、间距在页面内随意硬编码。

---

## 9. 推荐目录结构

```text
lib
├── main.dart
│
├── app
│   ├── app.dart
│   ├── router.dart
│   ├── routes.dart
│   └── theme.dart
│
├── core
│   ├── constants
│   │   ├── app_constants.dart
│   │   └── api_constants.dart
│   │
│   ├── design
│   │   ├── xo_colors.dart
│   │   ├── xo_radius.dart
│   │   ├── xo_spacing.dart
│   │   ├── xo_text_styles.dart
│   │   └── xo_theme.dart
│   │
│   ├── errors
│   │   ├── app_exception.dart
│   │   └── error_handler.dart
│   │
│   ├── network
│   │   ├── api_client.dart
│   │   ├── api_response.dart
│   │   ├── dio_provider.dart
│   │   └── interceptors
│   │       ├── auth_interceptor.dart
│   │       └── log_interceptor.dart
│   │
│   ├── storage
│   │   ├── secure_storage_service.dart
│   │   └── preferences_service.dart
│   │
│   ├── utils
│   │   ├── money_utils.dart
│   │   ├── date_utils.dart
│   │   └── number_utils.dart
│   │
│   └── widgets
│       ├── xo_page.dart
│       ├── xo_card.dart
│       ├── xo_money_text.dart
│       ├── xo_empty.dart
│       ├── xo_loading.dart
│       ├── xo_error_view.dart
│       ├── xo_section_header.dart
│       └── xo_bottom_sheet.dart
│
├── features
│   ├── splash
│   ├── auth
│   ├── main
│   ├── home
│   ├── ledger
│   ├── transaction
│   ├── account
│   ├── investment
│   ├── budget
│   ├── report
│   ├── profile
│   └── settings
│
└── shared
    ├── models
    └── enums
```

---

## 10. 路由规则

必须使用 `go_router`。

基础路由：

```text
/splash
/login
/main
/transaction/edit
/investment/trade
/budget
/report
/settings
```

推荐定义：

```dart
class AppRoutes {
  static const splash = '/splash';
  static const login = '/login';
  static const main = '/main';
  static const transactionEdit = '/transaction/edit';
  static const investmentTrade = '/investment/trade';
  static const budget = '/budget';
  static const report = '/report';
  static const settings = '/settings';
}
```

---

## 11. 底部导航规则

底部导航固定为：

```text
首页
记账
中间悬浮 +
投资
我的
```

中间 `+` 点击后展示底部操作面板：

```text
记一笔
转账
投资交易
新增账户
新增预算
```

---

## 12. 状态管理规则

必须使用 Riverpod。

至少包含：

```text
authProvider
appSettingsProvider
mainTabProvider
```

### 12.1 authProvider

职责：

```text
登录状态
Token 状态
Mock 登录
退出登录
```

### 12.2 appSettingsProvider

职责：

```text
是否隐藏金额
是否深色模式
主题配置预留
```

### 12.3 mainTabProvider

职责：

```text
当前底部 Tab index
```

---

## 13. 网络层规则

必须使用 Dio。

需要包含：

```text
ApiClient
DioProvider
AuthInterceptor
LogInterceptor
ApiResponse
AppException
ErrorHandler
```

### 13.1 网络请求要求

1. 统一配置 `baseUrl`。
2. 统一配置超时时间。
3. 请求自动添加 Token。
4. 统一处理 401。
5. 统一处理业务异常。
6. 页面不允许直接调用 Dio。
7. 页面必须通过 Repository / ApiClient 间接调用接口。
8. 后端 `message` 可用于前端错误提示，但不能直接暴露敏感信息。

### 13.2 默认 API 地址

开发环境默认：

```text
http://localhost:8080/api
```

后续必须支持环境切换：

```text
dev
test
prod
```

---

## 14. 页面规则

### 14.1 Splash 页面

职责：

```text
展示 App 名称 / Logo
检查登录态
跳转 Login 或 Main
```

第一阶段允许 mock 跳转。

---

### 14.2 Login 页面

职责：

```text
登录表单
登录按钮
登录状态
错误提示
```

第一阶段允许 mock 登录。

---

### 14.3 首页

首页是资产驾驶舱。

必须包含：

```text
顶部问候
总资产卡
收支快览
账户资产摘要
投资摘要
AI 今日总结
最近流水
```

首页规则：

```text
总资产必须最突出
金额必须支持隐藏
首页不要堆大量图表
最近流水最多展示 5 条
投资收益不要统一叫今日收益
```

---

### 14.4 记账页

记账页使用日历视图。

必须包含：

```text
年月切换
日历视图
当天收入
当天支出
当天结余
当天流水列表
记一笔入口
```

禁止：

```text
不要在记账主页面放本月支出分类占比
不要把分类统计塞进主记账页
```

---

### 14.5 流水录入页

必须包含：

```text
收入 / 支出 / 转账切换
金额输入
分类选择
账户选择
时间选择
备注
图片附件入口
保存按钮
```

规则：

```text
金额必须大于 0
账户必选
分类必选
时间默认当前时间
不做标签功能
```

---

### 14.6 账户页

账户页只展示账户相关内容。

账户类型：

```text
现金
银行卡
支付宝
微信
信用卡
其他
```

禁止：

```text
不要把投资资产混入账户页
不要把账户和投资混成一个列表
```

---

### 14.7 投资页

投资类型：

```text
基金
股票
虚拟货币
其他
```

收益口径：

```text
基金：昨日收益
股票：今日收益
虚拟货币：24h 收益
总投资：分项展示，不强行合并为今日收益
```

禁止：

```text
不要把基金收益写成今日收益
不要把所有投资统一按一个口径展示
不要在 App 端计算最终资产收益
最终收益以后端返回字段为准
```

---

### 14.8 预算页

必须包含：

```text
本月预算总览
分类预算列表
预算预警
预算编辑入口
```

规则：

```text
正常状态用绿色
接近超支用橙色
已超支用红色
```

---

### 14.9 AI 报告页

AI 报告页不是聊天页。

必须包含：

```text
今日报告
月度报告
异常消费
投资变化
预算风险
可执行建议
```

规则：

```text
报告内容要短
建议必须可执行
不要写空话
不要提供投资买卖建议
```

---

### 14.10 我的页

必须包含：

```text
用户信息
账户管理
分类管理
预算管理
AI 报告
设置
关于 XOAssets
```

---

### 14.11 设置页

必须包含：

```text
金额隐藏
深色模式预留
清理缓存
退出登录
版本信息
```

危险操作需要二次确认。

---

## 15. 业务口径规则

### 15.1 资产口径

```text
总资产 = 现金类账户余额 + 投资资产市值
总负债 = 信用卡负债 + 借款 + 贷款
净资产 = 总资产 - 总负债
```

### 15.2 收支口径

```text
支出影响账户余额
收入影响账户余额
转账不计入收入支出
转账只影响账户余额
```

### 15.3 投资口径

```text
投资买入不写入普通流水
投资卖出不写入普通流水
投资交易影响资金账户
投资收益以后端计算为准
前端只展示后端返回字段
```

### 15.4 金额口径

```text
金额展示必须统一格式化
业务 ID 按字符串处理
不要用 double 做金额最终计算
App 端只做展示和表单输入
最终金额计算以后端为准
```

---

## 16. 阶段开发规则

所有阶段以 `MOBILE_APP_PHASES.md` 为准。

当前阶段必须由用户明确指定。

Codex 每次只能执行一个阶段。

禁止：

```text
越阶段实现
一次性实现所有业务
引入未批准依赖
重写无关模块
删除无关文件
```

---

## 17. Codex 执行规则

### 17.1 执行前

每次改代码前必须先输出：

```text
当前目录结构分析
准备新增的文件
准备修改的文件
本次执行计划
风险点
需要用户确认的问题
```

未经用户确认，不要直接大范围修改代码。

---

### 17.2 执行中

必须遵守：

```text
只做当前阶段任务
不要越阶段实现
不要引入未批准依赖
不要删除无关文件
不要修改后端代码
不要修改 Web 端代码
不要生成大量无关代码
不要做顺手重构
```

---

### 17.3 执行后

必须输出：

```text
完成内容
新增文件
修改文件
新增依赖
运行命令
验证结果
遗留问题
下一步建议
```

---

## 18. 代码风格规则

1. 页面只负责 UI 组装。
2. 业务状态放到 Provider。
3. 网络请求放到 `core/network` 或对应 feature 的 data 层。
4. 通用 UI 放到 `core/widgets`。
5. 主题样式放到 `core/design`。
6. 常量不要散落在页面里。
7. 不要把所有代码写在一个文件。
8. 不要做无关格式化。
9. 不要改动与任务无关的文件。
10. 类、组件、复杂方法需要有必要注释。
11. 注释要说明业务原因或非显然逻辑，避免空话。
12. 不确定且高风险时先询问用户。
13. 低风险场景可做合理假设并说明。

---

## 19. 错误处理规则

必须处理以下场景：

```text
金额为空
金额小于等于 0
账户未选择
分类未选择
登录过期
网络错误
后端业务异常
空数据
加载中
请求失败重试
```

错误展示要求：

```text
提示简洁
不要暴露后端敏感信息
不要暴露第三方接口原始错误
关键操作失败要保留重试入口
```

---

## 20. 安全规则

1. Token 必须保存到 Secure Storage。
2. Token 不允许保存到 SharedPreferences。
3. 请求使用 `Authorization: Bearer <token>`。
4. 日志中不要打印 Token。
5. 日志中不要打印密码。
6. 日志中不要打印完整敏感财务数据。
7. 不要在 App 中写死生产环境密钥。
8. 不要在 App 中写死对象存储密钥。
9. 不要在 App 中写死 MySQL 地址、账号、密码。
10. 权限申请必须有明确用途。

---

## 21. 构建与验证规则

### 21.1 常用命令

```bash
flutter pub get
flutter analyze
flutter test
flutter run
flutter build apk
```

### 21.2 验证要求

每次完成代码修改后，至少说明以下结果：

```text
flutter pub get 是否成功
flutter analyze 是否通过
flutter test 是否运行
Android 是否可运行
iOS 是否可运行，若当前环境支持
```

如果无法验证，必须说明原因和剩余风险。

---

## 22. Git 规则

未经用户明确要求，不执行：

```bash
git add
git commit
git push
git branch
git checkout -b
```

写完代码后只汇报：

```text
改动内容
验证结果
剩余风险
是否建议提交
```

用户要求提交时，默认使用中文提交描述。

推荐提交格式：

```text
type(scope): summary
```

示例：

```text
feat(mobile): 初始化 Flutter App 壳子
```

---

## 23. 依赖管理规则

新增依赖必须说明：

```text
依赖名称
用途
为什么需要
是否有替代方案
是否影响包体积
```

第一阶段允许依赖：

```text
flutter_riverpod
go_router
dio
flutter_secure_storage
shared_preferences
intl
```

第一阶段禁止依赖：

```text
drift
sqlite3_flutter_libs
hive
isar
大型 UI 组件库
```

---

## 24. 环境配置规则

必须支持环境区分：

```text
dev
test
prod
```

第一阶段可以先使用常量配置，但需要预留环境切换能力。

默认开发环境 API：

```text
http://localhost:8080/api
```

注意：

Android 模拟器访问宿主机本地服务时，可能需要使用：

```text
http://10.0.2.2:8080/api
```

iOS 模拟器通常可以使用：

```text
http://localhost:8080/api
```

具体以后端运行环境为准。

---

## 25. Android / iOS 规则

### 25.1 Android

需要关注：

```text
包名
应用图标
启动页
签名配置
权限说明
AAB 打包
```

默认包名：

```text
com.xoassets.app
```

### 25.2 iOS

需要关注：

```text
Bundle ID
应用图标
启动页
证书
权限说明
IPA 打包
```

默认 Bundle ID：

```text
com.xoassets.app
```

---

## 26. 第一版不做内容

第一版明确不做：

```text
离线记账
离线同步
本地数据库
银行卡自动同步
支付宝自动同步
微信自动同步
券商自动同步
投资买卖建议
社交功能
复杂后台管理
真实 AI 大模型调用
```

AI 报告第一版可以先做模板化财务复盘。

---

## 27. 与后端接口协作规则

1. 接口路径以后端文档为准。
2. App 端只调用后端 API。
3. 第三方行情只能由后端调用。
4. 第三方 AI 只能由后端调用。
5. App 不直接调用第三方行情源。
6. App 不直接调用第三方汇率源。
7. App 不直接访问对象存储密钥。
8. 后端 Long ID 在 App 中按字符串处理。
9. 金额字段建议按字符串接收，避免精度问题。
10. App 端展示金额时统一格式化。

---

## 28. 页面开发优先级

开发顺序以 `MOBILE_APP_PHASES.md` 为准，默认如下：

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

---

## 29. Codex 通用任务模板

后续每次给 Codex 执行任务时，建议使用：

```md
请根据当前目录下的 `AGENTS.md` 和 `MOBILE_APP_PHASES.md` 执行任务。

当前阶段：

阶段 X：{阶段名称}

要求：

1. 严格遵守 `AGENTS.md`。
2. 严格遵守 `MOBILE_APP_PHASES.md`。
3. 只做当前阶段内容，不要越阶段实现。
4. 不要引入 SQLite / Drift / Hive / Isar。
5. 不要引入大型 UI 组件库。
6. 不要直接连接 MySQL。
7. 不要修改后端代码。
8. 不要修改 Web 端代码。
9. 不要删除无关文件。
10. 修改前先扫描项目结构。
11. 先输出执行计划，等待确认后再改代码。

请先输出：

- 当前项目结构分析
- 准备新增的文件
- 准备修改的文件
- 本阶段实现计划
- 风险点
```

---

## 30. 长期原则

1. 移动端优先。
2. 后端 MySQL 是主数据源。
3. App 不直接连接数据库。
4. 第一版不做离线。
5. UI 使用 Material 3 + XO Design System。
6. 页面不要写死样式。
7. 金额展示必须统一。
8. 投资收益口径必须分类型展示。
9. 基金不要写今日收益。
10. 首页不要堆复杂图表。
11. 记账页使用日历视图。
12. AI 报告不是聊天首页。
13. Codex 每次只做一个阶段。
14. 每次改动前必须输出计划。
15. 每次改动后必须输出验证结果。
16. 不确定的高风险问题先问用户。
17. 低风险问题做合理假设并说明。
