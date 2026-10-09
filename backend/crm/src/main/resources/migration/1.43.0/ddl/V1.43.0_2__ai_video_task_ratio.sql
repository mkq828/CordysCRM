SET SESSION innodb_lock_wait_timeout = 7200;

-- 短视频成片比例可切换：风格页选 9:16 / 16:9 / 1:1，出图与合成按该比例输出
ALTER TABLE ai_video_task
    ADD COLUMN ratio VARCHAR(10) NOT NULL DEFAULT '9:16' COMMENT '成片比例(9:16/16:9/1:1)';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
