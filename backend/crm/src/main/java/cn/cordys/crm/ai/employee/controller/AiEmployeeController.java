package cn.cordys.crm.ai.employee.controller;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.controller.BaseAiStreamController;
import cn.cordys.crm.ai.controller.SseEventWriter;
import cn.cordys.crm.ai.conversation.service.AgentConversationService;
import cn.cordys.crm.ai.dto.response.AiStreamResult;
import cn.cordys.crm.ai.employee.dto.AiAgentUpdateProposal;
import cn.cordys.crm.ai.employee.dto.request.AiAgentTaskApplyRequest;
import cn.cordys.crm.ai.employee.dto.request.AiEmployeeAgentSaveRequest;
import cn.cordys.crm.ai.employee.dto.request.AiEmployeeChatRequest;
import cn.cordys.crm.ai.employee.dto.request.AiMeetingMinutesGenerateRequest;
import cn.cordys.crm.ai.employee.dto.response.AiEmployeeAgentResponse;
import cn.cordys.crm.ai.employee.dto.response.AiEmployeeDetailResponse;
import cn.cordys.crm.ai.employee.dto.response.AiEmployeeDivisionResponse;
import cn.cordys.crm.ai.employee.dto.response.AiEmployeeItemResponse;
import cn.cordys.crm.ai.employee.dto.response.AiMeetingMinutesResponse;
import cn.cordys.crm.ai.employee.service.AiAgentTaskService;
import cn.cordys.crm.ai.employee.service.AiEmployeeService;
import cn.cordys.crm.ai.employee.service.AiMeetingService;
import cn.cordys.crm.ai.llm.LlmUsage;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * AI 员工接口（功能 6，编排层）：系统角色目录浏览 + 租户岗位 Agent 增删改 + 以角色提示词编排的流式对话。
 */
@RestController
@RequestMapping("/agent/employee")
@Tag(name = "AI 员工")
public class AiEmployeeController extends BaseAiStreamController {

    @Resource
    private AiEmployeeService aiEmployeeService;

    @Resource
    private AiMeetingService aiMeetingService;

    @Resource
    private AiAgentTaskService aiAgentTaskService;

    @Resource
    private AgentConversationService agentConversationService;

    @GetMapping("/divisions")
    @Operation(summary = "角色目录-部门导航（部门 + 角色数量）")
    public List<AiEmployeeDivisionResponse> divisions() {
        return aiEmployeeService.listDivisions();
    }

    @GetMapping("/catalog")
    @Operation(summary = "角色目录-某部门下角色列表")
    public List<AiEmployeeItemResponse> catalog(@RequestParam String division,
            @RequestParam(required = false) String keyword) {
        return aiEmployeeService.listCatalog(division, keyword);
    }

    @GetMapping("/{id}")
    @Operation(summary = "角色目录-角色详情（含系统提示词）")
    public AiEmployeeDetailResponse detail(@PathVariable String id) {
        return aiEmployeeService.getEmployee(id);
    }

