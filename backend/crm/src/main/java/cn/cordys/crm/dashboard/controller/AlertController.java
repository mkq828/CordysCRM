package cn.cordys.crm.dashboard.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.dashboard.dto.response.AlertResponse;
import cn.cordys.crm.dashboard.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 经营大屏「主动预警」。
 */
@RestController
@RequestMapping("/dashboard/business-screen")
@Tag(name = "经营大屏")
public class AlertController {

    @Resource
    private AlertService alertService;

    @GetMapping("/alerts")
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "经营大屏主动预警异常项列表")
    public AlertResponse alerts() {
        return alertService.alerts(OrganizationContext.getOrganizationId());
    }
}
