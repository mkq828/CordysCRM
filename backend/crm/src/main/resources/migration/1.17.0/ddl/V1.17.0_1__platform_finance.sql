SET SESSION innodb_lock_wait_timeout = 7200;

-- 平台合同表：平台与租户之间的签约合同（平台账·应收），按 organization_id 绑定租户
CREATE TABLE platform_contract
(
    id                VARCHAR(32)     NOT NULL COMMENT '主键',
    contract_no       VARCHAR(64)     NOT NULL COMMENT '合同编号',
    organization_id   VARCHAR(50)     NOT NULL COMMENT '租户组织ID',
    org_name          VARCHAR(255)    COMMENT '租户主体信息-企业全称(快照)',
    credit_code       VARCHAR(64)     COMMENT '租户主体信息-统一社会信用代码',
    legal_person      VARCHAR(64)     COMMENT '租户主体信息-法人',
    address           VARCHAR(500)    COMMENT '租户主体信息-地址',
    edition_code      VARCHAR(64)     COMMENT '套餐版本编码(快照)',
    edition_name      VARCHAR(128)    COMMENT '套餐版本名称(快照)',
    amount            DECIMAL(20, 10) COMMENT '合同金额(应收)',
    validity_days     INT             COMMENT '订阅时长(天)',
    sign_type         VARCHAR(32)     COMMENT '签署方式：OFFLINE线下签/ONLINE线上签',
    sign_manager_id   VARCHAR(32)     COMMENT '签约城市经理(占位，P3填充)',
    follow_manager_id VARCHAR(32)     COMMENT '当前跟进城市经理(占位，P3填充)',
    status            VARCHAR(32)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿/PENDING_SIGN待签署/COMPLETED已完成/ARCHIVED已归档/VOIDED已作废',
    attachment_ids    VARCHAR(2000)   COMMENT '扫描件附件ID(逗号分隔)',
    remark            VARCHAR(500)    COMMENT '备注',
    create_time       BIGINT          COMMENT '创建时间',
    update_time       BIGINT          COMMENT '更新时间',
    create_user       VARCHAR(32)     COMMENT '创建人',
    update_user       VARCHAR(32)     COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_contract_no (contract_no),
    KEY idx_platform_contract_org (organization_id),
    KEY idx_platform_contract_status (status)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 平台回款表：平台收到的租户回款（平台账·现金流），核销后自动开通企业版
CREATE TABLE platform_payment_record
(
    id                    VARCHAR(32)     NOT NULL COMMENT '主键',
    contract_id           VARCHAR(32)     NOT NULL COMMENT '平台合同ID',
    organization_id       VARCHAR(50)     NOT NULL COMMENT '租户组织ID',
    record_no             VARCHAR(64)     COMMENT '回款单号',
    amount                DECIMAL(20, 10) COMMENT '实收金额(现金流)',
    payment_type          VARCHAR(32)     COMMENT '收款方式：TRANSFER对公转账/ALIPAY支付宝/WECHAT微信/OFFLINE线下',
    bank_account          VARCHAR(128)    COMMENT '收款账户(平台对公账户快照)',
    voucher_attachment_ids VARCHAR(2000)  COMMENT '付款凭证附件ID(逗号分隔)',
    verification_status   VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '核销状态：PENDING待核销/DONE已完成',
    verify_user           VARCHAR(32)     COMMENT '核销人',
    verify_time           BIGINT          COMMENT '核销时间',
    verify_remark         VARCHAR(500)    COMMENT '核销备注',
    verify_proof          VARCHAR(1000)   COMMENT '收款证明附件ID(逗号分隔)',
    revoke_user           VARCHAR(32)     COMMENT '撤回人',
    revoke_time           BIGINT          COMMENT '撤回时间',
    revoke_remark         VARCHAR(500)    COMMENT '撤回备注',
    remark                VARCHAR(500)    COMMENT '备注',
    create_time           BIGINT          COMMENT '创建时间',
    update_time           BIGINT          COMMENT '更新时间',
    create_user           VARCHAR(32)     COMMENT '创建人',
    update_user           VARCHAR(32)     COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_platform_payment_contract (contract_id),
    KEY idx_platform_payment_org (organization_id),
    KEY idx_platform_payment_status (verification_status)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 平台发票表：平台开给租户的发票（平台账·开票），三个金额口径之一
CREATE TABLE platform_invoice
(
    id              VARCHAR(32)     NOT NULL COMMENT '主键',
    contract_id     VARCHAR(32)     NOT NULL COMMENT '平台合同ID',
    organization_id VARCHAR(50)     NOT NULL COMMENT '租户组织ID',
    invoice_no      VARCHAR(64)     COMMENT '发票号',
    invoice_type    VARCHAR(32)     COMMENT '发票类型：NORMAL普票/SPECIAL专票',
    amount          DECIMAL(20, 10) COMMENT '开票金额(发票)',
    tax_rate        DECIMAL(20, 10) DEFAULT 6 COMMENT '税率(%)，默认6',
    tax_amount      DECIMAL(20, 10) COMMENT '税额',
    invoice_status  VARCHAR(32)     NOT NULL DEFAULT 'NOT_INVOICED' COMMENT '开票状态：NOT_INVOICED未开/INVOICED已开/VOIDED作废',
    business_title  VARCHAR(255)    COMMENT '开票抬头',
    remark          VARCHAR(500)    COMMENT '备注',
    create_time     BIGINT          COMMENT '创建时间',
    update_time     BIGINT          COMMENT '更新时间',
    create_user     VARCHAR(32)     COMMENT '创建人',
    update_user     VARCHAR(32)     COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_platform_invoice_contract (contract_id),
    KEY idx_platform_invoice_org (organization_id),
    KEY idx_platform_invoice_status (invoice_status)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
