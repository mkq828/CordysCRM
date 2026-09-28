SET SESSION innodb_lock_wait_timeout = 7200;

-- 租户归属字段（业绩口径主表：sys_organization）
-- sign_manager_id 签约城市经理(永久) / follow_manager_id 跟进城市经理(可转移)
ALTER TABLE sys_organization
    ADD COLUMN `sign_manager_id`   VARCHAR(32) COMMENT '签约城市经理(永久)' AFTER `business_license_attachment_id`,
    ADD COLUMN `follow_manager_id` VARCHAR(32) COMMENT '跟进城市经理(可转移)' AFTER `sign_manager_id`;

CREATE INDEX idx_org_sign_manager ON sys_organization (sign_manager_id ASC);
CREATE INDEX idx_org_follow_manager ON sys_organization (follow_manager_id ASC);

-- 平台合同表按经理聚合用索引（字段 1.17.0 已建，这里补索引）
CREATE INDEX idx_pc_sign_manager ON platform_contract (sign_manager_id ASC);
CREATE INDEX idx_pc_follow_manager ON platform_contract (follow_manager_id ASC);

-- 城市经理账号（平台员工档案，登录主体复用 sys_user，此处只存状态/快照）
CREATE TABLE platform_city_manager
(
    id          VARCHAR(32) NOT NULL COMMENT '主键=sys_user.id',
    name        VARCHAR(64) COMMENT '姓名(快照)',
    phone       VARCHAR(32) COMMENT '手机号(快照,登录标识)',
    status      VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED在职/DISABLED离职',
    create_time BIGINT      COMMENT '创建时间',
    update_time BIGINT      COMMENT '更新时间',
    create_user VARCHAR(32) COMMENT '创建人',
    update_user VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_pcm_status (status)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
