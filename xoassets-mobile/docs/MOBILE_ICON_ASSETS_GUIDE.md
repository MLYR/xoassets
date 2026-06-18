# 移动端图标与资源规范

## 1. App 图标

推荐规格：

```text
1024x1024 PNG
```

推荐文件：

```text
assets/images/app_icon.png
assets/images/app_icon_foreground.png
```

推荐工具：

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

## 2. 启动页 Logo

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

## 3. App 内部图标风格

所有功能图标统一使用 SVG。

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

## 4. 图标目录结构

```text
assets
├── icons
│   ├── app
│   │   ├── logo.svg
│   │   └── logo_mark.svg
│   ├── tab
│   │   ├── home.svg
│   │   ├── ledger.svg
│   │   ├── investment.svg
│   │   └── profile.svg
│   ├── action
│   │   ├── add_transaction.svg
│   │   ├── transfer.svg
│   │   ├── investment_trade.svg
│   │   ├── add_account.svg
│   │   └── add_budget.svg
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
│   ├── account
│   │   ├── cash.svg
│   │   ├── bank_card.svg
│   │   ├── credit_card.svg
│   │   ├── alipay.svg
│   │   └── wechat_pay.svg
│   └── investment
│       ├── fund.svg
│       ├── stock.svg
│       ├── crypto.svg
│       └── other.svg
└── images
    ├── app_icon.png
    └── splash_logo.png
```

## 5. 命名规范

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

## 6. 代码使用规则

所有 SVG 图标必须通过 `XoIcon` 使用。

禁止页面直接调用：

```dart
SvgPicture.asset(...)
```

图标路径必须统一维护在 `XoIcons`。

## 7. 图标承载容器

分类、账户、投资类图标必须使用浅色圆形或圆角容器承载。

规则：

```text
底部 Tab 图标不加背景
分类图标使用浅色圆形背景
账户图标使用浅色圆角方形背景
投资图标使用浅色圆角方形或圆形背景
危险操作图标使用浅红背景
警告操作图标使用浅橙背景
```

## 8. 插画规范

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

规则：

```text
插画必须低对比
插画不能影响表单阅读
插画颜色必须使用深青绿、科技青、浅灰绿
插画不能使用复杂人物
插画不能使用卡通风
插画不能抢主按钮和输入框层级
```
