package cn.cordys.crm.dashboard.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.dashboard.dto.response.BusinessScreenOverviewResponse;
import cn.cordys.crm.dashboard.dto.response.RegionDetailResponse;
import cn.cordys.crm.dashboard.dto.response.SalesOverviewResponse;
import cn.cordys.crm.dashboard.dto.response.StageDetailResponse;
import cn.cordys.crm.dashboard.service.BusinessScreenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 经营大屏（租户视角）。
 */
@RestController
@RequestMapping("/dashboard/business-screen")
@Tag(name = "经营大屏")
public class BusinessScreenController {

    @Resource
    private BusinessScreenService businessScreenService;

    @GetMapping("/overview")
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "经营大屏总览")
    public BusinessScreenOverviewResponse overview(@RequestParam(required = false) String range) {
        return businessScreenService.overview(OrganizationContext.getOrganizationId(), range);
    }

    @GetMapping("/sales/{ownerId}")
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "经营大屏下钻：销售个人明细")
    public SalesOverviewResponse salesDetail(@PathVariable String ownerId,
                                             @RequestParam(required = false) String range) {
        return businessScreenService.salesDetail(OrganizationContext.getOrganizationId(), ownerId, range);
    }

    @GetMapping("/region/{adcode}")
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "经营大屏下钻：省级区域明细")
    public RegionDetailResponse regionDetail(@PathVariable String adcode,
                                             @RequestParam(required = false) String range) {
        return businessScreenService.regionDetail(OrganizationContext.getOrganizationId(), adcode, range);
    }

    @GetMapping("/stage/{stage}")
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "经营大屏下钻：合同阶段明细")
    public StageDetailResponse stageDetail(@PathVariable String stage,
                                           @RequestParam(required = false) String range) {
        return businessScreenService.stageDetail(OrganizationContext.getOrganizationId(), stage, range);
    }
}
