-- 注册审核权限点（授予内置 org_admin 角色）
INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES
    ('register_audit_read', 'org_admin', 'SYS_REGISTER_AUDIT:READ'),
    ('register_audit_approve', 'org_admin', 'SYS_REGISTER_AUDIT:APPROVE'),
    ('register_audit_reject', 'org_admin', 'SYS_REGISTER_AUDIT:REJECT');
