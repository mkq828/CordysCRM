-- 注册申请单
CREATE TABLE sys_register_application
(
    `id`                             VARCHAR(32)  NOT NULL COMMENT 'id',
    `type`                           VARCHAR(20)  NOT NULL COMMENT '注册类型(PERSONAL/ENTERPRISE)',
    `name`                           VARCHAR(255) NOT NULL COMMENT '主体名称(个人姓名/企业名称)',
    `phone`                          VARCHAR(11)  NOT NULL COMMENT '手机号',
    `password`                       VARCHAR(64)  NOT NULL COMMENT '密码(md5)',
    `id_card`                        VARCHAR(256) COMMENT '身份证号(AES加密)',
    `id_card_hash`                   VARCHAR(64)  COMMENT '身份证号md5',
    `unified_social_credit_code`     VARCHAR(64)  COMMENT '统一社会信用代码',
    `legal_person_name`              VARCHAR(255) COMMENT '法人姓名',
    `business_license_attachment_id` VARCHAR(32)  COMMENT '营业执照附件ID',
    `verify_status`                  VARCHAR(20)  NOT NULL COMMENT '审核状态(PENDING/APPROVED/REJECTED)',
    `verify_remark`                  VARCHAR(500) COMMENT '审核备注',
    `verify_user`                    VARCHAR(32)  COMMENT '审核人',
    `verify_time`                    BIGINT       COMMENT '审核时间',
    `create_time`                    BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`                    BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`                    VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`                    VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (id)
) COMMENT = '注册申请单'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_register_application_phone ON sys_register_application (phone ASC);
CREATE INDEX idx_register_application_id_card_hash ON sys_register_application (id_card_hash ASC);
CREATE INDEX idx_register_application_credit_code ON sys_register_application (unified_social_credit_code ASC);

-- 组织扩展字段（用于区分个人/企业租户及企业实名信息）
ALTER TABLE sys_organization
    ADD COLUMN `org_type` VARCHAR(20) COMMENT '组织类型(PERSONAL/ENTERPRISE)',
    ADD COLUMN `unified_social_credit_code` VARCHAR(64) COMMENT '统一社会信用代码',
    ADD COLUMN `legal_person_name` VARCHAR(255) COMMENT '法人姓名',
    ADD COLUMN `legal_person_id_card` VARCHAR(256) COMMENT '法人身份证号(AES加密)',
    ADD COLUMN `legal_person_id_card_hash` VARCHAR(64) COMMENT '法人身份证号md5',
    ADD COLUMN `business_license_attachment_id` VARCHAR(32) COMMENT '营业执照附件ID';

-- 用户实名扩展字段（个人实名）
ALTER TABLE sys_user
    ADD COLUMN `id_card` VARCHAR(256) COMMENT '身份证号(AES加密)',
    ADD COLUMN `id_card_hash` VARCHAR(64) COMMENT '身份证号md5';

CREATE INDEX idx_user_id_card_hash ON sys_user (id_card_hash ASC);
CREATE INDEX idx_organization_credit_code ON sys_organization (unified_social_credit_code ASC);
