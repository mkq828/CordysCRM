SET SESSION innodb_lock_wait_timeout = 7200;

-- 模块配置新增「版本功能编码」：有值则按租户版本(hasFeature)过滤，为空则所有版本可见。
-- 用于「AI 经营决策中枢 / AI 创作中心」等仅专业版+可见的一级菜单的版本门控。
ALTER TABLE sys_module
    ADD COLUMN feature_code VARCHAR(64) DEFAULT NULL COMMENT '版本功能编码(空=所有版本可见)' AFTER pos;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
