-- 租户管理员（org_admin）默认拥有「升级/续费申请」权限，可自行在角色权限里配置给其他角色
SET SESSION innodb_lock_wait_timeout = 7200;

INSERT INTO sys_role_permission
(id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'TENANT_PLAN:APPLY');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
