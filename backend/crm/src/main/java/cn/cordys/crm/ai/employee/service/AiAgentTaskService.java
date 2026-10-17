package cn.cordys.crm.ai.employee.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.conversation.domain.AgentMessage;
import cn.cordys.crm.ai.conversation.mapper.ExtAgentConversationMapper;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.employee.domain.AiEmployee;
import cn.cordys.crm.ai.employee.domain.AiEmployeeAgent;
import cn.cordys.crm.ai.employee.dto.AiAgentTaskProgress;
import cn.cordys.crm.ai.employee.dto.AiAgentUpdateProposal;
import cn.cordys.crm.ai.employee.dto.request.AiAgentTaskApplyRequest;
import cn.cordys.crm.ai.employee.dto.request.AiEmployeeChatRequest;
import cn.cordys.crm.ai.employee.tool.AiAgentTool;
import cn.cordys.crm.ai.employee.tool.AiAgentToolRegistry;
import cn.cordys.crm.ai.knowledge.service.AiKnowledgeService;
import cn.cordys.crm.ai.llm.LlmChatRequest;
import cn.cordys.crm.ai.llm.LlmMessage;
import cn.cordys.crm.ai.llm.LlmProvider;
import cn.cordys.crm.ai.llm.LlmProviderFactory;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.crm.ai.model.domain.AgentModel;
import cn.cordys.crm.ai.model.service.AgentModelService;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.dto.request.CustomerUpdateRequest;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI 员工「自动跑任务」（功能 6 编排层的工具能力）：岗位 Agent 对话增强。
 * <p>
 * 用「prompt JSON」做 ReAct 式工具循环（不依赖厂商 function calling）：把只读工具清单写进系统提示词，
 * 模型需要查数据时输出 {@code {"tool":"xx","args":{...}}}，本类解析后以登录用户身份执行只读查询、把精简
 * 结果回填进对话历史，循环到模型直接给出最终答案。每轮调用都走额度→模型→provider→计费链路，全程只读、
 * 按租户 + 行级数据权限隔离，可商用。
 * </p>
 */
@Service
@Slf4j
public class AiAgentTaskService {

    /** 工具循环轮次上限，防模型反复查数据烧额度 */
    private static final int MAX_ROUNDS = 5;

    /** 回填给模型的单次工具结果长度上限，防 token 爆炸 */
    private static final int MAX_TOOL_RESULT_CHARS = 2000;

    /** 回放的历史消息条数上限，让「请选择候选」这类多轮追问能衔接上下文，又不撑爆上下文 */
    private static final int MAX_HISTORY_MESSAGES = 10;

    /** 客户全景分析类任务的 HTML 报告骨架（只给结构 class 约定，样式由前端套主题，AI 不输出 <style>） */
    private static final String REPORT_SKELETON = """
            <div class="ai-report">
              <header class="hero">
                <span class="hero-tag">{{报告标签，如「客户全景分析」}}</span>
                <h1>{{客户名称}}</h1>
                <div class="hero-meta"><span class="hero-chip">{{关键事实1，如最近跟进时间}}</span><span class="hero-chip">{{关键事实2，如负责人}}</span></div>
              </header>
              <section class="kpis">
                <div class="kpi"><div class="kpi-label">{{指标名}}</div><div class="kpi-value">{{数值}}<small>{{单位}}</small></div><div class="kpi-foot">{{说明}}</div></div>
                <!-- 放 3~4 个指标卡，例如意向分、跟进次数、商机金额、回款率 -->
              </section>
              <section class="section">
                <div class="sec-head"><div class="sec-icon">🔍</div><div><h2>{{分区标题}}</h2><p>{{副标题}}</p></div></div>
                <table class="tbl">
                  <thead><tr><th>{{列名}}</th><th>{{列名}}</th></tr></thead>
                  <tbody><tr><td class="k">{{项}}</td><td>{{值}}</td></tr></tbody>
                </table>
              </section>
              <div class="insight"><span class="em">💡</span><div><strong>关键洞察：</strong>{{洞察内容}}</div></div>
              <section class="section">
                <div class="sec-head"><div class="sec-icon">🚨</div><div><h2>{{行动项标题}}</h2></div></div>
                <div class="actions">
                  <div class="act"><div class="act-no">01<small>{{优先级，如「本周」}}</small></div><div class="act-body"><h4>{{行动标题}}</h4><ul><li>{{要点}}</li></ul></div><div class="act-meta"><div><span class="ml">责任人</span><span class="mv">{{岗位}}</span></div><div><span class="ml">截止</span><span class="mv due">{{日期}}</span></div></div></div>
                </div>
              </section>
            </div>""";

