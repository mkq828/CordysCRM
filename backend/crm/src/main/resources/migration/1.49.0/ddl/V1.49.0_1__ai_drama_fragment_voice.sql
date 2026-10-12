-- AI 漫剧：分镜口播支持逐镜独立音色（多人对话每人不同声音）。
-- 每镜可指定音色标识，空则回退分集默认音色；取值对应模型设置音色列表（系统音色 + 克隆音色）的 value。
ALTER TABLE ai_drama_fragment
    ADD COLUMN voice VARCHAR(64) NULL COMMENT '本镜口播音色标识(空则用分集默认音色)' AFTER narration_attachment_id;
