package cn.cordys.crm.ai.content.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.content.dto.request.AiContentGenerateRequest;
import cn.cordys.crm.ai.content.dto.response.AiContentGenerateResponse;
import cn.cordys.crm.ai.content.dto.response.AiContentItem;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.security.SessionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 获客内容生成（功能 3）：输入行业 + 产品/卖点 + 目标平台，产出选题/爆款文案/配图文案。
 * 复用会话军师的额度→模型→provider→计费四步链路，按 {@code ai_acquire} 记账；纯生成型，不落库。
 */
@Service
@Slf4j
public class AiContentGenerateService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final int DEFAULT_TOPIC_COUNT = 5;

    private static final int MAX_TOPIC_COUNT = 10;

    /** 蒸馏 marketingskills 的 social / copywriting / content-strategy / image 四个 skill，见功能 3 PRD */
    private static final String SYSTEM_PROMPT = """
            你是资深社交媒体运营与转化文案专家。请根据用户提供的「行业 + 产品/卖点 + 目标平台」，生成一批可直接发布的获客物料包。
            写作铁律：
            1. 清晰优于机巧：宁可用大白话讲清楚，不用看不懂的俏皮话。
            2. 讲利益不讲功能：说客户能得到的改变/好处，而不是罗列参数。
            3. 具体优于模糊：用场景、数字、对比代替「高效、优质、专业」这类空话。

            按平台特点写作：
            - 抖音：3 秒钩子开头、口播节奏短句、爆点前置、强互动引导（评论/关注）。
            - 小红书：种草体、带 emoji、利他干货、真诚人设。
            - 微信朋友圈：软性信任感、场景代入、不硬广、口语化、像朋友分享。

            每条内容是一份「可直接发布的物料包」，包含以下字段（字段按平台有所侧重，不相关字段输出空字符串）：
            - topic：选题（一句话角度）
            - title：标题（发布标题，抖音/小红书要有钩子感）
            - copy：正文文案（小红书/朋友圈的正文；抖音可为口播字幕稿）
            - imageCopy：配图文案（配图/贴纸上的短文案）
            - coverCopy：封面文案（抖音封面、小红书首图上的大字）
            - hashtags：话题标签（字符串数组，2-4 个，带 # 号）
            - bestTime：最佳发布时间（如「工作日 19:00-21:00」）
            - script：口播脚本（视频口播稿，含开头钩子；朋友圈可留空）

            输出严格 JSON（不要 markdown 代码块、不要任何多余文字），结构如下：
            {"contents":[{"topic":"选题","title":"标题","copy":"正文","imageCopy":"配图文案","coverCopy":"封面文案","hashtags":["#标签1","#标签2"],"bestTime":"发布时间","script":"口播脚本"}]}
            只输出上述 JSON 本身，不要任何前后缀。
            """;

    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;

    public AiContentGenerateResponse generate(String organizationId, AiContentGenerateRequest request) {
        if (StringUtils.isBlank(request.getIndustry())) {
            throw new GenericException("请填写行业");
        }
        if (StringUtils.isBlank(request.getProduct())) {
            throw new GenericException("请填写产品/卖点");
        }
        int topicCount = request.getTopicCount() == null ? DEFAULT_TOPIC_COUNT
                : Math.min(Math.max(request.getTopicCount(), 1), MAX_TOPIC_COUNT);
        String platform = resolvePlatform(request.getPlatform());

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

        StringBuilder content = new StringBuilder();
        AtomicBoolean emitted = new AtomicBoolean(false);
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
                llmRequest.setMessages(buildMessages(request, platform, topicCount));

                LlmUsage usage = provider.chatStream(llmRequest, chunk -> {
                    emitted.set(true);
                    content.append(chunk);
                });
                aiQuotaService.record(organizationId, AiQuotaConstant.AI_ACQUIRE, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return parseResult(content.toString(), topicCount);
            } catch (Exception e) {
                lastError = e;
                if (emitted.get()) {
                    log.error("获客内容生成模型调用中途失败，provider={}, model={}", model.getProvider(), model.getModelName(), e);
                    throw new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("获客内容生成模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }

        log.error("获客内容生成全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    private List<LlmMessage> buildMessages(AiContentGenerateRequest request, String platform, int topicCount) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", SYSTEM_PROMPT));
        StringBuilder sb = new StringBuilder();
        sb.append("行业：").append(request.getIndustry().trim()).append('\n')
                .append("产品/卖点：").append(request.getProduct().trim()).append('\n')
                .append("目标平台：").append(platform).append('\n')
                .append("请生成 ").append(topicCount).append(" 条内容。");
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    /** 平台编码映射为中文，用于注入 prompt 明确平台风格；未知值兜底抖音 */
    private String resolvePlatform(String platform) {
        if (platform == null) {
            return "抖音";
        }
        return switch (platform.trim().toLowerCase()) {
            case "xiaohongshu", "小红书" -> "小红书";
            case "moments", "朋友圈", "微信朋友圈" -> "微信朋友圈";
            default -> "抖音";
        };
    }

    /** 解析模型返回 JSON：读 contents 数组，过滤空条目并截断到 topicCount；解析失败回退原文 */
    private AiContentGenerateResponse parseResult(String text, int topicCount) {
        AiContentGenerateResponse response = new AiContentGenerateResponse();
        String json = extractJson(text);
        if (json == null) {
            response.setRawContent(text);
            return response;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode arr = root.get("contents");
            if (arr == null || !arr.isArray()) {
                response.setRawContent(text);
                return response;
            }
            List<AiContentItem> contents = new ArrayList<>();
            for (JsonNode node : arr) {
                String topic = node.path("topic").asText(null);
                String title = node.path("title").asText(null);
                String copy = node.path("copy").asText(null);
                if (StringUtils.isBlank(topic) && StringUtils.isBlank(title) && StringUtils.isBlank(copy)) {
                    continue;
                }
                AiContentItem item = new AiContentItem();
                item.setTopic(topic);
                item.setTitle(title);
                item.setCopy(copy);
                item.setImageCopy(node.path("imageCopy").asText(null));
                item.setCoverCopy(node.path("coverCopy").asText(null));
                item.setHashtags(parseHashtags(node.path("hashtags")));
                item.setBestTime(node.path("bestTime").asText(null));
                item.setScript(node.path("script").asText(null));
                contents.add(item);
                if (contents.size() >= topicCount) {
                    break;
                }
            }
            response.setContents(contents);
        } catch (Exception e) {
            log.warn("获客内容生成结果 JSON 解析失败，回退原文", e);
            response.setRawContent(text);
        }
        return response;
    }

    /** 解析话题标签数组，兼容模型偶发返回字符串的情况；过滤空值并截断到 4 个 */
    private List<String> parseHashtags(JsonNode node) {
        List<String> hashtags = new ArrayList<>();
        if (node == null || node.isNull()) {
            return hashtags;
        }
        if (node.isArray()) {
            for (JsonNode t : node) {
                String tag = t.asText(null);
                if (StringUtils.isNotBlank(tag)) {
                    hashtags.add(tag.trim());
                    if (hashtags.size() >= 4) {
                        break;
                    }
                }
            }
            return hashtags;
        }
        String single = node.asText(null);
        if (StringUtils.isNotBlank(single)) {
            hashtags.add(single.trim());
        }
        return hashtags;
    }

    /** 剥 ```json ... ``` 包裹，取首个 { 到末个 } 的 JSON 片段；无合法片段返回 null */
    private String extractJson(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int start = t.indexOf('\n');
            if (start < 0) {
                return null;
            }
            t = t.substring(start + 1);
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
            t = t.trim();
        }
        int begin = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (begin < 0 || end < 0 || end <= begin) {
            return null;
        }
        return t.substring(begin, end + 1);
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
