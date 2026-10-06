SET SESSION innodb_lock_wait_timeout = 7200;

-- 功能点：私信获客画像（AI 画像挂 G2 额度 + G3 权限开关）
-- INSERT IGNORE：feature_code / (edition_id, feature_id) 均有唯一键，幂等，重复执行或已存在时跳过
INSERT IGNORE INTO sys_feature (id, feature_code, name, category, enable, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'dm_profile', '私信获客画像', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

-- 专业版+：专业版、企业版均可使用私信获客画像
INSERT IGNORE INTO sys_edition_feature (id, edition_id, feature_id, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), e.id, f.id, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_edition e
         JOIN sys_feature f ON f.feature_code = 'dm_profile'
WHERE e.code IN ('PRO', 'ENTERPRISE');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
