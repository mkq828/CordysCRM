package cn.cordys.crm.ai.employee.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.employee.domain.AiEmployee;
import cn.cordys.crm.ai.employee.domain.AiEmployeeAgent;
import cn.cordys.crm.ai.employee.dto.request.AiEmployeeAgentSaveRequest;
import cn.cordys.crm.ai.employee.dto.request.AiEmployeeChatRequest;
import cn.cordys.crm.ai.employee.dto.response.AiEmployeeAgentResponse;
import cn.cordys.crm.ai.employee.dto.response.AiEmployeeDetailResponse;
import cn.cordys.crm.ai.employee.dto.response.AiEmployeeDivisionResponse;
import cn.cordys.crm.ai.employee.dto.response.AiEmployeeItemResponse;
import cn.cordys.crm.ai.knowledge.service.AiKnowledgeService;
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
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * AI 员工（功能 6，编排层）：系统角色目录浏览 + 租户「岗位 Agent」CRUD + 以角色提示词编排的流式对话。
 * <p>
 * 对话复用额度→模型→provider→计费四步链路，按 {@code ai_employee} 记账；系统提示词 = 角色提示词 + 岗位
 * 自定义提示词（可选）。岗位开启「挂载知识库」时，先召回企业知识库相关片段作为参考资料注入用户消息。
 * </p>
 */
@Service
@Slf4j
public class AiEmployeeService {

    @Resource
    private BaseMapper<AiEmployee> employeeMapper;

    @Resource
    private BaseMapper<AiEmployeeAgent> agentMapper;

    @Resource
    private AiQuotaService aiQuotaService;

    @Resource
    private AgentModelService agentModelService;

    @Resource
    private LlmProviderFactory llmProviderFactory;

    @Resource
    private AiKnowledgeService aiKnowledgeService;

    // ==================== 角色目录 ====================

    /** 部门导航：按部门聚合角色数量，部门名升序 */
    public List<AiEmployeeDivisionResponse> listDivisions() {
        Map<String, Long> counts = employeeMapper.selectAll(null).stream()
                .collect(Collectors.groupingBy(e -> StringUtils.defaultString(e.getDivision()), Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    AiEmployeeDivisionResponse response = new AiEmployeeDivisionResponse();
                    response.setDivision(e.getKey());
                    response.setCount(e.getValue());
                    return response;
                })
                .toList();
    }

