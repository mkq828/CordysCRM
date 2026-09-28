SET SESSION innodb_lock_wait_timeout = 7200;

-- 版本 AI 月度配额（示例值，admin 可在版本配置里调整）
UPDATE sys_edition SET ai_monthly_quota = 20 WHERE code = 'BASIC';
UPDATE sys_edition SET ai_monthly_quota = 300 WHERE code = 'PRO';
UPDATE sys_edition SET ai_monthly_quota = 1000 WHERE code = 'ENTERPRISE';

-- AI 模型单价种子（示例值，admin 手动维护；输入/输出单价单位：元/1k token）
INSERT INTO ai_model_price (id, model_code, model_name, input_price, output_price, status, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'qwen', '通义千问', 0.0005, 0.002, 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'deepseek', 'DeepSeek', 0.001, 0.002, 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'doubao', '豆包(火山引擎)', 0.0008, 0.002, 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
