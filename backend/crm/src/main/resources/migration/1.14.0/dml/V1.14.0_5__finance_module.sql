SET SESSION innodb_lock_wait_timeout = 7200;

-- 财务模块：默认组织 100001 新增，存量租户回填
INSERT INTO sys_module (id, organization_id, module_key, enable, pos, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), '100001', 'finance', 1, 12, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

INSERT INTO sys_module (id, organization_id, module_key, enable, pos, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), o.id, m.module_key, m.enable, m.pos, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_organization o
JOIN sys_module m ON m.organization_id = '100001' AND m.module_key = 'finance'
WHERE o.id <> '100001'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_module x
      WHERE x.organization_id = o.id
        AND x.module_key = 'finance'
  );

SET SESSION innodb_lock_wait_timeout = DEFAULT;
