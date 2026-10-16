package cn.cordys.crm.ai.attribution.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.attribution.dto.request.AttributionRequest;
import cn.cordys.crm.ai.attribution.dto.response.AttributionResponse;
import cn.cordys.crm.ai.attribution.service.AttributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 经营大屏「经营归因」。
 */
@RestController
@RequestMapping("/dashboard/business-screen")
@Tag(name = "经营大屏")
public class AttributionController {

    @Resource
    private AttributionService attributionService;

    @PostMapping("/attribution")
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "经营归因：针对某维度下钻数据生成「为什么」结论")
    public AttributionResponse attribute(@RequestBody AttributionRequest request) {
        return attributionService.attribute(OrganizationContext.getOrganizationId(), request);
    }
}
