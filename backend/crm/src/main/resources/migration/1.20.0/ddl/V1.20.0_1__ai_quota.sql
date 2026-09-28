SET SESSION innodb_lock_wait_timeout = 7200;

-- 版本表加 AI 月度配额（标准次数），admin 可在版本配置里调整；已开通租户按 tenant_edition 快照
ALTER TABLE sys_edition
    ADD COLUMN `ai_monthly_quota` INT NOT NULL DEFAULT 0 COMMENT 'AI月度配额(标准次数)' AFTER `validity_days`;

-- 租户版本快照冗余 AI 月度配额，开通时快照，admin 改版本配额只影响新开通租户
ALTER TABLE tenant_edition
    ADD COLUMN `ai_monthly_quota` INT NOT NULL DEFAULT 0 COMMENT 'AI月度配额快照(标准次数)' AFTER `features_json`;

-- AI 用量明细：每次模型调用一条，input/output token 分开，折算标准次数
CREATE TABLE ai_usage_record
(
    id              VARCHAR(32)    NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)    NOT NULL COMMENT '租户组织ID',
    feature_code    VARCHAR(64)    NOT NULL COMMENT 'AI功能编码(sys_feature.feature_code)',
    model_code      VARCHAR(64)    NOT NULL COMMENT '模型编码(ai_model_price.model_code)',
    input_tokens    BIGINT         NOT NULL DEFAULT 0 COMMENT '输入token数',
    output_tokens   BIGINT         NOT NULL DEFAULT 0 COMMENT '输出token数',
    total_tokens    BIGINT         NOT NULL DEFAULT 0 COMMENT '总token数',
    cost_calls      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '折算标准次数',
    status          VARCHAR(16)    NOT NULL DEFAULT 'NORMAL' COMMENT '当次额度状态:NORMAL/SOFT_LIMITED/HARD_LIMITED',
    create_time     BIGINT         COMMENT '创建时间(毫秒)',
    update_time     BIGINT         COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)    COMMENT '创建人',
    update_user     VARCHAR(32)    COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_time (organization_id, create_time),
    KEY idx_org_feature (organization_id, feature_code)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- AI 月度累计：按 org+period 记本月已用次数，天然按月重置（新月份新 period 从 0 起）
CREATE TABLE ai_quota_usage
(
    id              VARCHAR(32)    NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)    NOT NULL COMMENT '租户组织ID',
    period          VARCHAR(7)     NOT NULL COMMENT '月份(yyyy-MM)',
    used_calls      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '本月已用标准次数',
    create_time     BIGINT         COMMENT '创建时间(毫秒)',
    update_time     BIGINT         COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)    COMMENT '创建人',
    update_user     VARCHAR(32)    COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_period (organization_id, period)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- AI 模型单价：成本看板折算用，admin 手动维护
CREATE TABLE ai_model_price
(
    id           VARCHAR(32)    NOT NULL COMMENT '主键',
    model_code   VARCHAR(64)    NOT NULL COMMENT '模型编码',
    model_name   VARCHAR(64)    NOT NULL COMMENT '模型名称',
    input_price  DECIMAL(12, 6) NOT NULL DEFAULT 0 COMMENT '输入单价(元/1k token)',
    output_price DECIMAL(12, 6) NOT NULL DEFAULT 0 COMMENT '输出单价(元/1k token)',
    status       TINYINT        NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    create_time  BIGINT         COMMENT '创建时间(毫秒)',
    update_time  BIGINT         COMMENT '更新时间(毫秒)',
    create_user  VARCHAR(32)    COMMENT '创建人',
    update_user  VARCHAR(32)    COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_code (model_code)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
