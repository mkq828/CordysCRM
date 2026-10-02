-- 销售话术库：给平台演示租户（org 100001）预置 3 条示例话术，供首次进入时参考「怎么用」。
-- 上线前若确定最终文案，可改为：平移给所有租户（INSERT ... SELECT 复制到每个 organization_id），
-- 并在新租户注册流程里复制一份默认话术。
INSERT INTO ai_sales_script (id, organization_id, category, title, content, source, create_time, update_time, create_user, update_user)
VALUES
    ('ai_script_seed_price', '100001', '价格异议', '价格贵·算账重塑价值',
     '先认同客户：「这个价格确实不低」。再帮客户算一笔账——把投入摊到每一天或每一单，对比它带来的效率提升、人力节省和风险规避，把「贵」翻译成「值」。不要一上来就降价，先谈价值。',
     '系统示例', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
    ('ai_script_seed_competitor', '100001', '竞品对比', '竞品更便宜·聚焦差异',
     '先肯定客户做过对比：「您对比过，说明您是认真在决策」。再点出我们独有的能力（如权限管理、一键导出、售后服务），说明价格差买来的是这些确定性，而不是泛泛地说「我们更好」。',
     '系统示例', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
    ('ai_script_seed_close', '100001', '促单逼单', '客户犹豫·给台阶促单',
     '客户说「再考虑考虑」时，用一个明确的推进理由（限时优惠、阶梯报价、排期资源）给客户一个台阶，并当场约定下次沟通的具体时间，避免无限期拖延。',
     '系统示例', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');
