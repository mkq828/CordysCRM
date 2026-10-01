SET SESSION innodb_lock_wait_timeout = 7200;

-- 续费/升级申请：审核备注（驳回原因/通过说明），核销或驳回时由 admin 填写，租户在申请记录与通知里可看到
ALTER TABLE tenant_plan_application
    ADD COLUMN verify_remark VARCHAR(255) COMMENT '审核备注（驳回原因/通过说明）' AFTER remark;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
