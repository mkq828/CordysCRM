SET SESSION innodb_lock_wait_timeout = 7200;

-- 声音克隆（功能 11）：租户音色库，上传样本异步复刻，成功后作为配音音色复用
CREATE TABLE ai_clone_voice
(
    id                   VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id      VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    name                 VARCHAR(256) COMMENT '音色名称(用户自定义展示名)',
    provider             VARCHAR(32)  COMMENT '服务商(固定豆包语音)',
    speaker_id           VARCHAR(256) COMMENT '火山引擎 custom_speaker_id',
    status               VARCHAR(32)  NOT NULL DEFAULT 'TRAINING' COMMENT '状态: TRAINING/SUCCESS/FAILED',
    sample_attachment_id VARCHAR(32)  COMMENT '样本音频附件ID(临时上传)',
    error_msg            VARCHAR(512) COMMENT '失败原因',
    create_time          BIGINT       COMMENT '创建时间(毫秒)',
    update_time          BIGINT       COMMENT '更新时间(毫秒)',
    create_user          VARCHAR(32)  COMMENT '创建人',
    update_user          VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_status (organization_id, status, create_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
