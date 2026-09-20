-- suggestion_vote 缺 BaseModel 的 create_user/update_user/update_time 三列，
-- BaseMapper 自动生成的 SELECT/INSERT 会带出这些列导致 Unknown column，补齐与 suggestion/suggestion_comment 一致。
ALTER TABLE suggestion_vote
    ADD COLUMN create_user VARCHAR(32) COMMENT '创建人' AFTER create_time,
    ADD COLUMN update_user VARCHAR(32) COMMENT '修改人' AFTER create_user,
    ADD COLUMN update_time BIGINT COMMENT '更新时间' AFTER update_user;
