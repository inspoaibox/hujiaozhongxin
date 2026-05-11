# 千牛云呼叫中心一键 Docker 部署指南

这份文档给第一次部署的人使用。目标是：服务器只安装 Docker，不安装 Java、Node、Maven，也不在服务器上编译代码。

## 1. 适合谁使用

适合以下场景：

- 想快速把系统跑起来，先体验座席工作台和管理后台。
- 已经使用 GitHub Actions 构建镜像，服务器只负责拉镜像和启动容器。
- 不想手动启动十几个后端服务和前端服务。

如果你是正式生产环境，还需要后续补充域名、HTTPS、备份、日志、监控、电话网关和安全加固。

## 2. 部署前准备

服务器最低建议：

| 项目 | 建议 |
|------|------|
| CPU | 4 核以上 |
| 内存 | 8 GB 以上，推荐 16 GB |
| 磁盘 | 50 GB 以上 |
| 系统 | Ubuntu / Debian / CentOS / Windows Server 均可 |
| 软件 | Docker 24+，Docker Compose 2+ |

检查 Docker 是否可用：

```bash
docker version
docker compose version
```

只要这两个命令能看到版本号，就可以继续。

## 3. 第一次一键启动

在服务器进入项目根目录，也就是能看到 `docker-compose.yml` 的目录。

### 第一步：复制配置文件

```bash
cp .env.example .env
```

Windows PowerShell 使用：

```powershell
Copy-Item .env.example .env
```

### 第二步：修改镜像命名空间

打开 `.env`，找到：

```env
IMAGE_NAMESPACE=inspoaibox
```

当前仓库是：

```text
https://github.com/inspoaibox/hujiaozhongxin
```

所以默认镜像命名空间就是 `inspoaibox`。如果以后项目迁移到其他 GitHub 账号或组织下，再把它改成新的 GitHub 用户名或组织名。例如：

```env
IMAGE_NAMESPACE=my-github-name
```

镜像完整地址规则是：

```text
ghcr.io/inspoaibox/qianniu/服务名:latest
```

### 第三步：如果 GHCR 镜像是私有的，先登录

公开镜像可以跳过这一步。

```bash
echo 你的GitHubToken | docker login ghcr.io -u 你的GitHub用户名 --password-stdin
```

GitHub Token 至少需要 `read:packages` 权限。

### 第四步：一键启动

最省事的方式是直接运行项目内置脚本。

Linux / macOS：

```bash
bash scripts/docker-up.sh
```

Windows PowerShell：

```powershell
.\scripts\docker-up.ps1
```

脚本会自动复制 `.env`、拉取镜像、启动服务并打印访问地址。

也可以不用脚本，直接执行：

```bash
docker compose up -d
```

第一次会下载很多镜像，时间取决于服务器网络。一般几分钟到十几分钟。

### 第五步：查看是否启动成功

```bash
docker compose ps
```

看到大部分服务是 `running` 或 `healthy`，说明启动正常。

## 4. 访问地址

假设服务器 IP 是 `1.2.3.4`，默认访问地址如下：

| 页面 | 地址 | 默认账号 |
|------|------|----------|
| 座席工作台 | `http://1.2.3.4:5173` | `admin / Admin@2025` |
| 管理后台 | `http://1.2.3.4:5174` | `admin / Admin@2025` |
| API 网关 | `http://1.2.3.4:8888` | 不直接登录 |
| WebSocket 网关 | `ws://1.2.3.4:8889/ws/agent` | 前端自动连接 |
| Nacos 控制台 | `http://1.2.3.4:8848/nacos` | 默认未开启鉴权 |
| MinIO 控制台 | `http://1.2.3.4:9001` | `qianniu / qianniu@2025` |

正式上线前必须修改默认密码。

## 5. 最常用命令

启动：

```bash
docker compose up -d
```

停止：

```bash
docker compose down
```

重启：

```bash
docker compose restart
```

查看状态：

```bash
docker compose ps
```

查看所有日志：

```bash
docker compose logs -f
```

