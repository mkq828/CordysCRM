SET SESSION innodb_lock_wait_timeout = 7200;

-- 功能点增加「是否已上线」标记：0=规划中(待完善/未上线)，1=已上线。
-- 用于在版本/功能对比中把封存功能显示为「规划中/敬请期待」，不误导租户以为已交付。
ALTER TABLE sys_feature
    ADD COLUMN launched BIT(1) NOT NULL DEFAULT 1 COMMENT '是否已上线(1=已上线 0=规划中)' AFTER `enable`;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
