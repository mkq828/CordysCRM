SET SESSION innodb_lock_wait_timeout = 7200;

-- 线索爬取下线后，功能点展示名收敛为「竞品洞察」（feature_code 保持不变，避免牵连历史会话/额度/版本门控）
UPDATE sys_feature
SET name = '竞品洞察',
    update_time = UNIX_TIMESTAMP() * 1000
WHERE feature_code = 'lead_crawl';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
