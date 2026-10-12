-- AI 漫剧：克隆音色 voice_id 形如 qwen-audio-3.1-tts-flash-v<uuid>（约 67 字符），
-- 原 VARCHAR(64) 绑定克隆音色时触发「Data too long for column 'voice'」导致保存失败。
-- 放宽到 VARCHAR(128)，与 ai_video_task.voice 口径一致，系统音色短值与克隆音色长值都装得下。
ALTER TABLE ai_drama_asset
    MODIFY COLUMN voice VARCHAR(128) NULL COMMENT '角色音色标识(空则配音用默认音色)';

ALTER TABLE ai_drama_fragment
    MODIFY COLUMN voice VARCHAR(128) NULL COMMENT '本镜口播音色标识(空则用分集默认音色)';
