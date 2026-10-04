package cn.cordys.crm.ai.employee.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.employee.domain.AiMeetingMinutes;
import cn.cordys.crm.ai.employee.dto.request.AiMeetingMinutesGenerateRequest;
import cn.cordys.crm.ai.employee.dto.response.AiMeetingMinutesResponse;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 员工虚拟会议室「会议纪要」服务（功能 6，编排层）：把董事长提问 + 各岗位依次发言的流水交给任务模型总结成
 * 结构化纪要（标题 / 发言纪要 / 后续行动 / 任务清单），落库 {@code ai_meeting_minutes} 供「会议纪要」tab 回看。
 * 走任务模型 {@link AgentModelService#resolveTaskModels}（洞察/总结类，回落对话模型），复用额度→模型→provider→计费链路。
 */
@Service
@Slf4j
public class AiMeetingService {

    /** 记录人默认值（与 DeepSeek 参考页一致） */
    private static final String RECORDER = "董事会秘书";

    /** 会议地点默认值 */
    private static final String PLACE = "虚拟会议室";

    private static final String SYSTEM_PROMPT = """
            你是「董事会秘书」，负责把一场虚拟董事会的讨论整理成规范的会议纪要。
            只输出一个 JSON 对象（不要 Markdown 代码块、不要任何前后缀），结构如下：
            {
              "title": "会议标题（一句话概括议题）",
              "content": ["岗位名：观点摘要", "..."],
              "next": ["后续行动/决议", "..."],
              "tasks": [["任务描述", "截止日期", "交付物"], "..."]
            }
            要求：客观、简洁、不编造，只依据发言内容总结；content 按发言顺序逐条概括每位发言人的核心观点；tasks 里截止日期无法确定时写「待定」。
            """;

    @Resource
    private BaseMapper<AiMeetingMinutes> minutesMapper;

    @Resource
    private AiQuotaService aiQuotaService;

    @Resource
    private AgentModelService agentModelService;

    @Resource
    private LlmProviderFactory llmProviderFactory;

    /** 生成并落库会议纪要，返回完整纪要 */
    public AiMeetingMinutesResponse generateMinutes(String orgId, String userId, AiMeetingMinutesGenerateRequest request) {
        if (request == null || request.getTranscript() == null || request.getTranscript().isEmpty()) {
            throw new GenericException("暂无会议发言内容，无法生成纪要");
        }

        List<AgentModel> models = agentModelService.resolveTaskModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        String status = quota.getStatus();
        if (isBlocked(status)) {
            throw new GenericException(blockedMessage(status));
        }

        String transcriptText = buildTranscriptText(request);
        List<LlmMessage> messages = List.of(
                new LlmMessage("system", SYSTEM_PROMPT),
                new LlmMessage("user", transcriptText + "\n\n请据此生成会议纪要 JSON。"));

        String raw = callTaskModel(orgId, userId, models, messages);
        MinutesContent minutes = parseMinutes(raw);

        AiMeetingMinutes entity = new AiMeetingMinutes();
        entity.setId(IDGenerator.nextStr());
        entity.setOrganizationId(orgId);
        entity.setTitle(StringUtils.isBlank(request.getTitle()) ? minutes.title : request.getTitle().trim());
        entity.setTimeText(request.getTimeText());
        entity.setPlace(PLACE);
        entity.setHost(request.getHost());
        entity.setRecorder(RECORDER);
        entity.setAttendeesJson(JSON.toJSONString(request.getAttendees() == null ? List.of() : request.getAttendees()));
        entity.setContentJson(JSON.toJSONString(minutes.content));
        entity.setNextJson(JSON.toJSONString(minutes.next));
        entity.setTasksJson(JSON.toJSONString(minutes.tasks));
        long now = System.currentTimeMillis();
        entity.setCreateUser(userId);
        entity.setUpdateUser(userId);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        minutesMapper.insert(entity);

        return toResponse(entity);
    }

    /** 当前租户纪要列表（按创建时间倒序，仅返回列表所需字段） */
    public List<AiMeetingMinutesResponse> listMinutes(String orgId) {
        LambdaQueryWrapper<AiMeetingMinutes> wrapper = new LambdaQueryWrapper<AiMeetingMinutes>()
                .eq(AiMeetingMinutes::getOrganizationId, orgId);
        wrapper.orderByDesc(AiMeetingMinutes::getCreateTime);
        return minutesMapper.selectListByLambda(wrapper).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 纪要详情（含发言/后续/任务全文） */
    public AiMeetingMinutesResponse getMinutes(String id, String orgId) {
        AiMeetingMinutes minutes = minutesMapper.selectByPrimaryKey(id);
        if (minutes == null || !orgId.equals(minutes.getOrganizationId())) {
            throw new GenericException("会议纪要不存在");
        }
        return toResponse(minutes);
    }

    // ==================== 内部 ====================

    /** 把发言流水拼成供模型总结的纯文本 */
    private String buildTranscriptText(AiMeetingMinutesGenerateRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("会议主持人：").append(StringUtils.defaultIfBlank(request.getHost(), "董事长")).append('\n');
        List<String> attendees = request.getAttendees() == null ? List.of() : request.getAttendees();
        if (!attendees.isEmpty()) {
            sb.append("参会人：").append(String.join("、", attendees)).append('\n');
        }
        sb.append("会议发言流水：\n");
        for (AiMeetingMinutesGenerateRequest.Speech speech : request.getTranscript()) {
            if (speech == null) {
                continue;
            }
            sb.append("【").append(StringUtils.defaultIfBlank(speech.getSpeaker(), "发言人")).append("】")
                    .append(StringUtils.defaultString(speech.getContent())).append('\n');
        }
        return sb.toString();
    }

    /** 走任务模型候选链，累积输出全文；全部失败抛异常 */
    private String callTaskModel(String orgId, String userId, List<AgentModel> models, List<LlmMessage> messages) {
        StringBuilder content = new StringBuilder();
        Exception lastError = null;
        for (AgentModel model : models) {
            try {
                aiQuotaService.checkModelDailyLimit(orgId, model, userId);
                LlmProvider provider = llmProviderFactory.get(model.getProvider());
                LlmChatRequest llmRequest = new LlmChatRequest();
                llmRequest.setModel(model.getModelName());
                llmRequest.setBaseUrl(model.getApiUrl());
                llmRequest.setApiKey(model.getApiKey());
                agentModelService.applyModelParams(llmRequest, model);
                llmRequest.setMessages(messages);

                LlmUsage usage = provider.chatStream(llmRequest, content::append);
                aiQuotaService.record(orgId, AiQuotaConstant.AI_EMPLOYEE, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return content.toString();
            } catch (Exception e) {
                lastError = e;
                if (!content.isEmpty()) {
                    log.error("会议纪要生成中途失败，provider={}, model={}", model.getProvider(), model.getModelName(), e);
                    throw new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("会议纪要生成失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("会议纪要全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    /** 从模型输出里抽取并解析纪要 JSON（容忍代码块围栏等前后缀） */
    private MinutesContent parseMinutes(String raw) {
        String json = extractJson(raw);
        try {
            JsonNode root = JSON.MAPPER.readTree(json);
            MinutesContent result = new MinutesContent();
            result.title = root.path("title").asText("").trim();
            result.content = readStringList(root.path("content"));
            result.next = readStringList(root.path("next"));
            result.tasks = readTasks(root.path("tasks"));
            return result;
        } catch (Exception e) {
            log.error("解析会议纪要 JSON 失败，raw={}", StringUtils.abbreviate(raw, 500), e);
            throw new GenericException("会议纪要生成失败，请重试");
        }
    }

    /** 截取首个 { 到最后一个 } 之间的 JSON 文本 */
    private String extractJson(String raw) {
        if (StringUtils.isBlank(raw)) {
            throw new GenericException("会议纪要生成失败，请重试");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new GenericException("会议纪要生成失败，请重试");
        }
        return raw.substring(start, end + 1);
    }

    private List<String> readStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String text = item.asText("").trim();
                if (StringUtils.isNotBlank(text)) {
                    list.add(text);
                }
            }
        }
        return list;
    }

    /** 任务清单：兼容 [任务,截止,交付物] 三元组数组 与 {task,deadline,deliverable} 对象两种形态 */
    private List<AiMeetingMinutesResponse.Task> readTasks(JsonNode node) {
        List<AiMeetingMinutesResponse.Task> tasks = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return tasks;
        }
        for (JsonNode item : node) {
            AiMeetingMinutesResponse.Task task = new AiMeetingMinutesResponse.Task();
            if (item.isArray()) {
                task.setTask(item.size() > 0 ? item.get(0).asText("").trim() : "");
                task.setDeadline(item.size() > 1 ? item.get(1).asText("").trim() : "");
                task.setDeliverable(item.size() > 2 ? item.get(2).asText("").trim() : "");
            } else {
                task.setTask(item.path("task").asText("").trim());
                task.setDeadline(item.path("deadline").asText("").trim());
                task.setDeliverable(item.path("deliverable").asText("").trim());
            }
            if (StringUtils.isNotBlank(task.getTask())) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    private AiMeetingMinutesResponse toResponse(AiMeetingMinutes minutes) {
        AiMeetingMinutesResponse response = new AiMeetingMinutesResponse();
        response.setId(minutes.getId());
        response.setTitle(minutes.getTitle());
        response.setTimeText(minutes.getTimeText());
        response.setPlace(minutes.getPlace());
        response.setHost(minutes.getHost());
        response.setRecorder(minutes.getRecorder());
        response.setAttendees(readJsonArray(minutes.getAttendeesJson()));
        response.setContent(readJsonArray(minutes.getContentJson()));
        response.setNext(readJsonArray(minutes.getNextJson()));
        response.setTasks(readJsonTasks(minutes.getTasksJson()));
        response.setCreateTime(minutes.getCreateTime());
        return response;
    }

    private List<String> readJsonArray(String json) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        try {
            return JSON.parseArray(json, String.class);
        } catch (Exception e) {
            log.warn("解析纪要 JSON 数组失败，json={}", StringUtils.abbreviate(json, 200), e);
            return List.of();
        }
    }

    private List<AiMeetingMinutesResponse.Task> readJsonTasks(String json) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        try {
            return JSON.parseArray(json, AiMeetingMinutesResponse.Task.class);
        } catch (Exception e) {
            log.warn("解析纪要任务清单失败，json={}", StringUtils.abbreviate(json, 200), e);
            return List.of();
        }
    }

    /** 纪要解析结果（标题/发言/后续/任务） */
    private static class MinutesContent {
        private String title = "";
        private List<String> content = List.of();
        private List<String> next = List.of();
        private List<AiMeetingMinutesResponse.Task> tasks = List.of();
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
