# 本地开发与部署

## 1. 基础访问

```text
Web Nginx: http://localhost:8088
后端服务: http://localhost:8080
Knife4j: http://localhost:8080/doc.html
XXL-JOB Admin: http://localhost:8081/xxl-job-admin
```

测试账号：

```text
demo / xoassets123
```

## 2. 启动后端

```bash
cd xoassets-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn spring-boot:run
```

## 3. 启动 Web

```bash
cd xoassets-web
npm install
npm run dev
```

## 4. 启动 React Native App

移动端目录：

```text
xoassets-app/
```

当前仓库实际使用 npm / package-lock，启动命令如下：

```bash
cd xoassets-app
npm install
```

### Expo Go 真机调试

当前 Expo Metro 固定使用 `8088` 端口，并跳过 Expo 联网依赖校验：

```bash
npm run start
```

手机和电脑需在同一局域网，启动后用 Expo Go 扫终端二维码。

### Expo Web 预览

```bash
npm run web
```

访问地址：

```text
http://localhost:8088
```

### Android 模拟器

```bash
npm run android:emulator
npm run android
```

说明：

- `npm run android:emulator` 启动本机 AVD，默认 `xoassets_api36`，并等待系统启动完成。
- `npm run android` 固定使用本机 OpenJDK 17、Android SDK 和 Metro `8088` 端口。
- Android 构建前会执行 `scripts/patch-react-native-gradle-repos.js`，用于给 React Native / Expo 的 Gradle 插件和 RN 模块临时补国内 Maven 镜像，避免 Maven Central 403。
- 如果只需要模拟器浏览器访问 Expo Web，可先执行 `adb reverse tcp:8088 tcp:8088`，再访问 `http://localhost:8088`。

### iOS 模拟器

```bash
npm run ios
```

### Expo 常用排查命令

```bash
lsof -nP -iTCP:8088 -sTCP:LISTEN
EXPO_NO_DEPENDENCY_VALIDATION=1 npx expo start --port 8088 --clear
adb reverse tcp:8088 tcp:8088
```

## 5. API Base URL

当前项目建议：

```text
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080
```

Android 模拟器访问宿主机本地后端通常使用：

```text
http://10.0.2.2:8080
```

iOS 模拟器通常使用：

```text
http://localhost:8080
```

真机调试需要使用电脑局域网 IP：

```text
http://<你的电脑IP>:8080
```

## 6. 环境变量建议

```text
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080
EXPO_PUBLIC_APP_ENV=dev
```

规则：

- 只允许把非敏感配置放入 `EXPO_PUBLIC_*`。
- 生产密钥不得写入 App。
- 对象存储密钥不得写入 App。
- MySQL 地址、账号、密码不得写入 App。
