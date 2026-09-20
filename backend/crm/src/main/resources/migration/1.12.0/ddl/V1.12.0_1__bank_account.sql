-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

CREATE TABLE bank_account
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `name`            VARCHAR(255) NOT NULL COMMENT '账户名称',
    `opening_bank`    VARCHAR(255) COMMENT '开户行',
    `bank_account`    VARCHAR(255) COMMENT '银行账号',
    `account_holder`  VARCHAR(255) COMMENT '户名',
    `remark`          VARCHAR(500) COMMENT '备注',
    `organization_id` VARCHAR(50)  NOT NULL COMMENT '组织id',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '收款账户'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_organization_id ON bank_account (organization_id ASC);
CREATE INDEX idx_name ON bank_account (name ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
