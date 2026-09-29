SET SESSION innodb_lock_wait_timeout = 7200;

-- 租户套餐开通/续费/升级历史：付费用户详情页展示成交价与版本变更轨迹
CREATE TABLE tenant_plan_history
(
    id              VARCHAR(32)    NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)    NOT NULL COMMENT '租户组织ID',
    action          VARCHAR(16)    NOT NULL COMMENT '动作:OPEN=开通/续费 UPGRADE=升级',
    from_version    VARCHAR(32)    COMMENT '变更前版本(BASIC/PRO/ENTERPRISE)',
    to_version      VARCHAR(32)    NOT NULL COMMENT '变更后版本(BASIC/PRO/ENTERPRISE)',
    price           DECIMAL(12, 2) COMMENT '本次成交价(元)',
    expire_time     BIGINT         COMMENT '本次后到期时间(毫秒)',
    remark          VARCHAR(255)   COMMENT '备注',
    create_time     BIGINT         COMMENT '创建时间(毫秒)',
    update_time     BIGINT         COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)    COMMENT '创建人',
    update_user     VARCHAR(32)    COMMENT '操作人',
    PRIMARY KEY (id),
    KEY idx_history_org_time (organization_id, create_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci COMMENT ='租户套餐开通/续费/升级历史';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
