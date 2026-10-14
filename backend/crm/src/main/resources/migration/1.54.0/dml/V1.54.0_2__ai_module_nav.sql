SET SESSION innodb_lock_wait_timeout = 7200;

-- 新增两个一级菜单对应的版本功能点（INSERT IGNORE 幂等，feature_code 唯一）
INSERT IGNORE INTO sys_feature (id, feature_code, name, category, enable, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'ai_business_screen', 'AI 经营决策中枢', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

INSERT IGNORE INTO sys_feature (id, feature_code, name, category, enable, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'ai_creation', 'AI 创作中心', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

-- 专业版 + 企业版开通（INSERT IGNORE 幂等，edition_id+feature_id 唯一）
INSERT IGNORE INTO sys_edition_feature (id, edition_id, feature_id, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), e.id, f.id, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_edition e
         JOIN sys_feature f ON f.feature_code IN ('ai_business_screen', 'ai_creation')
WHERE e.code IN ('PRO', 'ENTERPRISE');

-- 所有组织 pos >= 2 的模块整体下移 2 位，为「经营决策中枢 / AI 创作中心」腾出第 2、3 位
UPDATE sys_module SET pos = pos + 2 WHERE pos >= 2;

-- 默认组织 100001 新增两个模块
INSERT INTO sys_module (id, organization_id, module_key, enable, pos, feature_code, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), '100001', 'businessScreen', 1, 2, 'ai_business_screen', 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

INSERT INTO sys_module (id, organization_id, module_key, enable, pos, feature_code, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), '100001', 'aiCreation', 1, 3, 'ai_creation', 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

-- 存量租户回填两个模块（NOT EXISTS 幂等）
INSERT INTO sys_module (id, organization_id, module_key, enable, pos, feature_code, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), o.id, m.module_key, m.enable, m.pos, m.feature_code, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_organization o
         JOIN sys_module m ON m.organization_id = '100001' AND m.module_key IN ('businessScreen', 'aiCreation')
WHERE o.id <> '100001'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_module x
      WHERE x.organization_id = o.id
        AND x.module_key = m.module_key
  );

SET SESSION innodb_lock_wait_timeout = DEFAULT;
