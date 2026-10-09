SET SESSION innodb_lock_wait_timeout = 7200;

-- 短视频「成片类型」：talk=口播科普(默认) / product=产品讲解 / scenario=情景剧 / comic=漫剧(占位延后)
-- 类型决定脚本结构与画面 prompt 策略，不改模型选择逻辑
ALTER TABLE ai_video_task
    ADD COLUMN video_type VARCHAR(20) NOT NULL DEFAULT 'talk' COMMENT '成片类型(talk/product/scenario/comic)' AFTER extra_prompt;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
