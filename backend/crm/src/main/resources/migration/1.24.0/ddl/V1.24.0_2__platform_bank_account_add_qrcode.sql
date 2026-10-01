SET SESSION innodb_lock_wait_timeout = 7200;

-- 平台收款账号增加收款码图片附件ID（仅微信 WECHAT 使用，租户自助续费/升级时展示扫码付款）
alter table platform_bank_account
    add qrcode varchar(255) null comment '收款码图片附件ID（仅 WECHAT 使用）' after bank_name;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
