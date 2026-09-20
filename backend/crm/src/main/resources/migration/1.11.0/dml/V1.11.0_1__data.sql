-- set innodb lock wait timeout

SET SESSION innodb_lock_wait_timeout = 7200;

-- 注册审核仅 admin 可见：撤销 org_admin 角色的注册审核权限授予
-- （admin 账号在 PermissionUtils 中硬编码绕过权限校验，仍可访问）
DELETE FROM sys_role_permission
WHERE role_id = 'org_admin'
  AND permission_id IN ('SYS_REGISTER_AUDIT:READ', 'SYS_REGISTER_AUDIT:APPROVE', 'SYS_REGISTER_AUDIT:REJECT');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
