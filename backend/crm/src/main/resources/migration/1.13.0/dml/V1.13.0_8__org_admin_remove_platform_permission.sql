-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 企业管理员（org_admin）剥离平台级权限，仅保留企业业务与企业内管理权限。
-- 平台功能（仪表盘/智能体/License/操作日志/系统设置）由平台 admin 账号维护。
DELETE FROM sys_role_permission
WHERE role_id = 'org_admin' AND permission_id IN (
  'AGENT:ADD', 'AGENT:DELETE', 'AGENT:READ', 'AGENT:UPDATE',
  'DASHBOARD:ADD', 'DASHBOARD:DELETE', 'DASHBOARD:READ', 'DASHBOARD:UPDATE',
  'LICENSE:EDIT', 'LICENSE:READ',
  'OPERATION_LOG:READ',
  'SYSTEM_SETTING:ADD', 'SYSTEM_SETTING:DELETE', 'SYSTEM_SETTING:READ', 'SYSTEM_SETTING:UPDATE'
);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
