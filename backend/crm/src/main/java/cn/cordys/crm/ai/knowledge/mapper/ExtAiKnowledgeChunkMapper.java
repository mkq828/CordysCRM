package cn.cordys.crm.ai.knowledge.mapper;

import cn.cordys.crm.ai.knowledge.domain.AiKnowledgeChunk;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 企业知识库分块扩展 Mapper（关键词召回、兜底召回、删除）。
 */
public interface ExtAiKnowledgeChunkMapper {

    /** 关键词召回：任一关键词命中 content 的块，按创建时间倒序取前 limit 条（命中数排序在服务层完成） */
    List<AiKnowledgeChunk> selectByKeywords(@Param("orgId") String orgId,
                                            @Param("keywords") List<String> keywords,
                                            @Param("limit") int limit);

    /** 兜底召回：关键词无命中时，取最近上传文档的最新块 */
    List<AiKnowledgeChunk> selectRecent(@Param("orgId") String orgId,
                                        @Param("limit") int limit);

    /** 按文档删除其全部分块（同租户校验） */
    int deleteByDocId(@Param("orgId") String orgId, @Param("docId") String docId);

    /** 加载租户下已向量化的分块（仅 id/content/vector 等必要字段），供内存余弦检索 */
    List<AiKnowledgeChunk> selectVectorizedByOrg(@Param("orgId") String orgId);

    /** 更新单个分块的向量（懒加载回填用） */
    int updateVector(@Param("id") String id, @Param("vector") String vector);
}
