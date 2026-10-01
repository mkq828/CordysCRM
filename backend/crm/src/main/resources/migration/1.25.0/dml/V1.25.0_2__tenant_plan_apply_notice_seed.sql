SET SESSION innodb_lock_wait_timeout = 7200;

-- 续费/升级申请通知模板（org 100001 平台级模板，admin 在消息设置里改通道开关与文案）
-- 通知框架按 sys_message_task 的 task_type + event 匹配后才投递，缺这些行对应通知会静默丢失：
--   1) TENANT_PLAN_APPLY     租户提交申请 → 通知平台管理员（补种，原漏种导致该通知一直没发出）
--   2) TENANT_PLAN_APPROVED  核销通过 → 通知租户管理员
--   3) TENANT_PLAN_REJECTED  驳回 → 通知租户管理员（含驳回原因）
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'TENANT_PLAN_APPLY', 'SYSTEM', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 5, 'admin', UNIX_TIMESTAMP() * 1000 + 5);

INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'TENANT_PLAN_APPROVED', 'SYSTEM', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 6, 'admin', UNIX_TIMESTAMP() * 1000 + 6);

INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'TENANT_PLAN_REJECTED', 'SYSTEM', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 7, 'admin', UNIX_TIMESTAMP() * 1000 + 7);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
