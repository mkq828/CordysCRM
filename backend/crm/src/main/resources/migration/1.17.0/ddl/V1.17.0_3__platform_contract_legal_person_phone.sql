SET SESSION innodb_lock_wait_timeout = 7200;

-- 平台合同补充「法人联系方式」：线下签约/归档时需要联系法人
ALTER TABLE platform_contract
    ADD COLUMN legal_person_phone VARCHAR(32) COMMENT '租户主体信息-法人联系方式' AFTER legal_person;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
