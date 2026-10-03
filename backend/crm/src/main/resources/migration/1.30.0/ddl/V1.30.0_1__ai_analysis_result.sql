SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 分析结果沉淀：会话军师等生成型能力把结构化结论按业务对象落此表，客户画像/数据大屏只读回读
CREATE TABLE ai_analysis_result
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    biz_type        VARCHAR(32) NOT NULL COMMENT '业务对象类型: customer/clue/opportunity',
    biz_id          VARCHAR(32) NOT NULL COMMENT '业务对象ID(客户/线索/商机主键)',
    feature_code    VARCHAR(32) NOT NULL COMMENT '来源能力: ai_advisor=会话军师',
    title           VARCHAR(128) COMMENT '标题(如「会话军师分析」)',
    result_json     TEXT        COMMENT '结构化结论JSON(会话军师结构化字段)',
    model_code      VARCHAR(64) COMMENT '生成所用模型',
    create_time     BIGINT      COMMENT '创建时间(毫秒)',
    update_time     BIGINT      COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_biz (organization_id, biz_type, biz_id, create_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
