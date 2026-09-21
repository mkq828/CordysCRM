ALTER TABLE sys_operation_log
    ADD COLUMN trace_id VARCHAR(64) NULL COMMENT '链路追踪ID' AFTER request_source,
    ADD COLUMN request_params TEXT NULL COMMENT '请求参数' AFTER trace_id,
    ADD COLUMN user_agent VARCHAR(512) NULL COMMENT '浏览器User-Agent' AFTER request_params,
    ADD COLUMN ip VARCHAR(64) NULL COMMENT '客户端IP' AFTER user_agent,
    ADD COLUMN error_stack TEXT NULL COMMENT '异常堆栈' AFTER ip;
