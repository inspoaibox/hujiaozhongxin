# GitHub Actions CI/CD 使用指南

**作者：** 深圳市千牛云科技有限公司

---

## 工作流说明

项目包含两个 GitHub Actions 工作流：

| 工作流文件 | 触发条件 | 作用 |
|-----------|---------|------|
| `build-and-push.yml` | push 到 main/release/tag | 构建所有镜像并推送到 GHCR |
| `pr-check.yml` | PR 到 main/develop | 编译+测试+代码检查（不推送镜像） |

---

## 镜像仓库

使用 **GitHub Container Registry (GHCR)**，地址格式：

```
ghcr.io/{你的GitHub用户名}/qianniu/{服务名}:{标签}
```

例如：
```
ghcr.io/qianniuyun/qianniu/call-service:latest
ghcr.io/qianniuyun/qianniu/agent-workspace:v1.0.0
```

**优点：**
- 完全免费（公开仓库）
- 与 GitHub 仓库权限集成
- 无需额外配置密钥（使用内置 `GITHUB_TOKEN`）

---

## 镜像标签规则

| 触发方式 | 生成的标签 |
|---------|-----------|
| push 到 `main` | `latest` + `sha-xxxxxxx` |
| push 到 `release/1.2.0` | `1.2.0` + `sha-xxxxxxx` |
| 推送 tag `v1.2.0` | `1.2.0` + `1.2` + `sha-xxxxxxx` |

---

## 首次使用配置

### 1. 开启 GHCR 写入权限

进入仓库 → **Settings** → **Actions** → **General** → 找到 **Workflow permissions**，选择 **Read and write permissions**，保存。

### 2. 设置镜像可见性（可选）

镜像默认为私有。如需公开：
进入 **Packages** → 找到对应镜像 → **Package settings** → **Change visibility** → Public

---

## 本地拉取镜像

```bash
# 登录 GHCR
echo $GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# 拉取镜像
docker pull ghcr.io/qianniuyun/qianniu/call-service:latest

# 运行单个服务（测试用）
docker run -d \
  -p 8080:8080 \
  -e DB_HOST=your-db-host \
  -e REDIS_HOST=your-redis-host \
  -e KAFKA_SERVERS=your-kafka:9092 \
  ghcr.io/qianniuyun/qianniu/call-service:latest
```

---

## 使用 docker-compose 拉取并运行

```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  call-service:
    image: ghcr.io/qianniuyun/qianniu/call-service:latest
    ports:
      - "8080:8080"
    environment:
      DB_HOST: postgres
      REDIS_HOST: redis
      KAFKA_SERVERS: kafka:9092
    restart: unless-stopped
```

```bash
# 登录后拉取并启动
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

---

## 构建流程图

```
代码推送到 main
       │
       ▼
┌─────────────────────────────────────────┐
│  Job 1: 后端编译与测试（Maven）           │
│  - mvn clean verify                     │
│  - 上传 JAR 产物                         │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│  Job 2: 前端构建（Node.js）              │
│  - npm ci && npm run build              │
│  - 上传 dist/ 产物                       │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│  Job 3: 并行构建 15 个 Docker 镜像       │
│  （矩阵策略，同时构建所有服务）           │
│                                         │
│  每个服务：                              │
│  1. 下载对应构建产物                     │
│  2. docker buildx build（多平台）        │
│  3. 推送到 ghcr.io                      │
│  4. 利用 GitHub Actions 缓存加速         │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│  Job 4: 发送完成通知                     │
└─────────────────────────────────────────┘
```

---

## 常见问题

**Q: 构建失败，提示权限不足？**

检查 Settings → Actions → General → Workflow permissions 是否设置为 Read and write。

**Q: 如何只重新构建某个服务？**

目前工作流会构建所有服务。如需单独构建，可以手动触发：
Actions → 选择工作流 → Run workflow。

**Q: 构建时间太长？**

首次构建约 15-20 分钟（需要下载依赖）。后续构建因为有 Maven 缓存、npm 缓存和 Docker 层缓存，通常在 5-8 分钟内完成。

**Q: 如何在服务器上自动更新镜像？**

可以配合 [Watchtower](https://containrrr.dev/watchtower/) 实现自动拉取最新镜像：

```bash
docker run -d \
  --name watchtower \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e REPO_USER=your-github-username \
  -e REPO_PASS=your-github-token \
  containrrr/watchtower \
  --interval 300  # 每5分钟检查一次
```
