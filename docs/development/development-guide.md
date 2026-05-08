# 千牛云呼叫中心系统 - 开发指南

**版本：** 1.0.0  
**作者：** 深圳市千牛云科技有限公司

---

## 目录

1. [开发环境搭建](#1-开发环境搭建)
2. [代码规范](#2-代码规范)
3. [新增微服务](#3-新增微服务)
4. [Kafka 事件规范](#4-kafka-事件规范)
5. [API 开发规范](#5-api-开发规范)
6. [测试规范](#6-测试规范)
7. [Git 工作流](#7-git-工作流)

---

## 1. 开发环境搭建

### 必要工具

```bash
# Java 17
java -version  # 应显示 17.x

# Maven 3.9+
mvn -version

# Node.js 18+
node -version

# Docker
docker -version

# kubectl（可选，用于本地 K8s 调试）
kubectl version
```

### IDE 推荐配置（IntelliJ IDEA）

1. 安装插件：Lombok、MapStruct Support、SonarLint
2. 启用注解处理器：Settings → Build → Compiler → Annotation Processors → Enable
3. 代码风格：导入 `docs/development/code-style.xml`

### 本地开发配置

在各服务的 `src/main/resources/application-dev.yml` 中配置本地环境：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/qianniu_callcenter
    username: qianniu
    password: qianniu@2025
  data:
    redis:
      host: localhost
      port: 6379
      password: qianniu@2025
  kafka:
    bootstrap-servers: localhost:29092

freeswitch:
  host: localhost
  port: 8021
  password: ClueCon

logging:
  level:
    com.qianniuyun: DEBUG
```

---

## 2. 代码规范

### 包命名规范

```
com.qianniuyun.{service-name}
  ├── controller/    # REST 控制器
  ├── service/       # 业务逻辑
  ├── repository/    # 数据访问层
  ├── entity/        # 数据库实体
  ├── dto/           # 数据传输对象
  ├── event/         # Kafka 事件对象
  ├── config/        # 配置类
  └── exception/     # 服务特定异常
```

### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase | `CallService` |
| 方法名 | lowerCamelCase | `handleCallAnswered` |
| 常量 | UPPER_SNAKE_CASE | `MAX_QUEUE_SIZE` |
| 数据库表 | snake_case | `call_records` |
| Kafka Topic | 点分隔小写 | `qianniu.call.events` |
| Redis Key | 冒号分隔 | `agent:status:123` |

### 注释规范

```java
/**
 * 处理呼叫接通事件
 * 由 FreeSWITCH CHANNEL_ANSWER 事件触发
 *
 * @param callId FreeSWITCH 呼叫 UUID
 */
@Transactional
public void handleCallAnswered(String callId) {
    // 1. 更新呼叫状态
    // 2. 启动录音
    // 3. 发布事件
}
```

---

## 3. 新增微服务

### 步骤

1. **在父 POM 中添加模块**

```xml
<!-- backend/pom.xml -->
<modules>
  ...
  <module>services/new-service</module>
</modules>
```

2. **创建服务目录结构**

```bash
mkdir -p backend/services/new-service/src/main/java/com/qianniuyun/newservice
mkdir -p backend/services/new-service/src/main/resources
mkdir -p backend/services/new-service/src/test/java/com/qianniuyun/newservice
```

3. **创建服务 pom.xml**（参考 `call-service/pom.xml`）

4. **创建 Spring Boot 启动类**

```java
@SpringBootApplication
@EnableDiscoveryClient
public class NewServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewServiceApplication.class, args);
    }
}
```

5. **在 API Gateway 添加路由**（`api-gateway/application.yml`）

6. **创建 K8s 部署配置**（参考 `call-service.yaml`）

---

## 4. Kafka 事件规范

### 现有 Topic 列表

| Topic | 生产者 | 消费者 | 说明 |
|-------|--------|--------|------|
| `qianniu.call.events` | call-service | distribution-engine, ws-gateway | 呼叫生命周期事件 |
| `qianniu.agent.status.events` | agent-service | distribution-engine, ws-gateway | 座席状态变更 |
| `qianniu.call.assign` | distribution-engine | call-service, agent-service | 呼叫分配结果 |
| `qianniu.ticket.events` | ticket-service | notification-service | 工单事件 |
| `qianniu.recording.process` | call-service | recording-service | 录音处理请求 |
| `qianniu.notification.events` | 各服务 | notification-service | 通知事件 |
| `qianniu.ivr.dtmf` | call-service | ivr-service | DTMF 按键事件 |
| `qianniu.queue.alert` | distribution-engine | ws-gateway | 队列预警 |

### 事件对象规范

```java
// 所有事件对象必须包含以下字段
public class BaseEvent {
    private String eventId;      // 事件唯一 ID（UUID）
    private String eventType;    // 事件类型
    private LocalDateTime timestamp;  // 事件时间
    private String source;       // 来源服务名
}
```

### 消费者配置规范

```java
@KafkaListener(
    topics = "qianniu.call.events",
    groupId = "your-service-name",  // 使用服务名作为消费组
    containerFactory = "kafkaListenerContainerFactory"
)
public void onCallEvent(CallEvent event) {
    // 幂等处理：检查事件是否已处理
    // 异常处理：记录日志，不抛出异常（避免无限重试）
}
```

---

## 5. API 开发规范

### RESTful 接口规范

```
GET    /resources          # 查询列表（分页）
GET    /resources/{id}     # 查询单个
POST   /resources          # 创建
PUT    /resources/{id}     # 全量更新
PATCH  /resources/{id}     # 部分更新
DELETE /resources/{id}     # 删除

# 动作型接口
POST   /calls/{id}/hangup
POST   /calls/{id}/transfer
```

### 控制器模板

```java
@RestController
@RequestMapping("/your-resource")
@RequiredArgsConstructor
@Tag(name = "资源管理", description = "资源相关接口")
public class YourController {

    private final YourService yourService;

    @GetMapping
    @Operation(summary = "分页查询")
    public Result<PageResult<YourDTO>> list(YourQuery query) {
        return Result.success(yourService.list(query));
    }

    @PostMapping
    @Operation(summary = "创建")
    public Result<YourDTO> create(
            @Valid @RequestBody CreateYourDTO dto,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(yourService.create(dto, userId));
    }
}
```

---

## 6. 测试规范

### 单元测试

每个 Service 类必须有对应的单元测试，覆盖率要求 ≥ 70%。

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("YourService 单元测试")
class YourServiceTest {

    @Mock
    private YourRepository repository;

    @InjectMocks
    private YourService service;

    @Test
    @DisplayName("正常场景：描述预期行为")
    void methodName_WhenCondition_ShouldExpectedBehavior() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(new YourEntity()));

        // When
        YourDTO result = service.findById(1L);

        // Then
        assertThat(result).isNotNull();
        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("异常场景：资源不存在时抛出异常")
    void findById_WhenNotFound_ShouldThrowException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不存在");
    }
}
```

### 运行测试

```bash
# 运行所有测试
cd backend
mvn test

# 运行特定服务测试
mvn test -pl services/call-service

# 生成测试覆盖率报告
mvn test jacoco:report
# 报告位于 target/site/jacoco/index.html
```

---

## 7. Git 工作流

### 分支策略

```
main          # 生产环境代码，只接受 release/* 合并
develop       # 开发主分支，功能开发完成后合并到此
feature/*     # 功能开发分支，从 develop 创建
bugfix/*      # Bug 修复分支，从 develop 创建
release/*     # 发布分支，从 develop 创建，测试通过后合并到 main
hotfix/*      # 紧急修复，从 main 创建，修复后合并到 main 和 develop
```

### 提交信息规范

```
<type>(<scope>): <subject>

type:
  feat     - 新功能
  fix      - Bug 修复
  docs     - 文档更新
  style    - 代码格式（不影响功能）
  refactor - 重构
  test     - 测试相关
  chore    - 构建/工具相关

示例：
feat(call-service): 添加三方通话功能
fix(recording): 修复 AES-CBC 加密 IV 未保存的问题
docs(api): 更新 API 接口文档
```

### 代码审查要求

- 所有 PR 必须至少 1 人审查通过
- 必须通过 CI 流水线（编译 + 测试 + 代码检查）
- 不允许直接推送到 main 和 develop 分支
