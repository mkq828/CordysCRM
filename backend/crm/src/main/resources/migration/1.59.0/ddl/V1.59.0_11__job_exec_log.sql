SET SESSION innodb_lock_wait_timeout = 7200;

-- 定时任务执行日志：记录每个 Quartz 任务的起止、耗时与成败，供平台管理员监控 AI 定时任务是否按时跑完。
-- 两阶段写入：jobToBeExecuted 落 RUNNING，jobWasExecuted 更新为 SUCCESS/FAILED；RUNNING 残留即「未完成/超时」。

CREATE TABLE sys_job_exec_log
(
    id          VARCHAR(32)   NOT NULL COMMENT '主键',
    job_name    VARCHAR(128)  NOT NULL COMMENT '任务名(Quartz JobKey name 简单类名)',
    job_group   VARCHAR(128)  COMMENT '任务组(Quartz JobKey group)',
    status      VARCHAR(16)   NOT NULL COMMENT '执行状态(RUNNING/SUCCESS/FAILED)',
    start_time  BIGINT        COMMENT '开始时间(毫秒)',
    end_time    BIGINT        COMMENT '结束时间(毫秒)',
    duration_ms BIGINT        COMMENT '耗时(毫秒)',
    error_msg   VARCHAR(1024) COMMENT '失败信息',
    create_time BIGINT        COMMENT '创建时间(毫秒)',
    PRIMARY KEY (id),
    KEY idx_job_name (job_name),
    KEY idx_start_time (start_time),
    KEY idx_status (status)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
