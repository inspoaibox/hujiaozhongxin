# 千牛云呼叫中心系统

> **深圳市千牛云科技有限公司** 出品  
> 版本：1.0.0 | 许可证：商业软件，保留所有权利

---

## 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [文档索引](#文档索引)
- [联系我们](#联系我们)

---

## 项目简介

千牛云呼叫中心系统是一套企业级全渠道客户服务平台，采用微服务架构设计，支持高并发、高可用的呼叫处理能力。系统集成了电话网关、IVR 语音导航、智能呼叫分配、实时监控、工单管理等核心功能模块，帮助企业提升客户服务质量和运营效率。

**核心指标：**

| 指标 | 目标值 |
|------|--------|
| 系统可用性 | 99.9%+ |
| 并发呼叫支持 | 1000+ |
| 在线座席支持 | 10000+ |
| 呼叫分配延迟 | < 2 秒 |
| 状态更新延迟 | < 1 秒 |
| API 响应时间 | < 200ms (P99) |

---

## 功能特性

### 呼叫管理
- ✅ 呼入/呼出呼叫全生命周期管理
- ✅ 智能 IVR 语音导航（支持 3 层菜单）
- ✅ 技能组路由 + 最长空闲时间分配算法
- ✅ VIP 客户优先分配
- ✅ 呼叫转接（盲转/协商转）和三方通话
- ✅ 队列管理与超时处理（300 秒道歉语音）

### 座席管理
- ✅ 座席状态实时管理（空闲/通话中/整理/休息/离线）
- ✅ 技能组管理与分配
- ✅ 培训模式（隔离真实呼叫）
- ✅ 整理超时提醒（180 秒）
- ✅ WebRTC 软电话支持

### 客户管理
- ✅ 来电自动弹屏（客户信息卡片）
- ✅ VIP 等级管理
- ✅ 客户历史记录（通话 + 工单）
- ✅ 黑名单管理

### 通话质量
- ✅ 全程自动录音（AES-256 加密存储）
- ✅ 通话质检（自定义评分模板）
- ✅ 客户满意度评价（5 分制）
- ✅ 录音在线播放（解密流式传输）

### 工单系统
- ✅ 工单创建与自动分配
- ✅ 状态流转（待处理→处理中→已解决→已关闭）
- ✅ 超时提醒（24 小时未处理通知管理员）
- ✅ 紧急工单标红显示

### 监控与报表
- ✅ 实时监控面板（5 秒刷新）
- ✅ 大屏展示模式
- ✅ 座席绩效报表
- ✅ 呼叫统计报表（接通率、放弃率）
- ✅ 报表导出（Excel/PDF）
- ✅ 定时报表自动发送

### 系统集成
- ✅ RESTful API（支持第三方 CRM/ERP 集成）
- ✅ API 密钥管理（限流 100 次/分钟）
- ✅ 多渠道接入（电话/在线聊天/邮件）

---

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                      客户端层                            │
│   座席工作台(Vue3)  管理后台(Vue3)  第三方系统(API)      │
└──────────────┬──────────────┬──────────────┬────────────┘
               │              │              │
┌──────────────▼──────────────▼──────────────▼────────────┐
│                      接入层                              │
│        API Gateway(8888)    WebSocket Gateway(8889)      │
│              Nginx 负载均衡（双机热备）                   │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                    业务服务层（微服务）                    │
│  auth  customer  agent  call  ivr  recording             │
│  ticket  quality  report  notification                   │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                    核心引擎层                             │
│    呼叫分配引擎    状态管理引擎    路由引擎               │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                      数据层                              │
│  PostgreSQL  MongoDB  Redis  Elasticsearch  MinIO        │
│              Kafka 消息总线                              │
└─────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                    电话系统                              │
│         FreeSWITCH（双活）  ←→  PSTN 网关               │
└─────────────────────────────────────────────────────────┘
```

---

## 技术栈

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.2.x | 微服务框架 |
| Spring Cloud | 2023.0.x | 服务治理 |
| PostgreSQL | 15 | 主业务数据库 |
| MongoDB | 6 | 日志/录音元数据 |
| Redis | 7 | 缓存/状态管理 |
| Apache Kafka | 3.5 | 事件驱动消息总线 |
| Elasticsearch | 8.11 | 全文检索 |
| MinIO | Latest | 录音文件对象存储 |
| FreeSWITCH | 1.10 | SIP 电话服务器 |

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4 | 前端框架 |
| TypeScript | 5.3 | 类型安全 |
| Element Plus | 2.4 | UI 组件库 |
| Pinia | 2.1 | 状态管理 |
| ECharts | 5.4 | 图表可视化 |
| SIP.js | 0.21 | WebRTC 软电话接入基础 |
| 原生 WebSocket | 浏览器内置 | 实时消息客户端 |

### 基础设施
| 技术 | 用途 |
|------|------|
| Kubernetes 1.28+ | 容器编排 |
| Docker | 容器化 |
| Nginx | 负载均衡/反向代理 |
| Prometheus + Grafana | 监控告警 |
| ELK Stack | 日志管理 |
| Jaeger | 链路追踪 |
| Jenkins | CI/CD 流水线 |

---

## 快速开始

### 环境要求

- Docker 24.0+
- Docker Compose 2.20+
- Java 17+（仅本地开发需要）
- Node.js 18+（仅本地开发需要）

### 方式一：一键 Docker 部署（推荐）

**1. 克隆项目**
```bash
git clone https://github.com/inspoaibox/hujiaozhongxin.git
cd hujiaozhongxin
```

**2. 复制配置文件**
```bash
cp .env.example .env
```

默认镜像命名空间已配置为 `inspoaibox`，对应仓库 `https://github.com/inspoaibox/hujiaozhongxin`。

**3. 一键启动**

Linux / macOS：
```bash
bash scripts/docker-up.sh
```

Windows PowerShell：
```powershell
.\scripts\docker-up.ps1
```

也可以直接使用 Docker Compose：
```bash
docker compose up -d
```

**4. 查看状态**
```bash
docker compose ps
```

**5. 访问系统**

| 服务 | 地址 | 默认账号 |
|------|------|---------|
| 座席工作台 | http://localhost:5173 | admin / Admin@2025 |
| 管理后台 | http://localhost:5174 | admin / Admin@2025 |
| MinIO 控制台 | http://localhost:9001 | qianniu / qianniu@2025 |
| Nacos 控制台 | http://localhost:8848/nacos | 默认未开启鉴权 |

更详细的一键部署说明见：`docs/deployment/docker-one-click.md`。

### 方式二：本地开发启动

开发者需要单独启动中间件、后端和前端。

**1. 启动基础设施**
```bash
cd infrastructure/docker-compose
docker compose up -d
```

**2. 启动后端服务**
```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run -pl services/auth-service
```

**3. 启动前端**
```bash
# 座席工作台
cd frontend/agent-workspace
npm install
npm run dev

# 管理后台
cd frontend/admin-portal
npm install
npm run dev
```

---

## 项目结构

```
callcenter/
├── README.md                          # 本文件
├── backend/                           # Java 后端
│   ├── pom.xml                        # Maven 父 POM
│   ├── common/                        # 公共模块
│   │   ├── common-utils/              # 工具类（日期、电话、工单号生成）
│   │   ├── common-model/              # 公共数据模型（Result、PageResult、枚举）
│   │   └── common-exception/          # 全局异常处理
│   ├── config/                        # 公共配置文件
│   │   ├── application-common.yml     # 数据库/Redis/Kafka/MinIO 配置
│   │   └── application-nacos.yml      # Nacos 服务注册配置
│   ├── gateway/                       # 网关服务
│   │   ├── api-gateway/               # API 网关（路由/认证/限流/熔断）
│   │   └── ws-gateway/                # WebSocket 网关（实时推送）
│   ├── services/                      # 业务微服务
│   │   ├── auth-service/              # 认证授权服务（JWT/RBAC）
│   │   ├── customer-service/          # 客户管理服务
│   │   ├── agent-service/             # 座席管理服务
│   │   ├── call-service/              # 呼叫管理服务（含 FreeSWITCH 集成）
│   │   ├── ivr-service/               # IVR 语音导航服务
│   │   ├── recording-service/         # 录音服务（AES-256 加密）
│   │   ├── ticket-service/            # 工单服务
│   │   ├── quality-service/           # 质检服务
│   │   ├── report-service/            # 报表服务
│   │   └── notification-service/      # 通知服务（WebSocket/邮件）
│   └── engines/                       # 核心引擎
│       └── call-distribution-engine/  # 呼叫分配引擎（技能匹配+最长空闲）
├── frontend/                          # Vue 3 前端
│   ├── agent-workspace/               # 座席工作台
│   │   └── src/
│   │       ├── components/            # 组件（SoftPhone 软电话等）
│   │       ├── stores/                # Pinia 状态管理
│   │       ├── composables/           # 组合式函数（WebSocket 等）
│   │       └── views/                 # 页面视图
│   └── admin-portal/                  # 管理后台
│       └── src/
│           └── views/
│               └── dashboard/         # 实时监控面板
├── infrastructure/                    # 基础设施配置
│   ├── docker-compose/                # 本地开发环境
│   │   ├── docker-compose.yml         # 所有中间件一键启动
│   │   └── monitoring/                # Prometheus/告警规则
│   ├── kubernetes/                    # K8s 生产部署配置
│   │   ├── namespaces.yaml            # 命名空间定义
│   │   ├── storage-classes.yaml       # 存储类定义
│   │   ├── databases/                 # 数据库 StatefulSet
│   │   ├── monitoring/                # 监控组件部署
│   │   └── services/                  # 业务服务 Deployment
│   ├── monitoring/                    # Prometheus 配置和告警规则
│   ├── logging/                       # Logstash 日志管道配置
│   ├── nginx/                         # Nginx 负载均衡配置
│   ├── sql/                           # 数据库初始化脚本
│   └── cicd/                          # Jenkins CI/CD 流水线
└── docs/                              # 项目文档
    ├── api/                           # API 接口文档
    ├── architecture/                  # 架构设计文档
    ├── deployment/                    # 部署指南
    ├── user-guide/                    # 用户操作手册
    ├── integration-test-plan.md       # 集成测试计划
    └── operations-manual.md           # 运维手册
```

---

## 文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 架构设计文档 | `docs/architecture/architecture.md` | 系统架构、组件设计、核心流程 |
| 产品解决方案 | `docs/product-solution.md` | 产品定位、业务方案、架构、实施路线、验收清单 |
| 完整小白使用手册 | `docs/user-guide/beginner-guide.md` | 每一页、每个功能、每个按钮和操作步骤 |
| API 接口文档 | `docs/api/api-reference.md` | 所有 REST API 接口说明 |
| 一键 Docker 部署 | `docs/deployment/docker-one-click.md` | 服务器直接拉 GitHub 镜像并一键启动 |
| 部署指南 | `docs/deployment/deployment-guide.md` | 本地开发和生产环境部署步骤 |
| 座席操作手册 | `docs/user-guide/agent-guide.md` | 座席工作台使用说明 |
| 管理员操作手册 | `docs/user-guide/admin-guide.md` | 管理后台使用说明 |
| 开发指南 | `docs/development/development-guide.md` | 开发规范、新增服务、测试规范 |
| 运维手册 | `docs/operations-manual.md` | 日常运维、故障处理、备份恢复 |
| 集成测试计划 | `docs/integration-test-plan.md` | 联调测试用例 |
| 需求文档 | `.kiro/specs/qianniu-call-center-platform/requirements.md` | 系统需求规格（20个需求） |
| 技术设计文档 | `.kiro/specs/qianniu-call-center-platform/design.md` | 详细技术设计 |
| 实施任务列表 | `.kiro/specs/qianniu-call-center-platform/tasks.md` | 开发任务分解 |

---

## 联系我们

**深圳市千牛云科技有限公司**

- 官网：https://www.qianniuyun.com
- 邮箱：support@qianniuyun.com
- 地址：广东省深圳市南山区科技园

---

*© 2025 深圳市千牛云科技有限公司 版权所有*
