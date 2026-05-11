# 千牛云呼叫中心系统 - API 接口文档

**版本：** 1.0.0  
**Base URL：** `https://callcenter.qianniuyun.com/api`  
**认证方式：** Bearer Token（JWT）  
**作者：** 深圳市千牛云科技有限公司

---

## 目录

- [认证接口](#认证接口)
- [座席管理接口](#座席管理接口)
- [客户管理接口](#客户管理接口)
- [呼叫管理接口](#呼叫管理接口)
- [工单接口](#工单接口)
- [报表接口](#报表接口)
- [系统配置接口](#系统配置接口)
- [错误码说明](#错误码说明)

---

## 通用说明

### 请求头

```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
Accept: application/json
```

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1704067200000
}
```

### 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

---

## 认证接口

### POST /auth/login

用户登录，获取 JWT Token。

**请求体：**
```json
{
  "username": "agent001",
  "password": "Agent@2025"
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "agent001",
    "realName": "张三",
    "role": "AGENT",
    "expiresIn": 86400
  }
}
```

---

### POST /auth/logout

用户登出。

**请求头：** 需要 Authorization

**响应：**
```json
{ "code": 200, "message": "success" }
```

---

### POST /auth/change-password

修改密码。

**请求参数：** Query 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| oldPassword | string | 是 | 原密码 |
| newPassword | string | 是 | 新密码（8-20位，含大小写和数字） |

---

## 座席管理接口

### GET /agents

分页查询座席列表。

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |
| skillGroupId | long | 否 | 技能组 ID |
| status | string | 否 | 状态筛选 |
| keyword | string | 否 | 工号或姓名模糊搜索 |

**响应：**
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "agentNo": "A001",
        "realName": "张三",
        "extension": "8001",
        "skillGroup": { "id": 1, "name": "通用客服" },
        "level": "NORMAL",
        "status": "IDLE",
        "trainingMode": false,
        "todayCalls": 25,
        "avgHandleTime": 180
      }
    ],
    "total": 50,
    "page": 1,
    "pageSize": 20
  }
}
```

---

### PUT /agents/{agentId}/status

更新座席状态。

**路径参数：** `agentId` - 座席 ID

**请求体：**
```json
{
  "status": "REST"
}
```

**status 可选值：** `IDLE` | `REST` | `WRAPUP` | `OFFLINE`

---

### PUT /agents/{agentId}/training-mode

切换培训模式。

**请求体：**
```json
{
  "enabled": true
}
```

---

### GET /agents/skill-groups

获取技能组列表。

**响应：**
```json
{
  "code": 200,
  "data": [
    { "id": 1, "code": "GENERAL", "name": "通用客服", "agentCount": 20 },
    { "id": 2, "code": "TECH", "name": "技术支持", "agentCount": 10 }
  ]
}
```

---

## 客户管理接口

### GET /customers

分页查询客户列表。

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量 |
| keyword | string | 否 | 姓名/电话模糊搜索 |
| vipLevel | string | 否 | VIP 等级筛选 |

---

### GET /customers/by-phone/{phone}

根据电话号码查询客户（来电弹屏使用）。

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 100,
    "phone": "13800138000",
    "name": "李四",
    "email": "lisi@example.com",
    "vipLevel": "VIP",
    "tags": ["重要客户", "投诉记录"],
    "notes": "需要重点关注",
    "recentCalls": 3,
    "openTickets": 1
  }
}
```

---

### POST /customers

创建客户。

**请求体：**
```json
{
  "phone": "13900139000",
  "name": "王五",
  "email": "wangwu@example.com",
  "address": "广东省深圳市",
  "vipLevel": "NORMAL",
  "notes": "备注信息"
}
```

---

### PUT /customers/{customerId}

更新客户信息。

---

### POST /customers/blacklist

添加黑名单。

**请求体：**
```json
{
  "phone": "13700137000",
  "reason": "恶意骚扰"
}
```

---

### DELETE /customers/blacklist/{phone}

移除黑名单。

---

## 呼叫管理接口

### POST /calls/outbound

发起呼出呼叫。

**请求体：**
```json
{
  "phone": "13800138000",
  "agentId": 1
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "callType": "OUTBOUND",
    "status": "INITIATED",
    "calledNumber": "13800138000"
  }
}
```

---

### POST /calls/{callId}/hangup

挂断呼叫。

---

### POST /calls/{callId}/transfer

转接呼叫。

**请求体：**
```json
{
  "targetAgentId": 5,
  "transferType": "BLIND"
}
```

**transferType：** `BLIND`（盲转）| `ATTENDED`（协商转）

---

### POST /calls/{callId}/conference

发起三方通话。

**请求体：**
```json
{
  "thirdPartyNumber": "13600136000"
}
```

---

### POST /calls/{callId}/hold

保持通话。

---

### POST /calls/{callId}/unhold

恢复通话。

---

### PUT /calls/{callId}/summary

填写通话小结。

**请求体：**
```json
{
  "summary": "客户咨询产品使用问题，已解答"
}
```

---

### GET /calls

分页查询通话记录。

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量 |
| agentId | long | 否 | 座席 ID |
| customerId | long | 否 | 客户 ID |
| callType | string | 否 | INBOUND / OUTBOUND |
| startDate | date | 否 | 开始日期（yyyy-MM-dd） |
| endDate | date | 否 | 结束日期 |

---

### GET /calls/{callId}/recording

获取通话关联的录音信息。

**响应：**
```json
{
  "code": 200,
  "data": {
    "callId": "550e8400-e29b-41d4-a716-446655440000",
    "recordingId": "/recordings/2026/05/11/xxx.wav",
    "recordingReady": true,
    "streamUrl": "/api/recordings/550e8400-e29b-41d4-a716-446655440000/stream"
  }
}
```

### GET /recordings/{callId}/stream

获取录音播放流。

**响应：** 音频流（audio/wav）

---

## 工单接口

### POST /tickets

创建工单。

**请求体：**
```json
{
  "title": "产品功能咨询",
  "description": "客户询问如何使用XX功能",
  "priority": "NORMAL",
  "category": "CONSULTATION",
  "customerId": 100,
  "callId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**priority 可选值：** `LOW` | `NORMAL` | `HIGH` | `URGENT`

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "ticketNo": "TK20250101120000001",
    "status": "PENDING",
    "priority": "NORMAL",
    "createdAt": "2025-01-01T12:00:00"
  }
}
```

---

### GET /tickets

分页查询工单列表。

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| pageSize | int | 否 | 每页数量 |
| status | string | 否 | 状态筛选 |
| priority | string | 否 | 优先级筛选 |
| assignedTo | long | 否 | 处理人 ID |

---

### PUT /tickets/{ticketId}/status

更新工单状态。

**请求体：**
```json
{
  "status": "RESOLVED",
  "comment": "问题已解决，客户确认"
}
```

---

### PUT /tickets/{ticketId}/assign

分配工单。

**请求体：**
```json
{
  "assigneeId": 5
}
```

---

## 报表接口

### GET /reports/realtime-dashboard

获取实时监控面板数据。

**响应：**
```json
{
  "code": 200,
  "data": {
    "realtimeMetrics": {
      "onlineAgents": 18,
      "talkingAgents": 7,
      "idleAgents": 8,
      "wrapupAgents": 2,
      "restAgents": 1,
      "queueSize": 4
    },
    "todayStats": {
      "inboundCalls": 286,
      "outboundCalls": 94,
      "answerRate": 93.6,
      "avgWaitTime": 18
    },
    "agentStatusList": [],
    "trendHours": ["09:00", "10:00"],
    "inboundTrend": [32, 48],
    "outboundTrend": [12, 18]
  }
}
```

### POST /reports/agent-performance

生成座席绩效报表（规划接口，当前基础版未实现）。

**请求体：**
```json
{
  "startDate": "2025-01-01",
  "endDate": "2025-01-31",
  "agentIds": [1, 2, 3]
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "reportId": 1,
    "status": "PENDING",
    "message": "报表生成中，请稍后查询"
  }
}
```

---

### POST /reports/call-statistics

生成呼叫统计报表（规划接口，当前基础版未实现）。

---

### GET /reports/{reportId}

查询报表状态和数据（规划接口，当前基础版未实现）。

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "reportType": "AGENT_PERFORMANCE",
    "status": "COMPLETED",
    "data": [
      {
        "agentNo": "A001",
        "realName": "张三",
        "callCount": 85,
        "totalTalkTime": 15300,
        "avgHandleTime": 180,
        "satisfaction": 4.8
      }
    ]
  }
}
```

---

### GET /reports/{reportId}/export

导出报表文件（规划接口，当前基础版未实现）。

**查询参数：** `format` = `excel` | `pdf`

**响应：** 文件流（application/vnd.openxmlformats-officedocument.spreadsheetml.sheet）

---

## 系统配置接口

### GET /system/configs

获取系统配置列表（仅管理员）。

---

### PUT /system/configs/{configKey}

更新系统配置（仅管理员）。

**请求体：**
```json
{
  "configValue": "300"
}
```

---

> 实时监控数据请使用 `GET /reports/realtime-dashboard`。

---

## 错误码说明

| 错误码 | 说明 | 处理建议 |
|--------|------|---------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 检查请求参数格式和必填项 |
| 401 | 未认证 | 重新登录获取 Token |
| 403 | 权限不足 | 联系管理员分配权限 |
| 404 | 资源不存在 | 检查资源 ID 是否正确 |
| 429 | 请求频率超限 | 降低请求频率（API Key 限制 100次/分钟） |
| 500 | 服务器内部错误 | 联系技术支持 |

### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 用户名或密码错误 |
| 1002 | 账号已被禁用 |
| 1003 | Token 已过期 |
| 2001 | 座席不存在 |
| 2002 | 座席状态转换非法 |
| 3001 | 客户电话号码已存在 |
| 3002 | 号码已在黑名单中 |
| 4001 | 呼叫不存在 |
| 4002 | 只有通话中的呼叫才能转接 |
| 4003 | 满意度评分必须在 1-5 之间 |
| 5001 | 工单不存在 |
| 5002 | 已关闭的工单不能变更状态 |
