SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 漫剧（功能 7 扩展）：项目 → 剧本 → 分集 → 分镜，加角色/场景/道具/音色资产库。
-- 分镜引用资产走 fragment.asset_ids_json 存储，不建独立关联表（本版不做画布反向查询）。

CREATE TABLE ai_drama_project
(
    id                 VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id    VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    title              VARCHAR(256) COMMENT '剧名',
    description        VARCHAR(512) COMMENT '创意/一句话描述',
    workflow           VARCHAR(16)  NOT NULL DEFAULT 'script' COMMENT '工作流: script(剧本)',
    content_json       TEXT         COMMENT '预留画布 JSON（本版不用）',
    params_json        TEXT         COMMENT '项目全局成片设置 JSON(画幅/清晰度/画面风格/字幕)',
    cover_attachment_id VARCHAR(32) COMMENT '封面附件ID',
    create_time        BIGINT       COMMENT '创建时间(毫秒)',
    update_time        BIGINT       COMMENT '更新时间(毫秒)',
    create_user        VARCHAR(32)  COMMENT '创建人',
    update_user        VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_time (organization_id, create_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE ai_drama_script
(
    id                   VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id      VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    project_id           VARCHAR(32) NOT NULL COMMENT '所属项目ID',
    name                 VARCHAR(256) COMMENT '剧本名',
    source_text          TEXT        COMMENT '整剧创意原始输入',
    summary_json         TEXT        COMMENT '剧情摘要 JSON(剧名/类型/一句话/梗概/人物)',
    episode_content_json TEXT        COMMENT '分集剧本 JSON 数组',
    summary_status       VARCHAR(32) COMMENT '摘要生成状态: running/completed/failed',
    params_json          TEXT        COMMENT 'image_style_id 等',
    create_time          BIGINT,
    update_time          BIGINT,
    create_user          VARCHAR(32),
    update_user          VARCHAR(32),
    PRIMARY KEY (id),
    UNIQUE KEY uk_project (project_id),
    KEY idx_org (organization_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE ai_drama_episode
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    project_id      VARCHAR(32) NOT NULL COMMENT '所属项目ID',
    name            VARCHAR(256) COMMENT '集名',
    sort_order      INT         NOT NULL DEFAULT 0 COMMENT '集序',
    status          VARCHAR(32) COMMENT '分镜规划状态: planning/completed/failed',
    video_attachment_id VARCHAR(32) COMMENT '合成成片附件ID(mp4)',
    params_json     TEXT        COMMENT '分镜规划/生成/合成附加信息 JSON',
    create_time     BIGINT,
    update_time     BIGINT,
    create_user     VARCHAR(32),
    update_user     VARCHAR(32),
    PRIMARY KEY (id),
    KEY idx_project (project_id, sort_order)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE ai_drama_fragment
(
    id                  VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id     VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    episode_id          VARCHAR(32) NOT NULL COMMENT '所属分集ID',
    sort_order          INT         NOT NULL DEFAULT 0 COMMENT '镜序',
    content             TEXT        COMMENT '画面描述(文生图 prompt)',
    narration           TEXT        COMMENT '口播台词(TTS + 字幕)',
    narration_attachment_id VARCHAR(32) COMMENT '口播配音音频附件ID(wav/mp3)',
    duration_sec        INT         COMMENT '时长(秒)',
    cover_attachment_id VARCHAR(32) COMMENT '画面/封面附件ID',
    video_attachment_id VARCHAR(32) COMMENT '镜头视频附件ID',
    asset_ids_json      TEXT        COMMENT '引用资产ID数组(角色/场景视觉锚点)',
    status              VARCHAR(32) COMMENT '生成状态: generating/done/failed',
    params_json         TEXT        COMMENT '附加信息 JSON',
    create_time         BIGINT,
    update_time         BIGINT,
    create_user         VARCHAR(32),
    update_user         VARCHAR(32),
    PRIMARY KEY (id),
    KEY idx_episode (episode_id, sort_order)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE ai_drama_asset
(
    id                 VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id    VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    project_id         VARCHAR(32) NOT NULL COMMENT '所属项目ID',
    type               VARCHAR(16) COMMENT '角色/场景/道具/音色: character/scene/prop/voice',
    asset_type         VARCHAR(32) COMMENT '更细类型',
    name               VARCHAR(256) COMMENT '资产名',
    cover_attachment_id VARCHAR(32) COMMENT '形象图/封面附件ID',
    params_json        TEXT        COMMENT '外观描述/image_style/generation 等 JSON',
    status             VARCHAR(32) COMMENT '形象生成状态: generating/done/failed',
    create_time        BIGINT,
    update_time        BIGINT,
    create_user        VARCHAR(32),
    update_user        VARCHAR(32),
    PRIMARY KEY (id),
    KEY idx_project_type (project_id, type)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
