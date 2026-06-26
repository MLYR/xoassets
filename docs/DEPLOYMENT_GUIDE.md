# XOAssets Docker 部署与增量重部署指南

> 本文档合并了原来的：
> - `docs/DEPLOYMENT_GUIDE.md`
> - `docs/DOCKER_INCREMENTAL_DEPLOYMENT_GUIDE.md`
>
> 目标只有一个：把 XOAssets 从本地稳定部署到服务器，并且清楚说明每个容器如何单独更新。

---

## 0. 先看结论

XOAssets 的部署分成两类镜像：

| 类型 | 镜像 | 处理方式 |
|---|---|---|
| 项目自研镜像 | `xoassets-server:1.0`、`xoassets-web:1.0` | 需要在本地构建后导出，再上传到服务器 |
| 官方现成镜像 | `mysql:8.0.46`、`redis:7.4.2`、`xuxueli/xxl-job-admin:3.4.0` | **不需要本地构建**，可直接在服务器 `docker pull`；如果服务器无法联网，再本地 `docker pull` 后 `docker save` |

也就是说：

- 后端和 Web，还是按你自己的代码打包
- MySQL、Redis 和 XXL-JOB Admin，直接用 Docker Hub 官方镜像即可
- 不要把“能直接拉的官方镜像”也当成项目产物去编译

如果你只想更新单个容器，直接看第 4 章。

---

## 1. 部署前准备

### 1.1 服务器目录

建议统一放到：

```text
/data/xoassets
```

### 1.2 服务器要求

- Docker Engine
- Docker Compose plugin
- `tar`、`gzip`
- 能访问 Docker Hub 时，MySQL、Redis 和 XXL-JOB Admin 可直接 `docker pull`

### 1.3 本地要求

- Docker
- Git
- MySQL 客户端或本地 MySQL 容器

---

## 2. 服务器架构先确认

先看服务器架构：

```bash
uname -m
```

| 输出 | 含义 | 目标平台 |
|---|---|---|
| `x86_64` | 常见 Intel / AMD 服务器 | `linux/amd64` |
| `aarch64` / `arm64` | ARM 服务器 | `linux/arm64` |

后续构建、拉取、导出镜像时，平台必须一致。

---

## 3. 首次部署流程

### 3.1 本地构建项目镜像

进入仓库根目录：

```bash
cd /Users/zreo/CODE/XOAssets
```

构建后端：

```bash
docker buildx build --platform linux/amd64 --provenance=false --sbom=false -t xoassets-server:1.0 --load ./xoassets-server
```

构建 Web：

```bash
docker buildx build --platform linux/amd64 --provenance=false --sbom=false -t xoassets-web:1.0 --load ./xoassets-web
```

> 如果服务器是 ARM，把 `linux/amd64` 改成 `linux/arm64`。

### 3.2 处理官方镜像

#### 方案 A：服务器可联网，直接在服务器拉取

```bash
docker pull --platform linux/amd64 mysql:8.0.46
docker pull --platform linux/amd64 redis:7.4.2
docker pull --platform linux/amd64 xuxueli/xxl-job-admin:3.4.0
```

#### 方案 B：服务器不能联网，本地先拉再导出

```bash
docker pull --platform linux/amd64 mysql:8.0.46
docker pull --platform linux/amd64 redis:7.4.2
docker pull --platform linux/amd64 xuxueli/xxl-job-admin:3.4.0

docker image save --platform linux/amd64 -o mysql-8.0.46.tar mysql:8.0.46
docker image save --platform linux/amd64 -o redis-7.4.2.tar redis:7.4.2
docker image save --platform linux/amd64 -o xxl-job-admin-3.4.0.tar xuxueli/xxl-job-admin:3.4.0
```

这两类镜像都不需要“本地编译”，只是在联网条件不一样时决定是直接 `pull` 还是 `save/load`。

### 3.3 导出项目镜像

```bash
docker image save --platform linux/amd64 -o xoassets-server.tar xoassets-server:1.0
docker image save --platform linux/amd64 -o xoassets-web.tar xoassets-web:1.0
```

### 3.4 导出数据库

