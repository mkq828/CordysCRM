SET SESSION innodb_lock_wait_timeout = 7200;

-- 跟进方式（recordMethod/planMethod）补齐「微信」选项：原表单模板只有到访(1)/电话(2)，
-- 实际销售跟进以「电话、微信」为主，补上微信(3) 并同步已存在的各租户表单配置。
-- 幂等：仅处理仍只有到访/电话两选项、且尚未含「微信」的表单，重复执行不会重复追加。

UPDATE sys_module_field_blob b
    JOIN sys_module_field f ON f.id = b.id
SET b.prop = REPLACE(
        b.prop,
        '{"value":"1","label":"到访","disabled":null},{"value":"2","label":"电话","disabled":null}',
        '{"value":"1","label":"到访","disabled":null},{"value":"2","label":"电话","disabled":null},{"value":"3","label":"微信","disabled":null}'
    )
WHERE f.internal_key IN ('recordMethod', 'planMethod')
  AND b.prop LIKE '%"label":"电话"%'
  AND b.prop NOT LIKE '%"label":"微信"%';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
