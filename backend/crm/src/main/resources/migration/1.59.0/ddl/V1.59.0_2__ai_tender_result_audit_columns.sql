SET SESSION innodb_lock_wait_timeout = 7200;

-- 补齐 ai_tender_result 的审计列：TenderResult 继承 BaseModel，查询/插入都会带上 update_user/update_time，
-- 1.59.0_1 漏建这两列导致 SELECT 报 Unknown column 'update_user'。

ALTER TABLE ai_tender_result
    ADD COLUMN update_time BIGINT COMMENT '更新时间(毫秒)' AFTER create_time,
    ADD COLUMN update_user VARCHAR(32) COMMENT '修改人' AFTER create_user;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
