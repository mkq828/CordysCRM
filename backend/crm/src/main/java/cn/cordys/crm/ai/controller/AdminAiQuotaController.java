package cn.cordys.crm.ai.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.ai.dto.request.AdminAiCostRequest;
import cn.cordys.crm.ai.dto.request.AiMockRecordRequest;
import cn.cordys.crm.ai.dto.request.AiModelPriceSaveRequest;
import cn.cordys.crm.ai.dto.request.AiQuotaConfigRequest;
import cn.cordys.crm.ai.dto.request.AiTenantQuotaListRequest;
import cn.cordys.crm.ai.dto.request.AiTenantQuotaResetRequest;
import cn.cordys.crm.ai.dto.request.AiTenantQuotaSaveRequest;
import cn.cordys.crm.ai.dto.response.AdminAiCostResponse;
import cn.cordys.crm.ai.dto.response.AiModelPriceResponse;
import cn.cordys.crm.ai.dto.response.AiQuotaConfigResponse;
import cn.cordys.crm.ai.dto.response.AiQuotaRecordResult;
import cn.cordys.crm.ai.dto.response.AiTenantQuotaRow;
import cn.cordys.crm.ai.service.AiQuotaService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 额度管理（平台端，仅 admin）
 */
@RestController
@RequestMapping("/admin/ai-quota")
@Tag(name = "AI 额度管理")
public class AdminAiQuotaController {

    @Resource
    private AiQuotaService aiQuotaService;

    @PostMapping("/cost")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_READ)
    @Operation(summary = "AI 额度-成本看板")
    public AdminAiCostResponse cost(@RequestBody AdminAiCostRequest request) {
        return aiQuotaService.getAdminCostOverview(request);
    }

    @GetMapping("/model-price/list")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_READ)
    @Operation(summary = "AI 额度-模型单价列表")
    public List<AiModelPriceResponse> modelPriceList() {
        return aiQuotaService.listModelPrices();
    }

    @PostMapping("/model-price/save")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_WRITE)
    @Operation(summary = "AI 额度-模型单价保存")
    public void modelPriceSave(@Validated @RequestBody AiModelPriceSaveRequest request) {
        aiQuotaService.saveModelPrice(request, SessionUtils.getUserId());
    }

    @GetMapping("/config")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_READ)
    @Operation(summary = "AI 额度-全局配置查询")
    public AiQuotaConfigResponse config() {
        return aiQuotaService.getConfig();
    }

    @PostMapping("/config")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_WRITE)
    @Operation(summary = "AI 额度-全局配置更新")
    public void configUpdate(@RequestBody AiQuotaConfigRequest request) {
        aiQuotaService.updateConfig(request);
    }

    /**
     * 验证用：模拟一次 AI 调用记账，用于无真实模型时验证计量/软硬超/熔断全链路。
     * 模型调用联网后由模型层调用 {@link AiQuotaService#record} 替代，上线前删除本接口。
     */
    @PostMapping("/mock-record")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_WRITE)
    @Operation(summary = "AI 额度-模拟记账(验证用，上线前删)")
    public AiQuotaRecordResult mockRecord(@Validated @RequestBody AiMockRecordRequest request) {
        return aiQuotaService.record(request.getOrganizationId(), request.getFeatureCode(),
                request.getModelCode(), request.getInputTokens(), request.getOutputTokens());
    }

    @PostMapping("/tenant/list")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_READ)
    @Operation(summary = "AI 额度-租户额度列表")
    public List<AiTenantQuotaRow> tenantList(@RequestBody AiTenantQuotaListRequest request) {
        return aiQuotaService.listTenantQuotas(request.getKeyword());
    }

    @PostMapping("/tenant/quota/save")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_WRITE)
    @Operation(summary = "AI 额度-按租户调额")
    public void tenantQuotaSave(@Validated @RequestBody AiTenantQuotaSaveRequest request) {
        aiQuotaService.saveOverride(request.getOrganizationId(), request.getQuota(), SessionUtils.getUserId());
    }

    @PostMapping("/tenant/quota/reset")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_WRITE)
    @Operation(summary = "AI 额度-恢复默认额度")
    public void tenantQuotaReset(@Validated @RequestBody AiTenantQuotaResetRequest request) {
        aiQuotaService.resetOverride(request.getOrganizationId());
    }
}
