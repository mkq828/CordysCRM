-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 收款账户扩展收款方式（银行卡/微信/支付宝）与收款二维码
ALTER TABLE bank_account
    ADD COLUMN `type` VARCHAR(32) NOT NULL DEFAULT 'BANK_CARD' COMMENT '收款方式',
    ADD COLUMN `qrcode` VARCHAR(255) COMMENT '收款二维码(附件ID)';

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
