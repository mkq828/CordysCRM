SET SESSION innodb_lock_wait_timeout = 7200;

-- 城市经理销售权限：合同/回款读写 + 租户资料编辑。
-- 核销/撤回(ADMIN_FINANCE_VERIFY)、发票(ADMIN_FINANCE_*)、全局营收(ADMIN_DASHBOARD_READ) 仍仅 admin，故不入种子。
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'city_manager', 'CITY_MANAGER_CONTRACT:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'city_manager', 'CITY_MANAGER_CONTRACT:WRITE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'city_manager', 'CITY_MANAGER_PAYMENT:READ');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'city_manager', 'CITY_MANAGER_PAYMENT:WRITE');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'city_manager', 'CITY_MANAGER_ORG:EDIT');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
