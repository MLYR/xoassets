# XOAssets Mobile V2 开发阶段约束文档

## 1. 项目定位

XOAssets Mobile V2 是 XOAssets / 小〇财迹 的新版移动端 App。

本项目定位为：

> 个人资产驾驶舱 + 记账 + 账户管理 + 投资管理 + 预算管理 + AI 财务分析

新版移动端从零开始设计，不依赖旧前端代码。

目标平台：

- Android
- iOS

当前阶段不考虑 Web 端复用，不考虑小程序，不考虑 uni-app。

------

## 2. 技术栈约束

### 2.1 App 技术栈

必须使用：

| 类型           | 技术                   |
| -------------- | ---------------------- |
| App 框架       | Flutter                |
| 语言           | Dart                   |
| UI 基础        | Flutter Material 3     |
| UI 体系        | 自研 XO Design System  |
| 状态管理       | Riverpod               |
| 路由           | go_router              |
| 网络请求       | Dio                    |
| Token 安全存储 | flutter_secure_storage |
| 普通配置存储   | shared_preferences     |
| 后端访问       | HTTP API               |
| 后端数据库     | MySQL                  |

------

### 2.2 明确禁止

Codex 不允许引入以下内容：

| 禁止项             | 原因                                       |
| ------------------ | ------------------------------------------ |
| SQLite             | 第一阶段不做离线                           |
| Drift              | 第一阶段不做离线                           |
| Hive               | 第一阶段不做本地数据库                     |
| Isar               | 第一阶段不做本地数据库                     |
| 大型 UI 组件库     | UI 使用 Material 3 + 自研 XO Design System |
| WebView 套壳       | 本项目是 Flutter App，不是 H5 包壳         |
| App 直连 MySQL     | App 只能通过后端 API 访问数据              |
| 过度封装框架       | 避免项目复杂化                             |
| 一次性实现所有业务 | 必须按阶段推进                             |

------

## 3. 数据库约束

### 3.1 正确架构

```text
Flutter App
  ↓ HTTP API
Spring Boot 后端
  ↓
MySQL
```

### 3.2 重要规则

1. MySQL 是后端主数据库。
2. Flutter App 不允许直接连接 MySQL。
3. App 第一版不做离线能力。
4. App 第一版不引入本地数据库。
5. App 只保存必要的本地配置和登录态。

------

## 4. 本地存储约束

### 4.1 Secure Storage

用于保存敏感信息：

```text
accessToken
refreshToken
```

### 4.2 SharedPreferences

用于保存普通配置：

```text
hideAmount
darkMode
lastSelectedAccountId
lastSelectedCategoryId
```

### 4.3 不允许保存

第一版不允许在本地长期保存完整业务数据：

```text
流水明细
账户资产
投资持仓
预算明细
AI 报告
```

这些数据必须从后端接口获取。

## 5. App 端 UI 设计约束

### 5.1 整体视觉定位

XOAssets Mobile V2 的 App 端 UI 风格统一定义为：

```text
科技金融
简约高级
安全可信
数据智能
清晰克制
移动端优先
```

整体视觉参考登录 / 注册页生成图的风格：

```text
深青绿色金融背景
白色圆角内容卡片
科技感数据波纹
轻量金融插画
克制的玻璃质感
大圆角输入框
深青绿色渐变主按钮
线性功能图标
干净留白
强层级金额展示
```

UI 目标：

- 看起来像专业金融 App，而不是普通后台系统。
- 有科技感，但不能过度炫酷。
- 有金融感，但不能像交易所或炒币 App。
- 有高级感，但不能牺牲可读性。
- 页面信息要清晰，不堆满图表和装饰。

------

### 5.2 视觉关键词

必须遵守：

```text
深青绿
白色卡片
轻阴影
大圆角
线性图标
数据波纹
金融图表点缀
柔和渐变
克制科技感
清晰表单
```

禁止出现：

```text
交易所黑金风
博彩风
荧光赛博风
重度玻璃拟态
重度霓虹效果
杂乱渐变
多套图标混用
Web 页面缩小版
后台管理系统风格
过度堆叠图表
```

------

### 5.3 页面背景规范

App 端默认页面背景使用浅色金融背景：

```text
#F4FAF8
```

适用于：

```text
首页
记账页
投资页
账户页
预算页
我的页
设置页
普通表单页
```

登录、注册、启动页允许使用深青绿色科技金融背景：

```text
#002F2A
#003D36
#004B43
```

登录 / 注册页背景可以包含：

```text
数据波纹
金融城市剪影
点阵球体
趋势线
柱状图
柔和光点
```

但装饰必须弱化，不能干扰输入框和按钮。

------

### 5.4 卡片规范

