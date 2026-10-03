-- AI 中心：会话记录底座。agent_conversation 增加能力类型，agent_message 增加结构化结果载荷。
ALTER TABLE agent_conversation
    ADD COLUMN feature_code VARCHAR(32) NOT NULL DEFAULT 'chat'
        COMMENT '能力类型：chat=哆咪AI对话, ai_advisor=会话军师, ai_kb=智能问答, ai_acquire=获客内容' AFTER title;

CREATE INDEX idx_feature_code_user ON agent_conversation (feature_code, user_id);

ALTER TABLE agent_message
    ADD COLUMN payload TEXT NULL COMMENT '能力结构化结果 JSON（出处引用/分析字段/物料包/输入参数），纯文本对话为 NULL' AFTER content;
