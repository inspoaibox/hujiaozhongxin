# 千牛云呼叫中心系统 - 部署指南

**版本：** 1.0.0  
**作者：** 深圳市千牛云科技有限公司

---

## 推荐阅读

如果只是想把系统快速跑起来，不想安装 Java、Node、Maven，也不想手动启动每个服务，优先使用一键 Docker 部署：

```bash
cp .env.example .env
docker compose up -d
```

完整步骤见：`docs/deployment/docker-one-click.md`。

下面内容更适合开发、联调、Kubernetes 生产部署和电话网关配置。

---

## 目录

1. [环境要求](#1-环境要求)
2. [本地开发环境](#2-本地开发环境)
3. [生产环境部署](#3-生产环境部署)
4. [FreeSWITCH 配置](#4-freeswitch-配置)
5. [环境变量说明](#5-环境变量说明)
6. [健康检查](#6-健康检查)
7. [常见问题](#7-常见问题)

---

## 1. 环境要求

### 最低硬件配置（生产环境）

| 节点类型 | CPU | 内存 | 磁盘 | 数量 |
|---------|-----|------|------|------|
| K8s Master | 4 核 | 8 GB | 100 GB SSD | 3 |
| K8s Worker（业务服务） | 8 核 | 16 GB | 200 GB SSD | 3+ |
| K8s Worker（数据库） | 8 核 | 32 GB | 500 GB SSD | 3 |
| FreeSWITCH 服务器 | 8 核 | 16 GB | 200 GB | 2 |
| 备份服务器 | 4 核 | 8 GB | 2 TB | 1 |

### 软件依赖

| 软件 | 版本 | 说明 |
|------|------|------|
| Kubernetes | 1.28+ | 容器编排 |
| Docker | 24.0+ | 容器运行时 |
| Helm | 3.12+ | K8s 包管理 |
| kubectl | 1.28+ | K8s 命令行工具 |
| Java | 17 | 后端运行环境 |
| Node.js | 18+ | 前端构建 |
| FreeSWITCH | 1.10+ | 电话服务器 |

---

## 2. 本地开发环境

### 2.1 一键启动中间件

```bash
cd infrastructure/docker-compose
docker-compose up -d

# 查看启动状态
docker-compose ps

# 查看日志
docker-compose logs -f postgres
```

### 2.2 初始化数据库

```bash
# 等待 PostgreSQL 健康检查通过后执行
docker exec -i qianniu-postgres psql -U qianniu -d qianniu_callcenter \
  < ../../infrastructure/sql/init.sql

# 验证初始化
docker exec -it qianniu-postgres psql -U qianniu -d qianniu_callcenter \
  -c "SELECT COUNT(*) FROM roles;"
```

### 2.3 启动后端服务

```bash
cd backend

# 编译所有模块
mvn clean install -DskipTests

# 按依赖顺序启动服务
# 1. 认证服务（其他服务依赖）
mvn spring-boot:run -pl services/auth-service \
  -Dspring-boot.run.profiles=dev

# 2. 其他服务（可并行启动）
mvn spring-boot:run -pl services/customer-service -Dspring-boot.run.profiles=dev
mvn spring-boot:run -pl services/agent-service -Dspring-boot.run.profiles=dev
mvn spring-boot:run -pl services/call-service -Dspring-boot.run.profiles=dev
# ... 其他服务
```

### 2.4 启动前端

```bash
# 座席工作台
cd frontend/agent-workspace
npm install
npm run dev
# 访问 http://localhost:5173

# 管理后台（新终端）
cd frontend/admin-portal
npm install
npm run dev
# 访问 http://localhost:5174
```

---

## 3. 生产环境部署

### 3.1 准备 K8s 集群

```bash
# 创建命名空间
kubectl apply -f infrastructure/kubernetes/namespaces.yaml

# 创建存储类
kubectl apply -f infrastructure/kubernetes/storage-classes.yaml
```

### 3.2 部署基础设施

```bash
# 部署数据库
kubectl apply -f infrastructure/kubernetes/databases/postgres.yaml
kubectl apply -f infrastructure/kubernetes/databases/mongodb.yaml
kubectl apply -f infrastructure/kubernetes/databases/redis.yaml
kubectl apply -f infrastructure/kubernetes/databases/kafka.yaml
kubectl apply -f infrastructure/kubernetes/databases/elasticsearch.yaml
kubectl apply -f infrastructure/kubernetes/databases/minio.yaml

# 等待数据库就绪
kubectl wait --for=condition=ready pod -l app=postgres \
  -n qianniu-infra --timeout=300s

# 初始化数据库
kubectl exec -it postgres-master-0 -n qianniu-infra -- \
  psql -U qianniu -d qianniu_callcenter -f /docker-entrypoint-initdb.d/init.sql
```

### 3.3 创建 Secrets

```bash
# 数据库密码
kubectl create secret generic db-secret \
  --from-literal=host=postgres-master.qianniu-infra \
  --from-literal=password='your-strong-password' \
  -n qianniu-callcenter

# JWT 密钥
kubectl create secret generic jwt-secret \
  --from-literal=secret='your-jwt-secret-key-min-32-chars' \
  -n qianniu-callcenter

# 录音加密密钥（必须32字节）
kubectl create secret generic recording-secret \
  --from-literal=encryption-key='your-32-byte-encryption-key!!' \
  -n qianniu-callcenter

# MinIO 凭证
kubectl create secret generic minio-secret \
  --from-literal=access-key='qianniu' \
  --from-literal=secret-key='your-minio-password' \
  -n qianniu-callcenter
```

### 3.4 部署监控系统

```bash
kubectl apply -f infrastructure/kubernetes/monitoring/jaeger.yaml

# 使用 Helm 部署 Prometheus + Grafana
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack \
  -n qianniu-monitoring \
  -f infrastructure/monitoring/prometheus-values.yaml
```

### 3.5 构建并推送镜像

```bash
# 设置镜像仓库地址
export REGISTRY=registry.qianniuyun.com
export VERSION=1.0.0

# 构建后端服务
cd backend
mvn clean package -DskipTests

for service in auth-service customer-service agent-service call-service \
               ivr-service recording-service ticket-service quality-service \
               report-service notification-service; do
  docker build -t $REGISTRY/qianniuyun/$service:$VERSION \
    -f services/$service/Dockerfile services/$service/
  docker push $REGISTRY/qianniuyun/$service:$VERSION
done

# 构建前端
for app in agent-workspace admin-portal; do
  cd frontend/$app
  npm ci && npm run build
  docker build -t $REGISTRY/qianniuyun/$app:$VERSION .
  docker push $REGISTRY/qianniuyun/$app:$VERSION
  cd ../..
done
```

### 3.6 部署业务服务

```bash
# 部署所有业务服务
kubectl apply -f infrastructure/kubernetes/services/

# 查看部署状态
kubectl get deployments -n qianniu-callcenter

# 等待所有服务就绪
kubectl wait --for=condition=available deployment \
  --all -n qianniu-callcenter --timeout=600s
```

### 3.7 配置 Nginx

```bash
# 复制 SSL 证书
cp your-cert.crt infrastructure/nginx/ssl/server.crt
cp your-cert.key infrastructure/nginx/ssl/server.key

# 部署 Nginx（K8s Ingress 或独立部署）
kubectl apply -f infrastructure/kubernetes/ingress.yaml
```

### 3.8 验证部署

```bash
# 检查所有 Pod 状态
kubectl get pods -n qianniu-callcenter
kubectl get pods -n qianniu-infra
kubectl get pods -n qianniu-monitoring

# 测试 API 健康检查
curl https://callcenter.qianniuyun.com/api/auth/health

# 测试登录
curl -X POST https://callcenter.qianniuyun.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@2025"}'
```

---

## 4. FreeSWITCH 配置

### 4.1 安装 FreeSWITCH

```bash
# Ubuntu 22.04
apt-get install -y gnupg2 wget lsb-release
wget -O - https://files.freeswitch.org/repo/deb/debian-release/fsstretch-archive-keyring.asc \
  | apt-key add -
echo "deb [signed-by=/usr/share/keyrings/freeswitch-archive-keyring.gpg] \
  https://files.freeswitch.org/repo/deb/debian-release/ $(lsb_release -sc) main" \
  > /etc/apt/sources.list.d/freeswitch.list
apt-get update && apt-get install -y freeswitch
```

### 4.2 配置 ESL（Event Socket Library）

编辑 `/etc/freeswitch/autoload_configs/event_socket.conf.xml`：

```xml
<configuration name="event_socket.conf" description="Socket Client">
  <settings>
    <param name="nat-map" value="false"/>
    <param name="listen-ip" value="0.0.0.0"/>
    <param name="listen-port" value="8021"/>
    <param name="password" value="your-esl-password"/>
    <param name="apply-inbound-acl" value="loopback.auto"/>
  </settings>
</configuration>
```

### 4.3 配置 SIP 网关

编辑 `/etc/freeswitch/sip_profiles/external/pstn-gateway.xml`：

```xml
<include>
  <gateway name="pstn-gateway">
    <param name="username" value="your-sip-username"/>
    <param name="password" value="your-sip-password"/>
    <param name="proxy" value="sip.your-carrier.com"/>
    <param name="register" value="true"/>
    <param name="caller-id-in-from" value="false"/>
    <param name="codec-prefs" value="PCMU,PCMA"/>
  </gateway>
</include>
```

### 4.4 配置呼叫路由

编辑 `/etc/freeswitch/dialplan/default.xml`，添加呼入路由：

```xml
<extension name="inbound-calls">
  <condition field="destination_number" expression="^4008001234$">
    <action application="set" data="call_timeout=30"/>
    <action application="answer"/>
    <!-- 通知 call-service -->
    <action application="socket" data="call-service:8080 async full"/>
  </condition>
</extension>
```

---

## 5. 环境变量说明

### 后端服务公共环境变量

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `DB_HOST` | PostgreSQL 主机 | `postgres-master.qianniu-infra` |
| `DB_PORT` | PostgreSQL 端口 | `5432` |
| `DB_NAME` | 数据库名 | `qianniu_callcenter` |
| `DB_USER` | 数据库用户 | `qianniu` |
| `DB_PASSWORD` | 数据库密码 | `your-password` |
| `REDIS_HOST` | Redis 主机 | `redis-master.qianniu-infra` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | `your-password` |
| `KAFKA_SERVERS` | Kafka 地址 | `kafka.qianniu-infra:9092` |
| `MINIO_ENDPOINT` | MinIO 地址 | `http://minio.qianniu-infra:9000` |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥 | `qianniu` |
| `MINIO_SECRET_KEY` | MinIO 密钥 | `your-password` |
| `JWT_SECRET` | JWT 签名密钥（≥32字符） | `your-jwt-secret-key` |

### call-service 专用环境变量

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `FREESWITCH_HOST` | FreeSWITCH 主机 | `freeswitch-1` |
| `FREESWITCH_PORT` | ESL 端口 | `8021` |
| `FREESWITCH_PASSWORD` | ESL 密码 | `your-esl-password` |

### recording-service 专用环境变量

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `RECORDING_ENCRYPTION_KEY` | 录音加密密钥（必须32字节） | `your-32-byte-key!!!!!!!!!!!!!!` |
| `RECORDING_RETENTION_DAYS` | 录音保留天数 | `90` |

---

## 6. 健康检查

所有 Spring Boot 服务均暴露以下健康检查端点：

```bash
# 存活检查
GET /actuator/health/liveness

# 就绪检查
GET /actuator/health/readiness

# 完整健康信息
GET /actuator/health

# Prometheus 指标
GET /actuator/prometheus
```

### 批量检查所有服务

```bash
for service in auth-service customer-service agent-service call-service \
               ivr-service recording-service ticket-service quality-service \
               report-service notification-service; do
  echo -n "$service: "
  kubectl exec -n qianniu-callcenter \
    $(kubectl get pod -l app=$service -n qianniu-callcenter -o name | head -1) \
    -- curl -s http://localhost:8080/actuator/health | python3 -c \
    "import sys,json; d=json.load(sys.stdin); print(d['status'])"
done
```

---

## 7. 常见问题

### Q: 服务启动后无法连接数据库

**检查步骤：**
```bash
# 1. 确认 PostgreSQL Pod 状态
kubectl get pod -l app=postgres -n qianniu-infra

# 2. 测试连接
kubectl exec -it postgres-master-0 -n qianniu-infra -- \
  psql -U qianniu -d qianniu_callcenter -c "SELECT 1"

# 3. 检查 Secret 是否正确
kubectl get secret db-secret -n qianniu-callcenter -o yaml
```

### Q: FreeSWITCH 无法接收呼叫

**检查步骤：**
```bash
# 1. 检查 FreeSWITCH 状态
fs_cli -x "status"

# 2. 检查 SIP 网关注册状态
fs_cli -x "sofia status gateway pstn-gateway"

# 3. 检查 ESL 连接
telnet freeswitch-host 8021
```

### Q: 录音文件无法播放

**检查步骤：**
```bash
# 1. 确认 MinIO 中文件存在
mc ls qianniu/qianniu-recordings/

# 2. 检查 recording-service 日志
kubectl logs -f deployment/recording-service -n qianniu-callcenter

# 3. 验证加密密钥是否一致（加密和解密使用同一密钥）
kubectl get secret recording-secret -n qianniu-callcenter -o jsonpath='{.data.encryption-key}' | base64 -d
```

### Q: 呼叫分配延迟过高

**检查步骤：**
```bash
# 1. 检查 Redis 队列状态
kubectl exec -it redis-master-0 -n qianniu-infra -- \
  redis-cli -a your-password ZRANGE call:queue:GENERAL 0 -1 WITHSCORES

# 2. 检查 Kafka 消费延迟
kubectl exec -it kafka-0 -n qianniu-infra -- \
  kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group call-distribution

# 3. 检查分配引擎日志
kubectl logs -f deployment/call-distribution-engine -n qianniu-callcenter
```
