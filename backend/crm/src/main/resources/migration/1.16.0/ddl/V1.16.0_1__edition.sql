SET SESSION innodb_lock_wait_timeout = 7200;

-- 版本表：可配置的套餐版本（code/名称/年价/首年促销价/软上限/有效期/排序/状态）
CREATE TABLE sys_edition
(
    id                VARCHAR(32)    NOT NULL COMMENT '主键',
    code              VARCHAR(32)    NOT NULL COMMENT '版本编码：BASIC/PRO/ENTERPRISE',
    name              VARCHAR(50)    NOT NULL COMMENT '版本名称',
    year_price        DECIMAL(12, 2) COMMENT '年价',
    first_year_price  DECIMAL(12, 2) COMMENT '首年促销价',
    soft_limit        INT            COMMENT '软上限(人数)',
    validity_days     INT            NOT NULL DEFAULT 365 COMMENT '有效期天数',
    sort              INT            NOT NULL DEFAULT 0 COMMENT '排序',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_time       BIGINT         COMMENT '创建时间',
    update_time       BIGINT         COMMENT '更新时间',
    create_user       VARCHAR(32)    COMMENT '创建人',
    update_user       VARCHAR(32)    COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_edition_code (code)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 功能表：可配置的功能点（feature_code/名称/分类/全局开关）
CREATE TABLE sys_feature
(
    id           VARCHAR(32) NOT NULL COMMENT '主键',
    feature_code VARCHAR(64) NOT NULL COMMENT '功能编码',
    name         VARCHAR(50) NOT NULL COMMENT '功能名称',
    category     VARCHAR(32) COMMENT '分类',
    enable       BIT(1)      NOT NULL DEFAULT 1 COMMENT '全局开关：1启用 0停用',
    create_time  BIGINT      COMMENT '创建时间',
    update_time  BIGINT      COMMENT '更新时间',
    create_user  VARCHAR(32) COMMENT '创建人',
    update_user  VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_feature_code (feature_code)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 版本-功能关联表（多对多）
CREATE TABLE sys_edition_feature
(
    id          VARCHAR(32) NOT NULL COMMENT '主键',
    edition_id  VARCHAR(32) NOT NULL COMMENT '版本ID',
    feature_id  VARCHAR(32) NOT NULL COMMENT '功能ID',
    create_time BIGINT      COMMENT '创建时间',
    update_time BIGINT      COMMENT '更新时间',
    create_user VARCHAR(32) COMMENT '创建人',
    update_user VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_edition_feature (edition_id, feature_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 租户版本快照表：开通时快照版本名/价格/功能集合/起止时间/状态
CREATE TABLE tenant_edition
(
    id              VARCHAR(32)    NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)    NOT NULL COMMENT '租户组织ID',
    edition_code    VARCHAR(32)    NOT NULL COMMENT '版本编码',
    edition_name    VARCHAR(50)    NOT NULL COMMENT '版本名称(快照)',
    price           DECIMAL(12, 2) COMMENT '成交价(快照)',
    features_json   TEXT           COMMENT '功能集合JSON(快照)',
    start_time      BIGINT         COMMENT '开通时间(毫秒)',
    expire_time     BIGINT         COMMENT '到期时间(毫秒)',
    status          VARCHAR(32)    NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/EXPIRED',
    create_time     BIGINT         COMMENT '创建时间',
    update_time     BIGINT         COMMENT '更新时间',
    create_user     VARCHAR(32)    COMMENT '创建人',
    update_user     VARCHAR(32)    COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_edition_org (organization_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
