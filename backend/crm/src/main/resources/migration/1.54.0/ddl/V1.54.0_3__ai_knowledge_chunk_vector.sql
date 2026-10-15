-- 企业知识库分块向量列：语义检索底座（text-embedding-v3 默认 1024 维，JSON 数组字符串）。
-- 老数据 vector 为 NULL，由检索链路懒加载回填（见 AiKnowledgeService.backfillVectors）。
ALTER TABLE ai_knowledge_chunk
    ADD COLUMN vector LONGTEXT NULL COMMENT '块向量(JSON数组，text-embedding-v3 默认1024维)' AFTER content;
