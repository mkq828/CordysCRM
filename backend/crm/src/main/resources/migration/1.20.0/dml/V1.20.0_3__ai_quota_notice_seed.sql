SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 单日成本熔断 → 平台管理员（org 100001）站内信告警
-- 通知框架按 sys_message_task 的 task_type + event 匹配后才投递，缺这行熔断告警会静默丢失
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'AI_QUOTA_BREAK', 'SYSTEM', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 3, 'admin', UNIX_TIMESTAMP() * 1000 + 3);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
