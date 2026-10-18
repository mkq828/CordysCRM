-- AI 员工跟进破冰：联网搜索客户最新动态开关（默认开启，关闭后跟进不再联网注入动态）
INSERT IGNORE INTO sys_parameter (`param_key`, `param_value`, `type`)
VALUES ('ai.auto_task.follow_up.enable_web_search', 'true', 'text');
