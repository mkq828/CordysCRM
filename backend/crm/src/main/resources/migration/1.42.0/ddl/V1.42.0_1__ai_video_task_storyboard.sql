SET SESSION innodb_lock_wait_timeout = 7200;

-- 短视频生成：分镜台展示用。生成过程中把「口播分镜 + 画面描述 + 配图附件ID」落库，
-- 前端据此渲染「确认分镜 → 画面与配音」的进度工作台（与 printfilm 分镜台对齐）。
ALTER TABLE ai_video_task
    ADD COLUMN storyboard_json TEXT NULL COMMENT '分镜JSON数组[{text,image,imageAttachmentId}]，生成中逐步回填' AFTER copy_json;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
