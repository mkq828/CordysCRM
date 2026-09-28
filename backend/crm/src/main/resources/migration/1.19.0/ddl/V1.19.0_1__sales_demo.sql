SET SESSION innodb_lock_wait_timeout = 7200;

-- 演示租户标记：is_demo=1 的租户不计入全局营收与城市经理业绩（演示数据不进账）
ALTER TABLE sys_organization
    ADD COLUMN `is_demo` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否演示租户(1=是,0=否)' AFTER `follow_manager_id`;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
