SET SESSION innodb_lock_wait_timeout = 7200;

-- 注册审核结果通知补开短信通道（平台组织 100001 级模板）：
--   - 通过 ENTERPRISE_REGISTER_APPROVED：站内信 + 短信双重触达
--   - 驳回 ENTERPRISE_REGISTER_REJECTED：企业注册驳回时租户无账号、站内信送不到，短信是唯一触达渠道
-- 短信真实发送在上线前接入阿里云后生效（见 SmsSender 骨架），此处只把通道开关打开。
UPDATE sys_message_task
SET sms_enable = b'1'
WHERE event IN ('ENTERPRISE_REGISTER_APPROVED', 'ENTERPRISE_REGISTER_REJECTED')
  AND organization_id = '100001';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
