SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 创作中心-工具中心任务：文生图/视频转格式等异步耗时工具，输入参数 + 输出附件
CREATE TABLE ai_tool_task
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    type            VARCHAR(16)  NOT NULL COMMENT '工具类型: IMAGE文生图/TRANSCODE视频转格式',
    name            VARCHAR(256) COMMENT '任务展示名(提示词摘要/源文件名)',
    params          TEXT         COMMENT '入参 JSON(文生图:prompt/size；转格式:sourceAttachmentId/format)',
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/DONE/FAILED',
    progress        INT          NOT NULL DEFAULT 0 COMMENT '进度百分比(0-100)',
    attachment_id   VARCHAR(32)  COMMENT '输出附件ID(附件系统)',
    error_msg       VARCHAR(512) COMMENT '失败原因',
    create_time     BIGINT       COMMENT '创建时间(毫秒)',
    update_time     BIGINT       COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)  COMMENT '创建人',
    update_user     VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_type (organization_id, type, create_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
