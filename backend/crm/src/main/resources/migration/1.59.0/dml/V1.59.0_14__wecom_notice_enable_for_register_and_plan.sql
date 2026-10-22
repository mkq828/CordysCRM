SET SESSION innodb_lock_wait_timeout = 7200;

-- 打开企业微信通道：企业注册申请 → 通知平台管理员（admin）
-- 原种子只开了 sys_enable 站内信，企微通道默认关闭；此处补开 we_com_enable。
-- 真正送达还需：组织 100001 在「企业设置-企业微信」填好群机器人 Webhook 地址（无需 corpId/agentId/appSecret/可信IP）。
UPDATE sys_message_task
SET we_com_enable = b'1'
WHERE event IN ('ENTERPRISE_REGISTER_APPLY')
  AND organization_id = '100001';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
