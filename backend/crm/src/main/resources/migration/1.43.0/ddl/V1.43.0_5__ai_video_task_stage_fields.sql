SET SESSION innodb_lock_wait_timeout = 7200;

-- 短视频分阶段人工确认流：成片方式 + 图片/视频模型选择 + 内置提示词三字段（风格/角色/额外）落库
-- 风格/角色/额外在「确认分镜」阶段可点击编辑并回填，供出图与脚本生成消费
ALTER TABLE ai_video_task
    ADD COLUMN mode VARCHAR(20) NOT NULL DEFAULT 'video' COMMENT '成片方式(video/image_text，暂都走 Ken Burns)' AFTER ratio;
ALTER TABLE ai_video_task
    ADD COLUMN image_model_id VARCHAR(64) NULL COMMENT '所选图片模型ID(空则取推荐)' AFTER mode;
ALTER TABLE ai_video_task
    ADD COLUMN video_model_id VARCHAR(64) NULL COMMENT '所选视频模型ID(预留，图生视频暂未接)' AFTER image_model_id;
ALTER TABLE ai_video_task
    ADD COLUMN style_prompt TEXT COMMENT '风格提示词(用户可覆盖，空则随模板)' AFTER video_model_id;
ALTER TABLE ai_video_task
    ADD COLUMN character_prompt TEXT COMMENT '角色提示词(用户可编辑，空则 AI 按模板与主题决定)' AFTER style_prompt;
ALTER TABLE ai_video_task
    ADD COLUMN extra_prompt TEXT COMMENT '额外要求(卖点/补充素材，也兼容「做成视频」带入的现成脚本)' AFTER character_prompt;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
