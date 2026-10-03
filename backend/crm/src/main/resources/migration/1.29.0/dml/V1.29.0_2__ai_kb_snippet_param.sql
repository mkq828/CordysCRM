-- 企业知识库智能问答「出处片段」长度系统参数（默认 200 字，可在 sys_parameter 调整，避免整段 chunk 直接暴露）
INSERT IGNORE INTO sys_parameter (`param_key`, `param_value`, `type`)
VALUES ('ai.kb.snippet_max', '200', 'text');
