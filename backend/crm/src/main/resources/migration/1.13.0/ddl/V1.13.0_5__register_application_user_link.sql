-- 注册申请单补充与开通账号的关联：
-- 记录审核通过后创建的用户ID，用于管理端展示「累计使用天数 / 最后登录时间 / 账号启停」。
ALTER TABLE sys_register_application
    ADD COLUMN user_id VARCHAR(32) NULL COMMENT '审核通过后开通的用户ID' AFTER verify_time;

-- 回填历史已开通申请单：按手机号反查 sys_user（注册时手机号全局唯一）
UPDATE sys_register_application ra
JOIN sys_user u ON u.phone = ra.phone
SET ra.user_id = u.id
WHERE ra.verify_status = 'APPROVED'
  AND ra.user_id IS NULL;
