SET SESSION innodb_lock_wait_timeout = 7200;

-- 线索爬取（功能 10）：企查查企业工商信息公开信息缓存表（平台级，跨租户共享去重）
-- 命中的企业详情直接从这里返回，不再重复调用企查查，节省租户 API 额度与费用
CREATE TABLE qcc_enterprise_info
(
    id             VARCHAR(32)  NOT NULL COMMENT '主键',
    name           VARCHAR(255) NOT NULL COMMENT '企业名称',
    tax_no         VARCHAR(64)  COMMENT '纳税人识别号',
    credit_code    VARCHAR(64)  COMMENT '统一社会信用代码',
    legal_person   VARCHAR(64)  COMMENT '法定代表人',
    register_capi  VARCHAR(64)  COMMENT '注册资本',
    person_scope   VARCHAR(64)  COMMENT '公司规模(人数)',
    scale          VARCHAR(64)  COMMENT '企业规模',
    status         VARCHAR(64)  COMMENT '经营状态',
    industry       VARCHAR(255) COMMENT '国标行业',
    area_province  VARCHAR(64)  COMMENT '所属省份',
    area_city      VARCHAR(64)  COMMENT '所属城市',
    register_phone VARCHAR(64)  COMMENT '注册电话',
    address        VARCHAR(512) COMMENT '注册地址',
    bank_name      VARCHAR(255) COMMENT '开户银行',
    bank_account   VARCHAR(64)  COMMENT '开户行账号',
    reg_no         VARCHAR(64)  COMMENT '工商注册号',
    source         VARCHAR(16)  COMMENT '数据来源(qcc)',
    create_time    BIGINT COMMENT '创建时间(毫秒)',
    update_time    BIGINT COMMENT '更新时间(毫秒，兼作缓存过期判断基准)',
    create_user    VARCHAR(32) COMMENT '创建人',
    update_user    VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name),
    KEY idx_area (area_province, area_city),
    KEY idx_industry (industry)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
