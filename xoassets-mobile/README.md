# XOAssets Mobile V2

XOAssets / 小〇财迹新版移动端 Flutter App。

当前已完成：

- App 壳子
- 真实登录
- 注册页
- 基础 API 通信
- Token 存储
- 登录态恢复
- 401 基础处理
- 首页部分接口接入
- 记账页部分接口接入
- 投资页部分接口接入
- 主题模式设置

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

## 文档入口

| 文件 | 说明 |
|---|---|
| `AGENTS.md` | 移动端 AI 协作约束 |
| `MOBILE_APP_PHASES.md` | 移动端阶段计划 |
| `docs/MOBILE_PRODUCT_SPEC.md` | 页面与功能说明 |
| `docs/MOBILE_UI_DESIGN_SYSTEM.md` | UI 设计系统 |
| `docs/MOBILE_ICON_ASSETS_GUIDE.md` | 图标与启动图规范 |
| `docs/MOBILE_API_INTEGRATION.md` | 接口接入状态 |
| `docs/MOBILE_QA.md` | 设计 QA 记录 |

## 运行方式

```bash
cd xoassets-mobile
flutter pub get
flutter run
```

指定设备：

```bash
flutter devices
flutter run -d <device-id>
```

Android 模拟器访问本机后端：

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

## Android Studio 启动

1. 确认 Android Studio 已安装 Flutter 插件。
2. 打开 `xoassets-mobile`。
3. 顶部运行配置选择 Flutter 类型，不要选择 Dart 命令行应用。
4. 入口文件选择 `lib/main.dart`。
5. 设备选择 Android 模拟器。
6. 点击 Run。

如果控制台命令开头是 `dart ... lib/main.dart`，说明运行配置选错了；正确启动应由 Flutter 执行。

## 环境配置

默认 API Base URL：

```text
http://localhost:8080/api
```

配置位置：

```text
lib/core/constants/api_constants.dart
```

可通过 `--dart-define=XO_API_BASE_URL=<url>` 覆盖。

## 路由

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
