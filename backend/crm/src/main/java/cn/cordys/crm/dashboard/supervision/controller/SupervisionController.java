package cn.cordys.crm.dashboard.supervision.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.dashboard.supervision.dto.request.SupervisionCreateRequest;
import cn.cordys.crm.dashboard.supervision.dto.response.SupervisionBoardResponse;
import cn.cordys.crm.dashboard.supervision.dto.response.SupervisionItem;
import cn.cordys.crm.dashboard.supervision.service.SupervisionService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 经营大屏「督办闭环」。
 */
@RestController
@RequestMapping("/dashboard/business-screen/supervision")
@Tag(name = "经营大屏")
public class SupervisionController {

    @Resource
    private SupervisionService supervisionService;

    @PostMapping
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "下达督办")
    public SupervisionItem create(@RequestBody SupervisionCreateRequest request) {
        return supervisionService.create(OrganizationContext.getOrganizationId(),
                SessionUtils.getUserId(), request);
    }

    @GetMapping
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "督办看板")
    public SupervisionBoardResponse board() {
        return supervisionService.board(OrganizationContext.getOrganizationId());
    }

    @PutMapping("/{id}/done")
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "老板标记督办完成")
    public void markDone(@PathVariable String id) {
        supervisionService.markDone(OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), id);
    }
}
