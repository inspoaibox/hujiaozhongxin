# 千牛云呼叫中心系统 - 架构设计文档

**版本：** 1.0.0  
**作者：** 深圳市千牛云科技有限公司  
**更新日期：** 2025年

---

## 目录

1. [设计目标](#1-设计目标)
2. [整体架构](#2-整体架构)
3. [微服务划分](#3-微服务划分)
4. [数据架构](#4-数据架构)
5. [通信机制](#5-通信机制)
6. [安全架构](#6-安全架构)
7. [高可用设计](#7-高可用设计)
8. [核心流程](#8-核心流程)

---

## 1. 设计目标

| 目标 | 指标 |
|------|------|
| 高可用性 | 系统可用性 ≥ 99.9%，支持故障自动切换 |
| 高并发 | 支持 1000+ 并发呼叫，10000+ 在线座席 |
| 实时性 | 呼叫分配延迟 < 2 秒，状态更新延迟 < 1 秒 |
| 可扩展性 | 支持水平扩展，模块化设计 |
| 安全性 | 数据加密传输和存储，完善的权限控制 |
| 可维护性 | 清晰的模块划分，完善的日志和监控 |

---

## 2. 整体架构

### 2.1 分层架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                          客户端层                                │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │  座席工作台   │  │   管理后台   │  │  第三方系统(API集成)  │  │
│  │  Vue3+WebRTC │  │  Vue3+ECharts│  │  CRM / ERP / 其他    │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │
└─────────┼─────────────────┼──────────────────────┼─────────────┘
          │ HTTPS            │ HTTPS                │ HTTPS/API Key
┌─────────▼─────────────────▼──────────────────────▼─────────────┐
│                          接入层                                  │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Nginx 负载均衡（双机热备 + Keepalived）      │   │
│  └──────────────────────┬──────────────────────────────────┘   │
│                          │                                       │
│  ┌───────────────────────┴──────────────────────────────────┐  │
│  │  API Gateway (8888)          WebSocket Gateway (8889)     │  │
│  │  - JWT 认证                  - 长连接维护                  │  │
│  │  - 路由转发                  - 实时事件推送                 │  │
│  │  - 限流熔断                  - 心跳检测                    │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
          │
┌─────────▼─────────────────────────────────────────────────────┐
│                        业务服务层                               │
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │auth-svc  │ │customer  │ │ agent    │ │   call-service   │  │
│  │认证授权  │ │客户管理  │ │ 座席管理 │ │   呼叫管理       │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘  │
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │ivr-svc   │ │recording │ │ ticket   │ │  quality-svc     │  │
│  │IVR导航   │ │录音服务  │ │ 工单服务 │ │  质检服务        │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘  │
│                                                                  │
│  ┌──────────┐ ┌──────────────────────────────────────────────┐ │
│  │report-svc│ │           notification-service               │ │
│  │报表服务  │ │           通知服务（WebSocket/邮件）           │ │
│  └──────────┘ └──────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
          │
┌─────────▼─────────────────────────────────────────────────────┐
│                        核心引擎层                               │
│                                                                  │
│  ┌─────────────────────────┐  ┌──────────────────────────────┐ │
│  │    呼叫分配引擎          │  │       状态管理引擎            │ │
│  │  技能匹配+最长空闲算法   │  │  座席状态实时同步             │ │
│  │  VIP优先+队列管理        │  │  Redis Pub/Sub广播           │ │
│  └─────────────────────────┘  └──────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
          │
┌─────────▼─────────────────────────────────────────────────────┐
│                          数据层                                 │
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │PostgreSQL│ │ MongoDB  │ │  Redis   │ │  Elasticsearch   │  │
│  │主业务数据│ │日志/元数据│ │缓存/状态 │ │  全文检索        │  │
│  │一主两从  │ │三节点副本│ │Sentinel  │ │  三节点集群      │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘  │
│                                                                  │
│  ┌──────────┐ ┌──────────────────────────────────────────────┐ │
│  │  MinIO   │ │              Apache Kafka                    │ │
│  │录音文件  │ │         事件驱动消息总线（3 Broker）           │ │
│  │对象存储  │ │  call-events / agent-events / ticket-events  │ │
│  └──────────┘ └──────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
          │
┌─────────▼─────────────────────────────────────────────────────┐
│                        电话系统层                               │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │         FreeSWITCH 集群（双活部署）                       │   │
│  │  - SIP 信令处理          - 媒体流管理                     │   │
│  │  - DTMF 按键识别         - 录音控制                       │   │
│  │  - 呼叫转接/会议         - WebRTC 网关                    │   │
│  └──────────────────────────┬──────────────────────────────┘   │
│                              │ SIP/PSTN                         │
│  ┌───────────────────────────▼──────────────────────────────┐  │
│  │                    PSTN 运营商网关                         │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 微服务划分

### 3.1 服务清单

| 服务名 | 端口 | 职责 | 数据存储 |
|--------|------|------|---------|
| api-gateway | 8888 | 路由、认证、限流、熔断 | Redis |
| ws-gateway | 8889 | WebSocket 长连接、实时推送 | - |
| auth-service | 8087 | 用户认证、JWT、RBAC、审计 | PostgreSQL |
| customer-service | 8082 | 客户信息、黑名单 | PostgreSQL + Redis |
| agent-service | 8081 | 座席状态、技能组、培训模式 | PostgreSQL + Redis |
| call-service | 8080 | 呼叫生命周期、FreeSWITCH 集成 | PostgreSQL + Kafka |
| ivr-service | 8084 | IVR 流程配置与执行 | PostgreSQL + Redis |
| recording-service | 8085 | 录音加密存储、播放 | MongoDB + MinIO |
| ticket-service | 8083 | 工单创建、流转、通知 | PostgreSQL |
| quality-service | 8086 | 质检评分、模板管理 | PostgreSQL |
| report-service | 8088 | 报表生成、导出、定时发送 | PostgreSQL |
| notification-service | 8089 | WebSocket 推送、邮件通知 | - |
| call-distribution-engine | 8090 | 呼叫分配算法、队列管理 | Redis |

### 3.2 服务间通信

```
同步通信（HTTP/REST via API Gateway）：
  前端 → API Gateway → 各业务服务

异步通信（Kafka 事件总线）：
  call-service     → [qianniu.call.events]        → call-distribution-engine
  call-service     → [qianniu.call.events]        → ws-gateway（推送座席）
  agent-service    → [qianniu.agent.status.events] → call-distribution-engine
  agent-service    → [qianniu.agent.status.events] → ws-gateway（推送监控）
  ticket-service   → [qianniu.ticket.events]       → notification-service
  call-service     → [qianniu.recording.process]   → recording-service
  call-distribution-engine → [qianniu.call.assign] → call-service + agent-service
```

---

## 4. 数据架构

### 4.1 核心数据模型

```
users (用户)
  ├── id, username, password, real_name, email, phone
  ├── role_id → roles (ADMIN / SUPERVISOR / AGENT / INSPECTOR)
  └── status (ACTIVE / DISABLED)

agents (座席)
  ├── id, agent_no, real_name, extension
  ├── user_id → users
  ├── skill_group_id → skill_groups
  ├── level (NORMAL / SENIOR)
  ├── status (IDLE / TALKING / WRAPUP / REST / OFFLINE)
  └── training_mode (boolean)

customers (客户)
  ├── id, phone, name, email, address
  ├── vip_level (NORMAL / VIP / SVIP)
  └── tags[], notes

calls (通话记录)
  ├── id (UUID), call_type (INBOUND/OUTBOUND)
  ├── status (INITIATED→RINGING→QUEUED→ANSWERED→COMPLETED)
  ├── caller_number, called_number
  ├── agent_id → agents
  ├── customer_id → customers
  ├── queue_enter_at, answer_at, hangup_at, duration
  ├── satisfaction (1-5)
  └── recording_id

tickets (工单)
  ├── id, ticket_no (TK+时间戳+序号)
  ├── title, description, priority (LOW/NORMAL/HIGH/URGENT)
  ├── status (PENDING→IN_PROGRESS→RESOLVED→CLOSED)
  ├── customer_id → customers
  ├── call_id → calls
  └── created_by, assigned_to → users

quality_inspections (质检记录)
  ├── id, call_id → calls
  ├── template_id → quality_templates
  ├── inspector_id → users
  ├── scores (JSONB), total_score
  └── passed (boolean), notes, suggestions
```

### 4.2 数据存储策略

| 数据类型 | 存储 | 原因 |
|---------|------|------|
| 用户/座席/客户/工单 | PostgreSQL | 结构化，需要事务 |
| 通话记录 | PostgreSQL | 结构化，需要复杂查询 |
| 录音元数据 | MongoDB | 半结构化，灵活扩展 |
| 录音文件 | MinIO | 大文件对象存储 |
| 座席实时状态 | Redis | 高频读写，毫秒级响应 |
| 呼叫队列 | Redis ZSet | 有序集合，优先级队列 |
| 通话记录检索 | Elasticsearch | 全文检索，复杂过滤 |
| 服务间事件 | Kafka | 解耦，异步，高吞吐 |

---

## 5. 通信机制

### 5.1 呼叫分配流程

```
呼入呼叫到达 FreeSWITCH
    │
    ▼
FreeSWITCH ESL 事件 → call-service.handleInboundCallCreated()
    │
    ▼
发布 CALL_CREATED 事件 → Kafka[qianniu.call.events]
    │
    ▼
call-distribution-engine 消费事件
    │
    ├─ 查询 Redis 获取技能组空闲座席列表
    │
    ├─ [有空闲座席] → 技能匹配 + 最长空闲算法 → 选择座席
    │       │
    │       ▼
    │   发布 CALL_ASSIGN 事件 → call-service 更新呼叫状态
    │                        → agent-service 更新座席状态为 TALKING
    │                        → ws-gateway 推送来电弹屏给座席
    │
    └─ [无空闲座席] → 加入 Redis ZSet 队列
            │
            ▼
        IVR 播放等待音乐（每30秒提示）
            │
        [座席变为空闲] → 触发队列分配
        [等待超300秒] → 播放道歉语音 + 留言选项
```

### 5.2 实时状态同步

```
座席状态变更
    │
    ▼
agent-service.updateStatus()
    ├── 写入 PostgreSQL（持久化）
    ├── 写入 Redis（缓存，1小时TTL）
    └── 发布 Kafka 事件[qianniu.agent.status.events]
            │
            ├── call-distribution-engine 消费 → 触发队列分配
            └── ws-gateway 消费 → 广播给管理后台监控面板
```

---

## 6. 安全架构

### 6.1 认证授权

```
请求流程：
客户端 → [携带 JWT Token] → Nginx → API Gateway
                                        │
                                    AuthFilter 验证 Token
                                        │
                                    解析 userId / role
                                        │
                                    注入请求头 X-User-Id / X-User-Role
                                        │
                                    转发到下游服务
```

**JWT Token 结构：**
```json
{
  "sub": "用户ID",
  "username": "用户名",
  "role": "ADMIN|SUPERVISOR|AGENT|INSPECTOR",
  "realName": "真实姓名",
  "iat": 签发时间,
  "exp": 过期时间（24小时）
}
```

### 6.2 角色权限矩阵

| 功能 | 管理员 | 座席主管 | 座席 | 质检员 |
|------|--------|---------|------|--------|
| 查看所有通话记录 | ✅ | ✅（本组） | ❌（仅自己） | ✅ |
| 查看所有工单 | ✅ | ✅（本组） | ❌（仅自己） | ❌ |
| 系统配置管理 | ✅ | ❌ | ❌ | ❌ |
| 座席管理 | ✅ | ✅（本组） | ❌ | ❌ |
| 质检评分 | ✅ | ✅ | ❌ | ✅ |
| 报表查看 | ✅ | ✅（本组） | ❌ | ❌ |
| 黑名单管理 | ✅ | ❌ | ❌ | ❌ |

### 6.3 数据安全

- **传输加密：** 全链路 HTTPS/TLS 1.2+
- **录音加密：** AES-256-CBC，每个文件独立随机 IV
- **密码存储：** BCrypt 哈希（cost factor 10）
- **API 限流：** 每个 API Key 100 次/分钟
- **SQL 注入防护：** MyBatis Plus 参数化查询
- **XSS 防护：** 前端输入过滤 + CSP 响应头

---

## 7. 高可用设计

### 7.1 各组件高可用方案

| 组件 | 方案 | 故障切换时间 |
|------|------|------------|
| Nginx | 双机热备 + Keepalived VIP 漂移 | < 30 秒 |
| API Gateway | K8s 2 副本 + 健康检查 | < 10 秒 |
| 业务服务 | K8s 2-3 副本 + HPA 自动扩缩 | < 30 秒 |
| PostgreSQL | 一主两从 + 自动故障切换 | < 60 秒 |
| Redis | Sentinel 模式（1主2从3哨兵） | < 30 秒 |
| Kafka | 3 Broker + 副本因子 3 + ISR ≥ 2 | 自动 |
| FreeSWITCH | 双活部署 + DNS 轮询 | < 5 秒 |
| MinIO | 分布式模式（4节点） | 自动 |

### 7.2 K8s HPA 配置

```yaml
# 呼叫服务自动扩缩容
minReplicas: 3
maxReplicas: 10
触发条件:
  CPU 使用率 > 70%
  内存使用率 > 80%
```

### 7.3 数据备份策略

| 数据 | 备份频率 | 保留时间 | 存储位置 |
|------|---------|---------|---------|
| PostgreSQL | 每日全量 + 实时 WAL | 30 天 | 独立备份服务器 |
| MongoDB | 每日全量 | 30 天 | 独立备份服务器 |
| 录音文件 | 实时上传 MinIO | 90 天 | MinIO 分布式 |
| 系统配置 | 每日 | 30 天 | Git 仓库 |

---

## 8. 核心流程

### 8.1 呼入全流程时序图

```
客户        FreeSWITCH    call-service   distribution   agent-service   座席工作台
  │               │              │            │               │               │
  │──拨打电话────▶│              │            │               │               │
  │               │──CHANNEL_CREATE事件──────▶│              │               │
  │               │              │──创建呼叫记录              │               │
  │               │              │──发布CALL_CREATED──────────▶              │
  │               │              │            │──查询空闲座席──▶              │
  │               │              │            │◀──返回座席列表─┤              │
  │               │              │            │──选择最长空闲座席             │
  │               │              │            │──发布CALL_ASSIGN──────────────▶
  │               │              │◀───────────┤──更新呼叫状态  │               │
  │               │              │            │               │──更新状态TALKING
  │               │              │            │               │──推送来电弹屏──▶
  │               │              │            │               │               │──弹出来电窗口
  │               │              │            │               │               │──座席点击接听
  │               │──CHANNEL_ANSWER事件───────▶              │               │
  │               │              │──更新状态ANSWERED          │               │
  │               │              │──启动录音──▶               │               │
  │◀──通话建立────│              │            │               │               │
  │               │              │            │               │               │
  │──挂断─────────▶              │            │               │               │
  │               │──CHANNEL_HANGUP事件───────▶              │               │
  │               │              │──计算通话时长              │               │
  │               │              │──更新状态COMPLETED         │               │
  │               │              │──停止录音──▶               │               │
  │               │              │──发布CALL_COMPLETED        │               │
  │               │              │            │               │──更新状态WRAPUP
  │               │              │            │               │──推送整理提醒──▶
  │               │              │──IVR满意度评价──────────────────────────────
  │──按键评分─────▶              │            │               │               │
  │               │              │──保存满意度评分            │               │
```

### 8.2 工单处理流程

```
座席通话中
    │
    ▼
座席创建工单（填写标题/描述/优先级）
    │
    ▼
ticket-service 生成工单编号（TK+时间戳+序号）
    │
    ▼
发布 TICKET_CREATED 事件 → Kafka
    │
    ▼
自动分配逻辑：
  - 根据工单类别确定技能组
  - URGENT 优先分配高级座席
  - 普通工单按待处理工单数负载均衡
    │
    ▼
notification-service 推送通知给被分配人
    │
    ▼
处理人处理工单 → 状态变更记录历史
    │
    ├── 24小时未处理 → 告警通知管理员
    │
    ▼
工单解决 → 客户确认 → 关闭
```
