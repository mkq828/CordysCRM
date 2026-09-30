SET SESSION innodb_lock_wait_timeout = 7200;

-- 企业注册审核结果 → 通知租户（org 100001 的平台级模板，由 admin 在消息设置里改通道开关与文案）
-- 通知框架按 sys_message_task 的 task_type + event 匹配后才投递，缺这两行审核通过/驳回通知会静默丢失
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'ENTERPRISE_REGISTER_APPROVED', 'SYSTEM', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 3, 'admin', UNIX_TIMESTAMP() * 1000 + 3);

INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'ENTERPRISE_REGISTER_REJECTED', 'SYSTEM', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 4, 'admin', UNIX_TIMESTAMP() * 1000 + 4);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
