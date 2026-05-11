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

## 3. GitHub 构建和服务器启动的关系

这个项目推荐使用“GitHub 构建，服务器运行”的方式。

简单理解：

```text
本地改代码
  -> 推送到 GitHub main 分支
  -> GitHub Actions 自动编译后端、编译前端、构建 Docker 镜像
  -> 镜像推送到 GHCR：ghcr.io/inspoaibox/qianniu/服务名:latest
  -> 服务器执行 git pull + docker compose pull
  -> 服务器用新镜像重启容器
```

服务器不需要安装 Java、Node、Maven，也不在服务器上编译代码。服务器只需要：

- 拉最新仓库代码，拿到最新的 `docker-compose.yml`、`.env.example`、Nginx 配置和启动脚本
- 拉 GitHub Actions 已经构建好的 Docker 镜像
- 用 Docker Compose 启动或重启容器

注意：只执行 `git pull` 不会更新正在运行的程序。因为真正运行的是 Docker 镜像，还必须执行 `docker compose pull` 和重建容器。

## 4. 第一次一键启动

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

### 第三步：确认 GitHub Actions 已经构建完成

打开 GitHub 仓库：

```text
https://github.com/inspoaibox/hujiaozhongxin/actions
```

找到工作流：

```text
构建并推送 Docker 镜像
```

确认最新一次运行是绿色成功状态。只有 Actions 成功后，服务器才能拉到最新镜像。

如果 Actions 还在运行或失败，服务器执行 `docker compose pull` 也拉不到新版本。

### 第四步：如果 GHCR 镜像是私有的，先登录

公开镜像可以跳过这一步。

```bash
echo 你的GitHubToken | docker login ghcr.io -u 你的GitHub用户名 --password-stdin
```

GitHub Token 至少需要 `read:packages` 权限。

### 第五步：一键启动

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

脚本支持两种启动模式：

- `lite`（默认）：轻量模式，只启动页面和登录必需服务（推荐先用它验证 5173/5174 能打开）
- `full`：完整模式，会启动全部中间件和全部微服务（需要更高硬件配置）

示例：

```bash
# 轻量（默认）
bash scripts/docker-up.sh

# 完整
bash scripts/docker-up.sh full
```

Windows：

```powershell
.\scripts\docker-up.ps1
.\scripts\docker-up.ps1 full
```

也可以不用脚本，直接执行下面命令启动完整模式（小服务器不推荐）：

```bash
docker compose up -d
```

第一次会下载很多镜像，时间取决于服务器网络。一般几分钟到十几分钟。

### 第六步：查看是否启动成功

```bash
docker compose ps
```

看到大部分服务是 `running` 或 `healthy`，说明启动正常。

## 5. 访问地址

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

## 6. 最常用命令

轻量启动（推荐先用这个验证页面和登录）：

```bash
bash scripts/docker-up.sh
```

完整启动（会启动全部中间件和全部微服务，需要更高配置）：

```bash
bash scripts/docker-up.sh full
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

## 7. 更新代码和镜像

当你在本地改完代码，或者我帮你改完代码后，完整更新流程是下面这样。

### 第一步：把代码推送到 GitHub

在本地电脑项目目录执行：

```bash
git status
git add .
git commit -m "更新部署和功能修复"
git push origin main
```

如果你直接在 GitHub 网页上编辑文件，保存提交到 `main` 分支即可。

### 第二步：等待 GitHub Actions 构建镜像

打开：

```text
https://github.com/inspoaibox/hujiaozhongxin/actions
```

等待最新的 `构建并推送 Docker 镜像` 变成绿色成功。

这一步会做这些事：

- 编译后端 Java 服务
- 校验后端 JAR 是可执行 Spring Boot JAR
- 编译两个前端页面
- 构建所有 Docker 镜像
- 推送镜像到 `ghcr.io/inspoaibox/qianniu/...`

### 第三步：服务器拉最新代码

在服务器执行：

```bash
cd ~/hujiaozhongxin
git pull
```

这一步只更新服务器上的配置文件、脚本和 compose 文件，不会自动更新容器。

### 第四步：服务器拉最新镜像并启动

推荐先用轻量模式：

```bash
bash scripts/docker-up.sh
```

脚本会自动：

- 拉取核心服务最新镜像
- 停掉完整模式才需要的重型服务
- 等待 Nacos 和 Redis 就绪
- 启动认证服务、API 网关、座席工作台和管理后台

如果你确定服务器资源足够，并且要启动完整系统：

```bash
bash scripts/docker-up.sh full
```

不用脚本时，可以手动执行：

```bash
docker compose pull
docker compose up -d --force-recreate
```

### 第五步：确认更新成功

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep qianniu

curl -i http://127.0.0.1:5173/health
curl -i http://127.0.0.1:5174/health
curl -i http://127.0.0.1:8888/actuator/health
```

