-- AI 漫剧：剧本创意原始输入支持长文（上限 5 万字）。
-- 原 TEXT 仅 65535 字节，按 utf8mb4 每汉字 3 字节 ≈ 2.1 万汉字，不够放 5 万字故事，扩为 MEDIUMTEXT。
ALTER TABLE ai_drama_script
    MODIFY source_text MEDIUMTEXT COMMENT '整剧创意原始输入';
