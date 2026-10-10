SET SESSION innodb_lock_wait_timeout = 7200;

-- 声音克隆供应商切换后（火山→阿里云 Qwen-Audio-TTS），复刻模型名（如 qwen-audio-3.1-tts-flash）
-- 由 admin 在「模型设置」配置，避免阿里模型下线时改代码重发版。留空时后端回退默认模型。
ALTER TABLE agent_model
    ADD COLUMN voice_clone_model VARCHAR(255) NULL COMMENT '声音克隆模型名（阿里云声音复刻 target_model，如 qwen-audio-3.1-tts-flash）' AFTER model_params;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
