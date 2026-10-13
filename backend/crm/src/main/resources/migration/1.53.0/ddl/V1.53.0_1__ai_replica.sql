SET SESSION innodb_lock_wait_timeout = 7200;

-- 爆款复刻（功能 7 扩展 · 换皮变体）：丢一条真实参考视频 → 解析（抽帧+转写）→ 生成 N 个「换钩子/标题/字幕样式/比例」的变体。
-- 画面与声音 100% 复用原视频，AI 只动文字包装层，规避漫剧的 AI 生图崩脸/风控问题。

CREATE TABLE ai_replica_task
(
    id                      VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id         VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    title                   VARCHAR(256) COMMENT '任务名(默认参考视频文件名)',
    reference_attachment_id VARCHAR(32)  COMMENT '参考视频附件ID',
    parse_status            VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '解析状态: PENDING/PARSING/READY/FAILED',
    parse_json              TEXT         COMMENT '解析结果 JSON(sentences[{text,beginMs,endMs}] + cover_attachment_id + duration_ms)',
    variant_count           INT          NOT NULL DEFAULT 3 COMMENT '变体数量(1~5)',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '整体状态: PENDING/PARSING/GENERATING/READY/FAILED',
    error_msg               VARCHAR(512) COMMENT '失败原因',
    create_time             BIGINT       COMMENT '创建时间(毫秒)',
    update_time             BIGINT       COMMENT '更新时间(毫秒)',
    create_user             VARCHAR(32)  COMMENT '创建人',
    update_user             VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_time (organization_id, create_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE ai_replica_variant
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    task_id         VARCHAR(32)  NOT NULL COMMENT '所属任务ID',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    variant_no      INT          NOT NULL DEFAULT 0 COMMENT '变体序号',
    hook_text       VARCHAR(512) COMMENT '钩子文案(开头大字)',
    title_text      VARCHAR(256) COMMENT '标题/主题标签文案',
    subtitle_style  VARCHAR(32)  COMMENT '字幕样式标识(预设样式 key)',
    ratio           VARCHAR(16)  COMMENT '输出比例: 9:16/16:9/1:1',
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/COMPOSING/DONE/FAILED',
    attachment_id   VARCHAR(32)  COMMENT '输出变体视频附件ID',
    error_msg       VARCHAR(512) COMMENT '失败原因',
    create_time     BIGINT,
    update_time     BIGINT,
    create_user     VARCHAR(32),
    update_user     VARCHAR(32),
    PRIMARY KEY (id),
    KEY idx_task (task_id, variant_no)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
