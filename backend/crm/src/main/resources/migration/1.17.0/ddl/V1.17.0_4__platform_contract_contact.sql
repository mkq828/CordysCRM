SET SESSION innodb_lock_wait_timeout = 7200;

-- 平台合同「法人」更正为「联系人」：字段语义与命名一并调整
ALTER TABLE platform_contract
    CHANGE COLUMN legal_person contact_person VARCHAR(64) COMMENT '联系人',
    CHANGE COLUMN legal_person_phone contact_phone VARCHAR(32) COMMENT '联系方式';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
