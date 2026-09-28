-- 付费用户管理：到期宽限天数（全局参数，默认 3 天）
INSERT INTO sys_parameter (param_key, param_value, type)
VALUES ('register.graceDays', '3', 'text');
