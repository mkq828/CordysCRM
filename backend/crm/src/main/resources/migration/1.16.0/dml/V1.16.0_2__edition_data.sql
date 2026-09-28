SET SESSION innodb_lock_wait_timeout = 7200;

-- 版本：基础版(免费)/专业版/企业版
INSERT INTO sys_edition (id, code, name, year_price, first_year_price, soft_limit, validity_days, sort, status, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'BASIC', '基础版', 0, 0, 5, 365, 1, 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'PRO', '专业版', 3980, 2980, 10, 365, 2, 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'ENTERPRISE', '企业版', 9800, 9800, 30, 365, 3, 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

-- 功能点（feature_code/名称/分类）
INSERT INTO sys_feature (id, feature_code, name, category, enable, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'crm_basic', '基础 CRM', 'crm', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'finance', '高级财务/回款核销', 'crm', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'sales_sop', '销售 SOP + 跟进提醒', 'crm', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'custom_field_export', '自定义字段 + 报表导出', 'crm', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'notify', '短信/企微通知', 'crm', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'ai_advisor', 'AI 客户军师 + 候选话术', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'ai_acquire', 'AI 获客内容生成', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'ai_sales_rag', '销售话术库 RAG', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'ai_video', '短视频生成', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'wecom_auto_analysis', '企微会话自动分析', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'ai_ppt', 'AI 方案/PPT 演示生成', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'lead_crawl', '线索爬取 + 竞品/行业洞察', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'dm_profile', '私信获客画像', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'ai_employee', 'AI 员工 + 语音外呼', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'ai_kb', '企业知识库', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'digital_human', '数字人/剪辑/爆款复刻', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000),
       (UUID_SHORT(), 'dedicated_support', '专属支持', 'service', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

-- 版本-功能归属（BASIC 仅基础 CRM）
INSERT INTO sys_edition_feature (id, edition_id, feature_id, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), e.id, f.id, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_edition e
         JOIN sys_feature f ON f.feature_code = 'crm_basic'
WHERE e.code = 'BASIC';

-- 专业版：基础 + 高级 CRM/通知 + 主力 AI 能力
INSERT INTO sys_edition_feature (id, edition_id, feature_id, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), e.id, f.id, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_edition e
         JOIN sys_feature f ON f.feature_code IN ('crm_basic', 'finance', 'sales_sop', 'custom_field_export', 'notify',
                                                  'ai_advisor', 'ai_acquire', 'ai_sales_rag', 'ai_video', 'dm_profile')
WHERE e.code = 'PRO';

-- 企业版：全量功能
INSERT INTO sys_edition_feature (id, edition_id, feature_id, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), e.id, f.id, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_edition e
         JOIN sys_feature f ON f.feature_code IN ('crm_basic', 'finance', 'sales_sop', 'custom_field_export', 'notify',
                                                  'ai_advisor', 'ai_acquire', 'ai_sales_rag', 'ai_video', 'wecom_auto_analysis',
                                                  'ai_ppt', 'lead_crawl', 'dm_profile', 'ai_employee', 'ai_kb',
                                                  'digital_human', 'dedicated_support')
WHERE e.code = 'ENTERPRISE';

-- 存量兼容：PERSONAL → BASIC（ENTERPRISE 保持不变）
UPDATE tenant_plan SET version = 'BASIC' WHERE version = 'PERSONAL';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
