package cn.cordys.crm.ai.employee.action.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.employee.action.dto.response.AiEmployeeActionResponse;
import cn.cordys.crm.ai.employee.action.service.AbstractAiAutoScanService;
import cn.cordys.crm.ai.employee.action.service.AiEmployeeActionService;
import cn.cordys.crm.ai.employee.action.service.AiFollowUpScanService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 员工待确认动作接口（功能 6 二期「自动跟进闭环」）。沿用 {@code /agent/employee/*} 门控方式：
 * 无 {@code @CsPermission}，靠服务层数据权限 + 执行时权限复核兜底。
 */
@RestController
@RequestMapping("/agent/employee/actions")
@Tag(name = "AI 员工")
public class AiEmployeeActionController {

    @Resource
    private AiEmployeeActionService actionService;

    @Resource
    private AiFollowUpScanService scanService;

    @Resource
    private List<AbstractAiAutoScanService> scanServices;

    @GetMapping
    @Operation(summary = "AI 员工待确认动作列表（本人+下级）")
    public List<AiEmployeeActionResponse> list() {
        return actionService.listPending(OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "确认执行 AI 员工跟进建议")
    public void approve(@PathVariable String id) {
        actionService.approve(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "驳回 AI 员工跟进建议")
    public void reject(@PathVariable String id) {
        actionService.reject(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/scan")
    @Operation(summary = "手动触发一次久未联系客户扫描（测试/按需）")
    public int scan() {
        return scanService.scan(OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/scan/{actionType}")
    @Operation(summary = "手动触发指定类型的自动任务扫描（测试/按需）")
    public int scanByType(@PathVariable String actionType) {
        AbstractAiAutoScanService service = scanServices.stream()
                .filter(s -> s.actionType().equalsIgnoreCase(actionType))
                .findFirst()
                .orElseThrow(() -> new GenericException("不支持的动作类型: " + actionType));
        return service.scan(OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
