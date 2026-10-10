SET SESSION innodb_lock_wait_timeout = 7200;

-- 声音克隆（功能 11）：企业版（INSERT IGNORE 幂等，feature_code 唯一）
INSERT IGNORE INTO sys_feature (id, feature_code, name, category, enable, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'ai_voice_clone', '声音克隆', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

-- 企业版开通（INSERT IGNORE 幂等，edition_id+feature_id 唯一）
INSERT IGNORE INTO sys_edition_feature (id, edition_id, feature_id, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), e.id, f.id, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_edition e
         JOIN sys_feature f ON f.feature_code = 'ai_voice_clone'
WHERE e.code = 'ENTERPRISE';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
