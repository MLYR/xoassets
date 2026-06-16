# XOAssets Mobile V2

XOAssets / 小〇财迹新版移动端 Flutter App。当前阶段只交付 App 壳子：路由、底部导航、Material 3 主题、XO Design System、网络层、Token 存储、全局状态和核心页面骨架。

## 技术栈

- Flutter / Dart
- Material 3
- Riverpod
- go_router
- Dio
- flutter_secure_storage
- shared_preferences
- intl

App 不直接连接 MySQL，只通过 Spring Boot HTTP API 访问后端。

## 目录结构

```text
lib/
├── app/                    # App 入口、路由、主题
├── core/
│   ├── constants/          # App / API 常量
│   ├── design/             # XO Design System token
│   ├── errors/             # 统一异常处理
│   ├── network/            # Dio、拦截器、ApiClient
│   ├── storage/            # Secure Storage / Preferences
│   ├── utils/              # 金额、日期、数字工具
│   └── widgets/            # XoPage / XoCard / XoMoneyText 等
├── features/               # splash/auth/main/home/ledger/investment 等
└── shared/                 # 共享模型和枚举预留
```

## 运行方式

```bash
cd /Users/zreo/CODE/XOAssets/xoassets-mobile
flutter pub get
flutter run
```

指定设备：

```bash
flutter devices
flutter run -d <device-id>
```

## Android Studio 启动

当前只需要先跑 Android：

1. 确认 Android Studio 已安装 Flutter 插件。
2. 打开 `/Users/zreo/CODE/XOAssets/xoassets-mobile`。
3. 顶部运行配置选择 Flutter 类型，不要选择 Dart 命令行应用。
4. 入口文件选择 `lib/main.dart`。
5. 设备选择 Android 模拟器，例如 `xoassets_api36`。
6. 点击 Run。

如果控制台命令开头是 `dart ... lib/main.dart`，说明运行配置选错了；正确启动应由 Flutter 执行，或直接使用：

```bash
cd /Users/zreo/CODE/XOAssets/xoassets-mobile
flutter run -d <android-device-id>
```

本机已配置的 Android 相关路径：

```text
Flutter SDK: /opt/homebrew/share/flutter
Dart SDK: /opt/homebrew/share/flutter/bin/cache/dart-sdk
Android SDK: /Users/zreo/Library/Android/sdk
JDK 17: /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

## 环境配置

默认 API Base URL：

```text
http://localhost:8080/api
```

配置位置：

```text
lib/core/constants/api_constants.dart
```

当前机器如果缺 Android SDK、完整 Xcode 或 CocoaPods，需要先补齐对应移动端工具链后才能运行 Android / iOS 模拟器。

## 路由说明

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

`/main` 内部通过底部导航切换：首页、记账、投资、我的。中间悬浮 `+` 会弹出快捷操作面板。

## 状态管理

- `authProvider`：mock 登录、Token 状态、退出登录预留。
- `appSettingsProvider`：金额隐藏、深色模式预留。
- `mainTabProvider`：底部 Tab 当前 index。

## 网络请求

网络层位于 `lib/core/network/`：

- `ApiClient`
- `dioProvider`
- `AuthInterceptor`
- `XoLogInterceptor`
- `ApiResponse`
- `AppException`
- `ErrorHandler`

本阶段不调用真实接口，但结构已按后续接入预留。

## Design System

XO Design System 位于 `lib/core/design/`：

- `XoColors`
- `XoRadius`
- `XoSpacing`
- `XoTextStyles`
- `AppTheme`

页面必须优先使用：

- `XoPage`
- `XoCard`
- `XoMoneyText`
- `XoSectionHeader`
- `XoBottomSheet`

## 第一阶段已完成

- Flutter Android / iOS 工程初始化。
- Material 3 light theme，dark theme 预留。
- 底部导航：首页、记账、投资、我的。
- 中间悬浮快捷操作按钮。
- Splash、Login、Home、Ledger、Transaction Edit、Investment、Investment Trade、Budget、Report、Profile、Settings 页面骨架。
- Dio 网络层、Token 存储、偏好设置存储基础结构。
- AGENTS.md 项目约束文件。

## 下一阶段建议

- 接入真实登录 `/api/auth/**`。
- 接入首页资产快照、统计、预算和 AI 报告接口。
- 接入记账流水列表与新增流水接口。
- 接入投资持仓、投资交易和行情刷新接口。
- 完成深色模式持久化和系统主题跟随。
