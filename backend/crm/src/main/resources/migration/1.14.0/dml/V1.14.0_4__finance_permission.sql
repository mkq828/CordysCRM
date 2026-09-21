-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 财务模块权限：org_admin / sales_manager 可查看并核销，sales_staff 只读
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'FINANCE:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'FINANCE:VERIFY');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'FINANCE:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'FINANCE:VERIFY');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'FINANCE:READ');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
