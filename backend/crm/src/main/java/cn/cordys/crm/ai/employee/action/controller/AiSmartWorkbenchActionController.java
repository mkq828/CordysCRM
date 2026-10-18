package cn.cordys.crm.ai.employee.action.controller;

import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.employee.action.dto.response.AgentActionApproveResponse;
import cn.cordys.crm.ai.employee.action.dto.response.AgentActionSuggestionResponse;
import cn.cordys.crm.ai.employee.action.service.AiEmployeeActionService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智慧工作台「AI 跟进建议 / AI 方案审核」卡片接口（功能 6 二期的投影视图）。
 * <p>
 * 数据源复用 ai_employee_action 的待确认动作，按类型投影为两卡片；不落地
 * agent_action_suggestion / agent_action_approve 两张上游空表，避免另起一套生成管线。
 * 忽略=驳回、确认=执行，均复用 {@link AiEmployeeActionService} 的两段确认闭环。
 * </p>
 */
@RestController
@Tag(name = "智慧工作台-AI 待办")
public class AiSmartWorkbenchActionController {

    @Resource
    private AiEmployeeActionService actionService;

    @PostMapping("/agent-action-suggestion/page")
    @Operation(summary = "AI 跟进建议分页（投影跟进类待确认动作）")
    public Pager<List<AgentActionSuggestionResponse>> suggestionPage(@RequestBody(required = false) BasePageRequest request) {
        return actionService.listSuggestions(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), request);
    }

    @PostMapping("/agent-action-suggestion/ignore/{id}")
    @Operation(summary = "忽略跟进建议（驳回待确认动作）")
    public void ignoreSuggestion(@PathVariable String id) {
        actionService.reject(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/agent-action-approve/page")
    @Operation(summary = "AI 方案审核分页（投影非跟进类待确认动作）")
    public Pager<List<AgentActionApproveResponse>> approvePage(@RequestBody(required = false) BasePageRequest request) {
        return actionService.listApprovals(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), request);
    }

    @PostMapping("/agent-action-approve/ignore/{id}")
    @Operation(summary = "拒绝方案（驳回待确认动作）")
    public void ignoreApprove(@PathVariable String id) {
        actionService.reject(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/agent-action-approve/confirm/{id}")
    @Operation(summary = "确认方案并执行")
    public void confirmApprove(@PathVariable String id) {
        actionService.approve(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