所有主要内容必须卡片化展示。

卡片风格：

```text
白色背景
大圆角
轻阴影
弱边框
宽松内边距
```

推荐：

```dart
class XoCardTokens {
  static const background = Color(0xFFFFFFFF);
  static const border = Color(0xFFE3EBE8);
  static const radius = 24.0;
  static const padding = 20.0;
}
```

卡片阴影：

```dart
static const cardShadow = BoxShadow(
  color: Color(0x14002F2A),
  blurRadius: 24,
  offset: Offset(0, 10),
);
```

禁止：

```text
强烈黑色阴影
硬边框卡片
小圆角后台风
颜色过多的卡片
同屏过多层级嵌套卡片
```

------

### 5.5 表单规范

登录、注册、记账、交易录入等表单统一使用大圆角输入框。

输入框风格：

```text
高度 56
圆角 16
白色或极浅色背景
浅灰绿色边框
左侧线性图标
右侧功能图标可选
聚焦时使用主色边框
```

推荐：

```dart
class XoInputTokens {
  static const height = 56.0;
  static const radius = 16.0;
  static const background = Color(0xFFFFFFFF);
  static const border = Color(0xFFDCE7E3);
  static const focusedBorder = Color(0xFF007C6E);
}
```

输入框禁止：

```text
过小高度
直角输入框
高饱和边框
多个颜色混用
表单项间距过窄
```

------

### 5.6 按钮规范

主按钮统一使用深青绿色渐变。

主按钮风格：

```text
深青绿渐变
白色文字
大圆角
轻阴影
高度 56
字体加粗
```

推荐渐变：

```dart
static const primaryGradient = LinearGradient(
  begin: Alignment.topLeft,
  end: Alignment.bottomRight,
  colors: [
    Color(0xFF008071),
    Color(0xFF004C43),
  ],
);
```

主按钮文字：

```text
16px
FontWeight.w700
白色
```

按钮高度：

```text
普通按钮：48
主操作按钮：56
底部固定按钮：56
```

按钮圆角：

```text
16 - 20
```

禁止：

```text
大面积红色按钮
高饱和蓝紫渐变
霓虹按钮
多种主按钮样式并存
```

------

### 5.7 登录 / 注册页视觉基线

登录和注册页必须作为 App 的视觉基准。

统一结构：

```text
顶部状态栏
品牌 Logo + XOAssets + 小〇财迹
科技金融插画区域
标题文案
副标题说明
白色表单卡片
主按钮
辅助链接
底部安全 / 智能 / 隐私类卖点
```

登录页标题示例：

```text
欢迎回来
登录您的账户，开启智能资产管理之旅
```

注册页标题示例：

```text
创建账户
注册新账户，开启您的资产管理之旅
```

登录 / 注册页允许出现的视觉元素：

```text
城市金融剪影
数据波纹
点阵地球
趋势线
柱状图
柔和光点
环形轨道
```

禁止：

```text
人物插画过大
卡通风
复杂 3D 模型
过重科技背景
背景压过表单
```

------

### 5.8 首页视觉规范

首页是资产驾驶舱。

视觉重点：

```text
总资产最突出
净资产清晰
收支快览清晰
投资摘要克制
AI 总结轻量
最近流水易读
```

首页卡片顺序建议：

```text
顶部问候
总资产卡
收支快览
账户摘要
投资摘要
AI 今日总结
最近流水
```

首页禁止：

```text
首屏堆多个图表
首屏堆过多按钮
投资收益和日常收支混在一起
基金收益写成今日收益
过度装饰背景
```

------

### 5.9 图表视觉规范

图表只作为辅助，不作为页面主体。

图表风格：

```text
线条细
颜色克制
坐标弱化
背景干净
标签简洁
默认不使用复杂 3D 图表
```

图表颜色：

```text
主趋势线：#007C6E
辅助趋势线：#63BFB4
警告线：#F2A93B
负向线：#D9534F
网格线：#E3EBE8
```

禁止：

```text
3D 饼图
高饱和面积图
过多图例
一个页面堆多个复杂图表
```

------

## 6. XO Design System

所有页面必须使用 XO Design System。

禁止在页面里直接写死：

```text
颜色
圆角
字号
阴影
间距
图标路径
金额格式
```

所有设计 token 必须集中维护在：

```text
lib/core/design/
```

推荐目录：

```text
lib/core/design
├── xo_colors.dart
├── xo_radius.dart
├── xo_spacing.dart
├── xo_text_styles.dart
├── xo_shadows.dart
├── xo_gradients.dart
├── xo_icons.dart
└── xo_theme.dart
```

------

### 6.1 颜色系统

