SET SESSION innodb_lock_wait_timeout = 7200;

-- 功能 7 短视频生成：模型路由策略新增「配音模型」（TTS）路由位，与对话/任务模型并列
ALTER TABLE agent_model_strategy
    ADD COLUMN tts_models VARCHAR(1000) NULL COMMENT '配音模型（TTS，逗号分隔模型ID）' AFTER task_models;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