导出业务库：

```bash
mysqldump -uroot -p \
  --single-transaction \
  --routines --triggers --events \
  --default-character-set=utf8mb4 \
  xoassets | gzip > xoassets.sql.gz
```

导出 XXL-JOB 库：

```bash
mysqldump -uroot -p \
  --single-transaction \
  --routines --triggers --events \
  --default-character-set=utf8mb4 \
  xxl_job | gzip > xxl_job.sql.gz
```

### 3.5 从 Docker 容器导出数据库

如果 MySQL 已经运行在 Docker 容器里，可以直接从容器里导出 SQL：

导出业务库：

```bash
MYSQL_ROOT_PASSWORD=$(grep '^MYSQL_ROOT_PASSWORD=' /data/xoassets/.env.prod | cut -d= -f2-)

docker exec -i xoassets-mysql mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \
  --single-transaction \
  --routines --triggers --events \
  --default-character-set=utf8mb4 \
  xoassets | gzip > xoassets.sql.gz
```

导出 XXL-JOB 库：

```bash
MYSQL_ROOT_PASSWORD=$(grep '^MYSQL_ROOT_PASSWORD=' /data/xoassets/.env.prod | cut -d= -f2-)

docker exec -i xoassets-mysql mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \
  --single-transaction \
  --routines --triggers --events \
  --default-character-set=utf8mb4 \
  xxl_job | gzip > xxl_job.sql.gz
```

---

## 4. 上传到服务器

把下面这些文件传到 `/data/xoassets`：

- `xoassets-server.tar`
- `xoassets-web.tar`
- `xoassets.sql.gz`
- `xxl_job.sql.gz`
- `redis-7.4.2.tar`
- `docker-compose.prod.yml`
- `.env.prod`
- `deploy.sh`（可选）

示例：

```bash
scp xoassets-server.tar root@服务器IP:/data/xoassets/
scp xoassets-web.tar root@服务器IP:/data/xoassets/
scp xoassets.sql.gz root@服务器IP:/data/xoassets/
scp xxl_job.sql.gz root@服务器IP:/data/xoassets/
scp docker-compose.prod.yml root@服务器IP:/data/xoassets/
scp .env.prod root@服务器IP:/data/xoassets/
```

如果你采用“服务器直接 pull 官方镜像”的方案，`mysql-8.0.46.tar` 和 `xxl-job-admin-3.4.0.tar` 就不需要上传。

---

## 5. 配置文件

### 5.1 `docker-compose.prod.yml`

```yaml
services:
  xoassets-mysql:
    image: mysql:8.0.46
    container_name: xoassets-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: xoassets
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - xoassets-mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -uroot -p${MYSQL_ROOT_PASSWORD} --silent"]
      interval: 5s
      timeout: 3s
      retries: 30

  xoassets-redis:
    image: redis:7.4.2
    container_name: xoassets-redis
    command: ["redis-server", "--appendonly", "yes", "--requirepass", "${REDIS_PASSWORD}"]
    environment:
      TZ: Asia/Shanghai
    ports:
      - "6379:6379"
    volumes:
      - xoassets-redis-data:/data

  xxl-job-admin:
    image: xuxueli/xxl-job-admin:3.4.0
    platform: linux/amd64
    container_name: xxl-job-admin
    environment:
      PARAMS: >-
        --server.port=8080
        --spring.datasource.url=jdbc:mysql://xoassets-mysql:3306/xxl_job?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
        --spring.datasource.username=root
        --spring.datasource.password=${MYSQL_ROOT_PASSWORD}
        --xxl.job.accessToken=${XXL_JOB_ACCESS_TOKEN}
      TZ: Asia/Shanghai
    depends_on:
      xoassets-mysql:
        condition: service_healthy
      xoassets-redis:
        condition: service_started
    ports:
      - "8081:8080"

  xoassets-server:
    image: xoassets-server:1.0
    container_name: xoassets-server
    volumes:
      - /data/xoassets/logs/xoassets-server:/app/logs
    environment:
      XOASSETS_DATASOURCE_URL: jdbc:mysql://xoassets-mysql:3306/xoassets?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
      XOASSETS_DB_USERNAME: root
      XOASSETS_DB_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      XOASSETS_REDIS_HOST: xoassets-redis
      XOASSETS_REDIS_PORT: 6379
      XOASSETS_REDIS_PASSWORD: ${REDIS_PASSWORD}
      XOASSETS_REDIS_DATABASE: 0
      XOASSETS_JWT_SECRET: ${XOASSETS_JWT_SECRET}
      XOASSETS_CORS_ALLOWED_ORIGINS: ${XOASSETS_CORS_ALLOWED_ORIGINS}
      XOASSETS_JWT_EXPIRE_MINUTES: 10080
      XXL_JOB_EXECUTOR_ENABLED: true
      XXL_JOB_ADMIN_ADDRESSES: http://xxl-job-admin:8080/xxl-job-admin
      XXL_JOB_ACCESS_TOKEN: ${XXL_JOB_ACCESS_TOKEN}
      XXL_JOB_EXECUTOR_APPNAME: xoassets-server
      XXL_JOB_EXECUTOR_ADDRESS: http://xoassets-server:9999/
      XXL_JOB_EXECUTOR_PORT: 9999
      TZ: Asia/Shanghai
    depends_on:
      xoassets-mysql:
        condition: service_healthy
      xoassets-redis:
        condition: service_started
      xxl-job-admin:
        condition: service_started
    ports:
      - "8080:8080"

  xoassets-web:
    image: xoassets-web:1.0
    container_name: xoassets-web
    depends_on:
      - xoassets-server
    ports:
      - "8088:80"

volumes:
  xoassets-mysql-data:
  xoassets-redis-data:
```

