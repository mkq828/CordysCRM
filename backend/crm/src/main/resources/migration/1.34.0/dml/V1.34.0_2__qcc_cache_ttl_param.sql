-- 线索爬取「企业信息缓存」过期天数系统参数（默认 90 天，可在 sys_parameter 调整）
INSERT IGNORE INTO sys_parameter (`param_key`, `param_value`, `type`)
VALUES ('ai.qcc.cacheTtlDays', '90', 'text');