查看某个服务日志，例如认证服务：

```bash
docker compose logs -f auth-service
```

更新到 GitHub 最新镜像：

```bash
docker compose pull
docker compose up -d
```

## 6. 修改端口

打开 `.env`，可以修改这些端口：

```env
AGENT_WORKSPACE_PORT=5173
ADMIN_PORTAL_PORT=5174
API_GATEWAY_PORT=8888
WS_GATEWAY_PORT=8889
MINIO_CONSOLE_PORT=9001
```

例如服务器 5173 被占用，可以改成：

```env
AGENT_WORKSPACE_PORT=18080
```

然后重启：

```bash
docker compose up -d
```

新的座席工作台地址就是：

```text
http://服务器IP:18080
```

## 7. 修改默认账号密码

打开 `.env`：

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=Admin@2025
```

改成自己的密码，例如：

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=MyStrongPassword@2026
```

然后重启认证服务：

```bash
docker compose up -d auth-service
```

注意：如果数据库里已经生成过管理员，只有首次初始化或密码为占位值时会自动写入默认密码。正式系统建议登录后走“修改密码”流程。

## 8. 数据保存在哪里

一键部署使用 Docker named volumes 保存数据，重启容器不会丢。

主要数据卷：

| 数据卷 | 用途 |
|--------|------|
| `auth_data` | 认证服务演示库 |
| `customer_data` | 客户服务演示库 |
| `agent_data` | 座席服务演示库 |
| `call_data` | 呼叫服务演示库 |
| `redis_data` | Redis 数据 |
| `kafka_data` | Kafka 数据 |
| `minio_data` | MinIO 文件 |

只停止服务不会删除数据：

```bash
docker compose down
```

如果执行下面命令，会删除数据，请谨慎：

```bash
docker compose down -v
```

## 9. 当前一键部署说明

当前一键部署的目标是“快速体验和演示”，不是最终生产高可用架构。

当前特点：

- 后端服务镜像由 GitHub Actions 构建，服务器只拉镜像。
- 前端容器已经内置 `/api` 和 `/ws` 反向代理，浏览器访问前端即可调用后端。
- 业务服务默认使用各自的 H2 文件库，数据保存在 Docker volume 中。
- Redis、Kafka、Nacos、MinIO 会随 Compose 一起启动。

生产环境建议：

- 改用 PostgreSQL、MongoDB、Redis、Kafka 的独立高可用实例。
- 配置 HTTPS 域名和外层 Nginx。
- 修改所有默认密码和 JWT 密钥。
- 配置备份策略。
- 接入 FreeSWITCH 和运营商 SIP 线路。
- 加入 Prometheus、Grafana、日志采集和告警。

## 10. 常见问题

### Q1：`docker compose up -d` 提示镜像不存在？

先确认 GitHub Actions 是否已经构建并推送镜像。然后检查 `.env`：

```env
IMAGE_NAMESPACE=inspoaibox
IMAGE_TAG=latest
```

如果镜像是私有的，还要先执行 `docker login ghcr.io`。

### Q2：页面能打开，但是登录失败？

先看认证服务和网关日志：

```bash
docker compose logs -f auth-service api-gateway
```

再确认 `.env` 里的默认账号密码是否和页面输入一致。

### Q3：页面打开后接口 404 或连接失败？

检查前端容器和网关是否都在运行：

```bash
docker compose ps agent-workspace admin-portal api-gateway ws-gateway
```

如果网关没启动，查看日志：

```bash
docker compose logs -f api-gateway ws-gateway
```

### Q4：服务器内存不够怎么办？

可以先停掉暂时不用的服务，例如录音、质检、通知：

```bash
docker compose stop recording-service quality-service notification-service
```

体验登录、座席工作台和管理后台，一般保留网关、认证、呼叫、座席、报表、Nacos、Redis、Kafka 即可。

### Q5：怎么完全重新部署？

保留数据重新拉镜像：

```bash
docker compose pull
docker compose up -d
```

删除全部数据重新开始：

```bash
docker compose down -v
docker compose up -d
```
