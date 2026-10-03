SET SESSION innodb_lock_wait_timeout = 7200;

-- 智能通话复盘（功能 9 · 第一版）：录音回流 -> 语音转写 -> AI 复盘 -> 落跟进
-- 通话记录 + 复盘结果，状态机：PENDING_TRANSCRIBE -> TRANSCRIBING -> ANALYZING -> DONE / FAILED
CREATE TABLE ai_call_record
(
    id                   VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id      VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    supplier             VARCHAR(64) COMMENT '外呼供应商名(回调带入；手动上传为空)',
    caller               VARCHAR(32) COMMENT '主叫号码',
    callee               VARCHAR(32) COMMENT '被叫号码',
    customer_phone       VARCHAR(32) COMMENT '客户号码(用于匹配客户)',
    customer_id          VARCHAR(32) COMMENT '关联客户ID(可空)',
    call_time            BIGINT COMMENT '通话时间(毫秒)',
    duration             INT COMMENT '通话时长(秒)',
    record_url           VARCHAR(512) COMMENT '录音公网URL(回调带入或手动粘贴)',
    record_attachment_id VARCHAR(64) COMMENT '手动上传的录音附件ID(可空)',
    transcript           TEXT COMMENT '语音转写文本',
    review_json          TEXT COMMENT '复盘结果JSON(结构=会话军师结构化字段)',
    status               VARCHAR(32)  NOT NULL DEFAULT 'PENDING_TRANSCRIBE' COMMENT '状态',
    asr_task_id          VARCHAR(64) COMMENT 'ASR任务ID(重试用)',
    error_msg            VARCHAR(512) COMMENT '失败原因',
    create_time          BIGINT COMMENT '创建时间(毫秒)',
    update_time          BIGINT COMMENT '更新时间(毫秒)',
    create_user          VARCHAR(32) COMMENT '创建人',
    update_user          VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_time (organization_id, call_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 租户回调配置：appKey/secretKey + 字段映射（标准字段 -> 供应商字段名）
CREATE TABLE ai_call_review_config
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id VARCHAR(50) NOT NULL COMMENT '租户组织ID(唯一)',
    app_key         VARCHAR(64) NOT NULL COMMENT '回调AppKey(拼进回调地址)',
    secret_key      VARCHAR(64) NOT NULL COMMENT '回调签名密钥(HMAC-SHA256)',
    field_mapping   TEXT COMMENT '标准字段->供应商字段名映射JSON',
    enable          TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否启用回调',
    create_time     BIGINT COMMENT '创建时间(毫秒)',
    update_time     BIGINT COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org (organization_id),
    UNIQUE KEY uk_app_key (app_key)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
