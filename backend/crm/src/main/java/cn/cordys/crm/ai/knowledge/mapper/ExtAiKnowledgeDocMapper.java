package cn.cordys.crm.ai.knowledge.mapper;

import cn.cordys.crm.ai.knowledge.dto.response.AiKnowledgeDocResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 企业知识库文档扩展 Mapper（分页列表）。
 */
public interface ExtAiKnowledgeDocMapper {

    /** 分页查询：按租户隔离，keyword 匹配文档名，更新时间倒序 */
    List<AiKnowledgeDocResponse> selectPage(@Param("orgId") String orgId,
                                            @Param("keyword") String keyword);
}
