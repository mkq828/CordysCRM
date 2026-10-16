SET SESSION innodb_lock_wait_timeout = 7200;

-- 数据大屏「AI 经营决策中枢」补挂基础版：免费全租户（查看型，只挂 DASHBOARD_READ 权限、不挂 G2 额度）。
-- INSERT IGNORE 幂等，edition_id + feature_id 唯一；不动 ai_creation（仍仅专业版/企业版）。
INSERT IGNORE INTO sys_edition_feature (id, edition_id, feature_id, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), e.id, f.id, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_edition e
         JOIN sys_feature f ON f.feature_code = 'ai_business_screen'
WHERE e.code = 'BASIC';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
