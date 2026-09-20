-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- Init bank account permissions
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'BANK_ACCOUNT:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'BANK_ACCOUNT:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'BANK_ACCOUNT:UPDATE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'org_admin', 'BANK_ACCOUNT:DELETE');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'BANK_ACCOUNT:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'BANK_ACCOUNT:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'BANK_ACCOUNT:UPDATE');

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'BANK_ACCOUNT:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'BANK_ACCOUNT:ADD');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_staff', 'BANK_ACCOUNT:UPDATE');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
