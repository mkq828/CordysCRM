package cn.cordys.crm.ai.ppt.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.ppt.dto.PptSection;
import cn.cordys.crm.ai.ppt.dto.request.AiPptOutlineRequest;
import cn.cordys.crm.ai.ppt.dto.request.AiPptSlidesRequest;
import cn.cordys.crm.ai.ppt.dto.response.AiPptOutlineResult;
import cn.cordys.crm.ai.ppt.dto.response.AiPptSlidesResult;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.ai.service.FencedJsonStreamer;
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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * AI 方案/PPT 生成（功能 5）：两步式——① 客户背景 → 解决方案大纲；② 大纲 → PPT 逐页。
 * 复用会话军师/获客内容的额度→模型→provider→计费四步链路，按 {@code ai_ppt} 记账；纯生成型，不落库。
 */
@Service
@Slf4j
public class AiPptService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 大纲 system prompt：6 区块结构化输出 */
    private static final String OUTLINE_SYSTEM_PROMPT = """
            你是资深售前解决方案顾问。请根据用户提供的「客户背景」，产出一份结构化的解决方案大纲。
            写作要求：
            1. 站在客户价值角度：讲清客户现状、痛点、我们如何解决、能带来什么结果，而不是堆功能。
            2. 具体优于空话：用可落地的功能点、实施步骤、量化收益代替「高效、优质、专业」这类空话。
            3. 固定 6 个区块，顺序不变：项目背景与客户需求、解决方案设计、功能架构、实施计划、报价建议、风险与保障。

            先输出一句简短引导语（如「已为您梳理方案大纲：」），引导语直接说明内容，不要以「好的」「好的，」「没问题」「OK」等客套话开头；空一行，再用 ```json 代码块输出严格 JSON（不要任何多余文字），结构如下：
            {"title":"方案标题","sections":[{"key":"background","title":"项目背景与客户需求","content":"该区块的 Markdown 内容，可用小标题、列表、加粗"},{"key":"solution","title":"解决方案设计","content":"..."},{"key":"architecture","title":"功能架构","content":"..."},{"key":"implementation","title":"实施计划","content":"..."},{"key":"pricing","title":"报价建议","content":"..."},{"key":"risk","title":"风险与保障","content":"..."}]}
            content 字段为 Markdown 字符串，可包含 # 小标题、- 列表、**加粗**。只输出引导语 + JSON 代码块，不要其他内容。
            """;

    /** 逐页 system prompt：大纲 → 逐页标题 + 要点 + 讲解词 */
    private static final String SLIDES_SYSTEM_PROMPT = """
            你是资深售前 PPT 撰写专家。请把用户提供的「解决方案大纲」转成一份演示 PPT 的逐页内容，可直接用于提案讲解。
            要求：
            1. 第一页为封面（标题=方案主标题，bullets 放副标题/客户名/日期等占位信息）。
            2. 每个大纲区块至少生成 1 页，最多 2 页；每页 3-5 个要点。
            3. 要点要短句有力（一句话讲清一个点），不要长段落。
            4. 每页配一段「讲解词」（notes，销售口头讲解的话，100 字以内）。
            5. 最后一页可为「致谢 / 下一步」。

            先输出一句简短引导语（如「已为您生成 PPT 逐页内容：」），引导语直接说明内容，不要以「好的」「好的，」「没问题」「OK」等客套话开头；空一行，再用 ```json 代码块输出严格 JSON（不要任何多余文字），结构如下：
            {"title":"方案标题","slides":[{"title":"封面","bullets":["副标题","客户：xxx"],"notes":"讲解词"},{"title":"项目背景","bullets":["要点1","要点2","要点3"],"notes":"讲解词"}]}
            bullets 是字符串数组；notes 是单字符串。只输出引导语 + JSON 代码块，不要其他内容。
            """;

    @Resource
    private AiQuotaService aiQuotaService;
    @Resource
    private AgentModelService agentModelService;
    @Resource
    private LlmProviderFactory llmProviderFactory;

    /** 流式生成方案大纲：先流式输出引导语，再解析 JSON 大纲。 */
    public AiStreamResult<AiPptOutlineResult> generateOutline(String organizationId,
            AiPptOutlineRequest request, Consumer<String> onChunk) {
        if (StringUtils.isBlank(request.getIndustry())) {
            throw new GenericException("请填写行业");
        }
        if (StringUtils.isBlank(request.getProduct())) {
            throw new GenericException("请填写产品/服务");
        }
        if (StringUtils.isBlank(request.getBackground())) {
            throw new GenericException("请填写客户背景与痛点");
        }
        return doGenerate(organizationId, AiQuotaConstant.AI_PPT, buildOutlineMessages(request), this::parseOutline, onChunk);
    }

    /** 流式生成 PPT 逐页：入参为（可编辑后的）大纲区块。 */
    public AiStreamResult<AiPptSlidesResult> generateSlides(String organizationId,
            AiPptSlidesRequest request, Consumer<String> onChunk) {
        if (request.getSections() == null || request.getSections().isEmpty()) {
            throw new GenericException("请先生成方案大纲");
        }
        return doGenerate(organizationId, AiQuotaConstant.AI_PPT, buildSlidesMessages(request), this::parseSlides, onChunk);
    }

    /** 通用生成链路：解析模型 → 额度预检 → 候选模型降级循环 → 计费 → 结构化解析。 */
    private <T> AiStreamResult<T> doGenerate(String organizationId, String featureCode, List<LlmMessage> messages,
            Function<String, T> parser, Consumer<String> onChunk) {
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

        FencedJsonStreamer streamer = new FencedJsonStreamer(onChunk);
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
                llmRequest.setMessages(messages);

                LlmUsage usage = provider.chatStream(llmRequest, chunk -> {
                    emitted.set(true);
                    streamer.accept(chunk);
                });
                aiQuotaService.record(organizationId, featureCode, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return new AiStreamResult<>(parser.apply(streamer.text()), usage);
            } catch (Exception e) {
                lastError = e;
                if (emitted.get()) {
                    log.error("方案/PPT 生成模型调用中途失败，provider={}, model={}",
                            model.getProvider(), model.getModelName(), e);
                    throw new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("方案/PPT 生成模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }

        log.error("方案/PPT 生成全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    private List<LlmMessage> buildOutlineMessages(AiPptOutlineRequest request) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", OUTLINE_SYSTEM_PROMPT));
        StringBuilder sb = new StringBuilder();
        sb.append("行业：").append(trim(request.getIndustry())).append('\n')
                .append("产品/服务：").append(trim(request.getProduct())).append('\n')
                .append("客户背景与痛点：").append(trim(request.getBackground()));
        if (StringUtils.isNotBlank(request.getCustomerName())) {
            sb.append("\n客户名称：").append(request.getCustomerName().trim());
        }
        if (StringUtils.isNotBlank(request.getBudget())) {
            sb.append("\n预算：").append(request.getBudget().trim());
        }
        if (StringUtils.isNotBlank(request.getCompetitor())) {
            sb.append("\n竞品：").append(request.getCompetitor().trim());
        }
        if (StringUtils.isNotBlank(request.getFocus())) {
            sb.append("\n方案侧重：").append(request.getFocus().trim());
        }
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    private List<LlmMessage> buildSlidesMessages(AiPptSlidesRequest request) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", SLIDES_SYSTEM_PROMPT));
        StringBuilder sb = new StringBuilder();
        sb.append("方案标题：").append(trim(request.getTitle())).append('\n');
        sb.append("方案大纲：\n");
        for (PptSection section : request.getSections()) {
            sb.append("## ").append(trim(section.getTitle())).append('\n')
                    .append(trim(section.getContent())).append('\n');
        }
        messages.add(new LlmMessage("user", sb.toString()));
        return messages;
    }

    /** 解析大纲 JSON：读 title + sections 数组；解析失败回退原文 */
    private AiPptOutlineResult parseOutline(String text) {
        AiPptOutlineResult result = new AiPptOutlineResult();
        String json = extractJson(text);
        if (json == null) {
            result.setRawContent(text);
            return result;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            result.setTitle(root.path("title").asText(null));
            JsonNode arr = root.path("sections");
            if (arr == null || !arr.isArray()) {
                result.setRawContent(text);
                return result;
            }
            List<PptSection> sections = new ArrayList<>();
            for (JsonNode node : arr) {
                String title = node.path("title").asText(null);
                String content = node.path("content").asText(null);
                if (StringUtils.isBlank(title) && StringUtils.isBlank(content)) {
                    continue;
                }
                PptSection section = new PptSection();
                section.setKey(node.path("key").asText(null));
                section.setTitle(title);
                section.setContent(content);
                sections.add(section);
            }
            result.setSections(sections);
        } catch (Exception e) {
            log.warn("方案大纲 JSON 解析失败，回退原文", e);
            result.setRawContent(text);
        }
        return result;
    }

    /** 解析逐页 JSON：读 title + slides 数组；解析失败回退原文 */
    private AiPptSlidesResult parseSlides(String text) {
        AiPptSlidesResult result = new AiPptSlidesResult();
        String json = extractJson(text);
        if (json == null) {
            result.setRawContent(text);
            return result;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            result.setTitle(root.path("title").asText(null));
            JsonNode arr = root.path("slides");
            if (arr == null || !arr.isArray()) {
                result.setRawContent(text);
                return result;
            }
            List<AiPptSlidesResult.PptSlide> slides = new ArrayList<>();
            for (JsonNode node : arr) {
                AiPptSlidesResult.PptSlide slide = new AiPptSlidesResult.PptSlide();
                slide.setTitle(node.path("title").asText(null));
                slide.setBullets(parseStringArray(node.path("bullets")));
                slide.setNotes(node.path("notes").asText(null));
                if (StringUtils.isBlank(slide.getTitle()) && slide.getBullets().isEmpty()
                        && StringUtils.isBlank(slide.getNotes())) {
                    continue;
                }
                slides.add(slide);
            }
            result.setSlides(slides);
        } catch (Exception e) {
            log.warn("PPT 逐页 JSON 解析失败，回退原文", e);
            result.setRawContent(text);
        }
        return result;
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return list;
        }
        for (JsonNode item : node) {
            String value = item.asText(null);
            if (StringUtils.isNotBlank(value)) {
                list.add(value.trim());
            }
        }
        return list;
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

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
