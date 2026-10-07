SET SESSION innodb_lock_wait_timeout = 7200;

-- 功能 7 短视频生成：出片后配套文案（抖音/小红书/朋友圈 JSON），与功能 3 获客内容打通
ALTER TABLE ai_video_task
    ADD COLUMN copy_json TEXT NULL COMMENT '配套文案(抖音/小红书/朋友圈 JSON，生成失败为 NULL)' AFTER script_text;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