```dart
class XoColors {
  XoColors._();

  /// 品牌主色：深青绿，来自登录 / 注册页主视觉
  static const primary = Color(0xFF007C6E);

  /// 品牌深色：用于深色背景、Logo 背景、强调区域
  static const primaryDark = Color(0xFF003D36);

  /// 更深背景色：用于登录、注册、启动页科技金融背景
  static const deepTeal = Color(0xFF002F2A);

  /// 主色浅背景：用于图标底色、标签底色、轻提示背景
  static const primaryLight = Color(0xFFE8F5EF);

  /// 科技青色：用于图表节点、数据波纹、轻量高光
  static const techCyan = Color(0xFF2CCBC0);

  /// 金融金色：少量用于高光、重要提示，不可大面积使用
  static const financeGold = Color(0xFFE9C46A);

  /// 页面背景
  static const pageBg = Color(0xFFF4FAF8);

  /// 卡片背景
  static const cardBg = Color(0xFFFFFFFF);

  /// 输入框背景
  static const inputBg = Color(0xFFFFFFFF);

  /// 主文字
  static const textMain = Color(0xFF1F2933);

  /// 次级文字
  static const textSecondary = Color(0xFF6B7280);

  /// 占位文字
  static const textPlaceholder = Color(0xFFA0A7B1);

  /// 边框
  static const border = Color(0xFFE3EBE8);

  /// 弱分割线
  static const divider = Color(0xFFEAF0EE);

  /// 收入
  static const income = Color(0xFF1F7A5B);

  /// 支出
  static const expense = Color(0xFFD9534F);

  /// 警告
  static const warning = Color(0xFFF2A93B);

  /// 信息
  static const info = Color(0xFF3B82F6);

  /// 成功
  static const success = Color(0xFF1F7A5B);
}
```

颜色使用规则：

```text
主色只能使用深青绿系
警告少量使用橙色
支出使用柔和红色
收入使用绿色
科技青色只作为点缀
金融金色只作为高光，不可大面积铺满
```

禁止：

```text
页面内随意出现高饱和紫色、荧光蓝、亮红、亮黄
同一业务含义使用多个颜色
```

------

### 6.2 渐变系统

```dart
class XoGradients {
  XoGradients._();

  static const primaryButton = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0xFF008071),
      Color(0xFF004C43),
    ],
  );

  static const authBackground = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [
      Color(0xFF002F2A),
      Color(0xFF004C43),
    ],
  );

  static const assetCard = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0xFF007C6E),
      Color(0xFF003D36),
    ],
  );

  static const lightCard = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0xFFFFFFFF),
      Color(0xFFF4FAF8),
    ],
  );
}
```

渐变规则：

```text
主按钮可以用深青绿渐变
总资产卡可以用深青绿渐变
登录注册背景可以用深青绿渐变
普通业务卡片默认白色，不要滥用渐变
```

------

### 6.3 圆角系统

```dart
class XoRadius {
  XoRadius._();

  static const xs = 6.0;
  static const sm = 8.0;
  static const md = 12.0;
  static const lg = 16.0;
  static const xl = 20.0;
  static const xxl = 24.0;

  /// 普通卡片
  static const card = 24.0;

  /// 输入框
  static const input = 16.0;

  /// 主按钮
  static const button = 18.0;

  /// 底部弹层
  static const bottomSheet = 28.0;
}
```

------

### 6.4 间距系统

```dart
class XoSpacing {
  XoSpacing._();

  static const xs = 4.0;
  static const sm = 8.0;
  static const md = 12.0;
  static const lg = 16.0;
  static const xl = 20.0;
  static const xxl = 24.0;
  static const xxxl = 32.0;

  /// 页面左右边距
  static const pageHorizontal = 20.0;

  /// 卡片内边距
  static const cardPadding = 20.0;

  /// 表单项间距
  static const formGap = 16.0;
}
```

------

### 6.5 阴影系统

```dart
class XoShadows {
  XoShadows._();

  static const card = BoxShadow(
    color: Color(0x14002F2A),
    blurRadius: 24,
    offset: Offset(0, 10),
  );

  static const button = BoxShadow(
    color: Color(0x33004C43),
    blurRadius: 18,
    offset: Offset(0, 8),
  );

  static const floating = BoxShadow(
    color: Color(0x26002F2A),
    blurRadius: 28,
    offset: Offset(0, 12),
  );
}
```

阴影规则：

```text
阴影必须轻
阴影颜色使用深青绿透明色
不要使用纯黑大阴影
不要让页面显得厚重
```

------

### 6.6 字体系统

