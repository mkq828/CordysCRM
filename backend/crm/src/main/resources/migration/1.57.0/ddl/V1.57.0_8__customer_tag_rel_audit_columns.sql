SET SESSION innodb_lock_wait_timeout = 7200;

-- 补齐 customer_tag_rel 审计字段：Domain 继承 BaseModel（含 update_user/update_time），
-- MyBatis 生成 SELECT 会查这两列，建表时遗漏导致客户详情查询报「Unknown column 'update_user'」。

ALTER TABLE customer_tag_rel
    ADD COLUMN update_time BIGINT COMMENT '更新时间(毫秒)',
    ADD COLUMN update_user VARCHAR(32) COMMENT '修改人';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
