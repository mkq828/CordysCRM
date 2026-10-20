SET SESSION innodb_lock_wait_timeout = 7200;

-- 安全事件留证：记录疑似恶意入侵/渗透/攻击的请求证据，用于报案取证。
-- 只记录不拦截，避免误杀正常业务。append-only，不设 update_time/update_user。

CREATE TABLE sys_security_event
(
    id              VARCHAR(32)   NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)   COMMENT '租户组织ID(未登录攻击为空)',
    event_type      VARCHAR(64)   NOT NULL COMMENT '事件类型(sql_injection/xss/path_traversal/scanner_ua/error_flood)',
    remote_addr     VARCHAR(64)   COMMENT '直连IP(TCP对端，可信)',
    client_ip       VARCHAR(64)   COMMENT '客户端IP(X-Forwarded-For首段，可伪造)',
    forward_chain   VARCHAR(512)  COMMENT '完整X-Forwarded-For转发链',
    ip_region       VARCHAR(64)   COMMENT 'IP归属地',
    method          VARCHAR(16)   COMMENT 'HTTP方法',
    uri             VARCHAR(2048) COMMENT '请求URI(含query，攻击载荷所在)',
    user_agent      VARCHAR(512)  COMMENT 'User-Agent',
    referer         VARCHAR(512)  COMMENT 'Referer',
    http_status     INT           COMMENT 'HTTP响应状态码',
    trace_id        VARCHAR(64)   COMMENT '链路追踪ID',
    detail          VARCHAR(255)  COMMENT '事件描述',
    create_time     BIGINT        COMMENT '创建时间(毫秒)',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_type (event_type),
    KEY idx_ip (client_ip)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
