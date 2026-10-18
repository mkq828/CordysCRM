SET SESSION innodb_lock_wait_timeout = 7200;

-- 竞品动态：租户内用户维护关注的竞品（竞品名/关键词），供定时+手动联网搜索竞品最新动态；
-- 搜索结果按 URL 去重落库，新结果站内信推给订阅人。

CREATE TABLE ai_competitor_subscription
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    user_id         VARCHAR(32) NOT NULL COMMENT '订阅人用户ID',
    competitor      VARCHAR(128) NOT NULL COMMENT '竞品名称',
    keyword         VARCHAR(128) COMMENT '补充关键词',
    enabled         TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否启用(1=启用,0=停用)',
    create_time     BIGINT COMMENT '创建时间(毫秒)',
    update_time     BIGINT COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org (organization_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE ai_competitor_result
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    subscription_id VARCHAR(32) NOT NULL COMMENT '订阅ID',
    title           VARCHAR(255) NOT NULL COMMENT '动态标题',
    url             VARCHAR(512) NOT NULL COMMENT '来源链接',
    snippet         TEXT COMMENT '摘要',
    content         TEXT COMMENT '正文(可空)',
    create_time     BIGINT COMMENT '创建时间(毫秒)',
    update_time     BIGINT COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sub_url (subscription_id, url),
    KEY idx_org (organization_id),
    KEY idx_sub (subscription_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
