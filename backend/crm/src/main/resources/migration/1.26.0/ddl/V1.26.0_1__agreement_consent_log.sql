SET SESSION innodb_lock_wait_timeout = 7200;

-- 协议勾选留痕：记录用户在注册页主动勾选《SaaS服务协议/知识产权与保密/隐私政策》的动作，
-- 留存用户ID、手机号、协议版本、勾选时间、IP、浏览器信息（至少保留 3 年）
CREATE TABLE sys_agreement_consent_log
(
    id                VARCHAR(32) NOT NULL COMMENT '主键',
    user_id           VARCHAR(32) COMMENT '用户ID（企业注册审核通过前为空）',
    phone             VARCHAR(32) COMMENT '手机号（注册提交时即已知，用于追溯）',
    agreement_type    VARCHAR(32) COMMENT '协议类型（SAAS_SERVICE）',
    agreement_version VARCHAR(32) COMMENT '协议版本（如 v1.0）',
    ip                VARCHAR(64) COMMENT '客户端IP',
    user_agent        VARCHAR(512) COMMENT '浏览器User-Agent',
    create_time       BIGINT      NOT NULL COMMENT '勾选时间',
    PRIMARY KEY (id),
    KEY idx_agreement_consent_user (user_id),
    KEY idx_agreement_consent_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