XXL-JOB 任务地址说明：

- 服务器 Docker 部署时，执行器地址必须使用容器网络可达地址
- 推荐在调度中心里使用自动注册
- 如果手动填写地址，必须是 `http://xoassets-server:9999/`
- 不要在服务器环境里使用 `http://host.docker.internal:10099/`，这个只适合本机开发场景

后端日志卷说明：

- 容器内日志路径是 `/app/logs`
- 宿主机持久化路径是 `/data/xoassets/logs/xoassets-server`
- 这样重建 `xoassets-server` 时，历史日志不会丢

### 5.2 `.env.prod`

```bash
MYSQL_ROOT_PASSWORD=root
REDIS_PASSWORD=replace-with-redis-password
XOASSETS_JWT_SECRET=replace-with-at-least-32-bytes-secret-key
XXL_JOB_ACCESS_TOKEN=xoassets-xxl-job-prod-token
XOASSETS_CORS_ALLOWED_ORIGINS=*
```

---

## 6. 导入镜像

### 6.1 如果你拿到的是 tar 文件

```bash
cd /data/xoassets
docker load -i xoassets-server.tar
docker load -i xoassets-web.tar
```

如果你也导出了官方镜像：

```bash
docker load -i mysql-8.0.46.tar
docker load -i redis-7.4.2.tar
docker load -i xxl-job-admin-3.4.0.tar
```

### 6.2 如果你选择在服务器直接拉取

```bash
docker pull --platform linux/amd64 mysql:8.0.46
docker pull --platform linux/amd64 redis:7.4.2
docker pull --platform linux/amd64 xuxueli/xxl-job-admin:3.4.0
```

---

## 7. 首次启动

### 7.1 先启动 MySQL

```bash
cd /data/xoassets
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d xoassets-mysql
```

### 7.2 启动 Redis

```bash
cd /data/xoassets
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d xoassets-redis
```

### 7.3 创建数据库

```bash
MYSQL_ROOT_PASSWORD=$(grep '^MYSQL_ROOT_PASSWORD=' /data/xoassets/.env.prod | cut -d= -f2-)

docker exec -i xoassets-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<'SQL'
CREATE DATABASE IF NOT EXISTS xoassets DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS xxl_job DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SQL
```

### 7.4 导入业务库和 XXL-JOB 库

