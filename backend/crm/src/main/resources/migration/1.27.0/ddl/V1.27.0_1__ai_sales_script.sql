SET SESSION innodb_lock_wait_timeout = 7200;

-- 销售话术库（功能 2）：租户沉淀自己的销售话术/常见异议应答，供 AI 按客户情况检索并改写推荐
CREATE TABLE ai_sales_script
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    category        VARCHAR(64)  COMMENT '话术分类(开场白/价格异议/竞品对比/促单逼单等)',
    title           VARCHAR(128) NOT NULL COMMENT '话术标题',
    content         TEXT         NOT NULL COMMENT '话术内容',
    source          VARCHAR(255) COMMENT '出处/来源(默认 租户自建)',
    create_time     BIGINT       COMMENT '创建时间(毫秒)',
    update_time     BIGINT       COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)  COMMENT '创建人',
    update_user     VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org (organization_id),
    KEY idx_org_category (organization_id, category)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
