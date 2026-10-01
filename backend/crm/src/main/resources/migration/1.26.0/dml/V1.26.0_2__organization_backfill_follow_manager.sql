SET SESSION innodb_lock_wait_timeout = 7200;

-- 历史数据补齐：创建合同时只回填签约经理、未补跟进经理的历史租户，按「谁签的谁跟进」补齐跟进经理
UPDATE sys_organization
SET follow_manager_id = sign_manager_id
WHERE sign_manager_id IS NOT NULL
  AND sign_manager_id <> ''
  AND (follow_manager_id IS NULL OR follow_manager_id = '');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