```dart
class XoTextStyles {
  XoTextStyles._();

  static const display = TextStyle(
    fontSize: 32,
    fontWeight: FontWeight.w700,
    height: 1.2,
  );

  static const titleLarge = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.w700,
    height: 1.25,
  );

  static const titleMedium = TextStyle(
    fontSize: 20,
    fontWeight: FontWeight.w700,
    height: 1.3,
  );

  static const titleSmall = TextStyle(
    fontSize: 18,
    fontWeight: FontWeight.w600,
    height: 1.3,
  );

  static const bodyLarge = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w400,
    height: 1.5,
  );

  static const body = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w400,
    height: 1.45,
  );

  static const caption = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w400,
    height: 1.4,
  );

  static const moneyLarge = TextStyle(
    fontSize: 34,
    fontWeight: FontWeight.w800,
    height: 1.1,
  );

  static const moneyMedium = TextStyle(
    fontSize: 22,
    fontWeight: FontWeight.w700,
    height: 1.2,
  );

  static const button = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w700,
    height: 1.2,
  );
}
```

字体规则：

```text
金额数字必须更醒目
标题加粗但不能过重
正文保持清晰易读
表单 placeholder 使用浅灰
按钮文字统一加粗
```

------

## 7. App 图标与 UI 图标规范

### 7.1 图标类型

App 端图标分为三类：

```text
App 启动图标 / 桌面图标
启动页 Logo
App 内部功能图标
```

三类图标必须分开管理，不允许混用。

------

### 7.2 App 启动图标

App 启动图标用于 Android / iOS 桌面。

设计方向：

```text
深青绿色圆角方形
白色 XO 抽象符号
简约金融科技感
高识别度
不要复杂文字
不要过多细节
```

推荐规格：

```text
1024x1024 PNG
```

推荐文件：

```text
assets/images/app_icon.png
assets/images/app_icon_foreground.png
```

推荐生成工具：

```text
flutter_launcher_icons
```

配置示例：

```yaml
flutter_launcher_icons:
  android: true
  ios: true
  image_path: "assets/images/app_icon.png"
  adaptive_icon_background: "#003D36"
  adaptive_icon_foreground: "assets/images/app_icon_foreground.png"
```

禁止：

```text
App 图标里塞太多中文
App 图标使用复杂城市背景
App 图标使用过细线条
App 图标直接截图 UI
App 图标使用多种不统一颜色
```

------

### 7.3 启动页 Logo

启动页 Logo 用于 Splash。

设计方向：

```text
XOAssets Logo
小〇财迹
深青绿主色
白色或浅色背景
干净居中
```

推荐文件：

```text
assets/images/splash_logo.png
assets/icons/app/logo.svg
assets/icons/app/logo_mark.svg
```

启动页背景：

```text
浅色启动页：#F4FAF8
深色启动页：#003D36
```

推荐工具：

```text
flutter_native_splash
```

------

### 7.4 App 内部图标风格

App 内部所有功能图标统一使用 SVG。

图标风格：

```text
线性图标
24x24 画布
2px 线宽
圆角端点
圆角连接
默认单色
可通过代码染色
不使用复杂渐变
不使用拟物图标
不使用 Emoji
```

颜色规则：

```text
默认图标色：#6B7280
选中图标色：#007C6E
浅背景图标色：#007C6E
危险图标色：#D9534F
警告图标色：#F2A93B
```

------

### 7.5 图标目录结构

```text
assets
├── icons
│   ├── app
│   │   ├── logo.svg
│   │   └── logo_mark.svg
│   │
│   ├── tab
│   │   ├── home.svg
│   │   ├── ledger.svg
│   │   ├── investment.svg
│   │   └── profile.svg
│   │
│   ├── action
│   │   ├── add_transaction.svg
│   │   ├── transfer.svg
│   │   ├── investment_trade.svg
│   │   ├── add_account.svg
│   │   └── add_budget.svg
│   │
│   ├── category
│   │   ├── food.svg
│   │   ├── coffee.svg
│   │   ├── transport.svg
│   │   ├── shopping.svg
│   │   ├── housing.svg
│   │   ├── medical.svg
│   │   ├── education.svg
│   │   ├── entertainment.svg
│   │   └── salary.svg
│   │
│   ├── account
│   │   ├── cash.svg
│   │   ├── bank_card.svg
│   │   ├── credit_card.svg
│   │   ├── alipay.svg
│   │   └── wechat_pay.svg
│   │
│   └── investment
│       ├── fund.svg
│       ├── stock.svg
│       ├── crypto.svg
│       └── other.svg
│
└── images
    ├── app_icon.png
    └── splash_logo.png
```

------

### 7.6 图标命名规范

使用小写加下划线：

```text
home.svg
bank_card.svg
credit_card.svg
add_transaction.svg
investment_trade.svg
wechat_pay.svg
```

禁止：