```bash
MYSQL_ROOT_PASSWORD=$(grep '^MYSQL_ROOT_PASSWORD=' /data/xoassets/.env.prod | cut -d= -f2-)

gunzip -c /data/xoassets/xoassets.sql.gz | docker exec -i xoassets-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" xoassets
gunzip -c /data/xoassets/xxl_job.sql.gz | docker exec -i xoassets-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" xxl_job
```

### 7.5 启动剩余容器

```bash
cd /data/xoassets
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d xxl-job-admin xoassets-server xoassets-web
```

### 7.5 验证

```bash
docker ps
docker logs -f xoassets-server
docker logs -f xoassets-web
docker logs -f xoassets-mysql
docker logs -f xxl-job-admin
```

---

## 8. 单容器重部署流程

### 8.1 只更新后端容器

本地：

```bash
cd /Users/zreo/CODE/XOAssets
docker buildx build --platform linux/amd64 --provenance=false --sbom=false -t xoassets-server:1.0 --load ./xoassets-server
docker image save --platform linux/amd64 -o xoassets-server.tar xoassets-server:1.0
scp xoassets-server.tar root@服务器IP:/data/xoassets/
```

服务器：

```bash
cd /data/xoassets
docker load -i xoassets-server.tar
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate xoassets-server
docker logs -f xoassets-server
```

### 8.2 只更新 Web 容器

本地：

```bash
cd /Users/zreo/CODE/XOAssets
docker buildx build --platform linux/amd64 --provenance=false --sbom=false -t xoassets-web:1.0 --load ./xoassets-web
docker image save --platform linux/amd64 -o xoassets-web.tar xoassets-web:1.0
scp xoassets-web.tar root@服务器IP:/data/xoassets/
```

服务器：

```bash
cd /data/xoassets
docker load -i xoassets-web.tar
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate xoassets-web
docker logs -f xoassets-web
```

### 8.3 只更新 MySQL 容器

适用场景：

- 只改了 MySQL 版本
- 只改了 MySQL 容器配置
- 业务数据没变

建议先备份：

```bash
MYSQL_ROOT_PASSWORD=$(grep '^MYSQL_ROOT_PASSWORD=' /data/xoassets/.env.prod | cut -d= -f2-)
mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" --single-transaction --routines --triggers --events --default-character-set=utf8mb4 xoassets | gzip > xoassets-backup.sql.gz
mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" --single-transaction --routines --triggers --events --default-character-set=utf8mb4 xxl_job | gzip > xxl-job-backup.sql.gz
```

如果服务器可联网，直接拉取新镜像：

```bash
docker pull --platform linux/amd64 mysql:8.0.46
```

如果服务器不能联网，就在本地先 `docker pull`，再 `docker save`，最后上传并 `docker load`。

重启容器：

```bash
cd /data/xoassets
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate xoassets-mysql
docker logs -f xoassets-mysql
```

如果 MySQL 重启后，业务库连接异常，再补重启依赖服务：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate xxl-job-admin xoassets-server
```

### 8.4 只更新 Redis 容器

适用场景：

- 只改了 Redis 镜像版本
- 只改了 Redis 密码或持久化参数

如果服务器可联网，直接拉取新镜像：

```bash
docker pull --platform linux/amd64 redis:7.4.2
```

如果服务器不能联网，就在本地先 `docker pull`，再 `docker save`，最后上传并 `docker load`。

重启容器：

```bash
cd /data/xoassets
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate xoassets-redis
docker logs -f xoassets-redis
```

如果你改了 `REDIS_PASSWORD`，记得同步检查后端里的 `XOASSETS_REDIS_PASSWORD`，再重启 `xoassets-server`。

### 8.5 只更新 XXL-JOB Admin 容器

如果服务器可联网：

```bash
docker pull --platform linux/amd64 xuxueli/xxl-job-admin:3.4.0
```

如果服务器不能联网，本地先 `docker pull`，再 `docker save`，上传后 `docker load`。

重启容器：

```bash
cd /data/xoassets
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate xxl-job-admin
docker logs -f xxl-job-admin
```

如果你改了 `XXL_JOB_ACCESS_TOKEN` 或 Admin 地址，记得同步检查 `xoassets-server` 里的对应环境变量，然后再重启后端。

### 8.6 MySQL 和 XXL-JOB 一起更新

```bash
cd /data/xoassets

