package cn.cordys.crm.ai.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.dto.response.AiFeatureUsagePoint;
import cn.cordys.crm.ai.dto.response.AiQuotaTrendPoint;
import cn.cordys.crm.ai.dto.response.TenantQuotaOverviewResponse;
import cn.cordys.crm.ai.service.AiQuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 额度（租户端，次数视角，仅当前登录租户）
 */
@RestController
@RequestMapping("/ai/quota")
@Tag(name = "AI 额度")
public class AiQuotaController {

    @Resource
    private AiQuotaService aiQuotaService;

    @GetMapping("/overview")
    @Operation(summary = "AI 额度-本月总览")
    public TenantQuotaOverviewResponse overview() {
        return aiQuotaService.getTenantOverview(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/trend")
    @Operation(summary = "AI 额度-用量趋势")
    public List<AiQuotaTrendPoint> trend(@RequestParam(required = false) String groupBy) {
        return aiQuotaService.getTenantTrend(OrganizationContext.getOrganizationId(), groupBy);
    }

    @GetMapping("/feature-usage")
    @Operation(summary = "AI 额度-分功能用量")
    public List<AiFeatureUsagePoint> featureUsage() {
        return aiQuotaService.getFeatureUsage(OrganizationContext.getOrganizationId());
    }
}
