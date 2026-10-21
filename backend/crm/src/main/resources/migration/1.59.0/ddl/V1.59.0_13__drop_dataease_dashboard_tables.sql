SET SESSION innodb_lock_wait_timeout = 7200;

-- DataEase 看板管理下线：dashboard / dashboard_module / dashboard_collection 三张表对应的
-- Controller/Service/实体/Mapper 已删除，表成为孤儿表。新增迁移 DROP 掉，保证全新初始化库时不再残留空表。
-- 幂等（IF EXISTS）：已手动删表的环境与全新库均可安全执行。

DROP TABLE IF EXISTS dashboard_collection;
DROP TABLE IF EXISTS dashboard;
DROP TABLE IF EXISTS dashboard_module;
