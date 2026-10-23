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

    /** 废止同名旧文档（排除自身），用于同名新版本上传后自动让旧版失效 */
    int revokeSameName(@Param("orgId") String orgId,
                       @Param("name") String name,
                       @Param("docId") String docId);

    /** 更新文档排序号（sortNo 可为 null，清空后回到「排最后」） */
    int updateSortNo(@Param("id") String id,
                     @Param("orgId") String orgId,
                     @Param("sortNo") Integer sortNo);
}
