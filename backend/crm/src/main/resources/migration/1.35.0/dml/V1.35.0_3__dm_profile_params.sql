-- 私信采集服务（16.test）接入参数：默认空值，部署时在 sys_parameter 填入实际值即可，无需改代码
-- ai.dm.baseUrl      采集服务地址，如 http://127.0.0.1:8000
-- ai.dm.internalToken 服务间鉴权 token，与采集服务 .env 的 INTERNAL_TOKEN 一致；为空则跳过校验（仅本地开发）
INSERT IGNORE INTO sys_parameter (`param_key`, `param_value`, `type`)
VALUES ('ai.dm.baseUrl', '', 'text'),
       ('ai.dm.internalToken', '', 'text');
