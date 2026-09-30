SET SESSION innodb_lock_wait_timeout = 7200;

-- 通知体系补短信通道：sys_message_task 增加短信开关（默认关，上线前接入阿里云后开启）
alter table sys_message_task
    add sms_enable bit default b'0' null comment '短信启用' after lark_enable;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
