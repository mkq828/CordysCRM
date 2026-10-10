SET SESSION innodb_lock_wait_timeout = 7200;

-- 声音克隆供应商切到阿里云 Qwen-Audio-TTS 后，克隆音色 voice_id 形如
-- qwen-audio-3.1-tts-flash-v5fc8d2c0-7d176feaf4ce40bc9cee05bb63a8026c（67 字符），
-- 超过原 32 长度，扩到 128 以容纳克隆音色标识。
ALTER TABLE ai_video_task
    MODIFY COLUMN voice VARCHAR(128) NULL COMMENT '音色标识';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
