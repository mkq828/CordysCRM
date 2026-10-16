SET SESSION innodb_lock_wait_timeout = 7200;

-- 经营大屏「老板 AI 4 能力」落库：
-- 1) 督办闭环：老板在经营大屏下达督办 → 被督办人收到站内信 → 到期自动标记逾期并提醒。
-- 2) 晨报/周报：定时任务生成的经营简报，落库供首屏「AI 经营简报」卡读取。

CREATE TABLE sys_supervision_task
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    assignee        VARCHAR(32)  NOT NULL COMMENT '被督办人 userId',
    assignee_name   VARCHAR(64)  COMMENT '被督办人姓名',
    content         VARCHAR(500) NOT NULL COMMENT '督办内容',
    deadline        BIGINT       COMMENT '截止时间(毫秒)',
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/DONE/OVERDUE',
    create_time     BIGINT       COMMENT '创建时间(毫秒)',
    update_time     BIGINT       COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)  COMMENT '创建人(下达督办的老板)',
    update_user     VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_status (organization_id, status)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE sys_ai_briefing
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    type            VARCHAR(32) NOT NULL COMMENT '报告类型: DAILY/WEEKLY',
    period          VARCHAR(32) COMMENT '报告周期(yyyy-MM-dd / yyyy-Www)',
    content         TEXT        COMMENT '简报正文',
    create_time     BIGINT      COMMENT '创建时间(毫秒)',
    update_time     BIGINT      COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_type_time (organization_id, type, create_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
