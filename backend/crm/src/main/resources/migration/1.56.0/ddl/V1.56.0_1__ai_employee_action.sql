SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 员工「自动跑任务」二期：自动跟进闭环。
-- 定时扫描「久未联系」客户 → LLM 生成跟进话术草稿 → 落一条待确认动作 → 站内信通知客户负责人 → 负责人确认/驳回。

CREATE TABLE ai_employee_action
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    action_type     VARCHAR(32)  NOT NULL DEFAULT 'FOLLOW_UP_CREATE' COMMENT '动作类型(预留扩展)',
    agent_id        VARCHAR(32)  COMMENT '岗位 Agent ID(定时扫描无岗位,可空)',
    customer_id     VARCHAR(32)  NOT NULL COMMENT '客户ID',
    customer_name   VARCHAR(128) COMMENT '客户名称(冗余展示)',
    owner_id        VARCHAR(32)  COMMENT '客户负责人(确认人)',
    payload         TEXT         COMMENT '动作载荷(JSON: content/followMethod/suggestion)',
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/EXECUTED/FAILED',
    result          TEXT         COMMENT '执行结果(可空)',
    confirm_user    VARCHAR(32)  COMMENT '确认人',
    confirm_time    BIGINT       COMMENT '确认时间(毫秒)',
    create_time     BIGINT       COMMENT '创建时间(毫秒)',
    update_time     BIGINT       COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)  COMMENT '创建人',
    update_user     VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_status (organization_id, status),
    KEY idx_owner (owner_id),
    KEY idx_customer (customer_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
