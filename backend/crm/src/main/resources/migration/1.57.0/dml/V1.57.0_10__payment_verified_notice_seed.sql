SET SESSION innodb_lock_wait_timeout = 7200;

-- 回款核销通知模板（org 100001 平台级模板，admin 在消息设置里改通道开关与文案）
-- 首款/合同款核销（平台收费-回款）后通知租户管理员「钱已到账」；续费核销已由 TENANT_PLAN_APPROVED 覆盖。
-- 幂等：同 event + organization_id 已存在则跳过，重复执行不重复插入。
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), 'PAYMENT_VERIFIED', 'SYSTEM', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 10, 'admin', UNIX_TIMESTAMP() * 1000 + 10
WHERE NOT EXISTS (
    SELECT 1 FROM sys_message_task WHERE event = 'PAYMENT_VERIFIED' AND organization_id = '100001'
);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
