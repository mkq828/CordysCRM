CREATE TABLE tenant_plan
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    version         VARCHAR(32) NOT NULL COMMENT '套餐版本：PERSONAL 个人版 / ENTERPRISE 企业版',
    status          VARCHAR(32) NOT NULL DEFAULT 'FREE' COMMENT '状态：FREE 试用中 / ACTIVE 已开通 / EXPIRED 已到期',
    expire_time     BIGINT COMMENT '到期时间(毫秒)',
    remark          VARCHAR(255) COMMENT '备注',
    create_time     BIGINT COMMENT '创建时间',
    update_time     BIGINT COMMENT '更新时间',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_plan_org (organization_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
CREATE INDEX idx_tenant_plan_expire_time ON tenant_plan (expire_time ASC);
