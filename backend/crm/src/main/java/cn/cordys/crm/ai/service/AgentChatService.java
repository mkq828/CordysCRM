package cn.cordys.crm.ai.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.request.AiChatStreamRequest;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * AI 对话服务：额度校验 → 租户模型解析（agent_model + 路由策略）→ 流式调用 → 记账。
 */
@Service
@Slf4j
public class AgentChatService {

    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;

    /**
     * 流式对话。onChunk 回调增量文本，返回 token 用量。
     *
     * @throws GenericException 额度被拦截或模型调用失败
     */
    public LlmUsage chat(String organizationId, AiChatStreamRequest request, Consumer<String> onChunk) {
        if (StringUtils.isBlank(request.getMessage())) {
            throw new GenericException("请输入内容");
        }

        List<AgentModel> models = agentModelService.resolveChatModels(organizationId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }

        String userId = SessionUtils.getUserId();
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(organizationId);
        String status = quota.getStatus();
        if (isBlocked(status)) {
            throw new GenericException(blockedMessage(status));
        }

        // 记录是否已输出内容：首个模型一旦产出内容便不再降级，避免半截输出与备用模型内容拼接错乱
        AtomicBoolean emitted = new AtomicBoolean(false);
        Consumer<String> guardedChunk = chunk -> {
            emitted.set(true);
            onChunk.accept(chunk);
        };

        Exception lastError = null;
        for (AgentModel model : models) {
            try {
                aiQuotaService.checkModelDailyLimit(organizationId, model, userId);
                LlmProvider provider = llmProviderFactory.get(model.getProvider());
                LlmChatRequest llmRequest = new LlmChatRequest();
                llmRequest.setModel(model.getModelName());
                llmRequest.setBaseUrl(model.getApiUrl());
                llmRequest.setApiKey(model.getApiKey());
                agentModelService.applyModelParams(llmRequest, model);
                llmRequest.setMessages(List.of(new LlmMessage("user", request.getMessage())));

                LlmUsage usage = provider.chatStream(llmRequest, guardedChunk);
                aiQuotaService.record(organizationId, AiQuotaConstant.AI_CHAT, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return usage;
            } catch (Exception e) {
                lastError = e;
                if (emitted.get()) {
                    log.error("模型调用中途失败，provider={}, model={}", model.getProvider(), model.getModelName(), e);
                    throw new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }

        log.error("全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    private boolean isBlocked(String status) {
        return AiQuotaConstant.STATUS_HARD_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)
                || AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status);
    }

    private String blockedMessage(String status) {
        if (AiQuotaConstant.STATUS_RATE_LIMITED.equals(status)) {
            return "调用过于频繁，请稍后再试";
        }
        if (AiQuotaConstant.STATUS_CIRCUIT_BROKEN.equals(status)) {
            return "AI 服务暂不可用，请联系管理员";
        }
        return "AI 额度已用完，请加购或升级";
    }
}