```text
图标1.svg
icon-new-copy.svg
Group 123.svg
微信图标.svg
未命名.svg
```

------

### 7.7 图标代码约束

所有 SVG 图标必须通过 `XoIcon` 使用。

禁止页面直接调用：

```dart
SvgPicture.asset(...)
```

必须统一封装：

```dart
class XoIcon extends StatelessWidget {
  const XoIcon(
    this.assetName, {
    super.key,
    this.size = 24,
    this.color,
  });

  final String assetName;
  final double size;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return SvgPicture.asset(
      assetName,
      width: size,
      height: size,
      colorFilter: color == null
          ? null
          : ColorFilter.mode(color!, BlendMode.srcIn),
    );
  }
}
```

图标路径必须统一维护在 `XoIcons`：

```dart
class XoIcons {
  XoIcons._();

  static const home = 'assets/icons/tab/home.svg';
  static const ledger = 'assets/icons/tab/ledger.svg';
  static const investment = 'assets/icons/tab/investment.svg';
  static const profile = 'assets/icons/tab/profile.svg';

  static const addTransaction = 'assets/icons/action/add_transaction.svg';
  static const transfer = 'assets/icons/action/transfer.svg';
  static const investmentTrade = 'assets/icons/action/investment_trade.svg';
  static const addAccount = 'assets/icons/action/add_account.svg';
  static const addBudget = 'assets/icons/action/add_budget.svg';

  static const cash = 'assets/icons/account/cash.svg';
  static const bankCard = 'assets/icons/account/bank_card.svg';
  static const creditCard = 'assets/icons/account/credit_card.svg';
  static const alipay = 'assets/icons/account/alipay.svg';
  static const wechatPay = 'assets/icons/account/wechat_pay.svg';

  static const fund = 'assets/icons/investment/fund.svg';
  static const stock = 'assets/icons/investment/stock.svg';
  static const crypto = 'assets/icons/investment/crypto.svg';

  static const food = 'assets/icons/category/food.svg';
  static const coffee = 'assets/icons/category/coffee.svg';
  static const transport = 'assets/icons/category/transport.svg';
  static const shopping = 'assets/icons/category/shopping.svg';
}
```

------

### 7.8 图标承载容器

分类、账户、投资类图标必须使用浅色圆形或圆角容器承载。

推荐：

```dart
Container(
  width: 44,
  height: 44,
  decoration: BoxDecoration(
    color: XoColors.primaryLight,
    borderRadius: BorderRadius.circular(22),
  ),
  child: const Center(
    child: XoIcon(
      XoIcons.food,
      size: 24,
      color: XoColors.primary,
    ),
  ),
)
```

规则：

```text
底部 Tab 图标不加背景
分类图标使用浅色圆形背景
账户图标使用浅色圆角方形背景
投资图标使用浅色圆角方形或圆形背景
危险操作图标使用浅红背景
警告操作图标使用浅橙背景
```

------

### 7.9 底部 Tab 图标

底部 Tab 图标使用线性 SVG。

状态：

```text
未选中：#6B7280
选中：#007C6E
```

底部 Tab 图标不使用复杂渐变。

示例：

```dart
NavigationDestination(
  icon: XoIcon(
    XoIcons.home,
    color: XoColors.textSecondary,
  ),
  selectedIcon: XoIcon(
    XoIcons.home,
    color: XoColors.primary,
  ),
  label: '首页',
)
```

------

### 7.10 插画规范

登录、注册、启动页允许使用轻量金融科技插画。

允许元素：

```text
数据波纹
金融城市剪影
点阵球体
趋势线
柱状图
环形轨道
柔和光点
```

插画规则：

```text
插画必须低对比
插画不能影响表单阅读
插画颜色必须使用深青绿、科技青、浅灰绿
插画不能使用复杂人物
插画不能使用卡通风
插画不能抢主按钮和输入框层级
```

推荐放置位置：

```text
登录页顶部
注册页顶部
启动页中部或背景弱化区域
空状态页面中心
AI 报告页顶部轻量点缀
```

------

## 8. 核心组件约束

必须优先封装并复用以下组件。

### 8.1 基础组件

```text
XoPage
XoCard
XoMoneyText
XoIcon
XoButton
XoTextField
XoEmpty
XoLoading
XoErrorView
XoSectionHeader
XoBottomSheet
```

------

### 8.2 业务组件

```text
XoAssetOverviewCard
XoQuickStatCard
XoTransactionItem
XoInvestmentItem
XoBudgetProgressCard
XoAiSummaryCard
XoAccountCard
XoFeatureIcon
XoAuthHeader
XoAuthScaffold
```

------

### 8.3 组件使用规则

