# XOAssets Mobile V2

XOAssets / 小〇财迹新版移动端 Flutter App。当前已完成 App 壳子、真实登录、基础 API 通信、Token 存储、登录态恢复和 401 基础处理。

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

Android 模拟器访问本机后端时建议覆盖 API 地址：

```bash
flutter run -d <android-device-id> \
  --dart-define=XO_API_BASE_URL=http://10.0.2.2:8080/api
```

Flutter Web 调试固定使用 5175 端口：

```bash
flutter run -d web-server \
  --web-hostname 0.0.0.0 \
  --web-port 5175 \
  --dart-define=XO_API_BASE_URL=http://<你的电脑IP>:8080/api
```

启动后可在浏览器手动访问：

```text
http://localhost:5175
http://<你的电脑IP>:5175
```

`0.0.0.0` 只用于服务监听，不建议在浏览器地址栏里访问。Android Studio 选择 Chrome 目标时会启动 `flutter run -d chrome --start-paused` 调试会话，这个地址依赖当前自动弹出的 Chrome、DWDS 和 VM Service；把地址复制到另一个浏览器可能只拿到临时 debug 壳，出现白屏。Chrome DevTools 设备模式如果缩放到 75% 之类的比例，Flutter Web 的 CanvasKit 画布会被浏览器缩放，文字可能发虚；预览时建议把缩放调回 100%，或直接使用 Android 模拟器查看真实移动端效果。

如果只想稳定预览、不需要热重载，可以构建后用静态服务器打开，避免 Chrome Debug 的 DWDS WebSocket 干扰：

```bash
flutter build web \
  --dart-define=XO_API_BASE_URL=http://<你的电脑IP>:8080/api
python3 -m http.server 5175 --bind 0.0.0.0 --directory build/web
```

## Android Studio 启动

当前只需要先跑 Android：

1. 确认 Android Studio 已安装 Flutter 插件。
2. 打开 `/Users/zreo/CODE/XOAssets/xoassets-mobile`。
3. 顶部运行配置选择 Flutter 类型，不要选择 Dart 命令行应用。
4. 入口文件选择 `lib/main.dart`。
5. 设备选择 Android 模拟器，例如 `xoassets_api36`。
6. 点击 Run。

如果选择 Chrome / Web 目标运行，运行配置里已固定：

```text
--web-hostname=0.0.0.0 --web-port=5175 --dart-define=XO_API_BASE_URL=http://<你的电脑IP>:8080/api
```

Web 调试推荐用 `-d web-server` 后手动打开浏览器，不要用 Chrome Debug。Debug 会让 Android Studio 附加 `--start-paused`，页面可能白屏并出现 `$dwdsSseHandler` WebSocket 错误。

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

可通过 `--dart-define=XO_API_BASE_URL=<url>` 覆盖。Android 模拟器访问宿主机本地后端通常使用 `http://10.0.2.2:8080/api`。

当前机器如果缺 Android SDK、完整 Xcode 或 CocoaPods，需要先补齐对应移动端工具链后才能运行 Android / iOS 模拟器。

## 登录 / 注册视觉基线

登录和注册页是 XOAssets Mobile V2 的当前视觉基准：

- 整体风格：深青绿色科技金融、白色圆角表单卡片、轻量金融插画、克制玻璃质感。
- 品牌区统一使用 `XoAuthHeader`，表单页统一使用 `XoAuthScaffold`。
- 表单输入统一使用 `XoTextField`：56 高度、大圆角、浅灰绿色边框、左侧线性图标。
- 主操作统一使用 `XoButton`：56 高度、深青绿色渐变、白色加粗文字、轻阴影。
- 视觉资源统一放在 `assets/images/auth/`，路径由 `XoAssets` 维护。
- 页面不要绕过 `lib/core/design/` 直接散落颜色、渐变、圆角、阴影或图片路径。

本次设计 QA 记录位于：

```text
design-qa.md
```

## 路由说明

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

`/main` 内部通过底部导航切换：首页、记账、投资、我的。中间悬浮 `+` 会弹出快捷操作面板。

## 状态管理

- `authProvider`：真实登录、Token 状态、当前用户、登录态恢复、退出登录。
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

登录与当前用户接口已接入：

- `POST /api/auth/login`
- `GET /api/auth/me`

当前后端暂无 `/api/auth/logout` 和 `/api/auth/refresh-token`，移动端退出登录仅清除本地 token，refresh token 字段先做兼容预留。

## Design System

XO Design System 位于 `lib/core/design/`：

- `XoColors`
- `XoAssets`
- `XoGradients`
- `XoRadius`
- `XoShadows`
- `XoSpacing`
- `XoTextStyles`
- `AppTheme`

页面必须优先使用：

- `XoPage`
- `XoCard`
- `XoButton`
- `XoTextField`
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

## 第二阶段已完成

- 登录页接入真实 `/api/auth/login`。
- 注册页接入真实 `/api/auth/register`，注册成功后复用登录接口建立会话。
- 登录成功后将 accessToken 保存到 `flutter_secure_storage`。
- `AuthInterceptor` 自动为请求添加 `Authorization: Bearer <token>`。
- `GET /api/auth/me` 用于 App 启动后的登录态恢复。
- 401 / `40100` 会统一清理本地 token。
- 我的页展示当前登录用户，并提供退出登录入口。
- 登录 / 注册页提供基础 loading、空值校验、密码显隐、协议勾选、验证码倒计时和弹窗错误提示，Web 网络 / CORS 错误会转成中文提示。

## 下一阶段建议

- 接入首页资产快照、统计、预算和 AI 报告接口。
- 接入记账流水列表与新增流水接口。
- 接入投资持仓、投资交易和行情刷新接口。
- 完成深色模式持久化和系统主题跟随。
