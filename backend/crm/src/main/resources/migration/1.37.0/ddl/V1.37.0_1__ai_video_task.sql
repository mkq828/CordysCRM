SET SESSION innodb_lock_wait_timeout = 7200;

-- 短视频生成（功能 7 MVP）：口播视频生成任务
CREATE TABLE ai_video_task
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    title           VARCHAR(256) COMMENT '主题/任务标题',
    script_text     TEXT         COMMENT '口播文案全文(分句)',
    voice           VARCHAR(32)  COMMENT '音色标识',
    style           VARCHAR(32)  COMMENT '风格/模板标识',
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/SCRIPTING/TTS/SYNTH/DONE/FAILED',
    progress        INT          NOT NULL DEFAULT 0 COMMENT '进度百分比(0-100)',
    attachment_id   VARCHAR(32)  COMMENT '输出视频附件ID(附件系统)',
    error_msg       VARCHAR(512) COMMENT '失败原因',
    create_time     BIGINT       COMMENT '创建时间(毫秒)',
    update_time     BIGINT       COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)  COMMENT '创建人',
    update_user     VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_status (organization_id, status, create_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
