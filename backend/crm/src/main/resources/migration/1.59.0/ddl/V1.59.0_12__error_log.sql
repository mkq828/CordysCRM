SET SESSION innodb_lock_wait_timeout = 7200;

-- 系统错误日志：全局 Logback ERROR Appender 采集一切 ERROR 级日志落库，供平台管理员在生产报错后本地复现排查。
-- 覆盖 HTTP 500 与定时任务内部吞掉的异常；预期失败(未配置模型/DataEase)已降 WARN，不在此表。

CREATE TABLE sys_error_log
(
    id          VARCHAR(32)   NOT NULL COMMENT '主键',
    trace_id    VARCHAR(64)   COMMENT '链路追踪ID(HTTP 请求链路，无登录态任务为空)',
    logger_name VARCHAR(255)  COMMENT '报错类(Logger名)',
    message     TEXT          COMMENT '错误信息',
    stack_trace MEDIUMTEXT    COMMENT '异常堆栈',
    create_time BIGINT        COMMENT '发生时间(毫秒)',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_logger_name (logger_name)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
