package cn.cordys.crm.ai.conversation.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.conversation.domain.AgentConversation;
import cn.cordys.crm.ai.conversation.domain.AgentMessage;
import cn.cordys.crm.ai.conversation.dto.request.AgentConversationPageRequest;
import cn.cordys.crm.ai.conversation.dto.response.AgentConversationDetailResponse;
import cn.cordys.crm.ai.conversation.dto.response.AgentConversationResponse;
import cn.cordys.crm.ai.conversation.mapper.ExtAgentConversationMapper;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.mybatis.BaseMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 会话持久化服务：承载四种能力（chat/军师/问答/获客）共用的会话记录底座。
 * 既提供 {@code /agent-conversation/*} 的增删改查，也提供流式过程里的会话/消息落库。
 */
@Service
@Slf4j
public class AgentConversationService {

    /** 哆咪AI对话的会话分组码（区别于额度记账的 ai_chat） */
    public static final String FEATURE_CHAT = "chat";

    private static final String DEFAULT_FEATURE_CODE = FEATURE_CHAT;

    private static final String ROLE_USER = "USER";

    private static final String ROLE_ASSISTANT = "ASSISTANT";

    private static final String STATUS_DONE = "done";

    @Resource
    private BaseMapper<AgentConversation> conversationMapper;

    @Resource
    private BaseMapper<AgentMessage> messageMapper;

    @Resource
    private ExtAgentConversationMapper extConversationMapper;

    // ==================== 查询 ====================

    public Pager<List<AgentConversationResponse>> page(AgentConversationPageRequest request, String orgId, String userId) {
        String featureCode = StringUtils.isBlank(request.getFeatureCode()) ? DEFAULT_FEATURE_CODE : request.getFeatureCode();
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<AgentConversationResponse> list = extConversationMapper.selectPage(orgId, userId, featureCode, request.getKeyword());
        return PageUtils.setPageInfo(page, list);
    }

    public AgentConversationDetailResponse detail(String id, String orgId, String userId) {
        AgentConversation conversation = checkConversation(id, orgId, userId);
        AgentConversationDetailResponse response = new AgentConversationDetailResponse();
        response.setConversation(toResponse(conversation));
        response.setMessages(extConversationMapper.selectMessages(orgId, id));
        return response;
    }

    // ==================== 变更 ====================

    public void delete(String id, String orgId, String userId) {
        checkConversation(id, orgId, userId);
        AgentMessage criteria = new AgentMessage();
        criteria.setConversationId(id);
        criteria.setOrganizationId(orgId);
        messageMapper.delete(criteria);
        conversationMapper.deleteByPrimaryKey(id);
    }

    public void rename(String id, String title, String orgId, String userId) {
        if (StringUtils.isBlank(title)) {
            throw new GenericException("标题不能为空");
        }
        AgentConversation conversation = checkConversation(id, orgId, userId);
        long now = System.currentTimeMillis();
        conversation.setTitle(title.trim());
        conversation.setUpdateTime(now);
        conversation.setUpdateUser(userId);
        conversationMapper.update(conversation);
    }

    // ==================== 流式过程落库 ====================

    /** 解析会话 ID：传入的 ID 若归属当前租户与用户则复用，否则新建并返回新 ID */
    public String resolveConversation(String orgId, String userId, String featureCode, String conversationId, String title) {
        if (StringUtils.isNotBlank(conversationId)) {
            AgentConversation existing = conversationMapper.selectByPrimaryKey(conversationId);
            if (existing != null && orgId.equals(existing.getOrganizationId()) && userId.equals(existing.getUserId())) {
                return conversationId;
            }
        }
        return createConversation(orgId, userId, featureCode, title);
    }

    public String createConversation(String orgId, String userId, String featureCode, String title) {
        long now = System.currentTimeMillis();
        AgentConversation conversation = new AgentConversation();
        conversation.setId(IDGenerator.nextStr());
        conversation.setTitle(StringUtils.isBlank(title) ? "新会话" : title);
        conversation.setFeatureCode(StringUtils.isBlank(featureCode) ? DEFAULT_FEATURE_CODE : featureCode);
        conversation.setUserId(userId);
        conversation.setOrganizationId(orgId);
        conversation.setCreateUser(userId);
        conversation.setUpdateUser(userId);
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        conversationMapper.insert(conversation);
        return conversation.getId();
    }

    /** 落一条用户消息（流式开始前），同时把会话顶到列表最前 */
    public void saveUserMessage(String conversationId, String runId, String orgId, String userId,
                                String content, String payloadJson) {
        saveMessage(conversationId, runId, null, orgId, userId, ROLE_USER, content, payloadJson, STATUS_DONE, null);
    }

    /** 落一条 AI 消息（流式结束后），同时把会话顶到列表最前 */
    public void saveAssistantMessage(String conversationId, String runId, String messageId, String orgId, String userId,
                                     String content, String payloadJson, String status, LlmUsage usage) {
        saveMessage(conversationId, runId, messageId, orgId, userId, ROLE_ASSISTANT, content, payloadJson, status, usage);
    }

    private void saveMessage(String conversationId, String runId, String messageId, String orgId, String userId,
                             String role, String content, String payloadJson, String status, LlmUsage usage) {
        long now = System.currentTimeMillis();
        AgentMessage message = new AgentMessage();
        message.setId(StringUtils.isBlank(messageId) ? IDGenerator.nextStr() : messageId);
        message.setRole(role);
        message.setRunId(runId);
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setPayload(payloadJson);
        message.setOrganizationId(orgId);
        message.setStatus(status);
        if (usage != null) {
            message.setInputTokens(usage.getInputTokens());
            message.setOutputTokens(usage.getOutputTokens());
            message.setTotalTokens(usage.getTotalTokens());
        }
        message.setCreateUser(userId);
        message.setUpdateUser(userId);
        message.setCreateTime(now);
        message.setUpdateTime(now);
        messageMapper.insert(message);

        touchConversation(conversationId, orgId, userId);
    }

    private void touchConversation(String conversationId, String orgId, String userId) {
        AgentConversation conversation = conversationMapper.selectByPrimaryKey(conversationId);
        if (conversation == null || !orgId.equals(conversation.getOrganizationId())) {
            return;
        }
        long now = System.currentTimeMillis();
        conversation.setUpdateTime(now);
        conversation.setUpdateUser(userId);
        conversationMapper.update(conversation);
    }

    // ==================== 内部 ====================

    private AgentConversation checkConversation(String id, String orgId, String userId) {
        AgentConversation conversation = conversationMapper.selectByPrimaryKey(id);
        if (conversation == null || !orgId.equals(conversation.getOrganizationId())
                || !userId.equals(conversation.getUserId())) {
            throw new GenericException("会话不存在");
        }
        return conversation;
    }

    private AgentConversationResponse toResponse(AgentConversation conversation) {
        AgentConversationResponse response = new AgentConversationResponse();
        response.setId(conversation.getId());
        response.setTitle(conversation.getTitle());
        response.setFeatureCode(conversation.getFeatureCode());
        response.setUserId(conversation.getUserId());
        response.setOrganizationId(conversation.getOrganizationId());
        response.setCreateUser(conversation.getCreateUser());
        response.setUpdateUser(conversation.getUpdateUser());
        response.setCreateTime(conversation.getCreateTime());
        response.setUpdateTime(conversation.getUpdateTime());
        return response;
    }
}
