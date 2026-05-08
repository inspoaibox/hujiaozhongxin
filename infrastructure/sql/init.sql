-- 千牛云呼叫中心系统 - 数据库初始化脚本
-- 作者：深圳市千牛云科技有限公司
-- 版本：1.0

-- 创建数据库扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ==================== 用户与权限 ====================

-- 角色表
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 用户表
CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    username     VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    real_name    VARCHAR(100),
    email        VARCHAR(200),
    phone        VARCHAR(20),
    role_id      BIGINT       NOT NULL REFERENCES roles(id),
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DISABLED
    last_login   TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- API 密钥表
CREATE TABLE api_keys (
    id          BIGSERIAL PRIMARY KEY,
    key_value   VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    rate_limit  INT          NOT NULL DEFAULT 100,
    created_by  BIGINT       REFERENCES users(id),
    expires_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 操作审计日志
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       REFERENCES users(id),
    action      VARCHAR(200) NOT NULL,
    resource    VARCHAR(200),
    detail      TEXT,
    ip_address  VARCHAR(50),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ==================== 座席管理 ====================

-- 技能组表
CREATE TABLE skill_groups (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 座席表
CREATE TABLE agents (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    agent_no        VARCHAR(50)  NOT NULL UNIQUE,
    real_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    extension       VARCHAR(20),
    skill_group_id  BIGINT       REFERENCES skill_groups(id),
    level           VARCHAR(20)  NOT NULL DEFAULT 'NORMAL', -- NORMAL, SENIOR
    status          VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE', -- IDLE, TALKING, WRAPUP, REST, OFFLINE
    training_mode   BOOLEAN      NOT NULL DEFAULT FALSE,
    status_changed_at TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 座席状态历史
CREATE TABLE agent_status_history (
    id          BIGSERIAL PRIMARY KEY,
    agent_id    BIGINT       NOT NULL REFERENCES agents(id),
    old_status  VARCHAR(20),
    new_status  VARCHAR(20)  NOT NULL,
    duration    INT,          -- 上一状态持续秒数
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ==================== 客户管理 ====================

-- 客户表
CREATE TABLE customers (
    id          BIGSERIAL PRIMARY KEY,
    phone       VARCHAR(20)  NOT NULL,
    name        VARCHAR(100),
    email       VARCHAR(200),
    address     VARCHAR(500),
    vip_level   VARCHAR(20)  NOT NULL DEFAULT 'NORMAL', -- NORMAL, VIP, SVIP
    tags        TEXT[],
    notes       TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_name ON customers USING gin(name gin_trgm_ops);

-- 黑名单表
CREATE TABLE blacklist (
    id          BIGSERIAL PRIMARY KEY,
    phone       VARCHAR(20)  NOT NULL UNIQUE,
    reason      VARCHAR(500),
    created_by  BIGINT       REFERENCES users(id),
    removed_by  BIGINT       REFERENCES users(id),
    removed_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ==================== 呼叫管理 ====================

-- 通话记录表
CREATE TABLE calls (
    id              VARCHAR(50)  PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    call_type       VARCHAR(20)  NOT NULL, -- INBOUND, OUTBOUND
    status          VARCHAR(20)  NOT NULL, -- INITIATED, RINGING, QUEUED, ANSWERED, COMPLETED, ABANDONED, FAILED
    caller_number   VARCHAR(20),
    called_number   VARCHAR(20),
    agent_id        BIGINT       REFERENCES agents(id),
    customer_id     BIGINT       REFERENCES customers(id),
    skill_group_id  BIGINT       REFERENCES skill_groups(id),
    queue_enter_at  TIMESTAMP,
    answer_at       TIMESTAMP,
    hangup_at       TIMESTAMP,
    duration        INT,          -- 通话时长（秒）
    wait_duration   INT,          -- 等待时长（秒）
    hangup_reason   VARCHAR(50),
    summary         TEXT,         -- 通话小结
    satisfaction    INT,          -- 满意度评分 1-5
    recording_id    VARCHAR(100),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_calls_agent_id ON calls(agent_id);
CREATE INDEX idx_calls_customer_id ON calls(customer_id);
CREATE INDEX idx_calls_created_at ON calls(created_at);
CREATE INDEX idx_calls_status ON calls(status);

-- ==================== IVR 配置 ====================

-- IVR 流程表
CREATE TABLE ivr_flows (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    config      JSONB        NOT NULL, -- IVR 流程 JSON 配置
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    version     INT          NOT NULL DEFAULT 1,
    created_by  BIGINT       REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 语音文件表
CREATE TABLE audio_files (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    file_size   BIGINT,
    duration    INT,          -- 音频时长（秒）
    format      VARCHAR(10),  -- WAV, MP3
    created_by  BIGINT       REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ==================== 工单系统 ====================

-- 工单表
CREATE TABLE tickets (
    id              BIGSERIAL PRIMARY KEY,
    ticket_no       VARCHAR(50)  NOT NULL UNIQUE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    priority        VARCHAR(20)  NOT NULL DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, RESOLVED, CLOSED
    category        VARCHAR(50),
    customer_id     BIGINT       REFERENCES customers(id),
    call_id         VARCHAR(50)  REFERENCES calls(id),
    created_by      BIGINT       REFERENCES users(id),
    assigned_to     BIGINT       REFERENCES users(id),
    resolved_at     TIMESTAMP,
    closed_at       TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_customer_id ON tickets(customer_id);
CREATE INDEX idx_tickets_assigned_to ON tickets(assigned_to);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);

-- 工单状态历史
CREATE TABLE ticket_history (
    id          BIGSERIAL PRIMARY KEY,
    ticket_id   BIGINT       NOT NULL REFERENCES tickets(id),
    old_status  VARCHAR(20),
    new_status  VARCHAR(20)  NOT NULL,
    comment     TEXT,
    operated_by BIGINT       REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ==================== 质检管理 ====================

-- 质检模板表
CREATE TABLE quality_templates (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    items       JSONB        NOT NULL, -- 评分项配置
    total_score INT          NOT NULL DEFAULT 100,
    pass_score  INT          NOT NULL DEFAULT 60,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_by  BIGINT       REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 质检记录表
CREATE TABLE quality_inspections (
    id              BIGSERIAL PRIMARY KEY,
    call_id         VARCHAR(50)  NOT NULL REFERENCES calls(id),
    template_id     BIGINT       REFERENCES quality_templates(id),
    inspector_id    BIGINT       REFERENCES users(id),
    scores          JSONB,        -- 各评分项得分
    total_score     INT,
    passed          BOOLEAN,
    notes           TEXT,
    suggestions     TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ==================== 报表 ====================

-- 报表记录表
CREATE TABLE reports (
    id              BIGSERIAL PRIMARY KEY,
    report_type     VARCHAR(50)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    start_date      DATE,
    end_date        DATE,
    parameters      JSONB,
    file_path       VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING, GENERATING, COMPLETED, FAILED
    created_by      BIGINT       REFERENCES users(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP
);

-- ==================== 系统配置 ====================

-- 系统配置表
CREATE TABLE system_configs (
    id          BIGSERIAL PRIMARY KEY,
    config_key  VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT        NOT NULL,
    description VARCHAR(500),
    updated_by  BIGINT       REFERENCES users(id),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ==================== 初始化基础数据 ====================

-- 插入角色
INSERT INTO roles (code, name, description) VALUES
('ADMIN',       '管理员',   '系统管理员，拥有所有权限'),
('SUPERVISOR',  '座席主管', '管理所属团队座席，查看团队数据'),
('AGENT',       '座席',     '处理客户呼叫，创建工单'),
('INSPECTOR',   '质检员',   '对通话进行质量检查和评分');

-- 插入默认管理员（密码：Admin@2025，需在应用层加密）
INSERT INTO users (username, password, real_name, email, role_id) VALUES
('admin', '$2a$10$placeholder_bcrypt_hash', '系统管理员', 'admin@qianniuyun.com',
 (SELECT id FROM roles WHERE code = 'ADMIN'));

-- 插入默认技能组
INSERT INTO skill_groups (code, name, description) VALUES
('GENERAL',   '通用客服',   '处理一般客户咨询'),
('TECH',      '技术支持',   '处理技术问题'),
('COMPLAINT', '投诉处理',   '处理客户投诉'),
('VIP',       'VIP专属',    '服务VIP客户');

-- 插入系统配置
INSERT INTO system_configs (config_key, config_value, description) VALUES
('ivr.welcome.audio',           'welcome.wav',  'IVR欢迎语音文件'),
('queue.max.wait.seconds',      '300',          '队列最大等待时长（秒）'),
('agent.wrapup.timeout.seconds','180',          '座席整理超时时长（秒）'),
('recording.retention.days',    '90',           '录音保留天数'),
('recording.encryption.enabled','true',         '是否启用录音加密'),
('satisfaction.timeout.seconds','15',           '满意度评价超时时长（秒）'),
('ticket.overdue.hours',        '24',           '工单超时提醒时长（小时）'),
('queue.alert.threshold',       '10',           '队列预警阈值（呼叫数）'),
('report.schedule.time',        '08:00',        '定时报表发送时间'),
('api.rate.limit.per.minute',   '100',          'API每分钟调用限制');

-- 插入默认质检模板
INSERT INTO quality_templates (name, description, items, total_score, pass_score, created_by) VALUES
('标准质检模板', '通用通话质量评估模板',
 '[
   {"id": 1, "name": "服务态度", "maxScore": 30, "description": "语气友好、耐心解答"},
   {"id": 2, "name": "业务能力", "maxScore": 30, "description": "准确解答客户问题"},
   {"id": 3, "name": "沟通技巧", "maxScore": 20, "description": "表达清晰、逻辑清楚"},
   {"id": 4, "name": "规范用语", "maxScore": 10, "description": "使用标准服务用语"},
   {"id": 5, "name": "处理效率", "maxScore": 10, "description": "及时有效解决问题"}
 ]'::jsonb,
 100, 60,
 (SELECT id FROM users WHERE username = 'admin'));

-- 创建索引
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_agent_status_history_agent_id ON agent_status_history(agent_id);
CREATE INDEX idx_quality_inspections_call_id ON quality_inspections(call_id);
CREATE INDEX idx_ticket_history_ticket_id ON ticket_history(ticket_id);
