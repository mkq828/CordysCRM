package cn.cordys.crm.ai.briefing.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.briefing.domain.AiBriefing;
import cn.cordys.crm.ai.briefing.dto.response.BriefingResponse;
import cn.cordys.crm.ai.briefing.service.BriefingService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 经营大屏「晨报/周报」。
 */
@RestController
@RequestMapping("/dashboard/business-screen/briefing")
@Tag(name = "经营大屏")
public class BriefingController {

    @Resource
    private BriefingService briefingService;

    @GetMapping("/latest")
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "最近一条经营简报")
    public BriefingResponse latest() {
        return briefingService.latest(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/generate")
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "手动生成经营简报")
    public BriefingResponse generate(@RequestParam(required = false) String type) {
        return briefingService.generate(OrganizationContext.getOrganizationId(),
                AiBriefing.TYPE_WEEKLY.equals(type) ? AiBriefing.TYPE_WEEKLY : AiBriefing.TYPE_DAILY,
                SessionUtils.getUserId());
    }
}
