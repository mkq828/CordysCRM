package cn.cordys.crm.ai.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.dto.request.AiQuotaPackApplyRequest;
import cn.cordys.crm.ai.dto.response.AiQuotaPackBalanceResponse;
import cn.cordys.crm.ai.dto.response.AiQuotaPackOrderResponse;
import cn.cordys.crm.ai.dto.response.AiQuotaPackResponse;
import cn.cordys.crm.ai.service.AiQuotaPackService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 额度包（租户端：购买 + 余额 + 我的订单）
 */
@RestController
@RequestMapping("/ai/quota-pack")
@Tag(name = "AI 额度包")
public class AiQuotaPackController {

    @Resource
    private AiQuotaPackService aiQuotaPackService;

    @GetMapping("/packs")
    @Operation(summary = "可购额度包列表")
    public List<AiQuotaPackResponse> packs() {
        return aiQuotaPackService.listPackResponses(true);
    }

    @GetMapping("/balance")
    @Operation(summary = "我的已购包余额")
    public List<AiQuotaPackBalanceResponse> balance() {
        return aiQuotaPackService.listBalanceResponses(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/apply")
    @Operation(summary = "申请购买额度包")
    public void apply(@Validated @RequestBody AiQuotaPackApplyRequest request) {
        aiQuotaPackService.applyOrder(OrganizationContext.getOrganizationId(),
                request.getPackId(), SessionUtils.getUserId());
    }

    @GetMapping("/orders")
    @Operation(summary = "我的购买单")
    public List<AiQuotaPackOrderResponse> orders(@RequestParam(required = false) String status) {
        return aiQuotaPackService.listOrgOrderResponses(OrganizationContext.getOrganizationId(), status);
    }
}
