-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 阶段配置表的 id 是语义键（CREATE/SUCCESS/FAIL/VOID/SIGNED 等），被前后端代码硬编码引用，
-- 不能按租户重映射。多租户下每个组织需要各自独立定制的一份阶段配置，
-- 故将主键由单列 id 改为复合主键 (id, organization_id)。
ALTER TABLE opportunity_stage_config DROP PRIMARY KEY, ADD PRIMARY KEY (`id`, `organization_id`);
ALTER TABLE contract_stage_config DROP PRIMARY KEY, ADD PRIMARY KEY (`id`, `organization_id`);
ALTER TABLE sales_order_stage_config DROP PRIMARY KEY, ADD PRIMARY KEY (`id`, `organization_id`);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
