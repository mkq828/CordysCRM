package cn.cordys.crm.ai.conversation.mapper;

import cn.cordys.crm.ai.conversation.domain.AgentMessage;
import cn.cordys.crm.ai.conversation.dto.response.AgentConversationResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 会话扩展 Mapper：会话分页列表 + 按会话取消息。
 */
public interface ExtAgentConversationMapper {

    /** 分页查询当前用户某能力下的会话列表，keyword 匹配标题，按更新时间倒序 */
    List<AgentConversationResponse> selectPage(@Param("orgId") String orgId,
                                               @Param("userId") String userId,
                                               @Param("featureCode") String featureCode,
                                               @Param("keyword") String keyword);

    /** 按会话取消息，按创建时间升序（回放顺序） */
    List<AgentMessage> selectMessages(@Param("orgId") String orgId,
                                      @Param("conversationId") String conversationId);
}