两个前端健康检查应该返回：

```text
OK
```

如果登录失败，再看后端日志：

```bash
docker logs --tail=120 qianniu-auth-service
docker logs --tail=120 qianniu-api-gateway
```

## 8. 修改端口

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
bash scripts/docker-up.sh
```

新的座席工作台地址就是：

```text
http://服务器IP:18080
```

## 9. 修改默认账号密码

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

## 10. 数据保存在哪里

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

## 11. 当前一键部署说明

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

## 12. 常见问题

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

### Q4：5173 或 5174 提示“该网页无法正常运作”？

先看前端容器是否在反复重启：

```bash
docker compose ps agent-workspace admin-portal
```

再看前端 Nginx 日志：

```bash
docker compose logs --tail=100 agent-workspace
docker compose logs --tail=100 admin-portal
```

如果日志里出现 `host not found in upstream "api-gateway"` 或 `host not found in upstream "ws-gateway"`，说明前端镜像使用的是旧版 Nginx 配置。更新镜像后重启：

```bash
docker compose pull agent-workspace admin-portal
docker compose up -d agent-workspace admin-portal
```

如果 `api-gateway` 或 `auth-service` 日志里出现 `no main manifest attribute, in app.jar`，说明 GitHub Actions 生成的后端 JAR 不是可执行 Spring Boot JAR。需要先把最新代码推送到 GitHub，等待 Actions 重新构建并推送镜像，然后服务器重新拉镜像：

```bash
docker compose pull api-gateway auth-service
docker compose up -d --force-recreate api-gateway auth-service
```

如果 `/health` 都打不开，说明问题在前端容器或端口映射，不是账号密码或页面功能：

```bash
curl http://服务器IP:5173/health
curl http://服务器IP:5174/health
```

### Q5：服务器内存不够怎么办？

如果你发现执行 `docker compose ps` / `docker compose logs` 都会明显卡顿、或者 5173/5174 打不开，通常是服务器资源不足导致 Docker daemon 负载过高。

推荐处理顺序：

1) 先用轻量模式启动（只启动页面和登录必需服务）：

```bash
bash scripts/docker-up.sh
```

2) 如果之前已经用完整模式启动过，先停掉重型服务（Kafka / MinIO / 录音 / 质检 / 通知）再观察：

```bash
docker compose stop kafka zookeeper minio recording-service quality-service notification-service
```

3) 仍然卡死时，优先检查服务器剩余内存和磁盘（内存不足会导致 OOM，磁盘满会导致容器写日志卡死）：

```bash
free -h
df -h
```

也可以先停掉暂时不用的服务，例如录音、质检、通知：

```bash
docker compose stop recording-service quality-service notification-service
```

体验登录、座席工作台和管理后台，一般保留网关、认证、呼叫、座席、报表、Nacos、Redis、Kafka 即可。

### Q6：怎么完全重新部署？

保留数据重新拉镜像：

```bash
git pull
bash scripts/docker-up.sh
```

删除全部数据重新开始：

```bash
docker compose down -v
bash scripts/docker-up.sh
```
