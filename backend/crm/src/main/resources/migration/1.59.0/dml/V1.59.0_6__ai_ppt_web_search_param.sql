-- AI 方案/PPT 行业实时数据：联网搜索开关（默认开启，关闭后生成大纲不再联网注入行业数据）
INSERT IGNORE INTO sys_parameter (`param_key`, `param_value`, `type`)
VALUES ('ai.ppt.enable_web_search', 'true', 'text');
