package cn.cordys.crm.ai.script.mapper;

import cn.cordys.crm.ai.script.domain.AiSalesScript;
import cn.cordys.crm.ai.script.dto.response.AiSalesScriptResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 销售话术库扩展 Mapper（分页、分类选项、检索候选加载）。
 */
public interface ExtAiSalesScriptMapper {

    /** 分页查询：按租户隔离，keyword 匹配标题/内容，category 等值过滤 */
    List<AiSalesScriptResponse> selectPage(@Param("orgId") String orgId,
                                           @Param("keyword") String keyword,
                                           @Param("category") String category);

    /** 当前租户已用分类去重列表 */
    List<String> selectCategories(@Param("orgId") String orgId);

    /** 检索候选：按租户 + 可选分类，更新时间倒序取前 limit 条 */
    List<AiSalesScript> selectCandidates(@Param("orgId") String orgId,
                                         @Param("category") String category,
                                         @Param("limit") int limit);
}
