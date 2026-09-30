package cn.cordys.crm.ai.model.mapper;

import cn.cordys.crm.ai.model.dto.response.AgentModelResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 模型配置扩展 Mapper（分页 + 今日用量统计 + 创建人名称）。
 */
public interface ExtAgentModelMapper {

    List<AgentModelResponse> selectPage(@Param("orgId") String orgId,
                                        @Param("keyword") String keyword,
                                        @Param("todayStart") long todayStart,
                                        @Param("todayEnd") long todayEnd);
}
