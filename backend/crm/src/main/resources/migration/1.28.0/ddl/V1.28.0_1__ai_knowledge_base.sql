SET SESSION innodb_lock_wait_timeout = 7200;

-- 企业知识库文档（功能 4）：租户上传的产品资料/制度/话术等文档，解析后按块存储供 AI 检索问答
CREATE TABLE ai_knowledge_doc
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    name            VARCHAR(255) NOT NULL COMMENT '文档名(原始文件名)',
    file_type       VARCHAR(16)  NOT NULL COMMENT '文件类型(pdf/docx/md/txt)',
    file_size       BIGINT       COMMENT '文件大小(字节)',
    status          VARCHAR(16)  NOT NULL DEFAULT 'READY' COMMENT '解析状态(READY/FAILED)',
    chunk_count     INT          NOT NULL DEFAULT 0 COMMENT '分块数',
    error_msg       VARCHAR(500) COMMENT '解析失败原因',
    create_time     BIGINT       COMMENT '创建时间(毫秒)',
    update_time     BIGINT       COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)  COMMENT '创建人',
    update_user     VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org (organization_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 企业知识库文档分块（功能 4）：按约 500 字符切块，是检索问答的基本召回单位
CREATE TABLE ai_knowledge_chunk
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    doc_id          VARCHAR(32)  NOT NULL COMMENT '所属文档ID',
    seq             INT          NOT NULL COMMENT '块序号(从0起)',
    content         TEXT         NOT NULL COMMENT '块文本',
    create_time     BIGINT       COMMENT '创建时间(毫秒)',
    update_time     BIGINT       COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)  COMMENT '创建人',
    update_user     VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_doc (doc_id),
    KEY idx_org (organization_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