# 如果是 tar 包
docker load -i mysql-8.0.46.tar
docker load -i xxl-job-admin-3.4.0.tar

docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate xoassets-mysql
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate xxl-job-admin
```

### 8.7 只改环境变量或 compose 文件

只改 `.env.prod`、端口映射、反向代理配置时，一般不需要重新打包镜像：

```bash
cd /data/xoassets
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --force-recreate xoassets-server
```

如果改的是 MySQL 或 XXL-JOB 的环境变量，再按对应容器重启。

### 8.8 数据库结构变更

如果你改了表结构，流程固定为：

1. 先执行迁移 SQL
2. 再重启后端

示例：

```bash
cd /data/xoassets
MYSQL_ROOT_PASSWORD=$(grep '^MYSQL_ROOT_PASSWORD=' .env.prod | cut -d= -f2-)

docker exec -i xoassets-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" xoassets < ./your-migration.sql
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate xoassets-server
```

---

## 9. 回滚

如果这次更新有问题，直接回滚对应容器的旧镜像即可。

### 回滚后端

```bash
cd /data/xoassets
docker load -i xoassets-server-previous.tar
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate xoassets-server
```

### 回滚 Web

```bash
cd /data/xoassets
docker load -i xoassets-web-previous.tar
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate xoassets-web
```

---

## 10. 常见问题

### 10.1 `exec format error`

镜像架构和服务器架构不一致。  
`x86_64` 服务器用 `linux/amd64`，`arm64` 服务器用 `linux/arm64`。

### 10.2 `mysql:8.0.46` 拉不下来

优先改成服务器直接 `docker pull`。  
如果服务器不能联网，再本地 `docker pull` 后 `docker save`，上传到服务器 `docker load`。

### 10.3 容器起来了但网页打不开

先查：

```bash
docker ps
docker logs -f xoassets-web
```

再检查端口、防火墙和安全组。

### 10.4 数据库名有了，但表为空

先看 `docker logs -f xoassets-mysql`，再检查卷权限和导入过程。  
不要直接删卷。

### 10.5 Redis 连接失败

先看 Redis 容器日志：

```bash
docker logs -f xoassets-redis
```

再检查密码是否一致：

```bash
docker exec -it xoassets-redis redis-cli -a "${REDIS_PASSWORD}" ping
```

如果后端仍然报 Redis 连接失败，再核对 `.env.prod` 里的 `REDIS_PASSWORD` 和 `XOASSETS_REDIS_PASSWORD`。

### 10.6 XXL-JOB 任务调用不到后端

如果调度日志里出现 `UnknownHostException: host.docker.internal`，说明任务还在用本机开发地址。

处理方式：

1. 在 XXL-JOB Admin 里把执行器改成自动注册
2. 或者把任务地址改成 `http://xoassets-server:9999/`
3. 确认 `xxl-job-admin` 和 `xoassets-server` 都已在同一个 Docker Compose 网络中启动

---

## 11. 最短操作顺序

### 首次部署

1. 本地构建 `xoassets-server`、`xoassets-web`
2. 服务器直接 `docker pull` 或本地 `docker save` MySQL、XXL-JOB Admin
3. 导出 `xoassets.sql.gz`、`xxl_job.sql.gz`
4. 上传到 `/data/xoassets`
5. `docker load`
6. 启动 MySQL
7. 导入数据库
8. 启动 `xxl-job-admin`、`xoassets-server`、`xoassets-web`

### 单容器更新

1. 只改哪个容器，就只处理哪个容器对应的镜像
2. `docker load` 或 `docker pull`
3. `docker compose up -d --no-deps --force-recreate <service>`
4. 看日志确认

### 单容器重启对照

- `xoassets-mysql`：数据库和初始化脚本变更时使用
- `xoassets-redis`：Redis 密码、版本、持久化参数变更时使用
- `xxl-job-admin`：调度中心版本或参数变更时使用
- `xoassets-server`：后端代码、配置或环境变量变更时使用
- `xoassets-web`：前端代码或静态资源变更时使用
