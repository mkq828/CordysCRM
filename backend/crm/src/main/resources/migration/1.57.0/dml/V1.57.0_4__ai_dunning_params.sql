-- AI 员工自动催款场景参数（代码内置默认值兜底，这里只是让默认值可配置化）
INSERT IGNORE INTO sys_parameter (`param_key`, `param_value`, `type`)
VALUES ('ai.auto_task.dunning.scan_limit', '10', 'text'),
       ('ai.auto_task.dunning.lookahead_days', '7', 'text'),
       ('ai.auto_task.dunning.overdue_days', '30', 'text');
