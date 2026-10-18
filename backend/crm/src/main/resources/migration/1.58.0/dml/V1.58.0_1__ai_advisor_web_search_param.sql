-- 会话军师「联网查客户背景」DeepSeek 网页搜索 API Key（平台级，空值=未启用联网搜索）
-- 到 platform.deepseek.com 申请后填入；与 DeepSeek 聊天补全共用同一把 key
INSERT IGNORE INTO sys_parameter (`param_key`, `param_value`, `type`)
VALUES ('ai.deepseek.searchApiKey', '', 'text');

-- 1 次联网搜索折算 token 数（默认 1000 ≈ 0.1 次标准额度；DeepSeek 搜索按次计费，折算后复用额度链路）
INSERT IGNORE INTO sys_parameter (`param_key`, `param_value`, `type`)
VALUES ('ai.deepseek.searchTokensPerCall', '1000', 'text');
