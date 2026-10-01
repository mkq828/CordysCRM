SET SESSION innodb_lock_wait_timeout = 7200;

-- 租户续费/升级申请：租户管理员在个人中心自助提交（选版本+付款方式+付款凭证），
-- admin/城市合伙人在付费用户管理核销（确认收款后调开通/升级），核销后可补发合同回填 contract_id
CREATE TABLE tenant_plan_application
(
    id              VARCHAR(32)   NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)   NOT NULL COMMENT '租户组织ID',
    org_name        VARCHAR(128) COMMENT '租户名（冗余，列表展示）',
    current_version VARCHAR(32) COMMENT '当前版本',
    target_version  VARCHAR(32)   NOT NULL COMMENT '目标版本编码(BASIC/PRO/ENTERPRISE)',
    amount          DECIMAL(12, 2) COMMENT '成交价',
    price_detail    VARCHAR(500) COMMENT '金额计算过程',
    validity_days   INT COMMENT '有效期天数',
    payment_type    VARCHAR(32) COMMENT '付款方式：WECHAT微信/TRANSFER对公转账/ALIPAY支付宝/OFFLINE线下',
    voucher_ids     VARCHAR(1000) COMMENT '付款凭证附件ID（逗号分隔）',
    status          VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待核销/APPROVED已核销/CANCELLED已驳回',
    contract_id     VARCHAR(32) COMMENT '核销后补发合同ID（可空）',
    remark          VARCHAR(255) COMMENT '备注',
    create_time     BIGINT COMMENT '创建时间',
    update_time     BIGINT COMMENT '更新时间',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_tenant_plan_application_org (organization_id),
    KEY idx_tenant_plan_application_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
