SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 月度额度租户覆盖：admin 按租户单独调额，覆盖套餐快照/试用配额；0=停用AI
CREATE TABLE ai_quota_override
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    quota           INT         NOT NULL COMMENT '覆盖后的AI月度额度(标准次数)，0=停用AI',
    create_time     BIGINT      COMMENT '创建时间(毫秒)',
    update_time     BIGINT      COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_quota_override_org (organization_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
