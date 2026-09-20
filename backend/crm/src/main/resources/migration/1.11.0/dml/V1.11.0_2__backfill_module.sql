SET SESSION innodb_lock_wait_timeout = 7200;

-- 回填存量租户缺失的模块（order/contract/dashboard/agent/customForm/tender 等后加模块）
-- 此前注册开通的新租户仅用 ModuleKey 初始化 6 个基础模块，缺少默认组织 100001 中后加的模块
INSERT INTO sys_module (id, organization_id, module_key, enable, pos, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), o.id, m.module_key, m.enable, m.pos, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_organization o
JOIN sys_module m ON m.organization_id = '100001'
WHERE o.id <> '100001'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_module x
      WHERE x.organization_id = o.id
        AND x.module_key = m.module_key
  );

SET SESSION innodb_lock_wait_timeout = DEFAULT;
