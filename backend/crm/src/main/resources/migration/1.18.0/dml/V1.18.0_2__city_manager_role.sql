-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 城市经理内置角色（平台员工角色，admin 之下、租户之上）
-- internal=1 使角色出现在内置角色维护列表；organization_id 挂默认组织，供权限加载对齐
INSERT INTO sys_role ( id, NAME, internal, data_scope, create_time, update_time, create_user, update_user, description, organization_id )
VALUES ( 'city_manager', 'city_manager', 1, 'ALL', UNIX_TIMESTAMP() * 1000 + 3, UNIX_TIMESTAMP() * 1000 + 3, 'admin', 'admin', '', '100001' );

-- 城市经理本人可见/可用的读权限（CITY_MANAGER:MANAGE 不种子，仅 admin 短路放行）
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'city_manager', 'CITY_MANAGER_ORG:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'city_manager', 'CITY_MANAGER_DASHBOARD:READ');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
