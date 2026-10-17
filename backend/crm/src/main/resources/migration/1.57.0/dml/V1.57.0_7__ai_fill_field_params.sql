-- AI 员工自动补全数据场景参数（代码内置默认值兜底，这里只是让默认值可配置化）
INSERT IGNORE INTO sys_parameter (`param_key`, `param_value`, `type`)
VALUES ('ai.auto_task.fill_field.scan_limit', '10', 'text');
