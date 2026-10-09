SET SESSION innodb_lock_wait_timeout = 7200;

-- 短视频：区分「项目名称（短标题）」与「原始输入（主题/完整文案）」，分镜台大纲展示原始输入
ALTER TABLE ai_video_task
    ADD COLUMN source_text TEXT COMMENT '原始输入（一句话主题或完整文案），用于脚本生成与分镜台大纲展示';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
