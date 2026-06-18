# XOAssets Mobile UI 设计系统

## 1. 整体视觉定位

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

## 2. 视觉关键词

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

## 3. 页面背景

默认页面背景：

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

装饰必须弱化，不能干扰输入框和按钮。

## 4. 卡片规范

所有主要内容必须卡片化展示。

卡片风格：

```text
白色背景
大圆角
轻阴影
弱边框
宽松内边距
```

推荐 token：

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

## 5. 表单规范

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

推荐 token：

```dart
class XoInputTokens {
  static const height = 56.0;
  static const radius = 16.0;
  static const background = Color(0xFFFFFFFF);
  static const border = Color(0xFFDCE7E3);
  static const focusedBorder = Color(0xFF007C6E);
}
```

## 6. 按钮规范

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

## 7. 登录 / 注册页视觉基线

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

允许视觉元素：

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

## 8. 首页视觉规范

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

## 9. 图表视觉规范

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

## 10. Design Token 目录

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

## 11. 核心组件

基础组件：

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

业务组件：

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

使用规则：

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
