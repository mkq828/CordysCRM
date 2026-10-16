package cn.cordys.crm.ai.bi.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.bi.dto.request.AskBiQueryRequest;
import cn.cordys.crm.ai.bi.dto.response.AskBiQueryResponse;
import cn.cordys.crm.ai.bi.dto.response.BiCatalogResponse;
import cn.cordys.crm.ai.bi.service.AskBiService;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.system.service.EditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 问数 BI 接口（功能 12，企业版 G3 门控，生成型挂 G2 额度）。
 * 同步返回结果 + 图表 + SQL，不做异步轮询（查询即时完成）。
 */
@RestController
@RequestMapping("/agent/bi")
@Tag(name = "AI 问数 BI")
public class AskBiController {

    @Resource
    private AskBiService askBiService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通 AI 问数 BI")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_ASK_BI);
    }

    @GetMapping("/catalog")
    @Operation(summary = "查询可查指标/示例问题/可查范围提示")
    public BiCatalogResponse catalog() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return askBiService.catalog();
    }

    @PostMapping("/query")
    @Operation(summary = "提交大白话数据问题，返回结果 + 图表 + SQL")
    public AskBiQueryResponse query(@Valid @RequestBody AskBiQueryRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return askBiService.query(orgId, request);
    }

    @PostMapping("/screen-query")
    @CsPermission(PermissionConstants.DASHBOARD_READ)
    @Operation(summary = "数据大屏「AI 经营助手」只读问数（免费全租户，不计量额度）")
    public AskBiQueryResponse screenQuery(@Valid @RequestBody AskBiQueryRequest request) {
        return askBiService.queryFree(OrganizationContext.getOrganizationId(), request);
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_ASK_BI)) {
            throw new GenericException("当前版本未开通 AI 问数 BI，请升级至企业版");
        }
    }
}
