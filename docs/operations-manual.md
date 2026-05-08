# 千牛云呼叫中心系统 - 运维手册

**版本：** 1.0.0  
**作者：** 深圳市千牛云科技有限公司

---

## 目录

1. [系统架构概览](#1-系统架构概览)
2. [日常运维操作](#2-日常运维操作)
3. [监控与告警](#3-监控与告警)
4. [数据库运维](#4-数据库运维)
5. [故障处理手册](#5-故障处理手册)
6. [数据备份与恢复](#6-数据备份与恢复)
7. [扩容操作](#7-扩容操作)
8. [日志查询](#8-日志查询)

---

## 1. 系统架构概览

系统采用微服务架构，部署在 Kubernetes 集群上，包含以下核心组件：

### 业务服务

| 服务 | 命名空间 | 副本数 | 说明 |
|------|---------|--------|------|
| api-gateway | qianniu-callcenter | 2 | API 网关 |
| ws-gateway | qianniu-callcenter | 2 | WebSocket 网关 |
| auth-service | qianniu-callcenter | 2 | 认证授权 |
| customer-service | qianniu-callcenter | 2 | 客户管理 |
| agent-service | qianniu-callcenter | 3 | 座席管理 |
| call-service | qianniu-callcenter | 3 | 呼叫管理 |
| ivr-service | qianniu-callcenter | 2 | IVR 服务 |
| recording-service | qianniu-callcenter | 2 | 录音服务 |
| ticket-service | qianniu-callcenter | 2 | 工单服务 |
| quality-service | qianniu-callcenter | 2 | 质检服务 |
| report-service | qianniu-callcenter | 2 | 报表服务 |
| notification-service | qianniu-callcenter | 2 | 通知服务 |
| call-distribution-engine | qianniu-callcenter | 2 | 呼叫分配引擎 |

### 基础设施

| 组件 | 命名空间 | 说明 |
|------|---------|------|
| PostgreSQL（1主2从） | qianniu-infra | 主业务数据库 |
| MongoDB（3节点） | qianniu-infra | 日志/录音元数据 |
| Redis（Sentinel） | qianniu-infra | 缓存/状态管理 |
| Kafka（3 Broker） | qianniu-infra | 消息总线 |
| Elasticsearch（3节点） | qianniu-infra | 全文检索 |
| MinIO | qianniu-infra | 录音文件存储 |
| Prometheus + Grafana | qianniu-monitoring | 监控 |
| Jaeger | qianniu-monitoring | 链路追踪 |

---

## 2. 日常运维操作

### 查看服务状态

```bash
# 查看所有业务服务 Pod
kubectl get pods -n qianniu-callcenter

# 查看基础设施 Pod
kubectl get pods -n qianniu-infra

# 查看服务详情
kubectl describe deployment call-service -n qianniu-callcenter
```

### 查看服务日志

```bash
# 实时查看日志
kubectl logs -f deployment/call-service -n qianniu-callcenter

# 查看最近 100 行
kubectl logs deployment/agent-service -n qianniu-callcenter --tail=100

# 查看特定 Pod 日志
kubectl logs call-service-7d9f8b-xxxxx -n qianniu-callcenter

# 查看错误日志
kubectl logs deployment/call-service -n qianniu-callcenter | grep ERROR
```

### 重启服务

```bash
# 滚动重启（不中断服务）
kubectl rollout restart deployment/call-service -n qianniu-callcenter

# 查看重启进度
kubectl rollout status deployment/call-service -n qianniu-callcenter

# 回滚到上一版本
kubectl rollout undo deployment/call-service -n qianniu-callcenter
```

### 更新服务镜像

```bash
# 更新单个服务
kubectl set image deployment/call-service \
  call-service=registry.qianniuyun.com/qianniuyun/call-service:1.0.1 \
  -n qianniu-callcenter

# 查看更新状态
kubectl rollout status deployment/call-service -n qianniu-callcenter
```

---

## 3. 监控与告警

### 监控面板访问

| 系统 | 地址 | 说明 |
|------|------|------|
| Grafana | http://grafana.qianniuyun.com | 监控面板（admin/qianniu@2025） |
| Prometheus | http://prometheus.qianniuyun.com | 指标查询 |
| Jaeger | http://jaeger.qianniuyun.com | 链路追踪 |
| Kibana | http://kibana.qianniuyun.com | 日志查询 |

### 关键告警规则

| 告警名称 | 触发条件 | 严重级别 | 处理建议 |
|---------|---------|---------|---------|
| ServiceDown | 服务不可用超过 1 分钟 | 严重 | 立即检查 Pod 状态和日志 |
| CallQueueOverload | 队列等待超过 10 个 | 警告 | 增加座席或扩容服务 |
| AllAgentsBusy | 所有座席忙碌超过 2 分钟 | 警告 | 通知主管调配座席 |
| PostgresConnectionHigh | 连接数超过 80 | 警告 | 检查连接池配置 |
| RedisMemoryHigh | 内存使用率超过 85% | 警告 | 清理过期 Key 或扩容 |
| KafkaConsumerLag | 消费延迟超过 1000 条 | 警告 | 检查消费者服务状态 |
| JvmMemoryHigh | JVM 堆内存超过 85% | 警告 | 检查内存泄漏或扩容 |

---

## 4. 数据库运维

### PostgreSQL 常用操作

```bash
# 连接主库
kubectl exec -it postgres-master-0 -n qianniu-infra -- \
  psql -U qianniu -d qianniu_callcenter

# 查看活跃连接数
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';

# 查看慢查询（超过 1 秒）
SELECT pid, now() - pg_stat_activity.query_start AS duration, query
FROM pg_stat_activity
WHERE (now() - pg_stat_activity.query_start) > interval '1 seconds'
  AND state = 'active';

# 查看表大小
SELECT relname, pg_size_pretty(pg_total_relation_size(relid))
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC;
```

### Redis 常用操作

```bash
# 连接 Redis
kubectl exec -it redis-master-0 -n qianniu-infra -- \
  redis-cli -a your-password

# 查看内存使用
INFO memory

# 查看呼叫队列
ZRANGE call:queue:GENERAL 0 -1 WITHSCORES

# 查看座席状态缓存
KEYS agent:status:*

# 清理过期 Key（谨慎操作）
DEBUG SLEEP 0
```

---

## 5. 故障处理手册

### 5.1 服务不可用

**症状：** 某个服务的 Pod 处于 CrashLoopBackOff 或 Error 状态

**处理步骤：**
```bash
# 1. 查看 Pod 状态
kubectl get pods -n qianniu-callcenter | grep -v Running

# 2. 查看 Pod 事件
kubectl describe pod <pod-name> -n qianniu-callcenter

# 3. 查看容器日志
kubectl logs <pod-name> -n qianniu-callcenter --previous

# 4. 常见原因和解决方案：
# - OOMKilled：内存不足，增加内存限制或优化代码
# - ImagePullBackOff：镜像拉取失败，检查镜像仓库和网络
# - CrashLoopBackOff：应用启动失败，查看日志排查原因
```

### 5.2 呼叫分配异常

**症状：** 呼叫进入队列后长时间未分配给座席

**处理步骤：**
```bash
# 1. 检查队列状态
kubectl exec -it redis-master-0 -n qianniu-infra -- \
  redis-cli -a your-password ZRANGE call:queue:GENERAL 0 -1

# 2. 检查分配引擎状态
kubectl get pods -l app=call-distribution-engine -n qianniu-callcenter

# 3. 检查 Kafka 消费延迟
kubectl exec -it kafka-0 -n qianniu-infra -- \
  kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group call-distribution

# 4. 重启分配引擎
kubectl rollout restart deployment/call-distribution-engine -n qianniu-callcenter
```

### 5.3 FreeSWITCH 连接断开

**症状：** 呼叫无法接入，call-service 日志显示 ESL 连接失败

**处理步骤：**
```bash
# 1. 检查 FreeSWITCH 服务状态
ssh freeswitch-server
fs_cli -x "status"

# 2. 检查 ESL 端口
netstat -tlnp | grep 8021

# 3. 重启 FreeSWITCH
systemctl restart freeswitch

# 4. 重启 call-service 重新建立连接
kubectl rollout restart deployment/call-service -n qianniu-callcenter
```

### 5.4 录音文件无法播放

**症状：** 点击播放按钮无响应或报错

**处理步骤：**
```bash
# 1. 检查 MinIO 状态
kubectl get pods -l app=minio -n qianniu-infra

# 2. 验证文件存在
kubectl exec -it minio-xxx -n qianniu-infra -- \
  mc ls local/qianniu-recordings/ | head -20

# 3. 检查 recording-service 日志
kubectl logs deployment/recording-service -n qianniu-callcenter | grep ERROR

# 4. 验证加密密钥配置
kubectl get secret recording-secret -n qianniu-callcenter \
  -o jsonpath='{.data.encryption-key}' | base64 -d | wc -c
# 应输出 32（32字节密钥）
```

---

## 6. 数据备份与恢复

### 手动备份 PostgreSQL

```bash
# 全量备份
kubectl exec -it postgres-master-0 -n qianniu-infra -- \
  pg_dump -U qianniu qianniu_callcenter \
  | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz

# 验证备份文件
gzip -t backup_*.sql.gz && echo "备份文件完整"
```

### 恢复 PostgreSQL

```bash
# 停止写入（可选，减少数据不一致风险）
# 恢复数据
gunzip -c backup_20250101_020000.sql.gz | \
  kubectl exec -i postgres-master-0 -n qianniu-infra -- \
  psql -U qianniu -d qianniu_callcenter

# 验证恢复
kubectl exec -it postgres-master-0 -n qianniu-infra -- \
  psql -U qianniu -d qianniu_callcenter \
  -c "SELECT COUNT(*) FROM calls;"
```

### 自动备份状态检查

```bash
# 查看备份任务日志（每日凌晨2点执行）
kubectl logs -l app=backup-job -n qianniu-infra --since=24h
```

---

## 7. 扩容操作

### 手动扩容服务

```bash
# 扩容 call-service 到 5 个副本
kubectl scale deployment/call-service --replicas=5 -n qianniu-callcenter

# 查看扩容进度
kubectl get pods -l app=call-service -n qianniu-callcenter -w
```

### 调整 HPA 配置

```bash
# 查看当前 HPA 状态
kubectl get hpa -n qianniu-callcenter

# 修改最大副本数
kubectl patch hpa call-service-hpa -n qianniu-callcenter \
  --patch '{"spec":{"maxReplicas":15}}'
```

---

## 8. 日志查询

### 通过 Kibana 查询

1. 访问 http://kibana.qianniuyun.com
2. 进入「Discover」
3. 选择索引模式 `qianniu-logs-*`
4. 常用查询示例：

```
# 查询特定呼叫的所有日志
callId: "550e8400-e29b-41d4-a716-446655440000"

# 查询错误日志
log_level: ERROR AND service_name: call-service

# 查询特定座席的操作日志
agentId: 5 AND @timestamp: [now-1h TO now]
```

### 通过 kubectl 查询

```bash
# 查询包含特定关键词的日志
kubectl logs deployment/call-service -n qianniu-callcenter \
  --since=1h | grep "callId=550e8400"

# 查询所有服务的错误日志
for svc in call-service agent-service ticket-service; do
  echo "=== $svc ==="
  kubectl logs deployment/$svc -n qianniu-callcenter \
    --since=1h | grep ERROR | tail -5
done
```

