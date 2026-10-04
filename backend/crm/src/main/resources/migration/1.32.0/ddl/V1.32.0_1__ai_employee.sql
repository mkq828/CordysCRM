SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 员工（功能 6）：系统级员工角色目录，来源于 agency-agents（MIT，保留署名），按部门沉淀 264 个岗位角色与提示词
CREATE TABLE ai_employee
(
    id            VARCHAR(32)  NOT NULL COMMENT '主键',
    division      VARCHAR(64)  NOT NULL COMMENT '部门英文标识(sales/marketing/engineering...)',
    name          VARCHAR(128) NOT NULL COMMENT '员工角色名',
    description   TEXT COMMENT '角色描述',
    color         VARCHAR(16) COMMENT '品牌色(hex)',
    emoji         VARCHAR(16) COMMENT 'emoji 图标',
    vibe          VARCHAR(255) COMMENT '一句话人设',
    system_prompt MEDIUMTEXT COMMENT '角色提示词(系统提示，Markdown)',
    sort          BIGINT COMMENT '同部门内排序',
    create_time   BIGINT COMMENT '创建时间(毫秒)',
    update_time   BIGINT COMMENT '更新时间(毫秒)',
    create_user   VARCHAR(32) COMMENT '创建人',
    update_user   VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_division (division, sort)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- AI 员工：租户「岗位 Agent」——从员工目录挑一个角色，落到具体岗位并附加自定义配置
CREATE TABLE ai_employee_agent
(
    id                VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id   VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    name              VARCHAR(128) NOT NULL COMMENT '岗位 Agent 名称',
    employee_id       VARCHAR(32)  NOT NULL COMMENT '关联员工角色(ai_employee.id)',
    custom_prompt     TEXT COMMENT '追加的自定义提示词(可选，拼在角色提示词后)',
    knowledge_enabled TINYINT      NOT NULL DEFAULT 0 COMMENT '是否挂知识库检索(1=是)',
    sort              BIGINT COMMENT '排序',
    create_time       BIGINT COMMENT '创建时间(毫秒)',
    update_time       BIGINT COMMENT '更新时间(毫秒)',
    create_user       VARCHAR(32) COMMENT '创建人',
    update_user       VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org (organization_id, sort)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
