-- 付费用户管理：免费试用天数 / 到期提醒天数（全局参数）
INSERT INTO sys_parameter (param_key, param_value, type)
VALUES ('register.freeTrialDays', '180', 'text'),
       ('register.expireRemindDays', '30,7', 'text');

-- 平台管理员（org 100001）站内信通知：企业注册申请、租户到期提醒
INSERT INTO sys_message_task (id, event, task_type, email_enable, sys_enable, organization_id, template, create_user, create_time, update_user, update_time)
VALUES
    (UUID_SHORT(), 'ENTERPRISE_REGISTER_APPLY', 'SYSTEM', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2),
    (UUID_SHORT(), 'PLAN_EXPIRE_REMIND', 'SYSTEM', false, true, '100001', null, 'admin', UNIX_TIMESTAMP() * 1000 + 2, 'admin', UNIX_TIMESTAMP() * 1000 + 2);
