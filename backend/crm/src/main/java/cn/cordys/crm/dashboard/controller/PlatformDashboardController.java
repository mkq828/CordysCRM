package cn.cordys.crm.dashboard.controller;

import cn.cordys.crm.dashboard.dto.response.PlatformDashboardResponse;
import cn.cordys.crm.dashboard.service.PlatformDashboardService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台大屏（仅 admin）。
 */
@RestController
@RequestMapping("/dashboard/platform")
@Tag(name = "平台大屏")
public class PlatformDashboardController {

    @Resource
    private PlatformDashboardService platformDashboardService;

    @GetMapping("/overview")
    @Operation(summary = "平台总览（仅admin）")
    public PlatformDashboardResponse overview() {
        return platformDashboardService.overview(SessionUtils.getUserId());
    }
}
