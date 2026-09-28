package cn.cordys.crm.platform.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.platform.dto.request.PlatformRevenueRequest;
import cn.cordys.crm.platform.dto.response.PlatformRevenueOverviewResponse;
import cn.cordys.crm.platform.service.PlatformRevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台营收看板（仅 admin）
 */
@RestController
@RequestMapping("/platform/revenue")
@Tag(name = "平台营收看板")
public class PlatformRevenueController {

    @Resource
    private PlatformRevenueService platformRevenueService;

    @PostMapping("/overview")
    @CsPermission(PermissionConstants.ADMIN_FINANCE_READ)
    @Operation(summary = "平台营收看板-总览")
    public PlatformRevenueOverviewResponse overview(@Validated @RequestBody PlatformRevenueRequest request) {
        return platformRevenueService.overview(request);
    }
}