    @Resource
    private BaseMapper<AiEmployeeAgent> agentMapper;

    @Resource
    private ExtAgentConversationMapper extAgentConversationMapper;

    @Resource
    private BaseMapper<AiEmployee> employeeMapper;

    @Resource
    private CustomerService customerService;

    @Resource
    private LogService logService;

    @Resource
    private ResourcePermissionService resourcePermissionService;

    @Resource
    private AgentModelService agentModelService;

    @Resource
    private AiQuotaService aiQuotaService;

    @Resource
    private LlmProviderFactory llmProviderFactory;

    @Resource
    private AiAgentToolRegistry toolRegistry;

    @Resource
    private AiKnowledgeService aiKnowledgeService;

    /**
     * 执行带工具调用的任务对话。中间轮次只通过 {@code onProgress} 回传步骤、不回传文本；
     * 最终答案一次性交给 {@code onChunk} 下发。
     */
    public AiStreamResult<String> taskStream(String orgId, String agentId, String conversationId,
            AiEmployeeChatRequest request, Consumer<AiAgentTaskProgress> onProgress, Consumer<String> onChunk,
            Consumer<AiAgentUpdateProposal> onProposal) {
        if (request == null || StringUtils.isBlank(request.getMessage())) {
            throw new GenericException("请输入内容");
        }
        AiEmployeeAgent agent = checkAgent(agentId, orgId);
        AiEmployee employee = employeeMapper.selectByPrimaryKey(agent.getEmployeeId());
        if (employee == null) {
            throw new GenericException("岗位关联的角色已失效，请重新选择角色");
        }
        List<AgentModel> models = agentModelService.resolveTaskModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }
        String userId = SessionUtils.getUserId();
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        String status = quota.getStatus();
        if (isBlocked(status)) {
            throw new GenericException(blockedMessage(status));
        }

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", buildSystemPrompt(employee, agent)));
        appendHistory(messages, conversationId, orgId);
        String userContent = request.getMessage().trim();
        if (isKnowledgeEnabled(agent)) {
            String knowledgeContext = aiKnowledgeService.retrieveContext(orgId, userContent);
            if (StringUtils.isNotBlank(knowledgeContext)) {
                userContent = userContent + "\n\n## 参考资料（来自企业知识库）\n" + knowledgeContext;
            }
        }
        messages.add(new LlmMessage("user", userContent));

        long totalInput = 0;
        long totalOutput = 0;

        for (int round = 0; round < MAX_ROUNDS; round++) {
            ModelCall call = callModel(orgId, userId, models, messages);
            totalInput += call.usage().getInputTokens();
            totalOutput += call.usage().getOutputTokens();

            ToolCall toolCall = parseToolCall(call.content());
            if (toolCall == null) {
                // 不再需要查数据：视为最终答案，全文下发
                onChunk.accept(call.content());
                LlmUsage usage = new LlmUsage();
                usage.setInputTokens(totalInput);
                usage.setOutputTokens(totalOutput);
                usage.setTotalTokens(totalInput + totalOutput);
                return new AiStreamResult<>(call.content(), usage);
            }

            int seq = round + 1;
            String title = toolTitle(toolCall.tool());
            onProgress.accept(AiAgentTaskProgress.running(seq, toolCall.tool(), "正在" + title, formatArgs(toolCall.args())));

            String result = executeTool(toolCall.tool(), orgId, userId, toolCall.args());

            AiAgentUpdateProposal proposal = parseProposal(result);
            if (proposal != null) {
                onProgress.accept(AiAgentTaskProgress.done(seq, toolCall.tool(), "已生成修改建议", ""));
                String summary = buildProposalSummary(proposal);
                onProposal.accept(proposal);
                onChunk.accept(summary);
                LlmUsage usage = new LlmUsage();
                usage.setInputTokens(totalInput);
                usage.setOutputTokens(totalOutput);
                usage.setTotalTokens(totalInput + totalOutput);
                return new AiStreamResult<>(summary, usage);
            }

            onProgress.accept(AiAgentTaskProgress.done(seq, toolCall.tool(), title + "完成",
                    StringUtils.abbreviate(result, 200)));

            // 把工具调用与其结果回填进对话历史，供下一轮推理
            messages.add(new LlmMessage("assistant", call.content()));
            messages.add(new LlmMessage("user", "工具 " + toolCall.tool() + " 返回结果：\n"
                    + StringUtils.abbreviate(result, MAX_TOOL_RESULT_CHARS)));
        }