1. 页面容器必须优先使用 `XoPage`。
2. 卡片必须优先使用 `XoCard`。
3. 金额必须使用 `XoMoneyText`。
4. 图标必须使用 `XoIcon`。
5. 主按钮必须使用 `XoButton`。
6. 输入框必须使用 `XoTextField`。
7. 空状态必须使用 `XoEmpty`。
8. 加载状态必须使用 `XoLoading`。
9. 错误状态必须使用 `XoErrorView`。
10. 登录、注册页必须优先使用 `XoAuthScaffold`。
11. 登录、注册页顶部品牌区域必须使用 `XoAuthHeader`。
12. 不允许每个页面单独写一套卡片样式。
13. 不允许金额格式散落在页面中。
14. 不允许图标路径散落在页面中。
15. 不允许页面直接写死渐变、阴影和圆角。

------

## 9. Codex UI 执行约束

当 Codex 执行 UI 相关任务时，必须遵守：

```text
先读取本 UI 规范
先检查是否已有 XO Design System
优先补全 token
优先复用 XoPage / XoCard / XoIcon / XoButton / XoTextField
不得直接在页面中硬编码颜色
不得直接在页面中硬编码 SVG 路径
不得引入大型 UI 组件库
不得将 Web 页面缩小为移动端页面
不得偏离深青绿科技金融风格
```

UI 任务完成后必须说明：

```text
新增了哪些 token
新增了哪些组件
新增了哪些图标资源
修改了哪些页面
是否符合登录 / 注册页视觉基线
是否存在未完成的视觉资源
```

------

## 10. 推荐目录结构

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
│   │   ├── xo_assets.dart
│   │   ├── xo_colors.dart
│   │   ├── xo_gradients.dart
│   │   ├── xo_radius.dart
│   │   ├── xo_shadows.dart
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
│       ├── xo_button.dart
│       ├── xo_text_field.dart
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

------

## 11. 路由约束

必须使用 `go_router`。

基础路由：

```text
/splash
/login
/register
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
  static const register = '/register';
  static const main = '/main';
  static const transactionEdit = '/transaction/edit';
  static const investmentTrade = '/investment/trade';
  static const budget = '/budget';
  static const report = '/report';
  static const settings = '/settings';
}
```

------

## 12. 底部导航约束

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

------

## 13. 全局状态约束

必须使用 Riverpod。

至少包含：

```text
authProvider
appSettingsProvider
mainTabProvider
```

### 13.1 authProvider

职责：

```text
登录状态
Token 状态
真实登录 / 注册状态
退出登录
```

### 13.2 appSettingsProvider

职责：

```text
是否隐藏金额
是否深色模式
主题配置预留
```

### 13.3 mainTabProvider

职责：

```text
当前底部 Tab index
```

------

## 14. 网络层约束

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

### 14.1 网络层规则

1. 统一配置 `baseUrl`。
2. 统一配置超时时间。
3. 统一添加 Token。
4. 统一处理 401。
5. 统一处理业务异常。
6. 页面不允许直接调用 Dio。
7. 页面必须通过 Repository / ApiClient 间接调用接口。
8. 注册成功后如后端未直接返回 Token，必须复用登录接口建立会话，不允许前端伪造 Token。

### 14.2 默认 API 地址

```text
http://localhost:8080/api
```

后续通过环境配置切换。

------

# 13. 开发阶段总览

## 阶段 0：基础约束与项目准备

### 目标

建立项目开发规则，防止 Codex 乱改、乱删、越阶段实现。

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
Codex 后续任务有约束依据
```

------

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

### 页面范围

```text
SplashPage
LoginPage
MainTabPage
HomePage
LedgerPage
InvestmentPage
ProfilePage
SettingsPage
TransactionEditPage
InvestmentTradePage
BudgetPage
ReportPage
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

------

## 阶段 2：登录与基础 API

### 目标

建立真实登录能力和基础 API 通信能力。

### 任务

```text
完善 Login 页面
接入登录接口
保存 accessToken
保存 refreshToken
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
POST /api/auth/logout
POST /api/auth/refresh-token
GET  /api/auth/me
```

### 本地存储

```text
accessToken -> Secure Storage
refreshToken -> Secure Storage
hideAmount -> SharedPreferences
darkMode -> SharedPreferences
```

### 禁止

```text
不要把 Token 存 SharedPreferences
不要在页面直接操作 Dio
不要在页面直接操作 Secure Storage
不要直接连接 MySQL
不要做离线同步
```

### 验收标准

```text
可以登录
可以退出
重启 App 后可恢复登录态
Token 可自动添加到请求头
401 有统一处理
```

### 当前完成情况