    @GetMapping("/agents")
    @Operation(summary = "岗位 Agent 列表（当前租户）")
    public List<AiEmployeeAgentResponse> agents() {
        return aiEmployeeService.listAgents(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/agents/add")
    @Operation(summary = "新增岗位 Agent")
    public void addAgent(@RequestBody AiEmployeeAgentSaveRequest request) {
        aiEmployeeService.saveAgent(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/agents/update")
    @Operation(summary = "更新岗位 Agent")
    public void updateAgent(@RequestBody AiEmployeeAgentSaveRequest request) {
        aiEmployeeService.saveAgent(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @GetMapping("/agents/delete/{id}")
    @Operation(summary = "删除岗位 Agent")
    public void deleteAgent(@PathVariable String id) {
        aiEmployeeService.deleteAgent(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/meeting/minutes")
    @Operation(summary = "生成会议纪要（董事长主持的虚拟会议自动总结）")
    public AiMeetingMinutesResponse generateMinutes(@RequestBody AiMeetingMinutesGenerateRequest request) {
        return aiMeetingService.generateMinutes(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(),
                request);
    }

    @GetMapping("/meeting/minutes")
    @Operation(summary = "会议纪要列表（当前租户）")
    public List<AiMeetingMinutesResponse> minutes() {
        return aiMeetingService.listMinutes(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/meeting/minutes/{id}")
    @Operation(summary = "会议纪要详情")
    public AiMeetingMinutesResponse minutesDetail(@PathVariable String id) {
        return aiMeetingService.getMinutes(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping(value = "/agents/{id}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "岗位 Agent 流式对话")
    public void chatStream(@PathVariable String id, @RequestBody AiEmployeeChatRequest request,
            HttpServletResponse response) throws IOException {
        String orgId = OrganizationContext.getOrganizationId();
        String userId = SessionUtils.getUserId();
        PrintWriter writer = beginSse(response);

        String runId = IDGenerator.nextStr();
        String assistantMessageId = IDGenerator.nextStr();
        String conversationId = agentConversationService.resolveConversation(orgId, userId,
                AiQuotaConstant.AI_EMPLOYEE, request.getConversationId(),
                title(request.getMessage(), "AI 员工"));
        agentConversationService.saveUserMessage(conversationId, runId, orgId, userId,
                request.getMessage() == null ? "" : request.getMessage().trim(), null);

        SseEventWriter.write(writer, "run", null, runData(conversationId, runId, assistantMessageId));

        StringBuilder answer = new StringBuilder();
        try {
            AiStreamResult<String> streamResult = aiEmployeeService.chatStream(orgId, id, request, chunk -> {
                answer.append(chunk);
                try {
                    SseEventWriter.write(writer, "chunk", conversationId, chunk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            LlmUsage usage = streamResult.usage();
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, orgId, userId,
                    answer.toString(), null, "done", usage);
            SseEventWriter.write(writer, "done", null, doneData(conversationId, runId, assistantMessageId, usage, null));
        } catch (Exception e) {
            String message = e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage();
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, orgId, userId,
                    message, null, "error", null);
            SseEventWriter.write(writer, "error", null, Map.of("message", message));
        } finally {
            writer.flush();
            writer.close();
        }
    }

    @PostMapping(value = "/agents/{id}/task/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "岗位 Agent 自动跑任务（工具调用对话）")
    public void taskStream(@PathVariable String id, @RequestBody AiEmployeeChatRequest request,
            HttpServletResponse response) throws IOException {
        String orgId = OrganizationContext.getOrganizationId();
        String userId = SessionUtils.getUserId();
        PrintWriter writer = beginSse(response);

        String runId = IDGenerator.nextStr();
        String assistantMessageId = IDGenerator.nextStr();
        String conversationId = agentConversationService.resolveConversation(orgId, userId,
                AiQuotaConstant.AI_EMPLOYEE, request.getConversationId(),
                title(request.getMessage(), "AI 员工"));
        agentConversationService.saveUserMessage(conversationId, runId, orgId, userId,
                request.getMessage() == null ? "" : request.getMessage().trim(), null);

        SseEventWriter.write(writer, "run", null, runData(conversationId, runId, assistantMessageId));

        StringBuilder answer = new StringBuilder();
        try {
            AiAgentUpdateProposal[] proposalHolder = new AiAgentUpdateProposal[1];
            AiStreamResult<String> streamResult = aiAgentTaskService.taskStream(orgId, id, conversationId, request,
                    progress -> {
                        try {
                            SseEventWriter.write(writer, "progress", conversationId, progress);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    chunk -> {
                        answer.append(chunk);
                        try {
                            SseEventWriter.write(writer, "chunk", conversationId, chunk);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    proposal -> proposalHolder[0] = proposal);
            LlmUsage usage = streamResult.usage();
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, orgId, userId,
                    answer.toString(), null, "done", usage);
            SseEventWriter.write(writer, "done", null,
                    doneData(conversationId, runId, assistantMessageId, usage, proposalHolder[0]));
        } catch (Exception e) {
            String message = e.getMessage() == null ? "AI 服务异常，请稍后重试" : e.getMessage();
            agentConversationService.saveAssistantMessage(conversationId, runId, assistantMessageId, orgId, userId,
                    message, null, "error", null);
            SseEventWriter.write(writer, "error", null, Map.of("message", message));
        } finally {
            writer.flush();
            writer.close();
        }
    }

    @PostMapping("/agents/{id}/task/apply")
    @Operation(summary = "确认执行 AI 员工提出的客户字段修改")
    public void applyTask(@PathVariable String id, @RequestBody AiAgentTaskApplyRequest request) {
        aiAgentTaskService.applyUpdate(request, id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