        throw new GenericException("任务步骤过多，请拆分为更具体的问题后再试");
    }

    /** 确认执行 AI 员工提出的客户字段修改：服务端再次复核权限后走正常客户更新接口（agentInvoke 部分合并），并落一条可追溯的 AI 员工操作日志 */
    public void applyUpdate(AiAgentTaskApplyRequest request, String agentId, String orgId, String userId) {
        if (request == null || StringUtils.isBlank(request.getCustomerId())
                || request.getChanges() == null || request.getChanges().isEmpty()) {
            throw new GenericException("缺少修改内容");
        }
        AiEmployeeAgent agent = checkAgent(agentId, orgId);
        resourcePermissionService.checkResourcePermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                request.getCustomerId(), FormKeyConstants.CUSTOMER, userId, orgId);

        CustomerUpdateRequest update = new CustomerUpdateRequest();
        update.setId(request.getCustomerId());
        update.setAgentInvoke(true);

        List<BaseModuleFieldValue> moduleFields = new ArrayList<>();
        for (AiAgentTaskApplyRequest.Change change : request.getChanges()) {
            if (StringUtils.isBlank(change.getFieldId())) {
                continue;
            }
            if ("name".equals(change.getFieldId())) {
                update.setName(change.getNewValue());
            } else {
                moduleFields.add(new BaseModuleFieldValue(change.getFieldId(), change.getNewValue()));
            }
        }
        if (!moduleFields.isEmpty()) {
            update.setModuleFields(moduleFields);
        }
        Customer customer = customerService.update(update, userId, orgId);
        logService.add(buildAgentUpdateLog(orgId, userId, agent, customer));
    }

    /** 追加一条可追溯的 AI 员工操作日志，标识「哪个岗位通过对话改了哪个客户」，与常规更新日志（含字段 diff）并存 */
    private LogDTO buildAgentUpdateLog(String orgId, String userId, AiEmployeeAgent agent, Customer customer) {
        LogDTO logDTO = new LogDTO(orgId, customer.getId(), userId, LogType.UPDATE, LogModule.CUSTOMER_INDEX,
                customer.getName());
        logDTO.setDetail("AI 员工「" + agent.getName() + "」通过对话修改");
        logDTO.setPath("/agent/employee/agents/" + agent.getId() + "/task/apply");
        return logDTO;
    }

    // ==================== 内部 ====================

    /** 岗位是否开启「深度用知识库」：与 AiEmployeeService 同口径，开启后在用户问题前拼入检索到的知识库参考 */
    private boolean isKnowledgeEnabled(AiEmployeeAgent agent) {
        return agent.getKnowledgeEnabled() != null && agent.getKnowledgeEnabled() == 1;
    }

    /** 系统提示词：角色提示词 + 岗位补充要求 + 只读工具清单 + 输出约定 */
    private String buildSystemPrompt(AiEmployee employee, AiEmployeeAgent agent) {
        StringBuilder sb = new StringBuilder();
        sb.append(employee.getSystemPrompt() == null ? "" : employee.getSystemPrompt().trim());
        if (StringUtils.isNotBlank(agent.getCustomPrompt())) {
            sb.append("\n\n## 岗位补充要求\n").append(agent.getCustomPrompt().trim());
        }

        sb.append("\n\n## 可用工具（只读查询 CRM 数据，以当前登录用户的可见范围为准）\n");
        for (AiAgentTool tool : toolRegistry.all()) {
            sb.append("- ").append(tool.name()).append("：").append(tool.description()).append('\n');
        }

        sb.append("\n## 输出约定\n");
        sb.append("1. 需要查询 CRM 数据时，只输出一个 JSON 对象（不要任何其他文字、不要 Markdown 代码块）：\n");
        sb.append("   {\"tool\":\"<工具名>\",\"args\":{\"<参数名>\":\"<值>\"}}\n");
        sb.append("2. 已获得足够信息能回答时，直接输出答案文本（不要再输出 JSON）。\n");
        sb.append("3. 查询结果为空或失败时，如实说明，不要编造数据。\n");
        sb.append("4. 搜索客户/线索命中多个候选时，不要自行猜测，先向用户列出候选（编号 + 名称）请其选择；用户确认后再用对应 id 继续查详情。\n");
        sb.append("5. 简单问答用 Markdown 排版：要点用列表、少量对比用表格，保持简短。\n");
        sb.append("6. 当用户要求「分析某客户整体情况 / 客户全景 / 诊断 / 经营分析」时，输出一段 HTML 报告（不要再输出 Markdown、不要用 |…| 表格、不要 <!DOCTYPE>、不要 <style>），从 <div class=\"ai-report\"> 开始，严格沿用下面骨架的 class 名，用真实查询数据替换 {{…}}，可按实际情况增删指标卡/分区/行动项：\n");
        sb.append(REPORT_SKELETON).append('\n');
        sb.append("7. 输出一律使用中文业务名称，不要出现系统内部的英文编码或枚举名（例如商机阶段、字段 code 这类租户看不懂的英文）；行动项里如需建议推进某个阶段，请沿用查询结果中已给出的中文阶段名，不要自行编造不存在的阶段。\n");
        sb.append("8. 用户提到的客户/公司名称，第一步必须先调用 search_customers 工具按该名称查询，以工具返回结果为准判断是否存在；不得因为名称生僻、像错别字、是简称或昵称，就自行推测用户打错字或替换成近音词，也不得在未查询的情况下断言「未匹配」。只有工具确实返回空时，才如实说明并列出候选或请用户补充信息。\n");
        return sb.toString();
    }

    /** 回放本次会话此前的问答，让「请选择候选」这类多轮追问能衔接上下文；跳过刚保存的当前用户消息与异常消息 */
    private void appendHistory(List<LlmMessage> messages, String conversationId, String orgId) {
        if (StringUtils.isBlank(conversationId)) {
            return;
        }
        List<AgentMessage> history = extAgentConversationMapper.selectMessages(orgId, conversationId);
        if (history == null || history.isEmpty()) {
            return;
        }
        // 最后一条是本次刚保存的当前用户消息，后面会作为当前输入重新追加，故跳过
        int end = history.size() - 1;
        int start = Math.max(0, end - MAX_HISTORY_MESSAGES);
        for (int i = start; i < end; i++) {
            AgentMessage m = history.get(i);
            if (!"done".equals(m.getStatus())) {
                continue;
            }
            if (StringUtils.isBlank(m.getContent())) {
                continue;
            }
            String role = "ASSISTANT".equalsIgnoreCase(m.getRole()) ? "assistant" : "user";
            messages.add(new LlmMessage(role, m.getContent().trim()));
        }
    }

    /** 走任务模型候选链累积一整轮输出，返回全文与 token 用量；全部失败抛异常 */
    private ModelCall callModel(String orgId, String userId, List<AgentModel> models, List<LlmMessage> messages) {
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
                return new ModelCall(content.toString(), usage);
            } catch (Exception e) {
                lastError = e;
                if (!content.isEmpty()) {
                    log.error("AI 员工任务模型调用中途失败，provider={}, model={}",
                            model.getProvider(), model.getModelName(), e);
                    throw new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("AI 员工任务模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }
        log.error("AI 员工任务全部模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    /** 解析模型输出：含 {@code tool} 字段则视为工具调用，否则返回 null（视为最终答案） */
    private ToolCall parseToolCall(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String json = extractJson(raw);
        if (json == null) {
            return null;
        }
        try {
            JsonNode root = JSON.MAPPER.readTree(json);
            String tool = root.path("tool").asText("").trim();
            if (StringUtils.isBlank(tool)) {
                return null;
            }
            Map<String, Object> args = new HashMap<>();
            JsonNode argsNode = root.get("args");
            if (argsNode != null && argsNode.isObject()) {
                argsNode.fields().forEachRemaining(e -> args.put(e.getKey(), asPlainValue(e.getValue())));
            }
            return new ToolCall(tool, args);
        } catch (Exception e) {
            return null;
        }
    }

    /** 截取首个 { 到最后一个 } 之间的 JSON 文本；无则返回 null */
    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end < start) {
            return null;
        }
        return raw.substring(start, end + 1);
    }

    private Object asPlainValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return node.toString();
    }

    /** 执行只读工具；任何异常都不打断循环，转成错误结果回填给模型 */
    private String executeTool(String toolName, String orgId, String userId, Map<String, Object> args) {
        AiAgentTool tool = toolRegistry.get(toolName);
        if (tool == null) {
            return JSON.toJSONString(Map.of("error", "未知工具：" + toolName));
        }
        try {
            return tool.execute(orgId, userId, args);
        } catch (Exception e) {
            log.warn("AI 员工工具执行失败，tool={}", toolName, e);
            return JSON.toJSONString(Map.of("error",
                    e.getMessage() == null ? "查询失败" : "查询失败：" + e.getMessage()));
        }
    }

    private String formatArgs(Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            return "";
        }
        return JSON.toJSONString(args);
    }

    /** 识别写建议工具返回的提案；非提案返回 null */
    private AiAgentUpdateProposal parseProposal(String result) {
        if (StringUtils.isBlank(result)) {
            return null;
        }
        try {
            JsonNode root = JSON.MAPPER.readTree(result);
            if (!"customer_update".equals(root.path("__proposal__").asText(""))) {
                return null;
            }
            AiAgentUpdateProposal proposal = new AiAgentUpdateProposal();
            proposal.setType("customer_update");
            proposal.setCustomerId(root.path("customerId").asText(null));
            proposal.setCustomerName(root.path("customerName").asText(null));
            List<AiAgentUpdateProposal.Change> changes = new ArrayList<>();
            JsonNode changesNode = root.get("changes");
            if (changesNode != null && changesNode.isArray()) {
                for (JsonNode c : changesNode) {
                    AiAgentUpdateProposal.Change change = new AiAgentUpdateProposal.Change();
                    change.setFieldId(c.path("fieldId").asText(null));
                    change.setFieldLabel(c.path("fieldLabel").asText(null));
                    change.setOldValue(c.path("oldValue").asText(null));
                    change.setNewValue(c.path("newValue").asText(null));
                    changes.add(change);
                }
            }
            proposal.setChanges(changes);
            return proposal;
        } catch (Exception e) {
            return null;
        }
    }

    /** 提案的 Markdown 摘要，作为本轮最终回答下发 */
    private String buildProposalSummary(AiAgentUpdateProposal proposal) {
        StringBuilder sb = new StringBuilder();
        sb.append("已为你生成修改建议，请确认：\n\n");
        sb.append("| 字段 | 原值 | 新值 |\n");
        sb.append("| --- | --- | --- |\n");
        for (AiAgentUpdateProposal.Change change : proposal.getChanges()) {
            sb.append("| ").append(escape(change.getFieldLabel())).append(" | ")
                    .append(escape(change.getOldValue())).append(" | ")
                    .append(escape(change.getNewValue())).append(" |\n");
        }
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("|", "\\|").replace("\n", " ");
    }

    private String toolTitle(String toolName) {
        return switch (toolName) {
            case "search_customers" -> "查询客户";
            case "get_customer_overview" -> "查询客户全景";
            case "search_clues" -> "查询线索";
            case "search_follow_ups" -> "查询跟进记录";
            case "search_orders" -> "查询订单";
            case "search_contracts" -> "查询合同";
            case "get_customer_fields" -> "查询可编辑字段";
            case "update_customer_field" -> "生成修改建议";
            default -> "执行查询";
        };
    }

    private AiEmployeeAgent checkAgent(String id, String orgId) {
        AiEmployeeAgent agent = agentMapper.selectByPrimaryKey(id);
        if (agent == null || !orgId.equals(agent.getOrganizationId())) {
            throw new GenericException("岗位 Agent 不存在");
        }
        return agent;
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

    /** 单轮模型调用结果 */
    private record ModelCall(String content, LlmUsage usage) {
    }

    /** 解析出的工具调用 */
    private record ToolCall(String tool, Map<String, Object> args) {
    }
}