```text
已接入 POST /api/auth/login
已接入 GET /api/auth/me
accessToken 已保存到 Secure Storage
请求已自动附加 Authorization: Bearer <token>
401 / 40100 已统一清理本地 token
退出登录当前按后端能力只清除本地 token
登录失败、空值校验和网络 / CORS 错误已使用弹窗提示
登录失败后保留输入框内容，避免路由刷新清空表单
```

### Web 调试注意

```text
Android Studio 的 Chrome 目标会启动 flutter run -d chrome 调试会话。
该地址依赖自动弹出的 Chrome、DWDS 和 VM Service，不适合作为普通网页复制到其他浏览器。
如果复制到其他浏览器出现白屏，优先使用 Android Studio 自动弹出的 Chrome，或改用 flutter build web + 静态服务器预览。
Chrome DevTools 设备模式缩放不是 100% 时，CanvasKit 文字可能因浏览器缩放发虚。
```

------

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
```

### 推荐返回结构

```json
{
  "totalAsset": "128560.80",
  "netAsset": "96520.30",
  "todayExpense": "128.50",
  "monthExpense": "4268.90",
  "monthIncome": "18000.00",
  "monthBalance": "13731.10",
  "accountSummary": [],
  "investmentSummary": {
    "fundYesterdayProfit": "126.30",
    "stockTodayProfit": "-58.20",
    "crypto24hProfit": "88.12"
  },
  "aiSummary": "今日支出正常，餐饮支出略高，投资整体小幅上涨。",
  "recentTransactions": []
}
```

### UI 规则

```text
首页不要堆大量图表
总资产必须最突出
投资收益不要统一叫今日收益
金额必须支持隐藏
最近流水最多展示 5 条
```

### 禁止

```text
不要在首页做复杂统计报表
不要在首页堆饼图
不要混淆基金、股票、虚拟货币收益口径
```

### 验收标准

```text
首页接口可请求
首页数据可展示
加载态正常
错误态正常
金额隐藏正常
```

------

## 阶段 4：记账与流水录入

### 目标

实现日常记账能力。

### 记账页设计

```text
年月切换
日历视图
当天收入
当天支出
当天结余
当天流水列表
记一笔入口
```

### 流水录入页设计

```text
收入 / 支出 / 转账切换
金额输入
分类选择
账户选择
时间选择
备注
图片附件
保存按钮
```

### 推荐接口

```text
GET    /api/app/transactions/calendar?month=2026-06
GET    /api/app/transactions/daily?date=2026-06-16
POST   /api/app/transactions
PUT    /api/app/transactions/{id}
DELETE /api/app/transactions/{id}
```

### 规则

```text
记账页默认显示当天
点不同日期展示不同流水
不要在记账主页面放本月支出分类占比
流水录入页不做标签功能
流水录入页支持图片附件入口
图片上传可以后续阶段实现
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

------

## 阶段 5：账户管理

### 目标

实现账户资产管理。

### 账户类型

```text
现金
银行卡
支付宝
微信
信用卡
其他
```

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

### 禁止

```text
不要把投资资产放到账户页里
不要把账户和投资混成一个列表
不要直接修改余额且无记录
```

### 验收标准

```text
可以查看账户列表
可以新增账户
可以编辑账户
可以查看账户详情
可以查看账户相关流水
```

------

## 阶段 6：投资管理

### 目标

实现投资资产管理。

### 投资类型

```text
基金
股票
虚拟货币
其他
```

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

### UI 规则

```text
投资页独立
不要做成交易所
不要过度炫酷
收益颜色要克制
风险提示要清楚
```

### 禁止

```text
不要把基金收益写成今日收益
不要把所有投资统一强行按一个口径展示
不要在 App 端计算最终资产收益
最终收益以后端计算结果为准
```

### 验收标准

```text
可以查看投资总览
可以按类型查看持仓
可以新增投资交易
可以查看持仓详情
收益口径展示正确
```

------

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

### UI 规则

```text
预算页要轻量
预算进度要清楚
接近超支用橙色
已超支用红色
正常状态用绿色
```

### 禁止

```text
不要把预算页做成复杂报表
不要在预算页展示无关投资信息
```

### 验收标准

```text
可以查看本月预算
可以查看分类预算
可以新增预算
可以编辑预算
可以看到超支提醒
```

------

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

### UI 规则

```text
AI 报告页不是聊天页
报告内容要短
建议必须可执行
不要写空话
不要占据核心业务流程
```

### 好的示例

```text
本月餐饮支出 1268 元，比上月高 18%。
主要增长来自外卖和咖啡，建议下周设置 300 元餐饮预算。
```

### 不好的示例

```text
建议您合理规划财务。
```

### 禁止

```text
不要做成 AI 聊天首页
不要让 AI 功能影响记账主流程
不要生成过长报告
```

