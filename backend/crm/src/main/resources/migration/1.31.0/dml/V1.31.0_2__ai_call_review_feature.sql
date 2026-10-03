SET SESSION innodb_lock_wait_timeout = 7200;

-- 功能点：智能通话复盘（录音转写 + AI 复盘，挂 G2 额度 + G3 权限开关）
INSERT INTO sys_feature (id, feature_code, name, category, enable, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'ai_call_review', '智能通话复盘', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

-- 专业版+：专业版、企业版均可使用智能通话复盘
INSERT INTO sys_edition_feature (id, edition_id, feature_id, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), e.id, f.id, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_edition e
         JOIN sys_feature f ON f.feature_code = 'ai_call_review'
WHERE e.code IN ('PRO', 'ENTERPRISE');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
