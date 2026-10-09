SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 创作中心-视频模板库：系统预置/后续可扩展的视频风格模板。
-- config_json 驱动 FFmpeg/ASS 合成（配色/字幕/片头片尾/转场/BGM）与生成提示词（脚本语气/画面风格），
-- 把原先硬编码在 VideoSynthesizer 的视觉参数改为配置化。
CREATE TABLE ai_video_template
(
    id           VARCHAR(32)  NOT NULL COMMENT '主键',
    template_key VARCHAR(64)  NOT NULL COMMENT '模板标识(唯一，任务回填)',
    name         VARCHAR(64)  NOT NULL COMMENT '模板名称',
    description  VARCHAR(256) COMMENT '模板描述',
    color        VARCHAR(16)  COMMENT '前端卡片主色(如 #7c3aed)',
    soft_color   VARCHAR(16)  COMMENT '前端卡片浅色(如 #ede9fe)',
    config_json  TEXT         COMMENT '合成/生成配置 JSON(视觉配色/字幕/转场/BGM/脚本语气/画面风格)',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序(越小越靠前)',
    enabled      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用(1是/0否)',
    create_time  BIGINT       COMMENT '创建时间(毫秒)',
    update_time  BIGINT       COMMENT '更新时间(毫秒)',
    create_user  VARCHAR(32)  COMMENT '创建人',
    update_user  VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_key (template_key)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 短视频任务回填生成时使用的模板标识（历史任务为 NULL，回退默认模板）
ALTER TABLE ai_video_task
    ADD COLUMN template_key VARCHAR(64) COMMENT '生成时使用的模板标识(ai_video_template.template_key)' AFTER style;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
