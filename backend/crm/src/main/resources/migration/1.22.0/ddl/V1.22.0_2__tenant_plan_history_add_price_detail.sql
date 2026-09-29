SET SESSION innodb_lock_wait_timeout = 7200;

-- 历史记录补充成交价计算过程（补差公式/首年价说明），供城市经理向客户解释、租户个人中心自查
ALTER TABLE tenant_plan_history
    ADD COLUMN price_detail VARCHAR(500) COMMENT '成交价计算过程(补差公式/首年价说明)';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
