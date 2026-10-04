SET SESSION innodb_lock_wait_timeout = 7200;

-- 智能通话复盘：一键转跟进成功后记录生成的跟进记录ID，详情据此禁用「一键转跟进」按钮，避免重复转
ALTER TABLE ai_call_record
    ADD COLUMN follow_record_id VARCHAR(32) COMMENT '一键转跟进生成的跟进记录ID(可空)' AFTER error_msg;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