### 验收标准

```text
可以查看今日报告
可以查看月度报告
可以生成报告
报告加载态正常
报告错误态正常
```

------

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

### 规则

```text
金额隐藏必须全局生效
深色模式可以先预留
设置项要分类清晰
危险操作需要二次确认
```

### 禁止

```text
不要把业务功能堆在设置页
不要在设置页直接写复杂逻辑
```

### 验收标准

```text
金额隐藏生效
退出登录正常
设置项展示正常
版本信息展示正常
```

------

## 阶段 10：附件与图片上传

### 目标

支持流水附件图片能力。

### 页面范围

```text
流水录入页
流水详情页
图片预览页
```

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

### 规则

```text
图片上传走后端接口
图片 URL 由后端返回
App 不直接操作对象存储密钥
上传失败可以提示重试
第一版不做离线队列
```

### 禁止

```text
不要把云存储密钥写进 App
不要在本地长期保存附件业务数据
不要做离线上传队列
```

### 验收标准

```text
可以选择图片
可以拍照
可以上传图片
可以预览图片
可以删除附件
```

------

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

### 禁止

```text
不要在首页堆多个图表
不要使用过重图表组件
不要让图表影响首屏性能
```

### 验收标准

```text
趋势图展示正常
分类图展示正常
加载性能可接受
空数据展示正常
```

------

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

### 规则

```text
长列表使用合适的列表组件
图片需要缓存
重复请求需要控制
页面状态要清晰
```

### 禁止

```text
不要为了动效牺牲性能
不要引入过重依赖
不要重复请求同一个接口
```

### 验收标准

```text
首页打开流畅
列表滚动流畅
图片加载稳定
错误提示友好
弱网有反馈
```

------

## 阶段 13：Android / iOS 上架准备

### 目标

完成应用上架前准备。

### Android

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

### iOS

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

### 权限

```text
网络权限
相机权限
相册权限
通知权限，后期可选
```

### 禁止

```text
不要申请无关权限
不要在权限说明里写模糊文案
不要把测试环境地址打进生产包
```

### 验收标准

```text
Android 可打包
iOS 可打包
权限说明完整
隐私政策入口存在
生产环境配置正确
```

------

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

### 禁止

```text
不要提交无法 analyze 的代码
不要提交明显未格式化代码
不要忽略严重 lint
```

### 验收标准

```text
flutter analyze 无严重错误
flutter test 可运行
Android 构建通过
README 更新完整
```

------

# 14. Codex 工作流规则

每次让 Codex 执行任务时，必须遵守以下规则。

## 14.1 执行前

Codex 必须先做：

```text
扫描当前目录
说明准备修改哪些文件
说明准备新增哪些文件
说明不会修改哪些目录
输出执行计划
等待确认
```

## 14.2 执行中

Codex 必须遵守：

```text
只做当前阶段任务
不要越阶段实现
不要引入未批准依赖
不要删除无关文件
不要重写后端
不要重写 Web
不要生成大量无关代码
```

## 14.3 执行后

Codex 必须输出：

```text
完成内容
新增文件
修改文件
运行命令
验证结果
遗留问题
下一步建议
```

------

# 15. 每阶段通用验收模板

每个阶段完成后，必须填写：

~~~md
## 本阶段完成内容

- 

## 新增文件

- 

## 修改文件

- 

## 新增依赖

- 

## 运行命令

```bash
~~~

## 验证结果

-  flutter pub get 成功
-  flutter analyze 通过
-  Android 可运行
-  iOS 可运行，若当前环境支持

## 未完成事项

- 

## 风险点

- 

## 下一阶段建议

- 

```
---

# 16. Codex 通用提示词模板

后续每个阶段都可以使用以下模板。

```md
请根据 `MOBILE_APP_PHASES.md` 执行当前阶段任务。

当前阶段：

阶段 X：{阶段名称}

要求：

1. 严格遵守 `MOBILE_APP_PHASES.md`。
2. 只做当前阶段内容，不要越阶段实现。
3. 不要引入 SQLite / Drift / Hive / Isar。
4. 不要引入大型 UI 组件库。
5. 不要直接连接 MySQL。
6. 不要修改后端代码。
7. 不要修改 Web 端代码。
8. 不要删除无关文件。
9. 修改前先扫描项目结构。
10. 先输出执行计划，等待确认后再改代码。

请先输出：

- 当前项目结构分析
- 准备新增的文件
- 准备修改的文件
- 本阶段实现计划
- 风险点
```

------

# 17. 总体阶段顺序

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

------

# 18. 长期原则

1. 移动端优先。
2. 后端 MySQL 是主数据源。
3. App 不做直接数据库连接。
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
