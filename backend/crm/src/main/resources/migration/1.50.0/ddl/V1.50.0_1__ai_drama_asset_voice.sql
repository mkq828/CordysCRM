-- AI 漫剧：角色资产绑定音色，支撑「多人对话每人不同声音」。
-- character 资产的 voice 存音色标识（系统音色 / 克隆音色 value），空则配音时回退分集默认音色。
ALTER TABLE ai_drama_asset
    ADD COLUMN voice VARCHAR(64) NULL COMMENT '角色音色标识(空则配音用默认音色)' AFTER name;
