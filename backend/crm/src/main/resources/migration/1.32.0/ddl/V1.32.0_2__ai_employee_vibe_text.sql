-- 修复：agency-agents 目录中部分角色 vibe 字段超过 255 字符（实测最长 768），VARCHAR(255) 会截断导致导入失败，
-- 改为 TEXT 与 description 保持一致，避免「一句话人设」丢内容。
ALTER TABLE ai_employee
    MODIFY vibe TEXT COMMENT '一句话人设';
