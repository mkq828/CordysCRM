SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 员工自动任务二期：动作表从「客户跟进专用」扩展为「多资源通用」。
-- 新增资源类型/资源ID/资源名称三列，客户相关列放宽为可空（催款对应合同、线索流转对应线索等）。
-- 老数据只有 customer_id，按「资源=客户」回填，保证后续按 actionType+resourceId 去重不丢历史 PENDING/REJECTED。

ALTER TABLE ai_employee_action
    ADD COLUMN resource_type VARCHAR(32) COMMENT '资源类型(如 CUSTOMER/CONTRACT/PAYMENT_PLAN/CLUE)' AFTER action_type,
    ADD COLUMN resource_id   VARCHAR(32) COMMENT '资源ID(客户/合同/回款计划/线索等)' AFTER resource_type,
    ADD COLUMN resource_name VARCHAR(128) COMMENT '资源名称(冗余展示)' AFTER resource_id;

ALTER TABLE ai_employee_action
    MODIFY COLUMN customer_id VARCHAR(32) NULL COMMENT '客户ID(部分动作类型无客户,可空)';

UPDATE ai_employee_action
SET resource_type = 'CUSTOMER',
    resource_id   = customer_id,
    resource_name = customer_name
WHERE action_type = 'FOLLOW_UP_CREATE'
  AND (resource_type IS NULL OR resource_type = '');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
