ALTER TABLE ai_knowledge_doc
    ADD COLUMN version        VARCHAR(64) NULL COMMENT '版本号(自由文本，如 V1.2 / 2026年第3版)',
    ADD COLUMN effective_date DATE        NULL COMMENT '生效日期，NULL=立即生效',
    ADD COLUMN revoked        TINYINT     NOT NULL DEFAULT 0 COMMENT '是否废止(1=已废止)';
