-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 企业注册默认角色（销售经理）补充合同/报价审批权限
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'CONTRACT:APPROVAL');
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES (UUID_SHORT(), 'sales_manager', 'OPPORTUNITY_QUOTATION:APPROVAL');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
