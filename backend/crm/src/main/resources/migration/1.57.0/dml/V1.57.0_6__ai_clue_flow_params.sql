-- AI 员工线索自动流转场景参数（代码内置默认值兜底，这里只是让默认值可配置化）
INSERT IGNORE INTO sys_parameter (`param_key`, `param_value`, `type`)
VALUES ('ai.auto_task.clue_flow.scan_limit', '10', 'text'),
       ('ai.auto_task.clue_flow.interest_follow_days', '7', 'text');
