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

## 2. 初始化本机 MySQL

```bash
cd xoassets-server
mysql -uroot -p < src/main/resources/db/schema.sql
mysql -uroot -p xoassets < src/main/resources/db/migration-market-calendar.sql
mysql -uroot -p xoassets < src/main/resources/db/dev-data.sql
mysql -uroot -p < src/main/resources/db/xxl-job-init.sql
```

`xxl-job-init.sql` 会创建独立库 `xxl_job`，业务库仍是 `xoassets`。

## 3. 启动后端

后端按 Java 17 编译。若本机有多个 JDK，建议显式指定：

```bash
cd xoassets-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn spring-boot:run
```

编译：

```bash
cd xoassets-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -DskipTests compile
```

## 4. 启动 Web

```bash
cd xoassets-web
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

## 5. 启动 Flutter

```bash
cd xoassets-mobile
flutter pub get
flutter run
```

Android 模拟器访问宿主机本地后端：

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

启动后访问：

```text
http://localhost:5175
http://<你的电脑IP>:5175
```

`0.0.0.0` 只用于服务监听，不建议在浏览器地址栏访问。

## 6. Docker 启动 XXL-JOB Admin

默认按本机 MySQL `root / root` 连接：

```bash
cd /Users/zreo/CODE/XOAssets
docker compose -f docker-compose.local-xxl.yml up -d
```

如果本机 MySQL 密码不是 `root`：

```bash
LOCAL_MYSQL_USERNAME=root LOCAL_MYSQL_PASSWORD=你的密码 docker compose -f docker-compose.local-xxl.yml up -d
```

访问：

```text
http://localhost:8081/xxl-job-admin
admin / 123456
```

## 7. 后端注册 XXL-JOB Executor

需要让后端注册到 XXL-JOB Admin 时，在 IDEA Run Configuration 加环境变量：

```bash
XXL_JOB_EXECUTOR_ENABLED=true
XXL_JOB_ADMIN_ADDRESSES=http://localhost:8081/xxl-job-admin
XXL_JOB_ACCESS_TOKEN=xoassets-xxl-job-local-token
XXL_JOB_EXECUTOR_APPNAME=xoassets-server
XXL_JOB_EXECUTOR_ADDRESS=http://host.docker.internal:10099/
XXL_JOB_EXECUTOR_PORT=10099
```

本地 XXL-JOB Admin 跑在 Docker 中、后端 executor 跑在宿主机 IDEA 中时，`XXL_JOB_EXECUTOR_ADDRESS` 必须使用 `http://host.docker.internal:10099/`。

## 8. 全量 Docker 部署

服务器部署使用仓库根目录 `docker-compose.yml`：

```bash
cd /path/to/XOAssets
docker compose up -d
```

访问：

```text
前端: http://服务器IP:8088
后端: http://服务器IP:8080
Knife4j: http://服务器IP:8080/doc.html
XXL-JOB Admin: http://服务器IP:8081/xxl-job-admin
MySQL: 服务器IP:3306
```

重置初始化数据：

```bash
docker compose down -v
docker compose up -d
```

## 9. 日志环境

```bash
XOASSETS_PROFILE=dev
XOASSETS_PROFILE=test
XOASSETS_PROFILE=prod
XOASSETS_LOG_PATH=logs
```

规则：

- `dev` 可输出业务、MyBatis、JDBC 调试日志。
- `prod` 不要开启 SQL DEBUG。
- 运行日志写入 `logs/`。
- `logs/` 必须加入 `.gitignore`。
- 提交前不要把本地日志文件加入 Git。
