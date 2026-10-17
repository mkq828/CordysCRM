SET SESSION innodb_lock_wait_timeout = 7200;

-- 客户标签：标签字典（租户级）+ 客户-标签关联，供 AI 自动打标签与客户列表按标签筛选。
-- 「分群」= 客户列表按标签筛选，不单独建 segmentation 表。

CREATE TABLE customer_tag
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    name            VARCHAR(64) NOT NULL COMMENT '标签名',
    color           VARCHAR(32) COMMENT '标签颜色(十六进制)',
    category        VARCHAR(64) COMMENT '标签分类',
    built_in        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否内置标签(1=内置,不可删)',
    create_time     BIGINT COMMENT '创建时间(毫秒)',
    update_time     BIGINT COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_name (organization_id, name),
    KEY idx_org (organization_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE customer_tag_rel
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    customer_id     VARCHAR(32) NOT NULL COMMENT '客户ID',
    tag_id          VARCHAR(32) NOT NULL COMMENT '标签ID',
    source          VARCHAR(32) COMMENT '来源(manual/AI)',
    create_time     BIGINT COMMENT '创建时间(毫秒)',
    create_user     VARCHAR(32) COMMENT '创建人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_tag (customer_id, tag_id),
    KEY idx_tag (tag_id),
    KEY idx_customer (customer_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
