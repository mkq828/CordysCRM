SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 员工（功能 6）虚拟会议室：董事长主持、多岗位 AI 依次发言后，用任务模型自动总结的会议纪要（租户级）
CREATE TABLE ai_meeting_minutes
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    title           VARCHAR(255) NOT NULL COMMENT '会议标题',
    time_text       VARCHAR(255) COMMENT '会议时间文本(如 2026年10月5日 星期一 10:30 — 11:15（时长 45 分钟）)',
    place           VARCHAR(64)  COMMENT '会议地点',
    host            VARCHAR(64)  COMMENT '主持人(董事长)',
    recorder        VARCHAR(64)  COMMENT '记录人(董事会秘书)',
    attendees_json  JSON COMMENT '参会人名单(数组)',
    content_json    JSON COMMENT '发言纪要(数组)',
    next_json       JSON COMMENT '后续行动(数组)',
    tasks_json      JSON COMMENT '任务清单(数组[任务/截止/交付物])',
    create_time     BIGINT COMMENT '创建时间(毫秒)',
    update_time     BIGINT COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org (organization_id, create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
