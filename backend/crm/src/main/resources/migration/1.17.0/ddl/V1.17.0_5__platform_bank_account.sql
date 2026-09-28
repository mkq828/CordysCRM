SET SESSION innodb_lock_wait_timeout = 7200;

-- 平台收款账号表：我方公司各收款方式的收款账户（对公转账/支付宝/微信/线下，每种一条），
-- 新增回款时按「收款方式」自动带出对应收款账户，避免财务每次手动填写
CREATE TABLE platform_bank_account
(
    id           VARCHAR(32)  NOT NULL COMMENT '主键',
    account_type VARCHAR(32)  NOT NULL COMMENT '收款方式：TRANSFER对公转账/ALIPAY支付宝/WECHAT微信/OFFLINE线下',
    account_name VARCHAR(128) COMMENT '户名',
    account_no   VARCHAR(128) COMMENT '账号',
    bank_name    VARCHAR(128) COMMENT '开户行/平台（银行卡填开户行，支付宝/微信填平台名）',
    create_time  BIGINT       COMMENT '创建时间',
    update_time  BIGINT       COMMENT '更新时间',
    create_user  VARCHAR(32)  COMMENT '创建人',
    update_user  VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_bank_account_type (account_type)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