    /** 某部门下角色列表（按 sort 升序），可带关键词模糊匹配角色名/描述 */
    public List<AiEmployeeItemResponse> listCatalog(String division, String keyword) {
        LambdaQueryWrapper<AiEmployee> wrapper = new LambdaQueryWrapper<AiEmployee>()
                .eq(AiEmployee::getDivision, division);
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.or(sub -> sub.like(AiEmployee::getName, kw).like(AiEmployee::getDescription, kw));
        }
        wrapper.orderByAsc(AiEmployee::getSort);
        return employeeMapper.selectListByLambda(wrapper).stream()
                .map(this::toItem)
                .toList();
    }

    /** 角色详情（含完整系统提示词） */
    public AiEmployeeDetailResponse getEmployee(String id) {
        AiEmployee employee = employeeMapper.selectByPrimaryKey(id);
        if (employee == null) {
            throw new GenericException("角色不存在");
        }
        AiEmployeeDetailResponse response = new AiEmployeeDetailResponse();
        copyItem(response, employee);
        response.setSystemPrompt(employee.getSystemPrompt());
        return response;
    }

    // ==================== 岗位 Agent ====================

    /** 当前租户的岗位 Agent 列表（按 sort 升序），并回填关联角色名/emoji/品牌色 */
    public List<AiEmployeeAgentResponse> listAgents(String orgId) {
        LambdaQueryWrapper<AiEmployeeAgent> wrapper = new LambdaQueryWrapper<AiEmployeeAgent>()
                .eq(AiEmployeeAgent::getOrganizationId, orgId);
        wrapper.orderByAsc(AiEmployeeAgent::getSort);
        List<AiEmployeeAgent> agents = agentMapper.selectListByLambda(wrapper);
        Map<String, AiEmployee> employeeMap = resolveEmployeeMap(agents);
        return agents.stream()
                .map(agent -> toAgentResponse(agent, employeeMap.get(agent.getEmployeeId())))
                .toList();
    }

    /** 新增/更新岗位 Agent */
    public void saveAgent(AiEmployeeAgentSaveRequest request, String orgId, String userId) {
        if (StringUtils.isBlank(request.getName())) {
            throw new GenericException("请填写岗位名称");
        }
        if (StringUtils.isBlank(request.getEmployeeId())) {
            throw new GenericException("请选择员工角色");
        }
        AiEmployee employee = employeeMapper.selectByPrimaryKey(request.getEmployeeId());
        if (employee == null) {
            throw new GenericException("所选角色不存在");
        }
        long now = System.currentTimeMillis();
        if (StringUtils.isBlank(request.getId())) {
            AiEmployeeAgent agent = new AiEmployeeAgent();
            agent.setId(IDGenerator.nextStr());
            agent.setOrganizationId(orgId);
            applyAgent(agent, request);
            agent.setCreateUser(userId);
            agent.setUpdateUser(userId);
            agent.setCreateTime(now);
            agent.setUpdateTime(now);
            agentMapper.insert(agent);
        } else {
            AiEmployeeAgent agent = checkAgent(request.getId(), orgId);
            applyAgent(agent, request);
            agent.setUpdateUser(userId);
            agent.setUpdateTime(now);
            agentMapper.updateById(agent);
        }
    }

    /** 删除岗位 Agent */
    public void deleteAgent(String id, String orgId) {
        checkAgent(id, orgId);
        agentMapper.deleteByPrimaryKey(id);
    }

    // ==================== 对话 ====================

    /** 以岗位 Agent 关联的角色提示词 + 自定义提示词编排流式对话，按 ai_employee 记账，返回累积全文 */
    public AiStreamResult<String> chatStream(String orgId, String agentId, AiEmployeeChatRequest request,
            Consumer<String> onChunk) {
        if (StringUtils.isBlank(request.getMessage())) {
            throw new GenericException("请输入内容");
        }
        AiEmployeeAgent agent = checkAgent(agentId, orgId);
        AiEmployee employee = employeeMapper.selectByPrimaryKey(agent.getEmployeeId());
        if (employee == null) {
            throw new GenericException("岗位关联的角色已失效，请重新选择角色");
        }

        List<AgentModel> models = agentModelService.resolveChatModels(orgId);
        if (models.isEmpty()) {
            throw new GenericException("请先在「模型设置」中配置并启用一个模型");
        }
        String userId = SessionUtils.getUserId();
        AiQuotaRecordResult quota = aiQuotaService.checkQuota(orgId);
        String status = quota.getStatus();
        if (isBlocked(status)) {
            throw new GenericException(blockedMessage(status));
        }

        String knowledgeContext = isKnowledgeEnabled(agent)
                ? aiKnowledgeService.retrieveContext(orgId, request.getMessage())
                : "";
        List<LlmMessage> messages = buildMessages(employee, agent, request.getMessage().trim(), knowledgeContext);
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

                LlmUsage usage = provider.chatStream(llmRequest, chunk -> {
                    content.append(chunk);
                    onChunk.accept(chunk);
                });
                aiQuotaService.record(orgId, AiQuotaConstant.AI_EMPLOYEE, model.getModelName(),
                        usage.getInputTokens(), usage.getOutputTokens(), userId);
                return new AiStreamResult<>(content.toString(), usage);
            } catch (Exception e) {
                lastError = e;
                if (!content.isEmpty()) {
                    log.error("AI 员工对话模型调用中途失败，provider={}, model={}", model.getProvider(), model.getModelName(), e);
                    throw new GenericException(e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage());
                }
                log.warn("AI 员工对话模型调用失败，自动降级尝试下一个候选，provider={}, model={}",
                        model.getProvider(), model.getModelName(), e);
            }
        }

        log.error("AI 员工对话全部 AI 模型调用失败，候选数={}", models.size(), lastError);
        throw new GenericException(lastError == null || lastError.getMessage() == null
                ? "AI 服务异常，请稍后重试" : lastError.getMessage());
    }

    // ==================== 内部 ====================

    /** 构造 system + user 消息：角色提示词为底，拼岗位自定义提示词；开启知识库时把召回片段作为参考资料注入用户消息 */
    private List<LlmMessage> buildMessages(AiEmployee employee, AiEmployeeAgent agent, String message,
            String knowledgeContext) {
        String systemPrompt = employee.getSystemPrompt() == null ? "" : employee.getSystemPrompt().trim();
        if (StringUtils.isNotBlank(agent.getCustomPrompt())) {
            systemPrompt = systemPrompt + "\n\n## 岗位补充要求\n" + agent.getCustomPrompt().trim();
        }
        String userMessage = message;
        if (StringUtils.isNotBlank(knowledgeContext)) {
            userMessage = message + "\n\n## 参考资料（来自企业知识库）\n" + knowledgeContext
                    + "\n\n请优先依据上述资料回答；若资料与问题无关，按你的角色正常回答。";
        }
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", systemPrompt));
        messages.add(new LlmMessage("user", userMessage));
        return messages;
    }

    /** 岗位是否开启「挂载知识库」（1=是） */
    private boolean isKnowledgeEnabled(AiEmployeeAgent agent) {
        return agent.getKnowledgeEnabled() != null && agent.getKnowledgeEnabled() == 1;
    }

    private AiEmployeeAgent checkAgent(String id, String orgId) {
        AiEmployeeAgent agent = agentMapper.selectByPrimaryKey(id);
        if (agent == null || !orgId.equals(agent.getOrganizationId())) {
            throw new GenericException("岗位 Agent 不存在");
        }
        return agent;
    }

    private void applyAgent(AiEmployeeAgent agent, AiEmployeeAgentSaveRequest request) {
        agent.setName(request.getName().trim());
        agent.setEmployeeId(request.getEmployeeId());
        agent.setCustomPrompt(request.getCustomPrompt());
        agent.setKnowledgeEnabled(Boolean.TRUE.equals(request.getKnowledgeEnabled()) ? 1 : 0);
        agent.setSort(request.getSort() == null ? 0L : request.getSort());
    }

    private Map<String, AiEmployee> resolveEmployeeMap(List<AiEmployeeAgent> agents) {
        List<String> employeeIds = agents.stream()
                .map(AiEmployeeAgent::getEmployeeId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        Map<String, AiEmployee> map = new HashMap<>();
        for (AiEmployee employee : employeeMapper.selectByIds(employeeIds)) {
            map.put(employee.getId(), employee);
        }
        return map;
    }

    private AiEmployeeItemResponse toItem(AiEmployee employee) {
        AiEmployeeItemResponse response = new AiEmployeeItemResponse();
        copyItem(response, employee);
        return response;
    }

    private void copyItem(AiEmployeeItemResponse response, AiEmployee employee) {
        response.setId(employee.getId());
        response.setDivision(employee.getDivision());
        response.setName(employee.getName());
        response.setDescription(employee.getDescription());
        response.setColor(employee.getColor());
        response.setEmoji(employee.getEmoji());
        response.setVibe(employee.getVibe());
    }

    private AiEmployeeAgentResponse toAgentResponse(AiEmployeeAgent agent, AiEmployee employee) {
        AiEmployeeAgentResponse response = new AiEmployeeAgentResponse();
        response.setId(agent.getId());
        response.setName(agent.getName());
        response.setEmployeeId(agent.getEmployeeId());
        if (employee != null) {
            response.setEmployeeName(employee.getName());
            response.setEmployeeEmoji(employee.getEmoji());
            response.setEmployeeColor(employee.getColor());
        }
        response.setCustomPrompt(agent.getCustomPrompt());
        response.setKnowledgeEnabled(agent.getKnowledgeEnabled());
        response.setSort(agent.getSort());
        response.setCreateTime(agent.getCreateTime());
        response.setUpdateTime(agent.getUpdateTime());
        return response;
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
