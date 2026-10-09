SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 额度包商品：admin 维护，按功能卖「单项额度包」；feature_code 为空串 = 全功能通用包
CREATE TABLE ai_quota_pack
(
    id           VARCHAR(32)  NOT NULL COMMENT '主键',
    feature_code VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'AI功能编码(sys_feature.feature_code)，空串=全功能通用包',
    name         VARCHAR(128) NOT NULL COMMENT '额度包名称',
    calls        INT          NOT NULL DEFAULT 0 COMMENT '次数',
    price_fen    BIGINT       NOT NULL DEFAULT 0 COMMENT '价格(分)',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    remark       VARCHAR(255) NULL COMMENT '备注',
    create_time  BIGINT       COMMENT '创建时间(毫秒)',
    update_time  BIGINT       COMMENT '更新时间(毫秒)',
    create_user  VARCHAR(32)  COMMENT '创建人',
    update_user  VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_feature_code (feature_code)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- AI 额度包购买/充值单：租户申请 → 客服/admin 确认到账（第一版不做在线支付）
CREATE TABLE ai_quota_pack_order
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    pack_id         VARCHAR(32)  NOT NULL COMMENT '额度包ID(ai_quota_pack.id)',
    feature_code    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'AI功能编码(下单时快照)',
    pack_name       VARCHAR(128) NULL COMMENT '额度包名称(下单时快照)',
    calls           INT          NOT NULL DEFAULT 0 COMMENT '购买次数',
    amount_fen      BIGINT       NOT NULL DEFAULT 0 COMMENT '金额(分)',
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待确认/CONFIRMED已到账/CANCELLED已取消',
    apply_user      VARCHAR(32)  COMMENT '申请人',
    apply_time      BIGINT       COMMENT '申请时间(毫秒)',
    confirm_user    VARCHAR(32)  COMMENT '确认人',
    confirm_time    BIGINT       COMMENT '确认时间(毫秒)',
    remark          VARCHAR(255) NULL COMMENT '备注/取消原因',
    create_time     BIGINT       COMMENT '创建时间(毫秒)',
    update_time     BIGINT       COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)  COMMENT '创建人',
    update_user     VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_status (organization_id, status)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- AI 已购额度包剩余：admin 确认订单时 +calls，生成调用超出月配额时按 feature 扣减；空串=通用包
CREATE TABLE ai_quota_pack_balance
(
    id              VARCHAR(32)   NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)   NOT NULL COMMENT '租户组织ID',
    feature_code    VARCHAR(64)   NOT NULL DEFAULT '' COMMENT 'AI功能编码，空串=通用包',
    remaining_calls DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '剩余次数',
    create_time     BIGINT        COMMENT '创建时间(毫秒)',
    update_time     BIGINT        COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)   COMMENT '创建人',
    update_user     VARCHAR(32)   COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_feature (organization_id, feature_code)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
