CREATE TABLE suggestion
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    title           VARCHAR(200) NOT NULL COMMENT '标题',
    content         TEXT NOT NULL COMMENT '内容',
    organization_id VARCHAR(50) COMMENT '组织ID',
    user_id         VARCHAR(32) COMMENT '提议者用户ID',
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    image_ids       TEXT COMMENT '图片附件ID(JSON数组)',
    vote_count      INT NOT NULL DEFAULT 0 COMMENT '票数',
    create_time     BIGINT COMMENT '创建时间',
    update_time     BIGINT COMMENT '更新时间',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
CREATE INDEX idx_suggestion_status ON suggestion (status ASC);
CREATE INDEX idx_suggestion_org ON suggestion (organization_id ASC);

CREATE TABLE suggestion_vote
(
    id            VARCHAR(32) NOT NULL COMMENT '主键',
    suggestion_id VARCHAR(32) NOT NULL COMMENT '建议ID',
    user_id       VARCHAR(32) NOT NULL COMMENT '用户ID',
    create_time   BIGINT COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_suggestion_user (suggestion_id, user_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE TABLE suggestion_comment
(
    id               VARCHAR(32) NOT NULL COMMENT '主键',
    suggestion_id    VARCHAR(32) NOT NULL COMMENT '建议ID',
    user_id          VARCHAR(32) COMMENT '用户ID',
    content          TEXT NOT NULL COMMENT '内容',
    reply_comment_id VARCHAR(32) COMMENT '回复评论ID',
    create_time      BIGINT COMMENT '创建时间',
    update_time      BIGINT COMMENT '更新时间',
    create_user      VARCHAR(32) COMMENT '创建人',
    update_user      VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
CREATE INDEX idx_suggestion_comment ON suggestion_comment (suggestion_id ASC);
